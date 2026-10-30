/*
RECRUIT_CAND_Filter_01 — Lọc ứng viên theo Vacancy
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 26
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: VAC_A; CAND_FILTER_A thuộc VAC_A
1. Vào Recruitment > Candidates.
2. Chọn Vacancy=VAC_A; Search.
3. Đối chiếu các dòng.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: CAND_FILTER_A xuất hiện; kết quả thuộc VAC_A.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_04
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Filter_01')
try {
    ui.filterCase('vacancy')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
