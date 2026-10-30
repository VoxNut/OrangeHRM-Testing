/*
RECRUIT_CAND_Search_01 — Lọc theo Candidate Name
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 27
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Tên chính xác CAND_FILTER_A
1. Mở Candidates; gõ tên, chọn đúng suggestion nếu được yêu cầu.
2. Search.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Hiện CAND_FILTER_A, không lẫn CAND_FILTER_B khác tên.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_04
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Search_01')
try {
    ui.filterCase('name')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
