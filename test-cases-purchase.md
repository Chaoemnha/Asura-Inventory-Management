# Bảng Kiểm Thử Chức Năng Nhập Hàng

## Mô tả điều kiện

| Điều kiện đầu vào | Vùng hợp lệ | Kỳ hiệu đánh dấu | Vùng không hợp lệ | Kỳ hiệu đánh dấu |
|-------------------|-------------|------------------|-------------------|------------------|
| Sản phẩm | Chọn sản phẩm có sẵn | 1 | Không chọn sản phẩm | 4 |
| Nhà cung cấp | Chọn nhà cung cấp hợp lệ | 2 | Không chọn nhà cung cấp | 5 |
| Số lượng | Số nguyên dương (>0) | 3 | Số âm hoặc bằng 0 | 6 |
| Giá nhập | 0 ≤ giá ≤ maxPrice | 7 | Giá âm hoặc > maxPrice | 8 |
| Người gửi (Admin) | Chọn người gửi hợp lệ | 9 | Bỏ trống | 10 |
| Mô tả | Văn bản bất kỳ | 11 | Bỏ trống | 12 |

## Mô tả test case

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 1 | productId="1", supplierId="2", quantity="10", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (4, 5, 6, 8) | Failed |
| 2 | productId="", supplierId="2", quantity="10", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (4, 2, 3, 7) | Failed |
| 3 | productId="1", supplierId="", quantity="10", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 5, 3, 7) | Failed |
| 4 | productId="1", supplierId="2", quantity="", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 5 | productId="1", supplierId="2", quantity="10", price="" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 3, 8) | Failed |
| 6 | productId="1", supplierId="2", quantity="-5", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 7 | productId="1", supplierId="2", quantity="0", price="5000000" | Thông báo lỗi: "Vui lòng điền đầy đủ thông tin" | (1, 2, 6, 7) | Failed |
| 8 | productId="1", supplierId="2", quantity="10", price="-1000000" | Thông báo lỗi: "Giá phải nằm trong khoảng 0 và [maxPrice]" | (1, 2, 3, 8) | Failed |
| 9 | productId="1", supplierId="2", quantity="10", price="50000000" | Thông báo lỗi: "Giá phải nằm trong khoảng 0 và [maxPrice]" | (1, 2, 3, 8) | Failed |
| 10 | productId="1", supplierId="2", quantity="10", price="8000000", description="Nhập hàng tháng 1" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 11) | Pass |
| 11 | productId="1", supplierId="2", quantity="5", price="7500000" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 12) | Pass |
| 12 | productId="1", supplierId="2", quantity="1", price="0" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 12) | Pass |
| 13 | productId="1", supplierId="2", quantity="100", price="maxPrice" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 12) | Pass |

## Trường hợp đặc biệt cho Admin

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 14 | productId="1", supplierId="2", quantity="10", price="5000000", senderId="3", createdAt="2024-01-15T10:00", status="COMPLETED" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 9, 11, 12) | Pass |
| 15 | productId="1", supplierId="2", quantity="10", price="5000000", senderId="", status="PENDING" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 10, 11, 12) | Pass |
| 16 | productId="1", supplierId="2", quantity="10", price="5000000", createdAt="2024-01-15T10:00" | Thông báo thành công: "Transaction Made Successfully" | (1, 2, 3, 7, 11, 12) | Pass |

## Ghi chú

- **maxPrice**: Được tính toán là `selectedProduct.price - 10000` (tối thiểu là 0)
- **Giá mặc định**: Khi chọn sản phẩm, giá được set mặc định là `selectedProduct.price * 0.8`
- **Người gửi**: Chỉ hiển thị khi Admin chọn nhà cung cấp, danh sách người gửi được lọc theo `supplierId`
- **Trạng thái mặc định**: Nếu Admin không chọn trạng thái, hệ thống sử dụng trạng thái mặc định
- **Ngày tạo mặc định**: Nếu Admin không nhập ngày tạo, hệ thống sử dụng thời gian hiện tại