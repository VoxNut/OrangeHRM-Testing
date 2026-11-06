import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

const root = path.dirname(fileURLToPath(import.meta.url));
const modules = {ADM_STRUCT:'adm_struct.mjs', ADM_JOB:'adm_job.mjs', ADM_USER:'adm_user.mjs'};
const args = process.argv.slice(2);
const value = (key, fallback) => args.includes(key) ? args[args.indexOf(key) + 1] : fallback;
const phase = value('--phase', 'both');
const moduleName = value('--module', 'all');
const legacy = args.includes('--legacy-read');
assert(['initial', 'retest', 'both'].includes(phase), 'Use --phase initial|retest|both');
assert(moduleName === 'all' || Object.hasOwn(modules, moduleName), 'Unknown Admin module');
const selected = legacy ? ['LEGACY_STRUCT'] : moduleName === 'all' ? Object.keys(modules) : [moduleName];
const repo = path.resolve(process.env.ADM_REPO || root);
const reportRoot = path.join(repo, 'reports');
const stamp = new Date().toISOString().replace(/[:.]/g, '-');
const run = path.join(reportRoot, 'run-' + stamp);
const read = file => JSON.parse(fs.readFileSync(file, 'utf8'));

function execute(name, stage, only) {
  const runId = [stamp, stage, name].join('_');
  const logFile = path.join(run, stage, name + '.log');
  const reportFile = path.join(repo, 'evidence', 'executions', '23130171', runId, 'results.json');
  fs.mkdirSync(path.dirname(logFile), {recursive:true});
  console.log(stage, name);
  const env = {...process.env, ADM_REPO:repo, ADM_RUN_ID:runId};
  if (only) env.ADM_ONLY = only;
  else delete env.ADM_ONLY;
  const child = spawnSync(process.execPath, [legacy ? 'run-adm.mjs' : modules[name]], {
    cwd:root, env, encoding:'utf8', maxBuffer:20*1024*1024
  });
  const log = [child.stdout, child.stderr, child.error?.message].filter(Boolean).join('\n');
  fs.writeFileSync(logFile, log);
  const report = fs.existsSync(reportFile) ? read(reportFile) : {};
  const cases = report.results || [];
  const error = report.fatal?.message || child.error?.message ||
    (child.status !== 0 ? 'Child exited with ' + child.status : cases.length === 0 ? 'No cases reported' : null);
  const cleanupFailed = (report.cleanup || []).some(item => item.status === 'Failed') ||
    (report.notes || []).some(note => note.startsWith('Cleanup error:'));
  return {module:name, exitCode:child.status, total:cases.length, counts:report.summary || {},
    cases, error, cleanupFailed, reportFile:fs.existsSync(reportFile) ? reportFile : null, logFile};
}

const initial = {}, retest = {};
const bad = result => ['Failed', 'Blocked'].includes(result.status);
for (const name of selected) {
  if (phase !== 'retest') initial[name] = execute(name, 'initial', legacy ? '1,2,3' : process.env.ADM_ONLY);
  if (phase === 'initial' || legacy) continue;
  const previous = initial[name] || fs.readdirSync(reportRoot).filter(n => n.startsWith('run-')).sort().reverse()
    .map(n => path.join(reportRoot, n, 'initial', 'summary.json'))
    .filter(file => fs.existsSync(file)).map(file => read(file)[name]).find(Boolean);
  if (!previous) {
    retest[name] = {module:name, error:'No initial report', total:0, counts:{}, cases:[]};
    continue;
  }
  const failed = previous.cases.filter(bad);
  if (failed.length && !previous.cleanupFailed && !previous.error) {
    // Structure cases share the parent and child created earlier in the suite.
    retest[name] = execute(name, 'retest', name === 'ADM_STRUCT' ? undefined : failed.map(row => row.id).join(','));
  }
}
for (const [stage, data] of Object.entries({initial, retest})) {
  fs.mkdirSync(path.join(run, stage), {recursive:true});
  fs.writeFileSync(path.join(run, stage, 'summary.json'), JSON.stringify(data,null,2));
  for (const result of Object.values(data)) {
    console.log(result.module, JSON.stringify(result.counts), result.error || '');
    if (result.error || result.cleanupFailed || result.cases.some(bad)) process.exitCode = 1;
  }
}
console.log('Reports:', run);
