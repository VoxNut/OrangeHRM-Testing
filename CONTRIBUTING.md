# Quy trình làm việc của Group01

## 1. Tạo nhánh riêng từ dev

Sau khi nhóm trưởng đã push nhánh `dev` lên GitHub, thành viên thực hiện:

```powershell
git fetch origin
git switch dev
git pull --ff-only origin dev
git switch -c thang/asm6-leave
```

Tên trên chỉ là ví dụ, phải đổi theo thành viên và chức năng được giao. Nhựt đã có nhánh `nhat/asm6-pim`, không tạo lại.

## 2. Viết workbook cá nhân

- Tên: `Group01_MaSV_HoTen.xlsx`, họ tên không dấu, viết liền. Ví dụ `Group01_23130226_VoMinhNhut.xlsx`.
- Vị trí: `docs/test-cases/members/`.
- Mỗi chức năng có TestDesign và 20–30 test case với input, bước, expected output cụ thể.
- ID không trùng giữa các thành viên. Dùng prefix chức năng trong bảng phân công, ví dụ `TC_PIM_EMP_001`.
- Giữ tên cột của mẫu. Input và tiền điều kiện nằm trong `Test Case Procedure`.
- Chưa chạy: `Untested`. Không điền ngày chạy, actual hoặc minh chứng giả.
- Mỗi người chỉnh một file riêng. Không đồng thời chỉnh file tổng hợp `.xlsx`, vì Git không merge nội dung Excel như file văn bản.

## 3. Commit và push

Đóng workbook trước khi commit để bảo đảm nội dung đã lưu. Kiểm tra tên nhánh hiện tại.

```powershell
git branch --show-current
git status --short
git add -- docs/test-cases/members/Group01_23130226_VoMinhNhut.xlsx
git commit -m "docs: add PIM test design for 23130226"
git push -u origin nhat/asm6-pim
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
