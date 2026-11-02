# Admin – Playwright

`23130171_NguyenThiThuyLinh_ASM7.mjs` khai báo 25 case ADM_STRUCT bằng `@playwright/test`. `run-adm.mjs` là runner riêng cho cùng phạm vi. Chưa có script ADM_JOB/ADM_USER trong `dev`.

Cài bằng `npm ci` trong thư mục này. [Danh sách case](ASM7.md) · [Minh chứng đã nộp](../../../evidence/executions/23130171/ADM_STRUCT/).

Trước khi chạy ASM8, cần giới hạn sửa/xóa vào node tạo riêng cho lần chạy, bổ sung assertion và cleanup. Script hiện chọn `.bi-pencil.first()`/`.bi-trash.first()`/`.last()` nên runner nhóm ghi Skipped. Các ảnh ASM7 không thay cho report chạy ASM8.
