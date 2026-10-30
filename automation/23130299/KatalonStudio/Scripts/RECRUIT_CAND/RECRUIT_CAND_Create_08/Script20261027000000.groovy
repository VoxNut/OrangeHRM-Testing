/*
RECRUIT_CAND_Create_08 — Email hợp lệ dạng example.com
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 16
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Email=quality.run08@example.com
1. Add ứng viên tên riêng hợp lệ với email hợp lệ.
2. Save và mở lại.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Email được lưu nguyên vẹn; có thể tìm đúng hồ sơ.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_02
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_08')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, '08', [email:'quality.' + ui.token + '@example.com']))
    ui.verifyListAndReopen(d, vac)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
