# Biểu đồ trình tự - Use case: Quản lý thông tin cá nhân (Profile Management)

## Mô tả
Use case này cho phép người dùng xem và chỉnh sửa thông tin cá nhân của mình, bao gồm: hiển thị profile, chỉnh sửa thông tin và cập nhật dữ liệu. Hệ thống hỗ trợ Gravatar avatar và validation dữ liệu.

## 1. Sequence Diagram - Xem thông tin cá nhân

```plantuml
@startuml
title Profile Management - Xem thông tin cá nhân

actor User as U
boundary "ProfileUI" as PUI
control "UserService" as US
entity "User" as UE
database CSDL as DB

== Khởi tạo và Authentication ==
U -> PUI: Truy cập trang Profile
PUI -> PUI: Kiểm tra authentication token
PUI -> PUI: Initialize component properties

== Load thông tin user ==
PUI -> US: getLoggedInUserInfo()
US -> US: Get email from SecurityContext
US -> UE: findByEmail(email)
UE -> DB: SELECT * FROM users WHERE email = ?
DB -> UE: User data
UE -> US: User entity
US -> US: ModelMapper.map(user, UserDTO.class)
US -> US: userDTO.setTransactions(null)
US -> PUI: UserDTO
PUI -> PUI: user = res, updateEditForm()
PUI -> PUI: Generate Gravatar hash from email
PUI -> U: Hiển thị profile với avatar và thông tin

@enduml
```

## 2. Sequence Diagram - Chỉnh sửa thông tin cá nhân

```plantuml
@startuml
title Profile Management - Chỉnh sửa thông tin cá nhân

actor User as U
boundary "ProfileUI" as PUI
control "UserService" as US
entity "User" as UE
database CSDL as DB

== Bắt đầu chỉnh sửa ==
U -> PUI: Kích "Edit Profile" button
PUI -> PUI: toggleEditMode()
PUI -> PUI: isEditMode = true
PUI -> PUI: updateEditForm() với dữ liệu hiện tại
PUI -> U: Hiển thị form chỉnh sửa với Save/Cancel buttons

== Nhập thông tin mới ==
U -> PUI: Thay đổi name, email, phoneNumber, address
PUI -> PUI: Two-way binding cập nhật editForm

== Lưu thay đổi ==
U -> PUI: Kích "Save" button
PUI -> PUI: saveChanges()
PUI -> PUI: Validate required fields và email format
PUI -> US: updateUser(user.id, updateData)
US -> UE: findById(id)
UE -> DB: SELECT * FROM users WHERE id = ?
DB -> UE: Existing user
UE -> US: User entity
US -> US: Update fields conditionally
US -> UE: save(existingUser)
UE -> DB: UPDATE users SET name=?, email=?, phoneNumber=?, address=? WHERE id=?
DB -> UE: Updated user
UE -> US: Saved user
US -> PUI: Response{status: 200, message: "User Successfully updated"}
PUI -> PUI: isEditMode = false
PUI -> PUI: fetchUserInfo() để refresh dữ liệu
PUI -> U: Hiển thị thông tin đã cập nhật với success notification

@enduml
```

## 3. Sequence Diagram - Hủy chỉnh sửa

```plantuml
@startuml
title Profile Management - Hủy chỉnh sửa

actor User as U
boundary "ProfileUI" as PUI

== Hủy chỉnh sửa ==
U -> PUI: Kích "Cancel" button
PUI -> PUI: cancelEdit()
PUI -> PUI: isEditMode = false
PUI -> PUI: updateEditForm() với dữ liệu gốc
PUI -> U: Quay về chế độ xem với dữ liệu ban đầu

@enduml
```

## 4. Sequence Diagram - Khởi tạo Avatar và Role Display

```plantuml
@startuml
title Profile Management - Avatar và Role Display

actor User as U
boundary "ProfileUI" as PUI

== Tạo Gravatar Avatar ==
PUI -> PUI: fetchUserInfo() response
PUI -> PUI: TextEncoder().encode(user.email)
PUI -> PUI: sha256.create().update(data)
PUI -> PUI: hashEmail = hash.hex()
PUI -> U: Hiển thị avatar từ gravatar.com với hash

== Hiển thị Role-based Information ==
PUI -> PUI: Kiểm tra user role
PUI -> PUI: isSupplier(), isStaff(), isAdmin()
PUI -> U: Hiển thị thông tin Supplier nếu role phù hợp

== Social Links ==
PUI -> U: Hiển thị email và phone links
U -> PUI: Kích email icon
PUI -> U: Mở mailto: link
U -> PUI: Kích phone icon  
PUI -> U: Mở tel: link

@enduml
```

## 5. Sequence Diagram - Validation và Error Handling

```plantuml
@startuml
title Profile Management - Validation và Error Handling

actor User as U
boundary "ProfileUI" as PUI
control "UserService" as US

== Client-side Validation ==
U -> PUI: Kích "Save" button với dữ liệu không hợp lệ
PUI -> PUI: Kiểm tra required fields
PUI -> PUI: Kiểm tra email format với regex
PUI -> U: Hiển thị error notification

== Server-side Success ==
U -> PUI: Kích "Save" button với dữ liệu hợp lệ
PUI -> US: updateUser(id, updateData)
US -> PUI: Response{status: 200}
PUI -> PUI: NotificationService.showSuccess()
PUI -> U: Hiển thị success message

== Network Error ==
PUI -> US: updateUser() network timeout
US -> PUI: Error response
PUI -> PUI: NotificationService.showError()
PUI -> U: Hiển thị "Không thể cập nhật profile" error

@enduml
```

## Đặc điểm chính của hệ thống

### 1. Security và Authentication
- JWT token validation tự động
- Chỉ user đăng nhập mới xem được profile
- Không thể chỉnh sửa role của chính mình

### 2. User Experience
- Edit mode toggle với Save/Cancel options
- Real-time form validation
- Gravatar integration cho avatar
- Role-based information display

### 3. Data Management
- Two-way data binding
- Form state management
- Original data preservation cho cancel functionality
- Conditional field updates

### 4. Integration Features
- Email và phone clickable links
- Supplier information display cho appropriate roles
- Responsive design với Bootstrap classes
- Icon-based action buttons

### 5. Performance Optimization
- Client-side validation trước server call
- Form data caching
- Selective field updates
- Efficient re-rendering