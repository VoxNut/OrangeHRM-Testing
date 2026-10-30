/*
RECRUIT_CAND_Filter_02 — Lọc ứng viên theo Keywords
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 28
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Keyword='run_keyword_a' trên CAND_FILTER_A
1. Mở Candidates; nhập Keywords=run_keyword_a; Search.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Hiện ứng viên có keyword phù hợp, gồm CAND_FILTER_A.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_04
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Filter_02')
try {
    ui.filterCase('keyword')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
