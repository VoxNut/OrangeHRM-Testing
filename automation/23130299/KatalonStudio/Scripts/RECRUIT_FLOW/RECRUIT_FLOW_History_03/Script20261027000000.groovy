/*
RECRUIT_FLOW_History_03 — History sau Reject
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 32
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_REJECT_24 được reject từ Initiated
1. Mở F_REJECT_24, xem History.
2. Đối chiếu description, trạng thái và ghi chú.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Có event Reject của đúng ứng viên; trạng thái Rejected phù hợp event.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_History_03')
try {
    Map d = ui.fixture(WorkflowChecks.REJECTED, '24')
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.REJECTED])
    if (d.rejectNote) ui.verifyEventNote(d, WorkflowChecks.REJECTED, d.rejectNote.toString())
    else ui.blocked('Reject Notes/History detail capability needs review; expected note is not omitted.')
    ui.stage(WorkflowChecks.REJECTED)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
