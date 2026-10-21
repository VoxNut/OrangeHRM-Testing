# PIM_EMP – Playwright

Runner thực thi 25 test case của Võ Minh Nhựt. Công cụ: Node.js, Playwright 1.62.1, Microsoft Edge headless. Không dùng phiên đăng nhập cá nhân. Tài khoản Admin/admin123 là tài khoản demo công khai.

## Chạy

Mở terminal tại thư mục này:

```powershell
npm install
npm test
```

Máy cần có Microsoft Edge. Runner tạo dữ liệu giả bằng UI, assertion sau Save/reload và Search, ghi JSON/screenshot rồi xóa đúng hồ sơ nó tạo. Không chạy đồng thời hai phiên vì ID fixture cố định theo workbook. Trước mỗi run, xác nhận các ID G1N01* của run trước đã dọn. Nếu trùng dữ liệu không rõ nguồn, dừng và chuẩn bị run token khác thay vì xóa hồ sơ có sẵn.

Kết quả ở `evidence/executions/23130226/<run-id>/results.json`, ảnh trong `screenshots/`, tệp upload trong `assets/`. Đây là fixture kỹ thuật, không phải screenshot tạo sẵn.

Có thể đặt `PIM_RUN_ID` để nhận diện lần chạy, `PIM_REPO` để đổi thư mục repo. `PIM_ONLY` nhận số dòng case 1–25, phân cách dấu phẩy, để chẩn đoán. Không dùng run một phần làm báo cáo 25 case.

`PIM_CLEANUP_PREVIOUS` nhận đường dẫn JSON của run cũ, phân cách bằng dấu chấm phẩy. Chỉ dùng với log do chính runner tạo: cleanup xác nhận Employee Id và numeric employee key trước khi xóa. Không truyền log của người khác.

## Diễn giải

Passed cần assertion đạt, không chỉ hoàn tất thao tác. Failed là actual khác expected; cần kiểm tra bằng chứng để phân biệt lỗi sản phẩm và script. Blocked là không hoàn tất do setup, locator hoặc timeout. Workbook chỉ lấy kết quả run cuối hợp lệ; không cộng nhiều lần chạy của cùng một case. PIM_PERSONAL/PIM_JOB chưa được runner này thực thi.

Demo công khai có thể bị reset hoặc thay đổi bởi người khác. Không đổi cấu hình chung, không quét bảo mật, không chạy tải lớn.

## Hạn chế đã quan sát

`PIM_EMP_Search_02`: truy vấn G01N01 có nhiều tên, ô autocomplete chỉ hiển thị 5 gợi ý đầu, F_PERSON có thể không xuất hiện. Run chính thức giữ Blocked nếu timeout. Trước retest cần tên fixture duy nhất hoặc bổ sung bước nhập cụ thể hơn trong test design, không đổi expected chỉ để đạt Passed.
