/*
RECRUIT_FLOW_ScheduleInterview_01 — Đặt lịch phỏng vấn với dữ liệu đầy đủ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 14
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_06; Title='Interview RUN06'; Interviewer=INT_A; Date=2026-11-10; Time=10:30; Notes='First round'
1. Mở F_SHORT_06 Shortlisted; nhấn Schedule Interview.
2. Điền các trường chính xác và chọn INT_A từ gợi ý.
3. Save; reload Candidate Details.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Chuyển sang Interview Scheduled; tiêu đề, người phỏng vấn, ngày 2026-11-10, giờ 10:30 và ghi chú đã lưu.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_02
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_01')
try {
    Map interview = ui.interviewData('06')
    Map d = ui.fixture(WorkflowChecks.SHORT, '06')
    ui.schedule(d, interview)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
