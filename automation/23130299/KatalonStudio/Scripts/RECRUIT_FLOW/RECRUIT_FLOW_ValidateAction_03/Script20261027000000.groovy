/*
RECRUIT_FLOW_ValidateAction_03 — Không tái Shortlist sau khi Reject
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 27
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_REJECT_19 có trạng thái Rejected
1. Mở F_REJECT_19, quan sát tất cả nút.
2. Thử truy cập lại menu nếu có.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Không hiển thị thao tác Shortlist trái quy tắc trạng thái; status vẫn Rejected.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_04
Positive; Chuyển trạng thái
Automation: P1
Cần kiểm tra UI: Ghi nhận flow nếu phiên bản hỗ trợ Re-activate ứng viên.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ValidateAction_03')
try {
    Map d = ui.fixture(WorkflowChecks.REJECTED, '19')
    ui.forbiddenAction('Shortlist')
    ui.reopen(d); ui.stage(WorkflowChecks.REJECTED)
    ui.historySequence([WorkflowChecks.INITIAL, WorkflowChecks.REJECTED])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
