
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const repo = process.env.ADM_REPO || __dirname;

const runId =
    process.env.ADM_RUN_ID ||
    `ADM_USER_${new Date().toISOString().replace(/[:.]/g, '-')}`;

const outputDir = path.join(
    repo,
    'evidence',
    'executions',
    '23130171',
    runId
);

const screenshotDir = path.join(outputDir, 'screenshots');

const BASE_URL =
    process.env.ORANGEHRM_URL ||
    'https://opensource-demo.orangehrmlive.com/web/index.php/auth/login';

const ADMIN_USER = process.env.ORANGEHRM_ADMIN_USER || 'Admin';
const ADMIN_PASS = process.env.ORANGEHRM_ADMIN_PASS || 'admin123';

const EMPLOYEE_NAME = process.env.USER_EMPLOYEE_NAME || '';
const EXISTING_USERNAME = process.env.USER_EXISTING_USERNAME || 'Admin';
const TEST_ACCOUNT = process.env.USER_TEST_ACCOUNT || '';
assert.ok(!TEST_ACCOUNT || TEST_ACCOUNT.toLowerCase() !== ADMIN_USER.toLowerCase(), 'Use a separate test account.');

const HEADLESS = process.env.ADM_HEADLESS === 'true';
const SLOW_MO = Number(process.env.ADM_SLOW_MO || 300);

const CASE_IDS = [
  'ADM_USER_Filter_01',
  'ADM_USER_Filter_02',
  'ADM_USER_Filter_03',
  'ADM_USER_Filter_04',
  'ADM_USER_Filter_05',
  'ADM_USER_Filter_06',

  'ADM_USER_Create_01',
  'ADM_USER_Create_02',
  'ADM_USER_Create_03',
  'ADM_USER_Create_04',
  'ADM_USER_Create_05',

  'ADM_USER_Update_01',
  'ADM_USER_Update_02',

  'ADM_USER_Username_01',
  'ADM_USER_Username_02',
  'ADM_USER_Username_03',
  'ADM_USER_Username_04',

  'ADM_USER_Role_01',
  'ADM_USER_Role_02',
  'ADM_USER_Role_03',
  'ADM_USER_Role_04',

  'ADM_USER_Status_01',
  'ADM_USER_Status_02',
  'ADM_USER_Status_03',
  'ADM_USER_Status_04'
];

const report = {
  module: 'ADM_USER',
  runId,
  startTime: new Date().toISOString(),
  baseUrl: BASE_URL,
  results: [],
  summary: {},
  notes: []
};

let browser;
let activePage;
let activeContext;

const createdUsers = [];
const responseLog = [];

function uniqueName(prefix = 'AUTO_USER') {
  return `${prefix}_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
}

function check(condition, message) {
  assert.ok(condition, message);
}

function skip(message) {
  const error = new Error(message);
  error.name = 'SkipTestError';
  throw error;
}

async function exists(locator) {
  return (await locator.count()) > 0;
}

async function visible(locator) {
  try {
    return await locator.isVisible();
  } catch {
    return false;
  }
}

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {});
  await page.locator('.oxd-loading-spinner').waitFor({
    state: 'hidden',
    timeout: 15000
  }).catch(() => {});
}

async function takeScreenshot(page, filename) {
  if (!page || page.isClosed()) return null;

  const file = path.join(screenshotDir, filename);

  await page.screenshot({
    path: file,
    fullPage: false,
    animations: 'disabled'
  }).catch(() => {});

  return file;
}

async function login(page, username = ADMIN_USER, password = ADMIN_PASS) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill(password);

  await page.getByRole('button', { name: /^login$/i }).click();

  await page.waitForURL(/dashboard/, { timeout: 20000 });
  await waitReady(page);
}

async function openAdminMenu(page) {
  const adminLink = page.getByRole('link', { name: /^admin$/i });

  if (await exists(adminLink)) {
    await adminLink.click();
  } else {
    await page.goto(
        `${new URL(BASE_URL).origin}/web/index.php/admin/viewSystemUsers`
    );
  }

  await page.waitForURL(/admin/, { timeout: 15000 });
  await waitReady(page);

  await page.getByRole('heading', { name: /^system users$/i })
      .waitFor({ state: 'visible', timeout: 20000 });
  await waitReady(page);
}

async function rowByText(page, text) {
  const rows = page.locator('.oxd-table-body .oxd-table-row');
  const row = rows.filter({ has: page.getByText(text, {exact:true}) }).first();

  await row.waitFor({ state: 'visible', timeout: 10000 });
  return row;
}

async function searchUsers(page, username) {
  await userField(page, /^username$/i).fill(username);
  await submitSearch(page);
}

async function submitSearch(page) {
  const response = page.waitForResponse(r => r.request().method() === "GET" && r.url().includes("/api/v2/admin/users?"));
  await page.getByRole('button', { name: /^search$/i }).click();
  check((await response).ok(), 'User search request failed.');
  await waitReady(page);
}

async function openAddUser(page) {
  await page.locator('button').filter({hasText:/^\s*Add\s*$/i}).click();
  await page.getByRole('heading', {name:/^add user$/i}).waitFor();
  await page.locator('.oxd-form').waitFor({ state: 'visible' });
  await waitReady(page);
}

function userForm(page) {
  return page.locator('.oxd-form').last();
}

function userField(page, label) {
  return userForm(page).locator('.oxd-input-group')
      .filter({ has: page.locator('label').filter({ hasText: label }) })
      .locator('input');
}

async function selectFormOption(page, label, option) {
  const form = userForm(page);

  const group = form.locator('.oxd-input-group').filter({
    has: page.locator('label').filter({
      hasText: new RegExp(`^${label}$`, 'i')
    })
  }).first();

  const dropdown = group.locator('.oxd-select-text').first();

  await dropdown.click();

  const optionLocator = page
      .locator('.oxd-select-dropdown .oxd-select-option')
      .filter({ hasText: new RegExp(`^${option}$`, 'i') })
      .first();

  await optionLocator.waitFor({ state: 'visible', timeout: 5000 });
  await optionLocator.click();
}

async function fillUserForm(
    page,
    {
      username,
      employee,
      role = 'ESS',
      status = 'Enabled',
      password = 'Test@12345',
      confirm = password
    }
) {
  await selectFormOption(page, 'User Role', role);

  if (employee) {
    const employeeInput = userField(page, /^employee name$/i);

    await employeeInput.fill(employee);

    const suggestions = page.locator('.oxd-autocomplete-option');

    await suggestions.first().waitFor({
      state: 'visible',
      timeout: 10000
    });

    const suggestion = suggestions.filter({
      hasText: employee
    }).first();

    await suggestion.click();
  }

  if (username !== undefined) {
    await userField(page, /^username$/i).fill(username);
  }

  await selectFormOption(page, 'Status', status);

  if (password !== undefined) {
    await userField(page, /^password$/i).fill(password);
  }

  if (confirm !== undefined) {
    await userField(page, /^confirm password$/i).fill(confirm);
  }
}

async function saveForm(page) {
  const saveButton = page.getByRole('button', { name: /^save$/i });

  await saveButton.click();
  await waitReady(page);
}

async function openUserEdit(page, username) {
  const row = await rowByText(page, username);
  const buttons = row.locator('button').filter({has:page.locator('.bi-pencil, .bi-pencil-fill')});

  check(
      await buttons.count() >= 1,
      `Không tìm thấy nút thao tác cho tài khoản ${username}.`
  );

  await buttons.first().click();
  await page.locator('.oxd-form').waitFor({ state: 'visible' });

  return row;
}

async function selectEditOption(page, label, option) {
  await selectFormOption(page, label, option);
}

async function getSelectedOption(page, label) {
  const form = userForm(page);

  const group = form.locator('.oxd-input-group').filter({
    has: page.locator('label').filter({
      hasText: new RegExp(`^${label}$`, 'i')
    })
  }).first();

  return (await group.locator('.oxd-select-text').innerText()).trim();
}

async function hasValidationError(page) {
  return page.locator('.oxd-form .oxd-input-field-error-message').first()
      .waitFor({state:'visible', timeout:5000}).then(() => true, () => false);
}

async function hasToast(page) {
  return await exists(page.locator('.oxd-toast-content'));
}

async function logout(page) {
  const dropdown = page.getByRole('button', {
    name: /user dropdown/i
  });

  await dropdown.click();

  await page.getByRole('menuitem', {
    name: /logout/i
  }).click();

  await page.waitForURL(/auth\/login/, { timeout: 15000 });
}

async function createTestUser(page, username) {
  await openAddUser(page);

  await fillUserForm(page, {
    username,
    employee: EMPLOYEE_NAME,
    role: 'ESS',
    status: 'Enabled'
  });

  await saveForm(page);

  if (await page.locator('.oxd-toast-content').isVisible().catch(() => false)) {
    createdUsers.push(username);
  }

  return username;
}

async function restoreTestAccount(page, originalRole, originalStatus) {
  if (!TEST_ACCOUNT) return;

  try {
    await page.goto(
        `${new URL(BASE_URL).origin}/web/index.php/admin/viewSystemUsers`
    );

    await waitReady(page);
    await searchUsers(page, TEST_ACCOUNT);

    await openUserEdit(page, TEST_ACCOUNT);

    if (originalRole) {
      await selectEditOption(page, 'User Role', originalRole);
    }

    if (originalStatus) {
      await selectEditOption(page, 'Status', originalStatus);
    }

    await saveForm(page);

    report.notes.push(
        `Đã gửi yêu cầu khôi phục Role=${originalRole}, Status=${originalStatus} cho ${TEST_ACCOUNT}.`
    );
  } catch (error) {
    report.notes.push(
        `Không thể tự động khôi phục tài khoản ${TEST_ACCOUNT}: ${error.message}`
    );
  }
}

async function runCase(id, title, fn) {
  if (process.env.ADM_ONLY) {
    const selected = process.env.ADM_ONLY.split(',')
        .map(value => value.trim());

    const index = CASE_IDS.indexOf(id) + 1;

    if (!selected.includes(id) && !selected.includes(String(index))) {
      return;
    }
  }

  let page;
  let context;
  const started = Date.now();

  const result = {
    id,
    title,
    status: 'Not Run',
    message: '',
    durationMs: 0,
    screenshot: null
  };

  try {
    context = await browser.newContext({
      viewport: {width: 1440, height: 1000},
      locale: 'en-US'
    });

    page = await context.newPage();
    page.setDefaultTimeout(12000);

    activePage = page;
    activeContext = context;

    page.on('response', async response => {
      const url = response.url();

      if (url.includes('/api/v2/admin/users')) {
        responseLog.push({
          caseId: id,
          status: response.status(),
          url
        });
      }
    });

    await login(page);
    await openAdminMenu(page);

    await fn(page);

    result.status = 'Passed';
    result.message = 'Test case thực hiện thành công.';
  } catch (error) {
    if (error.name === 'SkipTestError') {
      result.status = 'Skipped';
      result.message = error.message;
    } else {
      result.status =
          error.name === 'AssertionError' ? 'Failed' : 'Blocked';

      result.message = error.message;
    }
  } finally {
    result.durationMs = Date.now() - started;

    if (page && !page.isClosed()) {
      const index = CASE_IDS.indexOf(id) + 1;

      result.screenshot = await takeScreenshot(
          page,
          `${String(index).padStart(2, '0')}_${id}.png`
      );
    }

    await context?.close().catch(() => {});

    report.results.push(result);

    await writeReport();

    console.log(
        `[${result.status}] ${id} - ${result.message}`
    );
  }
}

async function writeReport() {
  const counts = {
    total: report.results.length,
    passed: report.results.filter(x => x.status === 'Passed').length,
    failed: report.results.filter(x => x.status === 'Failed').length,
    blocked: report.results.filter(x => x.status === 'Blocked').length,
    skipped: report.results.filter(x => x.status === 'Skipped').length
  };

  report.summary = counts;
  report.endTime = new Date().toISOString();

  await fs.writeFile(
      path.join(outputDir, 'results.json'),
      JSON.stringify(report, null, 2),
      'utf8'
  );
}

// ------------------------------------------------------
// TEST CASES
// ------------------------------------------------------

await fs.mkdir(screenshotDir, { recursive: true });
try {
  browser = await chromium.launch({ headless: HEADLESS, slowMo: SLOW_MO });
// 01
await runCase(
    'ADM_USER_Filter_01',
    'Lọc theo Username',
    async page => {
      await searchUsers(page, EXISTING_USERNAME);

      const row = await rowByText(page, EXISTING_USERNAME);

      check(
          (await row.innerText()).includes(EXISTING_USERNAME),
          'Kết quả không chứa Username cần tìm.'
      );
    }
);

// 02
await runCase(
    'ADM_USER_Filter_02',
    'Lọc theo User Role',
    async page => {
      await selectFormOption(page, 'User Role', 'Admin');

      await submitSearch(page);

      const rows = page.locator('.oxd-table-body .oxd-table-row');

      check(await rows.count() > 0, 'Không có tài khoản Admin.');

      const firstRow = rows.first();

      check(
          (await firstRow.innerText()).includes('Admin'),
          'Kết quả không chứa Role Admin.'
      );
    }
);

// 03
await runCase(
    'ADM_USER_Filter_03',
    'Lọc theo Employee Name',
    async page => {
      if (!EMPLOYEE_NAME) {
        skip('Thiếu USER_EMPLOYEE_NAME.');
      }

      const employeeInput = userField(page, /^employee name$/i);

      await employeeInput.fill(EMPLOYEE_NAME);

      const suggestion = page.locator('.oxd-autocomplete-option')
          .filter({ hasText: EMPLOYEE_NAME })
          .first();

      await suggestion.waitFor({ state: 'visible', timeout: 10000 });
      await suggestion.click();

      await submitSearch(page);

      check(
          await page.locator('.oxd-table-body .oxd-table-row').count() > 0,
          'Không tìm thấy tài khoản theo Employee Name.'
      );
    }
);

// 04
await runCase(
    'ADM_USER_Filter_04',
    'Lọc theo Status',
    async page => {
      await selectFormOption(page, 'Status', 'Enabled');

      await submitSearch(page);

      const rows = page.locator('.oxd-table-body .oxd-table-row');

      check(await rows.count() > 0, 'Không tìm thấy tài khoản Enabled.');

      check(
          (await rows.first().innerText()).includes('Enabled'),
          'Kết quả đầu tiên không có trạng thái Enabled.'
      );
    }
);

// 05
await runCase(
    'ADM_USER_Filter_05',
    'Kết hợp nhiều điều kiện',
    async page => {
      await userField(page, /^username$/i).fill(EXISTING_USERNAME);

      await selectFormOption(page, 'User Role', 'Admin');
      await selectFormOption(page, 'Status', 'Enabled');

      await submitSearch(page);

      const row = await rowByText(page, EXISTING_USERNAME);
      const text = await row.innerText();

      check(text.includes('Admin'), 'Role không đúng.');
      check(text.includes('Enabled'), 'Status không đúng.');
    }
);

// 06
await runCase(
    'ADM_USER_Filter_06',
    'Tìm kiếm không có kết quả',
    async page => {
      const missing = uniqueName('NO_SUCH_USER');

      await searchUsers(page, missing);

      check(
          await page.getByText(/no records found/i).first().waitFor({state:'visible'}).then(() => true),
          'Hệ thống không hiển thị thông báo No Records Found.'
      );
    }
);

// 07
await runCase(
    'ADM_USER_Create_01',
    'Tạo tài khoản hợp lệ',
    async page => {
      if (!EMPLOYEE_NAME) {
        skip('Thiếu USER_EMPLOYEE_NAME để liên kết tài khoản.');
      }

      const username = uniqueName('auto_user');

      await createTestUser(page, username);

      await searchUsers(page, username);

      const row = await rowByText(page, username);

      check(
          (await row.innerText()).includes(username),
          'Tài khoản mới không xuất hiện trong danh sách.'
      );
    }
);

// 08
await runCase(
    'ADM_USER_Create_02',
    'Bỏ trống trường bắt buộc',
    async page => {
      await openAddUser(page);
      await saveForm(page);

      check(
          await hasValidationError(page),
          'Không hiển thị lỗi khi bỏ trống trường bắt buộc.'
      );
    }
);

// 09
await runCase(
    'ADM_USER_Create_03',
    'Username đã tồn tại',
    async page => {
      await openAddUser(page);
      const username = userField(page, /^username$/i);
      await username.fill(EXISTING_USERNAME);
      await username.blur();
      const error = userForm(page).locator('.oxd-input-group')
          .filter({has:page.locator('label').filter({hasText:/^username$/i})})
          .getByText(/already exists/i);
      await error.waitFor({state:'visible'});
    }
);

// 10
await runCase(
    'ADM_USER_Create_04',
    'Password và Confirm Password không khớp',
    async page => {
      if (!EMPLOYEE_NAME) {
        skip('Thiếu USER_EMPLOYEE_NAME.');
      }

      await openAddUser(page);

      await fillUserForm(page, {
        username: uniqueName('auto_mismatch'),
        employee: EMPLOYEE_NAME,
        password: 'Test@12345',
        confirm: 'Other@12345'
      });

      await saveForm(page);

      check(
          await hasValidationError(page),
          'Không hiển thị lỗi khi Password và Confirm Password không khớp.'
      );
    }
);

// 11
await runCase(
    'ADM_USER_Create_05',
    'Hủy tạo tài khoản',
    async page => {
      await openAddUser(page);

      const username = uniqueName('auto_cancel');

      await userField(page, /^username$/i).fill(username);

      await page.getByRole('button', { name: /cancel/i }).click();
      await waitReady(page);

      check(
          await page.getByText(username, { exact: true }).count() === 0,
          'Tài khoản vẫn xuất hiện sau khi hủy.'
      );
    }
);

// 12
await runCase(
    'ADM_USER_Update_01',
    'Cập nhật User Role',
    async page => {
      if (!TEST_ACCOUNT) {
        skip('Thiếu USER_TEST_ACCOUNT riêng. Không sử dụng tài khoản Admin.');
      }

      await searchUsers(page, TEST_ACCOUNT);

      await openUserEdit(page, TEST_ACCOUNT);

      const originalRole = await getSelectedOption(page, 'User Role');
      const originalStatus = await getSelectedOption(page, 'Status');

      try {
        const newRole = originalRole === 'Admin' ? 'ESS' : 'Admin';

        await selectEditOption(page, 'User Role', newRole);
        await saveForm(page);

        await searchUsers(page, TEST_ACCOUNT);

        const row = await rowByText(page, TEST_ACCOUNT);

        check(
            (await row.innerText()).includes(newRole),
            `Không cập nhật được Role sang ${newRole}.`
        );
      } finally {
        await restoreTestAccount(page, originalRole, originalStatus);
      }
    }
);

// 13
await runCase(
    'ADM_USER_Update_02',
    'Cập nhật Status',
    async page => {
      if (!TEST_ACCOUNT) {
        skip('Thiếu USER_TEST_ACCOUNT riêng.');
      }

      await searchUsers(page, TEST_ACCOUNT);

      await openUserEdit(page, TEST_ACCOUNT);

      const originalRole = await getSelectedOption(page, 'User Role');
      const originalStatus = await getSelectedOption(page, 'Status');

      try {
        const newStatus =
            originalStatus === 'Enabled' ? 'Disabled' : 'Enabled';

        await selectEditOption(page, 'Status', newStatus);
        await saveForm(page);

        await searchUsers(page, TEST_ACCOUNT);

        const row = await rowByText(page, TEST_ACCOUNT);

        check(
            (await row.innerText()).includes(newStatus),
            `Không cập nhật được Status sang ${newStatus}.`
        );
      } finally {
        await restoreTestAccount(page, originalRole, originalStatus);
      }
    }
);

// 14
await runCase(
    'ADM_USER_Username_01',
    'Username phải duy nhất',
    async page => {
      await openAddUser(page);
      const username = userField(page, /^username$/i);
      await username.fill(EXISTING_USERNAME);
      await username.blur();
      const error = userForm(page).locator('.oxd-input-group')
          .filter({has:page.locator('label').filter({hasText:/^username$/i})})
          .getByText(/already exists/i);
      await error.waitFor({state:'visible'});
    }
);

// 15
await runCase(
    'ADM_USER_Username_02',
    'Chọn Employee hợp lệ',
    async page => {
      if (!EMPLOYEE_NAME) {
        skip('Thiếu USER_EMPLOYEE_NAME.');
      }

      await openAddUser(page);

      const employeeInput = userField(page, /^employee name$/i);

      await employeeInput.fill(EMPLOYEE_NAME);

      const suggestion = page.locator('.oxd-autocomplete-option')
          .filter({ hasText: EMPLOYEE_NAME })
          .first();

      await suggestion.waitFor({ state: 'visible', timeout: 10000 });
      await suggestion.click();

      check(
          (await employeeInput.inputValue()).length > 0,
          'Employee Name chưa được chọn.'
      );
    }
);

// 16
await runCase(
    'ADM_USER_Username_03',
    'Autocomplete Employee',
    async page => {
      if (!EMPLOYEE_NAME) {
        skip('Thiếu USER_EMPLOYEE_NAME.');
      }

      await openAddUser(page);

      await userField(page, /^employee name$/i).fill(
          EMPLOYEE_NAME.slice(0, 3)
      );

      const suggestion = page.locator('.oxd-autocomplete-option').first();

      await suggestion.waitFor({ state: 'visible', timeout: 10000 });

      check(
          await suggestion.isVisible(),
          'Không hiển thị danh sách Employee autocomplete.'
      );
    }
);

// 17
await runCase(
    'ADM_USER_Username_04',
    'Không chọn Employee',
    async page => {
      await openAddUser(page);

      await userField(page, /^username$/i).fill(uniqueName('auto_no_employee'));

      await saveForm(page);

      check(
          await hasValidationError(page),
          'Không hiển thị lỗi khi bỏ trống Employee Name.'
      );
    }
);

// 18
await runCase(
    'ADM_USER_Role_01',
    'Admin truy cập chức năng quản trị',
    async page => {
      check(
          await page.getByRole('link', { name: /^admin$/i }).isVisible(),
          'Tài khoản Admin không nhìn thấy menu Admin.'
      );

      check(
          await page.getByRole('heading', { name: /system users/i }).isVisible(),
          'Không truy cập được System Users.'
      );
    }
);

// 19
await runCase(
    'ADM_USER_Role_02',
    'ESS truy cập chức năng ESS',
    async page => {
      if (!process.env.ESS_USERNAME || !process.env.ESS_PASSWORD) {
        skip('Cần ESS_USERNAME và ESS_PASSWORD riêng.');
      }

      await logout(page);

      await login(
          page,
          process.env.ESS_USERNAME,
          process.env.ESS_PASSWORD
      );

      check(/dashboard/.test(page.url()), 'ESS không vào được Dashboard.');

      check(
          await page.getByRole('link', { name: /^admin$/i }).count() === 0,
          'Tài khoản ESS vẫn nhìn thấy menu Admin.'
      );
    }
);

// 20
await runCase(
    'ADM_USER_Role_03',
    'ESS không truy cập được Admin',
    async page => {
      if (!process.env.ESS_USERNAME || !process.env.ESS_PASSWORD) {
        skip('Cần ESS_USERNAME và ESS_PASSWORD riêng.');
      }

      await logout(page);

      await login(
          page,
          process.env.ESS_USERNAME,
          process.env.ESS_PASSWORD
      );

      const adminUrl =
          `${new URL(BASE_URL).origin}/web/index.php/admin/viewSystemUsers`;

      await page.goto(adminUrl, { waitUntil: 'domcontentloaded' });
      await waitReady(page);

      check(
          !page.url().includes('/admin/viewSystemUsers'),
          'Tài khoản ESS có thể truy cập trang System Users.'
      );
    }
);

// 21
await runCase(
    'ADM_USER_Role_04',
    'Kiểm tra quyền sau khi thay đổi Role',
    async page => {
      skip(
          'Cần tài khoản ESS riêng và quy trình khôi phục quyền; không thay đổi quyền tài khoản dùng chung.'
      );
    }
);

// 22
await runCase(
    'ADM_USER_Status_01',
    'Tài khoản Enabled đăng nhập được',
    async page => {
      if (!process.env.ENABLED_USERNAME || !process.env.ENABLED_PASSWORD) {
        skip('Cần ENABLED_USERNAME và ENABLED_PASSWORD riêng.');
      }

      await logout(page);

      await login(
          page,
          process.env.ENABLED_USERNAME,
          process.env.ENABLED_PASSWORD
      );

      check(
          /dashboard/.test(page.url()),
          'Tài khoản Enabled không đăng nhập được.'
      );
    }
);

// 23
await runCase(
    'ADM_USER_Status_02',
    'Tài khoản Disabled không đăng nhập được',
    async page => {
      if (!process.env.DISABLED_USERNAME || !process.env.DISABLED_PASSWORD) {
        skip('Cần DISABLED_USERNAME và DISABLED_PASSWORD riêng.');
      }

      await logout(page);

      await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

      await page.getByPlaceholder('Username').fill(
          process.env.DISABLED_USERNAME
      );

      await page.getByPlaceholder('Password').fill(
          process.env.DISABLED_PASSWORD
      );

      await page.getByRole('button', { name: /^login$/i }).click();

      await page.waitForTimeout(3000);

      check(
          !/dashboard/.test(page.url()),
          'Tài khoản Disabled vẫn đăng nhập được.'
      );
    }
);

// 24
await runCase(
    'ADM_USER_Status_03',
    'Chuyển tài khoản Enabled sang Disabled',
    async page => {
      skip(
          'Cần tài khoản kiểm thử riêng, được phép vô hiệu hóa và khôi phục sau test.'
      );
    }
);

// 25
await runCase(
    'ADM_USER_Status_04',
    'Chuyển tài khoản Disabled sang Enabled',
    async page => {
      skip(
          'Cần tài khoản Disabled riêng, được phép kích hoạt và khôi phục sau test.'
      );
    }
);

// ------------------------------------------------------

} catch (error) {
  report.fatal = { name: error.name, message: error.message };
  console.error(error);
  process.exitCode = 1;
} finally {
// FINAL REPORT
// ------------------------------------------------------

await fs.mkdir(screenshotDir, { recursive: true });

const summary = {
  total: report.results.length,
  passed: report.results.filter(x => x.status === 'Passed').length,
  failed: report.results.filter(x => x.status === 'Failed').length,
  blocked: report.results.filter(x => x.status === 'Blocked').length,
  skipped: report.results.filter(x => x.status === 'Skipped').length
};

report.summary = summary;
report.endTime = new Date().toISOString();
report.apiResponses = responseLog;

await writeReport();

await fs.writeFile(
    path.join(outputDir, 'summary.json'),
    JSON.stringify(summary, null, 2),
    'utf8'
);

console.log('\n========== ADM_USER SUMMARY ==========');
console.log(`Total   : ${summary.total}`);
console.log(`Passed  : ${summary.passed}`);
console.log(`Failed  : ${summary.failed}`);
console.log(`Blocked : ${summary.blocked}`);
console.log(`Skipped : ${summary.skipped}`);
console.log(`Evidence: ${outputDir}`);
console.log('======================================\n');

await browser?.close();

}
