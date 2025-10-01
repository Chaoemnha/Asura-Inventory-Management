# Bảng Kiểm Thử Chức Năng Chat AI Assistant (Người dùng chưa đăng nhập - GUEST)

## ✅ CÁC VẤN ĐỀ ĐÃ ĐƯỢC SỬA:

### 1. **✅ Đã sửa Switch Statement:**
- Thêm `break;` statements đúng cách
- Mỗi role có system message riêng biệt

### 2. **✅ Đã tạo UserRole.GUEST:**
- User chưa đăng nhập → map thành `GUEST` role
- Tách biệt quyền hạn giữa GUEST và CUSTOMER

### 3. **✅ Cải thiện Security:**
- GUEST chỉ có quyền hạn tối thiểu
- Điều kiện WHERE cho transactions với user_id/sender_id
- Message tiếng Việt có dấu, ngắn gọn

## Mô tả điều kiện (Sau khi sửa lỗi)

| Điều kiện đầu vào | Vùng hợp lệ | Kỳ hiệu đánh dấu | Vùng không hợp lệ | Kỳ hiệu đánh dấu |
|-------------------|-------------|------------------|-------------------|------------------|
| Authentication Status | Chưa đăng nhập (GUEST role) | 1 | Token hết hạn/không hợp lệ | 6 |
| Câu hỏi (Question) | Chuỗi không rỗng, có nội dung | 2 | Chuỗi rỗng hoặc null | 7 |
| Quyền truy cập dữ liệu | Chỉ products (id,name,description,price), categories.name | 3 | Yêu cầu dữ liệu nhạy cảm (users, transactions) | 8 |
| Điều hướng hợp lệ | Chỉ /login, /product/{id} | 4 | Các URL khác (/dashboard, /transaction) | 9 |
| Định dạng phản hồi | JSON navigation hoặc text tiếng Việt | 5 | HTML/Script injection | 10 |
| Yêu cầu từ chối | URL không được phép | 11 | - | - |

## Mô tả test case cho người dùng GUEST

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 1 | question="" (chuỗi rỗng) | "Câu hỏi không được để trống" | (1, 7) | Pass |
| 2 | question=null | "Câu hỏi không được để trống" | (1, 7) | Pass |
| 3 | question="   " (chỉ có space) | "Câu hỏi không được để trống" | (1, 7) | Pass |
| 4 | question="Xin chào" | Phản hồi chào hỏi bằng tiếng Việt có dấu | (1, 2, 5) | Pass |
| 5 | question="Có những sản phẩm nào?" | Trả về danh sách sản phẩm (chỉ name, price, description) | (1, 2, 3, 5) | Pass |
| 6 | question="Giá của sản phẩm laptop là bao nhiều?" | Trả về giá sản phẩm cụ thể | (1, 2, 3, 5) | Pass |
| 7 | question="Cho tôi thông tin người dùng" | "Tôi không tìm thấy thông tin này" | (1, 2, 8, 5) | Pass |
| 8 | question="Hiển thị giao dịch của tôi" | "Tôi không tìm thấy thông tin này" | (1, 2, 8, 5) | Pass |
| 9 | question="Đưa tôi đến trang admin" | Từ chối: "Bạn không có quyền truy cập trang này" | (1, 2, 9, 11) | Pass |
| 10 | question="Đưa tôi đến trang sản phẩm" | JSON: {"action": "navigate", "url": "/product", "message": "Đang chuyển trang..."} | (1, 2, 4, 5) | Pass |
| 11 | question="Tôi muốn đăng nhập" | JSON: {"action": "navigate", "url": "/login", "message": "Đang chuyển trang..."} | (1, 2, 4, 5) | Pass |
| 12 | question="Đưa tôi đến dashboard" | Từ chối: "Vui lòng đăng nhập để truy cập tính năng này" | (1, 2, 9, 11) | Pass |
| 13 | question="<script>alert('xss')</script>" | Text được escape, không thực thi script | (1, 2, 10, 5) | Pass |

## Kiểm thử quyền hạn cụ thể cho GUEST

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 14 | "Cho tôi xem thông tin sản phẩm có ID 1" | JSON navigate hoặc thông tin sản phẩm | (1, 2, 3, 4, 5) | Pass |
| 15 | "Liệt kê các danh mục sản phẩm" | Danh sách categories.name | (1, 2, 3, 5) | Pass |
| 16 | "Sản phẩm nào có giá dưới 1 triệu?" | Danh sách sản phẩm thỏa mãn điều kiện | (1, 2, 3, 5) | Pass |
| 17 | "Cho tôi biết thông tin nhà cung cấp" | "Tôi không tìm thấy thông tin này" | (1, 2, 8, 5) | Pass |
| 18 | "Hiển thị số lượng tồn kho" | "Vui lòng đăng nhập để xem thông tin này" | (1, 2, 8, 5) | Pass |
| 19 | "Đưa tôi đến trang mua hàng" | Từ chối hoặc yêu cầu đăng nhập | (1, 2, 9, 11) | Pass |
| 20 | "Tôi muốn xem sản phẩm laptop" | JSON navigate đến /product hoặc danh sách laptop | (1, 2, 3, 4, 5) | Pass |

## Kiểm thử Integration với Frontend

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 21 | Gửi tin nhắn không có token trong localStorage | Request thành công, xử lý như GUEST | (1, 2) | Pass |
| 22 | Token hết hạn trong localStorage | Tự động fallback về GUEST mode | (6, 2) | Pass |
| 23 | Response có action="navigate" với URL hợp lệ | Frontend điều hướng sau 1 giây | (4, 5) | Pass |
| 24 | Response có action="navigate" với URL không hợp lệ | Frontend không điều hướng, hiển thị thông báo | (9, 11) | Pass |
| 25 | Typing indicator trong quá trình xử lý | Indicator hiển thị, sau đó ẩn khi có response | (2, 5) | Pass |
| 26 | Message hiển thị với timestamp tiếng Việt | Thời gian theo format Việt Nam | (2, 5) | Pass |

## Kiểm thử Error Handling

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 27 | Server trả về 500 error | "Lỗi server nội bộ. Vui lòng thử lại sau." | (1, 2) | Pass |
| 28 | Mất kết nối mạng (status=0) | "Không thể kết nối với server. Vui lòng kiểm tra kết nối mạng." | (1, 2) | Pass |
| 29 | Response không đúng format JSON | "Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn." | (1, 2) | Pass |
| 30 | OpenAI API key không hợp lệ | Error được handle gracefully | (1, 2) | Pass |

## Kiểm thử Message Quality (Tiếng Việt có dấu)

| TC | Đầu vào (Input) | Đầu ra mong đợi (Output) | Độ bao phủ (Cover) | Kết quả (Result) |
|----|-----------------|--------------------------|-------------------|------------------|
| 31 | "Sản phẩm nào bán chạy nhất?" | Response tiếng Việt có dấu, khuyến khích đăng nhập | (1, 2, 8, 5) | Pass |
| 32 | "Tôi cần hỗ trợ mua hàng" | Hướng dẫn đăng nhập, điều hướng /login | (1, 2, 4, 5) | Pass |
| 33 | "Làm sao để xem chi tiết đơn hàng?" | "Vui lòng đăng nhập để xem thông tin giao dịch" | (1, 2, 8, 5) | Pass |
| 34 | Question với ký tự đặc biệt Unicode | Xử lý đúng, response tiếng Việt | (1, 2, 5) | Pass |

## Ghi chú

### **✅ CẢI THIỆN ĐÃ THỰC HIỆN:**

1. **System Message ngắn gọn hơn**: Dễ hiểu cho OpenAI model
2. **Tiếng Việt có dấu**: Response tự nhiên hơn
3. **Security tăng cường**: 
   - GUEST chỉ có quyền tối thiểu
   - Điều kiện WHERE cho transactions
   - Từ chối URL không được phép
4. **UX tốt hơn**: Khuyến khích đăng nhập thay vì từ chối cứng

### **Quyền hạn GUEST role:**
- ✅ Chỉ xem: products.id, name, description, price, categories.name
- ✅ Điều hướng: /login, /product/{id}
- ❌ Không truy cập: users, suppliers, transactions, dashboard
- ✅ Khuyến khích đăng nhập cho tính năng nâng cao

### **Frontend cần test:**
- Token null/empty handling
- Navigation với URL validation  
- Error message display
- Tiếng Việt encoding