# Biểu đồ trình tự - Use case: Xem thống kê (Dashboard)

## Mô tả
Use case này cho phép người dùng xem các thống kê về giao dịch thông qua dashboard với các biểu đồ trực quan: số lượng giao dịch theo loại, tổng giá trị giao dịch theo loại, và thống kê giao dịch theo ngày trong tháng được chọn.

## 1. Sequence Diagram - Khởi tạo Dashboard và Load thống kê tổng quan

```plantuml
@startuml
title Dashboard Initialization - Load tổng quan thống kê

actor User as U
boundary "DashboardUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "Product" as P
entity "User" as UserE
entity "Supplier" as S
database MySQL as DB

== Khởi tạo Dashboard ==
U -> UI: Truy cập trang Dashboard
UI -> UI: Kiểm tra authentication
UI -> UI: Khởi tạo component properties
note over UI: Initialize:\n- transactions: []\n- transactionTypeData: []\n- transactionAmountData: []\n- monthlyTransactionData: []

== Load tất cả giao dịch để thống kê ==
UI -> TC: getAllTransactions(userId = -1)
note right: Load tất cả transactions\nkhông có filter điều kiện
TC -> T: findAll()
T -> DB: SELECT transactions LEFT JOIN products, users, suppliers
DB -> T: All transactions with related data

T -> P: getProductInfo() for each transaction
P -> T: Product details
T -> UserE: getUserInfo() for each transaction
UserE -> T: User details
T -> S: getSupplierInfo() for each transaction
S -> T: Supplier details

T -> TC: List<TransactionDTO> - All transactions
TC -> UI: Response{transactions: allTransactions}

== Xử lý dữ liệu cho biểu đồ ==
UI -> UI: processChartData()
note over UI: Xử lý 2 loại thống kê:
note over UI: 1. Đếm số lượng giao dịch theo type
note over UI: 2. Tính tổng giá trị theo type

loop for each transaction
    UI -> UI: Count by transactionType
    note right: typeCounts[type] = count + 1
    UI -> UI: Sum amount by transactionType  
    note right: amountByType[type] = sum + totalPrice
end

UI -> UI: Generate transactionTypeData[]
note over UI: [{name: "PURCHASE", value: count},\n{name: "SALE", value: count},\n{name: "RETURN_TO_SUPPLIER", value: count}]

UI -> UI: Generate transactionAmountData[]
note over UI: [{name: "PURCHASE", value: totalAmount},\n{name: "SALE", value: totalAmount},\n{name: "RETURN_TO_SUPPLIER", value: totalAmount}]

UI -> U: Hiển thị Dashboard với 2 biểu đồ
note right: 1. Bar chart - Transaction counts by type\n2. Pie chart - Total amounts by type

@enduml
```

## 2. Sequence Diagram - Load thống kê theo tháng/năm cụ thể

```plantuml
@startuml
title Monthly Statistics - Load thống kê theo tháng

actor User as U
boundary "DashboardUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
entity "Product" as P
entity "User" as UserE
entity "Supplier" as S
database MySQL as DB

== User chọn tháng/năm và load data ==
U -> UI: Chọn month và year từ dropdown
U -> UI: Click "Show Monthly Data" button

UI -> UI: Validate selectedMonth và selectedYear
alt Valid month/year selected
    UI -> TC: getTransactionsByMonthAndYear(month, year)
    TC -> T: findAllByMonthAndYear(month, year)
    T -> DB: SELECT transactions WHERE YEAR(createdAt) = ? AND MONTH(createdAt) = ?
    
    alt Có dữ liệu trong tháng/năm
        DB -> T: Monthly transactions
        T -> P: getProductInfo() for monthly transactions
        P -> T: Product details
        T -> UserE: getUserInfo() for monthly transactions
        UserE -> T: User details
        T -> S: getSupplierInfo() for monthly transactions  
        S -> T: Supplier details
        
        T -> TC: List<TransactionDTO> for specific month/year
        TC -> UI: Response{transactions: monthlyTransactions}
        
        == Xử lý dữ liệu monthly ==
        UI -> UI: Update transactions property
        UI -> UI: processChartData()
        note over UI: Cập nhật lại 2 biểu đồ overview\nvới dữ liệu của tháng đã chọn
        
        UI -> UI: processMonthlyData(monthlyTransactions)
        note over UI: Tạo biểu đồ thống kê theo ngày trong tháng
        
        loop for each transaction in month
            UI -> UI: Extract day from createdAt
            UI -> UI: Accumulate dailyTotals by day
            note right: dailyTotals[day] = sum + totalPrice
        end
        
        UI -> UI: Generate monthlyTransactionData[]
        note over UI: [{name: "Day 1", value: totalForDay1},\n{name: "Day 2", value: totalForDay2},\n...]
        
        UI -> U: Update Dashboard với 3 biểu đồ
        note right: 1. Bar chart - Counts by type (monthly)\n2. Pie chart - Amounts by type (monthly)\n3. Bar chart - Daily totals in month
        
    else Không có dữ liệu
        DB -> T: Empty result
        T -> TC: Empty list
        TC -> UI: Response{transactions: []}
        UI -> UI: Clear all chart data
        UI -> U: Hiển thị "No data for selected month/year"
    end
    
else Invalid month/year
    UI -> U: Không thực hiện gì (exit function)
end

@enduml
```

## 3. Sequence Diagram - Tương tác với biểu đồ và cập nhật real-time

```plantuml
@startuml
title Chart Interactions và Real-time Updates

actor User as U
boundary "DashboardUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
database MySQL as DB

== Tương tác với biểu đồ ==
U -> UI: Hover/Click trên biểu đồ elements
UI -> U: Hiển thị tooltip với chi tiết data
note right: Ngx-charts tự động handle\ntooltip và legend interactions

U -> UI: Toggle legend items
UI -> UI: Filter/highlight chart data
UI -> U: Update biểu đồ display

== Refresh data manually ==
U -> UI: Click refresh hoặc reload page
UI -> TC: getAllTransactions(-1)
TC -> T: findAll() với updated data
T -> DB: SELECT latest transactions
DB -> T: Current transaction data
T -> TC: Updated transaction list
TC -> UI: Fresh data
UI -> UI: processChartData() with new data
UI -> U: Updated charts với dữ liệu mới

== Reset về view tổng quan ==
U -> UI: Clear month/year selection
U -> UI: Click load default data
UI -> UI: Reset selectedMonth = ''
UI -> UI: Reset selectedYear = ''
UI -> UI: loadTransactions() - load all data
UI -> TC: getAllTransactions(-1)
TC -> T: findAll()
T -> DB: SELECT all transactions
DB -> T: All transaction data
T -> TC: Complete dataset
TC -> UI: All transactions
UI -> UI: processChartData() - generate overview
UI -> UI: Clear monthlyTransactionData
UI -> U: Dashboard reset về tổng quan

@enduml
```

## 4. Sequence Diagram - Error Handling và Performance

```plantuml
@startuml
title Error Handling và Performance Optimization

actor User as U
boundary "DashboardUI" as UI
control "TransactionController" as TC
entity "Transaction" as T
database MySQL as DB

== Error Handling ==
U -> UI: Truy cập Dashboard
UI -> TC: getAllTransactions(-1)
TC -> T: findAll()

alt Database connection error
    T -> DB: SELECT transactions
    DB -> T: Connection timeout/error
    T -> TC: Database exception
    TC -> UI: Error response
    UI -> U: Hiển thị "Unable to load data. Please try again."
    
else Authentication error
    TC -> UI: 401/403 response
    UI -> U: Redirect to login page
    
else Large dataset performance
    T -> DB: SELECT with large result set
    DB -> T: Large dataset (slow query)
    T -> TC: Large transaction list
    TC -> UI: Large response (slow)
    UI -> UI: Loading indicator during processing
    UI -> UI: processChartData() - heavy computation
    UI -> U: Charts rendered (sau loading)
    note right: Consider pagination\nor data aggregation\nfor large datasets
end

== Performance Optimization ==
UI -> UI: Implement data caching
note over UI: Cache processed chart data\nto avoid recomputation
UI -> UI: Debounce user interactions
note over UI: Delay API calls khi user\nthay đổi filters nhanh
UI -> UI: Lazy loading cho monthly data
note over UI: Chỉ load monthly data\nkhi user request cụ thể

== Memory Management ==
UI -> UI: Clear old chart data
note over UI: Prevent memory leaks\nkhi switch between views
UI -> UI: Optimize chart rendering
note over UI: Use ngx-charts efficiently\nvới proper change detection

@enduml
```

## Các trường hợp đặc biệt

### 1. Data Processing Logic
```typescript
// Xử lý dữ liệu cho biểu đồ
processChartData(): void {
    const typeCounts: { [key: string]: number } = {};
    const amountByType: { [key: string]: number } = {};

    this.transactions.forEach((transaction) => {
        const type = transaction.transactionType;
        typeCounts[type] = (typeCounts[type] || 0) + 1;
        amountByType[type] = (amountByType[type] || 0) + transaction.totalPrice;
    });

    // Generate chart data arrays
    this.transactionTypeData = Object.keys(typeCounts).map((type) => ({
        name: type, value: typeCounts[type]
    }));
    
    this.transactionAmountData = Object.keys(amountByType).map((type) => ({
        name: type, value: amountByType[type]
    }));
}
```

### 2. Monthly Data Processing
```typescript
// Xử lý dữ liệu theo ngày trong tháng
processMonthlyData(transactions: any[]): void {
    const dailyTotals: { [key: string]: number } = {};

    transactions.forEach((transaction) => {
        const day = new Date(transaction.createdAt).getDate().toString();
        dailyTotals[day] = (dailyTotals[day] || 0) + transaction.totalPrice;
    });

    this.monthlyTransactionData = Object.keys(dailyTotals).map((day) => ({
        name: `Day ${day}`, value: dailyTotals[day]
    }));
}
```

### 3. Chart Configuration
- **Bar Charts**: Vertical bars với labels trên trục X và Y
- **Pie Chart**: Doughnut style với legends và labels
- **Responsive Design**: Charts adapt theo screen size
- **Animations**: Smooth transitions khi data changes

### 4. Performance Considerations
- **Data Caching**: Cache chart data để tránh recomputation
- **Lazy Loading**: Chỉ load monthly data khi cần thiết
- **Debouncing**: Delay API calls khi user thay đổi selections
- **Memory Management**: Clear old data để tránh memory leaks

### 5. Error States
- **No Data**: Hiển thị empty state với helpful message
- **Loading States**: Show loading indicators during API calls
- **Network Errors**: Retry mechanisms và error notifications
- **Authentication Errors**: Redirect to login

## Tích hợp với hệ thống
- **Role-based Access**: Dashboard có thể hiển thị data khác nhau theo role
- **Real-time Updates**: Có thể tích hợp WebSocket để update charts real-time
- **Export Functionality**: Có thể thêm export charts as images/PDF
- **Drill-down**: Click vào chart elements để xem chi tiết transactions