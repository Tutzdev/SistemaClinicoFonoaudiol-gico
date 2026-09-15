import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import { chromium } from 'playwright';

const baseURL = process.env.APP_BASE_URL || 'http://127.0.0.1:5173';
if (!['localhost', '127.0.0.1'].includes(new URL(baseURL).hostname)) throw new Error('Use apenas a demonstração local.');
assert.ok(process.env.BOOTSTRAP_ADMIN_PASSWORD && process.env.BOOTSTRAP_ADMIN_EMAIL, 'Carregue scripts/use-local-env.ps1.');
const out = 'output/playwright'; await mkdir(out, { recursive: true });
const browser = await chromium.launch({ channel: 'chrome', headless: true });
const context = await browser.newContext({ baseURL, viewport: { width: 1440, height: 1000 }, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
const page = await context.newPage(); page.setDefaultTimeout(15000);
const tag = Date.now().toString(36);
const names = { service: `DEMO E2E serviço ${tag}`, person: `DEMO E2E profissional ${tag}`, patient: `DEMO E2E paciente ${tag}`, desk: `DEMO E2E recepção ${tag}` };
const deskEmail = `e2e-${tag}@example.test`; const deskPassword = randomBytes(24).toString('base64url');
const report = { startedAt: new Date().toISOString(), baseURL, checks: [], runtimeErrors: [] };
page.on('pageerror', error => report.runtimeErrors.push(error.message));
async function check(name, fn) { await fn(); report.checks.push({ name, result: 'PASS' }); console.log('PASS ' + name); }
async function expectResponse(action, method, path, status) {
  const pending = page.waitForResponse(r => r.request().method() === method && new URL(r.url()).pathname === `/api/v1${path}`);
  await action(); const response = await pending;
  if (response.status() !== status) assert.equal(response.status(), status, `${method} ${path}: ${await response.text().catch(() => 'Corpo indisponível')}`);
  return status === 204 ? undefined : response.json();
}
async function login(email, password) {
  await page.goto('/login');
  await page.getByLabel(/^E-mail/).fill(email);
  await page.getByLabel(/^Senha/).fill(password);
  await page.getByRole('button', { name: 'Entrar na clínica' }).click();
  await page.waitForURL('**/admin');
}
async function search(route, name) {
  await page.goto(route);
  await page.getByRole('textbox', { name: /Buscar por nome/ }).fill(name);
  await page.getByRole('button', { name: 'Buscar', exact: true }).click();
  await page.getByRole('cell', { name, exact: false }).first().waitFor();
}
let service, professional, patient;
try {
  await check('Entrar como ADMIN pelo formulário', () => login(process.env.BOOTSTRAP_ADMIN_EMAIL, process.env.BOOTSTRAP_ADMIN_PASSWORD));
  await check('Criar serviço na interface e persistir pela API', async () => {
    await page.goto('/admin/servicos');
    await page.getByRole('button', { name: 'Novo serviço', exact: true }).first().click();
    await page.getByLabel('Nome do serviço').fill(names.service);
    await page.getByLabel('Duração em minutos').fill('45');
    await page.getByLabel('Descrição do serviço').fill('Demonstração fictícia para teste integrado. Não publicar.');
    service = await expectResponse(() => page.getByRole('button', { name: 'Salvar serviço', exact: true }).click(), 'POST', '/services', 201);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
  });
  await check('Criar profissional e vincular serviço', async () => {
    await page.goto('/admin/profissionais');
    await page.getByRole('button', { name: 'Novo profissional', exact: true }).first().click();
    await page.getByLabel('Nome completo').fill(names.person);
    await page.getByLabel('Telefone (opcional)', { exact: true }).fill('61999990000');
    await page.getByRole('checkbox', { name: names.service, exact: true }).check();
    professional = await expectResponse(() => page.getByRole('button', { name: 'Salvar profissional', exact: true }).click(), 'POST', '/professionals', 201);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    await search('/admin/profissionais', names.person);
  });
  await check('Configurar disponibilidade pelo diálogo', async () => {
    await page.getByRole('button', { name: `Disponibilidade de ${names.person}`, exact: true }).click();
    for (let i = 0; i < 7; i++) {
      await page.getByRole('button', { name: 'Adicionar período', exact: true }).click();
      await page.getByLabel('Dia da semana', { exact: true }).nth(i).selectOption(String(i + 1));
      await page.getByLabel(/^Início(?: \*)?$/).nth(i).fill('08:00');
      await page.getByLabel(/^Fim(?: \*)?$/).nth(i).fill('18:00');
    }
    await expectResponse(() => page.getByRole('button', { name: 'Salvar disponibilidade', exact: true }).click(), 'PUT', `/professionals/${professional.id}/availability`, 200);
    await page.keyboard.press('Escape');
  });
  await check('Cadastrar paciente e conferir após recarregar', async () => {
    await page.goto('/admin/pacientes');
    await page.getByRole('button', { name: 'Novo paciente', exact: true }).first().click();
    await page.getByLabel('Nome completo').fill(names.patient);
    await page.getByLabel('Data de nascimento').fill('1995-05-15');
    await page.getByLabel('Telefone de contato').fill('61999990000');
    patient = await expectResponse(() => page.getByRole('button', { name: 'Salvar paciente', exact: true }).click(), 'POST', '/patients', 201);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    await search('/admin/pacientes', names.patient);
    await page.reload();
    assert.ok((await (await page.request.get(`/api/v1/patients/${patient.id}`)).json()).name === names.patient);
    await page.getByRole('button', { name: `Editar ${names.patient}`, exact: true }).waitFor();
  });
  // O fluxo de agenda é exercitado abaixo, com datas relativas e todas as operações pela UI.
  await runAgenda();
  await check('Editar, inativar e reativar paciente preserva cadastro', async () => {
    await search('/admin/pacientes', names.patient);
    await page.getByRole('button', { name: `Editar ${names.patient}`, exact: true }).click();
    await page.getByLabel('E-mail (opcional)').fill(`patient-${tag}@example.test`);
    patient = await expectResponse(() => page.getByRole('button', { name: 'Salvar paciente', exact: true }).click(), 'PUT', `/patients/${patient.id}`, 200);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    const row = page.getByRole('row').filter({ has: page.getByText(names.patient, { exact: true }) });
    await row.getByRole('button', { name: 'Inativar', exact: true }).click();
    patient = await expectResponse(() => page.getByRole('button', { name: 'Inativar cadastro', exact: true }).click(), 'PATCH', `/patients/${patient.id}/active`, 200);
    assert.equal(patient.active, false);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    await row.getByRole('button', { name: 'Reativar', exact: true }).click();
    patient = await expectResponse(() => page.getByRole('button', { name: 'Reativar cadastro', exact: true }).click(), 'PATCH', `/patients/${patient.id}/active`, 200);
    assert.equal(patient.active, true);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
  });
  await check('Configurações consomem a API e salvam sem publicar endereço', async () => {
    await page.goto('/admin/configuracoes');
    const before = await (await page.request.get('/api/v1/clinic-settings')).json();
    await page.getByLabel('Nome de apresentação').waitFor();
    const after = await expectResponse(() => page.getByRole('button', { name: /Salvar/ }).click(), 'PUT', '/clinic-settings', 200);
    assert.equal(after.displayName, before.displayName);
    assert.equal(after.addressConfirmed, false);
    await page.reload();
    await page.getByLabel('Nome de apresentação').waitFor();
    assert.equal(await page.getByLabel('Nome de apresentação').inputValue(), before.displayName);
  });
  await check('Criar conta de recepção na interface', async () => {
    await page.goto('/admin/usuarios');
    await page.getByRole('button', { name: 'Novo usuário', exact: true }).first().click();
    await page.getByLabel('Nome completo').fill(names.desk);
    await page.getByLabel('E-mail de acesso').fill(deskEmail);
    await page.getByLabel('Permissão').selectOption('RECEPCAO');
    await page.getByLabel('Senha inicial').fill(deskPassword);
    await expectResponse(() => page.getByRole('button', { name: 'Salvar usuário', exact: true }).click(), 'POST', '/users', 201);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
  });
  await check('Recepção: interface restrita e bloqueio HTTP 403 no servidor', async () => {
    await page.getByRole('button', { name: 'Sair', exact: true }).click();
    await page.waitForURL('**/login');
    await login(deskEmail, deskPassword);
    assert.equal(await page.getByRole('link', { name: 'Equipe e acessos', exact: true }).count(), 0);
    assert.equal((await page.request.get('/api/v1/users')).status(), 403);
    assert.equal((await page.request.get('/api/v1/clinic-settings')).status(), 403);
    assert.equal((await page.request.get('/api/v1/patients')).status(), 200);
    const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
    const response = await page.request.post('/api/v1/services', { headers: { [csrf.headerName]: csrf.token }, data: { name: 'Acesso negado', durationMinutes: 30, published: false } });
    assert.equal(response.status(), 403);
    await page.goto('/admin/usuarios');
    await page.getByRole('heading', { name: 'Acesso restrito' }).waitFor();
    await page.screenshot({ path: `${out}/reception-restriction.png`, fullPage: true });
  });
  await check('Trocar senha exige a atual e encerra sessão', async () => {
    const changedPassword = randomBytes(24).toString('base64url');
    await page.getByRole('button', { name: /Conta de.*Alterar senha/ }).click();
    await page.getByLabel(/^Senha atual/).fill(deskPassword);
    await page.getByLabel(/^Nova senha/).fill(changedPassword);
    await page.getByLabel(/^Confirmar nova senha/).fill(changedPassword);
    await expectResponse(() => page.getByRole('button', { name: 'Salvar senha', exact: true }).click(), 'PUT', '/auth/password', 204);
    await page.getByRole('button', { name: 'Voltar ao login', exact: true }).click();
    await page.waitForURL('**/login');
    assert.equal((await page.request.get('/api/v1/patients')).status(), 401);
    await login(deskEmail, changedPassword);
    await page.getByRole('button', { name: 'Sair', exact: true }).click();
    await page.waitForURL('**/login');
    assert.equal((await page.request.get('/api/v1/patients')).status(), 401);
  });
  assert.deepEqual(report.runtimeErrors, []);
  report.result = 'PASS';
} catch (error) {
  report.result = 'FAIL'; report.error = error.message; console.error(error); process.exitCode = 1;
  await page.screenshot({ path: `${out}/e2e-failure.png`, fullPage: true }).catch(() => {});
} finally {
  report.finishedAt = new Date().toISOString();
  await writeFile(`${out}/e2e.json`, JSON.stringify(report, null, 2));
  await browser.close();
}

async function runAgenda() {
  const day = new Date(Date.now() + 8 * 86400000).toISOString().slice(0, 10);
  let appointment;
  async function fillNew() {
    await page.getByRole('button', { name: 'Novo agendamento', exact: true }).first().click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel(/^Paciente/).selectOption(patient.id);
    await dialog.getByLabel(/^Profissional/).selectOption(professional.id);
    await dialog.getByLabel(/^Serviço/).selectOption(service.id);
    await dialog.getByLabel(/^Data e horário/).fill(`${day}T10:00`);
  }
  await check('Criar consulta pela interface', async () => {
    await page.goto('/admin/agenda');
    await page.getByLabel('Data da agenda', { exact: true }).fill(day);
    await fillNew();
    appointment = await expectResponse(() => page.getByRole('button', { name: 'Criar agendamento', exact: true }).click(), 'POST', '/appointments', 201);
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    await page.getByRole('button', { name: names.patient, exact: true }).waitFor();
  });
  await check('Consulta persiste após recarregar a agenda', async () => {
    await page.reload();
    await page.getByLabel('Data da agenda', { exact: true }).fill(day);
    await page.getByRole('button', { name: names.patient, exact: true }).waitFor();
    await page.screenshot({ path: `${out}/agenda-persisted.png`, fullPage: true });
  });
  await check('Consulta conflitante retorna 409 e erro próximo do formulário', async () => {
    await fillNew();
    await expectResponse(() => page.getByRole('button', { name: 'Criar agendamento', exact: true }).click(), 'POST', '/appointments', 409);
    await page.getByRole('dialog').getByRole('alert').waitFor();
    await page.screenshot({ path: `${out}/appointment-conflict.png`, fullPage: true });
    await page.keyboard.press('Escape');
  });
  await check('Confirmar, reagendar e consultar histórico sem perder versão', async () => {
    await page.getByRole('button', { name: names.patient, exact: true }).click();
    await page.getByRole('button', { name: 'Confirmar consulta', exact: true }).click();
    appointment = await expectResponse(() => page.getByRole('button', { name: 'Confirmar alteração', exact: true }).click(), 'PATCH', `/appointments/${appointment.id}/status`, 200);
    assert.equal(appointment.status, 'CONFIRMADO');
    await page.getByRole('button', { name: 'Reagendar', exact: true }).click();
    await page.getByLabel(/^Nova data e horário/).fill(`${day}T11:00`);
    appointment = await expectResponse(() => page.getByRole('button', { name: 'Salvar novo horário', exact: true }).click(), 'PATCH', `/appointments/${appointment.id}/reschedule`, 200);
    assert.equal(appointment.status, 'AGENDADO');
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    await page.getByRole('button', { name: names.patient, exact: true }).click();
    await page.getByText('Consulta reagendada', { exact: true }).waitFor();
    await page.screenshot({ path: `${out}/appointment-history.png`, fullPage: true });
  });
  await check('Cancelar preserva o histórico e não permite reativação', async () => {
    await page.getByRole('button', { name: 'Cancelar consulta', exact: true }).click();
    appointment = await expectResponse(() => page.getByRole('button', { name: 'Confirmar alteração', exact: true }).click(), 'PATCH', `/appointments/${appointment.id}/status`, 200);
    assert.equal(appointment.status, 'CANCELADO');
    assert.equal(await page.getByRole('button', { name: 'Reagendar', exact: true }).count(), 0);
    const history = await (await page.request.get(`/api/v1/appointments/${appointment.id}/history`)).json();
    assert.ok(history.length >= 4);
    await page.keyboard.press('Escape');
  });
}
