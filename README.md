# Group01 – OrangeHRM Testing

Tài liệu phân tích, thiết kế test case và script automation cho môn Software Testing, Assignment 6–7.

- Website: https://opensource-demo.orangehrmlive.com/
- Mỗi thành viên tự chọn công cụ automation phù hợp; giảng viên không bắt buộc một công cụ chung.
- Phạm vi: kiểm thử chức năng trên dữ liệu giả do nhóm tạo. Không pentest, quét lỗ hổng hoặc load test demo công khai.

## Thành viên

| MSSV | Họ tên | Vai trò | Chức năng đã nhận |
| --- | --- | --- | --- |
| 23130226 | Võ Minh Nhựt | Nhóm trưởng | PIM_EMP, PIM_PERSONAL, PIM_JOB |
| 23130299 | Nguyễn Văn Thắng | Thành viên | RECR_VAC, RECR_CAND, RECR_FLOW |
| 23130339 | Tôn Bích Trâm | Thành viên | TIME_ATTEND, TIME_MY, TIME_EMP |
| 22130254 | Cao Tiến Thành | Thành viên | LEAVE_CONFIG, LEAVE_SELF, LEAVE_ADMIN |
| 23130171 | Nguyễn Thị Thủy Linh | Thành viên | ADM_STRUCT, ADM_JOB, ADM_USER |

Nhóm trưởng xác nhận các thành viên đã hoàn thành phần thiết kế cho các chức năng được giao. Mỗi người phụ trách 3 chức năng, khoảng 20–30 test case mỗi chức năng. Phân bổ thiết kế là 25 case/chức năng, tổng dự kiến 375 case; số case thực tế lấy từ workbook đã review. Xem [phạm vi và phân công chi tiết](docs/planning/function-allocation.md).

Hoàn thành thiết kế không đồng nghĩa automation đã chạy hoặc mọi workbook đã được merge. Chỉ ghi kết quả thực thi khi có log/ảnh tương ứng; không tự chuyển các case Untested sang Passed.

## Cấu trúc

```text
OrangeHRM-Testing/
├── Group01.txt
├── README.md
├── CONTRIBUTING.md
├── docs/
│   ├── planning/function-allocation.md
│   ├── templates/README.md
│   ├── test-cases/
│   │   ├── README.md
│   │   ├── members/                  # Workbook riêng từng thành viên
│   │   └── merged/                   # Workbook tổng hợp do nhóm trưởng quản lý
│   └── automation/README.md
├── automation/                       # Chia theo MSSV/công cụ/chức năng khi triển khai
├── test-data/                        # Dữ liệu giả và hướng dẫn fixture
└── evidence/
    ├── git/                         # Ảnh push nhánh riêng và merge dev
    └── executions/                  # Log và screenshot test thực tế
```

## Rà soát và tích hợp tài liệu

1. Đối chiếu 3 chức năng của mỗi thành viên với `docs/planning/function-allocation.md`; kiểm tra không trùng phạm vi.
2. Rà soát workbook theo `docs/templates/README.md`: ID, TestDesign, bước/input/expected, fixture và kế hoạch automation.
3. Push bản hoàn chỉnh lên nhánh cá nhân, lưu bằng chứng và tạo pull request vào `dev`. Xem `CONTRIBUTING.md`.
4. Nhóm trưởng review, merge rồi tổng hợp vào `docs/test-cases/merged/Group01_OrangeHRM.xlsx`; kiểm tra công thức và truy vết sau khi ghép.
5. Ghi nhận riêng tiến độ thiết kế, tích hợp tài liệu và thực thi automation. Cập nhật Result theo bằng chứng của từng case.

ZIP nộp Classroom được chuẩn bị trên máy cá nhân, không lưu trong repo. Các thư mục `submission/` và `submissions/` được bỏ qua bởi Git; tài liệu nguồn và minh chứng vẫn lưu theo cấu trúc trên.

## Nhánh

- `main`: bản nền và các mốc đã được nhóm thống nhất.
- `dev`: tích hợp phần của thành viên sau review.
- `nhut`: phần PIM của Võ Minh Nhựt.
- Các nhánh cá nhân hiện có trên remote: `nhut`, `NVThang`, `thanh`.
- Trâm và Linh dùng nhánh cá nhân của mình; nếu chưa tạo, có thể dùng `tram` và `linh`. Kiểm tra tên nhánh thực tế trước khi push, không tạo lại nhánh đã có.

Luồng nộp: **nhánh thành viên → pull request → dev**. Không sửa workbook của thành viên khác trên nhánh riêng. Chưa có automation execution thì để `Untested`; không dùng log hoặc ảnh mẫu làm minh chứng thực thi.

## Công cụ automation

Các thành viên không cần dùng cùng công cụ. Mỗi người ghi rõ công cụ, phiên bản, cách cài đặt/import, cách chạy và dữ liệu cần chuẩn bị trong phần của mình. Phần PIM của Nhựt dùng Playwright, không phải quy định công cụ của cả nhóm.

Tài liệu test design dùng format chung và không phụ thuộc công cụ. Suite/script, assertion và báo cáo chạy phải tương ứng công cụ thực tế của người thực hiện. Lưu theo `automation/MaSV/TenCongCu/MaChucNang/`.

## Assignment 7 — phần cá nhân Võ Minh Nhựt

[23130226_VoMinhNhut_ASM7.mjs](automation/23130226/Playwright/23130226_VoMinhNhut_ASM7.mjs) triển khai 15 case đại diện cho PIM_EMP, PIM_PERSONAL và PIM_JOB, gồm positive, negative, lưu/mở lại hồ sơ, attachment và hợp đồng. Chạy bằng `npm run asm7`; xem [hướng dẫn và bảng truy vết](automation/23130226/Playwright/ASM7.md).

Lần chạy ngày 24/10/2026: **15 Passed, 0 Failed, 0 Blocked**, cleanup đã xác nhận không còn hồ sơ test. [Report và 19 screenshot thật](evidence/executions/23130226/ASM7_2026-10-24T02-46-28-365Z/README.md). Kết quả này chỉ áp dụng cho 15 case đã chạy, không thay kết quả ASM6 hoặc kết quả của thành viên khác.
