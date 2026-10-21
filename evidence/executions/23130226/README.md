# PIM_EMP – Kết quả thực thi

Run chính thức cho workbook: `PIM_EMP_20261021_04`. Mở `report.html` để xem tổng hợp và link screenshot hoặc `results.json` để đối chiếu assertion, thời điểm, phản hồi request và cleanup.

25 case PIM_EMP được thực thi trên OrangeHRM demo bằng Playwright/Edge. PIM_PERSONAL và PIM_JOB chưa thực thi. Mỗi case có ảnh chụp thật tại thời điểm chạy; tệp màu đơn trong `assets/` chỉ dùng kiểm thử upload, không phải minh chứng kết quả.

Run chính thức: 23 Passed, 0 Failed, 2 Blocked. `PIM_EMP_Search_02` bị chặn vì F_PERSON không nằm trong 5 gợi ý đầu của truy vấn G01N01. `PIM_EMP_Sort_01` chưa hoàn thành assertion do Playwright không đọc được response body trước điều hướng (`Network.getResponseBody`). Chưa có căn cứ kết luận hai chức năng này lỗi sản phẩm. Retest cần sửa đồng bộ response và thu hẹp truy vấn/tên fixture. Toàn bộ 12 bản ghi do run 04 tạo đã được cleanup thành công, có đối chiếu numeric key trong JSON.

Các run 01–03 dùng để chẩn đoán đồng bộ/locator hoặc bị dừng trong setup. Chúng không được cộng vào Result của workbook. Bản chẩn đoán giữ ở máy làm bài, ngoài bộ kết quả chính thức.

Trong log run 04, thông điệp assertion tìm bản ghi của một số negative case có hậu tố chung về ID 11 ký tự. Hậu tố đó chỉ áp dụng cho `PIM_EMP_Create_13`; kết quả các case khác dựa vào lỗi đúng trường và tìm ID riêng. Workbook bỏ hậu tố không liên quan, JSON gốc được giữ nguyên. Runner đã sửa thông điệp cho các lần chạy sau.

Cleanup chỉ xử lý bản ghi do runner tạo, đối chiếu Employee Id và numeric employee key. Không sửa/xóa hồ sơ có sẵn của người khác. Run trước và rerun không phải các test case khác nhau.
