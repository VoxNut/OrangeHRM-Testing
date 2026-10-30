/*
RECRUIT_FLOW_ScheduleInterview_06 — Thời gian sai định dạng
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 19
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_11; Time=25:99; Title hợp lệ; INT_A; Date hợp lệ
1. Mở Schedule Interview.
2. Nhập/paste giờ 25:99 vào Time; Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Không nhận thời gian ngoài miền hợp lệ; chặn nhập hoặc báo lỗi.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_02
Negative; Phân vùng tương đương / Giá trị biên
Automation: P1
Cần kiểm tra UI: Kiểm tra giao diện Time có cho nhập tự do hay chỉ chọn giờ.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_ScheduleInterview_06')
try {
    Map interview = ui.interviewData('11')
    Map d = ui.fixture(WorkflowChecks.SHORT, '11')
    ui.scheduleNegative(d, interview, 'invalidTime') // Exactly 25:99; no conversion to a valid time.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
