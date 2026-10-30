/*
RECRUIT_CAND_Create_06 — Không chọn Vacancy tùy chọn
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 14
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: First=An; Last=NguyenRUN06; Email=run06@example.com; Vacancy rỗng
1. Mở Add Candidate, điền First/Last/Email.
2. Để trống Vacancy; Save; mở hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Nếu demo cho phép vacancy tùy chọn: lưu ứng viên và hiển thị Vacancy rỗng; ghi nhận chính xác hành vi.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_01
Positive; Phân vùng tương đương
Automation: P1
Cần kiểm tra UI: Theo hướng dẫn OrangeHRM, Vacancy không đánh dấu bắt buộc; kiểm tra trên demo.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_06')
try {
    // This scenario deliberately leaves Vacancy blank; no disposable Vacancy is consumed.
    // The shared workbook precondition does not require creating a new unused vacancy per run.
    ui.list()
    Map vac = null
    Map d = ui.create(ui.draft(vac, '06', [vacancy:'', vacancyUrl:null]))
    ui.verifyListAndReopen(d, vac) // Optional vacancy must really persist blank; never substitute another vacancy.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
