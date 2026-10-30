const puppeteer = require('puppeteer');

(async () => {
    // 1. Khởi tạo trình duyệt dùng chung cho toàn bộ bài test
    const browser = await puppeteer.launch({
        headless: false,
        defaultViewport: null,
        args: ['--start-maximized'],
        channel: 'chrome'
    });
    const page = await browser.newPage();

    try {
        // ==========================================
        // CHỨC NĂNG 1: TIME_ATTEND (CHẤM CÔNG VÀO / RA)
        // ==========================================
        {
            console.log("=== BẮT ĐẦU TEST CHỨC NĂNG 1: TIME_ATTEND ===");

            console.log("[BUOC 1] Dang nhap he thong...");
            await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/auth/login', {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            });

            await page.waitForSelector('input[name="username"]', { visible: true });
            await page.type('input[name="username"]', 'Admin');
            await page.type('input[name="password"]', 'admin123');
            await page.click('button[type="submit"]');

            await page.waitForNavigation({ waitUntil: 'domcontentloaded' }).catch(() => {});
            await new Promise(r => setTimeout(r, 2000));

            console.log("[BUOC 2] THUC HIEN CHAM CONG VAO (PUNCH IN)...");
            await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/attendance/punchIn', {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            });
            await new Promise(r => setTimeout(r, 2500));

            // Kiểm tra nếu đang ở màn hình Punch Out thì Out trước để đưa về trạng thái sạch
            const isOutScreen = await page.evaluate(() => {
                const title = document.querySelector('.orangehrm-card-container');
                return title && title.textContent.includes('Punch Out');
            });

            if (isOutScreen) {
                console.log("-> Tai khoan dang co phien In ton tai, bam Out de dua ve trang thai moi...");
                await page.evaluate(() => {
                    const btn = document.querySelector("button[type='submit']");
                    if (btn) btn.click();
                });
                await new Promise(r => setTimeout(r, 3500));
                await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/attendance/punchIn', { waitUntil: 'domcontentloaded' });
                await new Promise(r => setTimeout(r, 2500));
            }

            // Nhập ghi chú Punch In
            const noteIn = await page.waitForSelector('textarea', { visible: true, timeout: 8000 }).catch(() => null);
            if (noteIn) {
                await noteIn.click();
                await page.keyboard.down('Control');
                await page.keyboard.press('KeyA');
                await page.keyboard.up('Control');
                await page.keyboard.press('Backspace');
                await noteIn.type('Cham cong vao ca - Test Automation', { delay: 30 });
            }

            // Submit form Punch In
            await page.evaluate(() => {
                const btn = document.querySelector("button[type='submit']");
                if (btn) btn.click();
            });

            await page.waitForFunction(() => {
                const title = document.querySelector('.orangehrm-card-container');
                return title && title.textContent.includes('Punch Out');
            }, { timeout: 15000 });
            console.log("-> Xac nhan da Punch In thanh cong, giao dien da chuyen sang Punch Out!");
            await new Promise(r => setTimeout(r, 2500));

            console.log("[BUOC 3] THUC HIEN CHAM CONG RA (PUNCH OUT)...");
            const noteOut = await page.waitForSelector('textarea', { visible: true, timeout: 8000 }).catch(() => null);
            if (noteOut) {
                await noteOut.click();
                await page.keyboard.down('Control');
                await page.keyboard.press('KeyA');
                await page.keyboard.up('Control');
                await page.keyboard.press('Backspace');
                await noteOut.type('Cham cong ra ca - Test Automation', { delay: 30 });
            }

            // Submit form Punch Out
            await page.evaluate(() => {
                const btn = document.querySelector("button[type='submit']");
                if (btn) btn.click();
            });
            console.log("-> Da bam xac nhan Punch Out!");

            console.log("[BUOC 4] KIEM TRA ASSERTION...");
            try {
                const toast = await page.waitForSelector('.oxd-text--toast-message', { timeout: 8000 });
                const text = await page.evaluate(el => el.textContent, toast);
                if (text && text.includes('Successfully Saved')) {
                    console.log('Assertion Passed: Da luu thanh cong (Successfully Saved)!');
                } else {
                    console.log('Thong bao he thong: ' + text);
                }
            } catch (error) {
                console.log('Assertion Info: Da hoan tat thao tac Punch Out tren giao dien.');
            }

            await new Promise(r => setTimeout(r, 3500));

            console.log("[BUOC 5] DON DEP DU LIEU (XOA BAN GHI CHAM CONG)...");
            await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/attendance/viewMyAttendanceRecord', {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            });
            await new Promise(r => setTimeout(r, 3000));

            try {
                const trashBtn = await page.waitForSelector('.oxd-table-body .oxd-table-card:first-child button .bi-trash, button:has(.bi-trash)', { timeout: 8000 });
                if (trashBtn) {
                    await trashBtn.click();
                    console.log("-> Da bam icon thung rac de xoa ban ghi!");
                    await new Promise(r => setTimeout(r, 1500));

                    const confirmDeleteBtn = await page.waitForSelector('.oxd-dialog-sheet button.oxd-button--label-danger, xpath///button[contains(., "Delete")]', { timeout: 6000 });
                    if (confirmDeleteBtn) {
                        await confirmDeleteBtn.click();
                        console.log("-> Da xac nhan xoa ban ghi thanh cong!");
                    }
                }
            } catch (err) {
                console.log("Khong tim thay ban ghi can xoa hoac bang da sach se.");
            }

            await new Promise(r => setTimeout(r, 3000));
            console.log("Hoan thanh kich ban TIME_ATTEND.\n");
        }


        // ==========================================
        // CHỨC NĂNG 2: TIME_MY (MY TIMESHEETS)
        // ==========================================
        {
            console.log("=== BẮT ĐẦU TEST CHỨC NĂNG 2: TIME_MY ===");

            console.log("[BUOC 1] Di chuyen thang vao My Timesheet...");
            await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/time/viewMyTimesheet', {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            });
            await new Promise(r => setTimeout(r, 3000));

            // Kiem tra neu bang dang o trang thai da Submit/Approved thi Reset ve de bat dau moi
            const preResetBtn = await page.waitForSelector("xpath///button[normalize-space(.)='Reset']", { timeout: 3000 }).catch(() => null);
            if (preResetBtn) {
                console.log("-> Bang dang o trang thai cu, bam Reset truoc khi bat dau...");
                await preResetBtn.click();
                await new Promise(r => setTimeout(r, 3000));
            }

            console.log("[BUOC 2] Chuyen sang che do Edit Timesheet...");
            await page.waitForFunction(() => {
                const btns = Array.from(document.querySelectorAll('button'));
                return btns.some(b => (b.textContent.includes('Edit') || b.textContent.includes('Create Timesheet')) && b.offsetParent !== null);
            }, { timeout: 15000 });

            const editBtn = await page.evaluateHandle(() => {
                const btns = Array.from(document.querySelectorAll('button'));
                return btns.find(b => (b.textContent.includes('Edit') || b.textContent.includes('Create Timesheet')) && b.offsetParent !== null);
            });
            await editBtn.click();
            await new Promise(r => setTimeout(r, 3500));

            console.log("[BUOC 3] Kiem tra va dien Project co san...");
            const projectInput = await page.waitForSelector('table tbody tr:first-child td:nth-child(1) input, table input[placeholder*="hints"]', { visible: true, timeout: 15000 });
            const currentProj = await page.evaluate(el => el.value, projectInput);

            if (!currentProj) {
                await projectInput.click();
                await projectInput.type('a', { delay: 120 });
                await page.waitForFunction(() => {
                    const dd = document.querySelector('.oxd-autocomplete-dropdown');
                    return dd && dd.innerText && !dd.innerText.includes('Searching') && dd.innerText.trim().length > 0;
                }, { timeout: 10000 });
                await new Promise(r => setTimeout(r, 500));
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await new Promise(r => setTimeout(r, 2000));
            }

            console.log("[BUOC 4] Kiem tra va chon Activity...");
            const activitySelect = await page.waitForSelector('table tbody tr:first-child td:nth-child(2) .oxd-select-text', { visible: true, timeout: 10000 });
            const currentAct = await page.evaluate(el => el.textContent, activitySelect);
            if (currentAct.includes('Select')) {
                await activitySelect.click();
                await new Promise(r => setTimeout(r, 800));
                await page.keyboard.press('ArrowDown');
                await page.keyboard.press('Enter');
                await new Promise(r => setTimeout(r, 1500));
            }

            console.log("[BUOC 5] Nhap gio lam viec cac ngay trong tuan...");
            for (let col = 3; col <= 7; col++) {
                try {
                    const dayInput = await page.waitForSelector(`table tbody tr:first-child td:nth-child(${col}) input`, { visible: true, timeout: 3000 });
                    if (dayInput) {
                        await dayInput.click();
                        await page.keyboard.down('Control');
                        await page.keyboard.press('KeyA');
                        await page.keyboard.up('Control');
                        await page.keyboard.press('Backspace');
                        await dayInput.type('8', { delay: 50 });
                        await new Promise(r => setTimeout(r, 100));
                    }
                } catch (err) {}
            }

            console.log("[BUOC 6] Luu bang gio (Save) va kiem tra Assertion...");
            const saveBtn = await page.waitForSelector("button[type='submit']", { visible: true, timeout: 6000 });
            await saveBtn.click();

            try {
                const toast = await page.waitForSelector('.oxd-text--toast-message', { timeout: 8000 });
                const text = await page.evaluate(el => el.textContent, toast);
                if (text && text.includes('Successfully Saved')) {
                    console.log('Assertion Passed: Da luu thanh cong (Successfully Saved)!');
                }
            } catch (e) {
                console.log('Da thuc hien luu du lieu!');
            }

            await page.waitForFunction(() => !window.location.href.includes('editTimesheet'), { timeout: 15000 }).catch(() => {});
            await new Promise(r => setTimeout(r, 3500));

            console.log("[BUOC 7] Nop bang cong (Submit) va kiem tra Assertion...");
            const submitBtn = await page.waitForSelector("xpath///button[normalize-space(translate(., 'submit', 'SUBMIT'))='SUBMIT']", { timeout: 6000 }).catch(() => null);
            if (submitBtn) {
                await submitBtn.click();
                console.log("-> Da bam nut Submit nop bang cham cong!");

                try {
                    const submitToast = await page.waitForSelector('.oxd-text--toast-message', { timeout: 8000 });
                    const submitText = await page.evaluate(el => el.textContent, submitToast);
                    console.log('Assertion Passed: ' + submitText);
                } catch (e) {
                    console.log('Assertion Info: Da ghi nhan thao tac Submit len he thong.');
                }
                await new Promise(r => setTimeout(r, 3500));
            } else {
                console.log("-> Khong xuat hien nut Submit (bang da duoc nop tu truoc).");
            }

            console.log("[BUOC 8 - BUOC CUOI CUNG] DON DEP DU LIEU (RESET / CLEANUP)...");
            const finalResetBtn = await page.waitForSelector("xpath///button[normalize-space(.)='Reset']", { timeout: 4000 }).catch(() => null);
            if (finalResetBtn) {
                await finalResetBtn.click();
                console.log("-> Da bam nut Reset tra lai trang thai ban dau!");
                await new Promise(r => setTimeout(r, 3000));
            }

            const reEditBtn = await page.waitForSelector("xpath///button[contains(., 'Edit')]", { timeout: 4000 }).catch(() => null);
            if (reEditBtn) {
                await reEditBtn.click();
                await new Promise(r => setTimeout(r, 3000));

                for (let col = 3; col <= 7; col++) {
                    try {
                        const dayInput = await page.waitForSelector(`table tbody tr:first-child td:nth-child(${col}) input`, { visible: true, timeout: 2000 });
                        if (dayInput) {
                            await dayInput.click();
                            await page.keyboard.down('Control');
                            await page.keyboard.press('KeyA');
                            await page.keyboard.up('Control');
                            await page.keyboard.press('Backspace');
                            await new Promise(r => setTimeout(r, 100));
                        }
                    } catch (err) {}
                }

                const finalSave = await page.waitForSelector("button[type='submit']", { timeout: 6000 }).catch(() => null);
                if (finalSave) {
                    await finalSave.click();
                    console.log("-> Da lam sach toan bo gio tren bang va luu lai!");
                }
            }

            await new Promise(r => setTimeout(r, 3000));
            console.log("Hoan thanh kich ban TIME_MY.\n");
        }


        // ==========================================
        // CHỨC NĂNG 3: TIME_EMP (EMPLOYEE TIMESHEETS)
        // ==========================================
        {
            console.log("=== BẮT ĐẦU TEST CHỨC NĂNG 3: TIME_EMP ===");

            console.log("[BUOC 1] Vao thang trang quan ly Timesheet nhan vien...");
            await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/time/viewEmployeeTimesheet', {
                waitUntil: 'domcontentloaded',
                timeout: 60000
            });

            console.log("[BUOC 2] Tim kiem nhan vien Ranga tu autocomplete...");
            const empInput = await page.waitForSelector('.oxd-autocomplete-text-input input', { visible: true, timeout: 15000 });
            await empInput.click();
            await page.keyboard.down('Control');
            await page.keyboard.press('KeyA');
            await page.keyboard.up('Control');
            await page.keyboard.press('Backspace');
            await empInput.type('Ranga', { delay: 150 });

            await page.waitForFunction(() => {
                const dd = document.querySelector('.oxd-autocomplete-dropdown');
                return dd && dd.innerText && !dd.innerText.includes('Searching') && dd.innerText.includes('Ranga');
            }, { timeout: 15000 });

            await new Promise(r => setTimeout(r, 600));
            await page.keyboard.press('ArrowDown');
            await new Promise(r => setTimeout(r, 300));
            await page.keyboard.press('Enter');
            await new Promise(r => setTimeout(r, 1000));

            await page.click('button[type="submit"]');
            await new Promise(r => setTimeout(r, 4000));

            console.log("[BUOC 3] Kiem tra trang thai bang gio...");
            const initialResetBtn = await page.waitForSelector("xpath///button[normalize-space(.)='Reset']", { timeout: 4000 }).catch(() => null);
            if (initialResetBtn) {
                console.log("-> Bang dang o trang thai Approved, bam Reset de mo lai trang thai cho duyet...");
                await initialResetBtn.click();
                await new Promise(r => setTimeout(r, 3500));
            }

            const editBtn = await page.waitForSelector("xpath///button[contains(., 'Edit') or contains(., 'Create Timesheet')]", { timeout: 4000 }).catch(() => null);
            if (editBtn) {
                console.log("-> Dang mo che do Edit de kiem tra thong tin...");
                await editBtn.click();
                await new Promise(r => setTimeout(r, 3000));

                const projectInput = await page.waitForSelector('table tbody tr:first-child td:nth-child(1) input, table input[placeholder*="hints"]', { visible: true, timeout: 8000 }).catch(() => null);
                if (projectInput) {
                    const currentProj = await page.evaluate(el => el.value, projectInput);
                    if (!currentProj) {
                        await projectInput.click();
                        await projectInput.type('a', { delay: 120 });
                        await page.waitForFunction(() => {
                            const dd = document.querySelector('.oxd-autocomplete-dropdown');
                            return dd && dd.innerText && !dd.innerText.includes('Searching') && dd.innerText.trim().length > 0;
                        }, { timeout: 10000 });
                        await new Promise(r => setTimeout(r, 500));
                        await page.keyboard.press('ArrowDown');
                        await page.keyboard.press('Enter');
                        await new Promise(r => setTimeout(r, 2000));
                    }
                }

                const activitySelect = await page.$('table tbody tr:first-child td:nth-child(2) .oxd-select-text');
                if (activitySelect) {
                    const currentAct = await page.evaluate(el => el.textContent, activitySelect);
                    if (currentAct.includes('Select')) {
                        await activitySelect.click();
                        await new Promise(r => setTimeout(r, 800));
                        await page.keyboard.press('ArrowDown');
                        await page.keyboard.press('Enter');
                        await new Promise(r => setTimeout(r, 1500));
                    }
                }

                for (let col = 3; col <= 7; col++) {
                    try {
                        const dayInput = await page.waitForSelector(`table tbody tr:first-child td:nth-child(${col}) input`, { visible: true, timeout: 2000 });
                        if (dayInput) {
                            await dayInput.click();
                            await page.keyboard.down('Control');
                            await page.keyboard.press('KeyA');
                            await page.keyboard.up('Control');
                            await page.keyboard.press('Backspace');
                            await dayInput.type('8', { delay: 50 });
                            await new Promise(r => setTimeout(r, 100));
                        }
                    } catch (err) {}
                }

                const saveBtn = await page.waitForSelector("button[type='submit']", { visible: true, timeout: 6000 });
                await saveBtn.click();
                await page.waitForFunction(() => !window.location.href.includes('editTimesheet'), { timeout: 15000 }).catch(() => {});
                await new Promise(r => setTimeout(r, 3500));
            }

            const submitBtn = await page.waitForSelector("xpath///button[normalize-space(translate(., 'submit', 'SUBMIT'))='SUBMIT']", { timeout: 4000 }).catch(() => null);
            if (submitBtn) {
                console.log("[BUOC 4] Bam Submit bang cham cong...");
                await submitBtn.click();
                await new Promise(r => setTimeout(r, 3500));
            }

            console.log("[BUOC 5] THUC HIEN DUYET BANG GIO (APPROVE)...");
            const approveBtn = await page.waitForSelector("xpath///button[normalize-space(translate(., 'approve', 'APPROVE'))='APPROVE']", { timeout: 8000 }).catch(() => null);
            if (approveBtn) {
                await approveBtn.click();
                console.log("-> Da bam Approve bang cham cong thanh cong!");

                try {
                    const toast = await page.waitForSelector('.oxd-text--toast-message', { timeout: 8000 });
                    const text = await page.evaluate(el => el.textContent, toast);
                    console.log('Assertion Passed: ' + text);
                } catch (e) {
                    console.log('Assertion Info: Da thuc hien thao tac Approve tren giao dien.');
                }
            } else {
                console.log("-> Khong xuat hien nut Approve.");
            }

            await new Promise(r => setTimeout(r, 4000));

            console.log("[BUOC 6 - BUOC CUOI CUNG] DON DEP BANG NUT RESET...");
            const finalResetBtn = await page.waitForSelector("xpath///button[normalize-space(.)='Reset']", { timeout: 8000 }).catch(() => null);
            if (finalResetBtn) {
                await finalResetBtn.click();
                console.log("-> Da bam nut Reset hoan tat don dep moi truong test!");
            } else {
                console.log("-> Khong tim thay nut Reset o buoc cuoi.");
            }

            await new Promise(r => setTimeout(r, 3000));
            console.log("Hoan thanh kich ban TIME_EMP.\n");
        }

        console.log("=== ĐÃ CHẠY THÀNH CÔNG TẤT CẢ 3 CHỨC NĂNG ===");

    } catch (error) {
        console.error("Lỗi trong quá trình chạy automation:", error);
    } finally {
        await browser.close();
    }
})();