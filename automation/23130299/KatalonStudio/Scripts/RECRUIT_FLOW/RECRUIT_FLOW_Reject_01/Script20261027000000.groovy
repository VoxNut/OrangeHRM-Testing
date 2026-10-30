/*
RECRUIT_FLOW_Reject_01 — Reject ứng viên từ trạng thái ban đầu
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 11
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_03 Application Initiated; Notes='Reject initial RUN'
1. Mở F_NEW_03.
2. Nhấn Reject; xác nhận trong hộp thoại.
3. Reload.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Status chuyển Rejected; Candidate History có sự kiện reject.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_Reject_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '03')
    ui.transition(d, 'Reject', WorkflowChecks.REJECTED, 'Reject initial ' + ui.token)
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.REJECTED])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
