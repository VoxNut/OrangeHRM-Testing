/*
RECRUIT_FLOW_ValidateAction_02 — Schedule Interview xuất hiện sau Shortlist
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_FLOW, row 25
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_SHORT_17 Shortlisted
1. Mở F_SHORT_17.
2. Quan sát menu hành động.
3. Nhấn Schedule Interview và kiểm tra form.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Nút Schedule Interview khả dụng; mở được form lịch của đúng candidate.
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

def ui = new WorkflowChecks('RECRUIT_FLOW_ValidateAction_02')
try {
    Map d = ui.fixture(WorkflowChecks.SHORT, '17')
    ui.scheduleForm(d)
    ui.one(WorkflowChecks.field('Interview Title'))
    ui.one(WorkflowChecks.field('Interviewer'))
    ui.buttonClick('Cancel')
    ui.reopen(d); ui.stage(WorkflowChecks.SHORT); ui.verifyDetails(d)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
