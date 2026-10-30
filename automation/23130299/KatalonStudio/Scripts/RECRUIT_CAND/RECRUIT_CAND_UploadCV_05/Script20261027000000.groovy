/*
RECRUIT_CAND_UploadCV_05 — Hủy chọn CV trước lưu
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 25
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Chọn resume_run.pdf rồi hủy/tắt form, tạo hồ sơ mới không file
1. Mở Add, chọn CV hợp lệ nhưng chưa Save.
2. Cancel/đóng form.
3. Mở Add lại, tạo ứng viên khác không file.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Không có CV của lần thao tác hủy trong hồ sơ mới; không phát sinh ứng viên lần đầu.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_03
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_UploadCV_05')
try {
    ui.preflightResume('pdf')
    Map vac = ui.vacancy(); Map cancelled = ui.draft(vac, 'CANCEL')
    ui.openAdd(cancelled)
    def assets = new ResumeFiles(ui); File cv = assets.pdf()
    assets.checkSmall(cv)
    ui.upload(cv); ui.buttonClick('Cancel'); ui.noCandidate(cancelled, vac)
    Map clean = ui.create(ui.draft(vac, 'CLEAN'))
    ui.reopen(clean)
    assert !ui.resumeText().contains(cv.name) : 'Cancelled file leaked into a different candidate'
    assert !ui.shown(CandidateChecks.group('Resume') + "//*[contains(concat(' ',normalize-space(@class),' '),' orangehrm-file-current ')]") : 'Cancelled attachment persisted'
    assert !ui.shown(CandidateChecks.group('Resume') + '//button[.//i[contains(@class,"bi-download")]]') : 'Unexpected persisted resume'
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
