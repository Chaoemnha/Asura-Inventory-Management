# Biểu đồ trình tự - Use case: Trả hàng (Return Product)

## Mô tả
Use case này cho phép người dùng thực hiện trả hàng từ một giao dịch đã hoàn thành, bao gồm: truy cập trang trả hàng, điền thông tin lý do và số lượng, submit request và quay về danh sách giao dịch.

## 1. Sequence Diagram - Truy cập trang trả hàng

```plantuml
@startuml
title Return Product - Truy cập trang trả hàng

actor User as U
boundary "TransactionDetailsUI" as TUI
boundary "ReturnUI" as RUI
control "TransactionService" as TS
entity "Transaction" as T
database CSDL as DB

== Truy cập từ Transaction Details ==
U -> TUI: Kích "Return" button trên transaction
TUI -> TUI: navigateToReturnPage()
TUI -> RUI: Router.navigate([`/transaction/return/${transactionId}`])

== Khởi tạo Return Component ==
RUI -> RUI: ngOnInit()
RUI -> RUI: Extract transactionId từ route params
RUI -> RUI: loadUserInfoAndTransaction()

== Load thông tin user ==
RUI -> TS: getLoggedInUserInfo()
TS -> TS: Get current user từ SecurityContext
TS -> RUI: UserDTO
RUI -> RUI: this.user = res

== Load chi tiết transaction ==
RUI -> TS: getTransactionById(transactionId)
TS -> T: findById(transactionId)
T -> DB: SELECT * FROM transactions WHERE id = ?
DB -> T: Transaction data
T -> TS: Transaction entity
TS -> TS: ModelMapper.map(transaction, TransactionDTO.class)
TS -> RUI: Response{status: 200, transaction: TransactionDTO}
RUI -> RUI: this.transaction = res.transaction
RUI -> RUI: maxReturnQuantity = transaction.totalProducts
RUI -> RUI: returnQuantity = maxReturnQuantity
RUI -> U: Hiển thị form trả hàng với thông tin transaction

@enduml
```

## 2. Sequence Diagram - Điền thông tin trả hàng

```plantuml
@startuml
title Return Product - Điền thông tin trả hàng

actor User as U
boundary "ReturnUI" as RUI

== Kiểm tra quyền trả hàng ==
RUI -> RUI: canReturn()
RUI -> RUI: Kiểm tra user.id === transaction.user.id
RUI -> RUI: Kiểm tra transaction.status === 'COMPLETED'
RUI -> RUI: Kiểm tra transactionType === 'PURCHASE' hoặc 'SALE'
RUI -> U: Hiển thị form nếu có quyền, ngược lại hiển thị error

== Nhập thông tin trả hàng ==
U -> RUI: Thay đổi returnQuantity input
RUI -> RUI: Two-way binding cập nhật returnQuantity
RUI -> RUI: getReturnAmount() tính toán return amount
RUI -> U: Hiển thị return amount được cập nhật

U -> RUI: Nhập returnReason vào textarea
RUI -> RUI: Two-way binding cập nhật returnReason
RUI -> RUI: Hiển thị character count
RUI -> U: Cập nhật UI với số ký tự đã nhập

== Validation real-time ==
RUI -> RUI: Kiểm tra returnQuantity > 0 và <= maxReturnQuantity
RUI -> RUI: Kiểm tra returnReason.trim() không empty
RUI -> RUI: Enable/disable submit button based on validation
RUI -> U: Visual feedback về trạng thái form

@enduml
```

## 3. Sequence Diagram - Submit return request

```plantuml
@startuml
title Return Product - Submit return request

actor User as U
boundary "ReturnUI" as RUI
control "TransactionService" as TS
entity "Transaction" as T
database CSDL as DB

== Submit return request ==
U -> RUI: Kích "Submit Return Request" button
RUI -> RUI: handleReturnSubmit()
RUI -> RUI: Final validation các required fields
RUI -> RUI: isLoading = true

== Tạo return transaction ==
RUI -> TS: returnProduct(returnData)
TS -> T: Tạo Transaction mới với RETURN_TO_SUPPLIER type
TS -> T: Set status = ADMIN_DECIDING
TS -> T: Set product, quantity, description từ returnData
T -> DB: INSERT INTO transactions VALUES (...)
DB -> T: New return transaction created
T -> TS: Saved transaction
TS -> TS: WebSocket broadcast cho real-time update
TS -> RUI: Response{status: 200, message: "Return request submitted successfully"}

== Completion ==
RUI -> RUI: isLoading = false
RUI -> RUI: NotificationService.showSuccess()
RUI -> RUI: Router.navigate(['/transaction'])
RUI -> U: Chuyển về trang danh sách giao dịch với success notification

@enduml
```

## 4. Sequence Diagram - Cancel return

```plantuml
@startuml
title Return Product - Cancel return

actor User as U
boundary "ReturnUI" as RUI

== Cancel return ==
U -> RUI: Kích "Cancel" button
RUI -> RUI: cancelReturn()
RUI -> RUI: Router.navigate(['/transaction', transactionId])
RUI -> U: Quay về trang transaction details

@enduml
```

## 5. Sequence Diagram - Error handling và validation

```plantuml
@startuml
title Return Product - Error handling và validation

actor User as U
boundary "ReturnUI" as RUI
control "TransactionService" as TS

== Client-side validation errors ==
U -> RUI: Kích submit với dữ liệu không hợp lệ
RUI -> RUI: Kiểm tra returnReason.trim() empty
RUI -> RUI: NotificationService.showError("Please provide a reason for return")
RUI -> U: Hiển thị error message

RUI -> RUI: Kiểm tra returnQuantity <= 0 hoặc > maxReturnQuantity
RUI -> RUI: NotificationService.showError("Return quantity must be between 1 and max")
RUI -> U: Hiển thị validation error

== Authorization errors ==
RUI -> RUI: canReturn() returns false
RUI -> RUI: NotificationService.showError("You are not authorized to return this transaction")
RUI -> U: Hiển thị authorization error

== Server-side errors ==
RUI -> TS: returnProduct(returnData) với invalid data
TS -> RUI: Error response
RUI -> RUI: isLoading = false
RUI -> RUI: NotificationService.showError(error message)
RUI -> U: Hiển thị server error message

== Network errors ==
RUI -> TS: API call network timeout
TS -> RUI: Network error
RUI -> RUI: isLoading = false
RUI -> RUI: NotificationService.showError("Unable to submit return request")
RUI -> U: Hiển thị network error

@enduml
```

## 6. Sequence Diagram - Currency formatting và calculations

```plantuml
@startuml
title Return Product - Currency formatting và calculations

actor User as U
boundary "ReturnUI" as RUI

== Display formatting ==
RUI -> RUI: formatCurrency(amount)
RUI -> RUI: Intl.NumberFormat('vi-VN') với currency: 'VND'
RUI -> U: Hiển thị giá tiền theo format Việt Nam

== Return amount calculation ==
U -> RUI: Thay đổi returnQuantity
RUI -> RUI: getReturnAmount()
RUI -> RUI: pricePerItem = totalPrice / totalProducts
RUI -> RUI: returnAmount = pricePerItem * returnQuantity
RUI -> RUI: formatCurrency(returnAmount)
RUI -> U: Hiển thị return amount được cập nhật real-time

@enduml
```

## Đặc điểm chính của hệ thống

### 1. Authorization và Security
- Chỉ người mua hoặc ADMIN mới có thể trả hàng
- Chỉ giao dịch COMPLETED mới được trả
- Chỉ PURCHASE và SALE transactions có thể return

### 2. User Experience
- Real-time validation và feedback
- Currency formatting theo chuẩn Việt Nam
- Character counter cho textarea
- Loading states during API calls
- Auto-calculate return amount

### 3. Data Validation
- Required field validation (reason, quantity)
- Quantity range validation (1 to maxReturnQuantity)
- Maximum character limit cho reason (1000 chars)
- Authorization checks

### 4. Business Logic
- Default return quantity = full quantity
- Return amount calculation based on unit price
- Create new RETURN_TO_SUPPLIER transaction
- Set initial status = ADMIN_DECIDING

### 5. Integration Features
- Router navigation với transaction ID
- WebSocket real-time updates
- Notification service cho user feedback
- Form state management với two-way binding

### 6. Error Handling
- Client-side validation trước server call
- Server error handling với user-friendly messages
- Network error handling
- Authorization error handling
- Loading state management