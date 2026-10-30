/*
RECRUIT_FLOW_SelectInterviewer_02 — Chọn nhiều interviewer
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 22
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_14; INT_A và INT_B khác người
1. Mở Schedule Interview.
2. Chọn INT_A, tiếp tục chọn INT_B qua suggestion.
3. Điền Title/Date; Save; mở lại.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Lưu đúng cả hai interviewer, không trùng; hiển thị đúng trên chi tiết.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_03
Positive; Phân vùng tương đương
Automation: P1
Cần kiểm tra UI: Chỉ chạy nếu bản demo hỗ trợ multi-select; nếu không, ghi Blocked và phiên bản.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_SelectInterviewer_02')
try {
    Map interview = ui.interviewData('14')
    String second = WorkflowChecks.config('interviewerB', '').toString().trim()
    if (!second || second == interview.interviewers[0]) ui.blocked('Configure two distinct active exact interviewer names. Multi-select capability must be verified live.')
    interview.interviewers = [interview.interviewers[0], second]
    Map d = ui.fixture(WorkflowChecks.SHORT, '14')
    ui.schedule(d, interview)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
