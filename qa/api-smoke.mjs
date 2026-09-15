import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import { request } from 'playwright';

// Apenas ambiente local isolado. Cria fixtures identificadas e cancela consultas.
const baseURL = process.env.API_BASE_URL || 'http://127.0.0.1:8081';
if (!['localhost', '127.0.0.1'].includes(new URL(baseURL).hostname)) throw new Error('Use um servidor local de testes.');
const email = process.env.BOOTSTRAP_ADMIN_EMAIL;
const password = process.env.BOOTSTRAP_ADMIN_PASSWORD;
assert.ok(email && password, 'Carregue scripts/use-local-env.ps1 ou forneça credenciais de teste.');
const report = { startedAt: new Date().toISOString(), baseURL, checks: [] };
const contexts = [];
async function client() {
  const context = await request.newContext({ baseURL }); contexts.push(context);
  return {
    context,
    async call(method, path, body, expected = 200, csrf = true) {
      const headers = {};
      if (!['GET', 'HEAD'].includes(method) && csrf) {
        const response = await context.get('/api/v1/auth/csrf');
        assert.equal(response.status(), 200, 'CSRF disponível');
        const token = await response.json(); headers[token.headerName] = token.token;
      }
      const response = await context.fetch('/api/v1' + path, { method, data: body, headers });
      assert.equal(response.status(), expected, `${method} ${path}: ${await response.text()}`);
      return response.status() === 204 ? undefined : response.json();
    },
  };
}
async function check(name, fn) { await fn(); report.checks.push({ name, result: 'PASS' }); console.log('PASS ' + name); }
const admin = await client();
const anon = await client();
const tag = Date.now().toString(36);
let svc, professional, patient, appointment, reception, secondPatient;
const receptionPassword = randomBytes(24).toString('base64url');
const date = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
const start = `${date}T10:00:00-03:00`;
const patientBody = name => ({ name, birthDate: '1995-05-15', phone: '61999990000', email: '', guardianName: '', guardianRelationship: '', guardianPhone: '' });
const serviceInput = s => ({ name: s.name, description: s.description, durationMinutes: s.durationMinutes, published: s.published, version: s.version });
try {
  await check('API pública não expõe endereço não confirmado ou pacientes', async () => {
    const clinic = await anon.call('GET', '/public/clinic');
    assert.ok(clinic.displayName); assert.ok(!JSON.stringify(clinic).includes('APTO 103'));
    await anon.call('GET', '/patients', undefined, 401);
  });
  await check('Login inválido e CSRF obrigatório', async () => {
    await anon.call('POST', '/auth/login', { email, password: 'incorreta' }, 401);
    await anon.call('POST', '/auth/login', { email, password }, 403, false);
  });
  await check('Login ADMIN e consulta da própria sessão', async () => {
    await admin.call('POST', '/auth/login', { email, password });
    assert.equal((await admin.call('GET', '/auth/me')).role, 'ADMIN');
    const openapi = await admin.call('GET', '/openapi');
    await mkdir('docs', { recursive: true });
    await writeFile('docs/openapi.json', JSON.stringify(openapi, null, 2));
  });
  await check('CRUD de serviço e proteção contra edição obsoleta', async () => {
    svc = await admin.call('POST', '/services', { name: `QA serviço ${tag}`, description: 'Fixture fictícia de integração.', durationMinutes: 45, published: false }, 201);
    const old = serviceInput(svc);
    svc = await admin.call('PUT', `/services/${svc.id}`, { ...serviceInput(svc), description: 'Fixture atualizada.' });
    await admin.call('PUT', `/services/${svc.id}`, old, 409);
  });
  await check('Profissional, disponibilidade e vínculo com serviço', async () => {
    professional = await admin.call('POST', '/professionals', { name: `QA profissional ${tag}`, phone: '61999990000', email: '', registration: '', region: '', bio: '', serviceIds: [svc.id], published: false }, 201);
    await admin.call('PUT', `/professionals/${professional.id}/availability`, { periods: [1, 2, 3, 4, 5, 6, 7].map(dayOfWeek => ({ dayOfWeek, startTime: '08:00', endTime: '18:00' })) });
  });
  await check('Paciente menor exige responsável; cadastro adulto persiste', async () => {
    await admin.call('POST', '/patients', { ...patientBody('QA menor'), birthDate: '2020-01-01' }, 400);
    patient = await admin.call('POST', '/patients', patientBody(`QA paciente ${tag}`), 201);
    secondPatient = await admin.call('POST', '/patients', patientBody(`QA segundo ${tag}`), 201);
    assert.equal((await admin.call('GET', `/patients/${patient.id}`)).name, patient.name);
  });
  await check('Bloqueio impede consulta; remoção libera intervalo', async () => {
    const block = await admin.call('POST', `/professionals/${professional.id}/blocks`, { start, end: `${date}T11:00:00-03:00` }, 201);
    await admin.call('POST', '/appointments', { patientId: patient.id, professionalId: professional.id, serviceId: svc.id, start }, 409);
    await admin.call('DELETE', `/professionals/${professional.id}/blocks/${block.id}`, undefined, 204);
  });
  await check('Duas requisições simultâneas resultam em uma única reserva', async () => {
    const token = await admin.call('GET', '/auth/csrf');
    const responses = await Promise.all([patient.id, secondPatient.id].map(patientId => admin.context.post('/api/v1/appointments', { data: { patientId, professionalId: professional.id, serviceId: svc.id, start }, headers: { [token.headerName]: token.token } })));
    assert.deepEqual(responses.map(r => r.status()).sort(), [201, 409]);
    appointment = await responses.find(r => r.status() === 201).json();
  });
  await check('Vínculos e consultas futuras impedem exclusão/inativação', async () => {
    await admin.call('DELETE', `/professionals/${professional.id}`, undefined, 409);
    const fresh = await admin.call('GET', `/professionals/${professional.id}`);
    await admin.call('PATCH', `/professionals/${professional.id}/active`, { active: false, version: fresh.version }, 409);
    await admin.call('PUT', `/professionals/${professional.id}/availability`, { periods: [] }, 409);
  });
  await check('Reagendamento, histórico e transições válidas', async () => {
    appointment = await admin.call('PATCH', `/appointments/${appointment.id}/reschedule`, { start: `${date}T11:00:00-03:00`, version: appointment.version });
    appointment = await admin.call('PATCH', `/appointments/${appointment.id}/status`, { status: 'CONFIRMADO', version: appointment.version });
    await admin.call('PATCH', `/appointments/${appointment.id}/status`, { status: 'CONCLUIDO', version: appointment.version }, 409);
    appointment = await admin.call('PATCH', `/appointments/${appointment.id}/status`, { status: 'CANCELADO', version: appointment.version });
    const history = await admin.call('GET', `/appointments/${appointment.id}/history`);
    assert.ok(history.length >= 4);
    await admin.call('PATCH', `/appointments/${appointment.id}/status`, { status: 'AGENDADO', version: appointment.version }, 409);
  });
  await check('Usuário RECEPCAO tem acesso restrito no servidor', async () => {
    reception = await admin.call('POST', '/users', { name: `QA recepção ${tag}`, email: `qa-${tag}@example.test`, password: receptionPassword, role: 'RECEPCAO' }, 201);
    const desk = await client();
    await desk.call('POST', '/auth/login', { email: reception.email, password: receptionPassword });
    await desk.call('GET', '/patients');
    await desk.call('GET', '/users', undefined, 403);
    await desk.call('POST', '/services', { name: 'Não permitido', durationMinutes: 30, published: false }, 403);
    await admin.call('PATCH', `/users/${reception.id}/active`, { active: false, version: reception.version });
    await desk.call('GET', '/patients', undefined, 401);
  });
  await check('Cadastros privados não aparecem no site', async () => {
    assert.ok(!(await anon.call('GET', '/public/services')).some(s => s.id === svc.id));
    assert.ok(!(await anon.call('GET', '/public/professionals')).some(p => p.id === professional.id));
  });
  await check('Logout invalida acesso', async () => {
    await admin.call('POST', '/auth/logout', undefined, 204);
    await admin.call('GET', '/patients', undefined, 401);
  });
  report.result = 'PASS';
} catch (error) {
  report.result = 'FAIL'; report.error = error.message;
  console.error(error); process.exitCode = 1;
} finally {
  report.finishedAt = new Date().toISOString();
  await mkdir('output/playwright', { recursive: true });
  await writeFile('output/playwright/api-smoke.json', JSON.stringify(report, null, 2));
  for (const context of contexts) await context.dispose();
}
