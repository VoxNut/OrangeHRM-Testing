/*
RECRUIT_CAND_UploadCV_02 — Tải lên CV DOCX hợp lệ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 22
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: resume_run.docx; dung lượng <CV_LIMIT
1. Chuẩn bị DOCX hợp lệ.
2. Add Candidate và upload file.
3. Save; mở chi tiết hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: CV DOCX được đính kèm và hiển thị đúng tên.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_03
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_UploadCV_02')
try {
    ui.preflightResume('docx')
    Map vac = ui.vacancy(); Map d = ui.draft(vac, 'DOCX')
    ui.openAdd(d)
    def assets = new ResumeFiles(ui); File cv = assets.docx()
    assets.checkSmall(cv)
    ui.track([kind:'resume-input', filename:cv.name, bytes:cv.length(), sha256:CandidateChecks.sha256(cv), mime:'application/vnd.openxmlformats-officedocument.wordprocessingml.document'])
    d.resume = cv; ui.upload(cv); ui.saveNew(d); ui.resumePersisted(d, cv)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
