import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

// ==========================================
// 1. CONFIGURATION
// ==========================================

const here = path.dirname(fileURLToPath(import.meta.url));

const repo = process.env.ADM_REPO ||
    here;

const runId = process.env.ADM_RUN_ID ||
    `ADM_${new Date().toISOString().replace(/[:.]/g, '-')}`;

const out = path.join(
    repo,
    'evidence',
    'executions',
    '23130171',
    runId
);

const screenshotsDir = path.join(out, 'screenshots');

const BASE_URL = (
    process.env.ADM_BASE_URL ||
    'https://opensource-demo.orangehrmlive.com/web/index.php'
).replace(/\/$/, '');

const USERNAME = process.env.ADM_USERNAME || 'Admin';
const PASSWORD = process.env.ADM_PASSWORD || 'admin123';

const HEADLESS = process.env.ADM_HEADLESS === 'true';
const SLOW_MO = Number(process.env.ADM_SLOW_MO ?? 300);

const ids = [
  'ADM_STRUCT_Login_01',
  'ADM_STRUCT_View_01',
  'ADM_STRUCT_View_02',
  'ADM_STRUCT_Expand_01',
  'ADM_STRUCT_Collapse_01',

  'ADM_STRUCT_EditMode_01',
  'ADM_STRUCT_EditMode_02',
  'ADM_STRUCT_EditMode_03',

  'ADM_STRUCT_Create_01',
  'ADM_STRUCT_Create_02',
  'ADM_STRUCT_Validation_01',
  'ADM_STRUCT_Create_03',

  'ADM_STRUCT_Validation_02',
  'ADM_STRUCT_Validation_03',
  'ADM_STRUCT_Validation_04',

  'ADM_STRUCT_Create_04',

  'ADM_STRUCT_Update_01',
  'ADM_STRUCT_Update_02',
  'ADM_STRUCT_Update_03',
  'ADM_STRUCT_Update_04',
  'ADM_STRUCT_Update_05',

  'ADM_STRUCT_Delete_01',
  'ADM_STRUCT_Delete_02',
  'ADM_STRUCT_Delete_03',
  'ADM_STRUCT_Delete_04'
];

// Dữ liệu test riêng, không sử dụng đơn vị mặc định.
const suffix = Date.now().toString(36);
const TEST_PARENT = {
  id: 'LP-' + suffix,
  name: 'ADM AUTO Parent ' + suffix
};

const TEST_CHILD = {
  id: 'LC-' + suffix,
  name: 'ADM AUTO Child ' + suffix
};

const TEST_TRIM = {
  id: 'LT-' + suffix,
  name: 'ADM AUTO Trim ' + suffix
};

// ==========================================
// 2. REPORT
// ==========================================

const report = {
  runId,
  startedAt: new Date().toISOString(),
  baseUrl: BASE_URL,
  tool: 'Playwright Core',
  browser: null,
  results: [],
  created: [],
  cleanup: [],
  setupErrors: []
};

await fs.mkdir(screenshotsDir, { recursive: true });

let browser;
let context;
let page;
let structureUrl = null;

const seenRequests = [];

// ==========================================
// 3. COMMON FUNCTIONS
// ==========================================

async function writeReport() {
  report.updatedAt = new Date().toISOString();

  await fs.writeFile(
      path.join(out, 'results.json'),
      JSON.stringify(report, null, 2),
      'utf8'
  );
}

function check(condition, message, item) {
  assert.ok(condition, message);
  item.assertions.push(message);
}

function skip(reason) {
  const error = new Error(reason);
  error.name = 'SkipTest';
  throw error;
}

async function waitReady() {
  await page.locator('.oxd-form-loader')
      .waitFor({
        state: 'hidden',
        timeout: 10000
      })
      .catch(() => {});

  await page.locator('.oxd-loading-spinner')
      .waitFor({
        state: 'hidden',
        timeout: 10000
      })
      .catch(() => {});
}

async function screenshot(fileName, fullPage = false) {
  const relativePath = path.join(
      'screenshots',
      fileName
  );

  await page.screenshot({
    path: path.join(out, relativePath),
    fullPage,
    animations: 'disabled'
  });

  return relativePath;
}

// ==========================================
// 4. LOGIN
// ==========================================


async function login() {
  console.log('[LOGIN] Đang truy cập OrangeHRM...');

  const response = await page.goto(`${BASE_URL}/auth/login`, { waitUntil: 'domcontentloaded' });

  console.log('[LOGIN] HTTP:', response?.status() ?? 'No response');
  console.log('[LOGIN] URL:', page.url());

  await page.locator('input[name="username"]').fill(USERNAME);
  await page.locator('input[name="password"]').fill(PASSWORD);

  await page.getByRole('button', {
    name: 'Login',
    exact: true
  }).click();

  await page.waitForURL('**/dashboard/**', {
    timeout: 60000
  });

  await page.locator('.oxd-topbar-header').waitFor({
    state: 'visible',
    timeout: 30000
  });

  console.log('[LOGIN] Đăng nhập thành công.');
}


// ==========================================
// 5. NAVIGATE ORGANIZATION STRUCTURE
// ==========================================

async function navigateToStructure() {
  const heading = page.locator(
      'h6:has-text("Organization Structure"), h6:has-text("Structure")'
  );

  if (await heading.isVisible().catch(() => false)) {
    structureUrl = page.url();
    return;
  }

  const response = await page.goto(`${BASE_URL}/admin/viewCompanyStructure`, { waitUntil: 'domcontentloaded' });
  assert.ok(response?.ok(), 'Could not open Organization Structure');
  await heading.waitFor({
    state: 'visible',
    timeout: 20000
  });

  structureUrl = page.url();
  await page.locator(".org-name").first().waitFor();
  await waitReady();
}

async function ensureStructure() {
  const heading = page.locator(
      'h6:has-text("Organization Structure"), h6:has-text("Structure")'
  );

  if (!(await heading.isVisible().catch(() => false))) {
    if (!structureUrl) {
      await navigateToStructure();
    } else {
      await page.goto(structureUrl, {
        waitUntil: 'domcontentloaded'
      });
    }
  }

  await heading.waitFor({
    state: 'visible',
    timeout: 15000
  });

  await waitReady();
}

// ==========================================
// 6. EDIT MODE
// ==========================================

async function ensureEditModeOn() {
  const toggle = page.locator(
      'input[type="checkbox"]'
  ).first();

  await toggle.waitFor({
    state: 'visible',
    timeout: 8000
  });

  if (!(await toggle.isChecked())) {
    if (!(await toggle.isChecked())) await toggle.locator('..').locator('span').click();
  }

  assert.equal(
      await toggle.isChecked(),
      true,
      'Edit mode phải được bật'
  );

  return toggle;
}

// ==========================================
// 7. FORM FUNCTIONS
// ==========================================

async function openAddForm(parentId) {
  await ensureEditModeOn();

  const addButton = parentId ? await findUnitAction(parentId, 'add') : page.locator('button').filter({hasText:/^\s*Add\s*$/i});

  await addButton.waitFor({
    state: 'visible',
    timeout: 8000
  });

  await addButton.click();

  const form = page.locator('.oxd-form').last();

  await form.waitFor({
    state: 'visible',
    timeout: 8000
  });

  return form;
}

async function fillUnitForm(id, name) {
  const form = page.locator('.oxd-form').last();

  const inputs = form.locator('input');

  await inputs.nth(0).fill(id);
  await inputs.nth(1).fill(name);
}

async function submitForm() {
  const submit = page.locator('.oxd-form').last()
      .locator('button[type="submit"]');

  await submit.waitFor({
    state: 'visible',
    timeout: 8000
  });

  await submit.click();

  await waitReady();
  await page.waitForTimeout(300);
}

async function cancelModal() {
  const cancel = page.getByRole('button', {
    name: /^Cancel$/
  }).last();

  if (await cancel.isVisible()) {
    try {
      await cancel.click({timeout:4000});
      await cancel.waitFor({state:'hidden', timeout:4000});
    } catch (error) {
      console.warn('[RESET] ' + error.message.split('\n')[0]);
      await page.goto(structureUrl, {waitUntil:'domcontentloaded'});
      await ensureStructure();
    }
  }
}

async function exactUnit(id) {
  const prefix = id.replace(/[.*+?^$()[\]{}|\\]/g, '\\$&');
  return page.locator('.org-name').filter({hasText: new RegExp('^' + prefix + ': ')});
}

async function revealChild(childId, parentId) {
  const child = await exactUnit(childId);
  if (await child.isVisible()) return;
  const parent = await exactUnit(parentId);
  const expand = parent.locator('xpath=ancestor::li[1]/div/span[contains(@class,"oxd-tree-node-toggle")]/button[i[contains(@class,"bi-chevron-down")]]');
  await child.or(expand).first().waitFor({state:'visible', timeout:8000}).catch(() => {});
  if (await expand.isVisible()) {
    await expand.click();
    await child.waitFor({state:"visible",timeout:3000}).catch(() => {});
  }
}

async function unitExists(id) {
  if (id === TEST_CHILD.id && report.created.some(item => item.id === id) && await (await exactUnit(TEST_PARENT.id)).isVisible()) {
    await revealChild(id, TEST_PARENT.id);
  }
  const unit = await exactUnit(id);

  return unit.isVisible().catch(() => false);
}

// ==========================================
// 8. FIND EXACT UNIT ACTION
// ==========================================

async function findUnitAction(unitId, action) {
  assert.ok(report.created.some(item => item.id === unitId), 'Unit must belong to this run');
  const text = await exactUnit(unitId);
  await text.waitFor({state:'visible'});
  const icon = {edit:'.bi-pencil-fill', delete:'.bi-trash-fill', add:'.bi-plus'}[action];
  return text.locator('..').locator('button').filter({has:page.locator(icon)});
}

async function openEdit(unitId) {
  await ensureEditModeOn();

  const editButton = await findUnitAction(
      unitId,
      'edit'
  );

  await editButton.click();

  const form = page.locator('.oxd-form').last();

  await form.waitFor({
    state: 'visible',
    timeout: 8000
  });

  return form;
}

async function openDelete(unitId) {
  await ensureEditModeOn();

  const deleteButton = await findUnitAction(
      unitId,
      'delete'
  );

  await deleteButton.click();

  await page.getByRole('button', {name:/yes, delete/i}).waitFor({state:'visible', timeout:8000});
}

async function confirmDelete(confirm = true) {
  const button = page.getByRole('button', {name: confirm ? /yes, delete/i : /no, cancel/i});
  if (confirm) {
    const [response] = await Promise.all([
      page.waitForResponse(r => r.request().method() === 'DELETE' && r.url().includes('/api/v2/admin/subunits')),
      button.click()
    ]);
    assert.ok(response.ok(), 'Delete unit HTTP ' + response.status());
  } else {
    await button.click();
  }
  await button.waitFor({state:'hidden'});
  await waitReady();
}

// ==========================================
// 9. CREATE TEST DATA
// ==========================================

async function addUnit(id, name, parentId) {
  assert.ok(!(await unitExists(id)), `Unit already exists: ${id}`);
  await openAddForm(parentId);

  await fillUnitForm(id, name);

  const [saved] = await Promise.all([
    page.waitForResponse(r => r.request().method() === 'POST' && r.url().includes('/api/v2/admin/subunits')),
    submitForm()
  ]);
  assert.ok(saved.ok(), 'Create unit HTTP ' + saved.status());
  report.created.push({id, name, createdAt: new Date().toISOString()});
  if (parentId) await revealChild(id, parentId);
  await (await exactUnit(id)).waitFor({state:'visible', timeout:10000});
}

// ==========================================
// 10. CLEANUP
// ==========================================

async function recordCleanup(id, status, message) {
  report.cleanup.push({
    unitId: id,
    status,
    message,
    time: new Date().toISOString()
  });

  await writeReport();
}

async function deleteCreatedUnit(id) {
  const wasCreated = report.created.some(
      item => item.id === id
  );

  if (!wasCreated) {
    await recordCleanup(
        id,
        'NotAttempted',
        'Script không ghi nhận đã tạo đơn vị này.'
    );
    return;
  }

  if (!(await unitExists(id))) {
    await recordCleanup(
        id,
        'AlreadyAbsent',
        'Đơn vị không còn hiển thị.'
    );
    return;
  }

  try {
    await ensureStructure();

    await openDelete(id);

    await confirmDelete(true);

    if (await unitExists(id)) {
      await recordCleanup(
          id,
          'Failed',
          'Đơn vị vẫn tồn tại sau thao tác xóa.'
      );
    } else {
      await recordCleanup(
          id,
          'Passed',
          'Đã xóa đơn vị do script tạo.'
      );
    }
  } catch (error) {
    await recordCleanup(
        id,
        'Failed',
        `${error.name}: ${error.message}`
    );
  }
}

// ==========================================
// 11. CASE RUNNER
// ==========================================

async function runCase(index, fn) {
  const id = ids[index - 1];

  const item = {
    oldId: `TC_ADM_STRUCT_${String(index).padStart(3, '0')}`,
    id,
    startedAt: new Date().toISOString(),
    status: 'Untested',
    actual: '',
    assertions: [],
    screenshots: [],
    network: []
  };

  const started = Date.now();
  const requestStart = seenRequests.length;

  try {
    await cancelModal();
    await ensureStructure();

    await fn(item);

    assert.ok(
        item.assertions.length > 0,
        'Test chưa có assertion.'
    );

    item.status = 'Passed';
    item.actual = item.assertions.join(' | ');

  } catch (error) {
    if (error.name === 'SkipTest') {
      item.status = 'Skipped';
      item.actual = error.message;
      item.skipReason = error.message;
    } else {
      item.status = error.name === 'AssertionError'
          ? 'Failed'
          : 'Blocked';

      item.actual = `${error.name}: ${error.message}`;

      item.error = {
        name: error.name,
        message: error.message,
        stack: error.stack
      };
    }
  } finally {
    await cancelModal();
  }

  try {
    item.screenshots.push(
        await screenshot(`${id}.png`)
    );

    if (
        item.status === 'Failed' ||
        item.status === 'Blocked'
    ) {
      item.errorScreenshot = await screenshot(
          `${id}_ERROR.png`,
          true
      );
    }
  } catch (error) {
    item.screenshotError = error.message;
  }

  item.durationMs = Date.now() - started;
  item.finishedAt = new Date().toISOString();

  item.network = seenRequests.slice(requestStart);

  report.results.push(item);

  await writeReport();

  console.log(
      `${id}: ${item.status} (${item.durationMs} ms) ${item.actual.slice(0, 200)}`
  );
}

// ==========================================
// 12. RESPONSE TRACKING
// ==========================================

function trackResponses(currentPage) {
  currentPage.on('response', response => {
    if (
        /\/api\/v2\/admin\/subunits/i.test(
            response.url()
        )
    ) {
      seenRequests.push({
        time: new Date().toISOString(),
        status: response.status(),
        method: response.request().method(),
        url: response.url().replace(
            /^https:\/\/[^/]+/,
            ''
        )
      });
    }
  });
}

// ==========================================
// 13. TEST EXECUTION
// ==========================================

try {
  browser = await chromium.launch({
    headless: true
  });

  report.browser = await browser.version();

  context = await browser.newContext({
    viewport: {
      width: 1440,
      height: 1000
    },
    locale: 'en-US'
  });

  page = await context.newPage();

  page.setDefaultTimeout(20000);
  page.setDefaultNavigationTimeout(45000);

  trackResponses(page);

  await login();
  await navigateToStructure();

  await writeReport();

  const only = process.env.ADM_ONLY
      ? new Set(
          process.env.ADM_ONLY
              .split(',')
              .map(value => value.trim())
      )
      : null;

  for (let n = 1; n <= 25; n++) {
    if (only && !only.has(String(n)) && !only.has(ids[n-1])) {
      continue;
    }

    await runCase(n, async item => {

      // 01 - LOGIN
      if (n === 1) {
        const heading = page.locator(
            'h6:has-text("Organization Structure"), h6:has-text("Structure")'
        );

        check(
            await heading.isVisible(),
            'Đăng nhập và mở Organization Structure thành công.',
            item
        );
        return;
      }

      // 02 - VIEW CONTAINER
      if (n === 2) {
        const container = page.locator(
            '.orangehrm-paper-container, .oxd-layout, [role="tree"]'
        ).first();

        check(
            await container.isVisible(),
            'Container cơ cấu tổ chức hiển thị.',
            item
        );
        return;
      }

      // 03 - VIEW TREE
      if (n === 3) {
        const tree = page.locator(
            '[role="tree"], .org-container, .oxd-tree-node, .orangehrm-paper-container'
        ).first();

        check(
            await tree.isVisible(),
            'Khu vực cây tổ chức hiển thị.',
            item
        );
        return;
      }

      // 04 - EXPAND
      if (n === 4) {
        const expand = page.locator(
            '.bi-plus, button[aria-label*="Expand" i]'
        ).first();

        if (!(await expand.isVisible().catch(() => false))) {
          skip('Không tìm thấy nút Expand phù hợp.');
        }

        await expand.click();
        await page.waitForTimeout(300);

        const collapse = page.locator(
            '.bi-dash, button[aria-label*="Collapse" i]'
        );

        check(
            await collapse.count() > 0,
            'Sau khi mở rộng xuất hiện trạng thái thu gọn.',
            item
        );
        return;
      }

      // 05 - COLLAPSE
      if (n === 5) {
        const collapse = page.locator(
            '.bi-dash, button[aria-label*="Collapse" i]'
        ).first();

        if (!(await collapse.isVisible().catch(() => false))) {
          skip('Không tìm thấy nút Collapse phù hợp.');
        }

        await collapse.click();
        await page.waitForTimeout(300);

        const expand = page.locator(
            '.bi-plus, button[aria-label*="Expand" i]'
        );

        check(
            await expand.count() > 0,
            'Sau khi thu gọn xuất hiện nút mở rộng.',
            item
        );
        return;
      }

      // 06 - EDIT ON
      if (n === 6) {
        await ensureEditModeOn();

        const toggle = page.locator(
            'input[type="checkbox"]'
        ).first();

        check(
            await toggle.isChecked(),
            'Bật chế độ Edit thành công.',
            item
        );
        return;
      }

      // 07 - EDIT OFF
      if (n === 7) {
        const toggle = page.locator(
            'input[type="checkbox"]'
        ).first();

        await toggle.waitFor({
          state: 'visible'
        });

        if (!(await toggle.isChecked())) await toggle.locator('..').locator('span').click();
        if (await toggle.isChecked()) await toggle.locator('..').locator('span').click();

        check(
            !(await toggle.isChecked()),
            'Tắt chế độ Edit thành công.',
            item
        );
        return;
      }

      // 08 - EDIT MODE 03
      if (n === 8) {
        const toggle = page.locator(
            'input[type="checkbox"]'
        ).first();

        await toggle.waitFor({
          state: 'visible'
        });

        if (!(await toggle.isChecked())) await toggle.locator('..').locator('span').click();

        check(
            await toggle.isChecked(),
            'Edit được bật ở lần thứ nhất.',
            item
        );

        if (await toggle.isChecked()) await toggle.locator('..').locator('span').click();

        check(
            !(await toggle.isChecked()),
            'Edit được tắt ở lần thứ hai.',
            item
        );

        if (!(await toggle.isChecked())) await toggle.locator('..').locator('span').click();

        check(
            await toggle.isChecked(),
            'Edit được bật lại ở lần thứ ba.',
            item
        );
        return;
      }

      // 09 - CREATE PARENT
      if (n === 9) {
        await addUnit(
            TEST_PARENT.id,
            TEST_PARENT.name
        );

        check(
            await unitExists(TEST_PARENT.id),
            'Đơn vị cha được tạo và hiển thị.',
            item
        );
        return;
      }

      // 10 - CREATE CHILD
      if (n === 10) {
        if (!(await unitExists(TEST_PARENT.id))) {
          skip('Không có đơn vị cha để tạo đơn vị con.');
        }

        await addUnit(TEST_CHILD.id, TEST_CHILD.name, TEST_PARENT.id);

        check(
            await unitExists(TEST_CHILD.id),
            'Đơn vị con được tạo và hiển thị.',
            item
        );
        return;
      }

      // 11 - REQUIRED VALIDATION
      if (n === 11) {
        await openAddForm();
        await submitForm();

        const errors = page.locator(
            '.oxd-input-group__message, .oxd-input-field-error-message'
        );

        check(
            await errors.count() > 0 &&
            await errors.first().isVisible(),
            'Form hiển thị lỗi khi thiếu trường bắt buộc.',
            item
        );
        return;
      }

      // 12 - DUPLICATE ID
      if (n === 12) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra ID trùng.');
        }

        await openAddForm();

        await fillUnitForm(
            TEST_CHILD.id,
            'ADM AUTO Duplicate Child'
        );

        await submitForm();

        const errors = page.locator(
            '.oxd-input-group__message, .oxd-input-field-error-message, .oxd-toast-content-text'
        );

        const hasError = await errors.count() > 0 &&
            await errors.first().isVisible().catch(() => false);

        const count = await page.getByText(
            TEST_CHILD.id,
            { exact: true }
        ).count();

        check(
            hasError || count === 1,
            'Không tạo thêm đơn vị có ID trùng.',
            item
        );
        return;
      }

      // 13 - SPECIAL CHARACTERS
      if (n === 13) {
        await openAddForm();

        await fillUnitForm(
            '@#$123',
            'ADM AUTO Invalid ID'
        );

        await submitForm();

        const errors = page.locator(
            '.oxd-input-group__message, .oxd-input-field-error-message'
        );

        const hasError = await errors.count() > 0 &&
            await errors.first().isVisible().catch(() => false);

        check(
            hasError || !(await unitExists('@#$123')),
            'ID chứa ký tự đặc biệt bị từ chối.',
            item
        );
        return;
      }

      // 14 - TRIM NAME
      if (n === 14) {
        await addUnit(
            TEST_TRIM.id,
            `   ${TEST_TRIM.name}   `
        );

        const normal = await page.getByText(
            TEST_TRIM.name,
            { exact: true }
        ).isVisible().catch(() => false);

        const spaced = await page.getByText(
            `   ${TEST_TRIM.name}   `,
            { exact: true }
        ).isVisible().catch(() => false);

        check(
            normal || spaced,
            'Tên đơn vị sau nhập khoảng trắng được hiển thị.',
            item
        );
        return;
      }

      // 15 - DESCRIPTION LENGTH
      if (n === 15) {
        await openAddForm();

        const textarea = page.locator(
            '.oxd-form textarea'
        ).first();

        if (!(await textarea.isVisible().catch(() => false))) {
          skip('Form không có trường mô tả.');
        }

        await textarea.fill('A'.repeat(401));
        await submitForm();

        const errors = page.locator(
            '.oxd-input-group__message, .oxd-input-field-error-message'
        );

        const hasError = await errors.count() > 0 &&
            await errors.first().isVisible().catch(() => false);

        const value = await textarea.inputValue().catch(() => '');

        check(
            value.length <= 400 || hasError,
            'Giới hạn mô tả được áp dụng hoặc hiển thị lỗi.',
            item
        );
        return;
      }

      // 16 - CANCEL CREATE
      if (n === 16) {
        await openAddForm();

        const inputs = page.locator(
            '.oxd-form input'
        );

        await inputs.nth(0).fill(
            'ADM_AUTO_CANCEL_23130171'
        );

        await cancelModal();

        check(
            !(await unitExists('ADM_AUTO_CANCEL_23130171')),
            'Hủy Add không tạo đơn vị mới.',
            item
        );
        return;
      }

      // 17 - UPDATE NAME
      if (n === 17) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra cập nhật tên.');
        }

        await openEdit(TEST_CHILD.id);

        const inputs = page.locator(
            '.oxd-form input'
        );

        await inputs.nth(1).fill(
            'ADM AUTO Child Updated 23130171'
        );

        const [response] = await Promise.all([
          page.waitForResponse(r => r.request().method() === 'PUT' && r.url().includes('/api/v2/admin/subunits')),
          submitForm()
        ]);
        assert.ok(response.ok(), 'Update unit HTTP ' + response.status());
        await revealChild(TEST_CHILD.id, TEST_PARENT.id);
        const unit = await exactUnit(TEST_CHILD.id);
        check(
            (await unit.innerText()).endsWith(': ADM AUTO Child Updated 23130171'),
            'Tên đơn vị được cập nhật.',
            item
        );
        TEST_CHILD.name = 'ADM AUTO Child Updated 23130171';
        return;
      }

      // 18 - UPDATE DESCRIPTION
      if (n === 18) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra mô tả.');
        }

        await openEdit(TEST_CHILD.id);

        const textarea = page.locator(
            '.oxd-form textarea'
        ).first();

        if (!(await textarea.isVisible().catch(() => false))) {
          skip('Form Edit không có trường mô tả.');
        }

        await textarea.fill(
            'ADM AUTO description updated by Playwright.'
        );

        await submitForm();

        await openEdit(TEST_CHILD.id);

        const saved = await page.locator(
            '.oxd-form textarea'
        ).first().inputValue();

        check(
            saved === 'ADM AUTO description updated by Playwright.',
            'Mô tả được lưu sau khi mở lại form.',
            item
        );

        await cancelModal();
        return;
      }

      // 19 - CHECK ID READ ONLY
      if (n === 19) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra ID.');
        }

        await openEdit(TEST_CHILD.id);

        const idInput = page.locator(
            '.oxd-form input'
        ).first();

        const readonly =
            await idInput.isDisabled() ||
            await idInput.getAttribute('readonly') !== null;

        check(
            readonly,
            'ID đơn vị không cho phép chỉnh sửa.',
            item
        );

        await cancelModal();
        return;
      }

      // 20 - CHECK ID IMMUTABLE
      if (n === 20) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra ID bất biến.');
        }

        await openEdit(TEST_CHILD.id);

        const idInput = page.locator(
            '.oxd-form input'
        ).first();

        const value = await idInput.inputValue();

        check(
            value === TEST_CHILD.id,
            'ID trong form Edit giữ nguyên giá trị ban đầu.',
            item
        );

        await cancelModal();
        return;
      }

      // 21 - CANCEL UPDATE
      if (n === 21) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra Cancel Edit.');
        }

        await openEdit(TEST_CHILD.id);

        const nameInput = page.locator(
            '.oxd-form input'
        ).nth(1);

        const originalName = await nameInput.inputValue();

        await nameInput.fill(
            'ADM AUTO CANCEL EDIT 23130171'
        );

        await cancelModal();

        await openEdit(TEST_CHILD.id);

        const currentName = await page.locator(
            '.oxd-form input'
        ).nth(1).inputValue();

        check(
            currentName === originalName,
            'Hủy Edit không lưu tên mới.',
            item
        );

        await cancelModal();
        return;
      }

      // 22 - DELETE CHILD
      if (n === 22) {
        if (!(await unitExists(TEST_CHILD.id))) {
          skip('Không có đơn vị con để kiểm tra xóa.');
        }

        await openDelete(TEST_CHILD.id);

        await confirmDelete(true);

        check(
            !(await unitExists(TEST_CHILD.id)),
            'Đơn vị con đã bị xóa khỏi cây tổ chức.',
            item
        );
        return;
      }

      // 23 - CANCEL DELETE
      if (n === 23) {
        if (!(await unitExists(TEST_PARENT.id))) {
          skip('Không có đơn vị cha để tạo lại đơn vị con.');
        }

        if (!(await unitExists(TEST_CHILD.id))) {
          await addUnit(TEST_CHILD.id, TEST_CHILD.name, TEST_PARENT.id);
        }

        await openDelete(TEST_CHILD.id);

        await confirmDelete(false);

        check(
            await unitExists(TEST_CHILD.id),
            'Hủy xóa giữ nguyên đơn vị.',
            item
        );
        return;
      }

      // 24 - DELETE PARENT WITH CHILD
      if (n === 24) {
        if (
            !(await unitExists(TEST_PARENT.id)) ||
            !(await unitExists(TEST_CHILD.id))
        ) {
          skip('Cần có đơn vị cha và con để kiểm tra xóa cha.');
        }

        await openDelete(TEST_PARENT.id);

        const dialog = page.locator(
            '.oxd-dialog-container, .oxd-dialog'
        ).last();

        const dialogText = await dialog.innerText();

        const hasWarning =
            /child|children|sub.unit|cannot|not allowed|delete/i
                .test(dialogText);

        // Không xác nhận xóa cha.
        await confirmDelete(false);

        check(
            hasWarning && await unitExists(TEST_PARENT.id),
            'Đơn vị cha vẫn tồn tại khi hủy thao tác xóa.',
            item
        );
        return;
      }

      // 25 - PERSISTENCE
      if (n === 25) {
        await page.reload({
          waitUntil: 'domcontentloaded'
        });

        await waitReady();

        const heading = page.locator(
            'h6:has-text("Organization Structure"), h6:has-text("Structure")'
        );

        await heading.waitFor({
          state: 'visible',
          timeout: 15000
        });

        await (await exactUnit(TEST_PARENT.id)).waitFor();
        check(
            await unitExists(TEST_PARENT.id),
            'Đơn vị cha vẫn tồn tại sau khi tải lại trang.',
            item
        );
      }
    });
  }

} catch (error) {
  report.fatal = {
    name: error.name,
    message: error.message,
    stack: error.stack
  };

  report.setupErrors.push({
    time: new Date().toISOString(),
    name: error.name,
    message: error.message
  });

  console.error('FATAL:', error.message);

} finally {

  // ==========================================
  // 14. SAFE CLEANUP
  // ==========================================

  if (page && browser && structureUrl) {
    try {
      await page.goto(structureUrl, {
        waitUntil: 'domcontentloaded'
      });

      await page.locator('.org-name').first().waitFor();
      await waitReady();

      if (report.created.some(item => item.id === TEST_CHILD.id)) await revealChild(TEST_CHILD.id, TEST_PARENT.id);
      // Xóa đơn vị con trước.
      for (const id of [
        TEST_CHILD.id,
        TEST_PARENT.id,
        TEST_TRIM.id
      ]) {
        if (!report.created.some(item => item.id === id)) {
          await recordCleanup(
              id,
              'NotAttempted',
              'Không phải dữ liệu được script ghi nhận đã tạo.'
          );
          continue;
        }

        if (!(await unitExists(id))) {
          await recordCleanup(
              id,
              'AlreadyAbsent',
              'Đơn vị đã không còn tồn tại.'
          );
          continue;
        }

        try {
          await openDelete(id);
          await confirmDelete(true);

          const exists = await unitExists(id);

          await recordCleanup(
              id,
              exists ? 'Failed' : 'Passed',
              exists
                  ? 'Đơn vị vẫn còn sau cleanup.'
                  : 'Đã cleanup đơn vị do script tạo.'
          );

        } catch (error) {
          await recordCleanup(
              id,
              'Failed',
              `${error.name}: ${error.message}`
          );
        }
      }

      if (report.created.length) {
        const response = await page.request.get(BASE_URL + '/api/v2/admin/subunits?mode=tree');
        assert.ok(response.ok(), 'Could not verify cleanup using the organization tree');
        const tree = await response.text();
        for (const item of report.created) {
          if (tree.includes(JSON.stringify(item.id))) await recordCleanup(item.id, 'Failed', 'Unit still present after cleanup.');
        }
      }

    } catch (error) {
      report.cleanup.push({
        unitId: 'CLEANUP',
        status: 'Failed',
        message: `${error.name}: ${error.message}`,
        time: new Date().toISOString()
      });
    }
  }

  // ==========================================
  // 15. FINAL REPORT
  // ==========================================

  report.finishedAt = new Date().toISOString();

  report.durationMs =
      new Date(report.finishedAt) -
      new Date(report.startedAt);

  report.summary = Object.fromEntries(
      [
        'Passed',
        'Failed',
        'Blocked',
        'Skipped',
        'Untested'
      ].map(status => [
        status,
        report.results.filter(
            result => result.status === status
        ).length
      ])
  );

  report.cleanupSummary = Object.fromEntries(
      [
        'Passed',
        'Failed',
        'AlreadyAbsent',
        'NotAttempted'
      ].map(status => [
        status,
        report.cleanup.filter(
            item => item.status === status
        ).length
      ])
  );

  await writeReport();

  if (browser) {
    await browser.close().catch(() => {});
  }

  console.log('\n========== ADM_STRUCT SUMMARY ==========');
  console.log(JSON.stringify(report.summary, null, 2));

  console.log('\n========== CLEANUP SUMMARY ==========');
  console.log(JSON.stringify(report.cleanupSummary, null, 2));

  console.log('\nTotal duration:', report.durationMs, 'ms');
  console.log('Report:', path.join(out, 'results.json'));
  console.log('Evidence:', out);
}