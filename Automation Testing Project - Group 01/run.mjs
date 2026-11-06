import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import assert from 'node:assert/strict';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.dirname(here);
const args = process.argv.slice(2);
const member = args.find(a => a.startsWith('--member='))?.split('=')[1];
const retest = args.includes('--retest-pim');
assert(args.every(a => ['--check', '--retest-pim'].includes(a) || /^--member=\d{8}$/.test(a)), 'Unknown argument');
assert(!(retest && member), 'Select either a member or the targeted PIM retest');
const read = file => fs.readFile(path.join(repo, file), 'utf8');
const pim = JSON.parse(await read('Automation Testing Project - Group 01/pim-cases.json'));
const suites = [
  { key: 'pim-emp', member: '23130226', tool: 'Playwright', source: 'automation/23130226/Playwright/run-pim.mjs', cases: pim.filter(c => c.module === 'PIM_EMP') },
  { key: 'pim-details', member: '23130226', tool: 'Playwright', source: 'automation/23130226/Playwright/run-pim-details.mjs', cases: pim.filter(c => c.module !== 'PIM_EMP') },
  { key: 'leave', member: '22130254', tool: 'Cypress', source: 'automation/22130254/cypress/e2e/leave/22130254_CaoTienThanh_ASM7.cy.js' },
  { key: 'admin', member: '23130171', tool: 'Playwright', source: 'automation/23130171/Playwright/23130171_NguyenThiThuyLinh_ASM7.mjs', skip: 'Chưa chạy: selector sửa/xóa .first()/.last() chưa giới hạn node do suite tạo; thiếu assertion ở một số case. Cần tác giả sửa fixture/cleanup trước khi chạy trên demo dùng chung.' },
  { key: 'time', member: '23130339', tool: 'Puppeteer', source: 'automation/23130339/KatalonRecorder/23130339_TonBichTram_ASM7.js', skip: 'Chưa chạy: dùng attendance/timesheet Admin dùng chung, catch bỏ qua lỗi, chưa có kết quả theo Case ID và exit code lỗi. Cần nhân viên test riêng, assertion và cleanup được đối chiếu.' }
];
for (const s of suites) {
  const text = await read(s.source);
  if (s.key === 'leave' || s.key === 'admin') {
    s.cases = [...text.matchAll(s.key === 'leave' ? /\bit\('([^']+)'/g : /\btest\('([^']+)'/g)].map(m => ({
      id: m[1].split(/\s+[–-]\s+|:/)[0], title: m[1], expected: 'Assertion trong script gốc', module: s.key
    }));
  }
  if (s.key === 'time') s.cases = ['TIME_ATTEND_PunchIn_01', 'TIME_ATTEND_PunchOut_01', 'TIME_MY_Edit_01', 'TIME_MY_Submit_01', 'TIME_EMP_Search_01', 'TIME_EMP_Approve_01'].map(id => ({ id, module: id.split('_').slice(0, 2).join('_'), title: id, expected: 'Phạm vi trong ASM07.md; script chưa tách kết quả theo ID' }));
  if (s.key === 'leave') {
    const design = JSON.parse(await read('Automation Testing Project - Group 01/leave-cases.json'));
    assert.deepEqual(design.map(c => c.id), s.cases.map(c => c.id), 'Leave script/design IDs must agree');
    s.cases = design;
  }
  s.sha256 = createHash('sha256').update(text).digest('hex');
}
for (const module of ['VAC', 'CAND', 'FLOW']) {
  const source = `automation/23130299/KatalonStudio/Test Suites/RECRUIT_${module}_25_ASC_001_TO_025.ts`;
  const text = await read(source);
  suites.push({ key: `recruit-${module.toLowerCase()}`, member: '23130299', tool: 'Katalon Studio', source,
    sha256: createHash('sha256').update(text).digest('hex'),
    skip: 'Bỏ qua theo yêu cầu nhóm trưởng ngày 31/10/2026. Project thiếu Profiles/default.glbl và maintenance/preflight58-plan.json; chưa có Katalon Runtime Engine trên máy chạy.',
    cases: [...text.matchAll(/<testCaseId>([^<]+)<\/testCaseId>/g)].map(m => ({ id: m[1].split('/').at(-1), title: m[1].split('/').at(-1), module: `RECRUIT_${module}`, expected: 'Assertion trong script Katalon gốc' })) });
}
const all = suites.flatMap(s => s.cases.map(c => ({ ...c, member: s.member, suite: s.key })));
assert.equal(all.length, 187, 'Review inventory when member scripts change');
assert.equal(new Set(all.map(c => c.id)).size, all.length, 'Duplicate Case ID');
assert.equal(pim.length, 75);
const totals = rows => Object.fromEntries(['Pass', 'Fail', 'Skipped'].map(s => [s, rows.filter(r => r.status === s).length]));
assert.deepEqual(totals([{ status: 'Pass' }, { status: 'Fail' }, { status: 'Skipped' }]), { Pass: 1, Fail: 1, Skipped: 1 });
if (member) assert(suites.some(s => s.member === member), 'Unknown member');
if (args.includes('--check')) {
  for (const s of suites) console.log(`${s.member} ${s.key}: ${s.cases.length} ${s.skip ? 'SKIP' : 'RUN'}`);
  console.log(`Inventory OK: ${all.length} IDs; 5 members; 13 functions represented. ADM_JOB/ADM_USER have no script in dev.`);
  process.exit(0);
}
const runId = `ASM8${retest ? '_RETEST' : ''}_${new Date().toISOString().replace(/[:.]/g, '-')}`;
const evidence = path.join(repo, 'Envidence - Group01', runId);
const reportDir = path.join(repo, 'Test Report - Group 01', runId);
await fs.mkdir(evidence, { recursive: true });
await fs.mkdir(reportDir, { recursive: true });
const selected = retest ? suites.filter(s => s.key === 'pim-emp').map(s => ({ ...s, cases: s.cases.filter(c => ['PIM_EMP_Search_02', 'PIM_EMP_Sort_01'].includes(c.id)) })) : suites.filter(s => !member || s.member === member);
const report = { runId, startedAt: new Date().toISOString(), finishedAt: null, durationMs: null,
  commit: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: repo, encoding: 'utf8' }).trim(),
  baseUrl: 'https://opensource-demo.orangehrmlive.com/', scope: retest ? 'pim-retest' : member || 'group',
  results: selected.flatMap(s => s.cases.map(c => ({ ...c, suite: s.key, member: s.member, status: 'Skipped', reason: s.skip || 'Chưa tới bước chạy', originalStatus: 'NotRun' }))),
  suites: selected.map(({ cases, ...s }) => ({ ...s, planned: cases.length })), issues: [] };
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
assert.equal(esc('<a>"'), '&lt;a&gt;&quot;');
const href = absolute => path.relative(reportDir, absolute).split(path.sep).map(encodeURIComponent).join('/');
async function save() {
  report.summary = totals(report.results);
  report.attempted = report.summary.Pass + report.summary.Fail;
  report.selected = report.results.length;
  await fs.writeFile(path.join(reportDir, 'results.json'), JSON.stringify(report, null, 2));
  const summaryRows = selected.map(s => { const t = totals(report.results.filter(r => r.suite === s.key)); return `<tr><td>${esc(s.member)}</td><td>${esc(s.key)}</td><td>${t.Pass}</td><td>${t.Fail}</td><td>${t.Skipped}</td></tr>`; }).join('');
  const rows = report.results.map(r => `<tr><td>${esc(r.id)}</td><td class="${r.status}">${r.status}</td><td>${esc(r.expected)}</td><td>${esc(r.actual || r.reason)}</td><td>${r.durationMs == null ? '' : (r.durationMs / 1000).toFixed(2)}</td><td>${(r.evidence || []).map(f => `<a href="${href(f)}">${esc(path.basename(f))}</a>`).join('<br>')}</td></tr>`).join('');
  await fs.writeFile(path.join(reportDir, 'index.html'), `<!doctype html><html lang="vi"><meta charset="utf-8"><title>Test Report - Group 01</title><style>body{font:16px Arial;margin:32px;color:#182637}table{border-collapse:collapse;width:100%;margin:24px 0}th,td{border:1px solid #cbd5e1;padding:10px;text-align:left;vertical-align:top}th{background:#eaf1f7}.Pass{color:#087143}.Fail{color:#ba2525}.Skipped{color:#865900}td:nth-child(1){overflow-wrap:anywhere}a{color:#075da5}small{color:#485b70}</style><h1>Test Report - Group 01</h1><p>${esc(runId)}<br>Online: ${esc(report.baseUrl)}<br>Bắt đầu: ${esc(report.startedAt)}; kết thúc: ${esc(report.finishedAt || 'Đang chạy')}<br>Thời gian toàn lượt: ${report.durationMs == null ? 'Đang chạy' : (report.durationMs / 1000).toFixed(2) + ' giây'}</p><p><strong>${report.attempted} đã thực thi / ${report.selected} case trong inventory: ${report.summary.Pass} Pass, ${report.summary.Fail} Fail, ${report.summary.Skipped} Skipped.</strong></p><p>Skipped là chưa thực thi, không tính vào pass rate. Blocked từ runner gốc được ghi Fail với nguyên nhân hạ tầng/fixture. Đây là inventory script hiện có, không phải toàn bộ case thiết kế trong workbook. PIM dùng suite 75 case đầy đủ, không chạy trùng 15 case đại diện ASM7.</p><h2>Theo suite</h2><table><tr><th>MSSV</th><th>Suite</th><th>Pass</th><th>Fail</th><th>Skipped</th></tr>${summaryRows}</table><h2>Vấn đề và cleanup</h2><ul>${report.issues.map(i => `<li>${esc(i)}</li>`).join('')}</ul><p>Admin: cần giới hạn node test, assertion và cleanup. Time: cần nhân viên riêng và report theo case. Recruitment: bỏ qua theo yêu cầu nhóm trưởng; thiếu tài nguyên project/Katalon. ADM_JOB và ADM_USER chưa có script. Chưa đủ điều kiện kết luận toàn bộ nhóm chạy thành công.</p><h2>Chi tiết</h2><table><tr><th>Case ID</th><th>Kết quả</th><th>Expected</th><th>Actual / lý do</th><th>Giây</th><th>Minh chứng</th></tr>${rows}</table><p><a href="results.json">JSON</a> · <a href="${href(path.join(evidence, 'sources.json'))}">Commit và SHA-256 nguồn chạy</a></p><small>Giữ nguyên log lần đầu; retest tạo Run ID riêng. Lượt chưa có retest không được kết luận flaky.</small></html>`);
}
await fs.writeFile(path.join(evidence, 'sources.json'), JSON.stringify({ commit: report.commit, runId, sources: report.suites }, null, 2));
await save();
const started = Date.now();
async function command(source, env, dir) {
  await fs.mkdir(dir, { recursive: true });
  const logFile = path.join(dir, 'console.log');
  const log = await fs.open(logFile, 'w');
  const child = spawn(process.execPath, [path.join(repo, source)], { cwd: repo, env: { ...process.env, ...env }, stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true });
  for (const stream of [child.stdout, child.stderr]) stream.on('data', chunk => { log.write(chunk); process.stdout.write(chunk); });
  const exit = await new Promise((resolve, reject) => { child.once('error', reject); child.once('close', resolve); });
  await log.close();
  return { exit, logFile };
}
async function find(file, dir) {
  const out = [];
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...await find(file, full));
    else if (entry.name === file) out.push(full);
  }
  return out;
}
for (const s of selected) {
  if (s.skip) { console.log(`SKIP ${s.key}: ${s.skip}`); continue; }
  const dir = path.join(evidence, s.key);
  const entry = report.suites.find(x => x.key === s.key);
  entry.startedAt = new Date().toISOString();
  try {
    if (s.key.startsWith('pim')) {
      const env = s.key === 'pim-emp' ? { PIM_REPO: dir, PIM_RUN_ID: runId, ...(retest ? { PIM_ONLY: '21,25' } : {}) } : { PIM_DETAILS_OUTPUT: dir };
      // Runs must not inherit an earlier filtered or cleanup-only invocation.
      for (const name of ['PIM_ONLY', 'PIM_CLEANUP_PREVIOUS', 'PIM_DETAILS_MODULE', 'PIM_DETAILS_CLEANUP']) delete process.env[name];
      const outcome = await command(s.source, env, dir);
      entry.exitCode = outcome.exit;
      const files = await find('results.json', dir);
      assert.equal(files.length, 1, 'Exactly one new source report is required');
      const rawFile = files[0];
      const raw = JSON.parse(await fs.readFile(rawFile, 'utf8'));
      entry.rawReport = path.relative(repo, rawFile).split(path.sep).join('/');
      entry.browser = raw.browser;
      entry.cleanup = raw.cleanup;
      for (const issue of raw.setupErrors || []) report.issues.push(`${s.key} setup: ${JSON.stringify(issue)}`);
      if (raw.fatal) report.issues.push(`${s.key} fatal: ${JSON.stringify(raw.fatal)}`);
      for (const original of raw.results || []) {
        const row = report.results.find(r => r.suite === s.key && r.id === original.id);
        assert(row, `Unknown source result ${original.id}`);
        Object.assign(row, { originalStatus: original.status, status: original.status === 'Passed' ? 'Pass' : ['Untested', 'NotRun'].includes(original.status) ? 'Skipped' : 'Fail',
          actual: original.actual || original.error?.message || JSON.stringify(original.assertions || []), durationMs: original.durationMs,
          evidence: [rawFile, outcome.logFile, ...(original.screenshots || []).map(f => path.resolve(path.dirname(rawFile), f))] });
      }
      const dirty = (raw.cleanup || []).filter(c => !/Deleted|absent/i.test(c.status || c.result || ''));
      if (dirty.length) report.issues.push(`${s.key} cleanup cần kiểm tra: ${JSON.stringify(dirty)}`);
      assert.equal(raw.results.length, s.cases.length, 'Source runner did not produce all scheduled cases');
    } else if (s.key === 'leave') {
      delete process.env.ELECTRON_RUN_AS_NODE;
      const { default: cypress } = await import('cypress');
      await fs.mkdir(dir, { recursive: true });
      const raw = await cypress.run({ project: repo, configFile: path.join(here, 'cypress.config.cjs'), browser: 'edge', headless: true,
        config: { screenshotsFolder: path.join(dir, 'screenshots'), videosFolder: path.join(dir, 'videos') },
        env: { runDataFile: path.join(dir, 'run-data.json') } });
      const rawFile = path.join(dir, 'cypress-results.json');
      await fs.writeFile(rawFile, JSON.stringify(raw, null, 2));
      entry.rawReport = path.relative(repo, rawFile).split(path.sep).join('/');
      entry.browser = raw.browserVersion;
      if (raw.status === 'failed') throw Error(raw.message || 'Cypress could not start');
      for (const run of raw.runs || []) for (const t of run.tests || []) {
        const id = t.title.join(' ').match(/TC_LEAVE_[A-Z]+_\d{3}/)?.[0];
        const row = report.results.find(r => r.suite === s.key && r.id === id);
        assert(row, `Unknown Cypress test ${t.title}`);
        const screenshots = (run.screenshots || []).filter(shot => shot.testId === t.testId || path.basename(shot.path).includes(id));
        Object.assign(row, { status: t.state === 'passed' ? 'Pass' : t.state === 'failed' ? 'Fail' : 'Skipped', originalStatus: t.state,
          actual: t.displayError || (t.state === 'passed' ? 'Toàn bộ assertion Cypress trong case hoàn tất.' : 'Hook hoặc case phụ thuộc chưa hoàn tất.'),
          durationMs: t.attempts?.reduce((n, a) => n + (a.wallClockDuration || 0), 0), evidence: [rawFile, ...screenshots.map(x => x.path)] });
      }
      const ledger = JSON.parse(await fs.readFile(path.join(dir, 'run-data.json'), 'utf8'));
      entry.cleanup = ledger.cleanupCompleted;
      if (!ledger.cleanupCompleted) report.issues.push('Leave cleanup chưa hoàn tất; xem run-data.json trước khi chạy lại.');
    } else {
      throw Error(`Chưa có adapter thực thi cho ${s.key}; giữ Skipped đến khi bổ sung runner/report phù hợp.`);
    }
  } catch (error) {
    entry.error = error.stack;
    report.issues.push(`${s.key}: ${error.message}`);
    for (const row of report.results.filter(r => r.suite === s.key && r.originalStatus === 'NotRun')) row.reason = `Không chạy được: ${error.message}`;
    console.error(entry.error);
  }
  entry.finishedAt = new Date().toISOString();
  await save();
}
report.finishedAt = new Date().toISOString();
report.durationMs = Date.now() - started;
await save();
console.log(`REPORT ${path.join(reportDir, 'index.html')}`);
console.log(JSON.stringify({ ...report.summary, attempted: report.attempted, selected: report.selected, durationMs: report.durationMs }));
process.exitCode = report.summary.Fail || report.summary.Skipped || report.issues.length ? 1 : 0;
