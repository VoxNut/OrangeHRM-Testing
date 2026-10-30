/*
RECRUIT_FLOW_SelectInterviewer_01 — Chọn interviewer qua autocomplete
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 21
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_13; INT_A nhân viên còn hiệu lực
1. Mở Schedule Interview.
2. Gõ tiền tố tên INT_A; chọn đúng suggestion.
3. Điền Title/Date, Save; mở lại.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Interviewer là INT_A, không lưu tên gõ tự do.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_03
Positive; Phân vùng tương đương / Autocomplete
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_SelectInterviewer_01')
try {
    Map interview = ui.interviewData('13')
    Map d = ui.fixture(WorkflowChecks.SHORT, '13')
    ui.schedule(d, interview) // Prefix -> exact suggestion -> persisted interviewer chip.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
