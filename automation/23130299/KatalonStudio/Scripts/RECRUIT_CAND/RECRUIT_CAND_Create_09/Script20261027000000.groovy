/*
RECRUIT_CAND_Create_09 — Email thiếu ký tự @
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 17
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Email=quality.example.com
1. Add ứng viên với First/Last hợp lệ; nhập Email thiếu @.
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

def ui = new CandidateChecks('RECRUIT_CAND_Create_09')
try {
    Map vac = ui.vacancy()
    ui.invalidCreate(ui.draft(vac, '09', [email:'quality.example.com']), vac, CandidateChecks.field('Email'), CandidateChecks.EMAIL_FORMAT_PATTERN)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
