/*
RECRUIT_FLOW_Status_01 — Kiểm tra trạng thái ứng viên mới
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 9
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_01; Vacancy=VAC_A; vừa tạo chưa thao tác
1. Tạo F_NEW_01 từ giao diện Candidates.
2. Mở chi tiết và Candidate History.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Trạng thái đầu vào là Application Initiated hoặc nhãn khởi tạo tương đương; chưa có sự kiện Shortlist/Reject.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_01
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_Status_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '01')
    ui.reopen(d); ui.stage(WorkflowChecks.INITIAL); ui.historySequence([WorkflowChecks.INITIAL])
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
