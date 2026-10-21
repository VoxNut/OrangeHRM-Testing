# PIM – Kế hoạch triển khai của Võ Minh Nhựt

## Phạm vi

75 test case trong `docs/test-cases/members/Group01_23130226_VoMinhNhut.xlsx`:

- PIM_EMP: 25 case Employee List/Add Employee.
- PIM_PERSONAL: 25 case Personal Details/Attachments.
- PIM_JOB: 25 case Job Details/Contract.

Đây là kế hoạch; chưa có kết quả chạy automation hoặc fixture đã tạo trên website.

Katalon Recorder là lựa chọn cá nhân cho phần PIM của Nhựt. Những thành viên khác tự chọn công cụ, không bị ràng buộc bởi kế hoạch này.

## Trình tự

1. Ghi môi trường, đọc lại UI/validation và xác nhận baseline OrangeHRM. Khác biệt phải cập nhật design trước run.
2. Sao chép `test-data/pim/fixtures.example.json` thành bản run riêng. Ghi employee key, baseline, các nhãn dropdown và giới hạn upload thật.
3. Tạo fixture bằng UI và xác nhận trước khi record. Mỗi case bắt đầu từ trạng thái riêng đã biết.
4. Làm smoke trước: `TC_PIM_EMP_001`, `TC_PIM_EMP_003`, `TC_PIM_EMP_020`, `TC_PIM_PERSONAL_001`, `TC_PIM_JOB_001`, `TC_PIM_JOB_012`, `TC_PIM_JOB_013`.
5. Record/chỉnh lệnh trên Katalon Recorder, thêm assertion. Chạy từng case rồi mới chạy nhóm.
6. Hoàn thiện các case P1, sau đó P2 trong workbook. Upload/download cần thử khả năng tương tác file của extension trước.
7. Lưu suite thật vào `automation/23130226/KatalonRecorder/PIM_<module>/`, dữ liệu và hướng dẫn import kèm theo.
8. Ghi actual, ngày thực chạy, lỗi, ảnh và log vào đúng Case ID. Report sẽ tự đếm khi cột Result đổi.
9. Khôi phục/xóa riêng fixture của nhóm. Lưu bằng chứng trước cleanup.

## Assertion bắt buộc

| Loại case | Kiểm tra |
| --- | --- |
| Tạo mới | Đúng employee key, tên/ID sau mở lại và bản ghi tìm được trong danh sách |
| Validation | Lỗi đúng trường, giá trị sai không lưu; không chỉ kiểm tra form còn hiện |
| Sửa thông tin | Giá trị sau Save và reload khớp input; không nhầm Save của khối khác |
| Search/filter | Có positive control và không có negative control; kiểm tra tập kết quả liên quan |
| Contract | Ngày start/end, trạng thái tệp keep/replace/delete và dữ liệu sau mở lại |
| Upload | Tên và nội dung file, giới hạn thực tế và các tệp khác không bị thay đổi |

Chọn command từ Reference của Katalon Recorder đã cài. Không sao chép `waitForElementVisible` của Selenium IDE khi extension báo Unknown command. Ưu tiên đợi điều kiện thay vì pause cố định.

## Reset

- Case sửa tên/ID phải mở bằng employee key đã ghi, không tìm lại bằng tên/ID vừa bị đổi.
- Case Replace/Delete cần setup tệp hiện tại trước từng lần chạy, không dựa vào case Upload trước đó.
- Case Add Employee dùng ID riêng chưa tồn tại và dọn sau khi đã lưu evidence.
- Không thay cấu hình chung hoặc tài khoản Admin của demo.
