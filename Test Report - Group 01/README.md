# Test Report - Group 01

Mỗi thư mục là một lần chạy ASM8: `index.html` để mở bằng trình duyệt, `results.json` để đối chiếu từng Case ID. Kết quả đầu và retest giữ riêng; không cộng trùng case giữa các lượt.

[Tổng hợp mới nhất 13/11/2026](ASM8_SUMMARY_2026-11-13T10-54-12-259Z/index.html): **150 Pass, 0 Fail, 106 Skipped** theo kết quả mới nhất của **256 ID** (`Pass Rate = 150 / 150 × 100% = 100.00%`). Đã thực thi 150 case khác nhau (75 PIM + 75 Leave), 165 lần nếu tính cả các lượt retest; tổng thời gian thực thi các lượt **5.325,68 giây**.
- **PIM (`23130226`):** 75/75 Pass (`25 PIM_EMP` + `50 PIM_DETAILS`).
- **Leave (`22130254`):** 75/75 Pass (`25 LEAVE_CONFIG` + `25 LEAVE_SELF` + `25 LEAVE_ADMIN`).
  - Lượt chạy đầy đủ đầu tiên ([ASM8_2026-11-13T08-57-47-357Z](ASM8_2026-11-13T08-57-47-357Z/index.html)): **72 Pass, 3 Fail, 0 Skipped** (`Pass Rate = 96.00%`, thời gian `3.710,67 giây`). 3 case lỗi thuộc `LEAVE_CONFIG` (`TC_LEAVE_CONFIG_002`, `TC_LEAVE_CONFIG_005`, `TC_LEAVE_CONFIG_011`) do xung đột giữa auto-redirect của OrangeHRM sau khi `Save` Leave Type và lệnh mở lại danh sách kiểm tra.
  - Lượt chạy lại 3 case sau khi đồng bộ chờ redirect ([ASM8_RETEST_2026-11-13T10-54-12-259Z](ASM8_RETEST_2026-11-13T10-54-12-259Z/index.html)): **3/3 Pass** (`75,54 giây`).
- **Admin (`23130171` - 25 ID), Time (`23130339` - 6 ID), Recruitment (`23130299` - 75 ID):** 106 Skipped (chưa đủ điều kiện chạy hoặc bỏ qua theo yêu cầu nhóm trưởng).

[Tổng hợp trước đó 31/10/2026 (khi Leave còn bản rút gọn 6 case)](ASM8_SUMMARY_2026-10-31T01-37-43-636Z/index.html): **81 Pass, 0 Fail, 106 Skipped** trên 187 ID.

[Cách chạy và giới hạn](../Automation%20Testing%20Project%20-%20Group%2001/README.md) · [Evidence](../Envidence%20-%20Group01/README.md)