# Assignment 8 — Recruitment, Group 01, Nguyễn Văn Thắng (23130299)

75 case / 3 suite VAC, CAND, FLOW (25 case/suite). Mở `OrangeHRM_RECRUIT_VAC_KatalonStudio.prj` bằng Katalon Studio **11.6.0**. Latest local: **71 PASSED / 0 FAILED / 3 ERROR / 1 SKIPPED**; đây là ba execution cá nhân riêng. **ASM8_INTEGRATION_NOT_VERIFIED; Recruitment giữ SKIPPED theo nhóm trưởng.** ERROR là trạng thái riêng do Katalon ghi, không quy đổi thành FAILED. Sửa source offline không nâng kết quả.

## Cài đặt và cấu hình

Cài Katalon Studio 11.6.0, Chrome và WebDriver tương thích (Studio Tools → Update WebDrivers nếu cần), quyền ghi thư mục project/output; tài khoản OrangeHRM hợp lệ do operator quản lý. Mở `.prj`, Refresh project và để Studio tạo lại `bin`, `Libs`, `.classpath`, `.project`; không chép Chrome profile hoặc license cá nhân. Không cần Object Repository/Data Files ngoài các source đã bàn giao: locator nằm trong Keywords, CV fixture được ResumeFiles sinh riêng theo execution.

Chọn Profile `default`; giữ `Profiles/default.glbl`, `maintenance/preflight58-plan.json`, `maintenance/workbook75-current.json` đúng bộ SHA. Để `ASM7_ALLOW_PIM_FIXTURE_CREATE=false`; không tạo/xóa Employee. Manager/interviewer phải là active numeric identity đã xác minh; không lấy option đầu tiên hoặc dùng tên giả. Job Title hiện cấu hình `QA Engineer` phải tồn tại thực tế. Không điền mật khẩu vào Profile/README/Git. `cvLimitBytes=0L` cố ý chặn UploadCV_04 đến khi có giới hạn byte chính xác. `phoneValidationConfirmed=false`, `timeFormat` rỗng giữ cơ chế preflight hiện hành; xác minh widget/requirement trước khi đổi.

Khởi động Studio từ môi trường đã đặt biến dưới đây nếu cần; không bắt buộc đường dẫn máy tác giả:

```powershell
$env:ASM7_ALLOW_PIM_FIXTURE_CREATE = 'false'
$env:ASM8_RUN_KIND = 'FIRST_RUN' # hoặc RETEST cho lượt có chủ đích
# Tùy chọn: dùng một thư mục output chuyên biệt trên máy của bạn
# $env:ASM7_EVIDENCE_ROOT = '<absolute-dedicated-output-directory>'
```

Không đặt ASM7_EVIDENCE_ROOT thì project độc lập ghi `evidence/executions/23130299`; khi đặt đúng `automation/23130299/KatalonStudio` trong repository, ghi namespace 23130299 của repo. Relative `asm7OutputDir` resolve theo project; mỗi execution/case có UUID riêng, không ghi đè ảnh. Không trỏ vào namespace thành viên khác. Runtime temp/download/Chrome profile ở `runtime/ASM7/executions/<UUID>` là dữ liệu chạy, không nộp vào Git.

## Chạy có kiểm soát

Task chuẩn bị này không chạy live. Operator chỉ chạy sau khi nhóm cho phép demo writes và kiểm tra prerequisites. Đăng nhập **thủ công** trong browser do execution sở hữu khi SessionGuard yêu cầu; không tự bỏ CAPTCHA hoặc chuyển sang profile Chrome cá nhân. URL deployment chính thức là OrangeHRM Demo: https://opensource-demo.orangehrmlive.com/ (đối chiếu ROOT trong RecruitmentUi khi deployment thay đổi).

Standalone: Test Cases → RECRUIT_VAC/RECRUIT_CAND/RECRUIT_FLOW → chọn đúng case → Run Chrome, Profile default. Listener tạo execution riêng và preflight trước fixture. Suite: Test Suites → `RECRUIT_<VAC|CAND|FLOW>_25_ASC_001_TO_025` → Run Chrome; giữ nguyên thứ tự 25 case, chạy tuần tự trong mỗi suite. Không có suite chung toàn nhóm trong repo hiện tại; không tự gọi ba suite này là full integration. Listener hiện áp dụng mọi TC nên trước khi nhập vào project Katalon có module khác, cần scope listener và review GlobalVariable/helper; chưa được phép gắn trực tiếp vào suite đa thành viên.

FIRST_RUN và RETEST: đặt ASM8_RUN_KIND **trước khi mở Studio**; giữ output của từng execution riêng. Khi thiếu metadata, giữ `UNCLASSIFIED`, không suy ra loại lượt từ timestamp. Không replay Save/Update/Submit/Shortlist/Interview khi outcome uncertain. Candidate81 đang uncertain: chỉ điều tra read-only có kiểm soát, không Save lại. Không chạy lại toàn bộ 75 case để tăng PASS.

## Report và evidence

Điểm vào repository đã kiểm tra link: [Recruitment report index](../../../Test%20Report%20-%20Group%2001/Individual/23130299/index.html), [75 case](../../../Test%20Report%20-%20Group%2001/Individual/23130299/RESULTS75.md), [failure analysis](../../../Test%20Report%20-%20Group%2001/Individual/23130299/recruitment-failure-analysis.md). Các đường dẫn `Reports/` và `Documentation/` bên dưới mô tả layout project lúc chạy; bản nộp nằm trong index repository này.

Official HTML/JUnit/console/execution logs ở `Reports/<launch>/<suite>/<execution>/`. Supplementary `results.json`, `results.html`, `results.xml`, `execution.log` ở `evidence/executions/23130299/RECRUIT_<module>_<UUID-prefix>/reports/UNCLASSIFIED/`. Đây là callback-derived bổ sung, không thay official Katalon report. `Documentation/index.html`, `RESULTS75.csv`, `RESULTS75.json`, `ERROR_STOPPED_ANALYSIS.md` map case → execution UUID → ảnh/log/report. Tên ảnh ERROR/STOPPED không quyết định final status. Historical official files giữ nguyên SHA, có thể chứa đường dẫn máy tác giả trong metadata; dùng index tương đối mới để truy cập.

## Kiểm tra offline và giới hạn

Kiểm chứng bổ sung 02/11: **OFFLINE PORTABILITY VERIFIED** — harness gốc 10/10 trong temporary directory mới; layout repo với khoảng trắng, UUID/case manifest và preflight-only PASS. Antlr2/Parrot production 104 units PASS. Log mới tại [team-feedback/offline](../../../maintenance/asm8-23130299/team-feedback/offline/). Log portability cũ dừng sau 9 nhóm được bảo toàn, không sửa lại để thành PASS. Không có browser execution hoặc cross-machine browser PASS.

Python 3 chỉ cần cho tooling; `python maintenance/run_isolated.py --preflight-only --suites VAC CAND FLOW` đọc cấu hình, không khởi động browser. Không thêm `--execute` khi chỉ chuẩn bị. Kết quả compiler/regression của release ở `Documentation/validation/`; compiler PASS không phải business/integration PASS. Full regression PARTIAL, legacy FAIL/UNAVAILABLE được giữ nguyên. Archive source lịch sử `ASM7_CURRENT_STATUS.md` và `ASM8_RECRUITMENT_PPTX_SOURCE.md` là tài liệu tham khảo cũ; ưu tiên README và Documentation của release này, không dùng link lịch sử thiếu đích làm bằng chứng mới.

Blocker: VAC_Filter_02 requirement discrepancy; CAND_UploadCV_04 thiếu exact byte limit; FLOW_Status_01 thiếu persistence readback Candidate81, underlying cause UNKNOWN; FLOW_SelectInterviewer_02 đã sửa locator Add Another nhưng chưa có official browser retest. Bốn case giữ ERROR/SKIPPED. Xem `Documentation/SMOKE_PLAN.md` trước retest. Package integrity READY chỉ xác nhận source/report/evidence có thật và ZIP hợp lệ, không chứng nhận mọi case PASSED hay máy khác đã chạy được.
