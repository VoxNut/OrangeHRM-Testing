/*
RECRUIT_CAND_Create_02 — Tạo ứng viên với thông tin đầy đủ
Source: Group01_23130299_NguyenVanThang.xlsx / RECRUIT_CAND, row 10
Tiền điều kiện: Đã đăng nhập Admin/Recruitment; có VAC_A đang hiệu lực; email example.com và fixture hồ sơ riêng theo RUN_ID.
Input: First=Minh; Middle=Quang; Last=TranRUN02; Email=minh.run02@example.com; Phone=0900000002; Keywords=java,qa; Notes=demo; VAC_A
1. Mở Add Candidate; nhập đủ dữ liệu và chọn VAC_A.
2. Chọn consent theo trạng thái cho phép lưu; Save.
3. Mở hồ sơ.
Kết thúc: khôi phục / dọn dẹp fixture theo Test Data; không dùng bản ghi của người khác.
Expected: Lưu đúng tên đầy đủ, email, phone, vacancy, keyword và notes.
Dependence: Không phụ thuộc test case khác; fixture được thiết lập trước case và khôi phục sau case.
Design note: TD_RECRUIT_CAND_01
Positive; Phân vùng tương đương
Automation: P1
Preparation status: NOT RUN / NEEDS LIVE VERIFICATION (not an execution result).
RUN placeholders resolve per case; negative literal inputs are kept unchanged.
*/
import recruitment.CandidateChecks
import recruitment.ResumeFiles

def ui = new CandidateChecks('RECRUIT_CAND_Create_02')
try {
    Map vac = ui.vacancy()
    Map d = ui.create(ui.draft(vac, '02', [first:'Minh', middle:'Quang', last:'Tran' + ui.token + '02',
        email:'minh.' + ui.token + '@example.com', phone:'0900000002', keywords:'java,qa', notes:'demo', consent:true]))
    ui.verifyListAndReopen(d, vac)
    ui.finish()
} catch (Throwable failure) {
    ui.failure(failure)
    throw failure
}
