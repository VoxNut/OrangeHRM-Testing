/*
RECRUIT_FLOW_Shortlist_01 — Shortlist ứng viên mới
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 10
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_02 có status Application Initiated; Notes='Shortlist RUN'
1. Mở F_NEW_02.
2. Nhấn Shortlist; nhập Notes nếu có; xác nhận Save/Shortlist.
3. Reload.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Status chuyển Shortlisted; History ghi sự kiện shortlist đúng ứng viên.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_Shortlist_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '02')
    ui.transition(d, 'Shortlist', WorkflowChecks.SHORT, 'Shortlist ' + ui.token)
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.SHORT])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
