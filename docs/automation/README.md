# Kế hoạch automation của nhóm

Mỗi thành viên tự chọn công cụ phù hợp với chức năng được giao. Không bắt buộc Katalon Recorder hoặc bất kỳ công cụ chung nào.

## Checklist chung

- [ ] Chốt phạm vi, quy tắc nghiệp vụ và test design của từng chức năng.
- [ ] Ghi công cụ và phiên bản. Chọn 3–5 case smoke đại diện, record hoặc viết script rồi chạy lại bằng công cụ đã chọn.
- [ ] Chuẩn bị fixture riêng cho từng người và từng lần chạy.
- [ ] Xác nhận command/API trong tài liệu công cụ thực tế, không dùng cú pháp của công cụ khác khi chưa chuyển đổi.
- [ ] Tổ chức suite theo mã chức năng, tên case khớp ID workbook.
- [ ] Dùng assertion kiểm tra expected, không chỉ dựa vào lệnh click/type hoàn tất.
- [ ] Thiết lập lại tiền điều kiện trước từng case, không phụ thuộc thứ tự ngầm.
- [ ] Lưu suite/script, dữ liệu input và hướng dẫn import/cài đặt/chạy tương ứng trong `automation/MaSV/TenCongCu/MaChucNang/`.
- [ ] Chạy, chụp ảnh/log thật, ghi actual và phân loại lỗi script/môi trường/sản phẩm.
- [ ] Dọn đúng dữ liệu nhóm tạo, review và merge vào dev.

Chi tiết PIM nằm trong sheet `Automation Plan` của workbook Nhựt. Các chức năng khác bổ sung kế hoạch sau khi phân công.
