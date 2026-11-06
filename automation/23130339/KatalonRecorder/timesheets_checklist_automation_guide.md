# Timesheets – Kế hoạch & Hướng dẫn Automation

[Workbook]: 41 case/chức năng cho TIME_MY (My Timesheets) và TIME_EMP (Employee Timesheets).

---

## 1. Yêu cầu môi trường & Phụ thuộc (Prerequisites)

* **Script chính**: `23130339_TonBichTram_ASM7.js` (hoặc các file runner tương ứng trong thư mục `automation/23130339/`).
* **Công cụ / Thư viện**: Puppeteer, Node.js (`fs`, `path`).
* **Node.js**: Phiên bản `18.x` trở lên.
* **Trình duyệt**: Google Chrome (đã được cài đặt sẵn trên máy).
* **Tài khoản Demo**: `Admin` / `admin123`

### Cài đặt dependencies

Mở Terminal / PowerShell tại thư mục gốc của dự án (`DoAnCK`) và chạy lệnh:

```powershell
npm install
```

*Lưu ý:* Nếu dự án chưa cài sẵn Puppeteer trong `package.json`, thực hiện cài bổ sung bằng lệnh:

```bash
npm install puppeteer
```

---

## 2. Lệnh thực thi Script (Execution Command)

Chạy trực tiếp file kịch bản tự động hóa bằng Node.js qua lệnh sau:

```powershell
node automation/23130339/23130339_TonBichTram_ASM7.js
```

Hoặc nếu chạy qua lệnh script tùy chỉnh trong `package.json`:

```bash
npm run test:timesheets
```

---

## 3. Danh sách Test Case triển khai (Test Inventory)

| Case ID | Kiểm tra / Mô tả chi tiết & Assertion Check |
| :--- | :--- |
| **TIME_ATTEND_PunchIn_01** | Kiểm tra nhập giờ Punch In hợp lệ, ghi nhận giờ vào ca và hiển thị toast `"Successfully Saved"`. |
| **TIME_ATTEND_PunchOut_01** | Kiểm tra nhập giờ Punch Out hợp lệ, đối chiếu xác nhận kết thúc ca làm việc. |
| **TIME_MY_Edit_01** | Mở My Timesheet, chọn Project & Activity và nhập 8 giờ làm việc cho các ngày trong tuần. |
| **TIME_MY_Submit_01** | Gửi nộp (Submit) bảng chấm công cá nhân lên hệ thống và kiểm tra assertion. |
| **TIME_EMP_Search_01** | Tìm kiếm bảng công của nhân viên theo tên từ gợi ý autocomplete. |
| **TIME_EMP_Approve_01** | Quản trị viên thực hiện phê duyệt (Approve) bảng chấm công nhân viên và reset dọn dẹp. |

---

## 4. Kịch bản thực thi End-to-End (E2E Workflow)

Kịch bản End-to-End chạy tuần tự qua 3 chức năng chính của module **Time**:

1. **TIME_ATTEND**: Thực hiện chu trình chấm công `Punch In` $\rightarrow$ `Punch Out` và dọn dẹp bản ghi trong lịch sử điểm danh.
2. **TIME_MY**: Tạo, lưu, gửi duyệt (`Submit`) và hoàn trả trạng thái ban đầu của bảng công cá nhân.
3. **TIME_EMP**: Tìm kiếm hồ sơ nhân viên, kiểm tra bảng công và thực hiện thao tác duyệt (`Approve`).

---

## 5. Quy trình thực hiện chi tiết (Implementation Rules)

1. **Giao diện & Ràng buộc (UI & Validation)**: Xác nhận UI, validation, định dạng ngày, tổng giờ (`Total` $\le$ 24h/ngày) và danh mục dự án.
2. **Chuẩn bị dữ liệu**: Chuẩn bị nhân viên/bảng giờ test riêng; ghi nhận Employee Id, trạng thái Submit và dữ liệu hàng ngang/dọc.
3. **Thao tác chính**: Kiểm tra Create/Search, chỉnh sửa số giờ, thêm/xóa dòng công việc và phê duyệt/từ chối.
4. **True Assertion**: Assert dữ liệu sau Save/Approve; negative case phải kiểm tra lỗi (ví dụ: vượt quá 24h/ngày, bỏ trống comment khi reject) và dữ liệu không được lưu sai.
5. **Bằng chứng & Cleanup**: Lưu log/ảnh screenshot vào đúng cấu trúc `evidence/executions/23130339/` trước khi cleanup; chỉ xóa đúng bảng giờ của lần chạy. Báo lỗi qua exit code nếu có case Fail.

Case sửa đổi mở bằng tài khoản quản trị hoặc nhân viên tương ứng. Runner script dùng ID và module phân tách, không chạy đồng thời để tránh xung đột dữ liệu.

`npm run timesheets` triển khai đầy đủ 41 test cases cho My Timesheets và Employee Timesheets, gồm validation, lưu/mở lại, chỉnh sửa giờ và trạng thái duyệt.

[Script và lệnh chạy] · [Kết quả]