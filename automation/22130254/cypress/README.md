# 22130254 – Cao Tiến Thành – ASM7 & ASM8

[Script Cypress](e2e/leave/22130254_CaoTienThanh_ASM7.cy.js) gồm **75 testcase** thuộc 3 chức năng (`25 LEAVE_CONFIG`, `25 LEAVE_SELF`, `25 LEAVE_ADMIN`), chạy tuần tự theo luồng nghiệp vụ: cấu hình loại phép & hạn mức → nhân viên xin nghỉ & tra cứu My Leave → Admin lọc, xử lý yêu cầu và kiểm tra đồng bộ.

| Phân hệ | Phạm vi kiểm thử | Số lượng |
| --- | --- | ---: |
| `LEAVE_CONFIG` (`TC_LEAVE_CONFIG_001` – `025`) | Tạo, sửa, xóa Leave Type, validation biên/trùng/rỗng; cấp và lọc Leave Entitlements | 25 |
| `LEAVE_SELF` (`TC_LEAVE_SELF_001` – `025`) | Apply Leave (1 ngày, nhiều ngày, nửa ngày, theo giờ, validation, trùng lịch, comment 250/251); lọc, Reset và Cancel trên My Leave | 25 |
| `LEAVE_ADMIN` (`TC_LEAVE_ADMIN_001` – `025`) | Lọc Leave List (Employee, Leave Type, Sub Unit, Date, Status, Past Employee, Reset), Approve/Reject/Cancel đơn lẻ & hàng loạt (Bulk), Comments, Phân trang (51 đơn), View Leave Details | 25 |

Các kết quả ASM8 lấy từ JSON Cypress và screenshot thực tế của lần chạy mới.

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


