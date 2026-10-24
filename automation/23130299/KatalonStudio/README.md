# Recruitment Vacancies – Katalon Studio

Nguyễn Văn Thắng – 23130299. 25 case RECRUIT_VAC trên [OrangeHRM Demo](https://opensource-demo.orangehrmlive.com/), dùng Katalon Studio 11.6.0, Groovy/Selenium, Chrome 155, Windows 11.

## Chạy

1. Mở `OrangeHRM_RECRUIT_VAC_KatalonStudio.prj` bằng Katalon Studio.
2. Chọn profile `default`; kiểm tra biến `evidenceDir` lưu screenshot.
3. Mở suite `RECRUIT_VAC_25_ASC_001_TO_025`, chọn **Run → Chrome**.
4. Đăng nhập thủ công trong 120 giây khi Console yêu cầu.
5. Xem kết quả ở **Log Viewer** và **Reports**.

Chạy riêng case tại `Test Cases/RECRUIT_VAC/`. Project dùng `RecorderBridge.groovy` và `VacancyChecks.groovy` trong `Keywords/recruitment/`; không cần import lại `.side`.

## Kết quả

Run `20261023_115454` ngày 23/10/2026: **23 Passed, 2 Error (92%)**. Update_02/03 timeout chờ `Successfully Updated`; chưa xác nhận dữ liệu sau mở lại. Kết quả thuộc run này; mã sửa sau đó cần chạy xác minh lại.

- [Workbook](../../../docs/test-cases/members/Group01_23130299_NguyenVanThang.xlsx)
- Bằng chứng: `evidence/executions/23130299/RECRUIT_VAC_20261023_01/` tính từ repo root; đối chiếu file HTML và screenshot theo Case ID/timestamp.

Suite tạo Vacancy test. Chỉ dọn fixture thuộc lần chạy của mình; không lưu tài khoản riêng trong mã nguồn.
