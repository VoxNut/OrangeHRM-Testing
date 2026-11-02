# ASM8 Recruitment — team leader feedback fixes

Ngày đối chiếu: 02/11/2026. Thành viên: Nguyễn Văn Thắng, 23130299. Phạm vi: sửa bàn giao Recruitment, hoàn toàn offline. **71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED** giữ nguyên. **SKIPPED_BY_GROUP_DECISION / GROUP_INTEGRATION_PENDING** giữ nguyên.

## A. Checklist yêu cầu nhóm trưởng

| Yêu cầu | Trước sửa | Đã thực hiện | Trạng thái sau | Evidence |
| --- | --- | --- | --- | --- |
| Portable evidence paths | Release đã có PortablePaths/ASM7_EVIDENCE_ROOT; log cũ 9 nhóm PASS rồi lỗi file tồn tại | Quét runtime/config; chạy harness gốc trong temporary directory mới; thêm kiểm tra member layout/lifecycle | OFFLINE PORTABILITY VERIFIED; browser máy khác NOT VERIFIED | [Log 10 nhóm](../../maintenance/asm8-23130299/team-feedback/offline/portability-original.log) |
| CAND official report | Đã có trên release, khó tìm từ nhánh khác | Dùng lại original HTML/JUnit/log; thêm index tại Individual/23130299 | VERIFIED AVAILABLE; không copy report | [Direct report links](../../Test%20Report%20-%20Group%2001/Individual/23130299/REPORT_LINKS.md) |
| FLOW official report | Đã có trên release, khó tìm từ nhánh khác | Dùng lại original HTML/JUnit/log; thêm index tại Individual/23130299 | VERIFIED AVAILABLE; không copy report | [Direct report links](../../Test%20Report%20-%20Group%2001/Individual/23130299/REPORT_LINKS.md) |
| JSON/HTML/XML index | Supplementary có sẵn; summary cũ còn layout gốc | Tạo index và official-derived RESULTS75 JSON/Markdown với relative links | 75/75 official HTML/JUnit/callback/XML khớp | [Index](../../Test%20Report%20-%20Group%2001/Individual/23130299/index.html), [RESULTS75](../../Test%20Report%20-%20Group%2001/Individual/23130299/RESULTS75.md) |
| ERROR/STOPPED analysis | Phân tích cũ có link ../Reports, ../evidence bị sai sau đóng gói | Tạo phân tích mới, searchable anomaly index, xác minh report lịch sử từ source khi liên kết duy nhất | Latest đầy đủ; historical reference-only và UNKNOWN tách rõ | [Failure analysis](../../Test%20Report%20-%20Group%2001/Individual/23130299/recruitment-failure-analysis.md), [anomaly index](../../Test%20Report%20-%20Group%2001/Individual/23130299/ANOMALY_SCREENSHOTS.md) |
| Standalone readiness | Profile/plan/workbook runtime đã có | Antlr2/Parrot compile production, launcher preflight-only trên project tạm | OFFLINE PASS; live browser NOT VERIFIED | [Offline results](../../maintenance/asm8-23130299/team-feedback/offline/results.json) |
| Shared group integration | Nhóm trưởng giữ Recruitment Skipped; release khác base với NVThang | Đọc hai runner trên NVThang; xác minh --check; ghi rõ prerequisites, không sửa shared runner | GROUP_INTEGRATION_PENDING / SKIPPED_BY_GROUP_DECISION | [Checklist](recruit-checklist.md), [runner inspection](../../maintenance/asm8-23130299/team-feedback/runner-inspection.json) |

[Gap analysis trước sửa](../../maintenance/asm8-23130299/team-feedback/GAP_ANALYSIS.md) và [audit SHA/inventory](../../maintenance/asm8-23130299/team-feedback/audit.json) được tạo trước khi sửa các tài liệu bàn giao. Release đã chứa source/report/evidence; không kết luận chưa bàn giao chỉ vì NVThang chưa có các thay đổi đó.

## B. Latest Recruitment results

| Module | Execution ID | Runtime UUID | Total | PASSED | FAILED | ERROR | SKIPPED |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| VAC | 20261101_142618 | 54fe3380-9fcc-4fb6-8ce8-6aad4a471383 | 25 | 24 | 0 | 1 | 0 |
| CAND | 20261101_213500 | 0532d310-60ab-4317-9cb7-e92005fefa11 | 25 | 24 | 0 | 0 | 1 |
| FLOW | 20261102_011836 | 40b57a16-e348-43e4-a6b9-e3d00bb8bf9e | 25 | 23 | 0 | 2 | 0 |
| TOTAL — 3 local executions | UNCLASSIFIED | Không phải shared ASM8 run | 75 | 71 | 0 | 3 | 1 |

Đã quét 15 official JUnit execution leaves trong Downloads; không có execution Recruitment mới hơn ba lượt trên. Outer launch folders của CAND/FLOW là `20261101_213459` và `20261102_011834`; final execution leaves là `20261101_213500` và `20261102_011836`. Giữ nguyên thư mục gốc. RESULTS75 mới được parse từ official HTML/JUnit, kiểm tra với callback JSON và supplementary XML. Không lấy console message làm assertion. Chuỗi lỗi có chữ FAILED trong log không thay official status ERROR.

## C. Source/config changes

**Không sửa runtime source hoặc config evidence path.** `PortablePaths.groovy`, `ExecutionContext.groovy`, `EvidenceCapture.groovy`, `AtomicFiles.groovy`, `RecorderBridge.groovy`, `ExecutionLease.groovy`, listener, 75 scripts/.tc, Profile, plan và workbook runtime giữ SHA trước/sau. Resolver hiện có đã đáp ứng relocation, project-relative fallback và dedicated absolute environment override. Historical absolute metadata không bị sửa.

209 file source/config/archive-reference trong project release khớp Downloads; sáu file Documentation riêng của bản release không tồn tại ở source reference, không phải source drift. README được cập nhật có chủ đích. Không sync lại toàn bộ project.

| Nhóm file | Thay đổi / before → after | Validation |
| --- | --- | --- |
| Root README, project README, report/evidence README, evidence/executions README | Thêm điều hướng member index; giải thích layout runtime và layout nộp | Relative-link checker |
| docs/automation/recruit-checklist.md | Kết quả VAC cũ/CAND-FLOW chưa xác minh → ba latest official executions và group pending | Official results + branch/runner inspection |
| Group runner README | Staging-only note → mô tả riêng release và NVThang, điều kiện adapter | Đọc run.mjs/summarize.mjs; --check offline |
| Individual/23130299/index.html, REPORT_LINKS.md, RESULTS75.json/md | Chưa có điểm vào tại member root → 75 case có provenance, timestamps, message, UUID, screenshot/log, blocker | 75 status khớp HTML/JUnit/callback/XML; 288 SHA |
| recruitment-failure-analysis.md, ANOMALY_SCREENSHOTS.json/md | Link gốc khó mở → index tương đối, phân loại latest/historical/unmatched | UUID/case/SHA hoặc unique timestamp/report match; UNKNOWN khi thiếu |
| maintenance/asm8-23130299/team_feedback.py | Thêm tooling audit/build/validate offline | Snapshot trước sửa, deterministic status derivation, link/preservation checks |
| maintenance/asm8-23130299/offline_feedback.py và checks/ | Thêm wrapper compile/harness, giữ nguyên hai harness từ Downloads | Harness SHA khớp gốc; fresh temporary directories; browser guards |
| team-feedback/ | Audit, source/reference inventory, validation và log mới | Không overwrite log/report lịch sử |

## D. Report/evidence paths

Mở [member report index](../../Test%20Report%20-%20Group%2001/Individual/23130299/index.html). [REPORT_LINKS.md](../../Test%20Report%20-%20Group%2001/Individual/23130299/REPORT_LINKS.md) có direct relative links đến **tất cả VAC/CAND/FLOW official HTML/JUnit, console0.log, execution0.log, supplementary HTML/JSON/XML/execution.log và evidence directory**.

- Official originals: `Test Report - Group 01/Individual/23130299/UNCLASSIFIED/Reports/<launch>/<suite>/<execution>/`.
- Supplementary originals: `Envidence - Group01/23130299/evidence/executions/23130299/RECRUIT_<module>_<UUID-prefix>/reports/UNCLASSIFIED/`.
- [RESULTS75.json](../../Test%20Report%20-%20Group%2001/Individual/23130299/RESULTS75.json) là **derived summary**, không phải official JUnit; original Documentation/RESULTS75.json/csv được giữ nguyên.
- **288/288 PNG latest** liên kết với report qua execution UUID, case-run UUID, case ID, manifest và SHA. Không thêm/copy/đổi tên PNG. UploadCV_04 có preflight/official log, không có screenshot do SKIPPED trước upload.

Không sửa historical HTML, JUnit, supplementary, manifests, screenshot hoặc absolute metadata cũ. Lỗi link trong analysis cũ được khắc phục bằng entry point mới; tài liệu lịch sử vẫn nguyên bytes để giữ provenance.

Summary mới bỏ các dòng machine/session diagnostics khỏi errorMessage; lỗi và stack chẩn đoán vẫn có nguồn official. Không sao chép session context, cookie hoặc browser profile. Official files đã có trong release không bị rewrite.

## E. Failure analysis

| Case | Final status | Điều đã xác minh | Còn thiếu / action |
| --- | --- | --- | --- |
| VAC_Filter_02 | ERROR | REQUIREMENT DISCREPANCY: nonexistent Vacancy requirement khác dropdown existing numeric ID; helper dừng | Nhóm trưởng xác nhận requirement; không đổi Expected Result/input để tăng PASS |
| CAND_UploadCV_04 | SKIPPED | CONFIGURATION BLOCKED: thiếu exact maxFileSize bytes; profile 0L chặn boundary | Có nguồn exact byte limit trước browser boundary test; không suy ra từ nhãn MB |
| FLOW_Status_01 | ERROR | Candidate81 toast/route sau Save, Profile No Records Found; full persistence/readback chưa đạt | Backend UNKNOWN; read-only reconciliation exact81 sau approval; không replay Save/Shortlist/pending write |
| FLOW_SelectInterviewer_02 | ERROR | VERIFIED ROOT CAUSE: Add another khác Add Another; locator đã sửa trong release trước task này | Offline FLOW/canary check không là browser retest; cần official run mới được phép |

[Failure analysis có link từng ảnh/log/report](../../Test%20Report%20-%20Group%2001/Individual/23130299/recruitment-failure-analysis.md). Không suy đoán HTTP401/403/422/500. STOPPED là checkpoint framework/helper trước final callback; ERROR suffix vẫn phải đối chiếu official result.

Anomaly inventory có **396 bản ghi đường dẫn** từ source/reference và repository: **12 latest path records cho 6 ảnh latest riêng biệt** (reference/submitted copies), **217 historical official verified ở source reference**, **167 HISTORICAL_EVIDENCE_UNMATCHED** giữ final UNVERIFIED và root cause UNKNOWN. Đây không phải 396 ảnh mới được commit. Các case lịch sử có log gốc ở Downloads được kiểm tra HTML/JUnit và runtime UUID hoặc timestamp duy nhất; source-only report/log ghi rõ REFERENCE ONLY, không tạo link giả đến file chưa nộp. Không dùng kết quả đó thay latest. Quan sát preflight/configuration có thể xác minh từ exception; root cause/cascade chưa chứng minh vẫn UNKNOWN, không tự gán FRAMEWORK/PREFLIGHT CASCADE cho mọi ảnh lỗi.

## F. Integration status

`NVThang` ở `6b4b362` có Node runner với Playwright PIM và Cypress Leave adapters, inventory Admin/Time/Recruitment. `run.mjs --check` chỉ inventory/source, đã xác minh 187 IDs và ba Recruitment suite 25 case đều SKIP. `--member=23130299` không phải Katalon adapter; chưa có execution standalone case/suite Katalon qua runner đó. `summarize.mjs` gộp group JSON và Pass/Fail/Skipped; chưa hỗ trợ nhập Katalon final ERROR riêng. Không chạy runner live hoặc summarize tạo group result mới.

Branch release base `6f8bd31...` chưa có `run.mjs`/`summarize.mjs`; hai file tồn tại ở checkout NVThang khác lịch sử. Không copy shared runner hoặc chỉnh code thành viên khác. CLI có trong Studio package ở máy kiểm tra; licensed KRE, manual-login orchestration và shared adapter NOT VERIFIED. Không cần bật adapter chưa đủ prerequisite chỉ để thay Skipped.

Nhóm trưởng cần phê duyệt:

1. Requirement VAC_Filter_02 và exact CV bytes; xác nhận active numeric manager/interviewer A/B, fixture ownership và cleanup.
2. Read-only reconciliation Candidate81; cấm replay uncertain writes.
3. Targeted browser smoke/retest theo [SMOKE_PLAN](../../automation/23130299/KatalonStudio/Documentation/SMOKE_PLAN.md), gồm case sửa locator và canary một interviewer; giữ report mới riêng.
4. Chọn runner/adapter opt-in, KRE license/manual login, scope listener trước khi import module khác và quy tắc aggregator giữ ERROR/SKIPPED.
5. Quyết định đưa Recruitment trở lại shared ASM8 run. Chỉ execution thật mới chứng minh group integration.

**Standalone: OFFLINE VERIFIED; BROWSER NOT VERIFIED. Shared suite: GROUP_INTEGRATION_PENDING / SKIPPED_BY_GROUP_DECISION.** Ba lượt cũ giữ UNCLASSIFIED; các lượt tương lai đặt FIRST_RUN/RETEST trước execution và không overwrite output.

## G. Preservation and validation

[Validation machine-readable](../../maintenance/asm8-23130299/team-feedback/validation.json): 75 scripts, 75 `.tc`, ba suite x 25, unique GUID, không trùng helper FQCN, suite order và source hashes giữ nguyên; workbook/Expected Results giữ nguyên. Offline compiler thực thi với Studio jars, Antlr2 và Parrot **104 production units PASS**. Harness portability gốc **10/10 PASS**; kiểm tra member layout, lifecycle helper, UUID/manifest, output isolation và launcher preflight-only PASS. Không username Admin hoặc đường dẫn máy tác giả trong runtime evidence config; nhánh ổ khác chỉ được resolve offline, không có browser/device test trên ổ đó.

Log portability cũ bị lỗi do chạy lại harness trong cùng temporary sandbox: `check_latest_flow.groovy` đã gọi portability ở cuối, rồi orchestrator cũ gọi portability lần nữa. File chống ghi đè tồn tại làm lần thứ hai dừng sau nhóm 9. Đã xác minh call chain; giữ nguyên log/validator. Wrapper mới cấp sandbox riêng cho từng harness, không thay assertions hay rebaseline manifest. Compiler/harness results mới không nâng official business results.

**Full legacy regression vẫn PARTIAL**, giữ nguyên 6 FAIL và 2 UNAVAILABLE_HISTORICAL_CAPTURE của release; không chạy lại toàn bộ legacy gate và không gán xanh. Report index/supplementary/evidence links kiểm tra bằng tooling mới. [Original historical validation](../../maintenance/asm8-23130299/OFFLINE_VALIDATION.json) giữ nguyên.

Không chỉnh PowerPoint, Excel, ZIP, source PIM/ADM/LEAVE/TIME, historical reports/evidence, .gitignore hoặc root .gitattributes. Chỉ thêm scoped `checks/.gitattributes` để hai harness gốc giữ LF/SHA khi clone trên Windows. Log offline mới chuẩn hóa newline cuối file; log lịch sử không đổi. Không browser/live API/Katalon suite execution/demo writes. Không reset/stash/revert/clean hoặc stage toàn repo. Worktree NVThang cùng bốn untracked evidence directories được bảo toàn.

[Final scoped review](../../maintenance/asm8-23130299/team-feedback/final-review.json) xác nhận **209 source reference files**, **29 original official report files** và **288 PNG** khớp originals. Kiểm tra bảo toàn trong repository bao phủ **1.254 file immutable**; runtime/shared helper không đổi. Không file mới trên 5 MiB, không conflict marker hoặc secret-pattern finding trong changed files. Đây là scoped review/pattern scan, không phải bảo đảm phát hiện mọi secret có thể có.

## H. Git

- Working branch: `feat/asm8-recruitment-feedback-23130299`, isolated worktree kế thừa `feat/asm8-recruitment-23130299`.
- Verified release commit: `e7c89705625cb4809ae6a8e5da81fb6871ab011d`; `git ls-remote` trên GitHub trả đúng SHA. Parent base khác NVThang, không merge/rebase branch chung.
- Remote: `origin` — `https://github.com/VoxNut/OrangeHRM-Testing.git`.
- Payload commit: `e1ee72c4639dbea2ffd2c0689a2bd7e9acbdea54` — 41 file bàn giao/tooling Recruitment. **Push SUCCESS** lên `origin/feat/asm8-recruitment-feedback-23130299`; `git ls-remote` trả đúng payload SHA sau push. [Git receipt](../../maintenance/asm8-23130299/team-feedback/git-receipt.json) ghi trạng thái tại thời điểm push payload. Commit bổ sung chứa cập nhật receipt này; xem `git log -1` để lấy tip cuối mà không nhúng self-referential commit hash.
- Branch release cũ vẫn ở `e7c89705625cb4809ae6a8e5da81fb6871ab011d`; không force push, không merge dev/main, không thay NVThang. Feature branch riêng được publish để nhóm trưởng review.
- Changed files được giới hạn ở README/checklist/report mới và tooling Recruitment như mục C; source/config/assertions/official originals không thay đổi. Kiểm tra actual Git diff, file lớn và secret scan trước commit.
