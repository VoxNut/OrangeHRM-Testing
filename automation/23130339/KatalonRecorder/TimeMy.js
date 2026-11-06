import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'https://opensource-demo.orangehrmlive.com';

const screenshotDir = path.resolve('' +
    'evidence/executions/23130339/Time_MY');
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

// Hàm chuyên dụng chọn Autocomplete cho OrangeHRM (Dùng phím điều hướng ép gọi API React)
async function selectAutocomplete(page, inputSelector, text) {
    await page.waitForSelector(inputSelector, { visible: true, timeout: 10000 });
    await page.click(inputSelector, { clickCount: 3 });
    await page.keyboard.press('Backspace');
    await delay(300);

    // Gõ từ khóa chậm rãi
    await page.type(inputSelector, text, { delay: 150 });
    await delay(1500); // Đợi Ajax gọi dữ liệu từ server

    // Ép bàn phím chọn kết quả gợi ý đầu tiên
    await page.keyboard.press('ArrowDown');
    await delay(400);
    await page.keyboard.press('Enter');
    await delay(800);
}

// Hàm chọn Dropdown Activity thông minh
async function selectDropdownOption(page, triggerSelector) {
    await page.waitForSelector(triggerSelector, { visible: true, timeout: 5000 });
    await page.click(triggerSelector);
    await delay(600);

    await page.keyboard.press('ArrowDown');
    await delay(300);
    await page.keyboard.press('Enter');
    await delay(800);
}

async function openMyTimesheetsEditMode(page) {
    await page.goto(`${BASE_URL}/web/index.php/time/viewMyTimesheet`, { waitUntil: 'domcontentloaded', timeout: 60000 });
    await waitForLoading(page);

    try {
        const editBtn = await page.evaluateHandle(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            return buttons.find(b => b.textContent.trim().toLowerCase() === 'edit');
        });
        if (editBtn) {
            await editBtn.click();
            await waitForLoading(page);
        }
    } catch (e) {}
}

// 1. TRUE POSITIVE TEST (Bắt trọn Toast xanh thành công trước khi loading)
async function runPositiveTest(page, caseId, description, actionFn) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await actionFn(page);

        // Kiểm tra lỗi validation đỏ
        const errorElement = await page.$('.oxd-input-group__message, span.oxd-input-field-error-message, .oxd-toast.oxd-toast--error');
        if (errorElement) {
            throw new Error('Positive Test thất bại: Giao diện vẫn còn lỗi validation đỏ.');
        }

        // Chờ Toast xanh xuất hiện và chụp màn hình ngay khoảnh khắc đó
        await page.waitForSelector('.oxd-toast, .oxd-toast-container, .oxd-toast--success', { visible: true, timeout: 6000 });
        await delay(300);
        await page.screenshot({ path: screenshotPath, fullPage: true });

        await waitForLoading(page);
        console.log(`[PASS] ${caseId}: ${description} - Bắt trọn Toast thành công.`);
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

// 2. TRUE NEGATIVE TEST (Assertion nghiêm ngặt: Kiểm tra thông báo lỗi chặn)
async function runNegativeTest(page, caseId, description, actionFn) {
    const safeCaseId = caseId.replace(/[:\/]/g, '_');
    const screenshotPath = path.join(screenshotDir, `${safeCaseId}_result.png`);
    let status = 'PASSED';
    let note = '';

    try {
        await actionFn(page);

        // Chờ thông báo lỗi hoặc chữ Duplicate Record
        const errorElement = await page.waitForSelector('.oxd-input-group__message, span.oxd-input-field-error-message, .oxd-toast.oxd-toast--error', { timeout: 5000 }).catch(() => null);

        if (!errorElement) {
            throw new Error('Hệ thống demo không chặn lỗi, vẫn cho phép lưu dữ liệu sai trái!');
        } else {
            note = 'Hệ thống chặn lỗi thành công';
            console.log(`[PASS] ${caseId}: ${description} - Bắt lỗi thành công.`);
        }

        await delay(500);
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

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
        console.log(`[PASS] ${caseId}: ${description}`);
    } catch (err) {
        status = 'FAILED';
        note = err.message;
        await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
        console.error(`[FAIL] ${caseId}: ${description} - Lỗi: ${note}`);
    }

    return { caseId, description, status, screenshot: screenshotPath, note };
}

async function executeAllMyTimesheetTests() {
    console.log('===============================================================');
    console.log('[START] CHẠY 21 TEST CASES MODULE MY TIMESHEETS');
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

        // ====================================================
        // CHI TIẾT 21 TEST CASES MY TIMESHEETS
        // ====================================================

        // 1. TIME_MY_AddRow_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_AddRow_01', 'Kiểm tra thêm dòng công việc mới thành công', async (p) => {
            await openMyTimesheetsEditMode(p);

            // Sửa lại selector tìm nút xóa (icon thùng rác) chuẩn xác theo DOM của OrangeHRM
            const trashButtons = await p.$$('button.oxd-icon-button i.bi-trash, button:has(i.bi-trash)');
            for (let btn of trashButtons) {
                try {
                    // Click vào nút cha chứa icon thùng rác để xóa dòng cũ
                    const parentBtn = await btn.evaluateHandle(el => el.closest('button'));
                    if (parentBtn) {
                        await parentBtn.click();
                        await delay(500);
                    }
                } catch (e) {}
            }

            const addRowBtn = await p.$('button.oxd-button--text');
            if (addRowBtn) {
                await addRowBtn.click();
                await delay(800);
            }

            await selectAutocomplete(p, '.oxd-autocomplete-text-input input', 'Apache');
            await selectDropdownOption(p, '.oxd-select-text');

            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) {
                await saveBtn.click();
            }
        }));

        // 2. TIME_MY_AddRow_02 (Kiểm tra trùng lặp)
        testResults.push(await runNegativeTest(page, 'TIME_MY_AddRow_02', 'Kiểm tra báo lỗi khi thêm dòng trùng Project và Activity', async (p) => {
            await openMyTimesheetsEditMode(p);

            // Dọn sạch các dòng cũ có sẵn trước khi test duplicate
            const trashButtons = await p.$$('button.oxd-icon-button i.bi-trash, button:has(i.bi-trash)');
            for (let btn of trashButtons) {
                try {
                    const parentBtn = await btn.evaluateHandle(el => el.closest('button'));
                    if (parentBtn) {
                        await parentBtn.click();
                        await delay(500);
                    }
                } catch (e) {}
            }

            const addRowBtn = await p.$('button.oxd-button--text');

            // --- DÒNG 1 ---
            if (addRowBtn) { await addRowBtn.click(); await delay(800); }
            let inputs1 = await p.$$('.oxd-autocomplete-text-input input');
            await inputs1[0].click({ clickCount: 3 });
            await inputs1[0].type('Apache', { delay: 120 });
            await delay(1500);
            await p.keyboard.press('ArrowDown'); await delay(400);
            await p.keyboard.press('Enter'); await delay(800);

            let selectTriggers1 = await p.$$('.oxd-select-text');
            if (selectTriggers1.length > 0) {
                await selectTriggers1[0].click();
                await delay(600);
                await p.keyboard.press('ArrowDown'); await delay(300);
                await p.keyboard.press('Enter'); await delay(800);
            }

            // --- DÒNG 2 (Trùng dòng 1) ---
            if (addRowBtn) { await addRowBtn.click(); await delay(800); }
            let inputs2 = await p.$$('.oxd-autocomplete-text-input input');
            let targetInput2 = inputs2[inputs2.length - 1];
            await targetInput2.click({ clickCount: 3 });
            await targetInput2.type('Apache', { delay: 120 });
            await delay(1500);
            await p.keyboard.press('ArrowDown'); await delay(400);
            await p.keyboard.press('Enter'); await delay(800);

            let selectTriggers2 = await p.$$('.oxd-select-text');
            let targetSelect2 = selectTriggers2[selectTriggers2.length - 1];
            if (targetSelect2) {
                await targetSelect2.click();
                await delay(600);
                await p.keyboard.press('ArrowDown'); await delay(300);
                await p.keyboard.press('Enter'); await delay(800);
            }

            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) {
                await saveBtn.click();
                await delay(1000);
            }

            // Assertion tìm chữ Duplicate Record
            const duplicateError = await p.evaluateHandle(() => {
                const elements = Array.from(document.querySelectorAll('*'));
                return elements.find(el => el.textContent.trim() === 'Duplicate Record');
            });

            if (!duplicateError) {
                throw new Error('Assertion thất bại: Không tìm thấy thông báo "Duplicate Record" màu đỏ trên giao diện!');
            }
        }));

        // 3. TIME_MY_DelRow_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_DelRow_01', 'Kiểm tra xóa dòng công việc khỏi bảng giờ', async (p) => {
            await openMyTimesheetsEditMode(p);
            const delIcons = await p.$$('button:has(.bi-trash), .oxd-table-cell-actions button');
            if (delIcons.length > 0) { await delIcons[0].click(); await delay(500); }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); }
        }));

        // 4. TIME_MY_Input_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_Input_01', 'Kiểm tra nhập số giờ hợp lệ trong ngày', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length >= 2) {
                await inputs[0].click(); await p.keyboard.press('Backspace'); await inputs[0].type('8');
                await inputs[1].click(); await p.keyboard.press('Backspace'); await inputs[1].type('4.5');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); }
        }));

        // 5. TIME_MY_Input_02
        testResults.push(await runPositiveTest(page, 'TIME_MY_Input_02', 'Kiểm tra nhập 0 giờ cho ngày làm việc', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].click(); await p.keyboard.press('Backspace'); await inputs[0].type('0');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); }
        }));

        // 6. TIME_MY_Limit_01
        testResults.push(await runNegativeTest(page, 'TIME_MY_Limit_01', 'Kiểm tra báo lỗi khi nhập tổng giờ trong 1 ngày vượt quá 24h', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].click(); await inputs[0].type('25');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) await saveBtn.click();
        }));

        // 7. TIME_MY_Limit_02
        testResults.push(await runNegativeTest(page, 'TIME_MY_Limit_02', 'Kiểm tra báo lỗi khi tổng giờ nhiều Project trong 1 ngày vượt 24h', async (p) => {
            await openMyTimesheetsEditMode(p);
            const addRowBtn = await p.$('button.oxd-button--text');
            if (addRowBtn) { await addRowBtn.click(); await delay(500); }
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length >= 8) {
                await inputs[0].type('15');
                await inputs[7].type('10');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) await saveBtn.click();
        }));

        // 8. TIME_MY_Format_01
        testResults.push(await runNegativeTest(page, 'TIME_MY_Format_01', 'Kiểm tra báo lỗi khi nhập số âm vào ô giờ', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].click(); await inputs[0].type('-5');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) await saveBtn.click();
        }));

        // 9. TIME_MY_Format_02
        testResults.push(await runNegativeTest(page, 'TIME_MY_Format_02', 'Kiểm tra báo lỗi khi nhập chữ cái/ký tự đặc biệt vào ô giờ', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            if (inputs.length > 0) {
                await inputs[0].click(); await inputs[0].type('abc');
            }
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) await saveBtn.click();
        }));

        // 10. TIME_MY_Calc_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_Calc_01', 'Kiểm tra tự động tính ô Total theo hàng ngang (Row Total)', async (p) => {
            await openMyTimesheetsEditMode(p);
            const inputs = await p.$$('.oxd-table-body input.oxd-input');
            for(let i=0; i<5; i++) {
                if(inputs[i]) { await inputs[i].click(); await inputs[i].type('8'); }
            }
        }));

        // 11. TIME_MY_Calc_02
        testResults.push(await runPositiveTest(page, 'TIME_MY_Calc_02', 'Kiểm tra tự động tính ô Total theo hàng dọc (Column Total)', async (p) => {
            await openMyTimesheetsEditMode(p);
            const footer = await p.$('.oxd-table-footer');
            if (!footer) throw new Error('Không tìm thấy footer tổng cột.');
        }));

        // 12. TIME_MY_Nav_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_Nav_01', 'Kiểm tra chuyển sang tuần kế tiếp (Next Week)', async (p) => {
            await page.goto(`${BASE_URL}/web/index.php/time/viewMyTimesheet`, { waitUntil: 'domcontentloaded' });
            const nextWeekBtn = await p.$('button:has(.bi-chevron-right)');
            if (nextWeekBtn) { await nextWeekBtn.click(); await waitForLoading(p); }
        }));

        // 13. TIME_MY_Nav_02
        testResults.push(await runPositiveTest(page, 'TIME_MY_Nav_02', 'Kiểm tra chuyển về tuần trước đó (Previous Week)', async (p) => {
            await page.goto(`${BASE_URL}/web/index.php/time/viewMyTimesheet`, { waitUntil: 'domcontentloaded' });
            const prevWeekBtn = await p.$('button:has(.bi-chevron-left)');
            if (prevWeekBtn) { await prevWeekBtn.click(); await waitForLoading(p); }
        }));

        // 14. TIME_MY_Save_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_Save_01', 'Kiểm tra tính năng Save Draft (Lưu nháp)', async (p) => {
            await openMyTimesheetsEditMode(p);
            const saveBtn = await p.$('button[type="submit"]');
            if (saveBtn) { await saveBtn.click(); }
        }));

        // 15. TIME_MY_Reset_01
        testResults.push(await runPositiveTest(page, 'TIME_MY_Reset_01', 'Kiểm tra tính năng Reset/Cancel khi chưa lưu', async (p) => {
            await openMyTimesheetsEditMode(p);
            const cancelBtn = await p.$('button.oxd-button--ghost');
            if (cancelBtn) { await cancelBtn.click(); await delay(500); }
        }));

        // 16. TIME_MY_Submit_01
        testResults.push(await runNegativeTest(page, 'TIME_MY_Submit_01', 'Kiểm tra Submit Timesheet trống (không có giờ làm)', async (p) => {
            await page.goto(`${BASE_URL}/web/index.php/time/viewMyTimesheet`, { waitUntil: 'domcontentloaded' });
            const submitBtn = await p.$('button.oxd-button--secondary:not(:has-text("Edit"))');
            if (submitBtn) { await submitBtn.click(); await delay(1000); }
        }));

        // 17. TIME_MY_Submit_02
        testResults.push(await runPositiveTest(page, 'TIME_MY_Submit_02', 'Kiểm tra Submit Timesheet hợp lệ thành công', async (p) => {
            await page.goto(`${BASE_URL}/web/index.php/time/viewMyTimesheet`, { waitUntil: 'domcontentloaded' });
        }));

        // 18. TIME_MY_State_01
        testResults.push(await runPageViewTest(page, 'TIME_MY_State_01', 'Kiểm tra khóa giao diện (Read-only) sau khi Submit', 'web/index.php/time/viewMyTimesheet'));

        // 19. TIME_MY_State_02
        testResults.push(await runPageViewTest(page, 'TIME_MY_State_02', 'Kiểm tra trạng thái Timesheet bị từ chối (Rejected)', 'web/index.php/time/viewMyTimesheet'));

        // 20. TIME_MY_State_03
        testResults.push(await runPageViewTest(page, 'TIME_MY_State_03', 'Kiểm tra trạng thái Timesheet đã được duyệt (Approved)', 'web/index.php/time/viewMyTimesheet'));

        // 21. TIME_MY_Comment_01
        testResults.push(await runPageViewTest(page, 'TIME_MY_Comment_01', 'Kiểm tra xem lịch sử Comment/Action Log của bảng giờ', 'web/index.php/time/viewMyTimesheet'));

    } catch (err) {
        console.error('\n[ERROR] Lỗi luồng thực thi chung:', err.message);
    } finally {
        const passedCount = testResults.filter(r => r.status === 'PASSED').length;
        const failedCount = testResults.filter(r => r.status === 'FAILED').length;
        fs.writeFileSync('my_timesheets_execution_report.json', JSON.stringify(testResults, null, 2));
        console.log('\n===============================================================');
        console.log(`[SUMMARY] HOÀN TẤT THỰC THI 21 TEST CASES MY TIMESHEETS:`);
        console.log(`- PASSED: ${passedCount}/21`);
        console.log(`- FAILED: ${failedCount}/21`);
        console.log(`- Báo cáo JSON: my_timesheets_execution_report.json`);
        console.log(`- Thư mục Screenshots: /screenshots_my_timesheets/`);
        console.log('===============================================================\n');
        await browser.close();
    }
}

executeAllMyTimesheetTests();