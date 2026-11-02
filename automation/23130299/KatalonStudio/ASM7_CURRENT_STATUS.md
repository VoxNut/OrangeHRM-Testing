<!-- ASM8 FULL AUDIT 20261102 START -->
# Recruitment ASM8 — latest local 71 PASSED / 3 ERROR / 1 SKIPPED

Đã kiểm kê 18.173 file nhìn thấy được; 425 lỗi truy cập đều nằm trong Chrome profiles, ghi PARTIAL_ENUMERATION. Đối chiếu từng case giữa HTML/JUnit của 15 execution reports hiện có. Latest VAC `20261101_142618` / `54fe3380-9fcc-4fb6-8ce8-6aad4a471383`: 24P/1E. Latest CAND `20261101_213500` / `0532d310-60ab-4317-9cb7-e92005fefa11`: 24P/1S. Latest FLOW `20261102_011836` / `40b57a16-e348-43e4-a6b9-e3d00bb8bf9e`: 23P/2E. Tổng 71P/0F/3E/1S/0U trên 75 final entries từ ba execution riêng; historical PASSED distinct72. Không phải report tích hợp ASM8.

Sửa production đúng một byte trong WorkflowChecks: nút native `Add Another` trước đó bị tìm bằng `Add another`. Candidate94/Shortlist đã verified, chưa Interview Save khi lỗi. Status_01 mới nhất là ERROR sau Candidate Save81, Profile No Records Found và ledger uncertain; không replay hoặc sửa shared CandidateChecks. VAC_Filter_02 requirement discrepancy và CAND_UploadCV_04 thiếu exact byte limit giữ nguyên.

Antlr2/Parrot PASS104/106 units mỗi parser. Focused retest PASS9 FLOW locator +10 portability +49/25 FLOW cũ +17 DOM. Full raw sweep35 PASS/6 FAIL/2 UNAVAILABLE; legacy Python2/9/1; giữ initial failures và focused retest riêng. Full regression PARTIAL: một legacy double yêu cầu spelling cũ, lỗi test mới đã sửa và retest PASS, cùng một generated offline args JSON bị harness cũ tái xuất. Full preservation gate còn FAIL cho artifact offline này; không rebaseline expected hash. Protected75 scripts/75.tc/6 suite files/order/shared/profile/workbook/185 Reports/1953 evidence/1596 frontend PASS SHA; historical source98/70/80/80 PASS, old report gates FAIL giữ nguyên.

Profile/plan đã có local, không secrets; stage exact bytes và source manifest. Git nhóm thiếu resource và cũ hơn16 scripts; không chép riêng hai file tạo bộ source không đồng bộ. GIT SYNC PENDING. Supplementary HTML/JSON/XML cả3 latest bundles đã có và khớp final status; thêm index, không sinh lại. Latest run kinds đềuUNCLASSIFIED; hướng dẫn FIRST_RUN/RETEST cho lượt tương lai, không đổi lịch sử.

**ASM8_INTEGRATION_NOT_VERIFIED — giữ quyết định Recruitment Skipped. BROWSER NOT VERIFIED cho thay đổi hiện tại.** Không mở browser/login/API live/Katalon cases/suites/demo writes, không commit/push/PPTX/ZIP. [Nguồn PowerPoint 8 phần + Appendix A–F](ASM8_RECRUITMENT_PPTX_SOURCE.md) · [Current report index](maintenance/asm8-full-audit-20261102/index.html) · [Audit/validation summary](maintenance/asm8-full-audit-20261102/audit-summary.json).

Toàn bộ nội dung lịch sử bên dưới giữ nguyên bytes; các số liệu trước đây không thay latest ở trên.
<!-- ASM8 FULL AUDIT 20261102 END -->

<!-- FLOW REVERIFICATION 20261102 START -->
# ASM7 — FLOW13/12 có sẵn được đối chiếu; CAND24/VAC24/62 historical cases frozen

Đối chiếu HTML/JUnit: execution yêu cầu222910 vẫn FLOW10 PASSED/15 ERROR; execution mới hơn001246 có sẵn là **FLOW13 PASSED/12 ERROR**. CAND24/0/1SKIPPED và VAC24/1ERROR giữ nguyên. Tổng latest61 PASSED/13 ERROR/1 SKIPPED. Ba PASS mới:_06,_07,SelectInterviewer_01; hợp historical62, supplement mới6 SHA, không ghi đè old59.

**Không sửa thêm production trong lượt kiểm chứng02/11.** Hai helper FLOW khớp bản sửa đã có. CAND/VAC/shared helpers/75 scripts/75.tc/suites/workbook/1596 frontend files/Reports hiện hữu giữ byte. Antlr2/Parrot104/106 PASS; focused49+25 Groovy và17 DOM PASS. Full regression PARTIAL, old FAIL/UNAVAILABLE giữ nguyên. Current5008-file preservation PASS; full historical reports FAIL96/92 và một old log drift đã có trước lượt này, không rebaseline để che lỗi.

**FLOW_REPAIR_BLOCKED_SHARED_HELPER_PROTECTION** cho12 ERROR mới: SelectInterviewer_02 bị shared PIM preflight HTTP422 khi xác minh interviewerB numeric323,11 case sau kế thừa lỗi. Không bỏ exact identity, sửa PIM/shared, clear unrelated latch hay bypass preflight. Backend422 root cause UNKNOWN. Lỗi context-load/login222910 vẫn chưa xác định HTTP root cause;13/12 không chứng minh nhánh recovery live đã chạy.

Không browser/login/API live/Katalon TC/suite/demo write/commit/push/ZIP. [30 câu trả lời hiện tại](maintenance/flow-repair-222910/reverification-20261102/repair-report.md) · [Regression/SHA](maintenance/flow-repair-222910/reverification-20261102/validation-summary.json). Operator giữ original smoke_06/_07/Select01/Validate01/History01 +canary_05; kiểm tra exact PIM422 precondition trước fullFLOW25. Không chạyCAND25 vì sửaFLOW.

Nội dung lịch sử bên dưới giữ nguyên bytes.
<!-- FLOW REVERIFICATION 20261102 END -->

<!-- FLOW REPAIR 222910 START -->
# ASM7 — FLOW222910 repaired offline; CAND24 / VAC24 / FLOW10 frozen

**Official (HTML/JUnit khớp): CAND24 PASSED/1 SKIPPED; VAC24 PASSED/1 ERROR; FLOW10 PASSED/15 ERROR. Tổng58 PASSED/16 ERROR/1 SKIPPED.** Không nâng business PASSED từ offline. Hợp lịch sử **59 distinct**.

- `_06`: ảnh chứng minh Schedule form đã render, heading dịch đúng, nhưng Candidate/Vacancy/Manager/Current Status trống; readiness gốc chặn đúng. Nguyên nhân GET/context-load phía server/session còn **UNKNOWN**, không có HTTP401/403/500 gốc để kết luận. Candidate118/Vacancy48 và Shortlist đã verified trước action; không replay.
- `_07` và13 case sau bị admission cascade: Dashboard recovery GET thấy login → SessionGuard exhausted → cached PREFLIGHT_FRAMEWORK_ERROR. Chưa phải14 business failures.
- Chỉ sửa **WorkflowChecks/WorkflowRouteRecovery**, nhánh recovery opt-in15 ERROR cases. Bounded owned-window/trusted-HTTPS GET, manual login qua API SessionRecovery hiện hữu, fresh numeric Manager/owned Candidate/Vacancy/status/action proof; một action GET remount; unknown writes giữ ERROR, không action/Save replay. CAND/VAC/shared helpers/config/scripts/.tc không sửa; FLOW10 giữ nguyên successful path và business bytes.
- **Compile PASS Antlr2/Parrot104/106**; focused **49+25 Groovy groups, 17 DOM VM PASS**. Full sweep **{'PASS': 36, 'FAIL': 4, 'UNAVAILABLE_HISTORICAL_CAPTURE': 2}**, Python **{'PASS': 2, 'FAIL': 9, 'UNAVAILABLE_HISTORICAL_CAPTURE': 1}**; mọi legacy common status giống checkpoint trước sửa. **Full regression PARTIAL**, không sửa old validators/manifests/evidence để xanh.
- **SHA PASS:**75 scripts+75.tc,3 suites×25/order/Expected Results, Reports/evidence/config/workbook/shared helpers; frontend1596 unchanged; historical source98/70/80/80 và59distinct118/118. Full historical report gates vẫn FAIL96/92 pre-existing paths, zero new drift.
- **BROWSER NOT VERIFIED.** Không browser/login/API live/Katalon execution/demo writes/CV/server/commit/push/ZIP.

[Đủ30 câu trả lời + smoke](maintenance/flow-repair-222910/repair-report.md) · [Frontend proof](maintenance/flow-repair-222910/frontend-source-proof.md) · [Root cause](maintenance/flow-repair-222910/root-cause.md) · [Regression](maintenance/flow-repair-222910/regression-results.md) · [SHA](maintenance/flow-repair-222910/source-preservation.json) · [Diff](maintenance/flow-repair-222910/final.diff).

Operator smoke: **ScheduleInterview_06 → _07 → SelectInterviewer_01 → ValidateAction_01 → History_01**, thêm protected canary **ScheduleInterview_05**. Chỉ sau smoke đạt mới FLOW25 đúng thứ tự trong execution mới; không chạy CAND25 chỉ vì sửa FLOW. Giữ Reports cũ/mới riêng.

Các phần bên dưới là lịch sử; giữ nguyên bytes, không thay thế kết quả mới nhất này.
<!-- FLOW REPAIR 222910 END -->

<!-- CAND RETEST 1609 REPAIR START -->
# ASM7 — CAND execution 01/11/2026 16:09: source repaired, browser retest pending

**Official: 17 PASSED / 7 ERROR / 1 SKIPPED**, HTML/JUnit khớp25 cases, execution `a3d28ae2-49be-428c-a768-e23b72fcc975`. Offline repair không đổi official status. Hợp **55 historically PASSED distinct**, không có PASSED lần đầu để thêm source-lock manifest.

- **Create_08:** Candidate96/Vacancy19 đã confirmed; List READY nhưng Vacancy popup rỗng. Add/List dùng cùng endpoint, khác `status`/`excludeInterviewers`. Chưa có live summary response để kết luận HTTP/eligibility/reset/build mismatch. Helper chỉ dùng native exact Name lookup khi fresh GET khớp toàn bộ Candidate + numeric Vacancy và List summary200 xác nhận option thiếu; giữ unique rows, Vacancy, native eye, exact numeric URL và full profile assertions. Không bỏ verifyListAndReopen, không Save96 lại.
- **Filter_02:** Candidate107 chỉ identified/uncertain theo evidence thật; chưa có readback live mới. Thêm reconcile-only exact GET, pending-write registry và một profile GET retry, không tạo lại/replay POST. API-only proof không là recordVerified; original UI assertions vẫn bắt buộc. Root Vue mount, profile component, translations, resource HTTP lỗi và available console errors được chẩn đoán riêng; không gán lỗi này là Chrome error hay session expiry khi chưa có bằng chứng.
- **Năm case cascade:** bounded same-origin document restoration trước PIM; owned driver/window, real Candidates list, pinned numeric Manager và original pending Candidate/Vacancy phải được xác minh. Chỉ clear transient document denial có provenance sau proof; unrelated fatal giữ canonical object. Healthy path không thêm API/navigation; VAC/FLOW ngoài nhánh recovery mới.
- **Preservation PASS:**75 scripts +75 `.tc`,3 suites×25/order/IDs/Expected Results, profile/workbook mappings, Reports/evidence/old ledgers và historical locks giữ byte. passed49 source98/98, passed35 70/70, regression30/preflight58 mỗi80/80. Frontend1596 files giữ SHA, aggregate `4b73abf630d45c32682ca646863f163fb3b30ed33b4876e9075a301162f3009b`.
- **Final compile PASS:** Antlr2 và Parrot,104 production/106 generated units; source inputs stable. **Focused gate11/11 PASS:** Groovy mới28 nhóm, VM mới24, Navigation28 +VM15, Session52, PIM27, Workflow/prior-repair25. Đây là offline doubles/source contracts, không phải business PASSED.
- **Full regression PARTIAL:** raw sweep32 PASS/6 FAIL/2 UNAVAILABLE; new harness boundary/export và navigation VM environment failures đã có targeted retest PASS. Original source_aligned/preflight58 driver doubles, old session helper-hash gate và original navigation VM thiếu window vẫn được giữ đúng FAIL. Legacy Python2 PASS/9 FAIL/1 UNAVAILABLE. Full-history passed49 FAIL96 report paths, passed35 FAIL92, tất cả có ngay tại checkpoint trước sửa; không sửa manifests/reports để xanh.
- **Production sửa2, thêm1:** CandidateChecks.groovy, CandidateNavigationRecovery.groovy, mới CandidateReadback.groovy. Auth/PIM/Workflow/VAC helpers khác không đổi. Không sửa bất kỳ protected business script/`.tc` nào.
- **Blockers giữ nguyên:** UploadCV_04 CONFIGURATION_BLOCKED thiếu exact numeric cvLimitBytes; VAC_Filter_02 REQUIREMENT_DISCREPANCY. Không đoán1MB/đổi workbook. Không browser/login/live API/demo write/upload/commit/push/ZIP.

[Báo cáo đủ22 câu trả lời + smoke](maintenance/cand-navigation-repair/retest1609/repair-report.md) · [Source proof](maintenance/cand-navigation-repair/retest1609/frontend-proof.md) · [Validation summary](maintenance/cand-navigation-repair/retest1609/validation-summary.json) · [SHA preservation](maintenance/cand-navigation-repair/retest1609/preservation.json) · [Dependency/SHA impact](maintenance/cand-navigation-repair/retest1609/dependency-impact.json) · [38-scenario coverage](maintenance/cand-navigation-repair/retest1609/coverage38.json).

Smoke operator: **Create_08 → Filter_02 → Filter_03 → Update_01 → UploadCV_05 → Create_10**, rồi CAND25 riêng VAC/FLOW. Cross-case recovery cần cùng execution/browser để được chứng minh; chạy standalone chỉ kiểm tra từng case. Candidate107 cũ không tự được coi là reconciled khi bắt đầu execution mới. Live dropdown response, profile root cause, persistence/recovery và mọi runtime sau sửa vẫn **BROWSER NOT VERIFIED**.

Các phần bên dưới là lịch sử trước execution16:09, không phải kết luận hiện tại.
<!-- CAND RETEST 1609 REPAIR END -->

<!-- CAND NAVIGATION REPAIR START -->
# ASM7 — CAND navigation recovery, source audited; live retest pending

Official mới nhất: **VAC24 PASSED/1 ERROR; CAND7 PASSED/17 ERROR/1 SKIPPED**. Đã đối chiếu HTML/JUnit từng case; CAND trước20/4/1. Không đổi official status từ offline.

- Audit đúng OrangeHRM_Frontend_Source trước sửa:32 related files +2 maps;1596 files giữ nguyên SHA. **Không có Vue Router trong navigation chain**; sidebar anchor, topbar location.replace và navigate() location.href đều full-document navigation.
- Create_08 đã fresh-verify Candidate102/all fields rồi sidebar viewRecruitmentModule gặp **HTTP500/Chrome error document**. Không phải lỗi Candidate Save. Create_09 bị ORIGIN trước fetch;15 lỗi sau dùng cached denial. Nguồn backend500 chưa chứng minh.
- Sửa3 helper RecruitmentUi/ConfigurationPreflight/SessionRecovery, thêm CandidateNavigationRecovery. Chỉ bounded read-only Candidates GET/manual auth; same-origin/API guard giữ nguyên; fresh pinned numeric manager; không replay Save hoặc case body. Giữ nguyên CandidateChecks và toàn bộ list/reopen assertions.
- **55 historically PASSED source cases được bảo vệ**, all75 scripts/.tc/order/inputs/Expected Results nguyên byte. Passed49 source98/98, passed35 source70/70 và historical80/80×2 PASS. Full-history gates vẫn **FAIL95/92 report paths**, có trước lượt này, không sửa manifests/reports để che lỗi.
- Final Antlr2/Parrot PASS103 production/105 generated. Retest mới **Groovy28/28, VM15/15, Session52/52, PIM27/27, prior repair25/25 PASS**. Full legacy regression vẫn PARTIAL; FAIL/UNAVAILABLE ghi riêng, initial/retest logs được giữ.
- Create_10 và ba lỗi30-character trước vẫn **BROWSER NOT VERIFIED**, vì execution mới chưa chạy tới business steps. UploadCV_04 CONFIGURATION_BLOCKED exact bytes; Filter_02 REQUIREMENT_DISCREPANCY chưa phê duyệt.
- **Không browser/login/API live/demo writes, commit/push/ZIP hoặc sửa frontend/workbook/reports gốc.**

Xem [35 câu trả lời và root cause](maintenance/cand-navigation-repair/navigation-root-cause.md), [source proof](maintenance/cand-navigation-repair/frontend-source-proof.md), [regression45](maintenance/cand-navigation-repair/regression-results.md), [SHA](maintenance/cand-navigation-repair/sha-verification.json).
Smoke đầu tiên: **RECRUIT_CAND_Create_08 → Create_09**, rồi Create_10 → UploadCV_01 → UploadCV_05 → Filter_01 → Update_01 → CancelUpdate_01 → VerifyDetails_01. Chạy tuần tự; original/FIRST_RUN/RETEST riêng. Mọi integration sau sửa còn **BROWSER NOT VERIFIED**.

Các mục phía dưới là lịch sử; không thay thế baseline mới nhất này.
<!-- CAND NAVIGATION REPAIR END -->

<!-- PASSED49 STATUS START -->
# ASM7 — official49 PASSED locked; offline repair; browser retest pending

Cập nhật 2026-11-01. **Official75 = 49 PASSED +25 ERROR +1 SKIPPED, 0 FAILED.**
Đã đối chiếu từng case giữa HTML/JUnit của VAC114617, CAND120620, FLOW124623.
Không chạy browser/login/API live hoặc ghi demo; không nâng case nào thành PASSED.

- Khóa current49 scripts +49 .tc; current49 ∪ old35 =50 distinct, hợp thêm historical80 =55. Hai bộ80/80 SHA và toàn bộ75 scripts/.tc/order/Expected Results giữ nguyên.
- Passed49 verifier PASS; old35 source70/70 PASS nhưng **FULL OLD35 GATE FAIL** do hai execution0.log đã lệch trước lượt này; không sửa reports hoặc manifest để che lỗi.
- FLOW: ảnh ScheduleInterview_01 là **HTTP500 trong Shortlist setup**, không phải bằng chứng lỗi dịch heading. Thêm readiness theo candidate/action/status/control, fail-fast error document và safe Dashboard GET cho admission của case kế tiếp; không replay action/Save. Nguồn server500 chưa xác định.
- ORIGIN xuất phát từ JavaScript so endpoint origin với effective document origin, không phải route action bị regex cũ từ chối. Giữ same-origin GET, thêm diagnostics, chỉ nhận known routes; không đoán profile URL.
- CAND: sinh Last Name fixture hợp lệ cho suffix dài, giữ toàn bộ ownership token và business overrides; bổ sung field-level masked diagnostics. Create_10 script/.tc nguyên byte; chỉ reconcile đúng owned numeric Vacancy rồi reload Add một lần khi option thiếu. Nguyên nhân server/eligibility của option rỗng vẫn chưa chứng minh.
- Compile Antlr2/Parrot PASS102 production/104 generated. New Groovy25/25, JS7/7, session compatibility52/52, source audit19/19 PASS. Existing Groovy16 PASS/3 FAIL/2 UNAVAILABLE; Node8 PASS; legacy Python2 PASS/9 FAIL/1 UNAVAILABLE. **Full regression PARTIAL**, có đủ logs; original session harness FAIL ở old blanket helper-byte assertion, adapter hành vi báo riêng.
- UploadCV_04: **CONFIGURATION_BLOCKED**, chưa có exact numeric bytes. Filter_02: **REQUIREMENT_DISCREPANCY**, chưa phê duyệt đề xuất mới.

Chi tiết27 mục, evidence/root causes/SHA/dependency impact và smoke: [passed49 protection report](maintenance/passed49-lock/protection-report.md).
Smoke đầu tiên: **RECRUIT_FLOW_ScheduleInterview_01**, rồi _02 → SelectInterviewer_01 → ValidateAction_01 → History_01; sau đó bốn CAND. Chạy tuần tự, FIRST_RUN/RETEST riêng. Mọi integration sau sửa vẫn **BROWSER NOT VERIFIED**.

Các mục phía dưới là lịch sử, không thay thế baseline mới nhất này.
<!-- PASSED49 STATUS END -->

# ASM7 — khóa 35 official PASSED, sửa infrastructure cho 13 CAND ERROR

Cập nhật 2026-11-01. **OFFICIAL PASSED BASELINE: 35/50; SOURCE LOCK VERIFIED;
COMPILE PASS; BROWSER NOT VERIFIED AFTER REPAIR.** Không chạy browser/Katalon
case/suite, gọi API live hoặc ghi server trong lượt sửa này.

- Official VAC 20261101_102651: 24 PASSED/1 ERROR. CAND 20261101_104433:
  11 PASSED/13 ERROR/1 SKIPPED; HTML/JUnit đồng nhất từng case.
- Khóa đủ 35 script + 35 .tc, SHA-256 manifest `ce7a874b68ba84e28c3af1210de2353f4a1335836d2ee241e5601ad829e2db41`.
  Không protected file nào thay đổi. Toàn bộ 75 scripts/75 .tc, suite IDs/order,
  Expected Results, workbook, frontend và report cũ giữ nguyên. Cả hai historical
  manifests khớp 80/80 SHA; 2.378 frozen files giữ nguyên.
- Production chỉ thêm SessionRecovery, sửa hook opt-in trong RecruitmentUi và
  listener cho đúng 13 CAND ERROR. SessionGuard/ApiReadTransport/identity/PIM/
  autocomplete/CandidateChecks/VacancyChecks/native Save giữ nguyên.
- Create_12: ảnh lỗi cho thấy form đã mount nhưng nhãn là khóa dịch chưa được dịch;
  locator Add Candidate tiếng Anh không khớp. 401 tiếp theo đã được chứng minh;
  chưa chứng minh session timeout/server reset gây lỗi dịch. Login → request 401
  đầu tiên: 933,998 giây. Chi tiết, ảnh và console-line provenance trong báo cáo.
- Recovery chỉ sau lỗi authentication: manual login trên Chrome Katalon đang sở
  hữu, 120 giây chờ login, tối đa 2 lần/execution, fresh read + cùng numeric Manager
  ID + owned record proof. Không replay Save/POST/Update/Delete khi outcome chưa rõ.
- Offline: **53/53 nhóm contract mới PASS**; compile Antlr2/Parrot PASS
  101 production / 103 gồm generated units. Groovy {'PASS': 17, 'FAIL': 2, 'UNAVAILABLE_HISTORICAL_CAPTURE': 2};
  Node {'PASS': 8}; Python legacy {'PASS': 2, 'FAIL': 9, 'UNAVAILABLE_HISTORICAL_CAPTURE': 1}. Full regression là PARTIAL,
  không che legacy FAIL/UNAVAILABLE. Không suy ra business PASSED mới từ doubles.
- **REQUIREMENT DISCREPANCY:** VAC_Filter_02; đề xuất riêng, chưa thay requirement.
  **CONFIGURATION BLOCKED:** UploadCV_04 chưa có exact numeric Resume maxFileSize.

Xem [protection-report.md](maintenance/passed35-lock/protection-report.md) cho đủ
20 câu trả lời, diff, dependency impact, evidence, regression và smoke/retest.
Xem [filter02-proposal.md](maintenance/passed35-lock/filter02-proposal.md) để quyết
định riêng về objective/Expected Result của case chưa PASSED này.

Smoke do người chạy thực hiện: Create_12 → UploadCV_01 → Filter_01 → Update_01;
UploadCV_04 sau khi đủ bytes; VAC_Filter_02 sau quyết định requirement. Sau đó
full CAND25, giữ FIRST_RUN/RETEST riêng, không chạy chồng hai suite.

Các mục bên dưới là lịch sử, không thay thế baseline mới nhất ở trên.

<!-- PASSED35 STATUS END -->
# ASM7 CURRENT STATUS — PIM read, pagination, Base URL và preflight

Cập nhật 2026-11-01. **OFFLINE REPAIR VERIFIED; BROWSER NOT VERIFIED.** Không mở Chrome, đăng nhập, gọi API demo, chạy Katalon TC/suite hoặc tạo/xóa Employee live. Không commit/push/copy repository nhóm/ZIP. `git status` không nhận diện repository tại workspace này; không khởi tạo hay đổi repository.

## Sửa cú pháp Groovy — kiểm chứng lại ngày 2026-11-01

**PRODUCTION COMPILE PASS; PIM/TRANSPORT OFFLINE PASS; BROWSER NOT VERIFIED.** Riêng lượt sửa cú pháp này chỉ đổi **1 file production**: `Keywords/recruitment/PimEmployeeFixtures.groovy`; cập nhật thêm chính file trạng thái này. Không sửa validator/harness/manifest lịch sử, profile, script test hoặc business assertions.

Compiler tái hiện trên source gốc: **`PimEmployeeFixtures.groovy:119:27: unexpected token: =`**, trong `inventory(Closure accept=null)`. Dùng JRE và `plugins/org.codehaus.groovy_3.0.25.v202609301323-e2609/lib/groovy-3.0.25-indy.jar` của **Katalon 11.6.0**, gọi `org.codehaus.groovy.tools.FileSystemCompiler --encoding UTF-8`, với **`-Dgroovy.antlr4=false`**. Parser xác nhận là `org.codehaus.groovy.antlr.AntlrParserPluginFactory`.

Nguyên nhân được kiểm tra bằng parser: Antlr2 từ chối khai báo nhiều biến trong initializer của vòng `for` này; riêng phần update nhiều biểu thức bằng dấu phẩy cũng báo `expecting ')', found ','`. Những khai báo nhiều biến ngoài vòng `for` và các generic/closure/map/list/ternary còn lại đều compile được, nên giữ nguyên. Parrot mặc định (`org.apache.groovy.parser.antlr4.Antlr4PluginFactory`) chấp nhận source cũ; vì vậy chỉ chạy harness mặc định trước đây không bắt được lỗi tương thích Antlr2 này. Không thay/cài Groovy hoặc dependency.

Trước, dòng 119:

```groovy
for(int offset=0,pageNumber=0;pageNumber<100;offset+=50,pageNumber++) {
```

Sau, dòng 119–120:

```groovy
for (int pageNumber = 0; pageNumber < 100; pageNumber++) {
    int offset = pageNumber * 50
```

Thân vòng lặp giữ nguyên từng byte. Offset vẫn 0, 50, ..., 4950; giữ 100 trang/lượt, 3 lượt, verification budget 100, progress/completeness/fingerprint checks, duplicate-ID handling, early-stop và exception/assertion. Không đổi discovery, numeric identity/exact autocomplete, A/B distinct IDs, provisioning opt-in, ledger/ownership, one-submit/reconcile, admission, classification, evidence/reporting. Incomplete scan vẫn ném lỗi, không trở thành danh sách rỗng hợp lệ.

Kết quả thực chạy sau sửa:

- **Antlr2 compile: PASS 100 real source units; PASS 102 units gồm generated Libs/Include hiện có. Parrot compile: PASS cùng 100/102 units.** Tất cả Keywords, 75 business scripts, suite Groovy và listener đều được biên dịch; đủ 11 helper/listener yêu cầu có output `.class`. Không còn lỗi syntax/compilation production sau lần sửa này; không sửa production nào khác. Không chạy initializer/TC/suite.
- **PIM provisioning 25/25 nhóm PASS; PIM read repair 27/27 nhóm PASS; 0 FAIL.** Chạy cả bằng classes Antlr2 và bằng harness Parrot hiện có với browser-launch guard. **Transport JS 17/17 nhóm PASS**, VM dùng đúng constant SCRIPT xuất từ compiled production; không network. Bao gồm pagination/duplicate/empty/no-progress/incomplete/early-stop, numeric A/B, appGlobal undefined/Base URL, propagation/preflight, opt-in false/no create, uncertain Save/no replay.
- Full Groovy harness: **16 PASS / 2 FAIL / 2 UNAVAILABLE_HISTORICAL_CAPTURE**. Full Node: **8 PASS / 0 FAIL**. Current structural checks: **12 PASS / 0 FAIL**, gồm no fixed sleep, profile false, structure và protected SHA. Legacy Python: **2 PASS / 9 FAIL / 1 UNAVAILABLE**. Existing audit functions: **8 PASS / 4 FAIL**; không gọi toàn bộ historical gate là PASS.
- Legacy Groovy FAIL: `check_source_aligned_offline.groovy`, `check_preflight58_offline.groovy` do driver doubles cũ thiếu currentUrl/read transport. UNAVAILABLE: `check_navigation_offline.groovy`, `check_performance_offline.groovy` thiếu historical XML.
- Legacy Python FAIL trong lượt này: `check_passed14.py`, `check_recovery_integrity.py`, `validate_offline.py` dừng ở SHA project `.prj` khác lock cũ; `check_preflight58.py`, `validate_regression30.py`, `completion75_verify.py`, `preflight58_verify.py` dừng ở SHA suite FLOW khác lock; `check_source_aligned.py` dừng ở reference `viewCandidates.html` khác lock; `validate75_offline.py` gặp PermissionError trong runtime Chrome-cache. `check_performance75.py` UNAVAILABLE do thiếu HTML benchmark. Hai Python PASS: frontend evidence và launcher offline. Không sửa lock/manifest để đổi kết quả.
- Bốn old-audit FAIL: hai `execution0.log` khác baseline cũ (VAC 20261031_221906 và CAND 20261030_184322), hai frontend harness đã khác baseline từ trước, hai static safety/wait gates còn dùng anchors trước refactor. **SHA snapshot đầu/cuối lượt sửa này xác nhận toàn bộ Reports/evidence giữ nguyên**; không restore hoặc sửa historical evidence để làm xanh audit.

Bảo toàn: **75 Groovy scripts, 75 .tc, 3 suites × 25 links đúng order/IDs/GUID-bearing files; 80/80 SHA trong cả preflight58-lock và regression30-lock khớp**. Workbook/75 Expected Results, 1.596 frontend source files, profile false, original reports/evidence và toàn bộ maintenance validators/manifests giữ nguyên so với đầu lượt. Không mở browser, đăng nhập, gọi API live, tạo Employee, chỉnh repository nhóm, commit/push hoặc ZIP; không tuyên bố 75/75 business TC Passed.

SHA-256 production trước: `4fb91ad87e9dd72495ca4b895788f1ba08fe939379d7a017117b8e7484757b9c`.
SHA-256 production sau: `b2408688c6af8b14010982f0b2de2112e813c8e9c953561c445c802215395a28`.

Log compiler trước/sau, parser probes, regression, legacy stacks và snapshot SHA giữ ở `C:\Users\Admin\AppData\Local\Temp\asm7-syntax-fix-svvhm54p` (`initial-3.0.25-antlr2.log`, `fixed-antlr2-production.log`, `fixed-antlr2-generated.log`, `parser-probe.log`, `parser-probe-parrot.log`, `regression.log`, `antlr2-regression-results.json`, `groovy-node-results.json`, `legacy-results.json`, `before-sha256.json`). Không tạo báo cáo riêng trong project. Các kết quả ở những mục bên dưới là lịch sử lượt PIM read repair, không thay thế số liệu vừa chạy ở mục này.


## Evidence thực tế và official results

Không tìm thấy đúng bốn tên export `console0(20261101-021444).log`, `failure-analyze(1).json`, `console0(20261101-021455).log`, `failure-analyze(2).json` sau khi tìm trong project, Reports và Downloads. Không tuyên bố đã đọc file vắng mặt. Đã đọc đầy đủ hai console0.log, hai failure-analyze.json tương ứng, execution0.log, HTML/JUnit thực có và runtime context/preflight của hai UUID.

| Execution | Bắt đầu local | Official HTML/JUnit đang có |
|---|---|---|
| CAND `e11d4e3d-959a-48bc-b08b-20c28e7f0d31` | 2026-11-01 09:08:22 | 25 case: **1 PASSED, 23 ERROR, 1 SKIPPED, 0 FAILED** |
| VAC `5e6373de-2d0e-4fb6-9cf8-46575ffc157a` | 2026-11-01 09:07:36 | Snapshot 13 case: **1 PASSED, 12 ERROR, 0 FAILED** |

CAND Create_06 và VAC Create_05 PASSED được cả HTML/JUnit xác nhận, không suy từ ASSERTIONS COMPLETED. CAND UploadCV_04 SKIPPED vì thiếu exact cvLimitBytes. VAC Create_13 thiếu endTime, JUnit suite time âm; 12 case còn lại không có final entry official dù console tiếp tục: **FINAL STATUS NOT INDEPENDENTLY VERIFIED**. VAC_Filter_02 có REQUIREMENT_DISCREPANCY trong console nhưng không có final entry trong official snapshot đang có. Không tự gán FAILED/PASSED.

CAND failure-analyze có24 object, VAC có25 object; đó không phải số final FAILED. [Crosscheck đầy đủ: input paths, per-case official result/case execution ID/profile](maintenance/pim-read-repair/official-crosscheck.json).

- [CAND HTML](Reports/20261101_090814/RECRUIT_CAND_25_ASC_001_TO_025/20261101_090814/20261101_090814.html), [CAND JUnit](Reports/20261101_090814/RECRUIT_CAND_25_ASC_001_TO_025/20261101_090814/JUnit_Report.xml).
- [VAC HTML](Reports/20261101_090726/RECRUIT_VAC_25_ASC_001_TO_025/20261101_090727/20261101_090727.html), [VAC JUnit](Reports/20261101_090726/RECRUIT_VAC_25_ASC_001_TO_025/20261101_090727/JUnit_Report.xml).

## Root cause và trước/sau

Lần trước discovery từ Vacancies chỉ có `(Deleted)`, gây NO_VERIFIED_HIRING_MANAGER. Lần này PIM discovery **đã được gọi nhưng chưa thành công**. Các lỗi mới thuộc framework sau chuyển discovery sang PIM.

1. **Global/Base URL:** PimEmployeeFixtures.readInternal cũ (executeAsyncScript dòng86, dereference dòng89) và EmployeeIdentity.resolve cũ (dòng36) dùng `new URL(window.appGlobal.baseUrl + endpoint, location.origin)`. URL được dựng ngoài promise catch, nên TypeError không gọi Selenium callback.
2. **Tab tạm trống:** preservingForm cũ mở `newWindow(WindowType.TAB)` rồi gọi resolveManager/inventory ngay, trước navigation. Tab mới bắt đầu ở `about:blank`, không có bootstrap appGlobal. ensureSession đã kiểm tra ở tab gốc trước khi mở tab; authenticated=true không chứng minh document tab mới đã mount. Đã thêm navigation qua runner.go tới Add Vacancy trước discovery và khôi phục tab gốc trong finally. Regression mô phỏng đúng newWindow→about:blank→discovery: bản compiled trung gian lỗi BASE_URL, bản cuối PASS cả đường thành công và lỗi/restore. Log cũ thiếu current URL từng GET nên không khẳng định mọi JavaScriptException đều cùng nhánh; đây là đường production tái hiện được nguyên nhân.
3. **Pagination:** code cũ `if (!seen.add(e.employeeId)) throw new Fault('BACKEND_UNAVAILABLE',...)` coi mọi ID lặp là backend unavailable. Không phân biệt trùng trong trang/giữa trang, không ghi offset/total/ID diagnostics. Chưa đủ bằng chứng quy duplicate cho server, sort, offset hoặc parse. Field cũ là empNumber; không có bằng chứng parse sai. Source chỉ chứng minh các sort không duy nhất như firstName, chưa chứng minh backend hỗ trợ sort empNumber.
4. **Cascade:** listener có thể log lỗi rồi Katalon vẫn gọi body. requireConfigured cũ đổi lỗi thành NO_VERIFIED_HIRING_MANAGER; VAC body lại gọi GET trong tab mới trống. Vì vậy cùng execution xuất hiện pagination error rồi baseUrl undefined. Hai lỗi phải sửa riêng.

Hai suites chạy chồng là giả thuyết tác động dữ liệu public demo, chưa là nguyên nhân chắc chắn. Cả hai có provisioning=false nên không mặc định chúng thêm Employee gây overlap. Concurrency không giải thích việc thiếu bootstrap trên about:blank.

[Katalon lifecycle docs](https://docs.katalon.com/katalon-studio/create-test-cases/test-fixtures-and-test-listeners-test-hooks-in-katalon-studio) nêu status listener steps không quyết định overall TC status; [skipThisTestCase](https://docs.katalon.com/katalon-studio/execute-tests/skip-test-cases-in-katalon-studio) là skip API được hỗ trợ. Bytecode local chỉ là bằng chứng bổ sung: javap có diagnostic access error, không gọi là complete runtime proof. Offline lifecycle gọi production listener với TestCaseContext double và production guards; không chạy suite engine.

## Base URL resolution contract và transport

ApiReadTransport.resolve dùng currentUrl của owned WebDriver trong Groovy, kiểm tra HTTPS/origin theo RecruitmentUi.BASE và route `/web/index.php/{dashboard,recruitment,pim}/...`. Context `/web/index.php` được chứng minh bởi bootstrap/script URLs trong `OrangeHRM_Frontend_Source/pages/current-page.html` và Axios baseURL + relative path trong `core/util/services/api.service.ts`. Không dùng document.baseURI/global chưa xác minh làm fallback. about:blank, thiếu context, login, foreign origin và configured base lặp context bị từ chối.

**Endpoint PIM: `/web/index.php/api/v2/pim/employees`.** Interviewer: `/web/index.php/api/v2/recruitment/interviewers`. Base dạng origin hoặc origin+/web/index.php resolve đúng một lần. Raw URL thiếu/lặp context bị JS guard từ chối. URL đã validate, params và requireTotal truyền bằng arguments; không nối untrusted URL vào JS. GET dùng credentials=same-origin, không đọc/lưu token/cookie, không HTTP write.

Callback finish có guard một lần; try/catch bao async setup; rejection, HTTP, JSON/schema và AbortError riêng. AbortController15s thực sự abort và callback; Selenium script timeout20s rồi restore original trong finally. Root WebDriverException giữ cause; JS errorType/detail và HTTP status giữ trong diagnostic. Fatal transport được giữ trong ExecutionContext cho cả PIM/Recruitment identity helpers; recovery bằng execution mới do operator khởi động, không biến lỗi transient thành identity missing vĩnh viễn.

| State | Kết quả admission |
|---|---|
| PIM_API_BASE_URL_MISSING_OR_INVALID | ERROR, no write |
| PIM_API_HTTP_ERROR | ERROR; giữ HTTP404/429/500/... riêng trong message/diagnostic |
| PIM_API_JSON_PARSE_ERROR / PIM_API_RESPONSE_SCHEMA_INVALID | ERROR; không trả danh sách rỗng giả |
| PIM_API_TIMEOUT / PIM_API_TRANSPORT_ERROR | ERROR; giữ root type/cause |
| PIM_PAGINATION_UNSTABLE / PIM_PAGINATION_NO_PROGRESS | ERROR; scan chưa đủ bằng chứng; no write |
| SESSION_EXPIRED | ERROR; không NO_ACTIVE_EMPLOYEE_AVAILABLE |
| PIM_PERMISSION_DENIED | Explicit permission/precondition blocker; no write |
| NO_ACTIVE_EMPLOYEE_AVAILABLE / EMPLOYEE_CREATION_DISABLED | Chỉ thiếu suitable identity được kiểm tra; explicit override không được thay người; OFF giữ configuration skip |
| EMPLOYEE_VERIFIED | Identity ready, không phải TC PASSED |
| EMPLOYEE_CREATED_BUT_NOT_SELECTABLE / HIRING_MANAGER_AUTOCOMPLETE_FAILURE / EMPLOYEE_IDENTITY_AMBIGUOUS | ERROR; không tạo thêm Employee để né lỗi |
| PREFLIGHT_FRAMEWORK_ERROR | Unexpected RuntimeException giữ cause; ERROR, không SKIP |

## Pagination mới và diagnostics

Giữ source contract limit50, offset, model=detailed, includeEmployees=onlyCurrent, employee.firstName/ASC; không bịa unique sort unsupported. empNumber phải numeric dương; textual Employee Id chỉ là employeeCode. Set canonical empNumber deduplicate, không biến overlap thành identity mới.

- Cache candidate pool theo execution. Chỉ early-stop khi fresh numeric GET và **native Recruitment exact autocomplete + selected ID** đều thành công. A/B khác numeric ID. Interviewer seed dùng pool hiện có; eligibility vẫn cần Recruitment autocomplete proof riêng.
- Overlap dùng được cho positive verified discovery. Trùng trong trang/giữa trang, page lặp và progress0 được phân biệt; không tự coi overlap là BACKEND_UNAVAILABLE.
- Nếu chưa có identity verified: tối đa3 scan attempts,100 page/attempt, giới hạn candidate verification, progress/terminal checks. Negative/provisioning đòi **hai scan liên tiếp đầy đủ, không duplicate, cùng tổng và cùng identity snapshot**. Incomplete/overlap/changed total/list/no-progress/budget limit không là bằng chứng thiếu Employee. Đây là read-consistency evidence, không phải snapshot isolation trên public demo.
- Provision chỉ sau suitable shortage được chứng minh và opt-in true; sau tạo invalidate inventory để tránh negative cache cũ. GET lỗi hoặc scan incomplete không tới Add/Save.
- Per-request/page diagnostic: execution/operation/case/run IDs, safe current URL, endpoint, offset/limit/sort, status/total/row/unique counts, first/last ID, duplicate lists, repeated-page, total-change, progress, sort keys, termination. elapsedMs của GET nối cùng operationId. Runtime log mới: `runtime/ASM7/executions/<UUID>/logs/pim-read.jsonl`; không token/cookie. Muốn kết luận duplicate live cần log này cùng IDs/metadata/timing từng page và session timeline.

## Canonical admission, lifecycle và GET counts

Mỗi case run có canonical decision và original exception reference. Listener lưu outcome; chỉ explicit configuration/data/permission blocker dùng skip API. Fatal được entry guard ném trong body một lần. RecorderBridge và RecruitmentUi constructors là entry path của75 script; Candidate/Workflow kế thừa guard. requireConfigured kiểm tra canonical trước resolved name, không đổi root lỗi thành NO_VERIFIED_HIRING_MANAGER. Pim manager/interviewer cũng guard. Unexpected RuntimeException trong admission được lưu thành framework ERROR có cause; không catch Throwable→SKIPPED.

Case cần Employee dùng chung fatal execution outcome; hai negative không cần Manager vẫn READY nếu auth/environment hợp lệ. Không fixture Save/business assertion khi admission denied. Constructor ở ngoài try của business script tránh screenshot/fixture cascade. Mỗi case vẫn có official ERROR riêng khi cần, nhưng cùng root cause, không phải nhiều business defect độc lập. Report lấy Katalon callback/official result, không markPassed/suy từ console.

| Phạm vi | Trước | Sau offline |
|---|---|---|
| Log thật CAND/VAC | 2 /25 PIM_READ measurements | Chưa có runtime sau sửa; không công bố tốc độ live |
| 25 admissions cùng fatal + helper tiếp | VAC có thể đọc lại mỗi body | 1 failed discovery; helper/identity transport tiếp trả original error, không GET thêm |
| 5.000 employees, A/B ở page đầu | Inventory cũ100 list pages trước A rồi fresh | PageProbe:1 list +2 numeric fresh=3 GET cho A/B; native identity transport GET riêng không cộng lẫn |
| Full65-row scan không early accept | Repeated ID có thể chặn | 4 list GET để chứng minh hai matching complete scans hai trang |
| Server bỏ qua offset | BACKEND_UNAVAILABLE do repeated ID | NO_PROGRESS, tối đa6 list GET/3 attempts; no write |

Không fixed sleep; dùng readiness polling. Không chạy hai full suites đồng thời trong smoke.

## Cấu hình, ledger và preservation

Profiles/default.glbl **nguyên byte; Boolean false**. Hai execution cũ profile default, snapshot READY/loaded settings match literals, context allowPimFixtureCreate=false. Không có bằng chứng stale profile; không suy lịch sử environment không được ghi. Strict Boolean/environment true/false override được regression. Nếu muốn framework tạo Employee khi thiếu suitable identity, operator phải chủ động bật **ASM7_ALLOW_PIM_FIXTURE_CREATE=true trước execution**, qua profile Boolean hoặc environment được Studio kế thừa; Refresh/Rebuild đúng project/profile. False vẫn cho phép discovery.

Giữ tối đa owned A/B mỗi execution, không login account, current/nonterminated proof, numeric ID, native exact suggestion, interviewer proof riêng, atomic ledger trước Save, một native submit, uncertain Save reconcile-only. Không reset ledger/UUID, sửa Vacancy người khác hoặc tự xóa Employee.

**80/80 historical SHA khớp**.75 scripts,75 .tc,6 suite files/3 suites,75 Expected Results, profile, workbook và1.596 frontend reference files không đổi. Checkpoint67 Reports files không đổi trong validation; không sửa/xóa original reports/manifests. Older baseline có mismatch execution0.log cũ; giữ kết quả đó thay vì restore/sửa hash để tạo PASS.

## Offline validation thực tế

- Compile **100 real Groovy units /103 gồm generated Libs/Include: PASS**; source inputs stable=true; không initializer/TC/browser.
- Groovy check files: **16 PASS /2 FAIL /2 UNAVAILABLE_HISTORICAL_CAPTURE**. New repair27 groups PASS; provisioning25 groups PASS; adapted frontend/native manager/interviewer PASS.
- Node mới nhất: **8 PASS /0 FAIL**; transport17 groups PASS trên **compiled Groovy SCRIPT** được export rồi dùng trực tiếp trong VM. Raw gate từng có1 Node failure khi so source CRLF với compiler LF; chỉ harness decoding được sửa, cùng17 groups retest PASS. Raw failure và retest giữ riêng.
- Python historical validator files: **2 PASS /9 FAIL /1 UNAVAILABLE**. Current structural Python file PASS với12 nhóm assertions. Tổng Python files: **3 PASS /9 FAIL /1 UNAVAILABLE**. Existing PIM audit functions:8 PASS/4 FAIL, không gộp thành all-green.
- [40 yêu cầu→asserting production/JS/structure harness](maintenance/pim-read-repair/regression40-coverage.json): coverage40 scenario, không phải40 business TC Passed.

[Latest summary](maintenance/pim-read-repair/final/latest-summary.json), [latest results](maintenance/pim-read-repair/final/latest-results.json), [raw gate summary](maintenance/pim-read-repair/final/validation-summary.json), [final log](maintenance/pim-read-repair/final-validation.log), [JS retest](maintenance/pim-read-repair/final/transport-retest.log), [current structural assertions](maintenance/pim-read-repair/current-audit.json), [legacy results/stacks](maintenance/pim-read-repair/final/legacy-results.json), [unchanged audit results](maintenance/pim-read-repair/final/audit-results.json).

Hai Groovy FAIL: check_source_aligned_offline.groovy, check_preflight58_offline.groovy — driver doubles không hỗ trợ getCurrentUrl/read-only identity transport. Hai UNAVAILABLE: check_navigation_offline.groovy, check_performance_offline.groovy — thiếu historical XML. Python PASS: check_frontend_evidence.py, check_launcher30_offline.py. Python UNAVAILABLE: check_performance75.py — thiếu historical HTML benchmark. Python FAIL: check_passed14.py, check_preflight58.py, check_recovery_integrity.py, check_source_aligned.py, validate_offline.py, validate75_offline.py, validate_regression30.py, completion75_verify.py, preflight58_verify.py. Lý do cụ thể: profile opt-in khác lock cũ, suite/reference HTML drift, hoặc broad runtime Chrome-cache PermissionError. Existing audit4 FAIL: older execution0.log mismatch, hai frontend harness adapt được phép, static HTTP/timeout/offset anchors trước refactor. Không sửa historical validators/manifests để che FAIL. New harness bổ sung production coverage tương ứng, không gọi các legacy FAIL là PASS.

Bốn harness adapt (bản trước sửa trong maintenance/pim-read-repair/original-harness): frontend Groovy trả source-shaped rows cho URL/params mới, giữ native selection/ID/distinct/delete assertions; mock PIM seed boundary để tránh browser, giữ pending Recruitment eligibility. Frontend/PIM Node dùng SCRIPT chung, validated absolute URL và schema/error classification mới. PIM Groovy thêm mock currentUrl,503 classification và expectation early-stop; full two-page pagination được kiểm tra riêng trên production inventory ở harness mới. Không sửa business Expected Result/assertions.

Chạy lại offline bằng interpreter thực (WindowsApps python alias hiện lỗi):

```powershell
& C:/Users/Admin/AppData/Local/Python/bin/python.exe maintenance/validate_pim_read_repair.py --final
& C:/Users/Admin/AppData/Local/Python/bin/python.exe maintenance/check_pim_read_structure.py
```

Full gate vẫn nonzero khi còn legacy FAIL. Gate compile/export production-api-read.js trước Node; không dùng compiled snapshot cũ sau khi sửa production.

## File đã sửa

Production8 file: **ApiReadTransport.groovy mới** (resolver/GET/callback/classification/latch); **PimEmployeeFixtures.groovy** (numeric schema/pagination/diagnostics/early proof/tab navigation/interviewer pool); **EmployeeIdentity.groovy** (transport chung); **ExecutionContext.groovy** (canonical exceptions/latch); **ConfigurationPreflight.groovy** (admission reuse/guard/root); **RecorderBridge.groovy**, **RecruitmentUi.groovy** (shared entry guards); **Test Listeners/Asm7EvidenceListener.groovy** (fatal defer tới body guard). [SHA before/after](maintenance/pim-read-repair/final/production-changes.json).

CandidateChecks, VacancyChecks, WorkflowChecks, WorkerSnapshot, ExecutionBrowser, ExecutionReport đã đọc, không cần đổi.75 business scripts không sửa. Maintenance: bốn adapted harness nêu trên; mới check_pim_read_repair_offline.groovy, check_pim_read_transport_offline.cjs, check_pim_read_structure.py, validate_pim_read_repair.py; task artifacts/baseline/diffs/coverage/crosscheck và duy nhất current report này. Không thêm runtime dependency.

## Blockers và smoke do operator chạy

**VAC_Filter_02 vẫn REQUIREMENT_DISCREPANCY:** VacancyDropdown chọn existing ID, không synthetic option/đổi Expected Result. **CAND_UploadCV_04 vẫn thiếu exact cvLimitBytes**, profile0; không đoán MB hay lấy PIM picture limit thay Resume limit.

Có thể bắt đầu smoke đầu tiên sau Refresh/Rebuild canonical project `OrangeHRM_RECRUIT_VAC_KatalonStudio.prj` và đúng profile. Live endpoint/data stability, PIM permissions/current status, native Manager A/B/interviewer, Add/unique-validation/Save/persistence/reconcile, Vacancy/Candidate persistence, uploads/downloads, workflow status/history và official reports sau sửa đều **BROWSER NOT VERIFIED**. Không tuyên bố75/75 Passed.

1. **Smoke A — RECRUIT_VAC_Create_01:** endpoint `/web/index.php/api/v2/pim/employees`, GET/PAGE diagnostics, suitable active identity + native Hiring Manager; chỉ provision khi opt-in true và thiếu identity thật; verify Vacancy persistence.
2. **Smoke B — RECRUIT_VAC_Create_05:** Required assertion độc lập Manager, không discovery/provision để làm negative.
3. **Smoke C — RECRUIT_CAND_Create_01:** owned Vacancy có verified Manager và Candidate persistence; không lấy Vacancy thiếu Manager từ negative case.
4. **Smoke D — RECRUIT_VAC_Update_03:** A/B khác numeric empNumber, verify persisted update.
5. **Smoke E — VAC25 → CAND25 tuần tự**, không chồng session trên public demo; sau đó mới FLOW25. Create_06 vẫn không cần Manager; UploadCV_04 giữ configuration blocker nếu thiếu exact bytes.

Đặt ASM8_RUN_KIND=FIRST_RUN hoặc RETEST trước execution, giữ reports/UUID riêng. Standalone cases có execution/cache riêng; suite chung mới chứng minh reuse qua cases. Sau fatal API/preflight, khắc phục environment rồi bắt đầu execution mới; không reset ledger/UUID để replay uncertain Save. Giữ console, HTML/JUnit, context.json, configuration-preflight.json, pim-read.jsonl, pim-fixtures.json. Cleanup Employee vẫn cần xác nhận thủ công theo exact owned IDs/dependencies sau khi giữ evidence; không auto delete.
