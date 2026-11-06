Script: `23130339_TonBichTram_ASM7.js`. Puppeteer/Chrome; tài khoản demo `Admin/admin123`.

```powershell
npm install
node automation/23130339/23130339_TonBichTram_ASM7.js

Cần Node.js 18+ và Google Chrome.

Case triển khai
ID,Kiểm tra
TIME_ATTEND_PunchIn_01,"Kiểm tra nhập giờ Punch In hợp lệ, ghi nhận giờ vào ca và hiển thị toast ""Successfully Saved"""
TIME_ATTEND_PunchOut_01,"Kiểm tra nhập giờ Punch Out hợp lệ, đối chiếu xác nhận kết thúc ca làm việc"
TIME_MY_Edit_01,"Mở My Timesheet, chọn Project & Activity và nhập 8 giờ làm việc cho các ngày trong tuần"
TIME_MY_Submit_01,Gửi nộp (Submit) bảng chấm công cá nhân lên hệ thống và kiểm tra assertion
TIME_EMP_Search_01,Tìm kiếm bảng công của nhân viên theo tên từ gợi ý autocomplete
TIME_EMP_Approve_01,Quản trị viên thực hiện phê duyệt (Approve) bảng chấm công nhân viên và reset dọn dẹp

Kịch bản End-to-End chạy tuần tự qua 3 chức năng chính của module Time:

TIME_ATTEND: Thực hiện chu trình chấm công Punch In -> Punch Out và dọn dẹp bản ghi trong lịch sử điểm danh.

TIME_MY: Tạo, lưu, gửi duyệt và hoàn trả trạng thái ban đầu của bảng công cá nhân.

TIME_EMP: Tìm kiếm hồ sơ nhân viên, kiểm tra bảng công và thực hiện thao tác duyệt (Approve).