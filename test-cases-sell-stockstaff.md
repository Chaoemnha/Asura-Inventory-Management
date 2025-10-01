# Bảng Kiểm Thử Chức Năng Xuất Hàng (STOCKSTAFF)

## Mô tả điều kiện

| Điều kiện đầu vào | Vùng hợp lệ | Kỳ hiệu đánh dấu | Vùng không hợp lệ | Kỳ hiệu đánh dấu |
|-------------------|-------------|------------------|-------------------|------------------|
| Sản phẩm | Chọn sản phẩm có sẵn | 1 | Không chọn sản phẩm | 4 |
| Khách hàng | Chọn khách hàng hợp lệ (role=CUSTOMER) | 2 | Không chọn khách hàng | 5 |
| Số lượng | Số nguyên dương (1 ≤ qty ≤ stockQuantity) | 3 | Số âm, 0, hoặc > stockQuantity | 6 |
| Mô tả | Văn bản bất kỳ | 7 | Bỏ trống | 8 |
| Tồn kho | Sản phẩm có stockQuantity > 0 | 9 | Sản phẩm hết hàng (stockQuantity = 0) | 10 |

## Mô tả test case cho STOCKSTAFF

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 1 | productId="", userId="2", quantity="5", description="Bán hàng" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (4, 2, 3, 7) | Failed |
| 2 | productId="1", userId="", quantity="5", description="Bán hàng" | Thông báo lỗi: "Vui lòng chọn khách hàng" | (1, 5, 3, 7) | Failed |
| 3 | productId="1", userId="2", quantity="", description="Bán hàng" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 4 | productId="1", userId="2", quantity="0", description="Bán hàng" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 5 | productId="1", userId="2", quantity="-3", description="Bán hàng" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 6 | productId="1" (stockQuantity=10), userId="2", quantity="15", description="Bán hàng" | Thông báo lỗi: "Số lượng vượt quá tồn kho" | (1, 2, 6, 7) | Failed |
| 7 | productId="1" (stockQuantity=0), userId="2", quantity="5", description="Bán hàng" | Thông báo lỗi: "Sản phẩm đã hết hàng" | (1, 2, 3, 7, 10) | Failed |
| 8 | productId="1" (stockQuantity=10), userId="2", quantity="5", description="Bán hàng cho khách VIP" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 9) | Pass |
| 9 | productId="1" (stockQuantity=10), userId="2", quantity="10", description="" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 8, 9) | Pass |
| 10 | productId="1" (stockQuantity=1), userId="2", quantity="1", description="Bán sản phẩm cuối" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 9) | Pass |
| 11 | productId="1" (stockQuantity=100), userId="2", quantity="50", description="Bán số lượng lớn" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 9) | Pass |

## Kiểm thử tính năng đặc biệt

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 12 | Chọn sản phẩm → Thay đổi số lượng | Tổng tiền được tính lại tự động (unitPrice × quantity) | (1, 3, 9) | Pass |
| 13 | Chọn sản phẩm có giá 1,000,000₫, quantity=3 | Hiển thị tổng tiền: 3,000,000₫ | (1, 3, 9) | Pass |
| 14 | Reset form sau khi điền thông tin | Tất cả trường được xóa và về trạng thái ban đầu | (1, 2, 3, 7) | Pass |
| 15 | Chọn sản phẩm từ query params (productId trong URL) | Sản phẩm được pre-select và thông tin hiển thị | (1, 9) | Pass |

## Kiểm thử quyền hạn STOCKSTAFF

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 16 | Login với role STOCKSTAFF | Form hiển thị dropdown "Chọn khách hàng" | (2) | Pass |
| 17 | Login với role STOCKSTAFF | Danh sách khách hàng chỉ hiển thị user có role=CUSTOMER | (2) | Pass |
| 18 | Login với role STOCKSTAFF | Không hiển thị các trường Admin (ngày tạo, trạng thái) | (-) | Pass |
| 19 | Submit form với role STOCKSTAFF | Body request có userId và senderId | (1, 2, 3) | Pass |
| 20 | Login với role STOCKSTAFF | Tiêu đề form hiển thị "Bán sản phẩm" | (-) | Pass |

## Kiểm thử validation dữ liệu

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 21 | quantity="abc" (không phải số) | quantity được parse thành 0, tổng tiền = 0 | (6) | Pass |
| 22 | quantity="3.5" (số thập phân) | quantity được parse thành 3 (parseInt) | (3) | Pass |
| 23 | Chọn userId không tồn tại | Thông báo lỗi từ backend | (5) | Failed |
| 24 | Chọn productId không tồn tại | Thông báo lỗi từ backend | (4) | Failed |

## Ghi chú

- **Vai trò STOCKSTAFF**: Có thể bán hàng cho khách hàng, phải chọn khách hàng từ danh sách
- **Validation tồn kho**: Frontend không validate stock quantity, để backend xử lý
- **Tính giá tự động**: `totalPrice = unitPrice × quantity`
- **senderId**: Tự động lấy từ thông tin user đăng nhập hiện tại
- **userId**: Bắt buộc phải chọn khách hàng từ dropdown (chỉ hiển thị user có role=CUSTOMER)
- **Description**: Trường optional, có thể để trống
- **Quantity input**: Có validation min="1" và max=stockQuantity trong HTML
- **Form reset**: Xóa tất cả dữ liệu và đặt lại về trạng thái ban đầu