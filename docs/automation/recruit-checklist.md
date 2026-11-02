# Recruitment – Kế hoạch automation

[Workbook](../test-cases/members/Group01_23130299_NguyenVanThang.xlsx): RECRUIT_VAC, RECRUIT_CAND và RECRUIT_FLOW, 25 case/chức năng.

1. Chuẩn bị quyền Recruitment, Job Title, Hiring Manager và fixture theo MSSV/timestamp.
2. Chạy smoke rồi suite; đợi dropdown, autocomplete và kết quả danh sách cập nhật.
3. Assert dữ liệu sau mở lại; kiểm tra validation, filter, Reset và Cancel.
4. Lưu report/ảnh theo Case ID và cùng timestamp.
5. Dọn đúng Vacancy đã tạo, push và tạo PR vào `dev`.

`dev` có đủ ba suite VAC/CAND/FLOW, mỗi suite 25 case. VAC có HTML run `20261023_115454`: 23 Passed, 2 Error. CAND/FLOW có screenshot ngày 30/10/2026 nhưng chưa có report tổng hợp trong Git để xác nhận số Pass/Fail.

ASM8 bỏ qua Recruitment theo yêu cầu nhóm trưởng. Khi chạy lại cần bổ sung profile, `maintenance/preflight58-plan.json`, tài nguyên worker và cấu hình đường dẫn evidence của project.

[Cài đặt và chạy](../../automation/23130299/KatalonStudio/README.md)
