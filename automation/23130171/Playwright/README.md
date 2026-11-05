# Admin – Playwright

`23130171_NguyenThiThuyLinh_ASM7.mjs` khai báo 25 case ADM_STRUCT bằng `@playwright/test`. `run-adm.mjs` là runner riêng cho cùng phạm vi. Chưa có script ADM_JOB/ADM_USER trong `dev`.

Cài bằng `npm ci` trong thư mục này. [Danh sách case](ASM7.md) · [Minh chứng đã nộp](../../../evidence/executions/23130171/ADM_STRUCT/).
# OrangeHRM Admin · 23130171

Cần Node.js 22 trở lên. Mở terminal tại thư mục chứa `package.json`.

```bash
npm ci
npx playwright install chromium
npm run admin:initial
```

- Chạy riêng với `npm run admin:initial -- --module ADM_STRUCT`. Đổi thành `ADM_JOB` ho�
