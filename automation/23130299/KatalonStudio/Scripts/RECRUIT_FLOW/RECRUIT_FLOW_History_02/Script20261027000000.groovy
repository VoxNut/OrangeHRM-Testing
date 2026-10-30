/*
RECRUIT_FLOW_History_02 — History sau Shortlist
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 31
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_23 đã chuyển từ Initiated bằng setup
1. Mở F_SHORT_23, xem Candidate History.
2. Mở View/chi tiết event nếu có.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Có event Shortlist gắn với đúng ứng viên và trình tự sau khi tạo.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_History_02')
try {
    Map d = ui.fixture(WorkflowChecks.SHORT, '23')
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.SHORT])
    if (d.shortlistNote) ui.verifyEventNote(d, WorkflowChecks.SHORT, d.shortlistNote.toString())
    else ui.blocked('Shortlist event details/optional Notes capability needs review; history chain was checked.')
    ui.stage(WorkflowChecks.SHORT)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
