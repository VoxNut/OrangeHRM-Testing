# Phân công chức năng

Mỗi thành viên nhận 3 chức năng, khoảng 20–30 test case mỗi chức năng. Tuần đầu nộp ít nhất một chức năng theo yêu cầu Classroom. Phần chưa có người nhận không phải đã phân công.

| Mã | Chức năng | Người phụ trách |
| --- | --- | --- |
| PIM_EMP | Employee List & Add Employee | 23130226 – Võ Minh Nhựt |
| PIM_PERSONAL | Personal Details & Attachments | 23130226 – Võ Minh Nhựt |
| PIM_JOB | Job Details & Employment Contract | 23130226 – Võ Minh Nhựt |
| LEAVE_CONFIG | Leave Types & Entitlements | Chưa chọn |
| LEAVE_SELF | Apply Leave & My Leave | Chưa chọn |
| LEAVE_ADMIN | Leave List & Request Actions | Chưa chọn |
| TIME_ATTEND | Punch In/Out & Attendance Records | Chưa chọn |
| TIME_MY | My Timesheets | Chưa chọn |
| TIME_EMP | Employee Timesheets & Review | Chưa chọn |
| RECR_VAC | Vacancies | Chưa chọn |
| RECR_CAND | Candidates | Chưa chọn |
| RECR_FLOW | Candidate Workflow & Interviews | Chưa chọn |
| ADM_STRUCT | Organization Structure | Chưa chọn |
| ADM_JOB | Job Titles & Job Specifications | Chưa chọn |
| ADM_USER | User Management & Roles | Chưa chọn |

## Phối hợp fixture

- PIM_JOB dùng danh mục Job Title, Category, Sub Unit, Location và Status có sẵn. Không xóa danh mục dùng chung.
- LEAVE_CONFIG cần chuẩn bị quỹ phép trước LEAVE_SELF. LEAVE_ADMIN cần đơn riêng và người duyệt có quyền phù hợp.
- TIME_MY cần Project/Activity. TIME_EMP cần timesheet Submitted của nhân viên khác người duyệt.
- RECR_VAC chuẩn bị vacancy và manager, RECR_CAND chuẩn bị ứng viên, RECR_FLOW dùng ứng viên riêng cho từng nhánh trạng thái.
- ADM_STRUCT, ADM_JOB và ADM_USER chỉ sửa/xóa dữ liệu nhóm tự tạo. Không sửa tài khoản Admin demo.
- Trước khi chốt test case phê duyệt, xác nhận vai trò và nút hành động xuất hiện trên fixture. Thiếu fixture không tự động được xem là lỗi sản phẩm.

Nhóm trưởng cập nhật bảng sau khi các thành viên chọn phần. Không mặc định mỗi người phải nhận trọn một module nếu quyền/dữ liệu phụ thuộc không phù hợp.
