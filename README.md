# Group01 – OrangeHRM Testing

Tài liệu phân tích, thiết kế test case và kế hoạch automation cho môn Software Testing, Assignment 6.

- Website: https://opensource-demo.orangehrmlive.com/
- Mỗi thành viên tự chọn công cụ automation phù hợp; giảng viên không bắt buộc một công cụ chung.
- Phạm vi: kiểm thử chức năng trên dữ liệu giả do nhóm tạo. Không pentest, quét lỗ hổng hoặc load test demo công khai.

## Thành viên

| MSSV | Họ tên | Vai trò | Chức năng đã nhận |
| --- | --- | --- | --- |
| 23130226 | Võ Minh Nhựt | Nhóm trưởng | PIM_EMP, PIM_PERSONAL, PIM_JOB |
| 23130299 | Nguyễn Văn Thắng | Thành viên | Chưa chọn |
| 23130339 | Tôn Bích Trâm | Thành viên | Chưa chọn |
| 22130254 | Cao Tiến Thành | Thành viên | Chưa chọn |
| 23130171 | Nguyễn Thị Thủy Linh | Thành viên | Chưa chọn |

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

## Bắt đầu làm bài

1. Xem `docs/planning/function-allocation.md`, đăng ký đủ 3 chức năng không trùng người khác.
2. Đọc `docs/templates/README.md` và workbook mẫu của Nhựt trên nhánh `nhut` trước khi viết tài liệu riêng.
3. Tuần đầu hoàn thiện ít nhất 1 chức năng khoảng 20–30 test case. Tiếp tục đủ 3 chức năng theo tiến độ của nhóm.
4. Làm trên nhánh riêng, commit và push, chụp bằng chứng, tạo pull request vào `dev`. Xem `CONTRIBUTING.md`.
5. Nhóm trưởng review rồi gộp tài liệu đã thống nhất vào `docs/test-cases/merged/Group01_OrangeHRM.xlsx`.

## Nhánh

- `main`: bản nền và các mốc đã được nhóm thống nhất.
- `dev`: tích hợp phần của thành viên sau review.
- `nhut`: phần PIM của Võ Minh Nhựt.
- Nhánh thành viên đề xuất: `thang/asm6-<module>`, `tram/asm6-<module>`, `thanh/asm6-<module>`, `linh/asm6-<module>`; thay `<module>` bằng tên phần đã chọn.

Luồng nộp: **nhánh thành viên → pull request → dev**. Không sửa workbook của thành viên khác trên nhánh riêng. Chưa có automation execution thì để `Untested`; không dùng log hoặc ảnh mẫu làm minh chứng thực thi.

## Công cụ automation

Các thành viên không cần dùng cùng công cụ. Mỗi người ghi rõ công cụ, phiên bản, cách cài đặt/import, cách chạy và dữ liệu cần chuẩn bị trong phần của mình. Katalon Recorder trong kế hoạch PIM chỉ là lựa chọn của Nhựt, không phải quy định của cả nhóm.

Tài liệu test design dùng format chung và không phụ thuộc công cụ. Suite/script, assertion và báo cáo chạy phải tương ứng công cụ thực tế của người thực hiện. Lưu theo `automation/MaSV/TenCongCu/MaChucNang/`.
