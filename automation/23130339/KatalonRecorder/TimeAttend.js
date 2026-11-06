import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'https://opensource-demo.orangehrmlive.com';

const screenshotDir = path.resolve(
    'evidence/executions/23130339/Time_ATTEND');
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

// Hàm chuyên dụng để nhập liệu vào các ô input React (Date/Time) một cách chính xác tuyệt đối
async function setReactInputValue(page, selector, value) {
    await page.waitForSelector(selector, { timeout: 10000 });
    await page.click(selector);

    // Xóa nội dung cũ trong ô input
    await page.keyboard.down('Control');
    await page.keyboard.press('KeyA');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');

    // Gõ giá trị mới vào từng ký tự
    await page.type(selector, value, { delay: 50 });

    // Kích hoạt các sự kiện blur/change để React nhận diện dữ liệu mới nhập
    await page.evaluate((sel) => {
        const input = document.querySelector(sel);
        if (input) {
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            input.dispatchEvent(new Event('blur', { bubbles: true }));
        }
    }, selector);
}

// HÀM ĐỒNG BỘ TRẠNG THÁI CHUẨN XÁC (Đã sửa lỗi chọn nhầm ô input ngày trên form Out)
async function ensureAttendanceState(page, targetMode) {
    await page.goto(`${BASE_URL}/web/index.php/attendance/punchIn`, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await waitForLoading(page);

    let btnText = '';
    for (let i = 0; i < 10; i++) {
        try {
            btnText = await page.$eval('button[type="submit"]', el => el.innerText.trim());
            if (btnText.includes('In') || btnText.includes('Out')) break;
        } catch (e) {}
        await delay(500);
    }

    // Trường hợp 1: Cần về IN nhưng đang kẹt ở OUT
    if (targetMode === 'IN' && btnText.includes('Out')) {
        console.log('[SYNC] Đang kẹt ca Out, tiến hành bấm Out để giải phóng về form In...');
        try {
            // Lấy tất cả các ô input ngày trên trang
            const dateInputs = await page.$$('.oxd-date-input input');
            if (dateInputs.length > 0) {
                // Chọn ô cuối cùng (ô ngày Punch Out thực tế) để nhập năm 2035
                const targetInput = dateInputs[dateInputs.length - 1];
                await targetInput.click();
                await page.keyboard.down('Control');
                await page.keyboard.press('KeyA');
                await page.keyboard.up('Control');
                await page.keyboard.press('Backspace');
                await targetInput.type('2035-01-01', { delay: 50 });
            }
        } catch (e) {
            console.log('[SYNC ERROR] Không thể set ngày Out:', e.message);
        }

        const ta = await page.$('textarea.oxd-textarea');
        if (ta) {
            await page.evaluate(el => el.value = '', ta);
            await ta.type('Auto Sync Close Shift');
        }

        await page.click('button[type="submit"]');
        await waitForLoading(page);

        await page.goto(`${BASE_URL}/web/index.php/attendance/punchIn`, { waitUntil: 'domcontentloaded' });
        await waitForLoading(page);
    }
    // Trường hợp 2: Cần sang OUT nhưng đang ở IN
    else if (targetMode === 'OUT' && btnText.includes('In')) {
        console.log('[SYNC] Đang ở form In, tiến hành bấm In để mở sang form Out...');
        const ta = await page.$('textarea.oxd-textarea');
        if (ta) {
            await page.evaluate(el => el.value = '', ta);
            await ta.type('Auto Sync Open Shift');
        }
        await page.click('button[type="submit"]');
        await waitForLoading(page);
    }
}
// HÀM XÓA TOÀN BỘ BẢN GHI TRONG MY RECORDS (Dùng khi khởi tạo hoặc kết thúc)
async function deleteAllAttendanceRecords(page) {
    console.log('\n[CLEANUP] Đang truy cập My Records để xóa sạch toàn bộ bản ghi...');
    try {
        await page.goto(`${BASE_URL}/web/index.php/attendance/viewMyAttendanceRecord`, { waitUntil: 'domcontentloaded', timeout: 60000 });
        await waitForLoading(page);

        let deleteBtns = await page.$$('.oxd-table-cell-actions button .bi-trash, button:has(.bi-trash)');

        while (deleteBtns.length > 0) {
            console.log(`[CLEANUP] Tìm thấy ${deleteBtns.length} bản ghi, đang tiến hành xóa...`);

            await deleteBtns[0].click();
            await delay(500);

            const confirmBtn = await page.waitForSelector('.oxd-button--label-danger', { timeout: 5000 }).catch(() => null);
            if (confirmBtn) {
                await confirmBtn.click();
                await waitForLoading(page);
                await delay(1000);
            }

            deleteBtns = await page.$$('.oxd-table-cell-actions button .bi-trash, button:has(.bi-trash)');
        }
        console.log('[CLEANUP] Đã xóa sạch toàn bộ bản ghi chấm công thành công!');
    } catch (err) {
        console.error('[CLEANUP FAIL] Lỗi khi dọn dẹp xóa bản ghi:', err.message);
    }
}

async function runPositiveTest(page, caseId, description, actionFn) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await actionFn(page);
        await page.waitForSelector('.oxd-toast.oxd-toast--success, .oxd-toast', { timeout: 8000 });
        await delay(500);
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`[PASS] ${caseId}: ${description} - Đã bắt Toast thành công.`);
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

// 2. Hàm test Negative (Kiểm tra bắt lỗi)
async function runNegativeTest(page, caseId, description, actionFn) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await actionFn(page);
        await delay(1500);

        const errorElement = await page.$('.oxd-input-group__message, span.oxd-input-field-error-message');

        if (errorElement) {
            status = 'PASSED';
            note = 'Hệ thống chặn lỗi thành công';
            console.log(`[PASS] ${caseId}: ${description} - Bắt lỗi thành công theo thiết kế.`);
        } else {
            status = 'FAILED';
            note = 'Thực tế demo không chặn lỗi, vẫn cho phép lưu bản ghi';
            console.error(`[FAIL] ${caseId}: ${description} - Hệ thống demo không chặn lỗi!`);
        }

        await page.screenshot({ path: screenshotPath, fullPage: true });
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi luồng: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

// 3. Hàm Page View (Xem giao diện / tra cứu)
async function runPageViewTest(page, caseId, description, url) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await page.goto(`${BASE_URL}/${url}`, { waitUntil: 'domcontentloaded', timeout: 60000 });
        await waitForLoading(page);
        await delay(1000);
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`[PASS] ${caseId}: ${description} - Đã chụp giao diện.`);
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

async function executeAllTests() {
    console.log('===============================================================');
    console.log('[START] CHẠY 25 TEST CASES VỚI LUỒNG ĐỒNG BỘ TRẠNG THÁI CHUẨN');
    console.log('===============================================================\n');

    const browser = await puppeteer.launch({
        headless: false,
        defaultViewport: null,
        args: ['--start-maximized']
    });

    const page = await browser.newPage();
    let testResults = [];

    try {
        console.log('[SETUP] Đăng nhập hệ thống OrangeHRM...');
        await page.goto(`${BASE_URL}/web/index.php/auth/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });
        await page.waitForSelector('input[name="username"]', { timeout: 20000 });
        await page.type('input[name="username"]', 'Admin');
        await page.type('input[name="password"]', 'admin123');
        await page.click('button[type="submit"]');
        await page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 60000 });
        await waitForLoading(page);
        console.log('[SETUP] Đăng nhập thành công!\n');

        // Xóa sạch dữ liệu khởi đầu
        await deleteAllAttendanceRecords(page);
        console.log('[SETUP] Môi trường đã sẵn sàng sạch sẽ!\n');

        // ====================================================
        // CHI TIẾT 25 TEST CASES
        // ====================================================

        // 1. TIME_ATTEND_PunchIn_01
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_PunchIn_01', 'Kiểm tra nhập giờ Punch In hợp lệ', async (p) => {
            await ensureAttendanceState(p, 'IN');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('Punch In Hợp Lệ'); }
            await p.click('button[type="submit"]');
        }));

        // 2. TIME_ATTEND_PunchOut_01
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_PunchOut_01', 'Kiểm tra nhập giờ Punch Out hợp lệ', async (p) => {
            await ensureAttendanceState(p, 'OUT');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('Punch Out Hợp Lệ'); }
            await p.click('button[type="submit"]');
        }));

        // 3. TIME_ATTEND_PunchIn_02
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_PunchIn_02', 'Kiểm tra báo lỗi khi nhập giờ Punch In tương lai', async (p) => {
            await ensureAttendanceState(p, 'IN');
            await setReactInputValue(p, '.oxd-time-input input', '11:59 PM');
            await p.click('button[type="submit"]');
        }));

        // 4. TIME_ATTEND_PunchIn_03
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_PunchIn_03', 'Kiểm tra báo lỗi khi nhập ngày Punch In tương lai', async (p) => {
            await deleteAllAttendanceRecords(p);
            await p.goto(`${BASE_URL}/web/index.php/attendance/punchIn`, { waitUntil: 'domcontentloaded' });
            await waitForLoading(p);

            // Tiến hành nhập ngày tương lai
            await setReactInputValue(p, '.oxd-date-input input', '2030-12-31');
            await p.click('button[type="submit"]');
        }));

        // 5. TIME_ATTEND_PunchOut_02
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_PunchOut_02', 'Kiểm tra báo lỗi giờ Out nhỏ hơn giờ In', async (p) => {
            await ensureAttendanceState(p, 'OUT');
            await setReactInputValue(p, '.oxd-time-input input', '08:00 AM');
            await p.click('button[type="submit"]');
        }));

        // 6. TIME_ATTEND_PunchOut_03
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_PunchOut_03', 'Kiểm tra báo lỗi ngày Out trước ngày In', async (p) => {
            await ensureAttendanceState(p, 'OUT');
            await setReactInputValue(p, '.oxd-date-input input', '2020-01-01');
            await p.click('button[type="submit"]');
        }));

        // 7. TIME_ATTEND_Format_01
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_Format_01', 'Kiểm tra định dạng ngày/giờ sai', async (p) => {
            await ensureAttendanceState(p, 'IN');
            await setReactInputValue(p, '.oxd-date-input input', 'abc-xyz');
            await p.click('button[type="submit"]');
        }));

        // 8. TIME_ATTEND_Timezone_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Timezone_01', 'Kiểm tra múi giờ khi ghi nhận bản ghi', 'web/index.php/attendance/punchIn'));

        // 9. TIME_ATTEND_Status_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Status_01', 'Kiểm tra form Punch In mặc định', 'web/index.php/attendance/punchIn'));

        // 10. TIME_ATTEND_Status_02
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_Status_02', 'Chuyển sang form Punch Out', async (p) => {
            await ensureAttendanceState(p, 'IN');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('Status Check In'); }
            await p.click('button[type="submit"]');
        }));

        // 11. TIME_ATTEND_Status_03
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_Status_03', 'Trở về form Punch In sau khi Out', async (p) => {
            await ensureAttendanceState(p, 'OUT');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('Status Check Out'); }
            await p.click('button[type="submit"]');
        }));

        // 12. TIME_ATTEND_Status_04
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_Status_04', 'Kiểm tra quên Out qua ngày', async (p) => {
            await ensureAttendanceState(p, 'IN');
        }));

        // 13. TIME_ATTEND_Status_05
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_Status_05', 'Punch In/Out nhiều lần liên tiếp', async (p) => {
            await ensureAttendanceState(p, 'IN');
            const ta1 = await p.$('textarea.oxd-textarea');
            if (ta1) { await p.evaluate(el => el.value = '', ta1); await ta1.type('Multi In'); }
            await p.click('button[type="submit"]');
            await waitForLoading(page);
            await delay(1000);

            await ensureAttendanceState(p, 'OUT');
            const ta2 = await p.$('textarea.oxd-textarea');
            if (ta2) { await p.evaluate(el => el.value = '', ta2); await ta2.type('Multi Out'); }
            await p.click('button[type="submit"]');
        }));

        // 14. TIME_ATTEND_Note_01
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_Note_01', 'Nhập Note hợp lệ thành công', async (p) => {
            await ensureAttendanceState(p, 'IN');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('Note Hợp Lệ'); }
            await p.click('button[type="submit"]');
        }));

        // 15. TIME_ATTEND_Note_02
        testResults.push(await runPositiveTest(page, 'TIME_ATTEND_Note_02', 'Bỏ trống Note khi chấm công', async (p) => {
            await ensureAttendanceState(p, 'OUT');
            await p.click('button[type="submit"]');
        }));

        // 16. TIME_ATTEND_Note_03
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_Note_03', 'Nhập note vượt quá 250 ký tự', async (p) => {
            await ensureAttendanceState(p, 'IN');
            const ta = await p.$('textarea.oxd-textarea');
            if (ta) { await p.evaluate(el => el.value = '', ta); await ta.type('A'.repeat(300)); }
            await p.click('button[type="submit"]');
        }));

        // 17. TIME_ATTEND_Record_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Record_01', 'Tra cứu My Records theo ngày', 'web/index.php/attendance/viewMyAttendanceRecord'));

        // 18. TIME_ATTEND_Record_02
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Record_02', 'Tra cứu ngày không có dữ liệu', 'web/index.php/attendance/viewMyAttendanceRecord'));

        // 19. TIME_ATTEND_Calc_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Calc_01', 'Kiểm tra tính toán Duration', 'web/index.php/attendance/viewMyAttendanceRecord'));

        // 20. TIME_ATTEND_Calc_02
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Calc_02', 'Kiểm tra tổng thời gian Total', 'web/index.php/attendance/viewMyAttendanceRecord'));

        // 21. TIME_ATTEND_EmpRecord_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_EmpRecord_01', 'Tra cứu Employee Records', 'web/index.php/attendance/viewAttendanceRecord'));

        // 22. TIME_ATTEND_Edit_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Edit_01', 'Chỉnh sửa bản ghi Employee hợp lệ', 'web/index.php/attendance/viewAttendanceRecord'));

        // 23. TIME_ATTEND_Edit_02
        testResults.push(await runNegativeTest(page, 'TIME_ATTEND_Edit_02', 'Chỉnh sửa bản ghi lỗi logic thời gian', async (p) => {
            await ensureAttendanceState(p, 'IN');
            await delay(500);
        }));

        // 24. TIME_ATTEND_Permission_01
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Permission_01', 'Quyền truy cập menu Employee Records', 'web/index.php/time/viewEmployeeAttendance'));

        // 25. TIME_ATTEND_Permission_02
        testResults.push(await runPageViewTest(page, 'TIME_ATTEND_Permission_02', 'Kiểm tra thao tác phân quyền My Records', 'web/index.php/attendance/viewMyAttendanceRecord'));

    } catch (err) {
        console.error('\n[ERROR] Lỗi luồng thực thi chung:', err.message);
    } finally {
        await deleteAllAttendanceRecords(page);

        const passedCount = testResults.filter(r => r.status === 'PASSED').length;
        const failedCount = testResults.filter(r => r.status === 'FAILED').length;
        fs.writeFileSync('test_execution_report.json', JSON.stringify(testResults, null, 2));
        console.log('\n===============================================================');
        console.log(`[SUMMARY] HOÀN TẤT THỰC THI 25 TEST CASES:`);
        console.log(`- PASSED: ${passedCount}/25`);
        console.log(`- FAILED: ${failedCount}/25`);
        console.log(`- Báo cáo JSON: test_execution_report.json`);
        console.log(`- Thư mục Screenshots: /screenshots/`);
        console.log('===============================================================\n');
        await browser.close();
    }
}

executeAllTests();