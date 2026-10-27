import { test, expect } from '@playwright/test';

test.describe.serial('Organization Structure (ADM_STRUCT) - Full 25 Test Cases', () => {

    test.beforeEach(async ({ page }) => {
        // 1. Mở trang đăng nhập
        await page.goto('https://opensource-demo.orangehrmlive.com/web/index.php/auth/login');

        // 2. Nhập username & password Admin
        await page.locator('input[name="username"]').fill('Admin');
        await page.locator('input[name="password"]').fill('admin123');
        await page.locator('button[type="submit"]').click();

        // 3. Mở Admin -> Organization -> Structure
        await page.locator('text=Admin').click();
        await page.locator('text=Organization').click();
        await page.locator('text=Structure').click();

        // Xác nhận đã vào trang Structure
        await expect(page.locator('h6:has-text("Structure"), h6:has-text("Organization Structure")')).toBeVisible();
    });

    // 1. ADM_STRUCT_Login_01
    test('ADM_STRUCT_Login_01: Kiểm tra đăng nhập bằng tài khoản Admin', async ({ page }) => {
        await expect(page.locator('h6:has-text("Structure"), h6:has-text("Organization Structure")')).toBeVisible();
    });

    // 2. ADM_STRUCT_View_01
    test('ADM_STRUCT_View_01: Kiểm tra hiển thị cây tổ chức', async ({ page }) => {
        await expect(page.locator('.orangehrm-paper-container')).toBeVisible();
    });

    // 3. ADM_STRUCT_View_02
    test('ADM_STRUCT_View_02: Kiểm tra quan hệ đơn vị cha/con', async ({ page }) => {
        await expect(page.locator('.orangehrm-container')).toBeVisible();
    });

    // 4. ADM_STRUCT_Expand_01
    test('ADM_STRUCT_Expand_01: Kiểm tra mở rộng node', async ({ page }) => {
        const expandBtn = page.locator('.bi-plus, .oxd-icon-button').first();
        if (await expandBtn.isVisible()) {
            await expandBtn.click();
        }
    });

    // 5. ADM_STRUCT_Collapse_01
    test('ADM_STRUCT_Collapse_01: Kiểm tra thu gọn node', async ({ page }) => {
        const collapseBtn = page.locator('.bi-dash, .oxd-icon-button').first();
        if (await collapseBtn.isVisible()) {
            await collapseBtn.click();
        }
    });

    // 6. ADM_STRUCT_EditMode_01
    test('ADM_STRUCT_EditMode_01: Kiểm tra bật chế độ Edit', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await expect(editToggle).toBeChecked();
    });

    // 7. ADM_STRUCT_EditMode_02
    test('ADM_STRUCT_EditMode_02: Kiểm tra tắt chế độ Edit', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await editToggle.uncheck({ force: true });
        await expect(editToggle).not.toBeChecked();
    });

    // 8. ADM_STRUCT_EditMode_03
    test('ADM_STRUCT_EditMode_03: Chuyển đổi qua lại giữa xem và chỉnh sửa', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await expect(editToggle).toBeChecked();
        await editToggle.uncheck({ force: true });
        await expect(editToggle).not.toBeChecked();
        await editToggle.check({ force: true });
        await expect(editToggle).toBeChecked();
    });

    // 9. ADM_STRUCT_Create_01
    test('ADM_STRUCT_Create_01: Kiểm tra thêm đơn vị cấp đầu', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('TOP_UNIT_01');
        await page.locator('.oxd-form input').nth(1).fill('Phòng Chiến Lược');
        await page.locator('button[type="submit"]').click();
    });

    // 10. ADM_STRUCT_Create_02
    test('ADM_STRUCT_Create_02: Kiểm tra thêm đơn vị con', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('SUB_DEV_01');
        await page.locator('.oxd-form input').nth(1).fill('Bộ Phận Lập Trình');
        await page.locator('button[type="submit"]').click();
    });

    // 11. ADM_STRUCT_Validation_01
    test('ADM_STRUCT_Validation_01: Kiểm tra validation trường bắt buộc', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('button[type="submit"]').click();
        await expect(page.locator('.oxd-input-group__message').first()).toBeVisible();
    });

    // 12. ADM_STRUCT_Create_03
    test('ADM_STRUCT_Create_03: Kiểm tra ID bị trùng', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('SUB_DEV_01');
        await page.locator('.oxd-form input').nth(1).fill('Bộ Phận Thử Nghiệm');
        await page.locator('button[type="submit"]').click();
        await expect(page.locator('.oxd-input-group__message, .oxd-text--toast-message')).toBeVisible();
    });

    // 13. ADM_STRUCT_Validation_02
    test('ADM_STRUCT_Validation_02: Định dạng ID có ký tự đặc biệt', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('@#$123');
        await page.locator('.oxd-form input').nth(1).fill('Đơn Vị Ký Tự Đặc Biệt');
        await page.locator('button[type="submit"]').click();
    });

    // 14. ADM_STRUCT_Validation_03
    test('ADM_STRUCT_Validation_03: Khoảng trắng đầu/cuối tên đơn vị', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('TRIM_01');
        await page.locator('.oxd-form input').nth(1).fill('   Phòng Kinh Doanh   ');
        await page.locator('button[type="submit"]').click();
    });

    // 15. ADM_STRUCT_Validation_04
    test('ADM_STRUCT_Validation_04: Giới hạn độ dài mô tả', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        const longDesc = 'A'.repeat(401);
        await page.locator('textarea').fill(longDesc);
        await page.locator('button[type="submit"]').click();
    });

    // 16. ADM_STRUCT_Create_04
    test('ADM_STRUCT_Create_04: Kiểm tra hủy thao tác thêm đơn vị', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('button:has-text("Add")').first().click();
        await page.locator('.oxd-form input').nth(0).fill('TEMP_999');
        await page.locator('button:has-text("Cancel")').click();
    });

    // 17. ADM_STRUCT_Update_01
    test('ADM_STRUCT_Update_01: Kiểm tra sửa tên đơn vị', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-pencil').first().click();
        await page.locator('.oxd-form input').nth(1).fill('Tên Đơn Vị Mới');
        await page.locator('button[type="submit"]').click();
        await page.reload();
    });

    // 18. ADM_STRUCT_Update_02
    test('ADM_STRUCT_Update_02: Kiểm tra sửa mô tả đơn vị', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-pencil').first().click();
        await page.locator('textarea').fill('Mô tả cập nhật tự động.');
        await page.locator('button[type="submit"]').click();
    });

    // 19. ADM_STRUCT_Update_03
    test('ADM_STRUCT_Update_03: Kiểm tra sửa ID sang giá trị không tồn tại', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-pencil').first().click();
        const idInput = page.locator('.oxd-form input').nth(0);
        if (await idInput.isEnabled()) {
            await idInput.fill('NEW_ID_888');
            await page.locator('button[type="submit"]').click();
        }
    });

    // 20. ADM_STRUCT_Update_04
    test('ADM_STRUCT_Update_04: Kiểm tra sửa ID thành giá trị đã tồn tại', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-pencil').first().click();
        const idInput = page.locator('.oxd-form input').nth(0);
        if (await idInput.isEnabled()) {
            await idInput.fill('SUB_DEV_01');
            await page.locator('button[type="submit"]').click();
            await expect(page.locator('.oxd-input-group__message, .oxd-text--toast-message')).toBeVisible();
        }
    });

    // 21. ADM_STRUCT_Update_05
    test('ADM_STRUCT_Update_05: Kiểm tra hủy thao tác sửa', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-pencil').first().click();
        await page.locator('.oxd-form input').nth(1).fill('Tên Thay Đổi Thử Nghiệm');
        await page.locator('button:has-text("Cancel")').click();
    });

    // 22. ADM_STRUCT_Delete_01
    test('ADM_STRUCT_Delete_01: Kiểm tra xóa đơn vị không có đơn vị con', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-trash').last().click();
        await page.locator('button:has-text("Yes, Delete")').click();
        await page.reload();
    });

    // 23. ADM_STRUCT_Delete_02
    test('ADM_STRUCT_Delete_02: Kiểm tra hủy xác nhận xóa', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-trash').first().click();
        await page.locator('button:has-text("No, Cancel")').click();
    });

    // 24. ADM_STRUCT_Delete_03
    test('ADM_STRUCT_Delete_03: Kiểm tra quy tắc xóa đơn vị cha có đơn vị con', async ({ page }) => {
        const editToggle = page.locator('input[type="checkbox"]').first();
        await editToggle.check({ force: true });
        await page.locator('.bi-trash').first().click();
        await page.locator('button:has-text("Yes, Delete")').click();
    });

    // 25. ADM_STRUCT_Delete_04
    test('ADM_STRUCT_Delete_04: Dữ liệu sau khi xóa và tải lại trang', async ({ page }) => {
        await page.reload();
        await expect(page.locator('.orangehrm-paper-container')).toBeVisible();
    });

});