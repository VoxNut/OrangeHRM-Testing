# PIM – Kế hoạch automation

[Workbook](../test-cases/members/Group01_23130226_VoMinhNhut.xlsx): 25 case/chức năng cho PIM_EMP, PIM_PERSONAL và PIM_JOB.

## Thực hiện

1. Xác nhận UI, validation, định dạng ngày và danh mục dropdown.
2. Chuẩn bị nhân viên/tệp test riêng; ghi Employee Id và numeric employee key.
3. Kiểm tra Create/Search, cập nhật hồ sơ, attachment và hợp đồng.
4. Assert dữ liệu sau Save/mở lại; negative case phải kiểm tra lỗi và dữ liệu không bị lưu sai.
5. Lưu log/ảnh trước cleanup; chỉ xóa đúng hồ sơ của lần chạy.

Case sửa ID mở bằng employee key. Case replace/delete tệp cần chuẩn bị tệp ban đầu. Runner ASM6 dùng ID cố định nên không chạy đồng thời.

`npm run details` triển khai đủ 25 case PIM_PERSONAL và 25 case PIM_JOB, gồm validation, lưu/mở lại, attachment và hợp đồng.

[Script và lệnh chạy](../../automation/23130226/Playwright/README.md) · [Kết quả](../../evidence/executions/23130226/README.md)
