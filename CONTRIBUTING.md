# Quy trình làm việc của Group01

## 1. Cập nhật nhánh riêng từ dev

Đã có các nhánh remote `nhut`, `NVThang` và `thanh`. Với nhánh cá nhân đã tồn tại, thành viên cập nhật như sau (ví dụ Thắng):

```powershell
git fetch origin
git switch NVThang
git pull --ff-only origin NVThang
git merge origin/dev
```

Đổi `NVThang` theo tên nhánh thực tế. Chỉ tạo nhánh mới từ `origin/dev` khi chưa có nhánh cá nhân; không tạo lại hoặc ghi đè nhánh của người khác. Nếu có conflict trong workbook Excel, phối hợp với người sở hữu file thay vì tự chọn một bản và bỏ thay đổi.

## 2. Viết workbook cá nhân

- Tên: `Group01_MaSV_HoTen.xlsx`, họ tên không dấu, viết liền. Ví dụ `Group01_23130226_VoMinhNhut.xlsx`.
- Vị trí: `docs/test-cases/members/`.
- Mỗi chức năng có TestDesign và 20–30 test case với input, bước, expected output cụ thể.
- ID không trùng giữa các thành viên. Dùng `Module_Action_XX`, ví dụ `PIM_EMP_Create_01`, `PIM_EMP_Search_01`, `USER_MGT_Login_01`. Đánh số từ 01 riêng cho từng hành động, theo yêu cầu của giảng viên.
- Giữ tên cột của mẫu. Input và tiền điều kiện nằm trong `Test Case Procedure`.
- Chưa chạy: `Untested`. Không điền ngày chạy, actual hoặc minh chứng giả.
- Mỗi thành viên tự chọn công cụ automation; ghi công cụ/phiên bản và hướng dẫn chạy trong tài liệu cá nhân. Không bắt buộc dùng Katalon Recorder.
- Mỗi người chỉnh một file riêng. Không đồng thời chỉnh file tổng hợp `.xlsx`, vì Git không merge nội dung Excel như file văn bản.
- Phần thiết kế của các thành viên đã hoàn thành theo xác nhận nhóm trưởng. Lần cập nhật tiếp theo tập trung review, chuẩn hóa và tích hợp theo `docs/planning/function-allocation.md`; không tự thay đổi phạm vi hoặc kết quả test của người khác.

## 3. Commit và push

Đóng workbook trước khi commit để bảo đảm nội dung đã lưu. Kiểm tra tên nhánh hiện tại.

```powershell
git branch --show-current
git status --short
git add -- docs/test-cases/members/Group01_23130226_VoMinhNhut.xlsx
git commit -m "docs: add PIM test design for 23130226"
git push -u origin nhut
```

Đổi tên workbook, message và nhánh theo người làm. Không dùng `git add .` nếu chưa kiểm tra toàn bộ file thay đổi.

## 4. Review và merge

1. Mở GitHub, tạo pull request với base `dev`, compare là nhánh cá nhân.
2. Mô tả 3 chức năng, số test case và những tiền điều kiện chưa được chuẩn bị.
3. Người review kiểm tra checklist dưới đây; chỉnh sửa trên nhánh cá nhân rồi push tiếp nếu cần.
4. Nhóm trưởng merge PR vào `dev`. Không force-push, không xóa nhánh của người khác.
5. Chụp màn hình push thành công kèm tên nhánh và commit, và trang PR đã merge vào `dev`. Có thể ghép hai ảnh thật thành một PNG minh chứng, giữ nội dung dễ đọc.

## Checklist review

- [ ] Đúng Group01, MSSV, họ tên, module và tên file.
- [ ] Đủ số chức năng và số case theo tiến độ tuần.
- [ ] Có positive, negative, giá trị biên và luồng liên quan khi phù hợp.
- [ ] Expected đo được, không dùng câu chung chung như “hoạt động đúng”.
- [ ] Input đủ để tái hiện; không giả định quy tắc nghiệp vụ chưa xác nhận.
- [ ] Case độc lập hoặc ghi rõ fixture/dependency.
- [ ] Mã TestDesign truy được sang test case.
- [ ] Có kế hoạch automation, assertion, setup và cleanup.
- [ ] Ghi rõ công cụ thực tế; có hướng dẫn chạy lại phù hợp công cụ đó.
- [ ] Kết quả, ngày, log và ảnh chỉ được ghi sau thực thi thật.
- [ ] Công thức tổng hợp và tên sheet tham chiếu không lỗi.

## 5. Nộp Classroom

Mỗi thành viên tự chuẩn bị:

```text
MaSV_HoTen.zip
├── Group01.txt
├── MaSV_HoTen.png
└── Group01_MaSV_HoTen.xlsx
```

`Group01.txt` chứa link repo chung public. PNG là minh chứng push/merge của chính thành viên. Không nén cả thư mục `.git`. Kiểm tra deadline thật trên Classroom; không dùng thời gian tạo file để thay bằng chứng push đúng hạn.

Chuẩn bị ZIP ngoài repo trên máy cá nhân. Không commit thư mục `submission/` hoặc `submissions/`; `.gitignore` đã bỏ qua hai thư mục này. Chỉ lưu workbook nguồn, tài liệu chung, script/test data và minh chứng cần thiết trên GitHub.

## 6. Assignment 7: script cá nhân

Mỗi thành viên triển khai automation cho 3 chức năng đã nhận bằng công cụ phù hợp; dùng ID truy vết được về workbook. File script mang tên `MaSV_HoTen_ASM7` kèm phần mở rộng của ngôn ngữ. Lưu phần của mình trong `automation/MaSV/TenCongCu/`, kèm cách cài đặt, chạy, fixture và cleanup. Push nhánh cá nhân và tạo PR vào `dev` như quy trình trên.

Chạy trên website online, giữ log và ảnh/video thật. Ghi rõ phạm vi đã triển khai; không coi một suite smoke là toàn bộ test design. Script của Nhựt dùng Playwright và 15 case đại diện cho ba chức năng PIM; xem `automation/23130226/Playwright/ASM7.md`.

Bài nộp Classroom ASM7 gồm gói script `.zip` khi có nhiều tệp và `MaSV_HoTen_ASM7.txt`. Dòng đầu TXT là link video quay màn hình trên YouTube, dòng thứ hai là link repo public có script. Kiểm tra video xem được bằng link và nhánh trong URL thực sự chứa script. Chỉ điền link sau khi tải lên thành công; không dùng URL mẫu. Gói Classroom và video lưu ngoài Git, script và bằng chứng thực thi lưu theo cấu trúc repo.
