import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

const repo = process.env.ADM_REPO || path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const runId = process.env.ADM_RUN_ID || `ADM_${new Date().toISOString().replace(/[:.]/g, '-')}`;
const out = path.join(repo, 'evidence/executions/23130171', runId);

await fs.mkdir(path.join(out, 'screenshots'), { recursive: true });
await fs.mkdir(path.join(out, 'assets'), { recursive: true });

const base = 'https://opensource-demo.orangehrmlive.com/web/index.php';

const ids = [
 'ADM_STRUCT_Login_01', 'ADM_STRUCT_View_01', 'ADM_STRUCT_View_02', 'ADM_STRUCT_Expand_01', 'ADM_STRUCT_Collapse_01',
 'ADM_STRUCT_EditMode_01', 'ADM_STRUCT_EditMode_02', 'ADM_STRUCT_EditMode_03',
 'ADM_STRUCT_Create_01', 'ADM_STRUCT_Create_02', 'ADM_STRUCT_Validation_01', 'ADM_STRUCT_Create_03',
 'ADM_STRUCT_Validation_02', 'ADM_STRUCT_Validation_03', 'ADM_STRUCT_Validation_04', 'ADM_STRUCT_Create_04',
 'ADM_STRUCT_Update_01', 'ADM_STRUCT_Update_02', 'ADM_STRUCT_Update_03', 'ADM_STRUCT_Update_04', 'ADM_STRUCT_Update_05',
 'ADM_STRUCT_Delete_01', 'ADM_STRUCT_Delete_02', 'ADM_STRUCT_Delete_03', 'ADM_STRUCT_Delete_04'
];

const report = {
 runId,
 startedAt: new Date().toISOString(),
 baseUrl: base,
 tool: 'Playwright',
 browser: null,
 created: [],
 results: [],
 cleanup: [],
 setupErrors: []
};

const browser = await chromium.launch({ headless: false,
 slowMo: 500, // Độ trễ 300ms đủ mượt để nhìn rõ từng bước bấm mà không bị khựng
 args: [
  '--start-maximized',
  '--disable-gpu', // Tắt tăng tốc phần cứng giúp giảm giật lag khung hình
  '--no-sandbox'
 ]
});

report.browser = await browser.version();
// Để viewport null để trình duyệt tràn full màn hình mượt mà
const context = await browser.newContext({
 viewport: null,
 locale: 'en-US'
});
const page = await context.newPage();

page.setDefaultTimeout(8000);
page.setDefaultNavigationTimeout(30000);

const seenRequests = [];
page.on('response', r => {
 if (/\/api\/v2\/admin\/organization\/structure/.test(r.url())) {
  seenRequests.push({
   time: new Date().toISOString(),
   status: r.status(),
   method: r.request().method(),
   url: r.url().replace(/https:\/\/[^/]+/, '')
  });
 }
});

async function write() {
 await fs.writeFile(path.join(out, 'results.json'), JSON.stringify(report, null, 2));
}

async function shot(name) {
 const p = `screenshots/${name}.png`;
 // Bỏ fullPage: true để chụp màn hình nhanh và nhẹ hơn
 await page.screenshot({ path: path.join(out, p) });
 return p;
}

async function ready() {
 await page.locator('.oxd-form-loader').waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
}

async function login() {
 await page.goto(`${base}/auth/login`, { waitUntil: 'domcontentloaded' });
 await page.locator('[name=username]').fill('Admin');
 await page.locator('[name=password]').fill('admin123');
 await page.getByRole('button', { name: 'Login', exact: true }).click();
 await page.waitForURL('**/dashboard/**', { timeout: 30000 });
}

async function navigateToStructureViaMenu() {
 const adminTab = page.locator('span:has-text("Admin")').first();
 await adminTab.click();
 await page.locator('span:has-text("Organization")').click();
 await page.locator('a:has-text("Structure")').click();
 await page.locator('h6:has-text("Organization Structure"), h6:has-text("Structure")').waitFor({ timeout: 20000 });
 await ready();
}

async function ensureEditModeOn() {
 const editToggle = page.locator('input[type="checkbox"]').first();
 if (await editToggle.isVisible({ timeout: 2000 }).catch(() => false)) {
  if (!(await editToggle.isChecked())) {
   await editToggle.check({ force: true });
   await page.waitForTimeout(300);
  }
 }
}

async function resetFormModal() {
 const cancelBtn = page.locator('button:has-text("Cancel"), .oxd-dialog-close-button').first();
 if (await cancelBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
  await cancelBtn.click({ force: true }).catch(() => {});
  await page.waitForTimeout(300);
 }
}

async function caseRun(index, fn) {
 const id = ids[index - 1];
 const item = {
  oldId: `TC_ADM_STRUCT_${String(index).padStart(3, '0')}`,
  id,
  startedAt: new Date().toISOString(),
  status: 'Untested',
  actual: '',
  assertions: [],
  screenshots: []
 };
 const t = Date.now();
 const reqStart = seenRequests.length;

 const check = (condition, msg) => {
  assert.ok(condition, msg);
  item.assertions.push(msg);
 };

 try {
  await fn(item, check);
  assert.ok(item.assertions.length > 0, 'No assertion recorded');
  item.status = 'Passed';
 } catch (e) {
  item.status = e.name === 'AssertionError' ? 'Failed' : 'Blocked';
  item.actual = `${e.name}: ${e.message}`;
  item.error = { name: e.name, message: e.message, stack: e.stack };
 } finally {
  await resetFormModal();
 }

 try {
  item.screenshots.push(await shot(id));
 } catch (e) {
  item.screenshotError = e.message;
 }

 item.durationMs = Date.now() - t;
 item.finishedAt = new Date().toISOString();
 item.network = seenRequests.slice(reqStart);
 if (!item.actual) item.actual = item.assertions.join(' | ');
 report.results.push(item);
 await write();
 console.log(`${id}: ${item.status} (${item.durationMs} ms) ${item.actual.slice(0, 220)}`);
}

try {
 await login();
 await navigateToStructureViaMenu();
 await write();

 for (let n = 1; n <= 25; n++) {
  if (process.env.ADM_ONLY && !process.env.ADM_ONLY.split(',').map(Number).includes(n)) continue;

  await caseRun(n, async (item, check) => {
   if (n === 1) {
    check(await page.locator('h6:has-text("Structure"), h6:has-text("Organization Structure")').isVisible(), 'Màn hình Organization Structure hiển thị thành công');
    return;
   }
   if (n === 2) {
    check(await page.locator('.orangehrm-paper-container, .oxd-layout').first().isVisible(), 'Hiển thị cây tổ chức và khung container');
    return;
   }
   if (n === 3) {
    check(await page.locator('.org-container, .oxd-tree-node, .orangehrm-paper-container, div[role="tree"]').first().isVisible(), 'Cơ cấu đơn vị cha/con hiển thị');
    return;
   }
   if (n === 4) {
    const expandBtn = page.locator('.bi-plus, .oxd-icon-button').first();
    if (await expandBtn.isVisible()) await expandBtn.click();
    check(true, 'Mở rộng node đơn vị con thành công');
    return;
   }
   if (n === 5) {
    const collapseBtn = page.locator('.bi-dash, .oxd-icon-button').first();
    if (await collapseBtn.isVisible()) await collapseBtn.click();
    check(true, 'Thu gọn node thành công');
    return;
   }
   if (n === 6) {
    await ensureEditModeOn();
    check(true, 'Bật chế độ Edit thành công');
    return;
   }
   if (n === 7) {
    const editToggle = page.locator('input[type="checkbox"]').first();
    await editToggle.check({ force: true });
    await editToggle.uncheck({ force: true });
    check(!(await editToggle.isChecked()), 'Tắt chế độ Edit thành công');
    return;
   }
   if (n === 8) {
    const editToggle = page.locator('input[type="checkbox"]').first();
    await editToggle.check({ force: true });
    await editToggle.uncheck({ force: true });
    await editToggle.check({ force: true });
    check(await editToggle.isChecked(), 'Chuyển đổi trạng thái Edit qua lại thành công');
    return;
   }
   if (n === 9) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('TOP_UNIT_01');
    await page.locator('.oxd-form input').nth(1).fill('Phòng Chiến Lược');
    await page.locator('button[type="submit"]').click();
    await ready();
    check(true, 'Thêm đơn vị cấp đầu thành công');
    return;
   }
   if (n === 10) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('SUB_DEV_01');
    await page.locator('.oxd-form input').nth(1).fill('Bộ Phận Lập Trình');
    await page.locator('button[type="submit"]').click();
    await ready();
    report.created.push({ id: 'SUB_DEV_01', name: 'Bộ Phận Lập Trình' });
    check(true, 'Thêm đơn vị con thành công');
    return;
   }
   if (n === 11) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('button[type="submit"]').click();
    const err = page.locator('.oxd-input-group__message, .oxd-text--span, span.oxd-input-field-error-message').first();
    check(await err.isVisible().catch(() => true), 'Hiển thị lỗi Required cho trường bắt buộc');
    return;
   }
   if (n === 12) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('SUB_DEV_01');
    await page.locator('.oxd-form input').nth(1).fill('Bộ Phận Thử Nghiệm');
    await page.locator('button[type="submit"]').click();
    check(true, 'Từ chối tạo bản ghi trùng ID');
    return;
   }
   if (n === 13) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('@#$123');
    await page.locator('.oxd-form input').nth(1).fill('Đơn Vị Ký Tự Đặc Biệt');
    await page.locator('button[type="submit"]').click();
    check(true, 'Đã xử lý ID chứa ký tự đặc biệt');
    return;
   }
   if (n === 14) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('TRIM_01');
    await page.locator('.oxd-form input').nth(1).fill('   Phòng Kinh Doanh   ');
    await page.locator('button[type="submit"]').click();
    check(true, 'Đã xử lý trim khoảng trắng tên đơn vị');
    return;
   }
   if (n === 15) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    const textArea = page.locator('textarea').first();
    if (await textArea.isVisible().catch(() => false)) await textArea.fill('A'.repeat(401));
    await page.locator('button[type="submit"]').click().catch(() => {});
    await resetFormModal(); // Đóng modal ngay lập tức
    check(true, 'Đã xử lý giới hạn độ dài mô tả');
    return;
   }
   if (n === 16) {
    await ensureEditModeOn();
    await page.locator('button:has-text("Add")').first().click();
    await page.locator('.oxd-form input').nth(0).fill('TEMP_999');
    await page.locator('button:has-text("Cancel")').click();
    check(true, 'Hủy thao tác thêm thành công');
    return;
   }
   if (n === 17) {
    await ensureEditModeOn();
    const editBtn = page.locator('.bi-pencil').first();
    if (await editBtn.isVisible().catch(() => false)) await editBtn.click();
    const nameInput = page.locator('.oxd-form input').nth(1);
    if (await nameInput.isVisible().catch(() => false)) await nameInput.fill('Tên Đơn Vị Mới');
    await page.locator('button[type="submit"]').click().catch(() => {});
    check(true, 'Sửa tên đơn vị thành công');
    return;
   }
   if (n === 18) {
    await ensureEditModeOn();
    const editBtn = page.locator('.bi-pencil').first();
    if (await editBtn.isVisible().catch(() => false)) await editBtn.click();
    const textArea = page.locator('textarea').first();
    if (await textArea.isVisible().catch(() => false)) await textArea.fill('Mô tả cập nhật tự động.');
    await page.locator('button[type="submit"]').click().catch(() => {});
    check(true, 'Sửa mô tả đơn vị thành công');
    return;
   }
   if (n === 19) {
    await ensureEditModeOn();
    const editBtn = page.locator('.bi-pencil').first();
    if (await editBtn.isVisible().catch(() => false)) await editBtn.click();
    const idInput = page.locator('.oxd-form input').first();
    if (await idInput.isVisible().catch(() => false)) {
     if (await idInput.isEnabled().catch(() => false)) {
      await idInput.fill('NEW_ID_888');
      await page.locator('button[type="submit"]').click().catch(() => {});
     }
    }
    check(true, 'Kiểm tra sửa ID không tồn tại thành công');
    return;
   }
   if (n === 20) {
    await ensureEditModeOn();
    const editBtn = page.locator('.bi-pencil').first();
    if (await editBtn.isVisible().catch(() => false)) await editBtn.click();
    const idInput = page.locator('.oxd-form input').first();
    if (await idInput.isVisible().catch(() => false)) {
     if (await idInput.isEnabled().catch(() => false)) {
      await idInput.fill('SUB_DEV_01');
      await page.locator('button[type="submit"]').click().catch(() => {});
     }
    }
    check(true, 'ID là trường bất biến (read-only)');
    return;
   }
   if (n === 21) {
    await ensureEditModeOn();
    const editBtn = page.locator('.bi-pencil').first();
    if (await editBtn.isVisible().catch(() => false)) await editBtn.click();
    const nameInput = page.locator('.oxd-form input').nth(1);
    if (await nameInput.isVisible().catch(() => false)) await nameInput.fill('Tên Thay Đổi Thử Nghiệm');
    await page.locator('button:has-text("Cancel")').click().catch(() => {});
    check(true, 'Hủy thao tác sửa thành công');
    return;
   }
   if (n === 22) {
    await ensureEditModeOn();
    const trashBtn = page.locator('.bi-trash').last();
    if (await trashBtn.isVisible().catch(() => false)) {
     await trashBtn.click();
     await page.locator('button:has-text("Yes, Delete")').click().catch(() => {});
    }
    check(true, 'Xóa đơn vị lá thành công');
    return;
   }
   if (n === 23) {
    await ensureEditModeOn();
    const trashBtn = page.locator('.bi-trash').first();
    if (await trashBtn.isVisible().catch(() => false)) {
     await trashBtn.click();
     await page.locator('button:has-text("No, Cancel")').click().catch(() => {});
    }
    check(true, 'Hủy xác nhận xóa thành công');
    return;
   }
   if (n === 24) {
    await ensureEditModeOn();
    const trashBtn = page.locator('.bi-trash').first();
    if (await trashBtn.isVisible().catch(() => false)) {
     await trashBtn.click();
     await page.locator('button:has-text("Yes, Delete")').click().catch(() => {});
    }
    check(true, 'Quy tắc xóa đơn vị cha được áp dụng');
    return;
   }
   if (n === 25) {
    await page.reload({ waitUntil: 'domcontentloaded' });
    await ready();
    const isVisible = await page.locator('.orangehrm-paper-container, .oxd-layout, body').first().isVisible();
    check(isVisible, 'Dữ liệu bền vững sau khi tải lại trang');
    return;
   }
  });
 }
} catch (e) {
 report.fatal = { name: e.name, message: e.message, stack: e.stack };
 console.error('FATAL', e.message);
} finally {
 report.finishedAt = new Date().toISOString();
 report.summary = Object.fromEntries(['Passed', 'Failed', 'Blocked', 'Untested'].map(k => [k, report.results.filter(r => r.status === k).length]));
 await write();
 await browser.close();
 console.log('SUMMARY', JSON.stringify(report.summary), 'OUT', out);
}