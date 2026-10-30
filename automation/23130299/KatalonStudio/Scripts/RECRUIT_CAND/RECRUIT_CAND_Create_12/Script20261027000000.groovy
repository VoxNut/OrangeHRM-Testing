/*
RECRUIT_CAND_Create_12 — Số điện thoại hợp lệ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 20
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Phone=0900000012; Email=run12@example.com
1. Add hồ sơ với First/Last/Email; nhập Phone=0900000012.
2. Save, reload.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Contact Number giữ đúng '0900000012' và không mất số 0 đầu.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_02
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_12')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, '12', [phone:'0900000012']))
    ui.reopen(d); ui.equalsValue(CandidateChecks.field('Contact Number'), '0900000012')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
