# Biểu đồ trình tự - Use case: Quản lý phân loại (Category Management)

## Mô tả
Use case này cho phép Admin quản lý các phân loại sản phẩm trong hệ thống, bao gồm: xem danh sách, thêm mới, chỉnh sửa và xóa phân loại. Hệ thống cũng hỗ trợ cập nhật real-time thông qua WebSocket.

## 1. Sequence Diagram - Khởi tạo và Load danh sách phân loại

```plantuml
@startuml
title Category Management - Load danh sách phân loại

actor Admin as A
boundary "CategoryManagementUI" as UI
control "CategoryController" as CC
entity "Category" as C
database MySQL as DB

== Khởi tạo và Authentication ==
A -> UI: Truy cập trang Category Management
UI -> UI: Kiểm tra authentication và ADMIN role
note over UI: @PreAuthorize("hasAuthority('ADMIN')")\nchỉ ADMIN mới có quyền quản lý

== Khởi tạo component ==
UI -> UI: Initialize component properties
note over UI: categories: []\ncategoryName: ''\nisEditing: false\neditingCategoryId: null

== Kết nối WebSocket ==
UI -> UI: Connect to WebSocket service
UI -> UI: Subscribe to WebSocket messages
note over UI: Subscribe để nhận real-time updates\ntừ các admin khác

== Load danh sách categories ==
UI -> CC: getAllCategories()
CC -> C: findAll(Sort.by(Sort.Direction.DESC, "id"))
C -> DB: SELECT * FROM categories ORDER BY id DESC
DB -> C: List of categories
C -> CC: List<Category>
CC -> UI: Response{status: 200, categories: List<CategoryDTO>}
UI -> A: Hiển thị danh sách categories với form thêm mới

@enduml
```

## 2. Sequence Diagram - Thêm phân loại mới

```plantuml
@startuml
title Add New Category - Thêm phân loại mới

actor Admin as A
boundary "CategoryManagementUI" as UI
control "CategoryController" as CC
entity "Category" as C
database MySQL as DB

== Thêm category mới ==
A -> UI: Nhập tên category
A -> UI: Click "Add Category" button

UI -> UI: Validate input (categoryName not empty)
alt Valid input
    UI -> CC: createCategory({name: categoryName})
    CC -> C: findByName(categoryName)
    C -> DB: SELECT * FROM categories WHERE name = ?
    
    alt Category name không tồn tại
        DB -> C: null (không tìm thấy)
        C -> CC: Optional.empty()
        CC -> C: save(new Category)
        C -> DB: INSERT INTO categories (name) VALUES (?)
        DB -> C: Saved category với ID
        C -> CC: Category entity
        
        == WebSocket broadcast ==
        CC -> CC: Create WebSocket message
        note over CC: message = {\n  "type": "CATEGORY_ADDED",\n  "data": {"id": id, "name": name}\n}
        CC -> CC: WebSocketHandler.broadcastChanges()
        note right: Broadcast tới tất cả clients\nđang kết nối
        
        CC -> UI: Response{status: 200, message: "Category created successfully"}
        UI -> UI: Clear categoryName input
        UI -> A: Hiển thị thông báo thành công
        
    else Category name đã tồn tại
        DB -> C: Existing category
        C -> CC: Optional.of(category)
        CC -> UI: InvalidCredentialsException("Category already exists")
        UI -> A: Hiển thị lỗi "Category already exists"
    end
    
else Invalid input
    UI -> A: Hiển thị lỗi "Category tên là bắt buộc"
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "CATEGORY_ADDED"
UI -> UI: addCategoryToList(newCategory)
UI -> A: Tự động thêm category mới vào danh sách

@enduml
```

## 3. Sequence Diagram - Chỉnh sửa phân loại

```plantuml
@startuml
title Edit Category - Chỉnh sửa phân loại

actor Admin as A
boundary "CategoryManagementUI" as UI
control "CategoryController" as CC
entity "Category" as C
database MySQL as DB

== Bắt đầu chỉnh sửa ==
A -> UI: Click "Edit" button trên category
UI -> UI: handleEditCategory(category)
UI -> UI: Set isEditing = true
UI -> UI: Set editingCategoryId = category.id
UI -> UI: Set categoryName = category.name
UI -> A: Form chuyển sang edit mode với dữ liệu đã load

== Thực hiện cập nhật ==
A -> UI: Thay đổi tên category
A -> UI: Click "Edit Category" button

UI -> UI: Validate input (editingCategoryId và categoryName not empty)
alt Valid input
    UI -> CC: updateCategory(editingCategoryId, {name: categoryName})
    CC -> C: findById(editingCategoryId)
    C -> DB: SELECT * FROM categories WHERE id = ?
    
    alt Category tồn tại
        DB -> C: Existing category
        C -> CC: Category entity
        CC -> C: Update category name
        C -> C: setName(newName)
        CC -> C: save(updatedCategory)
        C -> DB: UPDATE categories SET name = ? WHERE id = ?
        DB -> C: Updated category
        C -> CC: Updated category entity
        
        == WebSocket broadcast ==
        CC -> CC: Create WebSocket message
        note over CC: message = {\n  "type": "CATEGORY_UPDATED",\n  "data": {"id": id, "name": newName}\n}
        CC -> CC: WebSocketHandler.broadcastChanges()
        
        CC -> UI: Response{status: 200, message: "Category Successfully Updated"}
        UI -> UI: Reset form (isEditing = false, clear inputs)
        UI -> A: Hiển thị thông báo cập nhật thành công
        
    else Category không tồn tại
        DB -> C: null
        C -> CC: NotFoundException("Category Not Found")
        CC -> UI: Error response
        UI -> A: Hiển thị lỗi "Category not found"
    end
    
else Invalid input
    UI -> UI: Return early (không làm gì)
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "CATEGORY_UPDATED"
UI -> UI: updateCategoryInList(updatedCategory)
UI -> A: Tự động cập nhật category trong danh sách

== Cancel editing ==
A -> UI: Click "Cancel Editing" button
UI -> UI: cancel()
UI -> UI: Reset form state
note over UI: isEditing = false\ncategoryName = ''\neditingCategoryId = null
UI -> A: Form quay về add mode

@enduml
```

## 4. Sequence Diagram - Xóa phân loại

```plantuml
@startuml
title Delete Category - Xóa phân loại

actor Admin as A
boundary "CategoryManagementUI" as UI
control "CategoryController" as CC
entity "Category" as C
database MySQL as DB

== Xóa category ==
A -> UI: Click "Delete" button trên category
UI -> UI: Show confirmation dialog
UI -> A: "Are you sure you want to delete this category?"

alt User confirms deletion
    A -> UI: Click "OK" trong confirmation dialog
    UI -> CC: deleteCategory(categoryId)
    CC -> C: findById(categoryId)
    C -> DB: SELECT * FROM categories WHERE id = ?
    
    alt Category tồn tại
        DB -> C: Existing category
        C -> CC: Category entity
        CC -> C: deleteById(categoryId)
        C -> DB: DELETE FROM categories WHERE id = ?
        DB -> C: Delete successful
        C -> CC: Deletion confirmed
        
        == WebSocket broadcast ==
        CC -> CC: Create WebSocket message
        note over CC: message = {\n  "type": "CATEGORY_DELETED",\n  "categoryId": categoryId\n}
        CC -> CC: WebSocketHandler.broadcastChanges()
        
        CC -> UI: Response{status: 200, message: "Category Successfully Deleted"}
        UI -> A: Hiển thị thông báo xóa thành công
        
    else Category không tồn tại
        DB -> C: null
        C -> CC: NotFoundException("Category Not Found")
        CC -> UI: Error response
        UI -> A: Hiển thị lỗi "Category not found"
    end
    
else User cancels deletion
    A -> UI: Click "Cancel" trong confirmation dialog
    UI -> A: Không làm gì, quay về danh sách
end

== Real-time update cho các clients khác ==
UI -> UI: Receive WebSocket message "CATEGORY_DELETED"
UI -> UI: removeCategoryFromList(categoryId)
alt Đang edit category bị xóa
    UI -> UI: Check if editingCategoryId === deletedCategoryId
    UI -> UI: cancel() - Reset form
end
UI -> A: Tự động xóa category khỏi danh sách

@enduml
```

## 5. Sequence Diagram - WebSocket Real-time Updates và Error Handling

```plantuml
@startuml
title WebSocket Updates và Error Handling

actor Admin as A
boundary "CategoryManagementUI" as UI
control "CategoryController" as CC
entity "Category" as C
database MySQL as DB

== WebSocket Message Handling ==
UI -> UI: Receive WebSocket message
UI -> UI: handleWebSocketMessage(message)

alt message.type === "CATEGORY_UPDATED"
    UI -> UI: updateCategoryInList(message.data)
    note over UI: Tìm category trong list\nvà cập nhật thông tin
    UI -> UI: Update form nếu đang edit category này
    UI -> A: Category được cập nhật real-time
    
else message.type === "CATEGORY_ADDED"
    UI -> UI: addCategoryToList(message.data)
    note over UI: Thêm category mới vào list\nnếu chưa tồn tại
    UI -> A: Category mới xuất hiện real-time
    
else message.type === "CATEGORY_DELETED"
    UI -> UI: removeCategoryFromList(message.categoryId)
    alt Đang edit category bị xóa
        UI -> UI: cancel() - Reset form
        UI -> A: Form được reset do category bị xóa
    end
    UI -> A: Category biến mất khỏi danh sách
end

== Error Handling ==
alt Network Error
    UI -> CC: API call fails
    CC -> UI: Network timeout/connection error
    UI -> A: Hiển thị "Unable to connect to server"
    
else Authorization Error
    UI -> CC: API call với invalid token
    CC -> UI: 401/403 response
    UI -> A: Redirect to login page
    
else Validation Error
    UI -> CC: API call với invalid data
    CC -> UI: 400 response với error message
    UI -> A: Hiển thị specific validation error
    
else Database Error
    CC -> C: Database operation fails
    C -> DB: Connection/query error
    DB -> C: Database exception
    C -> CC: Database error
    CC -> UI: 500 response
    UI -> A: Hiển thị "Server error occurred"
end

== Component Cleanup ==
UI -> UI: ngOnDestroy()
UI -> UI: Unsubscribe from WebSocket
UI -> UI: Disconnect WebSocket connection
note over UI: Prevent memory leaks\nkhi component bị destroy

@enduml
```

## Các trường hợp đặc biệt

### 1. Authorization và Security
```typescript
// Chỉ ADMIN mới có quyền thao tác
@PreAuthorize("hasAuthority('ADMIN')")
public ResponseEntity<Response> createCategory(@RequestBody @Valid CategoryDTO categoryDTO)

// Frontend kiểm tra role trước khi hiển thị
if (!this.apiService.isAdmin()) {
    // Redirect hoặc hide functionality
}
```

### 2. WebSocket Message Types
```typescript
interface WebSocketMessage {
    type: 'CATEGORY_ADDED' | 'CATEGORY_UPDATED' | 'CATEGORY_DELETED';
    data?: {id: string, name: string};
    categoryId?: string; // Cho delete operation
}
```

### 3. Real-time Synchronization
- **CATEGORY_ADDED**: Thêm category mới vào danh sách tất cả clients
- **CATEGORY_UPDATED**: Cập nhật category trong danh sách và form nếu đang edit
- **CATEGORY_DELETED**: Xóa category khỏi danh sách và reset form nếu cần

### 4. Form State Management
```typescript
interface FormState {
    isEditing: boolean;           // Edit mode vs Add mode
    editingCategoryId: string;    // ID của category đang edit
    categoryName: string;         // Input value
}
```

### 5. Database Constraints
- **Unique Constraint**: Category name phải unique
- **Foreign Key**: Categories có thể được reference bởi Products
- **Soft Delete**: Có thể implement soft delete thay vì hard delete

### 6. Error Scenarios
- **Duplicate Name**: Tên category đã tồn tại
- **Category In Use**: Category đang được sử dụng bởi products
- **Concurrent Modification**: Hai admin cùng edit một category
- **Network Issues**: Mất kết nối trong quá trình thao tác

## Performance Considerations
- **Caching**: Cache category list để giảm database calls
- **Pagination**: Implement pagination nếu có nhiều categories
- **Debouncing**: Debounce WebSocket updates để tránh spam
- **Optimistic Updates**: Update UI trước, rollback nếu API fails

## Business Rules
1. **ADMIN Only**: Chỉ ADMIN mới có quyền CRUD categories
2. **Unique Names**: Tên category phải unique trong hệ thống  
3. **Required Field**: Tên category không được để trống
4. **Real-time Sync**: Tất cả changes được sync real-time
5. **Confirmation**: Xóa category cần confirmation
6. **Form Reset**: Form được reset sau successful operations