# Biểu đồ trình tự - Use case: Quản lý nhà cung cấp (Supplier Management)

## Mô tả
Use case này cho phép Admin quản lý các nhà cung cấp trong hệ thống, bao gồm: xem danh sách, thêm mới, chỉnh sửa và xóa nhà cung cấp. Hệ thống cũng hỗ trợ cập nhật real-time thông qua WebSocket và validation các trường dữ liệu unique (name, email, phone).

## 1. Sequence Diagram - Khởi tạo và Load danh sách nhà cung cấp

```plantuml
@startuml
title Supplier Management - Load danh sách nhà cung cấp

actor Admin as A
boundary "SupplierManagementUI" as UI
control "SupplierController" as SC
entity "Supplier" as S
database MySQL as DB

== Khởi tạo và Authentication ==
A -> UI: Truy cập trang Supplier Management
UI -> UI: Kiểm tra authentication và ADMIN role
note over UI: @PreAuthorize("hasAuthority('ADMIN')")\nchỉ ADMIN mới có quyền quản lý

== Khởi tạo component ==
UI -> UI: Initialize component properties
note over UI: suppliers: []\nsupplierName, email, phone, address: ''\nisEditing: false\neditingSupplierId: null

== Kết nối WebSocket ==
UI -> UI: Connect to WebSocket service
UI -> UI: Subscribe to WebSocket messages
note over UI: Subscribe để nhận real-time updates\ntừ các admin khác

== Load danh sách suppliers ==
UI -> SC: getAllSuppliers()
SC -> S: findAll(Sort.by(Sort.Direction.DESC, "id"))
S -> DB: SELECT * FROM suppliers ORDER BY id DESC
DB -> S: List of suppliers
S -> SC: List<Supplier>
SC -> UI: Response{status: 200, suppliers: List<SupplierDTO>}
UI -> A: Hiển thị danh sách suppliers với button "Add Product"

@enduml
```

## 2. Sequence Diagram - Thêm nhà cung cấp mới

```plantuml
@startuml
title Add New Supplier - Thêm nhà cung cấp mới

actor Admin as A
boundary "SupplierManagementUI" as UI
control "SupplierController" as SC
entity "Supplier" as S
database MySQL as DB

== Navigate to Add Supplier Page ==
A -> UI: Click "Add Product" button
UI -> UI: navigateToAddSupplierPage()
UI -> A: Chuyển đến trang /add-supplier

== Thêm supplier mới ==
A -> UI: Nhập thông tin supplier (name, email, phone, address)
A -> UI: Click "Add Supplier" button

UI -> UI: Validate required fields
alt Valid input (name và email không empty)
    UI -> SC: addSupplier({name, email, phone, address})
    SC -> S: Kiểm tra duplicate data
    S -> DB: SELECT * FROM suppliers WHERE name = ? OR email = ? OR phone = ?
    
    alt Không có duplicate
        DB -> S: No existing records
        S -> SC: Validation passed
        SC -> S: save(new Supplier)
        S -> DB: INSERT INTO suppliers (name, email, phone, address) VALUES (...)
        DB -> S: Saved supplier với ID
        S -> SC: Supplier entity
        
        == WebSocket broadcast ==
        SC -> SC: Create WebSocket message
        note over SC: message = {\n  "type": "SUPPLIER_ADDED_RENDER",\n  "data": {id, name, email, phone, address}\n}
        SC -> SC: WebSocketHandler.broadcastChanges()
        note right: Broadcast tới tất cả clients\nđang kết nối
        
        SC -> UI: Response{status: 200, message: "Supplier added successfully"}
        UI -> UI: Clear form và show success notification
        UI -> A: Hiển thị thông báo thành công
        
    else Có duplicate (name, email, hoặc phone)
        DB -> S: Existing supplier records
        S -> SC: Duplicate found
        SC -> UI: InvalidCredentialsException("Supplier already exists")
        UI -> A: Hiển thị lỗi "Supplier already exists"
    end
    
else Invalid input (thiếu name hoặc email)
    UI -> A: Hiển thị lỗi validation tương ứng
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "SUPPLIER_ADDED_RENDER"
UI -> UI: addSupplierToList(newSupplier)
UI -> A: Tự động thêm supplier mới vào danh sách

@enduml
```

## 3. Sequence Diagram - Chỉnh sửa nhà cung cấp

```plantuml
@startuml
title Edit Supplier - Chỉnh sửa nhà cung cấp

actor Admin as A
boundary "SupplierManagementUI" as UI
control "SupplierController" as SC
entity "Supplier" as S
database MySQL as DB

== Navigate to Edit Supplier Page ==
A -> UI: Click "Edit" button trên supplier
UI -> UI: navigateToEditSupplierPage(supplierId)
UI -> A: Chuyển đến trang /edit-supplier/{id}

== Load existing supplier data ==
UI -> SC: getSupplierById(supplierId)
SC -> S: findById(supplierId)
S -> DB: SELECT * FROM suppliers WHERE id = ?
alt Supplier tồn tại
    DB -> S: Existing supplier
    S -> SC: Supplier entity
    SC -> UI: Response{status: 200, supplier: SupplierDTO}
    UI -> UI: Pre-fill form với existing data
    UI -> A: Form hiển thị với dữ liệu hiện tại
else Supplier không tồn tại
    DB -> S: null
    S -> SC: NotFoundException("Supplier Not Found")
    SC -> UI: Error response
    UI -> A: Hiển thị lỗi "Supplier not found"
end

== Thực hiện cập nhật ==
A -> UI: Thay đổi thông tin supplier
A -> UI: Click "Update Supplier" button

UI -> UI: Validate input (name không empty)
alt Valid input
    UI -> SC: updateSupplier(supplierId, {name, email, phone, address})
    SC -> S: findById(supplierId)
    S -> DB: SELECT * FROM suppliers WHERE id = ?
    
    alt Supplier tồn tại
        DB -> S: Existing supplier
        S -> SC: Supplier entity
        SC -> S: Update fields conditionally
        note over SC: if (name != null) setName(name)\nif (email != null) setEmail(email)\nif (phone != null) setPhone(phone)\nif (address != null) setAddress(address)
        SC -> S: save(updatedSupplier)
        S -> DB: UPDATE suppliers SET ... WHERE id = ?
        DB -> S: Updated supplier
        S -> SC: Updated supplier entity
        
        == WebSocket broadcast ==
        SC -> SC: Create WebSocket message
        note over SC: message = {\n  "type": "SUPPLIER_UPDATED_RENDER",\n  "data": {id, name, email, phone, address}\n}
        SC -> SC: WebSocketHandler.broadcastChanges()
        
        SC -> UI: Response{status: 200, message: "Supplier Successfully Updated"}
        UI -> A: Hiển thị thông báo cập nhật thành công
        
    else Supplier không tồn tại
        DB -> S: null
        S -> SC: NotFoundException("Supplier Not Found")
        SC -> UI: Error response
        UI -> A: Hiển thị lỗi "Supplier not found"
    end
    
else Invalid input
    UI -> A: Hiển thị lỗi validation
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "SUPPLIER_UPDATED_RENDER"
UI -> UI: updateSupplierInList(updatedSupplier)
UI -> A: Tự động cập nhật supplier trong danh sách

@enduml
```

## 4. Sequence Diagram - Xóa nhà cung cấp

```plantuml
@startuml
title Delete Supplier - Xóa nhà cung cấp

actor Admin as A
boundary "SupplierManagementUI" as UI
control "SupplierController" as SC
entity "Supplier" as S
database MySQL as DB

== Xóa supplier ==
A -> UI: Click "Delete" button trên supplier
UI -> UI: Show confirmation dialog
UI -> A: "Are you sure you want to delete this supplier?"

alt User confirms deletion
    A -> UI: Click "OK" trong confirmation dialog
    UI -> SC: deleteSupplier(supplierId)
    SC -> S: findById(supplierId)
    S -> DB: SELECT * FROM suppliers WHERE id = ?
    
    alt Supplier tồn tại
        DB -> S: Existing supplier
        S -> SC: Supplier entity
        SC -> S: deleteById(supplierId)
        S -> DB: DELETE FROM suppliers WHERE id = ?
        DB -> S: Delete successful
        S -> SC: Deletion confirmed
        
        == WebSocket broadcast ==
        SC -> SC: Create WebSocket message
        note over SC: message = {\n  "type": "SUPPLIER_DELETED_RENDER",\n  "supplierId": supplierId\n}
        SC -> SC: WebSocketHandler.broadcastChanges()
        
        SC -> UI: Response{status: 200, message: "Supplier Successfully Deleted"}
        UI -> UI: Reload suppliers list
        UI -> A: Hiển thị thông báo xóa thành công
        
    else Supplier không tồn tại
        DB -> S: null
        S -> SC: NotFoundException("Supplier Not Found")
        SC -> UI: Error response
        UI -> A: Hiển thị lỗi "Supplier not found"
    end
    
else User cancels deletion
    A -> UI: Click "Cancel" trong confirmation dialog
    UI -> A: Không làm gì, quay về danh sách
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "SUPPLIER_DELETED_RENDER"
UI -> UI: removeSupplierFromList(supplierId)
alt Đang edit supplier bị xóa
    UI -> UI: Check if editingSupplierId === deletedSupplierId
    UI -> UI: cancel() - Reset form
end
UI -> A: Tự động xóa supplier khỏi danh sách

@enduml
```

## 5. Sequence Diagram - WebSocket Real-time Updates và Error Handling

```plantuml
@startuml
title WebSocket Updates và Error Handling

actor Admin as A
boundary "SupplierManagementUI" as UI
control "SupplierController" as SC
entity "Supplier" as S
database MySQL as DB

== WebSocket Message Handling ==
UI -> UI: Receive WebSocket message
UI -> UI: handleWebSocketMessage(message)

alt message.type === "SUPPLIER_UPDATED_RENDER"
    UI -> UI: updateSupplierInList(message.data)
    note over UI: Tìm supplier trong list\nvà cập nhật thông tin
    UI -> A: Supplier được cập nhật real-time
    
else message.type === "SUPPLIER_ADDED_RENDER"
    UI -> UI: addSupplierToList(message.data)
    note over UI: Thêm supplier mới vào list\nnếu chưa tồn tại
    UI -> A: Supplier mới xuất hiện real-time
    
else message.type === "SUPPLIER_DELETED_RENDER"
    UI -> UI: removeSupplierFromList(message.supplierId)
    alt Đang edit supplier bị xóa
        UI -> UI: cancel() - Reset form
        UI -> A: Form được reset do supplier bị xóa
    end
    UI -> A: Supplier biến mất khỏi danh sách
end

== Error Handling ==
alt Network Error
    UI -> SC: API call fails
    SC -> UI: Network timeout/connection error
    UI -> A: Hiển thị "Unable to connect to server"
    
else Authorization Error
    UI -> SC: API call với invalid token
    SC -> UI: 401/403 response
    UI -> A: Redirect to login page
    
else Validation Error - Duplicate Data
    UI -> SC: API call với duplicate name/email/phone
    SC -> S: Check existing records
    S -> DB: Found duplicate entries
    DB -> S: Existing supplier data
    S -> SC: InvalidCredentialsException
    SC -> UI: 400 response với "Supplier already exists"
    UI -> A: Hiển thị "Supplier already exists"
    
else Validation Error - Required Fields
    UI -> UI: Client-side validation fails
    UI -> A: Hiển thị "Supplier name/email is required"
    
else Database Error
    SC -> S: Database operation fails
    S -> DB: Connection/query error
    DB -> S: Database exception
    S -> SC: Database error
    SC -> UI: 500 response
    UI -> A: Hiển thị "Server error occurred"
    
else Foreign Key Constraint Error
    SC -> S: Delete supplier được reference
    S -> DB: DELETE fails due to foreign key
    DB -> S: Foreign key constraint violation
    S -> SC: Constraint violation
    SC -> UI: Error response
    UI -> A: Hiển thị "Cannot delete supplier in use"
end

== Component Cleanup ==
UI -> UI: ngOnDestroy()
UI -> UI: Unsubscribe from WebSocket
UI -> UI: Disconnect WebSocket connection
note over UI: Prevent memory leaks\nkhi component bị destroy

== Form Management ==
alt Edit Mode
    UI -> UI: handleEditSupplier(supplier)
    UI -> UI: Set form data từ selected supplier
    UI -> UI: Navigate to edit page
    
else Add Mode
    UI -> UI: clearForm()
    UI -> UI: Reset tất cả form fields
    UI -> UI: Navigate to add page
    
else Cancel Editing
    UI -> UI: cancel()
    UI -> UI: Reset form state và navigate back
end

@enduml
```

## Các trường hợp đặc biệt

### 1. Authorization và Security
```typescript
// Chỉ ADMIN mới có quyền thao tác
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<Response> addSupplier(@RequestBody @Valid SupplierDTO supplierDTO)

// Frontend kiểm tra role trước khi hiển thị
if (!this.apiService.isAdmin()) {
    // Hide CRUD functionality
}
```

### 2. Unique Constraints Validation
```java
// Backend validation cho 3 trường unique
Optional<Supplier> supplier1 = supplierRepository.findByName(supplierDTO.getName());
Optional<Supplier> supplier2 = supplierRepository.findByPhone(supplierDTO.getPhone());
Optional<Supplier> supplier3 = supplierRepository.findByEmail(supplierDTO.getEmail());

if (supplier1.isPresent() || supplier2.isPresent() || supplier3.isPresent()) {
    throw new InvalidCredentialsException("Supplier already exists");
}
```

### 3. WebSocket Message Types
```typescript
interface SupplierWebSocketMessage {
    type: 'SUPPLIER_ADDED_RENDER' | 'SUPPLIER_UPDATED_RENDER' | 'SUPPLIER_DELETED_RENDER';
    data?: {
        id: string;
        name: string;
        email: string;
        phone: string;
        address: string;
    };
    supplierId?: string; // Cho delete operation
}
```

### 4. Form Data Structure
```typescript
interface SupplierForm {
    supplierName: string;     // Required
    supplierEmail: string;    // Required, unique
    supplierPhone: string;    // Optional, unique if provided
    supplierAddress: string;  // Optional
    isEditing: boolean;
    editingSupplierId: string | null;
}
```

### 5. Navigation Flow
- **List View**: `/supplier` - Hiển thị tất cả suppliers với Edit/Delete buttons
- **Add View**: `/add-supplier` - Form thêm supplier mới
- **Edit View**: `/edit-supplier/{id}` - Form edit với data pre-filled

### 6. Database Schema
```sql
CREATE TABLE suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(255) UNIQUE,
    address TEXT
);
```

### 7. Real-time Synchronization Features
- **Multi-admin Support**: Nhiều admin có thể quản lý suppliers cùng lúc
- **Conflict Resolution**: WebSocket updates giúp sync changes real-time
- **Form State Management**: Reset form nếu supplier đang edit bị xóa bởi admin khác

### 8. Error Scenarios Specific to Suppliers
- **Duplicate Name**: Tên supplier đã tồn tại
- **Duplicate Email**: Email đã được sử dụng
- **Duplicate Phone**: Số điện thoại đã tồn tại
- **Supplier In Use**: Supplier đang được reference bởi transactions/products
- **Required Fields**: Name và email bắt buộc phải có

## Performance Considerations
- **Pagination**: Implement nếu có nhiều suppliers
- **Search/Filter**: Thêm search functionality cho large datasets
- **Lazy Loading**: Load supplier details khi cần thiết
- **Optimistic Updates**: Update UI ngay, rollback nếu API fails

## Business Rules
1. **ADMIN Only**: Chỉ ADMIN mới có quyền CRUD suppliers
2. **Unique Constraints**: Name, email, phone phải unique
3. **Required Fields**: Name và email không được để trống
4. **Real-time Sync**: Tất cả changes được sync real-time
5. **Confirmation**: Xóa supplier cần confirmation
6. **Navigation**: Separate pages cho Add/Edit operations
7. **Form Reset**: Form được reset sau successful operations
8. **Foreign Key Protection**: Không thể xóa supplier đang được sử dụng