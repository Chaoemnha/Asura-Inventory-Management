# Biểu đồ trình tự - Hệ thống Quản lý Giao dịch

```plantuml
@startuml
!theme plain
skinparam backgroundColor White
skinparam sequenceMessageAlign center

actor User
boundary "Giao dịch UI" as TransactionUI
boundary "Chi tiết Giao dịch UI" as DetailsUI
boundary "Return UI" as ReturnUI
control "Transaction Controller" as TransactionController
entity "Transaction Entity" as TransactionEntity
database "Database" as CSDL

== Quy trình xem danh sách giao dịch ==
User -> TransactionUI: Truy cập trang giao dịch
TransactionUI -> TransactionController: GET /api/transactions/all
TransactionController -> CSDL: Truy vấn danh sách transactions
CSDL --> TransactionController: Trả về dữ liệu
TransactionController --> TransactionUI: Trả về danh sách giao dịch
TransactionUI -> User: Hiển thị bảng giao dịch với filters

== Quy trình xem chi tiết giao dịch ==
User -> TransactionUI: Click "Xem chi tiết"
TransactionUI -> DetailsUI: Navigate to /transaction/:id
User -> DetailsUI: Xem thông tin chi tiết
DetailsUI -> TransactionController: GET /api/transactions/:id
TransactionController -> CSDL: Truy vấn transaction theo ID
CSDL --> TransactionController: Trả về transaction data
TransactionController -> TransactionEntity: Tạo entity với đầy đủ thông tin
TransactionEntity -> TransactionEntity: Load Product, User, Supplier info
TransactionController --> DetailsUI: Trả về transaction details
DetailsUI -> User: Hiển thị thông tin chi tiết

== Quy trình cập nhật trạng thái giao dịch ==
User -> DetailsUI: Click action button (Confirm/Reject)
DetailsUI -> TransactionController: PUT /api/transactions/update/:id
TransactionController -> CSDL: Kiểm tra quyền và trạng thái hiện tại
CSDL --> TransactionController: Xác nhận hợp lệ
TransactionController -> TransactionEntity: Cập nhật trạng thái
TransactionController -> CSDL: Lưu thay đổi
CSDL --> TransactionController: Xác nhận cập nhật
TransactionController -> TransactionController: Gửi WebSocket broadcast
TransactionController --> DetailsUI: Trả về kết quả thành công
DetailsUI -> DetailsUI: Refresh transaction data
DetailsUI -> User: Hiển thị trạng thái mới

== Quy trình trả hàng ==
User -> DetailsUI: Click "Trả hàng"
DetailsUI -> ReturnUI: Navigate to /transaction/return/:id
User -> ReturnUI: Xem thông tin giao dịch gốc
ReturnUI -> TransactionController: GET /api/transactions/:id
TransactionController -> CSDL: Lấy transaction gốc
CSDL --> TransactionController: Trả về transaction data
TransactionController --> ReturnUI: Hiển thị thông tin return form

User -> ReturnUI: Nhập số lượng và lý do
User -> ReturnUI: Click "Gửi yêu cầu trả hàng"
ReturnUI -> ReturnUI: Validate form data
ReturnUI -> TransactionController: POST /api/transactions/return
TransactionController -> TransactionController: Kiểm tra quyền trả hàng
TransactionController -> CSDL: Kiểm tra transaction status
CSDL --> TransactionController: Xác nhận có thể trả
TransactionController -> TransactionEntity: Tạo transaction RETURN_TO_SUPPLIER
TransactionEntity -> TransactionEntity: Set thông tin return
TransactionController -> CSDL: Lưu transaction return
CSDL --> TransactionController: Xác nhận tạo thành công
TransactionController -> TransactionController: Gửi WebSocket broadcast
TransactionController --> ReturnUI: Trả về thành công
ReturnUI -> TransactionUI: Redirect về danh sách
ReturnUI -> User: Thông báo gửi yêu cầu thành công

== Quy trình tìm kiếm và lọc ==
User -> TransactionUI: Sử dụng advanced search
User -> TransactionUI: Nhập tiêu chí tìm kiếm
User -> TransactionUI: Click "Tìm kiếm"
TransactionUI -> TransactionController: GET /api/transactions/all với params
TransactionController -> CSDL: Truy vấn với điều kiện lọc
CSDL --> TransactionController: Trả về kết quả filtered
TransactionController --> TransactionUI: Danh sách đã lọc
TransactionUI -> User: Hiển thị kết quả tìm kiếm

@enduml
```

## Mô tả quy trình

### 1. Xem danh sách giao dịch
- User truy cập trang giao dịch
- Hệ thống load và hiển thị danh sách với pagination
- Hỗ trợ tìm kiếm và lọc theo nhiều tiêu chí

### 2. Xem chi tiết giao dịch
- User click vào giao dịch để xem chi tiết
- Hệ thống load thông tin đầy đủ (Product, User, Supplier)
- Hiển thị các action buttons tùy theo role và trạng thái

### 3. Cập nhật trạng thái
- User thực hiện action (Confirm, Reject, etc.)
- Hệ thống validate quyền và cập nhật trạng thái
- Gửi WebSocket notification và refresh UI

### 4. Trả hàng
- User yêu cầu trả hàng từ chi tiết giao dịch
- Điền form với số lượng và lý do
- Hệ thống tạo transaction RETURN_TO_SUPPLIER mới
- Thông báo thành công và quay về danh sách

### 5. Tìm kiếm và lọc
- Hỗ trợ tìm kiếm theo ID, type, status, tên sản phẩm
- Lọc theo khoảng thời gian
- Real-time update với WebSocket integration