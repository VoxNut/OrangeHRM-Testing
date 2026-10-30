/*
RECRUIT_CAND_UploadCV_04 — Tải CV vượt dung lượng tối đa
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 24
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: resume_oversize.pdf; dung lượng CV_LIMIT+1 byte
1. Đọc giới hạn file được hiển thị trên UI.
2. Add Candidate, chọn file PDF vượt đúng 1 byte; Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Hiển thị lỗi vượt giới hạn; file quá lớn không được đính kèm.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_03
Negative; Phân tích giá trị biên
Automation: P1
Cần kiểm tra UI: CV_LIMIT lấy từ demo, không áp dụng mặc định 5MB của bản thương mại.
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_UploadCV_04')
try {
    ui.preflightResume('pdf', true, true)
    Map vac = ui.vacancy(); Map d = ui.draft(vac, 'BIG')
    ui.openAdd(d)
    def assets = new ResumeFiles(ui); long limit = assets.limit(); File oversize = assets.oversized(limit)
    assert oversize.length() == limit + 1L
    ui.track([kind:'resume-input', filename:oversize.name, bytes:oversize.length(), boundary:'CV_LIMIT+1'])
    ui.invalidResume(d, vac, oversize, '(?i).*(size.*exceed|exceed.*size|file.*too large|maximum.*size).*')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
