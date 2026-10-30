/*
RECRUIT_FLOW_History_01 — History sau khi tạo ứng viên
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 30
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_22 mới được tạo
1. Mở F_NEW_22.
2. Cuộn đến Candidate History, đọc các dòng.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Có dữ liệu sự kiện khởi tạo/đính vacancy theo cơ chế bản demo; không có event interview/reject.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_History_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '22')
    ui.reopen(d); ui.historySequence([WorkflowChecks.INITIAL]); ui.stage(WorkflowChecks.INITIAL)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
