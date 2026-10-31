# Automation Testing Project - Group 01

Runner ASM8 gọi script gốc trong `automation/MSSV/`, chạy tuần tự trên OrangeHRM online và tạo một report nhóm. Giữ nguyên công cụ của từng thành viên.

## Cài đặt

Cần Node.js 22+, Microsoft Edge, mạng Internet. Chạy tại repo root:

```powershell
npm ci
npx cypress install
npm run check
npm test
```

Nếu Cypress báo `bad option: --smoke-test`, xóa biến `ELECTRON_RUN_AS_NODE` trong terminal. Nếu npm chặn postinstall, lệnh `npx cypress install` cài binary riêng.

## Phạm vi chạy

| Thành viên | Script trong inventory | ASM8 |
| --- | ---: | --- |
| 23130226 | 75 case PIM | Chạy 25 EMP, sau đó 50 Personal/Job |
| 22130254 | 6 case Leave | Chạy Cypress, fixture và cleanup riêng |
| 23130171 | 25 case ADM_STRUCT | Skipped: selector sửa/xóa chưa giới hạn node test, thiếu assertion/cleanup |
| 23130339 | 6 bước Time theo tài liệu | Skipped: dùng dữ liệu Admin, bỏ qua lỗi, chưa có report theo case |
| 23130299 | 75 case Recruitment | Skipped theo yêu cầu nhóm trưởng, thiếu cấu hình/Katalon |

Inventory có 187 ID, gồm 6 ID Time lấy từ mô tả ASM7. Đây không phải toàn bộ case thiết kế trong workbook. Chưa có script ADM_JOB/ADM_USER; các suite ASM7 rút gọn không chứng minh đã triển khai đủ 20–30 case/chức năng.

## Chạy riêng và đọc kết quả

```powershell
npm run test:pim
npm run test:leave
npm run retest:pim   # Search_02 và Sort_01 sau sửa script
```

Mỗi lệnh tạo Run ID mới. `npm test` chọn toàn bộ inventory; `--member=MSSV` chọn một thành viên. Lệnh `--check` chỉ kiểm tra source, số ID và trùng ID, không thao tác website.

`node "Automation Testing Project - Group 01/summarize.mjs" <JSON-luot-nhom> <JSON-retest-1> <JSON-retest-2>` tổng hợp kết quả mới nhất theo Case ID, giữ nguyên từng report gốc và tính riêng số lần thực thi có retest.

- `Test Report - Group 01/<RunID>/index.html`: tổng hợp và chi tiết từng case; JSON ở cùng thư mục.
- `Envidence - Group01/<RunID>/`: log, screenshot, fixture, report gốc và SHA-256 source. Giữ cách viết `Envidence` theo đề.
- Pass: assertion hoàn tất. Fail: assertion lỗi hoặc runner gốc báo Blocked, giữ nguyên trạng thái gốc và lý do. Skipped: chưa thực thi, không tính pass rate.
- Exit code 1 nếu có Fail, Skipped hoặc lỗi hạ tầng/cleanup. Không dùng exit code của script con để tự suy ra toàn bộ case Pass.

PIM_EMP dùng ID fixture cố định, không chạy đồng thời. Personal/Job và Leave tạo dữ liệu riêng. Kiểm tra ledger cleanup trước khi chạy lại; chỉ dọn bản ghi xác nhận thuộc lần chạy. Retest lưu Run ID riêng và so sánh cùng Case ID trước khi kết luận flaky.

## Việc cần hoàn thiện

1. Linh sửa selector theo ID/node riêng, assertion và cleanup; bổ sung ADM_JOB/ADM_USER, cấu hình `testMatch` cho `.mjs`.
2. Trâm dùng nhân viên test riêng, assert giờ/trạng thái đã lưu, xuất kết quả từng ID và báo lỗi qua exit code.
3. Khi các script đạt điều kiện, cập nhật inventory/adapter trong `run.mjs`, chạy riêng rồi chạy nhóm lại. Recruitment giữ Skipped theo yêu cầu hiện tại.
4. Mỗi người làm 8 phần slide theo đề, dùng cùng bố cục slide của nhóm. Nộp PPTX và ZIP cá nhân lên Classroom.

[Báo cáo](../Test%20Report%20-%20Group%2001/README.md) · [Minh chứng](../Envidence%20-%20Group01/README.md)
