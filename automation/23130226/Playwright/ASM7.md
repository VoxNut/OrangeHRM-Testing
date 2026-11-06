# ASM7 – PIM

Script: `23130226_VoMinhNhut_ASM7.mjs`. Playwright/Edge; tài khoản demo `Admin/admin123`.

```powershell
npm install
npm run asm7
```

Cần Node.js 20+ và Edge. Nếu thiếu bộ ghi video: `npx playwright install ffmpeg`.

## Case triển khai

| ID | Kiểm tra |
| --- | --- |
| PIM_EMP_Create_03 | First Name trống: Required, không tạo hồ sơ |
| PIM_EMP_CancelCreate_01 | Cancel, không tạo hồ sơ |
| PIM_EMP_Create_01 | Tạo, mở lại và tìm đúng nhân viên |
| PIM_EMP_Search_01 | Tìm đúng Employee Id |
| PIM_EMP_Search_03 | ID không tồn tại: No Records Found |
| PIM_PERSONAL_Update_01 | Lưu ba thành phần tên |
| PIM_PERSONAL_Update_02 | Chặn tên rỗng, giữ dữ liệu cũ |
| PIM_PERSONAL_Update_14 | Ngày sinh nhuận 29/02/2000 |
| PIM_PERSONAL_Update_18 | Lưu quốc tịch |
| PIM_PERSONAL_CreateAttachment_01 | Upload/tải PDF, đối chiếu SHA-256 |
| PIM_JOB_UpdateJob_08 | Lưu ngày vào làm và 5 danh mục |
| PIM_JOB_UpdateJob_02 | Chặn ngày 31/02, giữ ngày cũ |
| PIM_JOB_ToggleContract_01 | Hiển thị trường hợp đồng |
| PIM_JOB_UpdateContract_02 | Chặn End Date trước Start Date |
| PIM_JOB_UploadContract_01 | Lưu ngày/tệp hợp đồng, đối chiếu SHA-256 |

15 case đại diện, 5 case/chức năng. Suite chạy tuần tự: Create_01 tạo nhân viên cho các case cập nhật phía sau. ID ngẫu nhiên theo run; cleanup đối chiếu Employee Id và employee key trước khi xóa.

Kết quả ở `results/ASM7_<timestamp>/`: JSON, screenshot, PDF và `video/browser-recording.webm`. Đổi thư mục bằng `ASM7_OUTPUT_DIR`. Failed = assertion không đạt; Blocked = không hoàn tất do setup/timeout; NotRun = chưa chạy. Exit code khác 0 nếu suite chưa đạt hoặc cleanup lỗi; xem JSON để xử lý đúng hồ sơ còn lại.

[Report](../../../evidence/executions/23130226/ASM7_2026-10-24T02-46-28-365Z/README.md) · [Video](https://youtu.be/CZvZ5havwgk)
