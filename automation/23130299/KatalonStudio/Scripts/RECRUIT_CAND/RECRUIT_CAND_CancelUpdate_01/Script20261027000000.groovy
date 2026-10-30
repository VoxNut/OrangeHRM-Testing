/*
RECRUIT_CAND_CancelUpdate_01 — Hủy chỉnh sửa hồ sơ ứng viên
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 32
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: CAND_EDIT; Email tạm=unsaved@example.com
1. Mở CAND_EDIT; ghi email gốc.
2. Vào Edit, đổi email; nhấn Cancel/Back.
3. Reload hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Email vẫn đúng baseline; không lưu email tạm.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_05
Positive; Chuyển trạng thái
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_CancelUpdate_01')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, 'CANCEL_EDIT', [phone:'0900000012']))
    ui.cancelEdit(d)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
