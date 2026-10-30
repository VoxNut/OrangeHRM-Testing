/*
RECRUIT_FLOW_Status_02 — Trạng thái sau khi đặt lịch
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 26
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_18; Title='Interview RUN18'; INT_A; Date hợp lệ
1. Mở F_SHORT_18; Schedule Interview đầy đủ.
2. Save; trở lại chi tiết, kiểm tra trạng thái.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Status là Interview Scheduled; không còn status Shortlisted.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_Status_02')
try {
    Map interview = ui.interviewData('18')
    Map d = ui.fixture(WorkflowChecks.SHORT, '18')
    ui.schedule(d, interview)
    ui.reopen(d); ui.stage(WorkflowChecks.SCHEDULED)
    assert ui.status() != WorkflowChecks.SHORT
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
