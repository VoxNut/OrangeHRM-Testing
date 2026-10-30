/*
RECRUIT_FLOW_ScheduleInterview_07 — Lưu ghi chú phỏng vấn
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 20
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_12; Title='Interview RUN12'; INT_A; Date=2026-11-12; Time=14:00; Notes='Đánh giá kỹ năng QA'
1. Mở Schedule Interview, nhập dữ liệu đầy đủ.
2. Nhập Notes có dấu; Save.
3. Mở chi tiết/history của lịch.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Ghi chú hiển thị chính xác, không lỗi dấu; lịch được tạo đúng candidate.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_02
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_07')
try {
    Map interview = ui.interviewData('12', [notes:'Đánh giá kỹ năng QA', time:'14:00'])
    Map d = ui.fixture(WorkflowChecks.SHORT, '12')
    ui.schedule(d, interview) // Persisted Unicode note is asserted in event details.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
