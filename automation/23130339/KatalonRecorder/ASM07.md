# Time – Puppeteer

Script `23130339_TonBichTram_ASM7.js` dùng Puppeteer/Chrome, dù thư mục có tên KatalonRecorder. Phạm vi demo: Punch In/Out, sửa/Submit My Timesheet, tìm/Approve Employee Timesheet.

Cần Node.js 18+, Chrome và package `puppeteer`. Đường dẫn script tính từ repo root:

```powershell
node automation/23130339/KatalonRecorder/23130339_TonBichTram_ASM7.js
```

ASM8 chưa chạy script này: đang thao tác trên attendance/timesheet của Admin, bỏ qua một số lỗi và chưa trả kết quả từng case. Cần tài khoản/nhân viên test riêng, assertion trạng thái/giờ đã lưu, log theo Case ID và exit code khác 0 khi lỗi. Reset bộ lọc không chứng minh đã xóa hay khôi phục dữ liệu.

[Project tích hợp](../../../Automation%20Testing%20Project%20-%20Group%2001/README.md)
