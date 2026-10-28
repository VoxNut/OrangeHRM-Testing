import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {randomBytes, createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import {chromium} from 'playwright';
import {expect} from 'playwright/test';
import {PDFDocument, StandardFonts} from 'pdf-lib';

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, '../../..');
const cases = JSON.parse(await fs.readFile(path.join(here, 'pim-details-cases.json'), 'utf8'));
assert.equal(cases.length, 50);
assert.equal(new Set(cases.map(c => c.id)).size, 50);
for (const module of ['PIM_PERSONAL', 'PIM_JOB']) assert.equal(cases.filter(c => c.module === module).length, 25);
const moduleFilter=process.env.PIM_DETAILS_MODULE;
assert.ok(!moduleFilter||['PIM_PERSONAL','PIM_JOB'].includes(moduleFilter),'PIM_DETAILS_MODULE must be PIM_PERSONAL or PIM_JOB');
const planned=moduleFilter?cases.filter(c=>c.module===moduleFilter):cases;
const runId = `PIM_DETAILS_${new Date().toISOString().replace(/[:.]/g, '-')}`;
const out = path.resolve(process.env.PIM_DETAILS_OUTPUT || path.join(here, 'results'), runId);
await fs.mkdir(path.join(out, 'screenshots'), {recursive: true});
await fs.mkdir(path.join(out, 'assets'), {recursive: true});
const base = 'https://opensource-demo.orangehrmlive.com/web/index.php';
const token = randomBytes(4).toString('hex');
const report = {runId, startedAt: new Date().toISOString(), baseUrl: base, browser: '', created: [], results: [], cleanup: [], setupErrors: []};
report.plannedCaseIds=planned.map(c=>c.id);
const write = () => fs.writeFile(path.join(out, 'results.json'), JSON.stringify(report, null, 2));
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
report.scriptSha256 = hash(await fs.readFile(fileURLToPath(import.meta.url)));
const assets = {};
for (const [name, text] of [['attachment_valid.pdf', 'Personal/Job attachment'], ['contract_v1.pdf', 'Contract version 1'], ['contract_v2.pdf', 'Contract version 2']]) {
  const doc = await PDFDocument.create();
  const font = await doc.embedFont(StandardFonts.Helvetica);
  doc.addPage().drawText(`Group01 23130226 | ${text} | Disposable fixture`, {x: 30, y: 750, size: 12, font});
  const bytes = Buffer.from(await doc.save());
  const filename = path.join(out, 'assets', name);
  await fs.writeFile(filename, bytes);
  assets[name] = {path: filename, sha256: hash(bytes)};
}
const disallowed = path.join(out, 'assets', 'contract_disallowed.bin');
await fs.writeFile(disallowed, 'Group01 23130226 disposable plain-text fixture, not executable code.\n');
const browser = await chromium.launch({channel: 'msedge', headless: true});
report.browser = browser.version();
const context = await browser.newContext({viewport: {width: 1440, height: 1000}, locale: 'en-US', acceptDownloads: true});
const page = await context.newPage();
page.setDefaultTimeout(20000);
page.setDefaultNavigationTimeout(45000);
const group = label => page.locator('.oxd-input-group').filter({has: page.locator('label').filter({hasText: new RegExp(`^${label}$`)})});
const field = label => group(label).locator('input');
const nameField = part => page.locator(`[name="${part}Name"]`);
const main = () => page.locator('form').first();
const save = () => main().getByRole('button', {name: 'Save', exact: true}).click();
const errorFor = control => control.locator('xpath=ancestor::div[contains(@class,"oxd-input-group")][1]').locator('.oxd-input-field-error-message');
const loaded = () => page.locator('.oxd-form-loader').waitFor({state: 'hidden'});
let current;
async function shot(suffix) {
  const relative = `screenshots/${current.id}${suffix ? '-' + suffix : ''}.png`;
  await page.screenshot({path: path.join(out, relative), fullPage: true});
  current.screenshots.push(relative);
}
async function search(id) {
  const initial = page.waitForResponse(r => r.url().includes('/api/v2/pim/employees?') && r.request().method() === 'GET');
  await page.goto(`${base}/pim/viewEmployeeList`);
  assert.equal((await initial).status(), 200);
  await page.waitForLoadState('networkidle');
  await field('Employee Id').fill(id);
  const response = page.waitForResponse(r => r.url().includes('/api/v2/pim/employees?') && new URL(r.url()).searchParams.get('employeeId') === id);
  await page.getByRole('button', {name: 'Search', exact: true}).click();
  const result = await response;
  assert.equal(result.status(), 200);
  const body = await result.json();
  const rows = page.locator('.oxd-table-body .oxd-table-card');
  await expect(rows).toHaveCount(body.data.length);
  for (let i = 0; i < body.data.length; i++) await expect(rows.nth(i).locator('.oxd-table-cell').nth(1)).toHaveText(body.data[i].employeeId);
  return rows;
}
async function createEmployee(prefix) {
  const id = `${prefix}${token}`;
  await expect(await search(id)).toHaveCount(0);
  await page.goto(`${base}/pim/addEmployee`);
  await loaded();
  await nameField('first').fill('G01N01');
  await nameField('middle').fill('Minh');
  await nameField('last').fill(`Nhut${prefix}${token}`);
  await field('Employee Id').fill(id);
  const response = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/api/v2/pim/employees') && r.request().method() === 'POST');
  await page.getByRole('button', {name: 'Save', exact: true}).click();
  const result = await response;
  assert.equal(result.status(), 200);
  const data = (await result.json()).data;
  assert.match(String(data.empNumber), /^\d+$/);
  const fixture = {id, key: String(data.empNumber), first: 'G01N01', middle: 'Minh', last: `Nhut${prefix}${token}`, allowedIds: [id]};
  report.created.push(fixture);
  await write();
  await page.waitForURL(new RegExp(`/viewPersonalDetails/empNumber/${fixture.key}$`));
  return fixture;
}
async function open(fixture, tab) {
  if (!fixture) throw Error(`Missing owned fixture for ${tab}`);
  const endpoint = tab === 'personal' ? 'personal-details' : 'job-details';
  const route = tab === 'personal' ? 'viewPersonalDetails' : 'viewJobDetails';
  const response = page.waitForResponse(r => new URL(r.url()).pathname.endsWith(`/employees/${fixture.key}/${endpoint}`) && r.request().method() === 'GET');
  await page.goto(`${base}/pim/${route}/empNumber/${fixture.key}`);
  assert.equal((await response).status(), 200);
  await page.waitForLoadState('networkidle');
  await loaded();
  assert.ok(page.url().endsWith(`/empNumber/${fixture.key}`));
  if (tab === 'personal') await expect(field('Employee Id')).toHaveValue(fixture.id);
  else await page.getByRole('heading', {name: 'Job Details', exact: true}).waitFor();
}
async function update() {
  await save();
  await page.getByText('Successfully Updated', {exact: true}).waitFor();
}
function formatDate(iso, pattern) {
  assert.match(pattern, /^(yyyy-mm-dd|yyyy-dd-mm|dd-mm-yyyy|dd\/mm\/yyyy|mm\/dd\/yyyy)$/);
  const [year, month, day] = iso.split('-');
  return pattern.replace('yyyy', year).replace('mm', month).replace('dd', day);
}
assert.equal(formatDate('2024-10-01', 'yyyy-dd-mm'), '2024-01-10');
assert.equal(formatDate('2000-02-29', 'yyyy-mm-dd'), '2000-02-29');
assert.throws(() => formatDate('2024-10-01', 'unknown'));
async function date(label, iso) {
  const value = formatDate(iso, await field(label).getAttribute('placeholder'));
  await field(label).fill(value);
  await field(label).press('Tab');
  return value;
}
async function choose(label, preferred) {
  const select = group(label);
  const old = (await select.locator('.oxd-select-text-input').innerText()).trim();
  const options = select.getByRole('option');
  if (!(await options.first().isVisible())) await select.locator('.oxd-select-text').click();
  await options.first().waitFor();
  const labels = (await options.allInnerTexts()).map(x => x.trim()).filter(x => x && x !== '-- Select --');
  const value = preferred || labels.find(x => x !== old);
  if (!value || !labels.includes(value)) throw Error(`No usable ${label} option`);
  await options.filter({hasText: new RegExp(`^${value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`)}).click();
  current.inputs[label] = value;
  return value;
}
async function rejected(control, pattern, reopen, before) {
  await save();
  await expect(errorFor(control)).toContainText(pattern);
  current.assertions.push(await errorFor(control).innerText());
  await shot('validation');
  await reopen();
  await expect(control).toHaveValue(before);
  current.assertions.push('Saved value unchanged after reopening');
}
async function contractOn() {
  if (!(await main().locator('input[type=checkbox]').isChecked())) await main().locator('.oxd-switch-input').click();
  await field('Contract Start Date').waitFor();
}
const contractFile = name => main().getByText(name, {exact: true});
const contractUpload = () => main().locator('input[type=file]');
async function contractChoice(label) {
  await main().locator('label').filter({hasText: new RegExp(`^${label}$`)}).click();
  if(label==='Replace Current')await contractUpload().waitFor({state:'attached'});
}
async function download(control, asset) {
  const waiting = page.waitForEvent('download');
  await control.click();
  const file = await waiting;
  const relative = `assets/${current.id}-${asset}`;
  await file.saveAs(path.join(out, relative));
  assert.equal(hash(await fs.readFile(path.join(out, relative))), assets[asset].sha256);
  current.downloads = [...(current.downloads || []), relative];
}
async function ensureContract(job, name = 'contract_v1.pdf') {
  await open(job, 'job');
  await contractOn();
  const start = await date('Contract Start Date', '2026-01-01');
  const end = await date('Contract End Date', '2026-12-31');
  if (!(await contractFile(name).isVisible())) {
    if (await main().getByText('Replace Current', {exact: true}).isVisible()) await contractChoice('Replace Current');
    await contractUpload().setInputFiles(assets[name].path);
  }
  await update();
  await open(job, 'job');
  await contractOn();
  await expect(field('Contract Start Date')).toHaveValue(start);
  await expect(field('Contract End Date')).toHaveValue(end);
  await expect(contractFile(name)).toBeVisible();
  return {start, end};
}
const attachmentForm = () => page.locator('form').filter({has: page.locator('input[type=file]')}).last();
const attachmentRows = () => page.locator('.oxd-table-body .oxd-table-card');
const attachmentRow = description => attachmentRows().filter({hasText: description});
async function addAttachment(fixture, tab, description) {
  await open(fixture, tab);
  await page.getByRole('button', {name: /Add/}).click();
  const form = attachmentForm();
  await form.locator('input[type=file]').setInputFiles(assets['attachment_valid.pdf'].path);
  await form.locator('textarea').fill(description);
  await form.getByRole('button', {name: 'Save', exact: true}).click();
  await page.getByText('Successfully Saved', {exact: true}).waitFor();
  await open(fixture, tab);
  const row = attachmentRow(description);
  await expect(row).toHaveCount(1);
  await expect(row).toContainText('attachment_valid.pdf');
  return row;
}
let person, job;
async function personalCase(n) {
  await open(person, 'personal');
  const reopen = () => open(person, 'personal');
  if (n === 1) {
    for (const [part, value] of [['first','G01N01'],['middle','Minh'],['last','NhutUpdated']]) await nameField(part).fill(value);
    await update(); await reopen();
    for (const [part, value] of [['first','G01N01'],['middle','Minh'],['last','NhutUpdated']]) await expect(nameField(part)).toHaveValue(value);
  } else if (n === 2 || n === 3) {
    const control = nameField(n === 2 ? 'first' : 'last');
    const before = await control.inputValue();
    await control.fill('');
    await rejected(control, /Required/, reopen, before);
  } else if ([4,5,6,7,9,10,11,12,13].includes(n)) {
    const control = n <= 5 ? nameField('first') : n === 6 ? nameField('middle') : n === 7 ? nameField('last') : n === 9 ? field('Employee Id') : n <= 11 ? field('Other Id') : field("Driver's License Number");
    const positive = [4,10,12].includes(n);
    const length = n === 9 ? 11 : positive ? 30 : 31;
    const value = n === 9 ? `T2${token}X` : (n < 9 ? 'A' : 'B').repeat(length);
    current.inputs.value = value;
    const before = await control.inputValue();
    await control.fill(value);
    if (positive) {await update(); await reopen(); await expect(control).toHaveValue(value);}
    else await rejected(control, new RegExp(`Should not exceed ${n === 9 ? 10 : 30} characters`), reopen, before);
  } else if (n === 8) {
    const alternate = `T2${token}`;
    await expect(await search(alternate)).toHaveCount(0);
    await reopen();
    person.allowedIds.push(alternate);
    await write();
    await field('Employee Id').fill(alternate); await update();
    const old = person.id; person.id = alternate;
    try {await reopen(); await expect(field('Employee Id')).toHaveValue(alternate); await shot('saved');}
    finally {await field('Employee Id').fill(old); await update(); person.id = old; await reopen();}
    current.inputs.value = alternate;
  } else if (n >= 14 && n <= 17) {
    const label = n <= 15 ? 'Date of Birth' : 'License Expiry Date';
    const iso = {14:'2000-02-29',15:'2000-02-30',16:'2030-12-31',17:'2030-04-31'}[n];
    const before = await field(label).inputValue();
    const value = await date(label, iso); current.inputs[label] = value;
    if (n === 14 || n === 16) {await update(); await reopen(); await expect(field(label)).toHaveValue(value);}
    else await rejected(field(label), /valid|format|date/i, reopen, before);
  } else if (n === 18 || n === 19) {
    const label = n === 18 ? 'Nationality' : 'Marital Status';
    const old = await group(label).locator('.oxd-select-text-input').innerText();
    const value = await choose(label, n === 19 ? old.trim() === 'Married' ? 'Single' : 'Married' : undefined);
    await update(); await reopen(); await expect(group(label).locator('.oxd-select-text-input')).toHaveText(value);
  } else if (n === 20) {
    const selected = await page.locator('input[type=radio][value="2"]').isChecked() ? 'Male' : 'Female';
    await page.locator('label').filter({hasText:new RegExp(`^${selected}$`)}).click(); await update(); await reopen();
    await expect(page.locator(`input[type=radio][value="${selected==='Female'?'2':'1'}"]`)).toBeChecked();
    await expect(page.locator('input[type=radio]:checked')).toHaveCount(1);
    current.inputs.Gender = selected;
  } else if (n === 21) {
    const row = await addAttachment(person, 'personal', 'G01N01 Personal attachment');
    await download(row.locator('button').filter({has:page.locator('.bi-download')}), 'attachment_valid.pdf');
  } else if (n === 22 || n === 24) {
    const count = await attachmentRows().count();
    await page.getByRole('button', {name:/Add/}).click();
    const form = attachmentForm();
    if (n === 24) await form.locator('input[type=file]').setInputFiles(assets['attachment_valid.pdf'].path);
    await form.locator('textarea').fill(n === 24 ? 'D'.repeat(201) : 'G01N01 No file');
    await form.getByRole('button', {name:'Save',exact:true}).click();
    await expect(form.locator('.oxd-input-field-error-message')).toContainText(n === 24 ? /200/ : /Required/);
    await shot('validation'); await reopen(); await expect(attachmentRows()).toHaveCount(count);
  } else if (n === 23) {
    const row = await addAttachment(person, 'personal', 'D'.repeat(200));
    await row.locator('button').filter({has: page.locator('.bi-pencil-fill')}).click();
    await expect(attachmentForm().locator('textarea')).toHaveValue('D'.repeat(200));
  } else if (n === 25) {
    const description = `Delete fixture ${token}`;
    await addAttachment(person, 'personal', description);
    const before = await attachmentRows().allInnerTexts();
    const other = before.filter(text => !text.includes(description));
    const remove = () => attachmentRow(description).locator('button').filter({has:page.locator('.bi-trash')}).click();
    await remove(); await page.getByRole('button',{name:/No, Cancel/}).click();
    await expect(attachmentRow(description)).toHaveCount(1);
    await remove(); await page.getByRole('button',{name:/Yes, Delete/}).click();
    await page.getByText('Successfully Deleted',{exact:true}).waitFor(); await reopen();
    await expect(attachmentRow(description)).toHaveCount(0);
    assert.deepEqual(await attachmentRows().allInnerTexts(), other);
  } else throw Error(`Unimplemented personal case ${n}`);
}
async function jobCase(n) {
  await open(job, 'job');
  const reopen = () => open(job, 'job');
  if ([1,2,9].includes(n)) {
    if (n === 9 && !(await field('Joined Date').inputValue())) {await date('Joined Date','2024-10-01');await update();await reopen();}
    const before = await field('Joined Date').inputValue();
    const value = n === 9 ? '' : await date('Joined Date',n === 1 ? '2024-10-01' : '2024-02-31');
    if(n===9)await field('Joined Date').fill('');
    if(n===2)await rejected(field('Joined Date'),/valid|format|date/i,reopen,before);
    else {await update(); await reopen(); await expect(field('Joined Date')).toHaveValue(value);}
    current.inputs.JoinedDate = value;
  } else if (n >= 3 && n <= 8) {
    const labels = ['Job Title','Job Category','Sub Unit','Location','Employment Status'];
    const selected = {};
    const dateValue = n === 8 ? await date('Joined Date','2024-10-01') : undefined;
    for (const label of n === 8 ? labels : [labels[n-3]]) selected[label] = await choose(label);
    await update();
    if(n===8)await open(job,'personal');
    await reopen();
    if(n===8)await expect(field('Joined Date')).toHaveValue(dateValue);
    for(const [label,value] of Object.entries(selected))await expect(group(label).locator('.oxd-select-text-input')).toHaveText(value);
  } else if (n===10) {
    if(await main().locator('input[type=checkbox]').isChecked())await main().locator('.oxd-switch-input').click();
    await expect(field('Contract Start Date')).toHaveCount(0);
    await contractOn(); await expect(field('Contract Start Date')).toBeVisible(); await expect(field('Contract End Date')).toBeVisible();
    await expect(contractUpload()).toBeAttached();
  } else if (n===11) {
    const dates=await ensureContract(job);
    await main().locator('.oxd-switch-input').click(); await expect(field('Contract Start Date')).toHaveCount(0);
    await contractOn(); await expect(field('Contract Start Date')).toHaveValue(dates.start); await expect(field('Contract End Date')).toHaveValue(dates.end);
    await expect(contractFile('contract_v1.pdf')).toBeVisible();
  } else if(n>=12&&n<=16) {
    await contractOn();
    const beforeStart=await field('Contract Start Date').inputValue(),beforeEnd=await field('Contract End Date').inputValue();
    const start=await date('Contract Start Date',n===15?'2026-02-31':'2026-01-01');
    const end=await date('Contract End Date',{12:'2026-12-31',13:'2025-12-31',14:'2026-01-01',15:'2026-12-31',16:'2026-04-31'}[n]);
    current.inputs.contract={start,end};
    if(n===12){await update();await reopen();await contractOn();await expect(field('Contract Start Date')).toHaveValue(start);await expect(field('Contract End Date')).toHaveValue(end);}
    else {
      await save(); const error=errorFor(field(n===15?'Contract Start Date':'Contract End Date'));
      await expect(error).toContainText(n===13||n===14?/after/i:/valid|format|date/i);
      current.assertions.push(await error.innerText()); await shot('validation'); await reopen();await contractOn();
      await expect(field('Contract Start Date')).toHaveValue(beforeStart);await expect(field('Contract End Date')).toHaveValue(beforeEnd);
    }
  } else if(n>=17&&n<=23) {
    const dates=await ensureContract(job);
    if(n===17) {
      await contractChoice('Delete Current');await update();await reopen();await contractOn();
      await expect(contractFile('contract_v1.pdf')).toHaveCount(0);
      await contractUpload().setInputFiles(assets['contract_v1.pdf'].path);await update();await reopen();await contractOn();
      await expect(contractFile('contract_v1.pdf')).toBeVisible();await download(contractFile('contract_v1.pdf'),'contract_v1.pdf');
    } else if(n===18||n===19||n===22) {
      await contractChoice('Replace Current');
      let upload;
      if(n===18){
        const hint=await main().innerText();const match=hint.match(/up to\s+(\d+(?:\.\d+)?)\s*(MB|KB)/i);assert.ok(match,`No upload limit hint: ${hint}`);
        const limit=Number(match[1])*(match[2].toUpperCase()==='MB'?1024*1024:1024);assert.ok(limit>0&&limit<20*1024*1024);
        upload=path.join(out,'assets','contract_over_limit.pdf');const original=await fs.readFile(assets['contract_v1.pdf'].path);
        await fs.writeFile(upload,Buffer.concat([original,Buffer.alloc(limit+1-original.length,32)]));
        current.inputs.upload={hint,size:(await fs.stat(upload)).size,limit};assert.equal((await fs.stat(upload)).size,limit+1);
      } else if(n===19){upload=disallowed;current.inputs.upload={filename:'contract_disallowed.bin',reason:'CSV accepted on public demo; test design permits another disallowed type'};}
      if(upload)await contractUpload().setInputFiles(upload);
      await save();
      const err=errorFor(contractUpload());
      await expect(err).toContainText(n===18?/size|exceed|large/i:n===19?/type|allowed/i:/Required/);
      current.assertions.push(await err.innerText());await shot('validation');await reopen();await contractOn();
      await expect(contractFile('contract_v1.pdf')).toBeVisible();await download(contractFile('contract_v1.pdf'),'contract_v1.pdf');
    } else if(n===20){
      await contractChoice('Keep Current');const value=await date('Contract End Date','2027-12-31');await update();await reopen();await contractOn();
      await expect(field('Contract End Date')).toHaveValue(value);await expect(contractFile('contract_v1.pdf')).toBeVisible();await download(contractFile('contract_v1.pdf'),'contract_v1.pdf');
    } else if(n===21){
      await contractChoice('Replace Current');await contractUpload().setInputFiles(assets['contract_v2.pdf'].path);await update();await reopen();await contractOn();
      await expect(contractFile('contract_v2.pdf')).toBeVisible();await expect(contractFile('contract_v1.pdf')).toHaveCount(0);await download(contractFile('contract_v2.pdf'),'contract_v2.pdf');
    } else {
      await contractChoice('Delete Current');await update();await reopen();await contractOn();await expect(contractFile('contract_v1.pdf')).toHaveCount(0);
      await expect(contractUpload()).toBeAttached();await expect(field('Contract Start Date')).toHaveValue(dates.start);await expect(field('Contract End Date')).toHaveValue(dates.end);
    }
  } else if(n===24){
    const description='G01N01 Job attachment';await addAttachment(job,'job',description);
    await open(job,'personal');await expect(attachmentRow(description)).toHaveCount(0);await reopen();await expect(attachmentRow(description)).toHaveCount(1);
  } else if(n===25){
    const before=await attachmentRows().allInnerTexts();await page.getByRole('button',{name:/Add/}).click();const form=attachmentForm();
    await form.locator('input[type=file]').setInputFiles(assets['attachment_valid.pdf'].path);await form.locator('textarea').fill('G01N01 Job cancel');
    await form.getByRole('button',{name:'Cancel',exact:true}).click();await expect(page.getByRole('heading',{name:'Add Attachment',exact:true})).toHaveCount(0);await reopen();
    await expect(attachmentRow('G01N01 Job cancel')).toHaveCount(0);assert.deepEqual(await attachmentRows().allInnerTexts(),before);
  } else throw Error(`Unimplemented job case ${n}`);
}

try {
  await page.goto(`${base}/auth/login`);await page.locator('[name=username]').fill('Admin');await page.locator('[name=password]').fill('admin123');
  await page.getByRole('button',{name:'Login',exact:true}).click();await page.waitForURL('**/dashboard/**');
  if(process.env.PIM_DETAILS_CLEANUP){
    const previous=JSON.parse(await fs.readFile(process.env.PIM_DETAILS_CLEANUP,'utf8'));
    assert.ok(Array.isArray(previous.created));
    report.created=previous.created;
  } else {
    if(!moduleFilter||moduleFilter==='PIM_PERSONAL')person=await createEmployee('P2');
    if(!moduleFilter||moduleFilter==='PIM_JOB')job=await createEmployee('J2');
    for(const test of planned){
      current={...test,startedAt:new Date().toISOString(),status:'Untested',inputs:{},assertions:[],screenshots:[]};
      const started=Date.now();console.log(`RUN ${test.id}`);
      try {
        const n=test.row-8;
        if(test.module==='PIM_PERSONAL')await personalCase(n);else await jobCase(n);
        current.observed=await page.locator('.orangehrm-edit-employee').evaluate(root=>({
          fields:[...root.querySelectorAll('input:not([type=file]),textarea')].map(e=>({name:e.name,label:e.closest('.oxd-input-group')?.querySelector('label')?.textContent,value:e.value,checked:['radio','checkbox'].includes(e.type)?e.checked:undefined})),
          selections:[...root.querySelectorAll('.oxd-input-group')].filter(g=>g.querySelector('.oxd-select-text-input')).map(g=>({label:g.querySelector('label')?.textContent,value:g.querySelector('.oxd-select-text-input').textContent.trim()})),
          attachmentRows:[...root.querySelectorAll('.oxd-table-body .oxd-table-card')].map(e=>e.innerText),
        }));
        current.status='Passed';current.actual=[current.title,...current.assertions,'Assertion đạt; dữ liệu sau mở lại và ảnh trong report'].join(' | ');
      } catch(error){current.status=error.name==='AssertionError'||error.matcherResult?'Failed':'Blocked';current.actual=`${error.name}: ${error.message}`;current.stack=error.stack;}
      try{await shot('result');}catch(error){current.screenshotError=error.message;}
      current.durationMs=Date.now()-started;current.finishedAt=new Date().toISOString();report.results.push(current);await write();
      console.log(`${test.id}: ${current.status} (${current.durationMs} ms) ${current.actual.slice(0,180)}`);
    }
  }
} catch(error){report.fatal={message:error.message,stack:error.stack};console.error('FATAL',error.message);}
finally {
  for(const fixture of report.created){
    try {
      await page.goto(`${base}/pim/viewPersonalDetails/empNumber/${fixture.key}`);await page.waitForLoadState('networkidle');
      const id=await field('Employee Id').inputValue();assert.ok(fixture.allowedIds.includes(id),'Cleanup ID must belong to this run');
      const rows=await search(id);await expect(rows).toHaveCount(1);await rows.first().locator('button').filter({has:page.locator('.bi-pencil-fill')}).click();await page.waitForURL(new RegExp(`/empNumber/${fixture.key}$`));
      await expect(field('Employee Id')).toHaveValue(id);const confirmed=await search(id);
      await confirmed.first().locator('button').filter({has:page.locator('.bi-trash')}).click();await page.getByRole('button',{name:/Yes, Delete/}).click();await page.getByText('Successfully Deleted',{exact:true}).waitFor();
      await expect(await search(id)).toHaveCount(0);report.cleanup.push({id,key:fixture.key,status:'Deleted and absence verified'});
    } catch(error){report.cleanup.push({id:fixture.id,key:fixture.key,status:'Needs cleanup',error:error.message});}
    await write();
  }
  await browser.close();report.finishedAt=new Date().toISOString();
  report.summary=Object.fromEntries(['Passed','Failed','Blocked'].map(s=>[s,report.results.filter(r=>r.status===s).length]));report.summary.NotRun=planned.length-report.results.length;
  await write();console.log('SUMMARY',JSON.stringify(report.summary),'CLEANUP',JSON.stringify(report.cleanup),'OUTPUT',out);
  if(report.fatal||report.summary.Passed!==planned.length||report.cleanup.some(c=>c.status==='Needs cleanup'))process.exitCode=1;
}
