/*
RECRUIT_CAND_Create_07 — Kiểm tra consent khi tạo hồ sơ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 15
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: First=An; Last=NguyenRUN07; Email=run07@example.com; Consent=On
1. Mở Add Candidate, nhập thông tin bắt buộc.
2. Bật checkbox consent (nếu có).
3. Save và mở lại hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Lưu thành công; trạng thái consent đúng tùy chọn hoặc hệ thống có ghi nhận chọn đồng ý.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_01
Positive; Phân vùng tương đương
Automation: P1
Cần kiểm tra UI: Demo có thể chỉ hiển thị consent ở form riêng; điều chỉnh nếu UI khác.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_07')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, '07', [consent:true]))
    ui.reopen(d); ui.verifyDetails(d) // Missing consent UI is a blocker, never an omitted assertion.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
