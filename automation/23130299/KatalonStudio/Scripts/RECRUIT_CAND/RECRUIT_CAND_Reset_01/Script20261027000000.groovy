/*
RECRUIT_CAND_Reset_01 — Reset bộ lọc Candidates
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 30
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: Vacancy=VAC_A; Keywords=run_keyword_a
1. Áp dụng hai điều kiện; Search.
2. Nhấn Reset.
3. Kiểm tra input và kết quả.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Hai filter về mặc định; danh sách trở lại trạng thái không lọc.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_04
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Reset_01')
try {
    ui.filterCase('reset')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
