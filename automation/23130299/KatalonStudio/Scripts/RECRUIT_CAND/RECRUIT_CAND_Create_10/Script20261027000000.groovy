/*
RECRUIT_CAND_Create_10 — Email thiếu phần domain
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 18
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Email=quality@
1. Add ứng viên với First/Last hợp lệ; nhập Email=quality@.
2. Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Email báo invalid; không tạo hồ sơ.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_02
Negative; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_10')
try {
    Map vac = ui.vacancy()
    ui.invalidCreate(ui.draft(vac, '10', [email:'quality@']), vac, CandidateChecks.field('Email'), CandidateChecks.EMAIL_FORMAT_PATTERN)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
