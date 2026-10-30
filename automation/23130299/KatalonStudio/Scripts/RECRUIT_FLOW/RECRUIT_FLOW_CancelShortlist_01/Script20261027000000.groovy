/*
RECRUIT_FLOW_CancelShortlist_01 — Hủy hộp thoại Shortlist
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 13
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_05 Application Initiated; Notes='Không được lưu'
1. Mở F_NEW_05; nhấn Shortlist.
2. Điền Notes nếu có; nhấn Cancel/Back thay vì xác nhận.
3. Reload.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Status vẫn Application Initiated; History không có sự kiện Shortlist mới.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_01
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_CancelShortlist_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '05')
    ui.cancelTransition(d, 'Shortlist', WorkflowChecks.INITIAL, 'Không được lưu')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
