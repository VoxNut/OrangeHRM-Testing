# Quy ước workbook mẫu

Mẫu đã điền của Nhựt: `docs/test-cases/members/Group01_23130226_VoMinhNhut.xlsx`, đã có trên `dev` và `nhut`. Dùng format chung này để rà soát, chuẩn hóa workbook của 5 thành viên trước khi tổng hợp.

## Cấu trúc thống nhất

| Sheet | Nội dung |
| --- | --- |
| Cover | Project, mã tài liệu, người viết, người review, phiên bản và lịch sử thay đổi |
| Test case List | Danh sách 3 chức năng, phạm vi và tiền điều kiện |
| TestDesign | Phân rã yêu cầu, tiêu chí, kỹ thuật thiết kế và ID test case liên quan |
| Sheet chức năng | 20–30 test case với ID, mô tả, procedure/input, expected, dependency, result, date, note |
| Test Data | Fixture, chuỗi biên, file thử, định dạng ngày, quy tắc khôi phục và nguồn xác nhận |
| Automation Plan | Setup, ưu tiên, assertion, thực thi, cleanup và review |
| Test Report | Tổng hợp tự động Passed/Failed/Untested/Blocked/Skipped |

Workbook cá nhân phải có đúng metadata và 3 chức năng theo [bảng phân công](../planning/function-allocation.md). Không chỉ đổi tên sheet: phải cập nhật mã TC/TD, nội dung, công thức report và số dòng đếm. Giữ nguyên nội dung riêng của mỗi người khi chuẩn hóa format.

Phần PIM_EMP dùng Playwright là ví dụ cá nhân của Nhựt. Thành viên chọn công cụ khác cần thay công cụ, command/assertion, file suite/script và cách chạy tương ứng. Giữ chung format test design, không bắt buộc chung công cụ. Case ID dùng `Module_Action_XX`, đánh số từ 01 trong từng hành động, ví dụ `PIM_EMP_Create_01`, `PIM_EMP_Search_01`.

## Ví dụ viết một case

- **Description:** Bỏ trống First Name khi tạo nhân viên.
- **Pre-condition:** Admin đăng nhập, đang ở Add Employee.
- **Input:** First rỗng, Last=NhutA003, ID=G1N01A0003 chưa tồn tại.
- **Procedure:** Mở Add Employee; nhập dữ liệu; bấm Save.
- **Expected:** First Name hiển thị Required, không tạo nhân viên.
- **Result:** Untested cho đến khi thực thi.
- **Note sau thực thi:** Actual, thời gian, browser, đường dẫn log/ảnh, Defect ID nếu có. Giữ mã TD để truy vết.

Expected được xác định trước run. Nếu hệ thống khác expected, ghi actual và điều tra. Chỉ sửa expected khi có căn cứ quy tắc thật, không sửa để biến case thành Passed.
