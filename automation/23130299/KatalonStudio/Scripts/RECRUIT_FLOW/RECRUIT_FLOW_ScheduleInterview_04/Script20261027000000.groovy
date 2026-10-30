/*
RECRUIT_FLOW_ScheduleInterview_04 — Bỏ trống ngày phỏng vấn
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 17
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_09; Title='Interview RUN09'; INT_A; Date rỗng
1. Mở Schedule Interview; điền Title và Interviewer.
2. Xóa Date, Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Date hiển thị Required; không lưu lịch.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_02
Negative; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_04')
try {
    Map interview = ui.interviewData('09', [date:''])
    Map d = ui.fixture(WorkflowChecks.SHORT, '09')
    ui.scheduleNegative(d, interview, 'date')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
