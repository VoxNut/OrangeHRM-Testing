# ERROR / STOPPED / SKIPPED — latest local executions

Official final status lấy từ HTML và JUnit đã đối chiếu từng case. Suffix ảnh không thay final status. Không replay uncertain Save.

| Case | Execution UUID | Official status | Verified observation / root cause | Unknown | Action / retest |
| --- | --- | --- | --- | --- | --- |
| RECRUIT_VAC_Filter_02 | 54fe3380-9fcc-4fb6-8ce8-6aad4a471383 | ERROR | Workbook yêu cầu tìm Vacancy không tồn tại; UI dùng dropdown existing ID. Helper dừng thay vì thay dữ liệu/Expected Result. | Chưa có quyết định nhóm trưởng giải quyết khác biệt workbook/UI. | Giữ ERROR; nhóm xác nhận requirement trước retest, không tự đổi oracle. / BROWSER RETEST NOT VERIFIED |
| RECRUIT_CAND_UploadCV_04 | 0532d310-60ab-4317-9cb7-e92005fefa11 | SKIPPED | Exact cvLimitBytes thiếu; profile0L; official SKIPPED do preflight. Không chạy upload boundary. | Chưa xác minh Resume byte limit thực tế. | Giữ SKIPPED; chỉ retest khi có giới hạn byte đã kiểm chứng. / BROWSER RETEST NOT VERIFIED |
| RECRUIT_FLOW_Status_01 | 40b57a16-e348-43e4-a6b9-e3d00bb8bf9e | ERROR | Candidate Save đã gửi, toast và numeric ID81 quan sát được; Profile No Records Found, không đạt full readback; Vacancy11 verified. Ledger candidate-save uncertain. | Không có response gốc chứng minh nguyên nhân record81 không hiển thị; không kết luận401/422/500 hay đã bị xóa. | BLOCKED_SHARED_CAND_PROTECTION: giữ shared CandidateChecks; reconcile-only exact81 bằng GET ở lượt được phép, không replay Save. / BROWSER RETEST NOT VERIFIED |
| RECRUIT_FLOW_SelectInterviewer_02 | 40b57a16-e348-43e4-a6b9-e3d00bb8bf9e | ERROR | Candidate94/Vacancy24 và Shortlist đã verified; Interview form ready, A selected. Exact locator Add another không khớp nút Add Another trong ảnh/source; timeout trước Interview Save. | Chưa xác minh live sau one-byte FLOW fix; không có bằng chứng lỗiPIM422 trong lần này. | Đã sửa duy nhất literal FLOW thành Add Another; giữ distinct active numeric A/B, status/history/Save guards. Operator retest _02 +single-interviewer canary. / BROWSER RETEST NOT VERIFIED |

## RECRUIT_VAC_Filter_02

- Screenshot: [`evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144123_001_BEFORE.png`](../evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144123_001_BEFORE.png) SHA256 `75e518d39e1380860c0ee8ad6b24ff0116d86e9d207c9e275d62bdb94e2dbd31`
- Screenshot: [`evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144124_002_STOPPED.png`](../evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144124_002_STOPPED.png) SHA256 `75e518d39e1380860c0ee8ad6b24ff0116d86e9d207c9e275d62bdb94e2dbd31`
- Screenshot: [`evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144125_003_CHECK.png`](../evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144125_003_CHECK.png) SHA256 `75e518d39e1380860c0ee8ad6b24ff0116d86e9d207c9e275d62bdb94e2dbd31`
- Screenshot: [`evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144125_004_ERROR.png`](../evidence/executions/23130299/RECRUIT_VAC_54fe3380-9fc/RECRUIT_VAC_Filter_02/r24db4656/144125_004_ERROR.png) SHA256 `75e518d39e1380860c0ee8ad6b24ff0116d86e9d207c9e275d62bdb94e2dbd31`
- Official log: [Reports/20261101_142618/RECRUIT_VAC_25_ASC_001_TO_025/20261101_142618/console0.log](../Reports/20261101_142618/RECRUIT_VAC_25_ASC_001_TO_025/20261101_142618/console0.log)
- Official HTML: [20261101_142618](../Reports/20261101_142618/RECRUIT_VAC_25_ASC_001_TO_025/20261101_142618/20261101_142618.html)
- Error / skip message: Test Cases/RECRUIT_VAC/RECRUIT_VAC_Filter_02 FAILED.
Reason:
java.lang.IllegalStateException: [BLOCKED / WORKBOOK-UI MISMATCH] REQUIREMENT_DISCREPANCY: RECRUIT_VAC_Filter_02 requires searching nonexistent Vacancy [VAC_NOT_EXIST_26c1d9385020]. Supplied viewJobVacancy.html and app.js use VacancyDropdown (select by existing id); no free-text Vacancy filter exists. No substitute fixture, incompatible Job Title or synthetic option was used. Requires an approved workbook/UI resolution; No Records Found remains unverified.
 at recruitment.VacancyChecks.searchMissingVacancy(VacancyChecks.groovy:414)
 at Script20261023080000.run(Script20261023080000.groovy:9)
 at com.kms.katalon.core.main.ScriptEngine.run(ScriptEngine.java:203)
 at com.kms.katalon.core.main.ScriptEngine.runScriptAsRawText(ScriptEngine.java:115)
 at com.kms.katalon.core.main.TestCaseExecutor.runScript(TestCaseExecutor.java:533)
 at com.kms.katalon.core.main.TestCaseExecutor.doExecute(TestCaseExecutor.java:524)
 at com.kms.katalon.core.main.TestCaseExecutor.processExecutionPhase(TestCaseExecutor.java:503)
 at com.kms.katalon.core.main.TestCaseExecutor.accessMainPhase(TestCaseExecutor.java:495)
 at com.kms.katalon.core.main.TestCaseExecutor.execute(TestCaseExecutor.java:359)
 at com.kms.katalon.core.common.CommonExecutor.accessTestCaseMainPhase(CommonExecutor.java:71)
 at com.kms.katalon.core.main.TestSuiteExecutor.accessTestSuiteMainPhase(TestSuiteExecutor.java:188)
 at com.kms.katalon.core.main.TestSuiteExecutor.execute(TestSuiteExecutor.java:115)
 at com.kms.katalon.core.main.TestCaseMain.startTestSuite(TestCaseMain.java:241)
 at S1101_3.run(S1101_3.groovy:41)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)

## RECRUIT_CAND_UploadCV_04

- Screenshot: MISSING / not captured before configuration skip; no substitute.
- Official log: [Reports/20261101_213459/RECRUIT_CAND_25_ASC_001_TO_025/20261101_213500/console0.log](../Reports/20261101_213459/RECRUIT_CAND_25_ASC_001_TO_025/20261101_213500/console0.log)
- Official HTML: [20261101_213500](../Reports/20261101_213459/RECRUIT_CAND_25_ASC_001_TO_025/20261101_213500/20261101_213500.html)
- Error / skip message: Test was skipped

## RECRUIT_FLOW_Status_01

- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012057_001_STOPPED.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012057_001_STOPPED.png) SHA256 `3ce18106cb3cf01d05563a61a445a73a584a423a04185f6e5b3a5b15119777b1`
- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012058_002_CHECK.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012058_002_CHECK.png) SHA256 `3ce18106cb3cf01d05563a61a445a73a584a423a04185f6e5b3a5b15119777b1`
- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012059_003_ERROR.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_Status_01/rb0410af1/012059_003_ERROR.png) SHA256 `3ce18106cb3cf01d05563a61a445a73a584a423a04185f6e5b3a5b15119777b1`
- Official log: [Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/console0.log](../Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/console0.log)
- Official HTML: [20261102_011836](../Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/20261102_011836.html)
- Error / skip message: Test Cases/RECRUIT_FLOW/RECRUIT_FLOW_Status_01 FAILED.
Reason:
org.openqa.selenium.TimeoutException: Expected condition failed: waiting for recruitment.CandidateChecks$_awaitCandidateRecord_closure15@362aa318
(tried for 30 seconds with 200 milliseconds interval)
Build info: version: &#x27;4.46.0&#x27;, revision: &#x27;df5a634 df5a6341cbb5f8f06b836bd595752775627c80dc&#x27;
System info: os.name: &#x27;Windows 11&#x27;, os.arch: &#x27;amd64&#x27;, os.version: &#x27;10.0&#x27;, java.version: &#x27;21.0.12&#x27;
Driver info: org.openqa.selenium.remote.RemoteWebDriver$ByteBuddy$jadaCpHD
Capabilities {acceptInsecureCerts: false, browserName: chrome, browserVersion: 156.0.8078.79, chrome: {chromedriverVersion: 156.0.8078.47 (1d17a2e2ee14..., userDataDir: C:\Users\Admin\Downloads\23...}, fedcm:accounts: true, goog:chromeOptions: {debuggerAddress: localhost:53850}, goog:processID: 40812, networkConnectionEnabled: false, pageLoadStrategy: normal, platformName: windows, proxy: Proxy(), se:cdp: ws://localhost:53850/devtoo..., se:cdpVersion: 156.0.8078.79, setWindowRect: true, strictFileInteractability: false, timeouts: {implicit: 0, pageLoad: 300000, script: 30000}, unhandledPromptBehavior: {alert: ignore, beforeUnload: ignore, confirm: ignore, default: ignore, file: ignore, prompt: ignore}, webSocketUrl: ws://localhost:47203/sessio..., webauthn:extension:cmtgKey: true, webauthn:extension:credBlob: true, webauthn:extension:largeBlob: true, webauthn:extension:minPinLength: true, webauthn:extension:prf: true, webauthn:virtualAuthenticators: true}
Session ID: 4ecb5e9e7134db47eef1e3787af03463
 at org.openqa.selenium.support.ui.WebDriverWait.timeoutException(WebDriverWait.java:85)
 at org.openqa.selenium.support.ui.FluentWait.until(FluentWait.java:234)
 at recruitment.CandidateChecks.awaitCandidateRecord(CandidateChecks.groovy:357)
 at recruitment.CandidateChecks.saveCandidateDraft(CandidateChecks.groovy:301)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.CandidateChecks$_saveNew_closure11.doCall(CandidateChecks.groovy:286)
 at recruitment.CandidateChecks$_saveNew_closure11.doCall(CandidateChecks.groovy)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.RecruitmentUi.measure(RecruitmentUi.groovy:72)
 at recruitment.CandidateChecks.saveNew(CandidateChecks.groovy:286)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.CandidateChecks$_create_closure10$_closure41.doCall(CandidateChecks.groovy:279)
 at recruitment.CandidateChecks$_create_closure10$_closure41.doCall(CandidateChecks.groovy)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.RecruitmentUi.measure(RecruitmentUi.groovy:72)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.CandidateChecks$_create_closure10.doCall(CandidateChecks.groovy:277)
 at recruitment.CandidateChecks$_create_closure10.doCall(CandidateChecks.groovy)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.CandidateChecks.create(CandidateChecks.groovy:283)
 at recruitment.WorkflowChecks.prepareWorkflowFixture(WorkflowChecks.groovy:23)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.WorkflowChecks$_fixture_closure1.doCall(WorkflowChecks.groovy:17)
 at recruitment.WorkflowChecks$_fixture_closure1.doCall(WorkflowChecks.groovy)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.EvidenceCapture.fixture(EvidenceCapture.groovy:17)
 at recruitment.WorkflowChecks.fixture(WorkflowChecks.groovy:17)
 at Script20261027000000.run(Script20261027000000.groovy:23)
 at com.kms.katalon.core.main.ScriptEngine.run(ScriptEngine.java:203)
 at com.kms.katalon.core.main.ScriptEngine.runScriptAsRawText(ScriptEngine.java:115)
 at com.kms.katalon.core.main.TestCaseExecutor.runScript(TestCaseExecutor.java:533)
 at com.kms.katalon.core.main.TestCaseExecutor.doExecute(TestCaseExecutor.java:524)
 at com.kms.katalon.core.main.TestCaseExecutor.processExecutionPhase(TestCaseExecutor.java:503)
 at com.kms.katalon.core.main.TestCaseExecutor.accessMainPhase(TestCaseExecutor.java:495)
 at com.kms.katalon.core.main.TestCaseExecutor.execute(TestCaseExecutor.java:359)
 at com.kms.katalon.core.common.CommonExecutor.accessTestCaseMainPhase(CommonExecutor.java:71)
 at com.kms.katalon.core.main.TestSuiteExecutor.accessTestSuiteMainPhase(TestSuiteExecutor.java:188)
 at com.kms.katalon.core.main.TestSuiteExecutor.execute(TestSuiteExecutor.java:115)
 at com.kms.katalon.core.main.TestCaseMain.startTestSuite(TestCaseMain.java:241)
 at S1102_0.run(S1102_0.groovy:41)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)

## RECRUIT_FLOW_SelectInterviewer_02

- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/015857_001_BEFORE.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/015857_001_BEFORE.png) SHA256 `23d421de228ae3c9f6c66ff32eae523fd718d2e582e76d6d7c79f34bb664a2dd`
- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/015959_002_STOPPED.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/015959_002_STOPPED.png) SHA256 `0a9fb0ed59ee6eb53dbbeea7a035758c80962f73a54c518fe021a23b3ffa5f89`
- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/020000_003_CHECK.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/020000_003_CHECK.png) SHA256 `0a9fb0ed59ee6eb53dbbeea7a035758c80962f73a54c518fe021a23b3ffa5f89`
- Screenshot: [`evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/020001_004_ERROR.png`](../evidence/executions/23130299/RECRUIT_FLOW_40b57a16-e34/RECRUIT_FLOW_SelectInterviewer_02/rf1409988/020001_004_ERROR.png) SHA256 `0a9fb0ed59ee6eb53dbbeea7a035758c80962f73a54c518fe021a23b3ffa5f89`
- Official log: [Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/console0.log](../Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/console0.log)
- Official HTML: [20261102_011836](../Reports/20261102_011834/RECRUIT_FLOW_25_ASC_001_TO_025/20261102_011836/20261102_011836.html)
- Error / skip message: Test Cases/RECRUIT_FLOW/RECRUIT_FLOW_SelectInterviewer_02 FAILED.
Reason:
org.openqa.selenium.TimeoutException: Expected condition failed: waiting for recruitment.RecruitmentUi$_click_closure8@511e402c
(tried for 30 seconds with 200 milliseconds interval)
Build info: version: &#x27;4.46.0&#x27;, revision: &#x27;df5a634 df5a6341cbb5f8f06b836bd595752775627c80dc&#x27;
System info: os.name: &#x27;Windows 11&#x27;, os.arch: &#x27;amd64&#x27;, os.version: &#x27;10.0&#x27;, java.version: &#x27;21.0.12&#x27;
Driver info: org.openqa.selenium.remote.RemoteWebDriver$ByteBuddy$jadaCpHD
Capabilities {acceptInsecureCerts: false, browserName: chrome, browserVersion: 156.0.8078.79, chrome: {chromedriverVersion: 156.0.8078.47 (1d17a2e2ee14..., userDataDir: C:\Users\Admin\Downloads\23...}, fedcm:accounts: true, goog:chromeOptions: {debuggerAddress: localhost:53850}, goog:processID: 40812, networkConnectionEnabled: false, pageLoadStrategy: normal, platformName: windows, proxy: Proxy(), se:cdp: ws://localhost:53850/devtoo..., se:cdpVersion: 156.0.8078.79, setWindowRect: true, strictFileInteractability: false, timeouts: {implicit: 0, pageLoad: 300000, script: 30000}, unhandledPromptBehavior: {alert: ignore, beforeUnload: ignore, confirm: ignore, default: ignore, file: ignore, prompt: ignore}, webSocketUrl: ws://localhost:47203/sessio..., webauthn:extension:cmtgKey: true, webauthn:extension:credBlob: true, webauthn:extension:largeBlob: true, webauthn:extension:minPinLength: true, webauthn:extension:prf: true, webauthn:virtualAuthenticators: true}
Session ID: 4ecb5e9e7134db47eef1e3787af03463
 at org.openqa.selenium.support.ui.WebDriverWait.timeoutException(WebDriverWait.java:85)
 at org.openqa.selenium.support.ui.FluentWait.until(FluentWait.java:234)
 at recruitment.RecruitmentUi.click(RecruitmentUi.groovy:203)
 at recruitment.RecruitmentUi.buttonClick(RecruitmentUi.groovy:215)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.WorkflowChecks$_fillInterview_closure28.doCall(WorkflowChecks.groovy:355)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
 at recruitment.WorkflowChecks.fillInterview(WorkflowChecks.groovy:354)
 at recruitment.WorkflowChecks.schedule(WorkflowChecks.groovy:394)
 at Script20261027000000.run(Script20261027000000.groovy:30)
 at com.kms.katalon.core.main.ScriptEngine.run(ScriptEngine.java:203)
 at com.kms.katalon.core.main.ScriptEngine.runScriptAsRawText(ScriptEngine.java:115)
 at com.kms.katalon.core.main.TestCaseExecutor.runScript(TestCaseExecutor.java:533)
 at com.kms.katalon.core.main.TestCaseExecutor.doExecute(TestCaseExecutor.java:524)
 at com.kms.katalon.core.main.TestCaseExecutor.processExecutionPhase(TestCaseExecutor.java:503)
 at com.kms.katalon.core.main.TestCaseExecutor.accessMainPhase(TestCaseExecutor.java:495)
 at com.kms.katalon.core.main.TestCaseExecutor.execute(TestCaseExecutor.java:359)
 at com.kms.katalon.core.common.CommonExecutor.accessTestCaseMainPhase(CommonExecutor.java:71)
 at com.kms.katalon.core.main.TestSuiteExecutor.accessTestSuiteMainPhase(TestSuiteExecutor.java:188)
 at com.kms.katalon.core.main.TestSuiteExecutor.execute(TestSuiteExecutor.java:115)
 at com.kms.katalon.core.main.TestCaseMain.startTestSuite(TestCaseMain.java:241)
 at S1102_0.run(S1102_0.groovy:41)
 at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
