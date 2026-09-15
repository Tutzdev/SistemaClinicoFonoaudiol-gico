import { chromium } from 'playwright';
import { PNG } from 'pngjs';
import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
const require = createRequire(import.meta.url);
const baseURL = process.env.APP_BASE_URL || 'http://127.0.0.1:5173';
if (!['localhost', '127.0.0.1'].includes(new URL(baseURL).hostname)) throw new Error('Verificação limitada ao ambiente local.');
const out = 'output/playwright'; await mkdir(out, { recursive: true });
const browser = await chromium.launch({ channel: 'chrome', headless: true });
const context = await browser.newContext({ baseURL, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
const page = await context.newPage();
const report = { startedAt: new Date().toISOString(), states: [], keyboard: [], runtimeErrors: [], limitations: ['Automação e medidas visuais não substituem validação completa com tecnologia assistiva.'] };
page.on('pageerror', e => report.runtimeErrors.push(e.message));

async function scan(name, width) {
  await page.setViewportSize({ width, height: 1000 });
  await page.evaluate(() => document.fonts.ready);
  await page.locator('h1').first().waitFor();
  await page.addScriptTag({ path: require.resolve('axe-core/axe.min.js') });
  const axe = await page.evaluate(async () => window.axe.run(document, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa', 'best-practice'] } }));
  const metrics = await page.evaluate(() => ({
    width: innerWidth, scrollWidth: document.documentElement.scrollWidth,
    lang: document.documentElement.lang, title: document.title,
    controls: [...document.querySelectorAll('a,button,input,select,textarea')].map(el => {
      const r = el.getBoundingClientRect(); const cs = getComputedStyle(el);
      return { name: el.getAttribute('aria-label') || el.innerText || el.getAttribute('name') || el.id, tag: el.tagName, width: r.width, height: r.height, x: r.x, y: r.y, display: cs.display, color: cs.color, background: cs.backgroundColor };
    }).filter(el => el.width > 0 && el.height > 0),
  }));
  const screenshot = `${out}/${name}-${width}.png`;
  await page.screenshot({ path: screenshot, fullPage: true });
  report.states.push({ name, width, url: page.url(), screenshot, metrics, violations: axe.violations.map(v => ({ id: v.id, impact: v.impact, description: v.description, helpUrl: v.helpUrl, nodes: v.nodes.map(n => ({ target: n.target, summary: n.failureSummary })) })), incomplete: axe.incomplete.map(v => ({ id: v.id, targets: v.nodes.map(n => n.target) })) });
  console.log(`${name} ${width}px: ${axe.violations.length} violações, overflow ${metrics.scrollWidth - width}px`);
}
try {
  await page.goto('/');
  await page.getByRole('link', { name: 'Conversar pelo WhatsApp', exact: true }).first().waitFor();
  for (const width of [360, 390, 768, 1440]) await scan('site', width);
  await page.goto('/login');
  await page.getByRole('button', { name: 'Entrar na clínica' }).waitFor();
  for (const width of [360, 390, 768, 1440]) await scan('login', width);
  if (process.env.BOOTSTRAP_ADMIN_PASSWORD && process.env.BOOTSTRAP_ADMIN_EMAIL) {
    await page.getByLabel(/^E-mail/).fill(process.env.BOOTSTRAP_ADMIN_EMAIL);
    await page.getByLabel(/^Senha/).fill(process.env.BOOTSTRAP_ADMIN_PASSWORD);
    await page.getByRole('button', { name: 'Entrar na clínica' }).click();
    await page.waitForURL('**/admin');
    for (const [route, name] of [['', 'visao-geral'], ['/pacientes', 'pacientes'], ['/profissionais', 'profissionais'], ['/servicos', 'servicos'], ['/agenda', 'agenda'], ['/usuarios', 'usuarios'], ['/configuracoes', 'configuracoes']]) {
      await page.goto('/admin' + route);
      await page.locator('h1').first().waitFor();
      for (const width of [360, 390, 768, 1440]) await scan(name, width);
    }
    await page.goto('/admin/pacientes');
    await page.getByRole('button', { name: 'Novo paciente', exact: true }).first().click();
    await page.getByRole('dialog').waitFor();
    for (const width of [360, 1440]) await scan('paciente-dialog', width);
    for (let i = 0; i < 18; i++) {
      await page.keyboard.press('Tab');
      report.keyboard.push(await page.evaluate(() => ({ active: document.activeElement?.outerHTML.slice(0, 300), inDialog: Boolean(document.activeElement?.closest('dialog')), outline: getComputedStyle(document.activeElement).outline })));
    }
    await page.keyboard.press('Escape');
    await page.getByRole('dialog').waitFor({ state: 'hidden' });
    report.focusRestored = await page.getByRole('button', { name: 'Novo paciente', exact: true }).first().evaluate(el => document.activeElement === el);
  } else report.limitations.push('Rotas autenticadas não verificadas: credenciais ausentes.');
  await page.goto('/'); await page.setViewportSize({ width: 320, height: 1000 });
  await scan('site-reflow-400', 320);
  await page.addStyleTag({ content: '* {line-height:1.5 !important;letter-spacing:.12em !important;word-spacing:.16em !important} p{margin-bottom:2em !important}' });
  await scan('site-text-spacing', 320);
  await page.goto('/'); await page.setViewportSize({ width: 1440, height: 1000 });
  await page.keyboard.press('Tab');
  const focus = await page.evaluate(() => { const el = document.activeElement; const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return { tag: el.tagName, text: el.textContent, rect: { x: r.x, y: r.y, width: r.width, height: r.height }, outlineColor: s.outlineColor, outlineStyle: s.outlineStyle, outlineWidth: s.outlineWidth, background: s.backgroundColor, color: s.color }; });
  const png = PNG.sync.read(await page.screenshot({ path: `${out}/focus.png` }));
  const colors = new Map();
  const { rect } = focus;
  for (let y = Math.max(0, Math.floor(rect.y - 8)); y < Math.min(png.height, Math.ceil(rect.y + rect.height + 8)); y++) {
    for (let x = Math.max(0, Math.floor(rect.x - 8)); x < Math.min(png.width, Math.ceil(rect.x + rect.width + 8)); x++) {
      const i = (y * png.width + x) * 4; const rgb = Array.from(png.data.subarray(i, i + 3)).join(',');
      colors.set(rgb, (colors.get(rgb) || 0) + 1);
    }
  }
  const luminance = rgb => rgb.map(v => { const s = v / 255; return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4; }).reduce((sum, value, i) => sum + value * [0.2126, 0.7152, 0.0722][i], 0);
  const contrast = (a, b) => { const l1 = luminance(a); const l2 = luminance(b); return Number(((Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05)).toFixed(2)); };
  const ring = focus.outlineColor.match(/\d+/g)?.slice(0, 3).map(Number);
  const surroundings = [247, 250, 249];
  report.focusMeasurement = { ...focus, screenshot: `${out}/focus.png`, pixelColors: [...colors.entries()].sort((a, b) => b[1] - a[1]).slice(0, 12), ringPixelCount: ring ? colors.get(ring.join(',')) || 0 : 0, outlineAgainstPageContrast: ring ? contrast(ring, surroundings) : null, whiteTextAgainstDarkSurfaceContrast: contrast([255, 255, 255], [23, 59, 59]) };
  await page.emulateMedia({ forcedColors: 'active', reducedMotion: 'reduce' });
  await scan('site-forced-colors', 390);
  report.result = report.states.some(s => s.violations.length || s.metrics.scrollWidth > s.width) || report.runtimeErrors.length || report.keyboard.some(k => !k.inDialog) || report.focusRestored === false ? 'FAIL' : 'AUTOMATED_CHECKS_PASS';
  if (report.result === 'FAIL') process.exitCode = 1;
} catch (error) { report.result = 'FAIL'; report.error = error.message; console.error(error); process.exitCode = 1; }
finally { report.finishedAt = new Date().toISOString(); await writeFile(`${out}/visual.json`, JSON.stringify(report, null, 2)); await browser.close(); }
