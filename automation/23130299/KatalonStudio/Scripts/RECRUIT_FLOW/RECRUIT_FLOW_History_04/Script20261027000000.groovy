/*
RECRUIT_FLOW_History_04 — History sau Schedule Interview
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 33
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SCHED_25 đã được đặt lịch Title='Interview RUN25'
1. Mở F_SCHED_25 và Candidate History.
2. Mở event phỏng vấn; đối chiếu Title/INT_A/Date.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Có event Interview Scheduled; chi tiết trùng dữ liệu lịch đã lưu và đúng candidate.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_05
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_History_04')
try {
    ui.interviewData('25') // Preflight before creating fixtures.
    Map d = ui.fixture(WorkflowChecks.SCHEDULED, '25')
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.SHORT, WorkflowChecks.SCHEDULED])
    ui.verifyInterview(d, d.interview)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
