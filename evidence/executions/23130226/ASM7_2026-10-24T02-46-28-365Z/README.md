# ASM7 — PIM — Võ Minh Nhựt, 23130226

Lần chạy thật trên <https://opensource-demo.orangehrmlive.com/>, ngày 24/10/2026, từ 09:46:28 đến 09:48:39 (UTC+7). Công cụ: Playwright 1.62.1; Microsoft Edge 156.0.4314.42, headless, có recordVideo.

Kết quả: **15 Passed, 0 Failed, 0 Blocked, 0 NotRun**. Exit code 0. Ba chức năng có 5 case đại diện mỗi chức năng; không phải kết quả thực thi toàn bộ 75 case thiết kế ASM6.

| Chức năng | Passed | Failed | Blocked |
| --- | ---: | ---: | ---: |
| PIM_EMP | 5 | 0 | 0 |
| PIM_PERSONAL | 5 | 0 | 0 |
| PIM_JOB | 5 | 0 | 0 |

- [Script và cách chạy](../../../../automation/23130226/Playwright/ASM7.md).
- [JSON report gốc](results.json): thời điểm, actual, duration, đường dẫn screenshot và kết quả cleanup cho từng case.
- `screenshots/`: 19 ảnh chụp thật, gồm 15 ảnh kết thúc case và 4 ảnh validation trước khi mở lại trang.
- `assets/`: PDF fixture chỉ chứa dữ liệu giả và hai bản tải xuống để so sánh SHA-256.
- Bản quay trình duyệt gốc dài 128,8 giây. Video demo có thêm tiêu đề, nhãn case và kết quả từ report, dài 146,8 giây; không cắt hoặc tăng tốc phần chạy thật. Video lưu ngoài Git.

Cleanup đã đối chiếu Employee Id `N7f86d260a` và numeric employee key `221`, xóa qua UI và tìm lại: không còn bản ghi. Không sửa/xóa nhân viên hoặc cấu hình dùng chung.

SHA-256 script đã chạy: `8b6b1f5bd3658edad429e5a479255977243825e38fa57a15b8bd43975eae64e0`.

Đường dẫn `video/browser-recording.webm` trong report là đường dẫn tương đối tại thư mục kết quả gốc; tệp video không được commit. Hai lần chạy chẩn đoán trước khi sửa thao tác dropdown/download được giữ riêng trên máy, không cộng vào số liệu của lần chạy này. Kết quả ASM6 được giữ nguyên.
