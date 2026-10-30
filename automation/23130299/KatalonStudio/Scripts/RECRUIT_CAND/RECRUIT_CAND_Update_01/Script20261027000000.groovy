/*
RECRUIT_CAND_Update_01 — Sửa Keywords và Notes hồ sơ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 31
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: CAND_EDIT; Keywords=playwright,regression; Notes='Updated RUN'
1. Mở chi tiết CAND_EDIT; ghi baseline.
2. Edit, đổi Keywords/Notes; Save; reload.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Keywords và Notes đúng giá trị mới; email và vacancy không đổi.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_05
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Update_01')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, 'EDIT', [phone:'0900000012', keywords:'baseline', notes:'Original ' + ui.token]))
    ui.updateNotes(d)
    ui.verifyListAndReopen(d, vac)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
