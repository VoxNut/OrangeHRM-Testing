# Recruitment Vacancies — Katalon Studio 11.6

## 1. Giới thiệu

Dự án kiểm thử tự động chức năng **Recruitment → Vacancies** trên hệ thống OrangeHRM Demo, được thực hiện bởi **Nguyễn Văn Thắng – MSSV 23130299**, thuộc Group01.

Dự án sử dụng Katalon Studio 11.6.0, Groovy và Selenium WebDriver. Các kịch bản kiểm thử được chuyển đổi từ Katalon Recorder (`.side`) và điều chỉnh để thực thi bằng Katalon Studio.

Phạm vi gồm 25 test case, tương ứng với bộ test case `RECRUIT_VAC` trong tài liệu Excel của nhóm.

Các chức năng được kiểm thử:

- Tạo Vacancy và kiểm tra dữ liệu bắt buộc.
- Kiểm tra dữ liệu không hợp lệ.
- Cập nhật thông tin Vacancy.
- Tìm kiếm và lọc Vacancy.
- Đặt lại bộ lọc (Reset).
- Hủy thao tác tạo và cập nhật Vacancy.

## 2. Môi trường kiểm thử

| Thành phần | Thông tin |
|---|---|
| Website | https://opensource-demo.orangehrmlive.com |
| Công cụ | Katalon Studio 11.6.0 |
| Ngôn ngữ | Groovy |
| Framework | Selenium WebDriver |
| Trình duyệt | Google Chrome 155 |
| Hệ điều hành | Windows 11 |
| Số lượng test case | 25 |

## 3. Cấu trúc project

```text
KatalonStudio/
├── OrangeHRM_RECRUIT_VAC_KatalonStudio.prj
├── README.md
├── Scripts/
│   └── RECRUIT_VAC/
├── Test Cases/
│   └── RECRUIT_VAC/
├── Test Suites/
│   ├── RECRUIT_VAC_25_ASC_001_TO_025.ts
│   └── RECRUIT_VAC_25_ASC_001_TO_025.groovy
├── Keywords/
│   └── recruitment/
│       ├── RecorderBridge.groovy
│       └── VacancyChecks.groovy
```

Project chỉ giữ các thành phần chính phục vụ kiểm thử. Các thư mục cache, bytecode, log và báo cáo thực thi được lưu riêng hoặc do Katalon Studio tự tạo.

## 4. Hướng dẫn chạy

1. Mở Katalon Studio 11.6.0.
2. Chọn **File → Open Project**.
3. Mở file `OrangeHRM_RECRUIT_VAC_KatalonStudio.prj`.
4. Chọn **Profiles → default** và kiểm tra đường dẫn lưu screenshot.
5. Mở **Test Suites → RECRUIT_VAC_25_ASC_001_TO_025**.
6. Nhấn **Run → Chrome** để bắt đầu kiểm thử.
7. Khi Chrome hiển thị trang đăng nhập OrangeHRM, đăng nhập thủ công trong thời gian tối đa 120 giây theo hướng dẫn trên Console.
8. Theo dõi kết quả tại **Log Viewer** và **Reports**.

Có thể chạy riêng từng test case trong `Test Cases/RECRUIT_VAC` để kiểm tra hoặc phân tích lỗi.

**Lưu ý:** Việc chạy suite có thể tạo dữ liệu Vacancy mới trên OrangeHRM Demo. Không xóa dữ liệu của người dùng khác và không lưu tài khoản hoặc mật khẩu trong mã nguồn.

## 5. Kết quả kiểm thử

Kết quả được sử dụng để báo cáo là lần chạy `20261023_115454`, ngày 23/10/2026.

| Kết quả | Số lượng |
|---|---:|
| Total | 25 |
| Passed | 23 |
| Error | 2 |
| Pass Rate | 92% |

Hai test case gặp lỗi:

- `RECRUIT_VAC_Update_02`
- `RECRUIT_VAC_Update_03`

Cả hai gặp lỗi timeout khi automation chờ thông báo `Successfully Updated` sau thao tác Save. Do chưa hoàn tất bước kiểm tra dữ liệu sau khi mở lại Vacancy, không có đủ bằng chứng để kết luận chức năng cập nhật của OrangeHRM bị lỗi.

**Lưu ý về phiên bản:** Kết quả trên thuộc lần chạy `20261023_115454`. Mã nguồn Katalon có các chỉnh sửa tiếp theo, vì vậy không được xem kết quả này là bằng chứng phiên bản mã nguồn mới nhất đã đạt 23/25 nếu chưa chạy xác minh lại.

## 6. Báo cáo và bằng chứng

Báo cáo thực thi được lưu trong repository:

```text
evidence/executions/23130299/
└── RECRUIT_VAC_20261023_01/
    ├── report.html
    └── screenshots/
```

Thư mục `screenshots` chứa ảnh của 25 test case thuộc lần chạy được chọn. Mỗi case được tổ chức theo mã test case và timestamp.

Ví dụ:

```text
screenshots/
└── RECRUIT_VAC_Create_01/
    └── 20261023_115501_970/
        ├── RECRUIT_VAC_Create_01_01_BEFORE_ADD.png
        ├── RECRUIT_VAC_Create_01_02_BEFORE_SAVE.png
        └── RECRUIT_VAC_Create_01_RESULT.png
```

Đường dẫn screenshot được cấu hình bằng biến `evidenceDir` trong **Profiles → default**.

Báo cáo và screenshot phục vụ việc đối chiếu Expected Result, Actual Result và trạng thái của từng test case.

## 7. Tài liệu liên quan

File thiết kế test case:

```text
docs/test-cases/members/
└── Group01_23130299_NguyenVanThang.xlsx
```

Kịch bản Katalon Recorder gốc được lưu riêng trong khu vực:

```text
automation/23130299/KatalonRecorder/RECRUIT_VAC/
```

Không cần import lại file `.side` khi chạy project Katalon Studio.

## 8. Lưu ý

- Chạy đúng suite `RECRUIT_VAC_25_ASC_001_TO_025`.
- Sử dụng Chrome và profile `default`.
- Các test case có thể bị ảnh hưởng bởi dữ liệu thay đổi trên OrangeHRM Demo.
- Khi xảy ra lỗi, kiểm tra Console, screenshot và báo cáo Katalon trước khi xác định nguyên nhân.
- Không tự thay đổi Expected Result hoặc đánh dấu Passed khi assertion chưa đạt.
- Kết quả thực thi phải được đối chiếu với đúng timestamp của báo cáo.

---

**Thực hiện:** Nguyễn Văn Thắng – 23130299  
**Nhóm:** Group01  
**Chức năng:** Recruitment → Vacancies  
**Công cụ:** Katalon Studio 11.6.0  
**Báo cáo tham chiếu:** `20261023_115454`  
**Ngày thực hiện:** 23/10/2026