# ASM7 – Admin
Script: `23130171_NguyenThiThuyLinh_ASM7.mjs`. Playwright/Edge; tài khoản demo `Admin/admin123`.

```powershell
npm install
# Chỉ chạy sau khi sửa fixture, selector và assertion nêu trong README.
```

Cần Node.js 20+ và Edge. Nếu thiếu bộ ghi video: `npx playwright install ffmpeg`.

## Case triển khai

| ID                       | Kiểm tra |
| --- | --- |
| ADM_STRUCT_Login_01      | Đăng nhập hệ thống và chuyển hướng đến trang Organization Structure |
| ADM_STRUCT_View_01       | Hiển thị đầy đủ cây sơ đồ tổ chức và khung container |
| ADM_STRUCT_View_02       | Hiển thị đúng quan hệ phân cấp đơn vị cha / đơn vị con |
| ADM_STRUCT_Expand_01     | Bấm nút (+) mở rộng node đơn vị con |
| ADM_STRUCT_Collapse_01   | Bấm nút (-) thu gọn node đơn vị |
| ADM_STRUCT_EditMode_01   | Bật chế độ chỉnh sửa (Edit Mode) bằng công tắc toggle |
| ADM_STRUCT_EditMode_02   | Tắt chế độ chỉnh sửa (Edit Mode) |
| ADM_STRUCT_EditMode_03   | Chuyển đổi trạng thái bật/tắt Edit Mode liên tục |
| ADM_STRUCT_Create_01     | Thêm mới đơn vị tổ chức cấp đầu (Root Unit) thành công |
| ADM_STRUCT_Create_02     | Thêm mới đơn vị con (Sub-unit) vào cây cơ cấu |
| ADM_STRUCT_Create_03     | Kiểm tra Unit ID trùng |
| ADM_STRUCT_Create_04     | Bấm Cancel để hủy thao tác thêm mới đơn vị |
| ADM_STRUCT_Validation_01 | Chặn gửi form để trống trường bắt buộc, hiển thị lỗi Required |
| ADM_STRUCT_Validation_02 | Thêm đơn vị thành công với Unit ID chứa ký tự đặc biệt |
| ADM_STRUCT_Validation_03 | Tự động loại bỏ khoảng trắng thừa (trim) ở 2 đầu tên đơn vị |
| ADM_STRUCT_Validation_04 | Giới hạn độ dài nhập liệu tối đa của trường Mô tả (Description) |
| ADM_STRUCT_Update_01     | Cập nhật thành công Tên đơn vị tổ chức |
| ADM_STRUCT_Update_02     | Cập nhật thành công Mô tả (Description) đơn vị |
| ADM_STRUCT_Update_03     | Thử đổi ID nếu trường cho phép sửa |
| ADM_STRUCT_Update_04     | Thử đổi ID sang giá trị trùng nếu trường cho phép sửa |
| ADM_STRUCT_Update_05     | Bấm Cancel để hủy thao tác cập nhật đơn vị |
| ADM_STRUCT_Delete_01     | Xóa thành công một đơn vị lá (không có đơn vị con) |
| ADM_STRUCT_Delete_02     | Hủy bỏ thao tác xóa khi bấm Cancel ở hộp thoại xác nhận |
| ADM_STRUCT_Delete_03     | Áp dụng quy tắc ràng buộc khi cố gắng xóa đơn vị cha |
| ADM_STRUCT_Delete_04     | Kiểm tra tính bền vững của dữ liệu cây tổ chức sau khi tải lại trang (Reload) |


Chưa có report JSON/HTML cho lượt chạy ASM8. Script dùng `@playwright/test`; package hiện chưa khai báo `testMatch` cho tên `.mjs`, cần bổ sung khi tác giả hoàn thiện runner.

