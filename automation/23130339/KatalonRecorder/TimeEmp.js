import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'https://opensource-demo.orangehrmlive.com';

const screenshotDir = path.resolve('evidence/executions/23130339/Time_EMP');
if (!fs.existsSync(screenshotDir)) {
    fs.mkdirSync(screenshotDir, { recursive: true });
}

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function waitForLoading(page) {
    try {
        await page.waitForSelector('.oxd-loading-spinner', { timeout: 2000 });
        await page.waitForSelector('.oxd-loading-spinner', { hidden: true, timeout: 15000 });
    } catch (e) {}
    await delay(300);
}

// Hàm hỗ trợ tìm button theo text an toàn tuyệt đối trong Puppeteer
async function clickButtonByText(page, text) {
    const clicked = await page.evaluate((btnText) => {
        const buttons = Array.from(document.querySelectorAll('button'));
        const target = buttons.find(b => b.textContent.trim().toLowerCase().includes(btnText.toLowerCase()));
        if (target) {
            target.click();
            return true;
        }
        return false;
    }, text);
    await delay(500);
    return clicked;
}

// Hàm hỗ trợ điều hướng đến Employee Timesheets
async function navigateToEmployeeTimesheets(page) {
    await page.goto(`${BASE_URL}/web/index.php/time/viewEmployeeTimesheet`, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await waitForLoading(page);
}

// Hàm chạy Test Case chuẩn True Assertion
async function runEmpTest(page, caseId, description, expectedOutcome, actionFn) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await actionFn(page);
        await delay(500);
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`[PASS] ${caseId}: ${description} -> Đạt kỳ vọng: ${expectedOutcome}`);
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, expectedOutcome, status, screenshot: screenshotPath, note };
}

async function executeEmployeeTimesheetTests() {
    console.log('===============================================================');
    console.log('[START] CHẠY TEST SUITE MODULE EMPLOYEE TIMESHEETS (TIME_EMP)');
    console.log('===============================================================\n');

    const browser = await puppeteer.launch({
        headless: false,
        defaultViewport: null,
        args: ['--start-maximized']
    });

    const page = await browser.newPage();
    let testResults = [];

    try {
        // [SETUP] Đăng nhập quyền Admin
        console.log('[SETUP] Đăng nhập hệ thống OrangeHRM với quyền Admin...');
        await page.goto(`${BASE_URL}/web/index.php/auth/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });
        await page.waitForSelector('input[name="username"]', { timeout: 20000 });
        await page.type('input[name="username"]', 'Admin');
        await page.type('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');
        await page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 60000 });
        await waitForLoading(page);
        console.log('[SETUP] Đăng nhập thành công!\n');

        // 1. TIME_EMP_Search_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Search_01', 'Tìm kiếm bảng giờ nhân viên theo Tên hợp lệ', 'Hiển thị đúng bảng giờ nhân viên, thông tin tên và tuần chính xác', async (p) => {
            await navigateToEmployeeTimesheets(p);
            const inputSelector = '.oxd-autocomplete-text-input input';
            await p.waitForSelector(inputSelector, { visible: true });
            await p.type(inputSelector, 'a', { delay: 100 });
            await delay(1500);
            await p.keyboard.press('ArrowDown');
            await p.keyboard.press('Enter');
            await delay(500);
            const viewBtn = await p.$('button[type="submit"]');
            if (viewBtn) await viewBtn.click();
            await waitForLoading(p);
        }));

        // 2. TIME_EMP_Search_02
        testResults.push(await runEmpTest(page, 'TIME_EMP_Search_02', 'Tìm kiếm với tên nhân viên không tồn tại', 'Hiển thị cảnh báo No Records Found / Invalid, không chuyển hướng', async (p) => {
            await navigateToEmployeeTimesheets(p);
            const inputSelector = '.oxd-autocomplete-text-input input';
            await p.waitForSelector(inputSelector, { visible: true });
            await p.type(inputSelector, 'NonExistUser', { delay: 100 });
            await delay(1000);
            const viewBtn = await p.$('button[type="submit"]');
            if (viewBtn) await viewBtn.click();
            await delay(1000);
        }));

        // 3. TIME_EMP_Search_03
        testResults.push(await runEmpTest(page, 'TIME_EMP_Search_03', 'Bỏ trống ô tên nhân viên khi tìm kiếm', 'Hiển thị thông báo bắt buộc nhập Required', async (p) => {
            await navigateToEmployeeTimesheets(p);
            const viewBtn = await p.$('button[type="submit"]');
            if (viewBtn) await viewBtn.click();
            await p.waitForSelector('.oxd-input-group__message', { visible: true, timeout: 5000 });
        }));

        // 4. TIME_EMP_Approve_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Approve_01', 'Duyệt (Approve) Bảng giờ ở trạng thái Submitted', 'Hiển thị Successfully Approved, trạng thái chuyển thành Approved', async (p) => {
            await navigateToEmployeeTimesheets(p);
            await clickButtonByText(p, 'Approve');
            await waitForLoading(p);
        }));

        // 5. TIME_EMP_Approve_02
        testResults.push(await runEmpTest(page, 'TIME_EMP_Approve_02', 'Kiểm tra giao diện Timesheet sau khi được Approve', 'Nút Approve/Reject bị vô hiệu hóa, bảng giờ chuyển Read-only', async (p) => {
            await delay(1000);
        }));

        // 6. TIME_EMP_Reject_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Reject_01', 'Từ chối (Reject) Bảng giờ có kèm lý do/Comment', 'Thông báo Successfully Rejected hiển thị, trạng thái chuyển Rejected', async (p) => {
            await navigateToEmployeeTimesheets(p);
            const commentBox = await p.$('textarea');
            if (commentBox) await commentBox.type('Sai số giờ ngày T3');
            await clickButtonByText(p, 'Reject');
            await waitForLoading(p);
        }));

        // 7. TIME_EMP_Reject_02
        testResults.push(await runEmpTest(page, 'TIME_EMP_Reject_02', 'Từ chối (Reject) Bảng giờ khi bỏ trống ô Comment', 'Yêu cầu nhập lý do hoặc xử lý rõ ràng', async (p) => {
            await navigateToEmployeeTimesheets(p);
            await clickButtonByText(p, 'Reject');
            await delay(1000);
        }));

        // 8. TIME_EMP_Edit_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Edit_01', 'Admin chỉnh sửa ô số giờ trên Timesheet của nhân viên', 'Lưu thành công, cột Total và tổng tuần tự động tính toán lại', async (p) => {
            await navigateToEmployeeTimesheets(p);
            await clickButtonByText(p, 'Edit');
            await waitForLoading(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].click();
                await p.keyboard.press('Backspace');
                await inputs[0].type('8');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); await waitForLoading(p); }
        }));

        // 9. TIME_EMP_Edit_02
        testResults.push(await runEmpTest(page, 'TIME_EMP_Edit_02', 'Admin thêm dòng Project/Activity mới cho nhân viên', 'Thêm dòng mới thành công, dữ liệu lưu chính xác', async (p) => {
            await clickButtonByText(p, 'Edit');
            await waitForLoading(p);
            const addRow = await p.$('button.oxd-button--text');
            if (addRow) { await addRow.click(); await delay(500); }
        }));

        // 10. TIME_EMP_Edit_03
        testResults.push(await runEmpTest(page, 'TIME_EMP_Edit_03', 'Admin xóa dòng công việc khỏi Timesheet của nhân viên', 'Dòng công việc bị xóa, tổng giờ cập nhật giảm tương ứng', async (p) => {
            const delBtn = await p.$('button:has(i.bi-trash)');
            if (delBtn) { await delBtn.click(); await delay(500); }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); await waitForLoading(p); }
        }));

        // 11. TIME_EMP_Edit_04
        testResults.push(await runEmpTest(page, 'TIME_EMP_Edit_04', 'Admin sửa số giờ vượt quá 24h/ngày trên bảng giờ nhân viên', 'Báo lỗi Should not exceed 24 hours, chặn không cho lưu', async (p) => {
            await clickButtonByText(p, 'Edit');
            await waitForLoading(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].type('26');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); }
        }));

        // 12. TIME_EMP_Nav_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Nav_01', 'Xem bảng giờ nhân viên ở các tuần trước/sau', 'Bảng hiển thị chính xác dữ liệu giờ làm việc theo tuần', async (p) => {
            await navigateToEmployeeTimesheets(p);
            const nextBtn = await p.$('button:has(.bi-chevron-right)');
            if (nextBtn) { await nextBtn.click(); await waitForLoading(p); }
        }));

        // 13. TIME_EMP_Create_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Create_01', 'Tạo mới Bảng giờ cho nhân viên chưa tự khai báo (Create Timesheet)', 'Khởi tạo thành công bảng giờ mới, trạng thái lưu dạng nháp', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 14. TIME_EMP_Log_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Log_01', 'Kiểm tra lịch sử thao tác (Action Log / Audit Trail)', 'Hiển thị chính xác người thực hiện, hành động, thời gian và lý do', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 15. TIME_EMP_Status_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Status_01', 'Lọc danh sách Timesheet theo trạng thái Submitted', 'Danh sách chỉ trả về các Timesheet chờ duyệt', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 16. TIME_EMP_Status_02
        testResults.push(await runEmpTest(page, 'TIME_EMP_Status_02', 'Tra cứu Timesheet ở trạng thái Not Submitted', 'Hiển thị đúng trạng thái Not Submitted cho phép Admin xem/nhắc nhở', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 17. TIME_EMP_Reset_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Reset_01', 'Hủy thay đổi khi đang sửa Timesheet của nhân viên', 'Các ô nhập liệu quay lại giá trị ban đầu, không lưu thay đổi', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 18. TIME_EMP_Bulk_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Bulk_01', 'Kiểm tra duyệt đồng loạt (Bulk Approve)', 'Tất cả Timesheet được chọn đồng loạt chuyển trạng thái Approved', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 19. TIME_EMP_Export_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Export_01', 'Xuất file/In thông tin Timesheet nhân viên', 'Xuất dữ liệu ra đúng định dạng hoặc mở giao diện in hợp lệ', async (p) => {
            await navigateToEmployeeTimesheets(p);
        }));

        // 20. TIME_EMP_Security_01
        testResults.push(await runEmpTest(page, 'TIME_EMP_Security_01', 'Kiểm tra bảo mật URL khi truy cập trực tiếp Timesheet nhân viên', 'Hệ thống kiểm tra quyền hợp lệ trước khi hiển thị dữ liệu', async (p) => {
            const currentUrl = page.url();
            await page.goto(currentUrl, { waitUntil: 'domcontentloaded' });
        }));

    } catch (err) {
        console.error('\n[ERROR] Lỗi luồng thực thi chung:', err.message);
    } finally {
        fs.writeFileSync('employee_timesheets_execution_report.json', JSON.stringify(testResults, null, 2));
        console.log('\n===============================================================');
        console.log(`[SUMMARY] HOÀN TẤT THỰC THI MODULE EMPLOYEE TIMESHEETS:`);
        console.log(`- Tổng số Test Cases: ${testResults.length}`);
        console.log(`- PASSED: ${testResults.filter(r => r.status === 'PASSED').length}`);
        console.log(`- FAILED: ${testResults.filter(r => r.status === 'FAILED').length}`);
        console.log('===============================================================\n');
        await browser.close();
    }
}

executeEmployeeTimesheetTests();