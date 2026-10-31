# Admin – Kế hoạch automation

[Workbook](../test-cases/members/Group01_23130171_NguyenThiThuyLinh.xlsx): 25 case/chức năng cho ADM_STRUCT, ADM_JOB và ADM_USER.

## Thực hiện
1. Xác nhận UI, validation, định dạng dữ liệu và danh mục dropdown.
2. Chuẩn bị dữ liệu test riêng cho Organization Structure, Job Titles & Job Specifications và User Management & Roles; ghi nhận ID của các bản ghi.
3. Kiểm tra hiển thị cây tổ chức, thêm/sửa/xóa đơn vị; quản lý chức danh và tệp specification; tạo/sửa tài khoản, kiểm tra role và status.
4. Assert dữ liệu sau Save/mở lại; negative case phải kiểm tra lỗi và dữ liệu không bị lưu sai.
5. Lưu log/ảnh trước cleanup; chỉ xóa dữ liệu do nhóm tạo và không thay đổi dữ liệu demo dùng chung.

Case xóa đơn vị cần kiểm tra quan hệ cha/con. Case replace/delete Job Specification cần chuẩn bị tệp ban đầu. Các case quản lý tài khoản cần dùng tài khoản test riêng để kiểm tra role/status và tránh ảnh hưởng tài khoản demo.

`dev` hiện có 25 case ADM_STRUCT; chưa có script ADM_JOB/ADM_USER. Trước chạy chung, thay selector sửa/xóa `.first()`/`.last()` bằng node test duy nhất, bổ sung assertion sau lưu và cleanup theo ID.

[Script và lệnh chạy](../../automation/23130171/Playwright/README.md) · [Minh chứng đã nộp](../../evidence/executions/23130171/ADM_STRUCT/)
