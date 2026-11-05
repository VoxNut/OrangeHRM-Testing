
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const repo =
    process.env.ADM_REPO || __dirname;

const runId =
    process.env.ADM_RUN_ID ||
    `ADM_JOB_${new Date().toISOString().replace(/[:.]/g, '-')}`;

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

const HEADLESS = process.env.ADM_HEADLESS === 'true';
const SLOW_MO = Number(process.env.ADM_SLOW_MO || 300);

const JOB_TITLE_TO_EDIT = process.env.JOB_TITLE_TO_EDIT || '';
const JOB_TITLE_TO_DELETE = process.env.JOB_TITLE_TO_DELETE || '';

const VALID_FILE = process.env.JOB_SPEC_VALID_FILE || '';
const INVALID_FILE = process.env.JOB_SPEC_INVALID_FILE || '';
const SPECIAL_FILE = process.env.JOB_SPEC_SPECIAL_FILE || '';
const LARGE_FILE = process.env.JOB_SPEC_LARGE_FILE || '';
const ORIGINAL_SPEC_FILE = process.env.JOB_SPEC_ORIGINAL_FILE || '';

const CASE_IDS = [
  'ADM_JOB_Create_01',
  'ADM_JOB_Create_02',
  'ADM_JOB_Create_03',
  'ADM_JOB_Create_04',
  'ADM_JOB_Create_05',

  'ADM_JOB_Validation_01',
  'ADM_JOB_Validation_02',
  'ADM_JOB_Validation_03',
  'ADM_JOB_Validation_04',
  'ADM_JOB_Validation_05',

  'ADM_JOB_Specification_01',
  'ADM_JOB_Specification_02',
  'ADM_JOB_Specification_03',
  'ADM_JOB_Specification_04',
  'ADM_JOB_Specification_05',
  'ADM_JOB_Specification_06',

  'ADM_JOB_Update_01',
  'ADM_JOB_Update_02',
  'ADM_JOB_Update_03',
  'ADM_JOB_Update_04',

  'ADM_JOB_Delete_01',
  'ADM_JOB_Delete_02',
  'ADM_JOB_Delete_03',
  'ADM_JOB_Delete_04',
  'ADM_JOB_Delete_05'
];

const report = {
  module: 'ADM_JOB',
  runId,
  startTime: new Date().toISOString(),
  baseUrl: BASE_URL,
  results: [],
  summary: {},
  createdJobTitles: [],
  cleanup: [],
  apiResponses: [],
  notes: []
};

let browser;

const createdTitles = [];
const responseLog = [];

function uniqueName(prefix = 'AUTO_JOB') {
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

async function waitReady(page) {
  await page.waitForLoadState('domcontentloaded').catch(() => {});

  await page.locator('.oxd-loading-spinner').waitFor({
    state: 'hidden',
    timeout: 15000
  }).catch(() => {});
}

async function screenshot(page, filename) {
  if (!page || page.isClosed()) return null;

  const file = path.join(screenshotDir, filename);

  await page.screenshot({
    path: file,
    fullPage: false,
    animations: 'disabled'
  }).catch(() => {});

  return file;
}

async function login(page) {
  await page.goto(BASE_URL, {
    waitUntil: 'domcontentloaded'
  });

  await page.getByPlaceholder('Username').fill(ADMIN_USER);
  await page.getByPlaceholder('Password').fill(ADMIN_PASS);

  await page.getByRole('button', {
    name: /^login$/i
  }).click();

  await page.waitForURL(/dashboard/, {
    timeout: 20000
  });

  await waitReady(page);
}

async function openJobTitles(page) {
  const jobMenu = page.getByRole('link', {
    name: /^job$/i
  });

  if (await exists(jobMenu)) {
    await jobMenu.click();
  } else {
    await page.goto(
        `${new URL(BASE_URL).origin}/web/index.php/admin/viewJobTitleList`
    );
  }

  await page.waitForURL(/viewJobTitleList/, {
    timeout: 15000
  });

  await waitReady(page);

  await page.getByRole('heading', { name: /^job titles$/i })
      .waitFor({ state: 'visible', timeout: 20000 });
  await page.locator('button').filter({hasText:/^\s*Add\s*$/i}).waitFor();
  await page.getByText(/Records Found/i).first().waitFor();
  await waitReady(page);
}

async function rowByText(page, title) {
  const rows = page.locator('.oxd-table-body .oxd-table-row');

  const row = rows.filter({
    hasText: title
  }).filter({
    has: page.getByText(title, { exact: true })
  }).first();

  await row.waitFor({
    state: 'visible',
    timeout: 10000
  });

  return row;
}

async function titleExists(page, title) {
  const rows = page.locator('.oxd-table-body .oxd-table-row');

  const row = rows.filter({
    has: page.getByText(title, { exact: true })
  }).first();

  return await row.count() > 0;
}

function jobField(page, label) {
  return page.locator('.oxd-form .oxd-input-group')
      .filter({ has: page.locator('label').filter({ hasText: label }) })
      .locator('input:not([type="file"]), textarea');
}

async function openAddForm(page) {
  await page.locator('button').filter({hasText:/^\s*Add\s*$/i}).click();

  await jobField(page, /^job title$/i).waitFor({
    state: 'visible'
  });

  await waitReady(page);
}

async function saveForm(page) {
  await page.getByRole('button', {
    name: /^save$/i
  }).click();

  await waitReady(page);
}

async function cancelForm(page) {
  await page.getByRole('button', {
    name: /^cancel$/i
  }).click();

  await waitReady(page);
}

async function fillJobTitle(page, title) {
  await jobField(page, /^job title$/i).fill(title);
}

async function fillDescription(page, description) {
  const field = jobField(page, /^job description$/i);

  if (await field.count()) {
    await field.fill(description);
  }
}

async function uploadSpecification(page, filePath) {
  check(
      await fs.stat(filePath).then(stat => stat.isFile()).catch(() => false),
      `Không tìm thấy file: ${filePath}`
  );

  const fileInput = page.locator('input[type="file"]');

  await fileInput.setInputFiles(filePath);
}

async function getJobTitleFromFirstRow(page) {
  const firstRow = page.locator(
      '.oxd-table-body .oxd-table-row'
  ).first();

  await firstRow.waitFor({
    state: 'visible',
    timeout: 10000
  });

  const cells = firstRow.locator('.oxd-table-cell');

  check(
      await cells.count() >= 2,
      'Không đọc được cột Job Title trong bảng.'
  );

  return (await cells.nth(1).innerText()).trim();
}

async function openEdit(page, title) {
  const row = await rowByText(page, title);

  const buttons = row.locator('button').filter({ has: page.locator('.bi-pencil, .bi-pencil-fill') });

  check(
      await buttons.count() === 1,
      `Không tìm thấy nút Edit của Job Title: ${title}`
  );

  await buttons.click();

  await jobField(page, /^job title$/i).waitFor({
    state: 'visible'
  });

  await waitReady(page);

  return row;
}

async function openDelete(page, title) {
  const row = await rowByText(page, title);

  const buttons = row.locator('button').filter({ has: page.locator('.bi-trash, .bi-trash-fill') });

  check(
      await buttons.count() === 1,
      `Không tìm thấy nút Delete của Job Title: ${title}`
  );

  await buttons.click();

  await waitReady(page);
}

async function confirmDelete(page) {
  const confirmButton = page.getByRole('button', {
    name: /yes, delete/i
  });

  await confirmButton.waitFor({
    state: 'visible',
    timeout: 5000
  });

  const response = page.waitForResponse(r => r.request().method() === "DELETE" && r.url().includes("/api/v2/admin/job-titles"));
  await confirmButton.click();
  check((await response).ok(), "Delete Job Title request failed.");
  await confirmButton.waitFor({state:"hidden"});
  await waitReady(page);
}

async function cancelDelete(page) {
  const cancelButton = page.getByRole('button', {
    name: /no, cancel/i
  });

  await cancelButton.waitFor({
    state: 'visible',
    timeout: 5000
  });

  await cancelButton.click();
  await waitReady(page);
}

async function createJob(page, title, description = '') {
  await openAddForm(page);

  await fillJobTitle(page, title);

  if (description) {
    await fillDescription(page, description);
  }

  const saved = await Promise.all([
    page.waitForResponse(r => r.request().method() === 'POST' && r.url().includes('/api/v2/admin/job-titles')),
    saveForm(page)
  ]).then(([response]) => response);
  check(saved.ok(), 'Create Job Title HTTP ' + saved.status());
  createdTitles.push(title);
  report.createdJobTitles.push(title);
  await page.waitForURL(/viewJobTitleList/);
  await rowByText(page, title);
  return true;
}

async function searchJob(page, title) {
  const searchInput = page.locator('.oxd-input').first();

  await searchInput.fill(title);

  await page.getByRole('button', {
    name: /^search$/i
  }).click();

  await waitReady(page);
}

async function getValidationErrors(page) {
  return page.locator('.oxd-input-field-error-message').allTextContents();
}

async function hasToast(page) {
  return await page.locator('.oxd-toast-content').count() > 0;
}

async function runCase(id, title, fn) {
  if (process.env.ADM_ONLY) {
    const selected = process.env.ADM_ONLY
        .split(',')
        .map(value => value.trim());

    const index = CASE_IDS.indexOf(id) + 1;

    if (!selected.includes(id) && !selected.includes(String(index))) {
      return;
    }
  }

  const started = Date.now();

  const result = {
    id,
    title,
    status: 'Not Run',
    message: '',
    durationMs: 0,
    screenshot: null,
    errorScreenshot: null
  };

  let context;
  let page;

  try {
    context = await browser.newContext({
      viewport: {width: 1440, height: 1000},
      locale: 'en-US'
    });

    page = await context.newPage();
    page.setDefaultTimeout(12000);

    page.on('response', response => {
      if (response.url().includes('/api/v2/admin/job-titles')) {
        responseLog.push({
          caseId: id,
          status: response.status(),
          url: response.url()
        });
      }
    });

    await login(page);
    await openJobTitles(page);

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

      if (page && !page.isClosed()) {
        const index = CASE_IDS.indexOf(id) + 1;

        result.errorScreenshot = await screenshot(
            page,
            `${String(index).padStart(2, '0')}_${id}_ERROR.png`
        );
      }
    }
  } finally {
    result.durationMs = Date.now() - started;

    if (page && !page.isClosed()) {
      const index = CASE_IDS.indexOf(id) + 1;

      result.screenshot = await screenshot(
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
  // Tạo thư mục báo cáo nếu chưa tồn tại
  await fs.mkdir(outputDir, { recursive: true });

  const summary = {
    total: report.results.length,
    passed: report.results.filter(x => x.status === 'Passed').length,
    failed: report.results.filter(x => x.status === 'Failed').length,
    blocked: report.results.filter(x => x.status === 'Blocked').length,
    skipped: report.results.filter(x => x.status === 'Skipped').length
  };

  report.summary = summary;
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
// 01 - Create valid Job Title
await runCase(
    'ADM_JOB_Create_01',
    'Tạo Job Title hợp lệ',
    async page => {
      const title = uniqueName('AUTO_JOB_CREATE');

      const created = await createJob(page, title);

      check(created, 'Không tìm thấy Job Title vừa tạo.');

      check(
          await titleExists(page, title),
          'Job Title không xuất hiện trong bảng.'
      );
    }
);

// 02 - Required field
await runCase(
    'ADM_JOB_Create_02',
    'Bỏ trống Job Title',
    async page => {
      await openAddForm(page);
      await saveForm(page);

      check(
          (await getValidationErrors(page)).length > 0,
          'Không hiển thị lỗi khi bỏ trống Job Title.'
      );
    }
);

// 03 - Duplicate title
await runCase(
    'ADM_JOB_Create_03',
    'Job Title trùng',
    async page => {
      const existing = await getJobTitleFromFirstRow(page);

      if (!existing) {
        skip('Không có Job Title có sẵn để kiểm tra trùng.');
      }

      await openAddForm(page);
      await fillJobTitle(page, existing);
      await saveForm(page);

      check(
          (await getValidationErrors(page)).length > 0 || await hasToast(page),
          'Hệ thống không phát hiện Job Title bị trùng.'
      );
    }
);

// 04 - Create with description
await runCase(
    'ADM_JOB_Create_04',
    'Tạo Job Title kèm mô tả',
    async page => {
      const title = uniqueName('AUTO_JOB_DESC');
      const description = 'Automation description';

      const created = await createJob(page, title, description);

      check(created, 'Không tạo được Job Title kèm mô tả.');

      const row = await rowByText(page, title);

      check(
          (await row.innerText()).includes(title),
          'Không tìm thấy Job Title trong bảng.'
      );
    }
);

// 05 - Cancel create
await runCase(
    'ADM_JOB_Create_05',
    'Hủy tạo Job Title',
    async page => {
      const title = uniqueName('AUTO_JOB_CANCEL');

      await openAddForm(page);
      await fillJobTitle(page, title);
      await cancelForm(page);

      check(
          !(await titleExists(page, title)),
          'Job Title xuất hiện dù đã hủy tạo.'
      );
    }
);

// 06 - Whitespace only
await runCase(
    'ADM_JOB_Validation_01',
    'Job Title chỉ có khoảng trắng',
    async page => {
      await openAddForm(page);
      await fillJobTitle(page, '   ');
      await saveForm(page);

      check(
          (await getValidationErrors(page)).length > 0,
          'Hệ thống chấp nhận Job Title chỉ có khoảng trắng.'
      );
    }
);

// 07 - Leading/trailing spaces
await runCase(
    'ADM_JOB_Validation_02',
    'Khoảng trắng đầu cuối',
    async page => {
      const title = uniqueName('AUTO_JOB_SPACE');
      const paddedTitle = `   ${title}   `;

      await openAddForm(page);
      await fillJobTitle(page, paddedTitle);
      await saveForm(page);

      const errors = await getValidationErrors(page);

      if (errors.length > 0) {
        check(
            true,
            'Hệ thống từ chối dữ liệu có khoảng trắng đầu cuối.'
        );
        return;
      }

      const saved = await titleExists(page, title);

      if (saved) {
        createdTitles.push(title);
        report.createdJobTitles.push(title);
      }

      check(
          saved,
          'Không tìm thấy Job Title sau khi xử lý khoảng trắng.'
      );
    }
);

// 08 - Special characters
await runCase(
    'ADM_JOB_Validation_03',
    'Ký tự đặc biệt',
    async page => {
      const title = `AUTO_JOB_#&_${Date.now()}`;

      const created = await createJob(page, title);

      check(
          created,
          'Không tạo được Job Title có ký tự đặc biệt.'
      );
    }
);

// 09 - Letters and numbers
await runCase(
    'ADM_JOB_Validation_04',
    'Chữ và số',
    async page => {
      const title = uniqueName('QA2026');

      const created = await createJob(page, title);

      check(created, 'Không tạo được Job Title gồm chữ và số.');
    }
);

// 10 - Long title
await runCase(
    'ADM_JOB_Validation_05',
    'Job Title dài',
    async page => {
      const title = `AUTO_${'LONG'.repeat(25)}_${Date.now()}`;

      await openAddForm(page);
      await fillJobTitle(page, title);
      await saveForm(page);

      const errors = await getValidationErrors(page);

      if (errors.length > 0) {
        check(true, 'Hệ thống từ chối Job Title quá dài.');
        return;
      }

      const created = await titleExists(page, title);

      if (created) {
        createdTitles.push(title);
        report.createdJobTitles.push(title);
      }

      check(
          created,
          'Không tìm thấy Job Title dài sau khi lưu.'
      );
    }
);

// 11 - Valid file upload
await runCase(
    'ADM_JOB_Specification_01',
    'Upload tệp hợp lệ',
    async page => {
      if (!VALID_FILE) {
        skip('Thiếu JOB_SPEC_VALID_FILE.');
      }

      const title = uniqueName('AUTO_JOB_FILE');

      await openAddForm(page);
      await fillJobTitle(page, title);
      await uploadSpecification(page, VALID_FILE);
      await saveForm(page);

      check(
          await titleExists(page, title),
          'Không tạo được Job Title có tệp đính kèm.'
      );

      createdTitles.push(title);
      report.createdJobTitles.push(title);
    }
);

// 12 - Unsupported file
await runCase(
    'ADM_JOB_Specification_02',
    'Upload tệp không hỗ trợ',
    async page => {
      if (!INVALID_FILE) {
        skip('Thiếu JOB_SPEC_INVALID_FILE.');
      }

      const title = uniqueName('AUTO_JOB_BADFILE');

      await openAddForm(page);
      await fillJobTitle(page, title);
      await uploadSpecification(page, INVALID_FILE);
      await saveForm(page);

      check(
          (await getValidationErrors(page)).length > 0 || await hasToast(page),
          'Không phát hiện lỗi khi upload tệp không hỗ trợ.'
      );
    }
);

// 13 - No attachment
await runCase(
    'ADM_JOB_Specification_03',
    'Không đính kèm tệp',
    async page => {
      const title = uniqueName('AUTO_JOB_NOFILE');

      const created = await createJob(page, title);

      check(
          created,
          'Không tạo được Job Title khi không đính kèm tệp.'
      );
    }
);

// 14 - Special filename
await runCase(
    'ADM_JOB_Specification_04',
    'Tên tệp có khoảng trắng và ký tự đặc biệt',
    async page => {
      if (!SPECIAL_FILE) {
        skip('Thiếu JOB_SPEC_SPECIAL_FILE.');
      }

      const title = uniqueName('AUTO_JOB_SPECIALFILE');

      await openAddForm(page);
      await fillJobTitle(page, title);
      await uploadSpecification(page, SPECIAL_FILE);
      await saveForm(page);

      check(
          await titleExists(page, title),
          'Không tạo được Job Title với tên file đặc biệt.'
      );

      createdTitles.push(title);
      report.createdJobTitles.push(title);
    }
);

// 15 - Large file
await runCase(
    'ADM_JOB_Specification_05',
    'Tệp dung lượng lớn',
    async page => {
      if (!LARGE_FILE) {
        skip('Thiếu JOB_SPEC_LARGE_FILE.');
      }

      const title = uniqueName('AUTO_JOB_LARGEFILE');

      await openAddForm(page);
      await fillJobTitle(page, title);
      await uploadSpecification(page, LARGE_FILE);
      await saveForm(page);

      const errors = await getValidationErrors(page);
      const created = await titleExists(page, title);

      check(
          errors.length > 0 || await hasToast(page) || !created,
          'Hệ thống chấp nhận tệp dung lượng lớn.'
      );

      if (created) {
        createdTitles.push(title);
        report.createdJobTitles.push(title);
      }
    }
);

// 16 - View/download specification
await runCase(
    'ADM_JOB_Specification_06',
    'Xem hoặc tải Job Specification',
    async page => {
      const title = process.env.JOB_TITLE_WITH_SPEC || '';

      if (!title) {
        skip('Thiếu JOB_TITLE_WITH_SPEC có tệp đính kèm.');
      }

      const row = await rowByText(page, title);

      await row.locator('button').first().click();
      await waitReady(page);

      check(
          await page.locator('input[type="file"]').isVisible(),
          'Không mở được form Job Title có Job Specification.'
      );

      const currentFile = page.locator('.oxd-file-input-div');

      if (await currentFile.count()) {
        check(
            await currentFile.isVisible(),
            'Không hiển thị khu vực Job Specification.'
        );
      }
    }
);

// 17 - Update title
await runCase(
    'ADM_JOB_Update_01',
    'Cập nhật tên Job Title',
    async page => {
      if (!JOB_TITLE_TO_EDIT) {
        skip('Thiếu JOB_TITLE_TO_EDIT.');
      }

      const oldTitle = JOB_TITLE_TO_EDIT;
      const newTitle = uniqueName('AUTO_JOB_RENAMED');

      await openEdit(page, oldTitle);
      await fillJobTitle(page, newTitle);
      await saveForm(page);

      check(
          await titleExists(page, newTitle),
          'Không tìm thấy Job Title sau khi đổi tên.'
      );

      // Restore title để tránh làm thay đổi dữ liệu dùng chung.
      await openEdit(page, newTitle);
      await fillJobTitle(page, oldTitle);
      await saveForm(page);

      check(
          await titleExists(page, oldTitle),
          'Không khôi phục được Job Title ban đầu.'
      );
    }
);

// 18 - Update description
await runCase(
    'ADM_JOB_Update_02',
    'Cập nhật mô tả',
    async page => {
      if (!JOB_TITLE_TO_EDIT) {
        skip('Thiếu JOB_TITLE_TO_EDIT.');
      }

      const row = await rowByText(page, JOB_TITLE_TO_EDIT);

      await row.locator('button').first().click();

      const description = jobField(page, /^job description$/i);

      const original = await description.inputValue();
      const updated = `Updated by automation ${Date.now()}`;

      try {
        await description.fill(updated);
        await saveForm(page);

        await openEdit(page, JOB_TITLE_TO_EDIT);

        check(
            await jobField(page, /^job description$/i).inputValue() === updated,
            'Mô tả mới không được lưu.'
        );
      } finally {
        // Khôi phục mô tả ban đầu.
        if (await jobField(page, /^job description$/i).count()) {
          await jobField(page, /^job description$/i).fill(original);
          await saveForm(page);
        }
      }
    }
);

// 19 - Update specification
await runCase(
    'ADM_JOB_Update_03',
    'Cập nhật Job Specification',
    async page => {
      if (!JOB_TITLE_TO_EDIT || !VALID_FILE || !ORIGINAL_SPEC_FILE) {
        skip(
            'Cần JOB_TITLE_TO_EDIT, JOB_SPEC_VALID_FILE và JOB_SPEC_ORIGINAL_FILE để có thể khôi phục tệp.'
        );
      }

      await openEdit(page, JOB_TITLE_TO_EDIT);

      try {
        await uploadSpecification(page, VALID_FILE);
        await saveForm(page);

        check(
            await titleExists(page, JOB_TITLE_TO_EDIT),
            'Không tìm thấy Job Title sau khi cập nhật Specification.'
        );
      } finally {
        if (await titleExists(page, JOB_TITLE_TO_EDIT)) {
          await openEdit(page, JOB_TITLE_TO_EDIT);
          await uploadSpecification(page, ORIGINAL_SPEC_FILE);
          await saveForm(page);
        }
      }
    }
);

// 20 - Cancel update
await runCase(
    'ADM_JOB_Update_04',
    'Hủy chỉnh sửa',
    async page => {
      if (!JOB_TITLE_TO_EDIT) {
        skip('Thiếu JOB_TITLE_TO_EDIT.');
      }

      await openEdit(page, JOB_TITLE_TO_EDIT);

      const field = jobField(page, /^job title$/i);
      const original = await field.inputValue();

      await field.fill(uniqueName('AUTO_CANCEL_EDIT'));
      await cancelForm(page);

      check(
          await titleExists(page, original),
          'Job Title ban đầu không còn sau khi hủy chỉnh sửa.'
      );
    }
);

// 21 - Delete test title
await runCase(
    'ADM_JOB_Delete_01',
    'Xóa Job Title kiểm thử',
    async page => {
      const title = uniqueName('AUTO_JOB_DELETE');

      const created = await createJob(page, title);

      check(created, 'Không tạo được Job Title để kiểm tra xóa.');

      await openDelete(page, title);
      await confirmDelete(page);

      check(
          !(await titleExists(page, title)),
          'Job Title vẫn còn sau khi xác nhận xóa.'
      );

      const index = createdTitles.indexOf(title);
      if (index >= 0) createdTitles.splice(index, 1);
    }
);

// 22 - Cancel delete
await runCase(
    'ADM_JOB_Delete_02',
    'Hủy xóa Job Title',
    async page => {
      if (!JOB_TITLE_TO_DELETE) {
        skip('Thiếu JOB_TITLE_TO_DELETE.');
      }

      await openDelete(page, JOB_TITLE_TO_DELETE);
      await cancelDelete(page);

      check(
          await titleExists(page, JOB_TITLE_TO_DELETE),
          'Job Title bị mất dù đã hủy thao tác xóa.'
      );
    }
);

// 23 - Delete multiple
await runCase(
    'ADM_JOB_Delete_03',
    'Xóa nhiều Job Titles',
    async page => {
      skip(
          'OrangeHRM Demo mặc định không cung cấp thao tác Delete Selected trong Job Titles.'
      );
    }
);

// 24 - Delete in-use title
await runCase(
    'ADM_JOB_Delete_04',
    'Xóa Job Title đang được sử dụng',
    async page => {
      skip(
          'Không xóa dữ liệu dùng chung hoặc Job Title đang được sử dụng trên demo công khai.'
      );
    }
);

// 25 - Delete and reload
await runCase(
    'ADM_JOB_Delete_05',
    'Kiểm tra sau xóa và reload',
    async page => {
      const title = uniqueName('AUTO_JOB_DELETE_RELOAD');

      const created = await createJob(page, title);

      check(created, 'Không tạo được Job Title để kiểm tra xóa.');

      await openDelete(page, title);
      await confirmDelete(page);

      await page.reload();
      await waitReady(page);

      check(
          !(await titleExists(page, title)),
          'Job Title vẫn xuất hiện sau khi reload.'
      );

      const index = createdTitles.indexOf(title);
      if (index >= 0) createdTitles.splice(index, 1);
    }
);


} catch (error) {
  report.fatal = { name: error.name, message: error.message };
  console.error(error);
  process.exitCode = 1;
} finally {
// ------------------------------------------------------
// CLEANUP
// ------------------------------------------------------

async function cleanupCreatedTitles() {
  if (createdTitles.length === 0) return;

  let context;

  try {
    context = await browser.newContext();
    const page = await context.newPage({viewport: {width: 1440, height: 1000}});
    page.setDefaultTimeout(12000);

    await login(page);
    await openJobTitles(page);

    for (const title of [...createdTitles].reverse()) {
      try {
        if (!(await titleExists(page, title))) {
          report.cleanup.push({
            title,
            status: 'Already Removed'
          });
          continue;
        }

        await openDelete(page, title);
        await confirmDelete(page);

        const removed = !(await titleExists(page, title));

        report.cleanup.push({
          title,
          status: removed ? 'Deleted' : 'Failed'
        });

        console.log(
            `[CLEANUP] ${title}: ${removed ? 'Deleted' : 'Failed'}`
        );
      } catch (error) {
        report.cleanup.push({
          title,
          status: 'Failed',
          message: error.message
        });
      }
    }

    await context.close();
  } catch (error) {
    report.notes.push(`Cleanup error: ${error.message}`);
    await context?.close().catch(() => {});
  }
}


// ------------------------------------------------------
// MAIN
// ------------------------------------------------------

  if (browser) {
    await cleanupCreatedTitles();
    await browser.close();
  }

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

  await fs.writeFile(
      path.join(outputDir, 'results.json'),
      JSON.stringify(report, null, 2),
      'utf8'
  );

  await fs.writeFile(
      path.join(outputDir, 'summary.json'),
      JSON.stringify(summary, null, 2),
      'utf8'
  );

  console.log('\n========== ADM_JOB SUMMARY ==========');
  console.log(`Total   : ${summary.total}`);
  console.log(`Passed  : ${summary.passed}`);
  console.log(`Failed  : ${summary.failed}`);
  console.log(`Blocked : ${summary.blocked}`);
  console.log(`Skipped : ${summary.skipped}`);
  console.log(`Evidence: ${outputDir}`);
  console.log('=====================================\n');
}
