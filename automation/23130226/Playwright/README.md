# PIM – Playwright

Node.js 20+, Microsoft Edge; Playwright 1.62.1. Dùng tài khoản demo `Admin/admin123`.

```powershell
npm install
npm test       # ASM6: 25 case PIM_EMP
npm run asm7   # ASM7: 15 case, 3 chức năng PIM
```

- ASM6: kết quả tại `evidence/executions/23130226/<run-id>/`. ID fixture cố định `G1N01*`; không chạy đồng thời hoặc xóa dữ liệu không rõ nguồn.
- ASM7: [case và cách chạy](ASM7.md); kết quả mặc định tại `results/ASM7_<timestamp>/`.

ASM6 hỗ trợ `PIM_RUN_ID`, `PIM_REPO`, `PIM_ONLY` (số case 1–25, phân cách dấu phẩy). `PIM_CLEANUP_PREVIOUS` nhận các đường dẫn JSON của chính runner, phân cách bằng dấu chấm phẩy; cleanup kiểm tra ID và employee key.

[Kết quả thực thi](../../../evidence/executions/23130226/README.md)
