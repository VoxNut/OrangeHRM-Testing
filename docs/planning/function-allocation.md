# Phân công chức năng

Mỗi thành viên phụ trách 3 chức năng, thiết kế 20–30 case/chức năng; mục tiêu 25 case. Nội dung chi tiết và số case thực tế nằm trong workbook cá nhân.

| Mã | Phạm vi | Người phụ trách |
| --- | --- | --- |
| PIM_EMP | Thêm nhân viên, validation, ảnh, tìm/lọc, Reset, sắp xếp, phân trang, Cancel | 23130226 – Võ Minh Nhựt |
| PIM_PERSONAL | Họ tên/định danh, ngày sinh/bằng lái, quốc tịch, hôn nhân, giới tính, attachments | 23130226 – Võ Minh Nhựt |
| PIM_JOB | Ngày vào làm, danh mục công việc, hợp đồng, ngày/tệp hợp đồng, attachments | 23130226 – Võ Minh Nhựt |
| LEAVE_CONFIG | Loại phép, validation, cấp/lọc/đối chiếu entitlement | 22130254 – Cao Tiến Thành |
| LEAVE_SELF | Apply Leave, ngày/thời lượng, số dư, My Leave, chi tiết và hủy đơn | 22130254 – Cao Tiến Thành |
| LEAVE_ADMIN | Lọc Leave List, chi tiết, comments, duyệt/từ chối/hủy theo quyền và trạng thái | 22130254 – Cao Tiến Thành |
| TIME_ATTEND | Punch In/Out, ngày/giờ, ghi chú, records, thời lượng và quyền sửa | 23130339 – Tôn Bích Trâm |
| TIME_MY | Kỳ tuần, Project/Activity, giờ, dòng, Reset, tổng giờ, Save/Submit | 23130339 – Tôn Bích Trâm |
| TIME_EMP | Tìm/xem timesheet nhân viên, tổng giờ, history, comments, Approve/Reject | 23130339 – Tôn Bích Trâm |
| RECR_VAC | Tạo/sửa vacancy, validation, manager, số lượng, Active/Publish, lọc/Reset/Cancel | 23130299 – Nguyễn Văn Thắng |
| RECR_CAND | Hồ sơ ứng viên, validation, CV, ngày, notes/consent, lọc/Reset | 23130299 – Nguyễn Văn Thắng |
| RECR_FLOW | Shortlist/Reject, lịch phỏng vấn, interviewer, chuyển trạng thái và history | 23130299 – Nguyễn Văn Thắng |
| ADM_STRUCT | Cây tổ chức, Edit, node cha/con, validation, sửa/hủy/xóa | 23130171 – Nguyễn Thị Thủy Linh |
| ADM_JOB | Job Titles, description/note, specification, validation, sửa/hủy/xóa | 23130171 – Nguyễn Thị Thủy Linh |
| ADM_USER | Tài khoản, employee/role/status, mật khẩu, lọc/Reset, đăng nhập và phân quyền | 23130171 – Nguyễn Thị Thủy Linh |

## Dữ liệu và phối hợp

- PIM: nhân viên riêng, danh mục công việc tồn tại; ghi employee key để mở lại và cleanup.
- Leave: cấu hình loại/quỹ phép trước Apply Leave; người duyệt và người xin nghỉ khác nhau.
- Time: chuẩn bị Project/Activity, tuần có thể sửa và timesheet Submitted cho case phê duyệt; xác nhận múi giờ/quyền sửa.
- Recruitment: vacancy/manager trước ứng viên; mỗi nhánh workflow có ứng viên và trạng thái đầu vào riêng.
- Admin: dùng node, chức danh, employee và tài khoản test riêng; xóa node con trước node cha.

Xác nhận validation, giới hạn tệp/ngày và nút hành động trên UI trước khi chốt expected. Chỉ sửa/xóa dữ liệu nhóm tạo; không đổi tài khoản Admin demo.
