# Kế hoạch automation

Phạm vi từng thành viên tại [bảng phân công](../planning/function-allocation.md). Mỗi người tự chọn công cụ.

1. Chọn case, xác nhận input/expected và quyền thao tác.
2. Tạo fixture riêng; ghi rõ dependency giữa các case.
3. Viết script có assertion, chạy smoke rồi chạy suite.
4. Lưu actual, log/ảnh và trạng thái theo Case ID.
5. Dọn dữ liệu đã tạo, push nhánh cá nhân và tạo PR vào `dev`.

Ưu tiên đợi điều kiện dữ liệu thay vì pause cố định. Chỉ sửa/xóa fixture của mình; lỗi setup hoặc timeout cần phân biệt với lỗi sản phẩm.

- [PIM](pim-checklist.md)
- [Recruitment](recruit-checklist.md)
- [Admin](adm-checklist.md)
- [Hướng dẫn chạy theo công cụ](../../automation/README.md)

ASM8 dùng [runner nhóm](../../Automation%20Testing%20Project%20-%20Group%2001/README.md) để gọi các script gốc tuần tự và chuẩn hóa report. Kiểm tra inventory trước mỗi lần chạy; case chưa đủ fixture/assertion phải ghi Skipped kèm lý do.
