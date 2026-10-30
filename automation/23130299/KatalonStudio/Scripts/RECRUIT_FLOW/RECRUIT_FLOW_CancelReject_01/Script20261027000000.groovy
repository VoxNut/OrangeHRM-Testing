/*
RECRUIT_FLOW_CancelReject_01 — Hủy Reject không đổi trạng thái
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 28
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_20 Shortlisted
1. Mở F_SHORT_20; nhấn Reject.
2. Nhấn Cancel/Back trong dialog.
3. Reload chi tiết.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Vẫn Shortlisted; không thêm bản ghi Reject vào History.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_CancelReject_01')
try {
    Map d = ui.fixture(WorkflowChecks.SHORT, '20')
    ui.cancelTransition(d, 'Reject', WorkflowChecks.SHORT, 'Không được lưu')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
