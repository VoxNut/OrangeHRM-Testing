# Quản lý test case

- `members/`: mỗi người quản lý một workbook riêng, đúng tên `Group01_MaSV_HoTen.xlsx`.
- `merged/`: nhóm trưởng quản lý `Group01_OrangeHRM.xlsx`, tổng hợp từ các workbook cá nhân sau review; không coi bảng phân công là workbook đã gộp.
- 5 thành viên đã hoàn thành phần thiết kế theo xác nhận nhóm trưởng. Phân công đủ 15 chức năng tại [function-allocation.md](../planning/function-allocation.md). Kiểm tra riêng workbook đã được push và merge vào `dev` trước khi tổng hợp.
- Giữ ID chức năng để ghép nhanh. Không đánh lại số TC của thành viên khi tổng hợp.
- Mỗi chức năng giữ riêng sheet hoặc vùng dữ liệu rõ ràng. Report tổng hợp phải đếm từ cột Result thật, không nhập tay số pass/fail.
- Khi đổi số dòng, kiểm tra lại vùng COUNTIF/COUNTA và tổng báo cáo. Chưa chạy không có pass rate thực tế.

## Checklist tích hợp

1. Đối chiếu mỗi workbook với đúng 3 mã chức năng của người phụ trách.
2. Kiểm đếm 20–30 case/chức năng, đối chiếu TestDesign, input, expected, dependency và kế hoạch automation.
3. Thống nhất ID `Module_Action_XX`; số bắt đầu từ 01 trong từng hành động. Nếu đổi ID khi review, cập nhật đồng thời truy vết, script và bằng chứng liên quan.
4. Merge workbook qua PR vào `dev`, sau đó nhóm trưởng ghép vào tài liệu chung, giữ thông tin tác giả và ngày thực chạy.
5. Kiểm tra lại tên sheet, công thức, ID không trùng và số case thực tế. Không cộng tổng dự kiến 375 như số đã kiểm đếm.

ZIP Classroom không được theo dõi trong repo. Tài liệu nguồn nằm ở `members/`, bản tổng hợp ở `merged/`, minh chứng ở `evidence/`.
