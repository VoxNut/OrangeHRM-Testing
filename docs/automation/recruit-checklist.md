# Recruitment — ASM8 handoff, 2026-11-02

75 case: VAC/CAND/FLOW, mỗi suite 25 case. Source release `feat/asm8-recruitment-23130299`, commit `e7c89705625cb4809ae6a8e5da81fb6871ab011d` đã xác minh trên GitHub. Bản bổ sung bàn giao ở `feat/asm8-recruitment-feedback-23130299`; không tự merge vào dev/main.

| Phạm vi | Trạng thái |
| --- | --- |
| Latest local official execution | VAC `20261101_142618`: 24 PASSED / 1 ERROR; CAND `20261101_213500`: 24 PASSED / 1 SKIPPED; FLOW `20261102_011836`: 23 PASSED / 2 ERROR |
| Tổng cá nhân | **71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED**, UNCLASSIFIED; không phải shared run |
| Source/config handoff | Có Profile, preflight plan, workbook runtime; source/config khớp Downloads; giữ assertions và suite order |
| Portability | **OFFLINE PORTABILITY VERIFIED**, harness gốc 10/10; không chứng minh browser chạy trên máy khác |
| Standalone | Antlr2/Parrot compile và launcher preflight PASS; browser retest NOT VERIFIED |
| ASM8 group | **SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING** |

[Official/supplementary report index](../../Test%20Report%20-%20Group%2001/Individual/23130299/index.html) · [75 case](../../Test%20Report%20-%20Group%2001/Individual/23130299/RESULTS75.md) · [Failure analysis](../../Test%20Report%20-%20Group%2001/Individual/23130299/recruitment-failure-analysis.md) · [Báo cáo sửa phản hồi](ASM8_RECRUITMENT_TEAM_LEADER_FIX_REPORT.md)

Katalon Studio 11.6.0, Chrome/WebDriver tương thích, Profile default, `maintenance/preflight58-plan.json` và `maintenance/workbook75-current.json` phải đi cùng project. Operator quản lý credentials ngoài Git và đăng nhập thủ công. Giữ `ASM7_ALLOW_PIM_FIXTURE_CREATE=false`; xác minh active numeric manager/interviewer identities. `ASM7_EVIDENCE_ROOT` là tùy chọn; mặc định resolve từ project/repo, không phụ thuộc username hoặc shell cwd.

Bốn case còn chặn: VAC_Filter_02 — requirement/UI discrepancy; CAND_UploadCV_04 — thiếu exact byte limit; FLOW_Status_01 — Candidate81 persistence/readback chưa xác minh, backend UNKNOWN, không replay Save; FLOW_SelectInterviewer_02 — locator Add Another đã sửa trước release, browser retest pending. Không đổi Expected Results hoặc official status.

Runner trên NVThang có adapter Playwright/Cypress và inventory Recruitment nhưng giữ SKIP; `summarize.mjs` chỉ gộp JSON của runner nhóm, chưa nhập Katalon HTML/JUnit. Release kế thừa base khác nên chưa có hai file runner đó. Không sao chép runner/module của người khác vào branch này. CLI `katalonc.exe` có trong package Studio tại máy kiểm tra; licensed KRE, manual-login orchestration và adapter chưa được xác minh. Không tạo adapter tự chạy demo writes.

Nhóm trưởng cần phê duyệt requirement VAC, exact CV bytes, read-only reconciliation Candidate81, fixture ownership/cleanup và browser retest có kiểm soát. Sau đó thống nhất adapter opt-in và cách giữ ERROR/SKIPPED riêng trong aggregator; FIRST_RUN và RETEST phải có execution/report mới, không đổi nhãn ba lượt UNCLASSIFIED. Mọi shared execution vẫn chờ quyết định đưa Recruitment trở lại.

[Hướng dẫn chạy trên máy khác](../../automation/23130299/KatalonStudio/README.md) · [Workbook giữ nguyên](../test-cases/members/Group01_23130299_NguyenVanThang.xlsx)
