# Format workbook

Tham khảo [workbook mẫu](../test-cases/members/Group01_23130226_VoMinhNhut.xlsx).

| Sheet | Nội dung |
| --- | --- |
| Cover | Project, tác giả, reviewer, phiên bản |
| Test case List | 3 chức năng, phạm vi và tiền điều kiện |
| TestDesign | Yêu cầu, kỹ thuật thiết kế, ID case |
| Sheet chức năng | ID, mô tả, bước/input, expected, dependency, result, ngày, note |
| Test Data | Fixture, dữ liệu biên, tệp test, cách khôi phục |
| Automation Plan | Setup, ưu tiên, assertion, chạy và cleanup |
| Test Report | Tổng hợp trạng thái từ cột Result |

ID dùng `Module_Action_XX`, đánh số từ 01 theo hành động. Mỗi chức năng có 20–30 case; cập nhật công thức report khi thay số dòng.

Ví dụ: First Name rỗng, Last hợp lệ, ID chưa tồn tại → Save → First Name báo Required, không tạo nhân viên. Case chưa chạy để `Untested`; sau chạy ghi actual và link log/ảnh.
