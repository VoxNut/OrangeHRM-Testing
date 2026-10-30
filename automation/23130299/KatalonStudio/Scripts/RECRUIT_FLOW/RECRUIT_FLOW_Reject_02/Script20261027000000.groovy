/*
RECRUIT_FLOW_Reject_02 — Reject ứng viên đã Shortlisted
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 12
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_04 đang Shortlisted; Notes='Reject shortlisted RUN'
1. Mở F_SHORT_04.
2. Nhấn Reject; xác nhận.
3. Reload và xem History.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Trạng thái Rejected; lịch sử còn sự kiện shortlist cũ và thêm reject.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_01
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_Reject_02')
try {
    Map d = ui.fixture(WorkflowChecks.SHORT, '04')
    ui.transition(d, 'Reject', WorkflowChecks.REJECTED, 'Reject shortlisted ' + ui.token)
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.SHORT, WorkflowChecks.REJECTED])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
