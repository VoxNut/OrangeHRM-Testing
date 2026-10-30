/*
RECRUIT_FLOW_VerifyDetails_01 — Candidate Details chứa đúng bản ghi
Source: Group01_23130299_NguyenVanThang_Source_Aligned.xlsx / RECRUIT_FLOW, row 29
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; VAC_A và INTERVIEWER_A/B hiệu lực; ứng viên độc lập đúng trạng thái đầu vào.
Input: F_NEW_21; name/email/vacancy cố định theo Test Data
1. Vào Candidates; chọn đúng Candidate Name của F_NEW_21 và Search; đối chiếu numeric ID.
2. Mở chi tiết, đối chiếu name/email/vacancy.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Tên, email, vacancy và trạng thái đúng một ứng viên; không hiển thị fixture khác.
Dependence: Không phụ thuộc test khác; fixture đúng trạng thái được tạo trong setup.
Riêng flow, không dùng candidate đã Rejected cho nhánh Shortlisted.
Design note: TD_RECRUIT_FLOW_05
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.WorkflowChecks
import recruitment.ResumeFiles

def ui = new WorkflowChecks('RECRUIT_FLOW_VerifyDetails_01')
try {
    Map d = ui.fixture(WorkflowChecks.INITIAL, '21')
    ui.verifyListAndReopen(d, d.fixtureVacancy)
    ui.verifyDetails(d); ui.stage(WorkflowChecks.INITIAL)
    ui.verifyDetails(d) // Source_Aligned: Email is verified inside the exact owned Profile, not a list filter.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
