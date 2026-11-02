# 22130254 – Cao Tiến Thành – ASM7

[Script Cypress](e2e/leave/22130254_CaoTienThanh_ASM7.cy.js) có **6 testcase**, chạy theo thứ tự cấu hình phép → nhân viên xin nghỉ → Admin duyệt → nhân viên kiểm tra kết quả.

| Testcase | Nội dung |
| --- | --- |
| `TC_LEAVE_CONFIG_001` | Tạo loại phép với các trường bắt buộc |
| `TC_LEAVE_CONFIG_016` | Cấp hạn mức phép hợp lệ cho nhân viên |
| `TC_LEAVE_CONFIG_008` | Kiểm tra hạn mức theo nhân viên, loại phép và kỳ |
| `TC_LEAVE_SELF_001` | Nhân viên nộp đơn nghỉ một ngày hợp lệ |
| `TC_LEAVE_ADMIN_010` | Admin duyệt đơn Pending Approval thành Scheduled |
| `TC_LEAVE_ADMIN_011` | Nhân viên thấy đơn Scheduled trên My Leave |

Các kết quả ASM8 lấy từ JSON Cypress và screenshot của lần chạy mới. Video ASM7 không có trong nhánh `dev` hiện tại.

## Cài đặt và chạy

Cần Node.js 20.19+ và Microsoft Edge. Chạy tại thư mục gốc `OrangeHRM-Testing`:

```powershell
npm ci
npm run test:leave
```

Mở giao diện Cypress bằng `npm run cy:open`. Cấu hình nhóm trỏ tới spec, support và fixture gốc; `testIsolation: false` giữ phiên giữa các case phụ thuộc. Runner không tự retry các thao tác ghi dữ liệu.

Nếu terminal có `ELECTRON_RUN_AS_NODE=1` và Cypress báo `bad option: --smoke-test`, bỏ biến trong phiên terminal rồi chạy lại:

```powershell
Remove-Item Env:ELECTRON_RUN_AS_NODE -ErrorAction SilentlyContinue
npm test
```


