/*
RECRUIT_FLOW_ScheduleInterview_05 — Ngày phỏng vấn không tồn tại
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 18
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_10; Date=31/02/2027; Title hợp lệ; INT_A
1. Mở Schedule Interview.
2. Nhập ngày 31/02/2027 theo định dạng UI (nếu datepicker chặn, ghi nhận).
3. Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Ngày không hợp lệ bị chặn hoặc báo lỗi; không tạo lịch với ngày này.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_02
Negative; Phân vùng tương đương / Giá trị biên
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_05')
try {
    Map interview = ui.interviewData('10')
    Map d = ui.fixture(WorkflowChecks.SHORT, '10')
    ui.scheduleNegative(d, interview, 'invalidDate') // Literal impossible 31/02/2027 is retained in helper.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
