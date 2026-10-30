/*
RECRUIT_CAND_Create_01 — Tạo ứng viên tối thiểu
Source: Group01_23130299_NguyenVanThang_Source_Aligned.xlsx / RECRUIT_CAND, row 9
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: First=An; Last=NguyenRUN01; Email=an.run01@example.com; Vacancy=VAC_A
1. Vào Recruitment > Candidates > Add.
2. Nhập First, Last, Email và chọn VAC_A.
3. Save; ghi Candidate ID; tìm bằng Candidate Name rồi mở đúng ID và kiểm tra Email trong Profile.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Tạo được ứng viên; tên, email và vacancy đúng input.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_01
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_01')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, '01'))
    ui.verifyListAndReopen(d, vac)
    ui.verifyDetails(d) // Source_Aligned: exact Email verification in the reopened owned Profile.
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
