/*
RECRUIT_CAND_UploadCV_01 — Tải lên CV PDF hợp lệ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 21
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: resume_run.pdf; dung lượng <CV_LIMIT
1. Chuẩn bị file PDF hợp lệ.
2. Add Candidate; nhập đủ trường và chọn CV.
3. Save rồi mở hồ sơ, đọc tên file.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: CV được đính kèm đúng tên và có thể mở/tải theo quyền của demo.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_03
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_UploadCV_01')
try {
    ui.preflightResume('pdf')
    Map vac = ui.vacancy(); Map d = ui.draft(vac, 'PDF')
    ui.openAdd(d)
    def assets = new ResumeFiles(ui); File cv = assets.pdf()
    assets.checkSmall(cv)
    ui.track([kind:'resume-input', filename:cv.name, bytes:cv.length(), sha256:CandidateChecks.sha256(cv), mime:'application/pdf'])
    d.resume = cv; ui.upload(cv); ui.saveNew(d)
    ui.resumePersisted(d, cv, true) // Native Chrome download + SHA-256, no API upload/download shortcut.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
