# Biểu đồ trình tự - Chức năng Nhập hàng (Stock Staff)

```plantuml
@startuml
!theme plain
skinparam backgroundColor White
skinparam sequenceMessageAlign center

actor "Stock Staff" as Staff
boundary "Purchase UI" as PurchaseUI
control "Transaction Controller" as TransactionController
entity "Transaction Entity" as TransactionEntity
database "Database" as CSDL

== Quy trình nhập hàng thành công ==
Staff -> PurchaseUI: Truy cập trang nhập hàng
PurchaseUI -> TransactionController: Lấy danh sách sản phẩm và nhà cung cấp
TransactionController -> CSDL: Truy vấn products và suppliers
CSDL --> TransactionController: Trả về danh sách
TransactionController --> PurchaseUI: Trả về dữ liệu
PurchaseUI -> Staff: Hiển thị form nhập hàng

Staff -> PurchaseUI: Chọn sản phẩm
PurchaseUI -> PurchaseUI: Cập nhật thông tin sản phẩm
PurchaseUI -> PurchaseUI: Tính toán giá tối đa

Staff -> PurchaseUI: Chọn nhà cung cấp
PurchaseUI -> TransactionController: getUserBySupplierId()
TransactionController -> CSDL: Truy vấn users theo supplierId
CSDL --> TransactionController: Trả về danh sách users
TransactionController --> PurchaseUI: Trả về availableSenders
PurchaseUI -> Staff: Hiển thị danh sách người gửi

Staff -> PurchaseUI: Nhập thông tin (số lượng, giá, mô tả)
Staff -> PurchaseUI: Chọn người gửi (nếu có)
Staff -> PurchaseUI: Click "Nhập hàng"

PurchaseUI -> PurchaseUI: Validate dữ liệu form
PurchaseUI -> TransactionController: POST /api/transactions/purchase
TransactionController -> TransactionController: Kiểm tra dữ liệu
TransactionController -> CSDL: Tìm Product theo ID
CSDL --> TransactionController: Trả về Product
TransactionController -> CSDL: Tìm Supplier theo ID
CSDL --> TransactionController: Trả về Supplier
TransactionController -> CSDL: Tìm User hiện tại
CSDL --> TransactionController: Trả về User
TransactionController -> TransactionEntity: Tạo Transaction mới
TransactionEntity -> TransactionEntity: Set thông tin giao dịch
TransactionController -> CSDL: Lưu Transaction
CSDL --> TransactionController: Xác nhận lưu thành công
TransactionController -> TransactionController: Gửi WebSocket broadcast
TransactionController --> PurchaseUI: Trả về thành công
PurchaseUI -> PurchaseUI: Reset form
PurchaseUI -> Staff: Hiển thị thông báo thành công

@enduml
```

## Mô tả quy trình

### Quy trình nhập hàng của Stock Staff
1. **Khởi tạo**: Truy cập trang nhập hàng và lấy danh sách sản phẩm/nhà cung cấp
2. **Chọn sản phẩm**: Hiển thị thông tin chi tiết và tính giá tối đa
3. **Chọn nhà cung cấp**: Lấy danh sách người gửi thuộc nhà cung cấp đó
4. **Nhập thông tin**: Số lượng, giá nhập, mô tả và người gửi
5. **Xử lý giao dịch**: Validate, tạo Transaction entity và lưu vào database
6. **Thông báo**: Gửi WebSocket broadcast và hiển thị kết quả
7. **Hoàn thành**: Reset form và sẵn sàng cho giao dịch tiếp theo