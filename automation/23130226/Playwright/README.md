# PIM – Playwright

Node.js 20+, Microsoft Edge; Playwright 1.62.1. Dùng tài khoản demo `Admin/admin123`.

```powershell
npm install
npm test       # ASM6: 25 case PIM_EMP
npm run asm7   # ASM7: 15 case, 3 chức năng PIM
npm run details # 25 PIM_PERSONAL + 25 PIM_JOB
```

- ASM6: kết quả tại `evidence/executions/23130226/<run-id>/`. ID fixture cố định `G1N01*`; không chạy đồng thời hoặc xóa dữ liệu không rõ nguồn.
- ASM7: [case và cách chạy](ASM7.md); kết quả mặc định tại `results/ASM7_<timestamp>/`.
- Personal/Job: `run-pim-details.mjs` đọc 50 ID/expected trong `pim-details-cases.json`; tạo hai nhân viên riêng, chạy assertion và cleanup. Log/ảnh/PDF tại `results/PIM_DETAILS_<timestamp>/` hoặc thư mục `PIM_DETAILS_OUTPUT`.

Đặt `PIM_DETAILS_MODULE=PIM_PERSONAL` hoặc `PIM_JOB` để chạy riêng 25 case. Case loại tệp hợp đồng dùng `.bin` vì môi trường cho phép CSV.

`node report-html.mjs <duong-dan-results.json>` tạo báo cáo HTML. Nếu cleanup lỗi, xem ID/key trong JSON; `PIM_DETAILS_CLEANUP=<duong-dan-JSON>` chỉ dọn fixture của run đó qua UI, không chạy case.

ASM6 hỗ trợ `PIM_RUN_ID`, `PIM_REPO`, `PIM_ONLY` (số case 1–25, phân cách dấu phẩy). `PIM_CLEANUP_PREVIOUS` nhận các đường dẫn JSON của chính runner, phân cách bằng dấu chấm phẩy; cleanup kiểm tra ID và employee key.

[Kết quả thực thi](../../../evidence/executions/23130226/README.md)
