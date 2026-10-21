# Phân công chức năng

Phân công đã thống nhất cho 5 thành viên, mỗi người 3 chức năng. Nhóm trưởng xác nhận các thành viên đã hoàn thành phần thiết kế chức năng. Bảng dưới là phạm vi thiết kế thống nhất; trạng thái push/merge và automation được kiểm tra riêng từ Git và bằng chứng thực thi.

Mỗi chức năng phân bổ 25 test case, phù hợp yêu cầu 20–30 case. Tổng dự kiến 75 case/người và 375 case/nhóm. Đây là phân bổ thiết kế, không phải số lượng Passed hoặc số case đã kiểm đếm từ tất cả workbook.

| Mã | Chức năng | Người phụ trách |
| --- | --- | --- |
| PIM_EMP | Employee List & Add Employee | 23130226 – Võ Minh Nhựt |
| PIM_PERSONAL | Personal Details & Attachments | 23130226 – Võ Minh Nhựt |
| PIM_JOB | Job Details & Employment Contract | 23130226 – Võ Minh Nhựt |
| LEAVE_CONFIG | Leave Types & Entitlements | 22130254 – Cao Tiến Thành |
| LEAVE_SELF | Apply Leave & My Leave | 22130254 – Cao Tiến Thành |
| LEAVE_ADMIN | Leave List & Request Actions | 22130254 – Cao Tiến Thành |
| TIME_ATTEND | Punch In/Out & Attendance Records | 23130339 – Tôn Bích Trâm |
| TIME_MY | My Timesheets | 23130339 – Tôn Bích Trâm |
| TIME_EMP | Employee Timesheets & Review | 23130339 – Tôn Bích Trâm |
| RECR_VAC | Vacancies | 23130299 – Nguyễn Văn Thắng |
| RECR_CAND | Candidates | 23130299 – Nguyễn Văn Thắng |
| RECR_FLOW | Candidate Workflow & Interviews | 23130299 – Nguyễn Văn Thắng |
| ADM_STRUCT | Organization Structure | 23130171 – Nguyễn Thị Thủy Linh |
| ADM_JOB | Job Titles & Job Specifications | 23130171 – Nguyễn Thị Thủy Linh |
| ADM_USER | User Management & Roles | 23130171 – Nguyễn Thị Thủy Linh |

## Võ Minh Nhựt – PIM

### PIM_EMP – Quản lý nhân viên

- Phạm vi: Employee List & Add Employee. Thêm nhân viên; tìm theo tên/mã và bộ lọc; sắp xếp, phân trang; ảnh đại diện khi tạo; hủy và tìm lại sau lưu.
- Phân bổ 25 case: thêm mới/validation bắt buộc (8); tìm kiếm, kết hợp bộ lọc, Reset (8); danh sách, sắp xếp, phân trang (4); ảnh đại diện (3); hủy và dữ liệu đã lưu (2).
- Tiền điều kiện: Admin; mã nhân viên riêng chưa tồn tại; nhân viên mẫu cho từng bộ lọc; đủ bản ghi nếu kiểm tra phân trang; tệp ảnh hợp lệ và dữ liệu biên theo giới hạn đã xác nhận.
- Automation: tách case thêm và tìm kiếm. Tên/mã theo MSSV và lần chạy; assert đúng nhân viên và giá trị lưu; đợi autocomplete/bảng cập nhật; chỉ dọn bản ghi test.

### PIM_PERSONAL – Hồ sơ cá nhân

- Phạm vi: Personal Details & Attachments. Họ tên, Employee ID, Other ID, bằng lái; quốc tịch, hôn nhân, ngày sinh, giới tính; attachments và đối chiếu sau mở lại.
- Phân bổ 25 case: họ tên/mã/validation (6); ngày sinh và ngày hết hạn bằng lái (6); quốc tịch/hôn nhân/giới tính (4); thêm, xem, sửa, xóa attachment (6); lưu và mở lại (3).
- Tiền điều kiện: nhân viên test riêng, quyền sửa, baseline để khôi phục, định dạng ngày và quy tắc tệp trên UI. Không giả định có trường CMND/CCCD.
- Automation: mở đúng nhân viên bằng mã riêng; sửa từng nhóm trường, Save và mở lại để assert. Tệp upload riêng; tránh Custom Fields do người khác thêm trên demo.

### PIM_JOB – Công việc và hợp đồng

- Phạm vi: Job Details & Employment Contract. Ngày vào làm; Job Title, Job Category, Sub Unit, Location, Employment Status; bật/tắt hợp đồng; ngày bắt đầu/kết thúc; tệp hợp đồng và attachments. Không gồm Contact Details.
- Phân bổ 25 case: chức danh/nhóm nghề/đơn vị/địa điểm/trạng thái (7); ngày vào làm và ngày hợp đồng (5); bật/tắt và lưu hợp đồng (4); tệp hợp đồng/attachments (5); lưu, mở lại và khôi phục (4).
- Tiền điều kiện: Admin, nhân viên test, danh mục còn tồn tại, ngày/tệp mẫu. Xác nhận quan hệ giữa các ngày trước khi chốt expected.
- Automation: kiểm tra danh mục trước suite, chọn dropdown theo nhãn; assert sau lưu/mở lại và khôi phục baseline trước từng case.

## Cao Tiến Thành – Leave

### LEAVE_CONFIG – Loại phép và quỹ phép

- Phạm vi: Leave Types & Entitlements. Loại phép, thuộc tính situational; cấp quỹ cho một nhân viên; tên, loại phép, kỳ phép, số ngày; xem/lọc và đối chiếu sau lưu.
- Phân bổ 25 case: thêm/sửa/hủy/xóa/xem loại phép (7); validation tên/thuộc tính (4); cấp quỹ hợp lệ (5); validation số ngày/nhân viên/loại/kỳ phép (5); tra cứu và đối chiếu (4).
- Tiền điều kiện: Admin; nhân viên, loại phép test và kỳ phép hiệu lực; số dư ban đầu được ghi nhận; tên riêng. Chuẩn bị quỹ riêng cho Apply Leave, không thay quỹ dùng chung.
- Automation: tách quản lý loại phép và cấp entitlement thành case độc lập; assert bản ghi và số quỹ sau lưu. Không chỉ kiểm tra form Add Leave Type.

### LEAVE_SELF – Xin nghỉ và quản lý đơn cá nhân

- Phạm vi: Apply Leave & My Leave. Loại phép, ngày đầu/cuối, thời lượng khi có, ghi chú, số dư; nộp đơn; tra cứu, chi tiết và hủy ở trạng thái cho phép.
- Phân bổ 25 case: nộp hợp lệ (5); ngày/khoảng ngày/thời lượng (6); số dư/quỹ/dữ liệu phụ thuộc (4); ghi chú/chi tiết/hủy (3); lọc My Leave, Reset và trạng thái (7).
- Tiền điều kiện: tài khoản gắn đúng nhân viên, quỹ/loại phép hiệu lực, lịch làm việc/ngày nghỉ/định dạng ngày đã xác nhận; ngày không trùng đơn khác. Khảo sát trước đó của Admin từng báo No Leave Types with Leave Balance; cần kiểm tra fixture khi chạy.
- Automation: kiểm tra quỹ trước case positive; assert đơn trong My Leave và trạng thái; dùng ngày riêng, khôi phục quỹ/trạng thái khi chạy lại. Thiếu fixture không phải mặc nhiên là lỗi nghiệp vụ.

### LEAVE_ADMIN – Danh sách và xử lý đơn nghỉ

- Phạm vi: Leave List & Request Actions. Lọc ngày/trạng thái/loại phép/nhân viên/đơn vị; chi tiết từng ngày; comments; duyệt/từ chối/hủy theo quyền và trạng thái.
- Phân bổ 25 case: lọc đơn/kết hợp/không kết quả/Reset (8); chi tiết (4); duyệt/từ chối (5); comments (3); hủy/quyền/chuyển trạng thái (5).
- Tiền điều kiện: đơn của nhân viên khác người duyệt; tài khoản đúng quyền; fixture Pending/Approved/Rejected/Cancelled theo trạng thái UI thực tế. Xác nhận nút Approve/Reject trên fixture trước khi chốt case.
- Automation: đơn độc lập cho từng nhánh; tìm bằng nhân viên/ngày riêng và assert kết quả lọc/trạng thái. Không suy ra quyền duyệt người khác từ đơn của chính Admin.

## Tôn Bích Trâm – Time

### TIME_ATTEND – Chấm công và bản ghi

- Phạm vi: Punch In/Out & Attendance Records. Ngày/giờ, ghi chú, My Records/Employee Records, khoảng thời gian và quyền sửa theo cấu hình. Không gồm định vị địa lý.
- Phân bổ 25 case: ngày/giờ/validation (8); trạng thái In/Out (5); ghi chú (3); tra cứu/đối chiếu records (5); quyền sửa/hành vi cấu hình (4).
- Tiền điều kiện: tài khoản/nhân viên riêng; trạng thái In/Out chuẩn bị trước; múi giờ, định dạng, quyền sửa được ghi nhận; thời gian không chồng bản ghi cũ.
- Automation: thiết lập trạng thái trước mỗi case; assert In/Out và record tương ứng; tính khoảng thời gian từ input, tránh phụ thuộc giờ hệ thống; kiểm tra bị chặn khi thiếu quyền.

### TIME_MY – Bảng giờ cá nhân theo tuần

- Phạm vi: My Timesheets. Kỳ tuần, Project/Activity, giờ từng ngày, thêm/xóa dòng, Reset, Save, Submit, tổng giờ và Not Submitted/Submitted.
- Phân bổ 25 case: Project/Activity/kỳ (5); giờ/định dạng/giá trị biên (7); dòng và Reset (4); tổng theo dòng/ngày/bảng (4); lưu/mở lại/Submit/trạng thái (5).
- Tiền điều kiện: tài khoản riêng; Project/Activity; tuần có thể sửa; quy tắc nhập giờ đã xác nhận. Không dùng timesheet Submitted cho case cần Not Submitted.
- Automation: chuẩn bị Project/Activity và tuần trước suite; assert giờ/tổng từ input; kiểm tra Save/Submit bằng trạng thái và dữ liệu mở lại; reset qua workflow được phép.

### TIME_EMP – Bảng giờ nhân viên và xử lý

- Phạm vi: Employee Timesheets & Review. Tìm nhân viên/kỳ, xem giờ/tổng/lịch sử, sửa theo quyền, comments, Approve/Reject theo trạng thái; không chỉ kiểm tra hai nút duyệt.
- Phân bổ 25 case: tìm nhân viên/kỳ/dữ liệu trống (6); chi tiết/tổng (4); comments/history (4); Approve/Reject/điều kiện trạng thái (7); sửa/quyền/lưu (4).
- Tiền điều kiện: nhân viên khác người duyệt, Project/Activity, timesheet có giờ và Submitted, tài khoản đúng quyền. Khảo sát timesheet của Admin từng chỉ thấy Edit; cần xác nhận nút trên fixture riêng.
- Automation: timesheet độc lập cho từng nhánh; assert nhân viên/kỳ, giờ, trạng thái, history. Không phụ thuộc ba timesheet mẫu của Admin; kiểm tra fixture trước phê duyệt.

## Nguyễn Văn Thắng – Recruitment

### RECR_VAC – Quản lý vị trí tuyển dụng

- Phạm vi: Vacancies. Thêm/sửa; tên, Job Title, Hiring Manager, số lượng, mô tả, Active/Publish; lọc/Reset; hủy và đối chiếu lưu.
- Phân bổ 25 case: tạo/trường bắt buộc (7); số lượng/validation (4); Active/Publish/hiển thị (4); lọc/Reset (6); sửa/hủy/lưu (4).
- Tiền điều kiện: Admin hoặc quyền Recruitment; Job Title và Hiring Manager còn tồn tại; tên riêng; quy tắc số lượng được xác nhận. Không dùng manager hiển thị (Deleted).
- Automation: tên vacancy theo MSSV, chọn manager bằng autocomplete đúng nhân viên; assert tên/trạng thái/lọc sau lưu. Tách dữ liệu vacancy khỏi hồ sơ và workflow ứng viên.

### RECR_CAND – Hồ sơ và tra cứu ứng viên

- Phạm vi: Candidates. Thêm/xem/sửa; tên, email, điện thoại, vacancy, CV, keywords, ngày ứng tuyển, notes, consent; lọc/Reset. Không gồm chuyển trạng thái phỏng vấn.
- Phân bổ 25 case: tạo/bắt buộc (7); email/điện thoại/validation (5); CV/giới hạn/thao tác (5); lọc/kết hợp/Reset (5); sửa/hủy/lưu (3).
- Tiền điều kiện: vacancy/manager còn hiệu lực; email giả example.com; bộ CV hợp lệ/không hợp lệ theo UI; ngày và tên riêng.
- Automation: bộ CV cố định, ứng viên riêng; assert hồ sơ theo email/tên đầy đủ, validation và tập kết quả lọc. Độc lập với workflow phỏng vấn.

### RECR_FLOW – Trạng thái ứng viên và phỏng vấn

- Phạm vi: Candidate Workflow & Interviews. Shortlist/Reject, Schedule Interview; tiêu đề/interviewer/ngày/giờ/notes; nút theo trạng thái và Candidate History.
- Phân bổ 25 case: khởi tạo/shortlist/reject (5); lịch/validation (7); interviewer/nhiều interviewer (3); nút/chuyển trạng thái (5); chi tiết/history (5).
- Tiền điều kiện: ứng viên, vacancy, manager, interviewer riêng còn hiệu lực; đúng trạng thái đầu vào cho mỗi nhánh. Khảo sát đã mở Schedule Interview từ Shortlisted; từng chuyển trạng thái vẫn cần xác nhận trên fixture.
- Automation: ứng viên độc lập cho mỗi nhánh, thao tác qua nút được phép; assert trạng thái/lịch/history. Không dùng ứng viên Rejected cho nhánh yêu cầu Shortlisted.

## Nguyễn Thị Thủy Linh – Admin

### ADM_STRUCT – Cây tổ chức và đơn vị trực thuộc

- Phạm vi: Organization Structure. Chế độ Edit, mở/thu cây, đơn vị cấp đầu/con, Unit ID/Name/Description, sửa/hủy/xóa đơn vị lá hoặc có con theo quy tắc xác nhận. Locations là chức năng riêng.
- Phân bổ 25 case: hiển thị/mở/thu (4); thêm cấp đầu/con/validation (8); sửa ID/tên/mô tả/lưu (5); xóa/hủy/cha-con (5); chế độ/quyền (3).
- Tiền điều kiện: Admin; cây test có cha/con/lá, tên/ID riêng; baseline và quy tắc xóa khi có con/liên kết. Chỉ sửa/xóa node nhóm tạo.
- Automation: định vị node theo tên/ID và đúng cha; assert vị trí/nội dung/mở-thu; dữ liệu riêng cho case xóa, cleanup từ con lên cha.

### ADM_JOB – Chức danh và tệp mô tả

- Phạm vi: Job Titles & Job Specifications. Danh sách, thêm/sửa/xóa, tên/description/note, upload/thay/xóa specification, hủy và đối chiếu lưu. Không gộp Pay Grades hoặc Education.
- Phân bổ 25 case: tạo/danh sách (5); validation tên/mô tả/ghi chú (5); specification/giới hạn/thay đổi (6); sửa/lưu (4); xóa/hủy/chọn bản ghi (5).
- Tiền điều kiện: Admin; chức danh riêng chưa gán cho employee/vacancy nếu test xóa; tệp và giới hạn upload đã xác nhận. Không giả định có Search nếu UI không có.
- Automation: tên duy nhất, assert nội dung/tệp sau mở lại; tách tạo/sửa/upload/xóa, dùng fixture mới cho xóa. Không sửa danh mục dùng chung.

### ADM_USER – Tài khoản và quyền truy cập

- Phạm vi: User Management & Roles. Thêm/xem/sửa; username/employee/role Admin-ESS/status/password-confirm; lọc/Reset; đăng nhập tài khoản test để kiểm tra role/status.
- Phân bổ 25 case: lọc username/role/employee/status/Reset (6); tạo/sửa/validation (7); username/liên kết employee (4); quyền role (4); trạng thái/đăng nhập (4).
- Tiền điều kiện: Admin và employee test; tài khoản Admin/ESS riêng, username duy nhất; quy tắc mật khẩu đã xác nhận. Không đổi mật khẩu/role/status của Admin demo.
- Automation: tách quản lý tài khoản khỏi đăng nhập/phân quyền; assert role/status trong danh sách, sau đó đăng nhập đúng tài khoản để kiểm tra quyền. Logout và reset session giữa các role.

## Phối hợp fixture

- PIM_JOB dùng danh mục Job Title, Category, Sub Unit, Location và Status có sẵn. Không xóa danh mục dùng chung.
- LEAVE_CONFIG cần chuẩn bị quỹ phép trước LEAVE_SELF. LEAVE_ADMIN cần đơn riêng và người duyệt có quyền phù hợp.
- TIME_MY cần Project/Activity. TIME_EMP cần timesheet Submitted của nhân viên khác người duyệt.
- RECR_VAC chuẩn bị vacancy và manager, RECR_CAND chuẩn bị ứng viên, RECR_FLOW dùng ứng viên riêng cho từng nhánh trạng thái.
- ADM_STRUCT, ADM_JOB và ADM_USER chỉ sửa/xóa dữ liệu nhóm tự tạo. Không sửa tài khoản Admin demo.
- Trước khi chốt test case phê duyệt, xác nhận vai trò và nút hành động xuất hiện trên fixture. Thiếu fixture không tự động được xem là lỗi sản phẩm.

## Đối chiếu với workbook

Phân bổ trên là khung phạm vi nhóm đã cung cấp. Khi review, đối chiếu số lượng và nội dung case thực tế trong từng workbook; nếu khác phân bổ, ghi nhận rõ phần đã bao phủ và phần cần bổ sung. Không xem tổng 375 là số case đã review hoặc đã chạy. Kết quả automation phải có bằng chứng riêng cho từng case.
