# Group01 – OrangeHRM Testing

Dự án kiểm thử website [OrangeHRM Demo](https://opensource-demo.orangehrmlive.com/) cho môn Software Testing.

## Thành viên

| MSSV | Họ tên | Vai trò | Chức năng |
| --- | --- | --- | --- |
| 23130226 | Võ Minh Nhựt | Nhóm trưởng | PIM_EMP, PIM_PERSONAL, PIM_JOB |
| 23130299 | Nguyễn Văn Thắng | Thành viên | RECR_VAC, RECR_CAND, RECR_FLOW |
| 23130339 | Tôn Bích Trâm | Thành viên | TIME_ATTEND, TIME_MY, TIME_EMP |
| 22130254 | Cao Tiến Thành | Thành viên | LEAVE_CONFIG, LEAVE_SELF, LEAVE_ADMIN |
| 23130171 | Nguyễn Thị Thủy Linh | Thành viên | ADM_STRUCT, ADM_JOB, ADM_USER |

## Tài liệu và mã nguồn

- [Phân công chức năng](docs/planning/function-allocation.md)
- [Test case](docs/test-cases/README.md) · [Format workbook](docs/templates/README.md)
- [Automation](automation/README.md) · [Kế hoạch triển khai](docs/automation/README.md)
- [Dữ liệu test](test-data/README.md) · [Kết quả chạy](evidence/executions/README.md)
- [Quy trình đóng góp](CONTRIBUTING.md)

Mỗi thành viên tự chọn công cụ và ghi hướng dẫn chạy trong thư mục của mình. Tích hợp qua **nhánh cá nhân → PR → dev**; `main` lưu các mốc thống nhất.


## Assignment 8 — Recruitment release (23130299)

Nhóm **01**, thành viên/module ở bảng trên đã được đối chiếu metadata repo. Công cụ từng phần: Recruitment Katalon Studio 11.6.0; PIM/ADM Playwright, LEAVE Cypress (version cụ thể theo package lock và README thành viên). TIME chưa có automation source trên branch được audit. Repo hiện là nhiều project theo thành viên; **chưa có một unified automation project/runner toàn nhóm**.

Clone `git clone https://github.com/VoxNut/OrangeHRM-Testing.git`; dùng branch release được ghi trong `maintenance/asm8-23130299/GIT_HANDOFF.md`, không tự merge main/dev. Mở `automation/23130299/KatalonStudio/OrangeHRM_RECRUIT_VAC_KatalonStudio.prj` bằng Studio 11.6.0. README tại project hướng dẫn generated files, Profile, môi trường, manual login, standalone TC, ba suite và prerequisites có kiểm soát. Không có shared suite chung để khẳng định đã chạy toàn nhóm.

Profile default/plan/workbook snapshot bàn giao cùng source; không có mật khẩu. Giữ `ASM7_ALLOW_PIM_FIXTURE_CREATE=false`, Manager/Interviewer numeric active identity, exact CV bytes chưa xác nhận thì UploadCV_04 giữ SKIPPED. Đặt `ASM8_RUN_KIND=FIRST_RUN` hoặc `RETEST` trước khi mở Studio cho lần chạy tương lai; report lịch sử giữ UNCLASSIFIED, không thay nhãn theo timestamp. `ASM7_EVIDENCE_ROOT` tùy chọn là absolute dedicated output directory, mặc định dùng relative project/repo namespace 23130299; runtime/case UUID giữ riêng.

Report chính thức và index: [Test Report - Group 01](Test%20Report%20-%20Group%2001/index.html). PNG/log/manifests: [Envidence - Group01](Envidence%20-%20Group01/README.md). [Automation deliverable status](Automation%20Testing%20Project%20-%20Group%2001/README.md) giải thích blocker unified project; không phải tuyên bố hoàn tất tích hợp. Evidence thành viên khác tại `evidence/executions/<MSSV>` được giữ nguyên; chưa audit đầy đủ cho toàn nhóm.

Latest Recruitment local: **71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED**, ba execution riêng; HTML và JUnit đối chiếu từng case. Xem summary JSON/CSV và ERROR_STOPPED_ANALYSIS trong report Individual/23130299/Documentation. ERROR tách khỏi FAILED; suffix screenshot không là final status. Không dùng console message thay business assertion.

**GROUP_EXECUTION_PENDING / ASM8_INTEGRATION_NOT_VERIFIED. Recruitment giữ SKIPPED theo nhóm trưởng.** Compiler/offline validation không chứng minh browser integration. VAC_Filter_02 chờ requirement; UploadCV_04 chờ exact byte limit; FLOW_Status_01 Candidate81 uncertain, cấm replay Save; SelectInterviewer_02 locator đã sửa nhưng retest live pending. Trước common suite phải thống nhất runner/module source, scope listener và review shared Profile/helpers. Không tạo/xóa Employee, không tự replay Save/Submit/Interview/Shortlist khi chưa biết outcome. Chỉ operator được phép thực thi có kiểm soát theo SMOKE_PLAN; giữ original/FIRST_RUN/RETEST và báo final status bằng official report.
