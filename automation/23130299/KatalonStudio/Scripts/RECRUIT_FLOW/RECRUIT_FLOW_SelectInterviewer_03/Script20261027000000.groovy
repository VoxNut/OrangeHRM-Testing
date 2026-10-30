/*
RECRUIT_FLOW_SelectInterviewer_03 — Không chọn interviewer đã xóa
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 23
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_15; gõ tên 'DELETED_INTERVIEWER_RUN' không tồn tại
1. Mở Schedule Interview.
2. Gõ tên không tồn tại vào Interviewer, không chọn suggestion.
3. Điền Title/Date, Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Không chấp nhận một chuỗi không khớp nhân viên còn hiệu lực; báo Required/Invalid.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_03
Negative; Phân vùng tương đương / Autocomplete
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_SelectInterviewer_03')
try {
    Map interview = ui.interviewData('15', [interviewers:[]])
    Map d = ui.fixture(WorkflowChecks.SHORT, '15')
    ui.scheduleNegative(d, interview, 'unknownInterviewer')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
