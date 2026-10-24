import fs from 'node:fs/promises';
import path from 'node:path';
import {randomBytes, createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import {chromium} from 'playwright';
import {expect} from 'playwright/test';
import {PDFDocument, StandardFonts} from 'pdf-lib';

const base = 'https://opensource-demo.orangehrmlive.com/web/index.php';
const token = randomBytes(4).toString('hex');
const employee = {id: `N7${token}`, first: `Nhut${token}`, middle: '', last: 'ASM7'};
const runId = `ASM7_${new Date().toISOString().replace(/[:.]/g, '-')}`;
const out = path.resolve(process.env.ASM7_OUTPUT_DIR || 'results', runId);
await fs.mkdir(path.join(out, 'screenshots'), {recursive: true});
await fs.mkdir(path.join(out, 'assets'), {recursive: true});
const report = {
  runId, student: '23130226 - Võ Minh Nhựt', startedAt: new Date().toISOString(),
  website: base, tool: 'Playwright', coverage: '15 representative cases: 5 per function, not all 75 ASM6 cases',
  employee, results: [], cleanup: {status: 'Not started'},
};
const writeReport = () => fs.writeFile(path.join(out, 'results.json'), JSON.stringify(report, null, 2));
const pdf = await PDFDocument.create();
const font = await pdf.embedFont(StandardFonts.Helvetica);
pdf.addPage().drawText('Group01 | 23130226 | ASM7 | Disposable attachment and contract fixture', {x: 30, y: 750, size: 12, font});
const fixtureBytes = Buffer.from(await pdf.save());
const fixture = path.join(out, 'assets', 'asm7_fixture.pdf');
await fs.writeFile(fixture, fixtureBytes);
const digest = bytes => createHash('sha256').update(bytes).digest('hex');
report.fixtureSha256 = digest(fixtureBytes);

function formatDate(iso, pattern) {
  assert.match(pattern, /^(yyyy-mm-dd|yyyy-dd-mm|dd-mm-yyyy|dd\/mm\/yyyy|mm\/dd\/yyyy)$/,
    `Unrecognized UI date format: ${pattern}`);
  const [year, month, day] = iso.split('-');
  return pattern.replace('yyyy', year).replace('mm', month).replace('dd', day);
}
// Small self-check for the date order that caused ambiguity on the public demo.
assert.equal(formatDate('2024-10-01', 'yyyy-dd-mm'), '2024-01-10');
assert.equal(formatDate('2000-02-29', 'yyyy-mm-dd'), '2000-02-29');
assert.equal(formatDate('2026-12-31', 'dd/mm/yyyy'), '31/12/2026');
assert.throws(() => formatDate('2024-10-01', 'unknown'), /Unrecognized/);

const browser = await chromium.launch({channel: 'msedge', headless: true, slowMo: 150});
report.browserVersion = browser.version();
let context;
let page;
let owned;
const catalog = {};
const group = label => page.locator('.oxd-input-group')
  .filter({has: page.locator('label').filter({hasText: new RegExp(`^${label}$`)})});
const field = label => group(label).locator('input');
const nameField = part => page.locator(`input[name="${part}Name"]`);
const form = () => page.locator('form').first();
const save = () => form().getByRole('button', {name: 'Save', exact: true}).click();
const loaded = () => page.locator('.oxd-form-loader').waitFor({state: 'hidden'});
function requireOwned() {
  if (!owned) throw new Error('Employee setup did not complete; refusing to use shared records');
}
async function list() {
  const response = page.waitForResponse(r => r.url().includes('/api/v2/pim/employees?') && r.request().method() === 'GET');
  await page.goto(`${base}/pim/viewEmployeeList`);
  assert.equal((await response).status(), 200, 'Employee List must load');
  await loaded();
  await page.getByRole('button', {name: 'Search', exact: true}).waitFor();
}
async function search(id) {
  await list();
  await field('Employee Id').fill(id);
  const response = page.waitForResponse(r => r.url().includes('/api/v2/pim/employees?') &&
    new URL(r.url()).searchParams.get('employeeId') === id && r.request().method() === 'GET');
  await page.getByRole('button', {name: 'Search', exact: true}).click();
  const result = await response;
  assert.equal(result.status(), 200);
  const data = await result.json();
  await loaded();
  const rows = page.locator('.oxd-table-body .oxd-table-card');
  await expect(rows).toHaveCount(data.data.length);
  for (let i = 0; i < data.data.length; i++) {
    await expect(rows.nth(i).locator('.oxd-table-cell').nth(1)).toHaveText(data.data[i].employeeId);
  }
  return rows;
}
async function noRecord(id) {
  await expect(await search(id)).toHaveCount(0);
  await expect(page.locator('.orangehrm-horizontal-padding')).toContainText('No Records Found');
}
async function openAdd() {
  await page.goto(`${base}/pim/addEmployee`);
  await nameField('first').waitFor();
  await loaded();
  await nameField('first').fill(employee.first);
  await nameField('middle').fill(employee.middle);
  await nameField('last').fill(employee.last);
  await field('Employee Id').fill(employee.id);
}
async function profile(tab = 'personal') {
  requireOwned();
  const endpoint = tab === 'personal' ? 'personal-details' : 'job-details';
  const route = tab === 'personal' ? 'viewPersonalDetails' : 'viewJobDetails';
  const response = page.waitForResponse(r => new URL(r.url()).pathname.endsWith(`/employees/${owned.key}/${endpoint}`) && r.request().method() === 'GET');
  await page.goto(`${base}/pim/${route}/empNumber/${owned.key}`);
  assert.equal((await response).status(), 200);
  await page.waitForLoadState('networkidle');
  await loaded();
  if (tab === 'personal') await expect(field('Employee Id')).toHaveValue(employee.id);
  else await page.getByRole('heading', {name: 'Job Details', exact: true}).waitFor();
  assert.ok(page.url().endsWith(`/empNumber/${owned.key}`));
}
async function update() {
  await save();
  await page.getByText('Successfully Updated', {exact: true}).waitFor();
}
async function choose(label) {
  const options = group(label).getByRole('option');
  // Tab from a date input can already open the next dropdown on focus.
  if (!(await options.first().isVisible())) await group(label).locator('.oxd-select-text').click();
  await options.first().waitFor();
  const labels = (await options.allInnerTexts()).map(t => t.trim()).filter(t => t && t !== '-- Select --');
  if (!labels.length) throw new Error(`No fixture catalog options for ${label}`);
  await page.getByRole('option', {name: labels[0], exact: true}).click();
  return labels[0];
}
async function date(label, iso) {
  const value = formatDate(iso, await field(label).getAttribute('placeholder'));
  await field(label).fill(value);
  await field(label).press('Tab');
  return value;
}
async function contractOn() {
  if (!(await form().locator('input[type=checkbox]').isChecked())) {
    await form().locator('.oxd-switch-input').click();
  }
  await field('Contract Start Date').waitFor();
}
async function checkDownload(control) {
  const event = page.waitForEvent('download');
  await control.click();
  const download = await event;
  const file = path.join(out, 'assets', `download-${Date.now()}.pdf`);
  await download.saveAs(file);
  assert.equal(digest(await fs.readFile(file)), report.fixtureSha256, 'Downloaded bytes must match the uploaded fixture');
}

const cases = [
  ['PIM_EMP_Create_03', 'Bỏ trống First Name', async () => {
    await noRecord(employee.id);
    await openAdd();
    await nameField('first').fill('');
    await save();
    await expect(group('Employee Full Name').locator('.oxd-input-field-error-message').first()).toHaveText('Required');
    assert.ok(page.url().endsWith('/pim/addEmployee'));
    await page.screenshot({path: path.join(out, 'screenshots', 'PIM_EMP_Create_03-validation.png')});
    await noRecord(employee.id);
    return 'First Name: Required; no employee was created';
  }],
  ['PIM_EMP_CancelCreate_01', 'Hủy thêm nhân viên', async () => {
    await openAdd();
    await page.getByRole('button', {name: 'Cancel', exact: true}).click();
    await page.waitForURL('**/pim/viewEmployeeList');
    await noRecord(employee.id);
    return 'Cancel returned to Employee List; no employee was created';
  }],
  ['PIM_EMP_Create_01', 'Tạo nhân viên và kiểm tra sau tải lại', async () => {
    await noRecord(employee.id);
    await openAdd();
    const response = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/api/v2/pim/employees') && r.request().method() === 'POST');
    await save();
    const created = await response;
    assert.equal(created.status(), 200, 'Create employee request must succeed');
    const body = await created.json();
    const key = body.data.empNumber;
    assert.match(String(key), /^\d+$/);
    owned = {id: employee.id, key: String(key)};
    report.createdEmployee = owned;
    await writeReport();
    await page.waitForURL(new RegExp(`/pim/viewPersonalDetails/empNumber/${key}$`));
    await profile();
    await expect(nameField('first')).toHaveValue(employee.first);
    await expect(nameField('middle')).toHaveValue('');
    await expect(nameField('last')).toHaveValue(employee.last);
    const rows = await search(employee.id);
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText(employee.first);
    await expect(rows.first()).toContainText(employee.last);
    return `Created numeric employee key ${key}; names and ID persisted; exactly one list row`;
  }],
  ['PIM_EMP_Search_01', 'Tìm đúng nhân viên theo Employee Id', async () => {
    requireOwned();
    const rows = await search(employee.id);
    await expect(rows).toHaveCount(1);
    await expect(rows.first().locator('.oxd-table-cell').nth(1)).toHaveText(employee.id);
    await expect(rows.first()).toContainText(employee.first);
    return `Search returned only ${employee.id} with the expected first name`;
  }],
  ['PIM_EMP_Search_03', 'Tìm mã nhân viên không tồn tại', async () => {
    const missing = `Z7${token}`;
    await noRecord(missing);
    return `Search ${missing}: zero rows and No Records Found`;
  }],
  ['PIM_PERSONAL_Update_01', 'Cập nhật ba thành phần tên', async () => {
    await profile();
    employee.middle = 'Minh';
    employee.last = 'NhutUpdated';
    await nameField('first').fill(employee.first);
    await nameField('middle').fill(employee.middle);
    await nameField('last').fill(employee.last);
    await update();
    await profile();
    for (const part of ['first', 'middle', 'last']) await expect(nameField(part)).toHaveValue(employee[part]);
    return 'First/Middle/Last persisted on the same numeric employee key after reopening';
  }],
  ['PIM_PERSONAL_Update_02', 'Chặn lưu First Name rỗng', async () => {
    await profile();
    await nameField('first').fill('');
    await save();
    await expect(group('Employee Full Name').locator('.oxd-input-field-error-message').first()).toHaveText('Required');
    await page.screenshot({path: path.join(out, 'screenshots', 'PIM_PERSONAL_Update_02-validation.png')});
    await profile();
    await expect(nameField('first')).toHaveValue(employee.first);
    return 'Required was displayed; the previously saved First Name remained unchanged';
  }],
  ['PIM_PERSONAL_Update_14', 'Ngày sinh nhuận hợp lệ', async () => {
    await profile();
    const value = await date('Date of Birth', '2000-02-29');
    await update();
    await profile();
    await expect(field('Date of Birth')).toHaveValue(value);
    return `Leap-day DOB persisted: ${value} in the actual UI date format`;
  }],
  ['PIM_PERSONAL_Update_18', 'Lưu quốc tịch từ danh mục', async () => {
    await profile();
    catalog.Nationality = await choose('Nationality');
    await update();
    await profile();
    await expect(group('Nationality').locator('.oxd-select-text-input')).toHaveText(catalog.Nationality);
    return `Nationality persisted after reopening: ${catalog.Nationality}`;
  }],
  ['PIM_PERSONAL_CreateAttachment_01', 'Upload và tải lại attachment PDF', async () => {
    await profile();
    await page.getByRole('button', {name: /Add/}).click();
    const attachment = page.locator('form').filter({has: page.locator('input[type=file]')});
    await attachment.locator('input[type=file]').setInputFiles(fixture);
    await attachment.locator('textarea').fill(`ASM7 Personal ${token}`);
    await attachment.getByRole('button', {name: 'Save', exact: true}).click();
    await page.getByText('Successfully Saved', {exact: true}).waitFor();
    await profile();
    const row = page.locator('.oxd-table-card').filter({hasText: `ASM7 Personal ${token}`});
    await expect(row).toHaveCount(1);
    await expect(row).toContainText('asm7_fixture.pdf');
    await checkDownload(row.locator('button').filter({has: page.locator('.bi-download')}));
    return 'Exactly one attachment row with the expected filename/description; downloaded SHA-256 matches';
  }],
  ['PIM_JOB_UpdateJob_08', 'Lưu ngày vào làm và 5 danh mục công việc', async () => {
    await profile('job');
    catalog.joinedDate = await date('Joined Date', '2024-10-01');
    for (const label of ['Job Title', 'Job Category', 'Sub Unit', 'Location', 'Employment Status']) catalog[label] = await choose(label);
    await update();
    await profile('job');
    await expect(field('Joined Date')).toHaveValue(catalog.joinedDate);
    for (const label of ['Job Title', 'Job Category', 'Sub Unit', 'Location', 'Employment Status']) {
      await expect(group(label).locator('.oxd-select-text-input')).toHaveText(catalog[label]);
    }
    report.catalog = catalog;
    return `Joined Date and all 5 selected job catalog values persisted: ${JSON.stringify(catalog)}`;
  }],
  ['PIM_JOB_UpdateJob_02', 'Chặn ngày vào làm không tồn tại', async () => {
    await profile('job');
    const before = await field('Joined Date').inputValue();
    await date('Joined Date', '2024-02-31');
    await save();
    await expect(group('Joined Date').locator('.oxd-input-field-error-message')).toBeVisible();
    await page.screenshot({path: path.join(out, 'screenshots', 'PIM_JOB_UpdateJob_02-validation.png')});
    await profile('job');
    await expect(field('Joined Date')).toHaveValue(before);
    return 'Nonexistent 31 February was rejected; saved Joined Date remained unchanged';
  }],
  ['PIM_JOB_ToggleContract_01', 'Bật phần thông tin hợp đồng', async () => {
    await profile('job');
    await expect(field('Contract Start Date')).toHaveCount(0);
    await contractOn();
    await expect(field('Contract Start Date')).toBeVisible();
    await expect(field('Contract End Date')).toBeVisible();
    await expect(group('Contract Details').locator('input[type=file]')).toBeAttached();
    return 'Toggling the switch displayed start date, end date and contract upload; no save claimed';
  }],
  ['PIM_JOB_UpdateContract_02', 'Chặn End Date trước Start Date', async () => {
    await profile('job');
    await contractOn();
    await date('Contract Start Date', '2026-06-01');
    await date('Contract End Date', '2026-05-31');
    await save();
    const error = group('Contract End Date').locator('.oxd-input-field-error-message');
    await expect(error).toContainText(/after/i);
    const text = await error.innerText();
    await page.screenshot({path: path.join(out, 'screenshots', 'PIM_JOB_UpdateContract_02-validation.png')});
    await profile('job');
    await contractOn();
    await expect(field('Contract Start Date')).toHaveValue('');
    await expect(field('Contract End Date')).toHaveValue('');
    return `${text}; invalid dates were not persisted`;
  }],
  ['PIM_JOB_UploadContract_01', 'Lưu hợp đồng PDF và kiểm tra tải xuống', async () => {
    await profile('job');
    await contractOn();
    const start = await date('Contract Start Date', '2026-01-01');
    const end = await date('Contract End Date', '2026-12-31');
    await group('Contract Details').locator('input[type=file]').setInputFiles(fixture);
    await update();
    await profile('job');
    await expect(form().locator('input[type=checkbox]')).toBeChecked();
    await expect(field('Contract Start Date')).toHaveValue(start);
    await expect(field('Contract End Date')).toHaveValue(end);
    const file = form().getByText('asm7_fixture.pdf', {exact: true});
    await expect(file).toBeVisible();
    await checkDownload(file);
    return `Contract start/end and filename persisted; downloaded PDF SHA-256 matches ${report.fixtureSha256}`;
  }],
];
assert.equal(cases.length, 15);
assert.equal(new Set(cases.map(([id]) => id)).size, 15, 'Case IDs must be unique');
for (const module of ['PIM_EMP', 'PIM_PERSONAL', 'PIM_JOB']) {
  assert.equal(cases.filter(([id]) => id.startsWith(`${module}_`)).length, 5);
}

try {
  context = await browser.newContext({viewport: {width: 1440, height: 1000}, locale: 'en-US',
    recordVideo: {dir: path.join(out, 'video'), size: {width: 1440, height: 1000}}});
  report.videoStartedAt = new Date().toISOString();
  page = await context.newPage();
  page.setDefaultTimeout(25000);
  page.setDefaultNavigationTimeout(45000);
  await page.goto(`${base}/auth/login`);
  await page.locator('[name=username]').fill('Admin');
  await page.locator('[name=password]').fill('admin123');
  await page.getByRole('button', {name: 'Login', exact: true}).click();
  await page.waitForURL('**/dashboard/**');
  for (const [id, title, run] of cases) {
    const item = {id, title, startedAt: new Date().toISOString()};
    console.log(`RUN ${id} | ${title}`);
    const started = Date.now();
    try {
      item.actual = await run();
      assert.ok(item.actual, 'Each completed test must explain its checked result');
      item.status = 'Passed';
    } catch (error) {
      item.status = error.name === 'AssertionError' || error.matcherResult ? 'Failed' : 'Blocked';
      item.actual = `${error.name}: ${error.message}`;
      item.stack = error.stack;
    }
    item.finishedAt = new Date().toISOString();
    item.durationMs = Date.now() - started;
    item.screenshot = `screenshots/${id}.png`;
    try {await page.screenshot({path: path.join(out, item.screenshot), fullPage: true});}
    catch (error) {item.screenshotError = error.message;}
    report.results.push(item);
    await writeReport();
    console.log(`${id}: ${item.status} (${item.durationMs} ms) ${item.actual.slice(0, 180)}`);
  }
} catch (error) {
  report.fatal = {message: error.message, stack: error.stack};
} finally {
  if (owned && page) {
    try {
      // Delete only the disposable employee whose key was returned by this run's own UI create.
      await profile();
      await expect(field('Employee Id')).toHaveValue(owned.id);
      const rows = await search(owned.id);
      await expect(rows).toHaveCount(1);
      await rows.first().locator('button').filter({has: page.locator('.bi-pencil-fill')}).click();
      await page.waitForURL(new RegExp(`/empNumber/${owned.key}$`));
      await expect(field('Employee Id')).toHaveValue(owned.id);
      const confirmed = await search(owned.id);
      await confirmed.first().locator('button').filter({has: page.locator('.bi-trash')}).click();
      await page.getByRole('button', {name: /Yes, Delete/}).click();
      await page.getByText('Successfully Deleted', {exact: true}).waitFor();
      await noRecord(owned.id);
      report.cleanup = {status: 'Deleted and absence verified', ...owned};
    } catch (error) {report.cleanup = {status: 'Needs cleanup', ...owned, error: error.message};}
  } else report.cleanup = {status: 'No employee was created'};
  if (context) {
    await context.close();
    if (page?.video()) {
      const original = await page.video().path();
      await fs.rename(original, path.join(out, 'video', 'browser-recording.webm'));
      report.video = 'video/browser-recording.webm';
    }
  }
  await browser.close();
  report.finishedAt = new Date().toISOString();
  report.summary = Object.fromEntries(['Passed', 'Failed', 'Blocked'].map(status =>
    [status, report.results.filter(r => r.status === status).length]));
  report.summary.NotRun = cases.length - report.results.length;
  await writeReport();
  console.log('SUMMARY', JSON.stringify(report.summary), 'CLEANUP', report.cleanup.status, 'OUTPUT', out);
  if (report.fatal || report.summary.Passed !== cases.length || report.cleanup.status === 'Needs cleanup') process.exitCode = 1;
}
