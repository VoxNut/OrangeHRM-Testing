/*
RECRUIT_FLOW_ValidateAction_01 — Schedule Interview không có ở trạng thái mới
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 24
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_16 Application Initiated
1. Mở chi tiết F_NEW_16 chưa Shortlist.
2. Kiểm tra nút hành động và menu.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Không có thao tác Schedule Interview hợp lệ từ trạng thái đầu vào; phải Shortlist trước.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_04
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ValidateAction_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '16')
    ui.forbiddenAction('Schedule Interview')
    ui.reopen(d); ui.stage(WorkflowChecks.INITIAL); ui.historySequence([WorkflowChecks.INITIAL])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
