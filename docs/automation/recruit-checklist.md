# Recruitment – Kế hoạch automation

[Workbook](../test-cases/members/Group01_23130299_NguyenVanThang.xlsx): RECRUIT_VAC, RECRUIT_CAND và RECRUIT_FLOW, 25 case/chức năng.

1. Chuẩn bị quyền Recruitment, Job Title, Hiring Manager và fixture theo MSSV/timestamp.
2. Chạy smoke rồi suite; đợi dropdown, autocomplete và kết quả danh sách cập nhật.
3. Assert dữ liệu sau mở lại; kiểm tra validation, filter, Reset và Cancel.
4. Lưu report/ảnh theo Case ID và cùng timestamp.
5. Dọn đúng Vacancy đã tạo, push và tạo PR vào `dev`.

Run `20261023_115454`: RECRUIT_VAC **23 Passed, 2 Error**; Update_02/03 timeout chờ toast, chưa xác nhận persistence. RECRUIT_CAND/FLOW chưa có execution được xác minh. Mã sửa sau run cần chạy lại để có kết quả mới.

[Cài đặt và chạy](../../automation/23130299/KatalonStudio/README.md)
