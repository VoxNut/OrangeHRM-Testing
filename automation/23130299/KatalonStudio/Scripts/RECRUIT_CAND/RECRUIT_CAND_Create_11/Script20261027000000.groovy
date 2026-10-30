/*
RECRUIT_CAND_Create_11 — Số điện thoại dạng chữ
Source: Group01_23130299_NguyenVanThang_Source_Aligned.xlsx / RECRUIT_CAND, row 19
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Phone='abcXYZ'; Email=run11@example.com
1. Add hồ sơ với tên và email hợp lệ, Phone=abcXYZ.
2. Save; kiểm tra trạng thái và hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Contact Number báo lỗi định dạng; Save không tạo Candidate khi nhập abcXYZ (Source_Aligned).
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_02
Negative; Phân vùng tương đương
Automation: P1
Source confirmed: app.js go allows digits and + - / ( ) spaces, maximum 25 characters; runtime message still verified by the test.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_11')
try {
    // Source_Aligned + app.js go validator now establish the frontend rule.
    // Keep the legacy profile flag unchanged; a source assertion is not a runtime Passed claim.
    assert !CandidateChecks.sourcePhoneAccepts('abcXYZ')
    Map vac = ui.vacancy()
    ui.invalidCreate(ui.draft(vac, '11', [phone:'abcXYZ']), vac, CandidateChecks.field('Contact Number'), CandidateChecks.PHONE_FORMAT_PATTERN)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
