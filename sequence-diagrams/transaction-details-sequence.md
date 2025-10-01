# Biểu đồ trình tự - Use case: Xem chi tiết giao dịch

## Mô tả
Use case này cho phép người dùng xem thông tin chi tiết của một giao dịch cụ thể, bao gồm thông tin sản phẩm, nhà cung cấp, người dùng, và các hành động có thể thực hiện tùy theo quyền và trạng thái giao dịch.

## Sequence Diagram - PlantUML

```plantuml
@startuml
title Sequence Diagram - Xem chi tiết giao dịch

actor User as U
boundary "TransactionDetailsUI" as UI
control "TransactionController" as TC
database MySQL as DB

== Khởi tạo và Authentication ==
U -> UI: Truy cập URL /transaction/{transactionId}
UI -> UI: Kiểm tra authentication
UI -> U: Hiển thị trang chi tiết giao dịch

== Lấy thông tin người dùng và chi tiết giao dịch ==
UI -> TC: getLoggedInUserInfo()
TC -> DB: SELECT user information
DB -> TC: User data
TC -> UI: User information

UI -> TC: getTransactionById(transactionId)
TC -> DB: SELECT transaction with product, supplier, user details
DB -> TC: Transaction with related data
TC -> UI: Transaction details

alt Transaction found và User có quyền truy cập
    UI -> UI: Xác định role (Admin/StockStaff/Customer/Supplier)
    UI -> UI: Kiểm tra trạng thái transaction
    UI -> UI: Xác định các action buttons
    
    note over UI: Xác định quyền dựa trên:
    note over UI: - User role
    note over UI: - Transaction type  
    note over UI: - Transaction status
    note over UI: - Ownership (user.id, supplier.id)
    
    UI -> U: Hiển thị chi tiết giao dịch với action buttons
    
else User không có quyền hoặc Transaction không tìm thấy
    UI -> U: Hiển thị thông báo lỗi (403/404)
end

== Use case mở rộng: Cập nhật trạng thái giao dịch ==
alt User thực hiện action (confirm/reject/etc.)
    U -> UI: Click action button
    UI -> TC: updateTransactionStatus(transactionId, newStatus)
    TC -> DB: UPDATE transactions SET status = ? WHERE id = ?
    DB -> TC: Update successful
    TC -> UI: Success response
    UI -> UI: Refresh transaction details
    UI -> U: Hiển thị trạng thái mới và thông báo thành công
end

== Use case mở rộng: Tạo QR Code và Export PDF ==
alt User click "Generate PDF with QR"
    U -> UI: Click Generate PDF button
    UI -> TC: exportInvoiceWithQR(transactionId, status)
    TC -> TC: Tạo QR code với URL
    note right: QR contains: baseUrl/transactions/{id}/{status}
    TC -> TC: Tạo PDF invoice với QR code
    TC -> UI: PDF file download
    UI -> U: Tải file PDF thành công
end

== Use case mở rộng: Quét QR Code ==
alt User click "Scan QR" (confirm arrival)
    U -> UI: Click Scan QR button
    UI -> U: Hiển thị camera interface để quét QR
    
    U -> UI: Quét QR code (URL với transaction ID và status)
    UI -> TC: updateTransactionStatusViaQR(id, status)
    TC -> TC: Validate QR và cập nhật status
    TC -> DB: UPDATE transactions SET status = ?
    DB -> TC: Update successful
    TC -> UI: Success response
    UI -> UI: Đóng QR scanner, refresh data
    UI -> U: Hiển thị trạng thái đã cập nhật
end

== Use case mở rộng: Navigate to Staff Profile ==
alt User click vào staff name (nếu user là STOCKSTAFF)
    U -> UI: Click staff name link
    UI -> U: Chuyển đến trang Staff Profile
    note right: Hiển thị thông tin staff và activity report
end

@enduml
```

## Các trường hợp đặc biệt

### 1. Quyền truy cập dựa trên Role và Ownership
- **ADMIN**: Có thể xem tất cả giao dịch
- **STOCKSTAFF**: Chỉ xem giao dịch mà họ tạo hoặc liên quan
- **CUSTOMER**: Chỉ xem giao dịch của chính họ
- **SUPPLIER**: Chỉ xem giao dịch liên quan đến supplier của họ

### 2. Action Buttons hiển thị theo điều kiện
```typescript
// Logic xác định action buttons
canGenQR = transaction.type != "RETURN_TO_SUPPLIER" && 
           transaction.supplier.id == user.supplier.id && 
           transaction.status == 'PENDING'

canScanQR = (!isCustomer && transaction.supplier.id == user.supplier.id && 
            transaction.status == "PENDING") ||
            (isCustomer && transaction.type == "SALE" && 
            transaction.user.id == user.id && transaction.status == "PENDING")

canReturn = (isAdmin || (isStockstaff && user.id == transaction.user.id)) && 
            transaction.type == "PURCHASE" && transaction.status == "COMPLETED"
```

### 3. Real-time Updates
- Khi trạng thái giao dịch được cập nhật, UI tự động refresh để hiển thị trạng thái mới
- Không cần WebSocket, sử dụng refresh mechanism đơn giản

### 4. QR Code Workflow
- QR Code chứa URL: `{baseUrl}/transactions/{transactionId}/{newStatus}`
- Khi quét QR, hệ thống tự động cập nhật trạng thái giao dịch
- Dùng cho xác nhận nhận hàng, xác nhận thanh toán

### 5. Error Handling
- **404**: Transaction không tồn tại
- **403**: Không có quyền truy cập
- **400**: Dữ liệu không hợp lệ
- **500**: Lỗi server

## Tích hợp với các Use Case khác
- Liên kết với **Transaction Search** khi navigate từ danh sách
- Kết nối với **Staff Profile** khi click vào tên staff
- Liên kết với **Return Transaction** use case khi click return button

## Performance Considerations
- Lazy loading cho Product, Supplier, User relationships
- Caching transaction details để tránh query lặp lại
- PDF generation optimization