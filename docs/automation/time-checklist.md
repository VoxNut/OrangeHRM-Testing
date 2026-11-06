# Timesheets – Kế hoạch automation

[Workbook]: 66 case/chức năng cho Time.

## Thực hiện

1. Xác nhận UI, validation, định dạng ngày, tổng giờ (Total) và danh mục dự án.
2. Chuẩn bị nhân viên/bảng giờ test riêng; ghi nhận Employee Id, trạng thái Submit và dữ liệu hàng ngang/dọc.
3. Kiểm tra Create/Search, chỉnh sửa số giờ, thêm/xóa dòng công việc và phê duyệt/từ chối.
4. Assert dữ liệu sau Save/Approve; negative case phải kiểm tra lỗi (ví dụ: vượt quá 24h/ngày, bỏ trống comment khi reject) và dữ liệu không được lưu sai.
5. Lưu log/ảnh trước cleanup; chỉ xóa đúng bảng giờ của lần chạy.

Case sửa đổi mở bằng tài khoản quản trị hoặc nhân viên tương ứng. Runner script dùng ID và module phân tách, không chạy đồng thời để tránh xung đột dữ liệu.

`npm run timesheets` triển khai đầy đủ các test case cho My Timesheets và Employee Timesheets, gồm validation, lưu/mở lại, chỉnh sửa giờ và trạng thái duyệt.

[Script và lệnh chạy] · [Kết quả]