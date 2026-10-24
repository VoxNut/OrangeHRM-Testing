# Kế hoạch automation của nhóm

Mỗi thành viên tự chọn công cụ phù hợp với chức năng được giao. Không bắt buộc Katalon Recorder hoặc bất kỳ công cụ chung nào.

Phân công và phần thiết kế của 15 chức năng đã được nhóm trưởng xác nhận hoàn thành. Kế hoạch thực thi theo người phụ trách:

| Người phụ trách | Phạm vi automation |
| --- | --- |
| Võ Minh Nhựt – 23130226 | PIM_EMP, PIM_PERSONAL, PIM_JOB |
| Cao Tiến Thành – 22130254 | LEAVE_CONFIG, LEAVE_SELF, LEAVE_ADMIN |
| Tôn Bích Trâm – 23130339 | TIME_ATTEND, TIME_MY, TIME_EMP |
| Nguyễn Văn Thắng – 23130299 | RECR_VAC, RECR_CAND, RECR_FLOW |
| Nguyễn Thị Thủy Linh – 23130171 | ADM_STRUCT, ADM_JOB, ADM_USER |

Trạng thái thiết kế không thay cho kết quả chạy. Checklist bên dưới dùng cho từng suite; chỉ đánh dấu khi có script, dữ liệu hoặc bằng chứng tương ứng.

Phần ASM7 của Võ Minh Nhựt đã triển khai 15 case đại diện trên cả 3 chức năng PIM, với 15 Passed trong lần chạy ngày 24/10/2026. Xem [script, setup/cleanup và truy vết](../../automation/23130226/Playwright/ASM7.md) và [kết quả chạy thật](../../evidence/executions/23130226/ASM7_2026-10-24T02-46-28-365Z/README.md). Không đánh dấu checklist chung của thành viên khác dựa trên kết quả này.

## Checklist chung

- [ ] Chốt phạm vi, quy tắc nghiệp vụ và test design của từng chức năng.
- [ ] Ghi công cụ và phiên bản. Chọn 3–5 case smoke đại diện, record hoặc viết script rồi chạy lại bằng công cụ đã chọn.
- [ ] Chuẩn bị fixture riêng cho từng người và từng lần chạy.
- [ ] Xác nhận command/API trong tài liệu công cụ thực tế, không dùng cú pháp của công cụ khác khi chưa chuyển đổi.
- [ ] Tổ chức suite theo mã chức năng, tên case khớp ID workbook.
- [ ] Dùng assertion kiểm tra expected, không chỉ dựa vào lệnh click/type hoàn tất.
- [ ] Thiết lập lại tiền điều kiện trước từng case, không phụ thuộc thứ tự ngầm.
- [ ] Lưu suite/script, dữ liệu input và hướng dẫn import/cài đặt/chạy tương ứng trong `automation/MaSV/TenCongCu/MaChucNang/`.
- [ ] Chạy, chụp ảnh/log thật, ghi actual và phân loại lỗi script/môi trường/sản phẩm.
- [ ] Dọn đúng dữ liệu nhóm tạo, review và merge vào dev.

Kế hoạch chi tiết nằm trong sheet `Automation Plan` của từng workbook cá nhân. Phạm vi, phân bổ case, tiền điều kiện và phối hợp dữ liệu của cả 15 chức năng nằm trong [bảng phân công](../planning/function-allocation.md).

Thứ tự chuẩn bị dữ liệu: Leave Types/Entitlements trước Apply Leave và Leave List; Project/Activity và My Timesheets trước Employee Timesheets; Vacancies trước Candidates và Candidate Workflow. Các suite Admin/PIM chỉ dùng danh mục và hồ sơ test riêng; không xóa dữ liệu mà suite khác đang sử dụng.
