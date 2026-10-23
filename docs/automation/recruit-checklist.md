# Recruitment – Kế hoạch triển khai của Nguyễn Văn Thắng

## Phạm vi

75 test case trong `docs/test-cases/members/Group01_23130299_NguyenVanThang.xlsx`:

- RECRUIT_VAC: 25 case Recruitment → Vacancies (Create, Update, Search, Filter, Reset, Cancel).
- RECRUIT_CAND: 25 case Recruitment → Candidates.
- RECRUIT_FLOW: 25 case xử lý quy trình tuyển dụng.

Đã chạy 25 case RECRUIT_VAC bằng Katalon Studio 11.6.0 (Groovy/Selenium) trên Chrome. Run báo cáo `20261023_115454` có **23 Passed, 2 Error** (`RECRUIT_VAC_Update_02` và `RECRUIT_VAC_Update_03`). Hai case Error bị timeout khi automation chờ thông báo `Successfully Updated`; chưa có bằng chứng xác nhận dữ liệu cập nhật đã lưu hay chưa, vì vậy không kết luận OrangeHRM có lỗi chức năng. RECRUIT_CAND và RECRUIT_FLOW chưa có execution được xác minh, giữ **Untested**.

Dùng đúng run trên làm mốc cho Excel và evidence; các lần chạy chẩn đoán hoặc mã nguồn sửa sau run không thay thế kết quả này nếu chưa được chạy xác nhận. ID theo dạng `Module_Action_XX`, ví dụ `RECRUIT_VAC_Create_01`, `RECRUIT_VAC_Search_01`.

## Trình tự

1. Ghi môi trường, URL OrangeHRM Demo, trình duyệt, phiên bản Katalon Studio và thời gian chạy. Khi Katalon mở Chrome, đăng nhập thủ công trong 120 giây; không lưu mật khẩu vào project.
2. Chuẩn bị fixture riêng cho từng case bằng tên `G01_23130299_<timestamp>_VXX`; ghi đúng tên Vacancy, URL/ID, Job Title, Hiring Manager và trạng thái ban đầu. Không sửa dữ liệu người khác.
3. Kiểm tra quyền Recruitment, Job Title và gợi ý Hiring Manager thực tế trước khi chạy. Mỗi case phải có dữ liệu đầu vào xác định, không phụ thuộc kết quả của case trước.
4. Chạy smoke RECRUIT_VAC trước: `Create_01`, `Create_03`, `Create_04`, `Create_05`, `Reset_01`; kiểm tra validation và dữ liệu sau Save/reload.
5. Chạy riêng từng case cần điều tra, sau đó chạy suite `RECRUIT_VAC_25_ASC_001_TO_025` với profile `default` và Chrome. Dùng explicit wait cho dropdown, autocomplete, kết quả Search/Reset và dữ liệu lưu; không chỉ chờ phần tử xuất hiện.
6. Đối chiếu đủ 25 case với Expected/Actual trong Excel. Với run `20261023_115454`, ghi 23 Passed và 2 Error đúng log; không tự đổi Error thành Passed.
7. Lưu project nguồn trong `automation/23130299/KatalonStudio/`, file `.side` nguồn trong `automation/23130299/KatalonRecorder/RECRUIT_VAC/` nếu có, kèm README hướng dẫn mở `.prj` và chạy suite.
8. Lưu `report.html`, `results.csv`, `JUnit_Report.xml`, `console0.log` và ảnh trong `evidence/executions/23130299/RECRUIT_VAC_20261023_01/`. Đối chiếu screenshot theo Case ID và timestamp thuộc cùng run.
9. Hoàn thiện workbook, rà Git diff, commit/push trên nhánh cá nhân, tạo PR theo quy trình nhóm và lưu ảnh Git thực tế sau khi thao tác thành công.
10. Khi tiếp tục RECRUIT_CAND/RECRUIT_FLOW, chuẩn bị fixture và chạy độc lập; chỉ cập nhật kết quả các case thật sự đã thực thi.

## Assertion bắt buộc

| Loại case | Kiểm tra |
| --- | --- |
| Tạo mới | Các trường bắt buộc, tên Vacancy duy nhất; Save xong mở lại đúng URL/fixture và xác nhận giá trị |
| Validation | Required, whitespace, Positions không hợp lệ; lỗi đúng trường và không lưu bản ghi sai |
| Sửa thông tin | Description, Hiring Manager, Positions và Active/Publish đúng sau Save và mở lại; trường không liên quan giữ nguyên |
| Search/filter | Kết quả thỏa bộ lọc Job Title, Vacancy, Hiring Manager; kiểm tra cả record đúng/sai và phân trang |
| Reset | Các bộ lọc trở về mặc định, danh sách được cập nhật và fixture cần kiểm tra xuất hiện lại |
| Cancel | Dữ liệu chưa Save không tồn tại hoặc giá trị ban đầu không đổi sau mở lại |

Không coi thao tác click Save hoặc toast là đủ bằng chứng persistence. Lỗi locator, timeout, script hoặc dữ liệu demo chưa đủ kết luận bug của OrangeHRM. Ghi đúng status theo report và mô tả nguyên nhân trong Note; không sửa Expected Result để hợp thức hóa test.

## Reset

- Mỗi case tạo dữ liệu dùng tiền tố và timestamp riêng; ghi fixture ledger để truy lại đúng bản ghi khi cần.
- Case Update phải mở lại đúng URL/ID fixture ban đầu, không lấy nhầm Vacancy của người khác hoặc dùng tên đã thay đổi làm khóa duy nhất.
- Sau khi lưu đủ log và screenshot, chỉ cleanup Vacancy thực sự thuộc phiên chạy của mình; nếu chưa cleanup thì ghi rõ chưa thực hiện.
- Không xóa dữ liệu của người dùng khác, không thay đổi tài khoản Admin hoặc cấu hình dùng chung của OrangeHRM Demo.
