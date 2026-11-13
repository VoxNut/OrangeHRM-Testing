# BÁO CÁO KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (TEST EXECUTION REPORT – GIAI ĐOẠN 4)

**Sinh viên thực hiện:** Cao Tiến Thành – **MSSV:** `22130254` – **Nhóm:** `Group 01`  
**Phân hệ phụ trách:** Leave Management (`LEAVE_CONFIG`, `LEAVE_SELF`, `LEAVE_ADMIN`)

---

## 1. Project Information (Thông tin dự án)
- **Tên dự án:** OrangeHRM Automation Testing Project – Group 01 (`Assignment 8: Integration & Test Execution`)
- **Môn học:** Kiểm thử phần mềm (Software Testing)
- **Quản lý mã nguồn:** Git / GitHub (nhánh `origin/thanh`, commit `c1c08323e76458be5aef08cc36561968df9617f9`)
- **Runner tích hợp nhóm:** [Automation Testing Project - Group 01/run.mjs](../../../Automation%20Testing%20Project%20-%20Group%2001/run.mjs) & [summarize.mjs](../../../Automation%20Testing%20Project%20-%20Group%2001/summarize.mjs)
- **Script cá nhân tích hợp:** [automation/22130254/cypress/e2e/leave/22130254_CaoTienThanh_ASM7.cy.js](../../../automation/22130254/cypress/e2e/leave/22130254_CaoTienThanh_ASM7.cy.js)

## 2. Testing Environment (Môi trường kiểm thử)
- **Website kiểm thử (SUT):** OrangeHRM OS 5.9 Demo (`https://opensource-demo.orangehrmlive.com/`)
- **Loại kiểm thử:** UI End-to-End Automation Testing trên môi trường online thực tế
- **Hệ điều hành:** Windows 10/11 64-bit (`win32 x64`)
- **Node.js Runtime:** `v24.18.0`
- **Trình duyệt thực thi:** Microsoft Edge `157.0.4341.46` (Headless mode, Viewport `1440 × 1000`)
- **Công cụ kiểm thử:** Cypress `15.21.1` (JavaScript), cấu hình `testIsolation: false`, `retries: 0`, `defaultCommandTimeout: 20000ms`

## 3. Test Scope (Phạm vi kiểm thử)
- **Phạm vi cá nhân (`22130254` – Phân hệ Leave):** Tổng cộng **75 Test Cases** chia đều cho 3 chức năng:
  1. **`LEAVE_CONFIG` (25 Test Cases: `TC_LEAVE_CONFIG_001` → `025`):** Quản lý Leave Types (Thêm mới, sửa, xóa, hủy xóa, kiểm tra biên 50–51 ký tự, khoảng trắng, trùng tên, cờ `Situational`) và Leave Entitlements (Cấp phép cá nhân/hàng loạt theo Sub Unit, kiểm tra số âm/0/chữ/thập phân, lọc `Employee Entitlements`).
  2. **`LEAVE_SELF` (25 Test Cases: `TC_LEAVE_SELF_001` → `025`):** Nhân viên tự nộp đơn nghỉ phép (`Apply`) và tra cứu/xử lý trên `My Leave` (`Full Day`, `Half Day - Morning/Afternoon`, `Specify Time`, nhiều ngày liên tiếp, kiểm tra trừ số dư, chặn đơn trùng/vượt số dư/chưa có hạn mức/cuối tuần/ngày lễ, hủy đơn `Pending Approval` và `Scheduled`).
  3. **`LEAVE_ADMIN` (25 Test Cases: `TC_LEAVE_ADMIN_001` → `025`):** Quản trị viên tra cứu và xử lý đơn trên `Leave List` (Lọc theo khoảng ngày, trạng thái, loại phép, nhân viên, phòng ban, bật `Include Past Employees` cho nhân viên đã nghỉ việc; `Approve`, `Reject`, `Cancel`, `Add Comment`, thao tác hàng loạt `Bulk Approve/Reject/Cancel` và kiểm chứng phân trang với 51 đơn nghỉ phép).
- **Phạm vi tích hợp toàn nhóm (`Group 01`):** Tổng inventory **256 Test Cases** (`75 PIM` + `75 Leave` + `25 Admin` + `6 Time` + `75 Recruitment`).

## 4. Test Execution Summary (Tổng hợp kết quả thực thi)

Công thức tính tỉ lệ đạt chuẩn:

$$\text{Pass Rate} = \frac{\text{Passed}}{\text{Executed}} \times 100\% = \frac{\text{Passed}}{\text{Passed} + \text{Failed}} \times 100\%$$

*(Không tính `Skipped` vào mẫu số của `Pass Rate`)*

### 4.1. Bảng tổng hợp các lượt chạy của phân hệ Leave (`22130254`)

| Đợt chạy (Run ID) | Phạm vi | Total | Executed | Passed | Failed | Skipped | Thời gian (s) | Cleanup | Pass Rate | Báo cáo HTML |
|---|---|---:|---:|---:|---:|---:|---:|:---:|---:|---|
| `ASM8_2026-11-13T08-57-47-357Z` | Lượt 1 – Full 75 Cases | 75 | 75 | 72 | 3 | 0 | `3,710.67s` | `true` | **96.00%** | [index.html](../../../Test%20Report%20-%20Group%2001/ASM8_2026-11-13T08-57-47-357Z/index.html) |
| `ASM8_RETEST_2026-11-13T10-54-12-259Z` | Lượt 2 – Retest 3 Cases lỗi | 3 | 3 | 3 | 0 | 0 | `75.54s` | `true` | **100.00%** | [index.html](../../../Test%20Report%20-%20Group%2001/ASM8_RETEST_2026-11-13T10-54-12-259Z/index.html) |
| **Tổng hợp Latest theo 75 Case ID** | **Toàn bộ 3 chức năng Leave** | **75** | **75** | **75** | **0** | **0** | **`3,786.22s`** | **`true`** | **100.00%** | [Summary HTML](../../../Test%20Report%20-%20Group%2001/ASM8_SUMMARY_2026-11-13T10-54-12-259Z/index.html) |

### 4.2. Bảng kết quả theo từng chức năng Leave (`22130254`)

| Chức năng (Module) | Total Cases | Lượt 1 (Pass / Fail / Skipped) | Pass Rate Lượt 1 | Sau Retest (Pass / Fail / Skipped) | Pass Rate Sau Retest |
|---|---:|---|---:|---|---:|
| `LEAVE_CONFIG` | 25 | 22 Pass / 3 Fail / 0 Skipped | 88.00% | **25 Pass / 0 Fail / 0 Skipped** | **100.00%** |
| `LEAVE_SELF` | 25 | 25 Pass / 0 Fail / 0 Skipped | 100.00% | **25 Pass / 0 Fail / 0 Skipped** | **100.00%** |
| `LEAVE_ADMIN` | 25 | 25 Pass / 0 Fail / 0 Skipped | 100.00% | **25 Pass / 0 Fail / 0 Skipped** | **100.00%** |
| **Tổng cộng (`Leave`)** | **75** | **72 Pass / 3 Fail / 0 Skipped** | **96.00%** | **75 Pass / 0 Fail / 0 Skipped** | **100.00%** |

### 4.3. Bảng tổng hợp toàn nhóm (`ASM8_SUMMARY_2026-11-13T10-54-12-259Z`)

| MSSV | Thành viên | Suite | Công cụ | Planned | Executed | Pass | Fail | Skipped | Pass Rate |
|---|---|---|---|---:|---:|---:|---:|---:|---:|
| `23130226` | Võ Minh Nhựt | `pim-emp` | Playwright | 25 | 25 | 25 | 0 | 0 | 100.00% |
| `23130226` | Võ Minh Nhựt | `pim-details` | Playwright | 50 | 50 | 50 | 0 | 0 | 100.00% |
| `22130254` | **Cao Tiến Thành** | **`leave`** | **Cypress** | **75** | **75** | **75** | **0** | **0** | **100.00%** |
| `23130171` | Nguyễn Thị Thủy Linh | `admin` | Playwright | 25 | 0 | 0 | 0 | 25 | N/A |
| `23130339` | Tôn Bích Trâm | `time` | Puppeteer | 6 | 0 | 0 | 0 | 6 | N/A |
| `23130299` | Nguyễn Văn Thắng | `recruit-vac/cand/flow` | Katalon Studio | 75 | 0 | 0 | 0 | 75 | N/A |
| **Tổng nhóm** | **5 thành viên** | **8 Suites** | **Đa công cụ** | **256** | **150** | **150** | **0** | **106** | **100.00%** |

## 5. Total Test Cases (Tổng số Test Case)
- **Phân hệ Leave (`22130254`):** `75` Test Cases thiết kế = `75` Test Cases tích hợp & thực thi (`78` lần chạy nếu tính cả 3 case chạy lại ở lượt Retest).
- **Toàn dự án nhóm (`Group 01`):** `256` Test Cases trong inventory (`150` Test Cases thực thi tự động, `165` lần chạy tính cả các lượt retest PIM & Leave).

## 6. Passed Test Cases (Số lượng Test Case Pass)
- **Phân hệ Leave (`22130254`):**
  - Lượt 1 (`ASM8_2026-11-13T08-57-47-357Z`): **72 / 75 Pass** (`96.00%`).
  - Sau Retest (`ASM8_RETEST_2026-11-13T10-54-12-259Z`): **75 / 75 Pass** (`100.00%`).
- **Toàn dự án nhóm (`Group 01`):** **150 / 150 Executed Cases Pass** (`100.00%`).

## 7. Failed Test Cases (Số lượng Test Case Fail)
- **Phân hệ Leave (`22130254`):**
  - Lượt 1: **3 Fail** (`TC_LEAVE_CONFIG_002`, `TC_LEAVE_CONFIG_005`, `TC_LEAVE_CONFIG_011`).
  - Sau Retest: **0 Fail**.
- **Toàn dự án nhóm (`Group 01`):** **0 Fail** (sau khi hoàn tất retest 2 case PIM và 3 case Leave).

## 8. Skipped Test Cases (Số lượng Test Case Skipped)
- **Phân hệ Leave (`22130254`):** **0 Skipped** (thực thi đủ 100% cả 75 case).
- **Toàn dự án nhóm (`Group 01`):** **106 Skipped** (25 case `Admin` chưa giới hạn selector/cleanup, 6 case `Time` dùng dữ liệu chung và chưa tách kết quả theo ID, 75 case `Recruitment` bỏ qua theo quyết định của nhóm trưởng do thiếu `Profiles/default.glbl` và Katalon Runtime Engine).

## 9. Execution Duration (Thời gian thực thi)
- **Lượt 1 – Full 75 Cases Leave (`ASM8_2026-11-13T08-57-47-357Z`):** `3,710,674 ms` (**`3,710.67 giây`** ≈ `61.84 phút`, bao gồm toàn bộ bước khởi tạo 4 nhân viên ESS, ca làm việc 8h, lịch làm việc, thực thi 75 test cases, tạo 51 đơn kiểm thử phân trang và dọn dẹp dữ liệu cuối suite).
- **Lượt 2 – Retest 3 Cases Leave (`ASM8_RETEST_2026-11-13T10-54-12-259Z`):** `75,542 ms` (**`75.54 giây`** ≈ `1.26 phút`).
- **Tổng thời gian thực thi phân hệ Leave:** **`3,786.22 giây`** (`63.10 phút`).
- **Tổng thời gian thực thi toàn bộ các lượt chạy của nhóm (6 lượt PIM + Leave):** **`5,325.68 giây`** (`88.76 phút`).

## 10. Failed Test Case Details (Chi tiết các Test Case thất bại ở Lượt 1)

| Test Case ID | Chức năng | Expected Result | Actual Result (Lượt 1 – `ASM8_2026-11-13T08-57-47-357Z`) | Kết quả Retest (`ASM8_RETEST_2026-11-13T10-54-12-259Z`) |
|---|---|---|---|---|
| `TC_LEAVE_CONFIG_002` | `LEAVE_CONFIG` | Lưu thành công. Khi mở lại bản ghi, radio `Is Entitlement Situational` được chọn là `Yes`. | `CypressError: Timed out retrying after 20000ms: cy.wait() timed out waiting 20000ms for the 1st request to the route: leaveNavigation...Reset. No request ever occurred.` | **PASS** (`24.54s`) – [Ảnh PASS](screenshots/TC_LEAVE_CONFIG_002.png) |
| `TC_LEAVE_CONFIG_005` | `LEAVE_CONFIG` | Lưu thành công. Khi mở Edit, `Name` có đúng 50 ký tự đã nhập; không mất dữ liệu. | `CypressError: Timed out retrying after 20000ms: cy.wait() timed out waiting 20000ms for the 1st request to the route: leaveNavigation...Reset. No request ever occurred.` | **PASS** (`9.30s`) – [Ảnh PASS](screenshots/TC_LEAVE_CONFIG_005.png) |
| `TC_LEAVE_CONFIG_011` | `LEAVE_CONFIG` | Lưu thành công. Radio `Situational` giữ giá trị `Yes` sau khi reload trang. | `CypressError: Timed out retrying after 20000ms: cy.wait() timed out waiting 20000ms for the 1st request to the route: leaveNavigation...Reset. No request ever occurred.` | **PASS** (`30.78s`) – [Ảnh PASS](screenshots/TC_LEAVE_CONFIG_011.png) |

## 11. Root Cause Analysis (Phân tích nguyên nhân gốc rễ)
- **Hiện tượng:** Cả 3 Test Case (`TC_LEAVE_CONFIG_002`, `005`, `011`) đều lưu thành công dữ liệu lên server (đã hiện thông báo `.oxd-toast` `Successfully Saved` / `Successfully Updated`), nhưng bị dừng ở lệnh `ctx.openTypeEdit()` khi điều hướng mở lại danh sách `Leave Types` để kiểm tra độ bền dữ liệu.
- **Nguyên nhân gốc rễ:**
  Trên OrangeHRM Demo, sau khi bấm `Save` tại trang `/leave/defineLeaveType`, giao diện hiển thị ngay thông báo `.oxd-toast` nhưng **đợi khoảng 1.2 giây mới kích hoạt sự kiện tự động chuyển hướng (auto-redirect) về `/leave/leaveTypeList`**.
  Trong script ban đầu, ngay khi thấy `.oxd-toast`, hàm `ctx.openTypeEdit()` gọi ngay `cy.leavePage('Configure', 'Leave Types')`. Do tại thời điểm đó URL vẫn là `/defineLeaveType`, `cy.leavePage` thực hiện bấm vào tab `Leave List` và chờ request `@leaveNavigation...Reset`. Đúng lúc đó, sự kiện auto-redirect 1.2 giây của trang `/defineLeaveType` kích hoạt và chuyển trình duyệt về `/leaveTypeList`, làm trình duyệt **hủy (abort)** request tới `Leave List` khiến `cy.wait()` bị timeout sau 20 giây.
- **Giải pháp khắc phục đã áp dụng:**
  1. Bổ sung `cy.location('pathname').should('include', '/leaveTypeList'); cy.ready();` ngay sau `ctx.success('Successfully Saved' | 'Successfully Updated')` khi thêm/sửa Leave Type để đợi trang hoàn tất auto-redirect tự nhiên.
  2. Cập nhật `ctx.openTypes()` trong [22130254_CaoTienThanh_ASM7.cy.js](../../../automation/22130254/cypress/e2e/leave/22130254_CaoTienThanh_ASM7.cy.js): nếu trình duyệt đã ở `/leaveTypeList` thì gọi `cy.ready()` và thao tác trực tiếp trên bảng, không điều hướng vòng qua `Leave List`.

## 12. Screenshots and Logs (Minh chứng ảnh chụp và nhật ký thực thi)
- **75 ảnh chụp màn hình PASS (`TC_LEAVE_CONFIG_001`..`025`, `TC_LEAVE_SELF_001`..`025`, `TC_LEAVE_ADMIN_001`..`025`):** Lưu tại [evidence/executions/22130254/screenshots/](screenshots/)
- **Minh chứng gốc Lượt 1 (`ASM8_2026-11-13T08-57-47-357Z`):**
  - Cypress Raw JSON: [cypress-results.json](../../../Envidence%20-%20Group01/ASM8_2026-11-13T08-57-47-357Z/leave/cypress-results.json)
  - Sổ cái Fixture & Cleanup (`cleanupCompleted: true`): [run-data.json](../../../Envidence%20-%20Group01/ASM8_2026-11-13T08-57-47-357Z/leave/run-data.json)
  - Thư mục ảnh chụp Lượt 1 (72 ảnh PASS + 3 ảnh FAIL): [screenshots/](../../../Envidence%20-%20Group01/ASM8_2026-11-13T08-57-47-357Z/leave/screenshots/22130254_CaoTienThanh_ASM7.cy.js/)
- **Minh chứng gốc Lượt 2 Retest (`ASM8_RETEST_2026-11-13T10-54-12-259Z`):**
  - Cypress Raw JSON: [cypress-results.json](../../../Envidence%20-%20Group01/ASM8_RETEST_2026-11-13T10-54-12-259Z/leave/cypress-results.json)
  - Sổ cái Fixture & Cleanup (`cleanupCompleted: true`): [run-data.json](../../../Envidence%20-%20Group01/ASM8_RETEST_2026-11-13T10-54-12-259Z/leave/run-data.json)
  - Thư mục ảnh chụp Retest (3 ảnh PASS): [screenshots/](../../../Envidence%20-%20Group01/ASM8_RETEST_2026-11-13T10-54-12-259Z/leave/screenshots/22130254_CaoTienThanh_ASM7.cy.js/)

## 13. Defect Analysis (Đánh giá lỗi sản phẩm vs lỗi Script / Flaky)
- **Không phát hiện lỗi nghiệp vụ (Application Defect) mới trên OrangeHRM Demo:** Cả 75/75 Test Cases đều xác nhận đúng quy tắc nghiệp vụ của OrangeHRM 5.9 (tạo/sửa/xóa Leave Type, cấp phép, tính ngày nghỉ làm việc trừ cuối tuần/ngày lễ, xét duyệt đơn, truy vấn nhân viên đã nghỉ việc và phân trang > 50 bản ghi).
- **Phân loại 3 lỗi ở Lượt 1:** Thuộc nhóm **Script Synchronization Issue** (chưa đồng bộ với hành vi auto-redirect trễ ~1.2s của form `defineLeaveType`). Sau khi đồng bộ hóa điểm chờ URL, cả 3 case đều đạt **PASS 100%**, chứng minh bộ test hoàn toàn ổn định, không bị Flaky.

## 14. Recommendations (Đề xuất cải tiến)
1. **Duy trì cơ chế đồng bộ URL + Spinner (`cy.ready()`):** Với các biểu mẫu trong OrangeHRM có hành vi tự động chuyển trang sau khi hiện toast (`Add Leave Type`, `Add Entitlements`, `Add Employee`), luôn đợi `cy.location('pathname')` chuyển sang trang danh sách trước khi thực hiện thao tác menu tiếp theo.
2. **Sử dụng chế độ chạy chọn lọc (`npm run retest:leave`) khi gỡ lỗi:** Giúp rút ngắn thời gian xác nhận sửa lỗi từ ~60 phút xuống ~1 phút mà vẫn bảo toàn cấu trúc kiểm tra inventory 256 IDs của nhóm.
3. **Mở rộng quy chuẩn Fixture Ledger sang `Admin` và `Time`:** Áp dụng cách gắn mã `runId` riêng và ghi `run-data.json` của `Leave`/`PIM` cho các phân hệ còn lại để toàn bộ nhóm có thể thực thi tự động 256/256 IDs.

---

## PHỤ LỤC: CHI TIẾT KẾT QUẢ THỰC THI 75 TEST CASES PHÂN HỆ LEAVE (`22130254`)

| STT | Test Case ID | Chức năng | Tên Test Case | Expected Result | Actual Result | Lượt 1 | Latest | Thời gian | Screenshot |
|---:|---|---|---|---|---|:---:|:---:|---:|---|
| 1 | `TC_LEAVE_ADMIN_001` | `LEAVE_ADMIN` | Tìm kiếm Leave List theo Employee Name hợp lệ qua autocomplete | Có đúng hai đơn fixture đang Pending Approval. Mọi dòng trả về thuộc F_EMP; không có đơn của nhân viên khác. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 85.13s | [TC_LEAVE_ADMIN_001.png](screenshots/TC_LEAVE_ADMIN_001.png) |
| 2 | `TC_LEAVE_ADMIN_002` | `LEAVE_ADMIN` | Tìm kiếm Leave List với Employee Name không tồn tại | Autocomplete hiển thị No Records Found và không chọn được nhân viên. Nếu Search khi còn chuỗi chưa chọn, Employee Name phải báo Invalid; bảng cũ không được coi là kết quả tìm kiếm thành công theo tên không tồn tại. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 3.39s | [TC_LEAVE_ADMIN_002.png](screenshots/TC_LEAVE_ADMIN_002.png) |
| 3 | `TC_LEAVE_ADMIN_003` | `LEAVE_ADMIN` | Lọc Leave List kết hợp Leave Type và trạng thái Pending Approval | Hai đơn trả về đều đúng F_TYPE và Pending Approval, đồng thời giữ đúng nhân viên/khoảng ngày đã lọc. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.12s | [TC_LEAVE_ADMIN_003.png](screenshots/TC_LEAVE_ADMIN_003.png) |
| 4 | `TC_LEAVE_ADMIN_004` | `LEAVE_ADMIN` | Lọc Leave List theo Sub Unit của tổ chức | Danh sách chỉ chứa các đơn của những nhân viên thuộc đúng Sub Unit đã chọn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 3.86s | [TC_LEAVE_ADMIN_004.png](screenshots/TC_LEAVE_ADMIN_004.png) |
| 5 | `TC_LEAVE_ADMIN_005` | `LEAVE_ADMIN` | Lọc Leave List theo khoảng thời gian From Date - To Date hợp lệ | Tất cả các đơn hiển thị đều có ngày nghỉ rơi vào khoảng từ 01/11/2026 đến 30/11/2026. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.61s | [TC_LEAVE_ADMIN_005.png](screenshots/TC_LEAVE_ADMIN_005.png) |
| 6 | `TC_LEAVE_ADMIN_006` | `LEAVE_ADMIN` | Lọc Leave List với From Date lớn hơn To Date | Trường To Date hiển thị lỗi "To date should be after from date". Hệ thống không thực hiện truy vấn với khoảng ngày sai. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 2.07s | [TC_LEAVE_ADMIN_006.png](screenshots/TC_LEAVE_ADMIN_006.png) |
| 7 | `TC_LEAVE_ADMIN_007` | `LEAVE_ADMIN` | Lọc đồng thời nhiều trạng thái trong Show Leave with Status | Bảng kết quả hiển thị đồng thời các đơn có trạng thái Pending Approval và Scheduled, không hiển thị Rejected hay Cancelled. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.98s | [TC_LEAVE_ADMIN_007.png](screenshots/TC_LEAVE_ADMIN_007.png) |
| 8 | `TC_LEAVE_ADMIN_008` | `LEAVE_ADMIN` | Bật toggle Include Past Employees khi tìm kiếm | Danh sách kết quả bao gồm cả các đơn nghỉ phép lịch sử của nhân viên đã thôi việc (Past Employees). | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 139.11s | [TC_LEAVE_ADMIN_008.png](screenshots/TC_LEAVE_ADMIN_008.png) |
| 9 | `TC_LEAVE_ADMIN_009` | `LEAVE_ADMIN` | Bấm nút Reset trên form lọc Leave List | Reset phục hồi From/To về đầu/cuối kỳ phép mặc định; Employee Name và Sub Unit về chưa chọn; Status trở về Pending Approval; bảng tải lại. Không yêu cầu ngày rỗng. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.98s | [TC_LEAVE_ADMIN_009.png](screenshots/TC_LEAVE_ADMIN_009.png) |
| 10 | `TC_LEAVE_ADMIN_010` | `LEAVE_ADMIN` | Phê duyệt (Approve) một đơn nghỉ phép đang ở trạng thái Pending Approval | Thông báo "Successfully Updated". Cột Status của đơn chuyển ngay từ "Pending Approval" sang "Scheduled". Nút Approve/Reject biến mất. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 12.03s | [TC_LEAVE_ADMIN_010.png](screenshots/TC_LEAVE_ADMIN_010.png) |
| 11 | `TC_LEAVE_ADMIN_011` | `LEAVE_ADMIN` | Xác nhận trạng thái Scheduled sau khi Approve hiển thị đồng bộ bên Employee | My Leave hiển thị Scheduled trên đúng đơn/loại phép; ngày và Number of Days khớp bản ghi Admin đã duyệt (ASM7: D1, 1.00 ngày). | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 17.45s | [TC_LEAVE_ADMIN_011.png](screenshots/TC_LEAVE_ADMIN_011.png) |
| 12 | `TC_LEAVE_ADMIN_012` | `LEAVE_ADMIN` | Từ chối (Reject) một đơn nghỉ phép đang ở trạng thái Pending Approval | Thông báo "Successfully Updated". Trạng thái của đơn chuyển ngay thành "Rejected". | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 25.99s | [TC_LEAVE_ADMIN_012.png](screenshots/TC_LEAVE_ADMIN_012.png) |
| 13 | `TC_LEAVE_ADMIN_013` | `LEAVE_ADMIN` | Kiểm tra hoàn trả hạn mức phép sau khi Admin Reject đơn | B_after = B_before + 2.00; chỉ hoàn số ngày của đơn đã Reject, không thay đổi entitlement gốc. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 65.88s | [TC_LEAVE_ADMIN_013.png](screenshots/TC_LEAVE_ADMIN_013.png) |
| 14 | `TC_LEAVE_ADMIN_014` | `LEAVE_ADMIN` | Admin thực hiện Hủy (Cancel) một đơn nghỉ phép đang Scheduled | Successfully Updated. Đơn chuyển Cancelled; số dư hoàn lại số ngày của đơn, không thay đổi đơn khác. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 90.34s | [TC_LEAVE_ADMIN_014.png](screenshots/TC_LEAVE_ADMIN_014.png) |
| 15 | `TC_LEAVE_ADMIN_015` | `LEAVE_ADMIN` | Kiểm tra không thể thực hiện Approve lại đơn đã ở trạng thái Rejected | Không có nút Approve trên dòng đơn đã Rejected. Không thể thay đổi trạng thái từ Rejected sang Approved/Scheduled. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 17.52s | [TC_LEAVE_ADMIN_015.png](screenshots/TC_LEAVE_ADMIN_015.png) |
| 16 | `TC_LEAVE_ADMIN_016` | `LEAVE_ADMIN` | Kiểm tra không thể thực hiện Approve lại đơn đã ở trạng thái Cancelled | Không xuất hiện nút Approve đối với đơn đã Cancelled. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 6.18s | [TC_LEAVE_ADMIN_016.png](screenshots/TC_LEAVE_ADMIN_016.png) |
| 17 | `TC_LEAVE_ADMIN_017` | `LEAVE_ADMIN` | Xem bình luận (View Comments) của nhân viên trên đơn nghỉ phép | Modal Leave Request Comments mở. Nội dung đã lưu khớp ghi chú của nhân viên; chỉ đối chiếu các thông tin tác giả/thời gian có hiển thị trên UI. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 34.78s | [TC_LEAVE_ADMIN_017.png](screenshots/TC_LEAVE_ADMIN_017.png) |
| 18 | `TC_LEAVE_ADMIN_018` | `LEAVE_ADMIN` | Admin thêm bình luận ghi chú mới vào đơn nghỉ phép | Lưu thành công; khi mở lại có đúng nội dung comment Admin vừa thêm, không ghi vào đơn khác. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 14.90s | [TC_LEAVE_ADMIN_018.png](screenshots/TC_LEAVE_ADMIN_018.png) |
| 19 | `TC_LEAVE_ADMIN_019` | `LEAVE_ADMIN` | Đóng modal bình luận bằng nút Cancel/Close mà không lưu | Modal đóng lại. Đoạn text chưa lưu bị hủy bỏ, danh sách comment cũ không bị thay đổi. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 8.84s | [TC_LEAVE_ADMIN_019.png](screenshots/TC_LEAVE_ADMIN_019.png) |
| 20 | `TC_LEAVE_ADMIN_020` | `LEAVE_ADMIN` | Chọn checkbox header để chọn tất cả các dòng trên trang Leave List | Tất cả dòng fixture trên trang hiện tại được chọn; không chọn ngầm trang khác hoặc bản ghi ngoài fixture. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 115.59s | [TC_LEAVE_ADMIN_020.png](screenshots/TC_LEAVE_ADMIN_020.png) |
| 21 | `TC_LEAVE_ADMIN_021` | `LEAVE_ADMIN` | Thực hiện phê duyệt hàng loạt (Bulk Approve) nhiều đơn Pending Approval | Cập nhật thành công. Cả hai đơn đã chọn chuyển Scheduled; đơn không được chọn không bị thay đổi. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 14.87s | [TC_LEAVE_ADMIN_021.png](screenshots/TC_LEAVE_ADMIN_021.png) |
| 22 | `TC_LEAVE_ADMIN_022` | `LEAVE_ADMIN` | Thực hiện từ chối hàng loạt (Bulk Reject) nhiều đơn Pending Approval | Cập nhật thành công. Cả hai đơn đã chọn chuyển Rejected; đơn không được chọn không bị thay đổi. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 16.19s | [TC_LEAVE_ADMIN_022.png](screenshots/TC_LEAVE_ADMIN_022.png) |
| 23 | `TC_LEAVE_ADMIN_023` | `LEAVE_ADMIN` | Kiểm tra phân trang (Pagination) trên bảng danh sách Leave List | Chuyển trang đúng, số trang được đánh dấu; bản ghi trang 2 đúng fixture tiếp theo và không trùng khóa trang 1. Nếu chưa đủ dữ liệu phân trang thì tiền điều kiện chưa đạt, không ghi Passed. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 530.74s | [TC_LEAVE_ADMIN_023.png](screenshots/TC_LEAVE_ADMIN_023.png) |
| 24 | `TC_LEAVE_ADMIN_024` | `LEAVE_ADMIN` | Xem chi tiết đơn bằng View Leave Details | Mở trang Leave Request Details của đúng đơn. Thông tin nhân viên/đơn và trạng thái ngày khớp dữ liệu ban đầu; Back quay về danh sách. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 9.54s | [TC_LEAVE_ADMIN_024.png](screenshots/TC_LEAVE_ADMIN_024.png) |
| 25 | `TC_LEAVE_ADMIN_025` | `LEAVE_ADMIN` | Đối chiếu tính chính xác của Number of Days và Leave Balance trên từng dòng đơn | Number of Days khớp lịch làm việc và thời lượng đơn (trừ ngày không làm việc/điều chỉnh nửa ngày). Leave Balance khớp entitlement trừ các ngày đã phân bổ/đã nghỉ trong cùng kỳ tại lúc kiểm tra; không giả định chỉ trừ duy nhất đơn đang xem. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 222.12s | [TC_LEAVE_ADMIN_025.png](screenshots/TC_LEAVE_ADMIN_025.png) |
| 26 | `TC_LEAVE_CONFIG_001` | `LEAVE_CONFIG` | Thêm mới Leave Type với các trường bắt buộc | Radio Is Entitlement Situational = No được chọn trước Save. Sau Save có Successfully Saved và đúng tên loại phép trong danh sách; danh sách chỉ có Name và Actions. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 25.41s | [TC_LEAVE_CONFIG_001.png](screenshots/TC_LEAVE_CONFIG_001.png) |
| 27 | `TC_LEAVE_CONFIG_002` | `LEAVE_CONFIG` | Thêm mới Leave Type với tùy chọn Situational = Yes | Lưu thành công. Khi mở lại bản ghi G01T01_Maternity, radio Is Entitlement Situational được chọn là Yes. | Lượt 1: FAIL (Timeout auto-redirect sau Save). Retest: PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Fail | **Pass** | 24.54s (Retest) | [TC_LEAVE_CONFIG_002.png](screenshots/TC_LEAVE_CONFIG_002.png) |
| 28 | `TC_LEAVE_CONFIG_003` | `LEAVE_CONFIG` | Bỏ trống trường Leave Type Name | Trường Name hiển thị thông báo lỗi "Required". Không tạo Leave Type mới và form không đóng. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 311.39s | [TC_LEAVE_CONFIG_003.png](screenshots/TC_LEAVE_CONFIG_003.png) |
| 29 | `TC_LEAVE_CONFIG_004` | `LEAVE_CONFIG` | Nhập Leave Type Name chỉ chứa khoảng trắng | Hệ thống từ chối lưu và hiển thị thông báo lỗi "Required" hoặc không hợp lệ do chuỗi chỉ có khoảng trắng. Không tạo bản ghi. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 70.82s | [TC_LEAVE_CONFIG_004.png](screenshots/TC_LEAVE_CONFIG_004.png) |
| 30 | `TC_LEAVE_CONFIG_005` | `LEAVE_CONFIG` | Leave Type Name đúng biên tối đa 50 ký tự | Lưu thành công. Khi mở Edit, Name có đúng 50 ký tự đã nhập; không mất dữ liệu dù ô danh sách có thể wrap/rút gọn. | Lượt 1: FAIL (Timeout auto-redirect sau Save). Retest: PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Fail | **Pass** | 9.30s (Retest) | [TC_LEAVE_CONFIG_005.png](screenshots/TC_LEAVE_CONFIG_005.png) |
| 31 | `TC_LEAVE_CONFIG_006` | `LEAVE_CONFIG` | Leave Type Name vượt biên 50 ký tự (51 ký tự) | Không lưu tên dài 51 ký tự: input chặn ký tự thứ 51 hoặc validation Should not exceed 50 characters. Nếu input chỉ nhận 50, ghi nhận chiều dài thực nhận; không coi việc lưu 50 ký tự là lưu 51. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 84.13s | [TC_LEAVE_CONFIG_006.png](screenshots/TC_LEAVE_CONFIG_006.png) |
| 32 | `TC_LEAVE_CONFIG_007` | `LEAVE_CONFIG` | Nhập Leave Type Name trùng với tên loại phép đã tồn tại | Hiển thị thông báo lỗi "Already exists" tại trường Name. Hệ thống không cho phép lưu bản ghi trùng tên. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 105.13s | [TC_LEAVE_CONFIG_007.png](screenshots/TC_LEAVE_CONFIG_007.png) |
| 33 | `TC_LEAVE_CONFIG_008` | `LEAVE_CONFIG` | Lọc Employee Entitlements và đối chiếu hạn mức theo nhân viên, loại phép, kỳ | Chỉ trả về entitlement đúng nhân viên, loại phép và kỳ đã chọn. Tổng Days = 20; không cộng dữ liệu F_EMP_B hoặc kỳ khác. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 119.01s | [TC_LEAVE_CONFIG_008.png](screenshots/TC_LEAVE_CONFIG_008.png) |
| 34 | `TC_LEAVE_CONFIG_009` | `LEAVE_CONFIG` | Hủy thao tác thêm mới Leave Type | Form thêm mới đóng lại, quay về danh sách Leave Types. Không xuất hiện loại phép G01T01_CancelType. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 3.35s | [TC_LEAVE_CONFIG_009.png](screenshots/TC_LEAVE_CONFIG_009.png) |
| 35 | `TC_LEAVE_CONFIG_010` | `LEAVE_CONFIG` | Chỉnh sửa tên Leave Type thành tên mới hợp lệ | Thông báo "Successfully Updated". Dòng loại phép hiển thị tên mới G01T01_Annual_Updated, tên cũ không còn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 123.34s | [TC_LEAVE_CONFIG_010.png](screenshots/TC_LEAVE_CONFIG_010.png) |
| 36 | `TC_LEAVE_CONFIG_011` | `LEAVE_CONFIG` | Chỉnh sửa Leave Type đổi Is Entitlement Situational từ No sang Yes | Lưu thành công. Radio Situational giữ giá trị Yes sau khi reload trang. | Lượt 1: FAIL (Timeout auto-redirect sau Save). Retest: PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Fail | **Pass** | 30.78s (Retest) | [TC_LEAVE_CONFIG_011.png](screenshots/TC_LEAVE_CONFIG_011.png) |
| 37 | `TC_LEAVE_CONFIG_012` | `LEAVE_CONFIG` | Chỉnh sửa Leave Type nhưng xóa trống trường Name | Hiển thị lỗi "Required" tại trường Name. Hệ thống không lưu tên rỗng, dữ liệu cũ vẫn giữ nguyên khi reload. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 33.75s | [TC_LEAVE_CONFIG_012.png](screenshots/TC_LEAVE_CONFIG_012.png) |
| 38 | `TC_LEAVE_CONFIG_013` | `LEAVE_CONFIG` | Chỉnh sửa Leave Type nhưng bấm Cancel | Đóng form chỉnh sửa. Tên bản ghi trong danh sách vẫn giữ nguyên giá trị trước khi sửa. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 23.58s | [TC_LEAVE_CONFIG_013.png](screenshots/TC_LEAVE_CONFIG_013.png) |
| 39 | `TC_LEAVE_CONFIG_014` | `LEAVE_CONFIG` | Xóa Leave Type của nhóm và xác nhận trên modal | Hiển thị thông báo "Successfully Deleted". Bản ghi G01T01_DeleteType biến mất hoàn toàn khỏi danh sách. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 33.73s | [TC_LEAVE_CONFIG_014.png](screenshots/TC_LEAVE_CONFIG_014.png) |
| 40 | `TC_LEAVE_CONFIG_015` | `LEAVE_CONFIG` | Hủy thao tác xóa Leave Type trên modal xác nhận | Modal đóng lại. Bản ghi G01T01_Annual_Updated vẫn còn nguyên vẹn trong danh sách, không bị xóa. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 11.03s | [TC_LEAVE_CONFIG_015.png](screenshots/TC_LEAVE_CONFIG_015.png) |
| 41 | `TC_LEAVE_CONFIG_016` | `LEAVE_CONFIG` | Thêm Leave Entitlement hợp lệ với số ngày nguyên cho nhân viên | Successfully Saved. Bảng có đúng F_TYPE, kỳ đã chọn và Days bằng số 20; chấp nhận cách hiển thị 20 hoặc 20.00 vì giá trị số tương đương. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 37.98s | [TC_LEAVE_CONFIG_016.png](screenshots/TC_LEAVE_CONFIG_016.png) |
| 42 | `TC_LEAVE_CONFIG_017` | `LEAVE_CONFIG` | Thêm Leave Entitlement hợp lệ với số ngày thập phân (7.5 ngày) | Lưu thành công. Hạn mức được cộng thêm chính xác 7.50 Ngày vào hồ sơ của nhân viên F_EMP. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 49.78s | [TC_LEAVE_CONFIG_017.png](screenshots/TC_LEAVE_CONFIG_017.png) |
| 43 | `TC_LEAVE_CONFIG_018` | `LEAVE_CONFIG` | Thêm Leave Entitlement nhưng bỏ trống Employee Name | Trường Employee Name hiển thị thông báo lỗi "Required". Không tạo entitlement. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 25.32s | [TC_LEAVE_CONFIG_018.png](screenshots/TC_LEAVE_CONFIG_018.png) |
| 44 | `TC_LEAVE_CONFIG_019` | `LEAVE_CONFIG` | Nhập Employee Name không tồn tại khi thêm Entitlement | Dropdown gợi ý hiển thị "No Records Found". Hệ thống không cho phép chọn tên tự do không có trong danh sách nhân viên. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 29.03s | [TC_LEAVE_CONFIG_019.png](screenshots/TC_LEAVE_CONFIG_019.png) |
| 45 | `TC_LEAVE_CONFIG_020` | `LEAVE_CONFIG` | Thêm Leave Entitlement nhưng không chọn Leave Type | Trường Leave Type hiển thị lỗi "Required". Không thực hiện lưu. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 33.24s | [TC_LEAVE_CONFIG_020.png](screenshots/TC_LEAVE_CONFIG_020.png) |
| 46 | `TC_LEAVE_CONFIG_021` | `LEAVE_CONFIG` | Thêm Leave Entitlement nhưng để trống trường Entitlement | Trường Entitlement hiển thị thông báo lỗi "Required". Không tạo entitlement. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 47.11s | [TC_LEAVE_CONFIG_021.png](screenshots/TC_LEAVE_CONFIG_021.png) |
| 47 | `TC_LEAVE_CONFIG_022` | `LEAVE_CONFIG` | Nhập giá trị Entitlement bằng chữ hoặc ký tự không phải số | Hiển thị lỗi định dạng số "Should be a number" hoặc "Expected a number". Hệ thống từ chối lưu giá trị phi số. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 38.77s | [TC_LEAVE_CONFIG_022.png](screenshots/TC_LEAVE_CONFIG_022.png) |
| 48 | `TC_LEAVE_CONFIG_023` | `LEAVE_CONFIG` | Nhập giá trị Entitlement là số âm | Hiển thị thông báo lỗi không cho phép số âm ("Should be greater than 0" hoặc "Invalid number"). Không lưu hạn mức âm. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 34.06s | [TC_LEAVE_CONFIG_023.png](screenshots/TC_LEAVE_CONFIG_023.png) |
| 49 | `TC_LEAVE_CONFIG_024` | `LEAVE_CONFIG` | Nhập giá trị Entitlement có nhiều hơn 2 chữ số thập phân | Báo lỗi định dạng số thập phân tối đa 2 chữ số ("Should be a valid decimal" hoặc tự động làm tròn/chặn nhập). Không lưu số có 3 chữ số thập phân không hợp lệ. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 24.35s | [TC_LEAVE_CONFIG_024.png](screenshots/TC_LEAVE_CONFIG_024.png) |
| 50 | `TC_LEAVE_CONFIG_025` | `LEAVE_CONFIG` | Hủy thao tác thêm Leave Entitlement khi đang nhập liệu | Cancel quay về trang trước. Tổng entitlement của đúng nhân viên/loại phép/kỳ không tăng so với baseline; không có bản ghi mới do thao tác vừa hủy. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 53.78s | [TC_LEAVE_CONFIG_025.png](screenshots/TC_LEAVE_CONFIG_025.png) |
| 51 | `TC_LEAVE_SELF_001` | `LEAVE_SELF` | Nộp đơn xin nghỉ phép 1 ngày hợp lệ khi đủ hạn mức | Successfully Saved. My Leave có đúng đơn D0 của nhân viên/loại phép fixture, Pending Approval và Number of Days = 1.00. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 42.16s | [TC_LEAVE_SELF_001.png](screenshots/TC_LEAVE_SELF_001.png) |
| 52 | `TC_LEAVE_SELF_002` | `LEAVE_SELF` | Nộp đơn xin nghỉ phép nhiều ngày liên tiếp hợp lệ | Nộp đơn thành công. Đơn hiển thị trong My Leave với khoảng ngày 2026-11-16 to 2026-11-18, Number of Days=3.00, Status="Pending Approval". | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 21.26s | [TC_LEAVE_SELF_002.png](screenshots/TC_LEAVE_SELF_002.png) |
| 53 | `TC_LEAVE_SELF_003` | `LEAVE_SELF` | Nộp đơn nghỉ nửa ngày buổi sáng | Nộp thành công. Đơn Pending Approval, thời lượng nửa ngày buổi sáng, Number of Days = 0.50; số dư giảm 0.50 ngày. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 33.33s | [TC_LEAVE_SELF_003.png](screenshots/TC_LEAVE_SELF_003.png) |
| 54 | `TC_LEAVE_SELF_004` | `LEAVE_SELF` | Không chọn Leave Type khi nộp đơn | Trường Leave Type hiển thị lỗi "Required". Đơn không được gửi đi. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.68s | [TC_LEAVE_SELF_004.png](screenshots/TC_LEAVE_SELF_004.png) |
| 55 | `TC_LEAVE_SELF_005` | `LEAVE_SELF` | Bỏ trống trường From Date khi nộp đơn | Trường From Date hiển thị thông báo lỗi "Required". Không thể nộp đơn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 8.24s | [TC_LEAVE_SELF_005.png](screenshots/TC_LEAVE_SELF_005.png) |
| 56 | `TC_LEAVE_SELF_006` | `LEAVE_SELF` | Bỏ trống trường To Date khi nộp đơn | Trường To Date hiển thị thông báo lỗi "Required". Không thể nộp đơn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 11.41s | [TC_LEAVE_SELF_006.png](screenshots/TC_LEAVE_SELF_006.png) |
| 57 | `TC_LEAVE_SELF_007` | `LEAVE_SELF` | Chọn To Date trước From Date khi nộp đơn | Trường To Date báo lỗi "To date should be after from date". Nút Apply không gửi dữ liệu. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 6.20s | [TC_LEAVE_SELF_007.png](screenshots/TC_LEAVE_SELF_007.png) |
| 58 | `TC_LEAVE_SELF_008` | `LEAVE_SELF` | Nộp đơn theo giờ bằng Duration = Specify Time | Nộp thành công. Đơn Pending Approval; thời lượng 2 giờ tương đương Number of Days = 0.25 với fixture ca 8 giờ đã xác nhận. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 35.85s | [TC_LEAVE_SELF_008.png](screenshots/TC_LEAVE_SELF_008.png) |
| 59 | `TC_LEAVE_SELF_009` | `LEAVE_SELF` | Nhập From Date là ngày không tồn tại trên lịch | Báo lỗi định dạng ngày không hợp lệ ("Should be a valid date" hoặc "Invalid Date"). Đơn không được chấp nhận. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 8.27s | [TC_LEAVE_SELF_009.png](screenshots/TC_LEAVE_SELF_009.png) |
| 60 | `TC_LEAVE_SELF_010` | `LEAVE_SELF` | Nhập To Date là ngày không tồn tại trên lịch | Báo lỗi định dạng ngày không hợp lệ tại ô To Date. Đơn không được chấp nhận. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.53s | [TC_LEAVE_SELF_010.png](screenshots/TC_LEAVE_SELF_010.png) |
| 61 | `TC_LEAVE_SELF_011` | `LEAVE_SELF` | Apply không khả dụng khi nhân viên chưa có entitlement kỳ hiện tại | Hiển thị No Leave Types with Leave Balance. Form chọn loại phép/ngày và nút Apply không khả dụng; không thể nộp đơn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 14.26s | [TC_LEAVE_SELF_011.png](screenshots/TC_LEAVE_SELF_011.png) |
| 62 | `TC_LEAVE_SELF_012` | `LEAVE_SELF` | Nộp đơn xin nghỉ trùng hoàn toàn khoảng ngày với đơn đã có | Hiển thị Overlapping Leave Request(s) Found, không tạo thêm đơn trùng. Tìm lại đúng nhân viên/loại phép/ngày trong My Leave và chỉ có một đơn ban đầu. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 23.68s | [TC_LEAVE_SELF_012.png](screenshots/TC_LEAVE_SELF_012.png) |
| 63 | `TC_LEAVE_SELF_013` | `LEAVE_SELF` | Nộp đơn xin nghỉ trùng một phần khoảng ngày (giao thoa 1 ngày) | Hiển thị Overlapping Leave Request(s) Found và cảnh báo từ chối nộp. Không thêm đơn mới giao thoa ngày với đơn đầu vào. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 13.48s | [TC_LEAVE_SELF_013.png](screenshots/TC_LEAVE_SELF_013.png) |
| 64 | `TC_LEAVE_SELF_014` | `LEAVE_SELF` | Nhập nội dung Comments đúng biên tối đa 250 ký tự | Đơn được gửi thành công. Comments lưu nguyên vẹn 250 ký tự mà không bị cắt bớt. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 22.98s | [TC_LEAVE_SELF_014.png](screenshots/TC_LEAVE_SELF_014.png) |
| 65 | `TC_LEAVE_SELF_015` | `LEAVE_SELF` | Nhập nội dung Comments vượt biên 250 ký tự (251 ký tự) | Hiển thị lỗi vượt quá độ dài "Should not exceed 250 characters" hoặc input chặn ký tự thứ 251. Không lưu chuỗi 251 ký tự. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 22.23s | [TC_LEAVE_SELF_015.png](screenshots/TC_LEAVE_SELF_015.png) |
| 66 | `TC_LEAVE_SELF_016` | `LEAVE_SELF` | Xem danh sách My Leave với bộ lọc trạng thái mặc định | Bảng hiển thị danh sách đơn của chính người dùng đăng nhập. Các cột Date, Employee Name, Leave Type, Leave Balance, Number of Days, Status hiển thị rõ ràng. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 23.77s | [TC_LEAVE_SELF_016.png](screenshots/TC_LEAVE_SELF_016.png) |
| 67 | `TC_LEAVE_SELF_017` | `LEAVE_SELF` | Lọc danh sách My Leave theo khoảng thời gian From Date - To Date | Tất cả các bản ghi trả về đều có ngày nghỉ nằm trong khoảng từ 01/11/2026 đến 30/11/2026. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 7.53s | [TC_LEAVE_SELF_017.png](screenshots/TC_LEAVE_SELF_017.png) |
| 68 | `TC_LEAVE_SELF_018` | `LEAVE_SELF` | Lọc danh sách My Leave với khoảng thời gian không có đơn | Bảng kết quả hiển thị thông báo "No Records Found". Không có dòng dữ liệu cũ nào còn sót lại. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 5.22s | [TC_LEAVE_SELF_018.png](screenshots/TC_LEAVE_SELF_018.png) |
| 69 | `TC_LEAVE_SELF_019` | `LEAVE_SELF` | Lọc danh sách My Leave theo trạng thái Pending Approval | Tất cả các dòng kết quả đều có giá trị cột Status là "Pending Approval", không lẫn trạng thái khác. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 31.06s | [TC_LEAVE_SELF_019.png](screenshots/TC_LEAVE_SELF_019.png) |
| 70 | `TC_LEAVE_SELF_020` | `LEAVE_SELF` | Lọc danh sách My Leave theo Leave Type cụ thể | Mọi dòng trả về đều có cột Leave Type đúng là F_TYPE. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 22.76s | [TC_LEAVE_SELF_020.png](screenshots/TC_LEAVE_SELF_020.png) |
| 71 | `TC_LEAVE_SELF_021` | `LEAVE_SELF` | Reset bộ lọc trên trang My Leave | Reset khôi phục From/To về đầu/cuối kỳ phép mặc định; Leave Type về chưa chọn và các chip trạng thái về mặc định của My Leave. Bảng tải lại theo bộ lọc mặc định, không yêu cầu ngày bị xóa rỗng. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 5.92s | [TC_LEAVE_SELF_021.png](screenshots/TC_LEAVE_SELF_021.png) |
| 72 | `TC_LEAVE_SELF_022` | `LEAVE_SELF` | Hủy đơn xin nghỉ phép đang ở trạng thái Pending Approval | Thông báo "Successfully Updated". Trạng thái đơn chuyển từ "Pending Approval" sang "Cancelled". Số dư phép (Leave Balance) được hoàn lại số ngày của đơn. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 30.32s | [TC_LEAVE_SELF_022.png](screenshots/TC_LEAVE_SELF_022.png) |
| 73 | `TC_LEAVE_SELF_023` | `LEAVE_SELF` | Hủy đơn xin nghỉ phép đang ở trạng thái Scheduled (đã duyệt) | Thao tác thành công. Trạng thái đơn đổi thành "Cancelled", số ngày nghỉ được hoàn lại vào Leave Balance của nhân viên. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 95.02s | [TC_LEAVE_SELF_023.png](screenshots/TC_LEAVE_SELF_023.png) |
| 74 | `TC_LEAVE_SELF_024` | `LEAVE_SELF` | Kiểm tra đơn đã Rejected không có tùy chọn Cancel | Tại cột Actions của đơn Rejected không xuất hiện nút Cancel hoặc nút Cancel bị vô hiệu hóa, không thể click. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 70.47s | [TC_LEAVE_SELF_024.png](screenshots/TC_LEAVE_SELF_024.png) |
| 75 | `TC_LEAVE_SELF_025` | `LEAVE_SELF` | Kiểm tra đơn đã Taken trong quá khứ không cho phép Cancel | Không cho phép Cancel đơn đã Taken. Không hiển thị nút Cancel trên dòng bản ghi này. | PASS – Toàn bộ assertion Cypress trong case hoàn tất. | Pass | **Pass** | 50.89s | [TC_LEAVE_SELF_025.png](screenshots/TC_LEAVE_SELF_025.png) |
