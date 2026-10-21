# PIM – Kế hoạch triển khai của Võ Minh Nhựt

## Phạm vi

75 test case trong `docs/test-cases/members/Group01_23130226_VoMinhNhut.xlsx`:

- PIM_EMP: 25 case Employee List/Add Employee.
- PIM_PERSONAL: 25 case Personal Details/Attachments.
- PIM_JOB: 25 case Job Details/Contract.

Tuần đầu chạy 25 case PIM_EMP bằng Playwright. PIM_PERSONAL/PIM_JOB chưa chạy, giữ Untested. JSON và screenshot thực tế nằm trong `evidence/executions/23130226/`. Các run chẩn đoán script không dùng làm kết quả chính thức.

Playwright là lựa chọn cá nhân cho phần PIM của Nhựt. Những thành viên khác tự chọn công cụ. ID dùng `Module_Action_XX`, số bắt đầu từ 01 trong mỗi nhóm hành động, ví dụ `PIM_EMP_Create_01`, `PIM_EMP_Search_01`.

## Trình tự

1. Ghi môi trường, đọc lại UI/validation và xác nhận baseline OrangeHRM. Khác biệt phải cập nhật design trước run.
2. Sao chép `test-data/pim/fixtures.example.json` thành bản run riêng. Ghi employee key, baseline, các nhãn dropdown và giới hạn upload thật.
3. Tạo fixture bằng UI và xác nhận trước khi record. Mỗi case bắt đầu từ trạng thái riêng đã biết.
4. Làm smoke trước: `PIM_EMP_Create_01`, `PIM_EMP_Create_03`, `PIM_EMP_Search_01`. Hai chức năng còn lại triển khai sau theo workbook.
5. Dùng Playwright, thêm assertion. Chạy từng case rồi mới chạy nhóm. Đợi dữ liệu form và dòng bảng cập nhật xong, không chỉ đợi phần tử tồn tại.
6. Hoàn thiện các case P1, sau đó P2 trong workbook. Upload/download cần thử khả năng tương tác file của extension trước.
7. Lưu runner thật vào `automation/23130226/Playwright/`, kèm dữ liệu và hướng dẫn chạy.
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

Chọn API đúng với phiên bản Playwright. Ưu tiên đợi điều kiện thay vì pause cố định. Lỗi setup, locator hoặc timeout chưa đủ chứng minh lỗi chức năng: ghi Blocked kèm nguyên nhân. Không đổi expected để đạt Passed.

## Reset

- Case sửa tên/ID phải mở bằng employee key đã ghi, không tìm lại bằng tên/ID vừa bị đổi.
- Case Replace/Delete cần setup tệp hiện tại trước từng lần chạy, không dựa vào case Upload trước đó.
- Case Add Employee dùng ID riêng chưa tồn tại và dọn sau khi đã lưu evidence.
- Không thay cấu hình chung hoặc tài khoản Admin của demo.
