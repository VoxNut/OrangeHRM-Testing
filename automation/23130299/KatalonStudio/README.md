# Recruitment – Katalon Studio

Nguyễn Văn Thắng – 23130299. Ba suite RECRUIT_VAC, RECRUIT_CAND, RECRUIT_FLOW, mỗi suite 25 case trên [OrangeHRM Demo](https://opensource-demo.orangehrmlive.com/), dùng Katalon Studio/Groovy/Selenium.

## Chạy

1. Mở `OrangeHRM_RECRUIT_VAC_KatalonStudio.prj` bằng Katalon Studio.
2. Bổ sung profile `default` và tài nguyên cấu hình còn thiếu; kiểm tra đường dẫn evidence.
3. Mở suite tương ứng trong `Test Suites`, chọn **Run → Chrome**.
4. Đăng nhập thủ công trong 120 giây khi Console yêu cầu.
5. Xem kết quả ở **Log Viewer** và **Reports**.

Chạy riêng case tại `Test Cases/<module>/`. CAND/FLOW dùng các keyword mới trong `Keywords/recruitment/`. Bản Git đang thiếu `Profiles/default.glbl` và `maintenance/preflight58-plan.json`; `ExecutionContext.groovy` còn đường dẫn tuyệt đối của máy tác giả. Cần hoàn thiện các tài nguyên này trước khi chạy trên máy khác.

## Kết quả

Run `20261023_115454` ngày 23/10/2026: **23 Passed, 2 Error (92%)**. Update_02/03 timeout chờ `Successfully Updated`; chưa xác nhận dữ liệu sau mở lại. Kết quả thuộc run này; mã sửa sau đó cần chạy xác minh lại.

CAND/FLOW có screenshot ngày 30/10/2026 trong `evidence/executions/23130299/`; chưa có report tổng hợp trong Git. ASM8 ghi Skipped theo yêu cầu nhóm trưởng.

- [Workbook](../../../docs/test-cases/members/Group01_23130299_NguyenVanThang.xlsx)
- Bằng chứng: `evidence/executions/23130299/RECRUIT_VAC_20261023_01/` tính từ repo root; đối chiếu file HTML và screenshot theo Case ID/timestamp.

Suite tạo Vacancy test. Chỉ dọn fixture thuộc lần chạy của mình; không lưu tài khoản riêng trong mã nguồn.
