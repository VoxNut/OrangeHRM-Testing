/*
RECRUIT_CAND_VerifyDetails_01 — Đối chiếu dữ liệu hồ sơ sau tải lại
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 33
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: CAND_EDIT; First, Last, Email, Contact, Vacancy theo baseline
1. Mở CAND_EDIT; ghi 5 trường baseline.
2. Tải lại; đối chiếu từng trường và danh sách.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Chi tiết và dòng danh sách nhất quán; không mất dữ liệu khi reload.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_05
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_VerifyDetails_01')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, 'DETAIL', [phone:'0900000012']))
    ui.reopen(d); ui.verifyDetails(d); ui.verifyListAndReopen(d, vac)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
