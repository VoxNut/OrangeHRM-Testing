import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';

const file = process.argv[2];
const report = JSON.parse(await fs.readFile(file, 'utf8'));
assert.ok(Array.isArray(report.results) && report.summary && report.runId, 'Execution report required');
const esc = value => String(value ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
assert.equal(esc('<a&"'), '&lt;a&amp;&quot;');
const rows = report.results.map(test => `<tr><td>${esc(test.id)}<br>${esc(test.title)}</td><td>${esc(test.expected)}</td><td class="${esc(test.status)}">${esc(test.status)}</td><td>${(test.durationMs / 1000).toFixed(2)}s</td><td>${esc(test.actual)}<br>${test.screenshots.map((name, i) => `<a href="${esc(name)}">Ảnh ${i + 1}</a>`).join(' · ')}</td></tr>`).join('');
const html = `<!doctype html><html lang="vi"><meta charset="utf-8"><title>PIM – ${esc(report.runId)}</title>
<style>body{font:15px Arial;margin:36px;color:#172033}table{border-collapse:collapse;width:100%}th,td{border:1px solid #b6bfca;padding:10px;text-align:left;vertical-align:top}th{background:#102e54;color:white}.Passed{color:#12633b}.Failed{color:#a12026}.Blocked{color:#885b00}a{color:#125bc6}</style>
<h1>Group01 – PIM</h1><p>Võ Minh Nhựt – 23130226 · ${esc(report.runId)}</p>
<p>Playwright / Edge ${esc(report.browser)}. ${esc(report.startedAt)} – ${esc(report.finishedAt)} (UTC).</p>
<p>${Object.entries(report.summary).map(([status, count]) => `${esc(status)}: ${count}`).join(' · ')}</p><p><a href="results.json">JSON report</a></p>
<table><tr><th>Test Case</th><th>Expected</th><th>Result</th><th>Time</th><th>Actual / Evidence</th></tr>${rows}</table>
<h2>Cleanup</h2><pre>${esc(JSON.stringify(report.cleanup, null, 2))}</pre></html>`;
await fs.writeFile(path.join(path.dirname(file), 'report.html'), html);
console.log('Report:', path.join(path.dirname(file), 'report.html'));
