# Quy trình đóng góp

## Tài liệu và script

- Workbook: `docs/test-cases/members/Group01_MaSV_HoTen.xlsx`; mỗi người phụ trách 3 chức năng, 20–30 case/chức năng.
- ID: `Module_Action_XX`, đánh số từ 01 cho từng hành động, ví dụ `PIM_EMP_Create_01`.
- Script: `automation/MaSV/TenCongCu/`; kèm cách cài đặt, chạy và chuẩn bị/dọn dữ liệu.
- Kết quả: `evidence/executions/MaSV/RunID/`; cập nhật Result theo log của đúng lần chạy.
- Nhóm trưởng ghép workbook đã review vào `docs/test-cases/merged/Group01_OrangeHRM.xlsx`.

## Git

Ví dụ trên nhánh `nhut`:

```powershell
git fetch origin
git switch nhut
git pull --ff-only origin nhut
git merge origin/dev
git add -- <duong-dan-file>
git commit -m "docs: update test cases"
git push origin nhut
```

Tạo PR vào `dev`; nhóm trưởng review và merge. Chỉ chỉnh phần được giao; conflict workbook cần phối hợp với tác giả. Không force-push nhánh chung.

Trước khi merge: kiểm tra tên file, ID, input/expected, dependency, công thức report và hướng dẫn chạy.

## Nộp bài

| Bài | File nộp |
| --- | --- |
| ASM6 | `MaSV_HoTen.zip`: `Group01.txt`, ảnh Git `MaSV_HoTen.png`, `Group01_MaSV_HoTen.xlsx` |
| ASM7 | Script `MaSV_HoTen_ASM7` (nhiều tệp nén ZIP) và `MaSV_HoTen_ASM7.txt`: dòng 1 link YouTube, dòng 2 link repo public |

Giữ gói nộp ngoài repo; `submission/`, `submissions/`, `node_modules/` và thông tin đăng nhập riêng không được commit.
