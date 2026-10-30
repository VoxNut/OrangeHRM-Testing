/*
RECRUIT_CAND_Filter_03 — Lọc kết hợp Vacancy và ngày ứng tuyển
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 29
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: VAC_A; From=RUN_DATE−1 ngày; To=RUN_DATE+1 ngày (ngày tạo fixture thực tế)
1. Ghi nhận ngày ứng tuyển thật của CAND_FILTER_A.
2. Chọn Vacancy=VAC_A và khoảng ngày bao phủ đúng ngày đó.
3. Search; đối chiếu kết quả ngày và vacancy.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data.
Expected: Chỉ xuất hiện ứng viên thỏa vacancy và ngày trong khoảng đã nhập.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_04
Positive; Bảng quyết định
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Filter_03')
try {
    ui.filterCase('date')
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
