/*
RECRUIT_CAND_UploadCV_03 — Tải CV định dạng không hợp lệ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 23
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: resume_run.exe; dung lượng nhỏ
1. Add Candidate với thông tin hợp lệ.
2. Chọn resume_run.exe; Save.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Hệ thống từ chối định dạng không hỗ trợ; không lưu tệp .exe.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_03
Negative; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_UploadCV_03')
try {
    ui.preflightResume('exe', false)
    Map vac = ui.vacancy(); Map d = ui.draft(vac, 'EXE')
    ui.openAdd(d)
    def assets = new ResumeFiles(ui); File invalid = assets.invalidExtension()
    assets.checkSmall(invalid, false)
    ui.invalidResume(d, vac, invalid, '(?i).*(file type.*allowed|file type.*supported|invalid.*file|file.*not allowed).*')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
