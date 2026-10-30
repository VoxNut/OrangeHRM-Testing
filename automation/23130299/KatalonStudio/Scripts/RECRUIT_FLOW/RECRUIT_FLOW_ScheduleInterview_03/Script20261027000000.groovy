/*
RECRUIT_FLOW_ScheduleInterview_03 — Bỏ trống Interviewer
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 16
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_08; Title='Interview RUN08'; Interviewer rỗng; Date=2026-11-10
1. Mở Schedule Interview; điền Title và Date.
2. Không chọn Interviewer; Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Interviewer hiển thị Required; không tạo lịch.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_03')
try {
    Map interview = ui.interviewData('08', [interviewers:[]])
    Map d = ui.fixture(WorkflowChecks.SHORT, '08')
    ui.scheduleNegative(d, interview, 'interviewer')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
