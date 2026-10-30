/*
RECRUIT_CAND_Create_04 — Bỏ trống Last Name
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 12
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: First=An; Last rỗng; Email=run04@example.com
1. Mở Add Candidate, nhập First/Email; Last để rỗng.
2. Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Last Name báo Required; hồ sơ không được tạo.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_01
Negative; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_04')
try {
    Map vac = ui.vacancy()
    ui.invalidCreate(ui.draft(vac, '04', [last:'']), vac, CandidateChecks.LAST, 'Required')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
