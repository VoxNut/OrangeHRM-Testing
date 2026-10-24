# Assignment 7 — Võ Minh Nhựt, 23130226

Script cá nhân: `23130226_VoMinhNhut_ASM7.mjs`. Website thật: <https://opensource-demo.orangehrmlive.com/>. Công cụ: Playwright trên Microsoft Edge. Tài khoản `Admin/admin123` là tài khoản demo công khai.

## Chạy lại

Máy cần Node.js 20 trở lên và Microsoft Edge. Tại thư mục Playwright:

```powershell
npm install
npm run asm7
```

Kết quả mặc định nằm trong `results/ASM7_<timestamp>/`: `results.json`, `screenshots/`, `assets/` và `video/browser-recording.webm`. Có thể đặt `ASM7_OUTPUT_DIR` để lưu tại thư mục khác. Video là bản ghi giao diện trình duyệt thực sự do Playwright tạo, không phải mô phỏng. Nếu máy thiếu FFmpeg của Playwright, chạy `npx playwright install ffmpeg` rồi chạy lại.

## Phạm vi và truy vết

Triển khai 15 case đại diện, 5 case cho mỗi chức năng. Đây là phạm vi ASM7 đã triển khai; không coi các case còn lại trong 75 case thiết kế ASM6 là đã chạy. ID theo workbook, dữ liệu nhân viên được thay bằng token riêng của từng lần chạy để tránh trùng dữ liệu trên demo dùng chung.

| Test case ID | Nội dung và assertion chính |
| --- | --- |
| PIM_EMP_Create_03 | First Name trống: Required, không tạo bản ghi |
| PIM_EMP_CancelCreate_01 | Cancel quay về danh sách, không tạo bản ghi |
| PIM_EMP_Create_01 | Tạo nhân viên; mở lại, đối chiếu tên/ID và đúng một dòng trong danh sách |
| PIM_EMP_Search_01 | Tìm theo Employee Id, chỉ có nhân viên của lần chạy |
| PIM_EMP_Search_03 | ID không tồn tại: bảng rỗng và No Records Found |
| PIM_PERSONAL_Update_01 | Ba thành phần tên được giữ sau khi mở lại đúng hồ sơ |
| PIM_PERSONAL_Update_02 | First Name rỗng bị chặn, tên cũ còn nguyên sau mở lại |
| PIM_PERSONAL_Update_14 | Lưu ngày sinh nhuận 29/02/2000 theo định dạng UI |
| PIM_PERSONAL_Update_18 | Quốc tịch đã chọn được giữ sau mở lại |
| PIM_PERSONAL_CreateAttachment_01 | Đúng một attachment PDF; tên, mô tả và SHA-256 tệp tải xuống khớp |
| PIM_JOB_UpdateJob_08 | Ngày vào làm và 5 danh mục công việc giữ đúng sau mở lại |
| PIM_JOB_UpdateJob_02 | Ngày 31/02 bị chặn; ngày đã lưu không thay đổi |
| PIM_JOB_ToggleContract_01 | Bật switch hiển thị ngày bắt đầu/kết thúc và upload hợp đồng |
| PIM_JOB_UpdateContract_02 | End Date trước Start Date bị chặn, không lưu hai ngày sai |
| PIM_JOB_UploadContract_01 | Hai ngày hợp lệ, tên PDF giữ sau mở lại; SHA-256 tệp tải xuống khớp |

## Setup, dependency và cleanup

Suite chạy tuần tự. Hai case validation/Cancel đầu không tạo dữ liệu; `PIM_EMP_Create_01` tạo một nhân viên dùng riêng cho các case sửa hồ sơ và công việc phía sau. Mỗi case mở lại đúng trang và xác nhận dữ liệu cần dùng; nếu tạo nhân viên thất bại, các case phụ thuộc bị Blocked. Đây là dependency được khai báo, không chạy riêng các case phía sau khi chưa setup.

ID gồm `N7` và 8 ký tự ngẫu nhiên. Trước khi tạo phải kiểm tra ID chưa tồn tại. Chỉ chọn danh mục có sẵn, ghi nhãn thực tế vào report; không sửa cấu hình, tài khoản hoặc nhân viên dùng chung. PDF được tạo tại chỗ chỉ chứa thông tin fixture giả. Script đọc placeholder ngày để phân biệt `yyyy-dd-mm` và `yyyy-mm-dd`.

Cuối suite, cleanup kiểm tra đúng Employee Id và numeric employee key do chính request tạo của lần chạy trả về, rồi xóa qua UI và tìm lại để xác nhận không còn bản ghi. Nếu cleanup lỗi, xem `createdEmployee` và `cleanup` trong report để xử lý đúng hồ sơ; không xóa hàng loạt hoặc chạy lại bằng ID của người khác.

## Đọc kết quả

- **Passed:** assertion trong case đạt; positive case kiểm tra sau mở lại, negative case kiểm tra validation và không lưu dữ liệu sai.
- **Failed:** actual không khớp assertion. Cần xem log/ảnh trước khi kết luận lỗi sản phẩm.
- **Blocked:** timeout, locator, mạng hoặc thiếu fixture làm case không hoàn tất.
- **NotRun:** chưa thực thi do lỗi ở cấp suite/setup.

Process trả exit code khác 0 nếu có case chưa Passed, lỗi cấp suite hoặc cleanup chưa xong. Không tự đổi Fail/Blocked thành Passed. Workbook và kết quả ASM6 không được cập nhật bằng run ASM7 này.

## Bài nộp

ZIP cá nhân được chuẩn bị ngoài repo, tên `23130226_VoMinhNhut_ASM7.zip`, gồm script, package.json và hướng dẫn chạy. File `23130226_VoMinhNhut_ASM7.txt` có đúng hai dòng: link video YouTube thật và link repo public có nhánh chứa script. Không đưa `node_modules`, video hoặc `.git` vào ZIP script. Video và report được lưu riêng; đường dẫn bằng chứng của lần chạy chính thức được ghi trong README repo.
