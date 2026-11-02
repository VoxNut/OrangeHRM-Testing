import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.dirname(here);
const [firstFile, ...retestFiles] = process.argv.slice(2);
assert(firstFile && retestFiles.length, 'Usage: node summarize.mjs <group-results.json> <retest-results.json> [...]');
const first = JSON.parse(await fs.readFile(firstFile, 'utf8'));
const retests = await Promise.all(retestFiles.map(async f => JSON.parse(await fs.readFile(f, 'utf8'))));
const retest = retests.at(-1);
assert(first.scope === 'group' && first.finishedAt && first.results.length === 187);
let previous = first;
for (const current of retests) {
  assert(current.scope === 'pim-retest' && current.finishedAt && current.results.length === 2);
  assert(Date.parse(current.startedAt) >= Date.parse(previous.finishedAt), 'Runs must be sequential');
  assert.deepEqual(current.results.map(r => r.id).sort(), ['PIM_EMP_Search_02', 'PIM_EMP_Sort_01']);
  previous = current;
}
function merge(original, later) {
  const map = new Map(original.map(r => [r.id, r]));
  assert.equal(map.size, original.length, 'Duplicate source IDs');
  for (const row of later) { assert(map.has(row.id), `Unknown retest ${row.id}`); map.set(row.id, row); }
  return [...map.values()];
}
assert.deepEqual(merge([{ id: 'a', status: 'Fail' }, { id: 'b', status: 'Skipped' }], [{ id: 'a', status: 'Pass' }]).map(r => r.status), ['Pass', 'Skipped']);
const tag = report => report.results.map(r => ({ ...r, evidenceRun: report.runId, sourceSha256: report.suites.find(s => s.key === r.suite).sha256 }));
let results = tag(first);
for (const current of retests) results = merge(results, tag(current));
const leave = JSON.parse(await fs.readFile(path.join(here, 'leave-cases.json'), 'utf8'));
for (const row of results) {
  const design = leave.find(c => c.id === row.id);
  if (design) Object.assign(row, design);
  if (row.status !== 'Skipped') delete row.reason;
}
const totals = rows => Object.fromEntries(['Pass', 'Fail', 'Skipped'].map(s => [s, rows.filter(r => r.status === s).length]));
const summary = totals(results);
assert.equal(summary.Pass + summary.Fail + summary.Skipped, 187);
const runId = `ASM8_SUMMARY_${retest.runId.replace(/^ASM8_RETEST_/, '')}`;
const dir = path.join(repo, 'Test Report - Group 01', runId);
await fs.mkdir(dir);
const report = { runId, type: 'latest-per-case', scope: 'group-summary', baseUrl: first.baseUrl,
  startedAt: first.startedAt, finishedAt: retest.finishedAt,
  durationMs: [first, ...retests].reduce((n, r) => n + r.durationMs, 0),
  elapsedIncludingGapMs: Date.parse(retest.finishedAt) - Date.parse(first.startedAt),
  selected: 187, attempted: summary.Pass + summary.Fail, attemptedIncludingRetest: [first, ...retests].reduce((n, r) => n + r.attempted, 0),
  summary, results, phases: [first, ...retests].map((r, i) => ({ runId: r.runId, startedAt: r.startedAt, finishedAt: r.finishedAt, durationMs: r.durationMs, attempted: r.attempted, summary: r.summary, report: path.resolve([firstFile, ...retestFiles][i]) })),
  suites: first.suites.map(s => ({ key: s.key, member: s.member, tool: s.tool, planned: s.planned, latest: totals(results.filter(r => r.suite === s.key)), skip: s.skip })),
  issues: [first, ...retests].flatMap(r => r.issues),
  diagnosis: [
    'Search_02: tiền tố G01N01 quá rộng, fixture không nằm trong gợi ý; sửa truy vấn thành NhutPerson.',
    'Sort_01: localeCompare JavaScript khác thứ tự server cho tên có số/ký tự đặc biệt; đối chiếu UI/response và hai fixture ASCII Alpha/Zulu ở hai chiều. Chưa kiểm chứng collation đa ngôn ngữ.',
    'Retest đầu: Sort_01 còn lỗi so sánh khoảng trắng HTML (Lucia  QA hiển thị Lucia QA); chuẩn hóa khoảng trắng tên trong bảng rồi retest tiếp.',
    'Retest thứ hai: page 2 có Employee ID null trong API nhưng ô UI rỗng; sửa hàm chờ bảng để so sánh cùng dạng ID hiển thị rồi retest tiếp.',
    'Hai case được sửa script rồi retest; chưa đủ cơ sở gọi đây là flaky test.',
    'Admin/Time chưa đủ fixture, assertion hoặc cleanup để chạy chung. ADM_JOB/ADM_USER chưa có script trong dev.',
    'Recruitment bỏ qua theo yêu cầu nhóm trưởng.'
  ] };
await fs.writeFile(path.join(dir, 'results.json'), JSON.stringify(report, null, 2));
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const href = file => path.relative(dir, file).split(path.sep).map(encodeURIComponent).join('/');
const phaseRows = report.phases.map(p => `<tr><td><a href="${href(path.join(path.dirname(p.report), 'index.html'))}">${esc(p.runId)}</a></td><td>${p.summary.Pass}</td><td>${p.summary.Fail}</td><td>${p.summary.Skipped}</td><td>${(p.durationMs / 1000).toFixed(2)}</td></tr>`).join('');
const suiteRows = report.suites.map(s => `<tr><td>${s.member}</td><td>${s.key}</td><td>${s.latest.Pass}</td><td>${s.latest.Fail}</td><td>${s.latest.Skipped}</td></tr>`).join('');
const rows = results.map(r => `<tr><td>${esc(r.id)}<br><small>${esc(r.evidenceRun)}</small></td><td class="${r.status}">${r.status}</td><td>${esc(r.expected)}</td><td>${esc(r.actual || r.reason)}</td><td>${(r.evidence || []).map(f => `<a href="${href(f)}">${esc(path.basename(f))}</a>`).join('<br>')}</td></tr>`).join('');
await fs.writeFile(path.join(dir, 'index.html'), `<!doctype html><html lang="vi"><meta charset="utf-8"><title>Test Report - Group 01</title><style>body{font:16px Arial;color:#18364a;margin:32px}table{width:100%;border-collapse:collapse;margin:24px 0}td,th{border:1px solid #cbd5e1;padding:10px;vertical-align:top;text-align:left}th{background:#eaf1f7}td:first-child{overflow-wrap:anywhere;min-width:220px}.Pass{color:#087143}.Fail{color:#ba2525}.Skipped{color:#865900}a{color:#075da5}small{font-size:12px}</style><h1>Test Report - Group 01</h1><p>Kết quả mới nhất theo Case ID, gồm lượt đầu và retest ngày 31/10/2026.</p><p><strong>${report.attempted}/187 case đã thực thi, không cộng trùng: ${summary.Pass} Pass, ${summary.Fail} Fail, ${summary.Skipped} Skipped.</strong><br>${report.attemptedIncludingRetest} lần thực thi nếu tính cả retest. Các case Skipped chưa được kiểm chứng; bộ nhóm chưa chạy đầy đủ.</p><p>Website: ${esc(first.baseUrl)}<br>Tổng thời gian ${report.phases.length} lượt chạy: ${(report.durationMs / 1000).toFixed(2)} giây, gồm setup/cleanup và không tính khoảng nghỉ giữa lượt.<br>Thời gian từ bắt đầu lượt đầu đến hết retest: ${(report.elapsedIncludingGapMs / 1000).toFixed(2)} giây.</p><h2>Lịch sử execution</h2><table><tr><th>Lượt</th><th>Pass</th><th>Fail</th><th>Skipped</th><th>Giây</th></tr>${phaseRows}</table><h2>Theo thành viên/suite</h2><table><tr><th>MSSV</th><th>Suite</th><th>Pass</th><th>Fail</th><th>Skipped</th></tr>${suiteRows}</table><h2>Nguyên nhân và xử lý</h2><ul>${report.diagnosis.map(s => `<li>${esc(s)}</li>`).join('')}${report.issues.map(s => `<li>${esc(s)}</li>`).join('')}</ul><p>PIM và Leave dùng dữ liệu riêng; trạng thái cleanup ở report gốc của từng lượt. Expected Leave trích từ workbook của tác giả. Inventory gồm các script/bước hiện có, không đại diện toàn bộ test design. Sáu ID Time lấy từ ASM07.md và chưa thực thi.</p><h2>Chi tiết</h2><table><tr><th>Case ID / nguồn</th><th>Latest</th><th>Expected</th><th>Actual / lý do</th><th>Evidence</th></tr>${rows}</table><p><a href="results.json">JSON tổng hợp</a>. Giữ nguyên report gốc của từng lượt.</p></html>`);
console.log(path.join(dir, 'results.json'));
