# Biểu đồ trình tự - Use case: Tìm kiếm giao dịch qua điều kiện lọc

## Mô tả
Use case này cho phép người dùng tìm kiếm giao dịch theo nhiều điều kiện khác nhau như: loại giao dịch, trạng thái, tên sản phẩm, khoảng thời gian, và người dùng. Hệ thống cũng hỗ trợ tìm kiếm nhanh theo ID giao dịch và phân trang kết quả.

## 1. Sequence Diagram - Khởi tạo và Load danh sách ban đầu

```plantuml
@startuml
title Load Transaction List - Khởi tạo trang

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Khởi tạo và Authentication ==
U -> UI: Truy cập trang Transaction List
UI -> UI: Kiểm tra authentication
UI -> TC: getLoggedInUserInfo()
TC -> UserE: getCurrentUser()
UserE -> DB: SELECT user WHERE token = ?
DB -> UserE: User data với role
UserE -> TC: UserDTO
TC -> UI: User information với role

== Load danh sách giao dịch ban đầu ==
UI -> TC: getAllTransactions(userId=-1, default filters)
TC -> T: findAllWithPagination()
T -> DB: SELECT transactions LEFT JOIN products, users, suppliers
note right: Áp dụng role-based filtering:\n- ADMIN: Xem tất cả\n- STOCKSTAFF: Chỉ giao dịch của họ\n- CUSTOMER: Chỉ giao dịch của họ\n- SUPPLIER: Giao dịch liên quan supplier
T -> P: getProductInfo()
T -> UserE: getUserInfo()
T -> S: getSupplierInfo()
DB -> T: Transaction list với thông tin liên quan
T -> TC: List<TransactionDTO> với pagination info
TC -> UI: Response{transactions, totalPages, currentPage}
UI -> U: Hiển thị danh sách giao dịch với pagination

@enduml
```

## 2. Sequence Diagram - Tìm kiếm nhanh theo ID

```plantuml
@startuml
title Quick Search by Transaction ID

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Tìm kiếm nhanh theo ID ==
U -> UI: Nhập transaction ID và click "Go!"
UI -> UI: Validate input (không empty)
alt Valid input
    UI -> TC: getTransactionById(transactionId)
    TC -> T: findById(transactionId)
    T -> DB: SELECT transaction WHERE id = ?
    
    alt Transaction found
        DB -> T: Transaction data
        T -> P: getProductDetails()
        P -> DB: SELECT product info
        DB -> P: Product data
        P -> T: Product info
        
        T -> UserE: getUserDetails()
        UserE -> DB: SELECT user info
        DB -> UserE: User data
        UserE -> T: User info
        
        T -> S: getSupplierDetails()
        S -> DB: SELECT supplier info
        DB -> S: Supplier data
        S -> T: Supplier info
        
        T -> TC: TransactionDTO với đầy đủ thông tin
        TC -> UI: Response{status: 200, transaction}
        UI -> UI: Set transactions = [single transaction]
        UI -> UI: Set isSearching = true
        UI -> U: Hiển thị 1 kết quả với button "Cancel"
        
    else Transaction not found
        DB -> T: null
        T -> TC: NotFoundException
        TC -> UI: Response{status: 404, message: "Transaction not found"}
        UI -> U: Hiển thị thông báo lỗi "Transaction not found"
    end
    
else Invalid input
    UI -> U: Hiển thị lỗi "Please enter transaction ID"
end

@enduml
```

## 3. Sequence Diagram - Advanced Search với điều kiện lọc

```plantuml
@startuml
title Advanced Search với nhiều điều kiện

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Advanced Search Flow ==
U -> UI: Click "Show Advanced Search"
UI -> U: Hiển thị form advanced search

U -> UI: Nhập các điều kiện lọc
note left: Điều kiện gồm:\n- Transaction Type\n- Status\n- Product Name\n- From Date\n- To Date

U -> UI: Click "Search" button
UI -> UI: Thu thập tất cả filter parameters
UI -> TC: getAllTransactions(userId, searchType, searchStatus, searchProductName, searchFromDate, searchToDate, page, size)

TC -> T: searchTransactions(filters)
T -> DB: Complex query với JOIN và WHERE conditions
note right: SELECT t FROM Transaction t\nLEFT JOIN t.product p\nLEFT JOIN t.user u\nLEFT JOIN t.supplier s\nWHERE conditions...

alt Có kết quả
    DB -> T: Filtered transaction list
    T -> P: getProductInfo() for each transaction
    P -> T: Product details
    T -> UserE: getUserInfo() for each transaction  
    UserE -> T: User details
    T -> S: getSupplierInfo() for each transaction
    S -> T: Supplier details
    
    T -> TC: List<TransactionDTO> với pagination
    TC -> UI: Response{transactions, totalPages, pagination info}
    UI -> U: Hiển thị kết quả đã lọc với pagination
    
else Không có kết quả
    DB -> T: Empty result set
    T -> TC: Empty list
    TC -> UI: Response{transactions: [], totalPages: 0}
    UI -> U: Hiển thị "No transactions found"
end

@enduml
```

## 4. Sequence Diagram - Clear Filters và Reset

```plantuml
@startuml
title Clear Filters và Reset Search

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Clear Filters Flow ==
U -> UI: Click "Clear" hoặc "Hide Advanced Search"
UI -> UI: Reset tất cả filter variables
note over UI: searchType = ''\nsearchStatus = ''\nsearchProductName = ''\nsearchFromDate = ''\nsearchToDate = ''
UI -> UI: Set showAdvancedSearch = false

UI -> TC: getAllTransactions(userId, no filters)
TC -> T: findAllWithDefaultConditions()
T -> DB: SELECT transactions với default conditions
DB -> T: Default transaction list

T -> P: getProductInfo()
P -> T: Product details
T -> UserE: getUserInfo()
UserE -> T: User details  
T -> S: getSupplierInfo()
S -> T: Supplier details

T -> TC: List<TransactionDTO>
TC -> UI: Default transaction list
UI -> U: Hiển thị lại danh sách gốc

@enduml
```

## 5. Sequence Diagram - Cancel Search và Pagination

```plantuml
@startuml
title Cancel Search và Pagination

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Cancel Search by ID ==
alt User đang search by ID
    U -> UI: Click "Cancel" button
    UI -> UI: Clear searchId
    UI -> UI: Set isSearching = false
    UI -> UI: Reset currentPage = 1
    UI -> TC: getAllTransactions(userId, current filters)
    TC -> T: findAllWithCurrentFilters()
    T -> DB: SELECT transactions với current filters
    DB -> T: Transaction list
    T -> TC: List<TransactionDTO>
    TC -> UI: Full transaction list
    UI -> U: Quay về danh sách đầy đủ
end

== Pagination ==
alt User click page navigation
    U -> UI: Click page number hoặc next/previous
    UI -> UI: Update currentPage
    
    alt Đang search by ID (single result)
        UI -> U: Không reload (chỉ 1 kết quả)
    else Normal search hoặc default view
        UI -> TC: getAllTransactions(userId, current filters, newPage)
        TC -> T: findWithPagination(page, size)
        T -> DB: SELECT transactions với OFFSET và LIMIT
        DB -> T: Transaction page data
        
        T -> P: getProductInfo()
        P -> T: Product details
        T -> UserE: getUserInfo()
        UserE -> T: User details
        T -> S: getSupplierInfo()
        S -> T: Supplier details
        
        T -> TC: Page data
        TC -> UI: New page data
        UI -> U: Hiển thị page mới
    end
end

@enduml
```

## 6. Sequence Diagram - Export và Navigate to Details

```plantuml
@startuml
title Export Results và Navigate to Details

actor User as U
boundary "TransactionSearchUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "User" as UserE
entity "Product" as P
entity "Supplier" as S
database MySQL as DB

== Export Filtered Results (Admin only) ==
alt Admin user click "Export"
    U -> UI: Click Export button
    UI -> UI: Kiểm tra isAdmin()
    alt User is Admin
        UI -> TC: exportTransactions(type, current filters)
        TC -> T: findAllByCondition(filters)
        T -> DB: SELECT ALL transactions matching filters
        DB -> T: Complete filtered dataset
        
        T -> P: getProductInfo() for all
        P -> T: All product details
        T -> UserE: getUserInfo() for all
        UserE -> T: All user details
        T -> S: getSupplierInfo() for all
        S -> T: All supplier details
        
        T -> TC: Complete dataset
        TC -> TC: Generate report file (PDF/Excel)
        TC -> UI: File download response
        UI -> U: Download file thành công
    else User is not Admin
        UI -> U: Hiển thị "Access denied"
    end
end

== Navigate to Transaction Details ==
U -> UI: Click "View Details" trên transaction
UI -> UI: navigateToTransactionDetailsPage(transactionId)
UI -> U: Chuyển đến trang Transaction Details
note right: Chuyển sang use case\n"Xem chi tiết giao dịch"

@enduml
```

## Các trường hợp đặc biệt

### 1. Role-based Access Control trong Search
```typescript
// Logic filtering dựa trên role
if (isAdmin()) {
    userId = -1; // Xem tất cả transactions
} else {
    userId = currentUser.id; // Chỉ xem transactions của mình
}

// Trong database query
// ADMIN: không filter by userId
// STOCKSTAFF: filter by user.id = userId OR sender.id = userId  
// CUSTOMER: filter by user.id = userId
// SUPPLIER: filter by supplier.id = currentUser.supplier.id OR status = SENDER_DECIDING
```

### 2. Search Modes
- **Quick Search**: Tìm theo ID, chỉ trả về 1 kết quả
- **Advanced Search**: Tìm theo nhiều điều kiện, có pagination
- **Default View**: Hiển thị tất cả theo role với pagination

### 3. Filter Parameters
```typescript
interface SearchFilters {
    searchType?: 'PURCHASE' | 'SALE' | 'RETURN_TO_SUPPLIER';
    searchStatus?: 'PENDING' | 'COMPLETED' | 'CANCELED' | 'SENDER_DECIDING' | 'ADMIN_DECIDING';
    searchProductName?: string;  // LIKE search
    searchFromDate?: string;     // >= date
    searchToDate?: string;       // <= date
    userId?: number;             // Role-based
    page?: number;               // Pagination
    size?: number;               // Page size
}
```

### 4. Database Query Optimization
- **JOIN Strategy**: LEFT JOIN với product, user, supplier để lấy thông tin liên quan
- **Indexing**: Index trên createdAt, status, transactionType để tăng tốc query
- **Pagination**: Sử dụng OFFSET và LIMIT để giới hạn kết quả
- **Date Filtering**: Sử dụng LocalDateTime comparison

### 5. UI State Management
```typescript
// Search states
isSearching: boolean = false;           // Đang search by ID
showAdvancedSearch: boolean = false;    // Hiển thị advanced form
searchId: string = '';                  // ID search input
currentPage: number = 1;                // Current page number
totalPages: number = 0;                 // Total pages
transactions: Transaction[] = [];       // Result list
```

### 6. Error Handling
- **Empty Search**: Validate input trước khi search
- **No Results**: Hiển thị friendly message
- **Access Denied**: Ẩn transactions không có quyền truy cập
- **Network Error**: Retry mechanism và error notifications

### 7. Performance Considerations
- **Debouncing**: Delay search khi user đang typing
- **Caching**: Cache recent search results
- **Lazy Loading**: Load thêm data khi scroll xuống
- **Query Optimization**: Minimize database calls

## Tích hợp với các Use Case khác
- **Transaction Details**: Navigate khi click "View Details"
- **Export Reports**: Admin có thể export filtered results
- **Real-time Updates**: WebSocket updates cho transaction list
- **Role Management**: Tích hợp với authentication system

## Business Rules
1. **ADMIN**: Có thể search tất cả transactions, có button Export
2. **STOCKSTAFF**: Chỉ search transactions mà họ tạo hoặc được assign
3. **CUSTOMER**: Chỉ search transactions của chính họ
4. **SUPPLIER**: Search transactions liên quan đến supplier của họ
5. **Pagination**: Default 10 items per page, có thể thay đổi
6. **Date Range**: Validation để đảm bảo fromDate <= toDate
7. **Product Search**: Case-insensitive LIKE search