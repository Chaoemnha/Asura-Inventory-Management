# Biểu đồ trình tự - Use case: Quản lý người dùng (User Management)

## Mô tả
Use case này cho phép Admin và Stock Staff quản lý người dùng trong hệ thống, bao gồm: xem danh sách, tìm kiếm, lọc theo role, thêm mới, chỉnh sửa và xóa người dùng. Hệ thống cũng hỗ trợ authentication, authorization và validation dữ liệu.

## 1. Sequence Diagram - Khởi tạo và Load danh sách người dùng

```plantuml
@startuml
title User Management - Load danh sách người dùng

actor Admin as A
boundary "UserManagementUI" as UI
control "UserController" as UC
control "UserService" as US
entity "User" as U
database MySQL as DB

== Khởi tạo và Authentication ==
A -> UI: Truy cập trang User Management
UI -> UI: Kiểm tra authentication và role
note over UI: @PreAuthorize("hasAnyAuthority({'ADMIN', 'STOCKSTAFF'})")\nADMIN và STOCKSTAFF có quyền xem

== Khởi tạo component ==
UI -> UI: Initialize component properties
note over UI: users: []\nfilteredUsers: []\nsearchText: ''\nselectedRole: ''\nroles: ['', 'ADMIN', 'STOCKSTAFF', 'CUSTOMER', 'SUPPLIER']

== Load danh sách users ==
UI -> UC: getAllUsers()
UC -> US: getAllUsers()
US -> U: findAll(Sort.by(Sort.Direction.DESC, "id"))
U -> DB: SELECT * FROM users ORDER BY id DESC
DB -> U: List of users
U -> US: List<User>
US -> UC: Response{status: 200, users: List<UserDTO>}
note over US: ModelMapper.map(users, List<UserDTO>)\nTransactions set to null for security
UC -> UI: ResponseEntity<Response>
UI -> UI: this.users = res.users
UI -> UI: applyFilters()
UI -> A: Hiển thị danh sách users với search và filter

@enduml
```

## 2. Sequence Diagram - Tìm kiếm và lọc người dùng

```plantuml
@startuml
title User Management - Search và Filter

actor Admin as A
boundary "UserManagementUI" as UI

== Search functionality ==
A -> UI: Nhập text vào search box
UI -> UI: onSearchChange()
UI -> UI: applyFilters()
note over UI: Filter by:\n- name.toLowerCase().includes(searchText)\n- email.toLowerCase().includes(searchText)\n- phoneNumber.includes(searchText)\n- role.toLowerCase().includes(searchText)

== Role filtering ==
A -> UI: Chọn role từ dropdown filter
UI -> UI: onRoleFilterChange()
UI -> UI: applyFilters()
note over UI: Filter by:\n- !selectedRole || user.role === selectedRole

== Apply combined filters ==
UI -> UI: filteredUsers = users.filter(user => matchesSearch && matchesRole)
UI -> A: Hiển thị danh sách đã được lọc

== Clear filters ==
A -> UI: Click "Clear Filters" button
UI -> UI: clearFilters()
UI -> UI: searchText = '', selectedRole = ''
UI -> UI: applyFilters()
UI -> A: Hiển thị tất cả users

@enduml
```

## 3. Sequence Diagram - Thêm người dùng mới

```plantuml
@startuml
title Add New User - Thêm người dùng mới

actor Admin as A
boundary "UserManagementUI" as UI
boundary "AddUserUI" as AUI
control "AuthController" as AC
control "UserService" as US
entity "User" as U
database MySQL as DB

== Navigate to Add User Page ==
A -> UI: Click "Add User" button
UI -> UI: navigateToAddUser()
UI -> AUI: Router.navigate(['/add-user'])

== Thêm user mới ==
A -> AUI: Nhập thông tin user (name, email, password, phoneNumber, role, address)
A -> AUI: Click "Register User" button

AUI -> AUI: Validate required fields
alt Valid input
    AUI -> AC: registerUser({name, email, password, phoneNumber, role, address})
    AC -> US: registerUser(registerRequest)
    
    == Check for existing user ==
    US -> U: findByName(name)
    US -> U: findByPhoneNumber(phoneNumber)  
    US -> U: findByEmail(email)
    U -> DB: SELECT * FROM users WHERE name/phone/email = ?
    
    alt User already exists
        DB -> U: Existing user found
        U -> US: Optional<User> present
        US -> AC: InvalidCredentialsException("This user already exists")
        AC -> AUI: Error response
        AUI -> A: Hiển thị lỗi "User already exists"
        
    else User không tồn tại
        DB -> U: No existing user
        U -> US: Optional<User> empty
        
        == Create new user ==
        US -> US: UserRole role = registerRequest.getRole() || CUSTOMER
        US -> US: User.builder() with encoded password
        US -> U: save(userToSave)
        U -> DB: INSERT INTO users VALUES (...)
        DB -> U: New user saved
        U -> US: Saved user
        US -> AC: Response{status: 200, message: "user created successfully"}
        AC -> AUI: Success response
        AUI -> A: Hiển thị success message
        AUI -> UI: Navigate back to user list
        UI -> UI: fetchUsers() - Reload danh sách
    end
    
else Invalid input
    AUI -> A: Hiển thị lỗi validation
end

@enduml
```

## 4. Sequence Diagram - Chỉnh sửa người dùng

```plantuml
@startuml
title Edit User - Chỉnh sửa người dùng

actor Admin as A
boundary "UserManagementUI" as UI
boundary "EditUserUI" as EUI
control "UserController" as UC
control "UserService" as US
entity "User" as U
database MySQL as DB

== Navigate to Edit User Page ==
A -> UI: Click "Edit" button trên user
UI -> UI: navigateToEditUser(userId)
UI -> EUI: Router.navigate([`/edit-user/${userId}`])

== Load existing user data ==
EUI -> UC: getUserById(userId)
UC -> US: getUserById(userId)
US -> U: findById(userId)
U -> DB: SELECT * FROM users WHERE id = ?

alt User tồn tại
    DB -> U: Existing user
    U -> US: User entity
    US -> UC: UserDTO (transactions = null)
    note over US: ModelMapper.map(user, UserDTO.class)\nSecurity: transactions set to null
    UC -> EUI: ResponseEntity<UserDTO>
    EUI -> A: Form hiển thị với dữ liệu hiện tại
    
else User không tồn tại
    DB -> U: null
    U -> US: NotFoundException("User Not Found")
    US -> UC: Exception
    UC -> EUI: Error response
    EUI -> A: Hiển thị lỗi "User not found"
end

== Thực hiện cập nhật ==
A -> EUI: Thay đổi thông tin user
A -> EUI: Click "Update User" button

EUI -> EUI: Validate input
alt Valid input
    EUI -> UC: updateUser(userId, userDTO)
    UC -> US: updateUser(id, userDTO)
    US -> U: findById(id)
    
    alt User exists for update
        U -> DB: SELECT * FROM users WHERE id = ?
        DB -> U: Existing user
        U -> US: User entity
        
        == Update fields conditionally ==
        US -> US: if (userDTO.getEmail() != null) existingUser.setEmail(...)
        US -> US: if (userDTO.getName() != null) existingUser.setName(...)
        US -> US: if (userDTO.getPhoneNumber() != null) existingUser.setPhoneNumber(...)
        US -> US: if (userDTO.getRole() != null) existingUser.setRole(...)
        US -> US: if (password != null && !empty) encode and set password
        
        US -> U: save(existingUser)
        U -> DB: UPDATE users SET ... WHERE id = ?
        DB -> U: Updated user
        U -> US: Saved user
        US -> UC: Response{status: 200, message: "User Successfully updated"}
        UC -> EUI: Success response
        EUI -> A: Hiển thị success message
        EUI -> UI: Navigate back to user list
        UI -> UI: fetchUsers() - Reload danh sách
        
    else User not found for update
        U -> US: NotFoundException("User Not Found")
        US -> UC: Exception
        UC -> EUI: Error response
        EUI -> A: Hiển thị lỗi "User not found"
    end
    
else Invalid input
    EUI -> A: Hiển thị lỗi validation
end

@enduml
```

## 5. Sequence Diagram - Xóa người dùng

```plantuml
@startuml
title Delete User - Xóa người dùng

actor Admin as A
boundary "UserManagementUI" as UI
control "UserController" as UC
control "UserService" as US
entity "User" as U
database MySQL as DB

== Xóa user ==
A -> UI: Click "Delete" button trên user
UI -> UI: handleDeleteUser(userId, userName)
UI -> A: Show confirmation dialog
note over UI: window.confirm(\n"Are you sure you want to delete user '{userName}'?\nThis action cannot be undone.")

alt User confirms deletion
    A -> UI: Click "OK" trong confirmation dialog
    UI -> UC: deleteUser(userId)
    UC -> US: deleteUser(id)
    
    == Check user exists ==
    US -> U: findById(id)
    U -> DB: SELECT * FROM users WHERE id = ?
    
    alt User exists
        DB -> U: Existing user
        U -> US: User entity
        
        == Delete user ==
        US -> U: deleteById(id)
        U -> DB: DELETE FROM users WHERE id = ?
        note over DB: CASCADE delete related data:\n- Transactions may be handled\n- Foreign key constraints checked
        
        alt Successful deletion
            DB -> U: User deleted successfully
            U -> US: Deletion completed
            US -> UC: Response{status: 200, message: "User Successfully Deleted"}
            UC -> UI: Success response
            UI -> UI: notificationService.showSuccess("Success", "User deleted successfully")
            UI -> UI: fetchUsers() - Reload danh sách
            UI -> A: User biến mất khỏi danh sách
            
        else Foreign key constraint error
            DB -> U: Constraint violation (user has transactions)
            U -> US: DataIntegrityViolationException
            US -> UC: Error response
            UC -> UI: Error response
            UI -> UI: notificationService.showError("Error", "Cannot delete user with transactions")
            UI -> A: Hiển thị lỗi constraint
        end
        
    else User not found
        DB -> U: null
        U -> US: NotFoundException("User Not Found")
        US -> UC: Exception
        UC -> UI: Error response
        UI -> UI: notificationService.showError("Error", "User not found")
        UI -> A: Hiển thị lỗi "User not found"
    end
    
else User cancels deletion
    A -> UI: Click "Cancel" trong confirmation dialog
    UI -> A: Không làm gì, quay về danh sách
end

@enduml
```

## 6. Sequence Diagram - Authentication và Authorization

```plantuml
@startuml
title User Management - Authentication và Authorization

actor User as U
actor Admin as A
actor StockStaff as SS
boundary "UserManagementUI" as UI
control "JwtUtils" as JWT
control "SecurityConfig" as SC

== Authentication Check ==
U -> UI: Truy cập /user-management
UI -> UI: Angular Route Guard
UI -> JWT: isAuthenticated()
JWT -> JWT: Check token in localStorage
JWT -> JWT: Decrypt and validate token

alt Valid token
    JWT -> UI: true
    UI -> UI: Continue to component
    
else Invalid/Missing token
    JWT -> UI: false
    UI -> U: Redirect to /login
end

== Authorization Check - Component Level ==
UI -> UI: ngOnInit() - Check user role
UI -> JWT: getUserRole()
JWT -> UI: User role from token

alt User is ADMIN
    UI -> UI: isAdmin() returns true
    UI -> A: Show full CRUD functionality
    note over UI: - View all users\n- Add new user\n- Edit any user\n- Delete any user
    
else User is STOCKSTAFF  
    UI -> UI: isStockStaff() returns true
    UI -> SS: Show limited functionality
    note over UI: - View all users\n- Edit own profile only\n- No delete permissions
    
else User is CUSTOMER/SUPPLIER
    UI -> UI: hasRole(['ADMIN', 'STOCKSTAFF']) returns false
    UI -> U: Access Denied / Redirect
    note over UI: Component không hiển thị\nhoặc redirect về trang chính
end

== Authorization Check - API Level ==
UI -> UC: getAllUsers()
UC -> SC: @PreAuthorize("hasAnyAuthority({'ADMIN', 'STOCKSTAFF'})")
SC -> JWT: Validate token authorities

alt User has required authority
    SC -> UC: Authorization passed
    UC -> UI: Execute API call
    
else User lacks authority
    SC -> UI: 403 Forbidden
    UI -> U: Display authorization error
end

== Method-specific Authorization ==
alt Delete User Operation
    UI -> UC: deleteUser(id)
    UC -> SC: @PreAuthorize("hasAuthority('ADMIN')")
    
    alt User is ADMIN
        SC -> UC: Authorization passed
        UC -> UI: Execute delete operation
        
    else User is not ADMIN
        SC -> UI: 403 Forbidden
        UI -> U: "Only administrators can delete users"
    end
    
else Update User Operation
    UI -> UC: updateUser(id, userDTO)
    UC -> UC: No specific @PreAuthorize
    note over UC: Any authenticated user can update\nbut business logic may restrict\nto own profile only
    UC -> UI: Execute update operation
end

@enduml
```

## 7. Sequence Diagram - Error Handling và Validation

```plantuml
@startuml
title User Management - Error Handling và Validation

actor Admin as A
boundary "UserManagementUI" as UI
control "UserController" as UC
control "UserService" as US
entity "User" as U
database MySQL as DB

== Client-side Validation ==
A -> UI: Nhập dữ liệu user form
UI -> UI: validateRequiredFields()

alt Missing required fields
    UI -> A: Hiển thị field validation errors
    note over UI: - Name is required\n- Email is required\n- Phone number is required
    
else Invalid email format
    UI -> A: Hiển thị "Invalid email format"
    
else Valid input
    UI -> UC: Proceed with API call
end

== Server-side Validation ==
UI -> UC: addUser/updateUser API call
UC -> US: Service method call

== Database Constraint Validation ==
US -> U: save(user)
U -> DB: INSERT/UPDATE user

alt Unique constraint violation
    DB -> U: Constraint violation (email/phone already exists)
    U -> US: DataIntegrityViolationException
    US -> UC: InvalidCredentialsException("This user already exists")
    UC -> UI: Error response {status: 400, message: "User already exists"}
    UI -> UI: notificationService.showError("Error", "User already exists")
    UI -> A: Hiển thị error notification
    
else Foreign key constraint violation
    DB -> U: FK constraint violation
    U -> US: DataIntegrityViolationException
    US -> UC: Error response
    UC -> UI: Error response {status: 400}
    UI -> UI: notificationService.showError("Error", "Cannot delete user with active transactions")
    UI -> A: Hiển thị constraint error
    
else Successful operation
    DB -> U: Operation completed
    U -> US: Success result
    US -> UC: Response {status: 200}
    UC -> UI: Success response
    UI -> UI: notificationService.showSuccess("Success", "Operation completed")
    UI -> A: Hiển thị success message
end

== Network Error Handling ==
alt Network connection failed
    UI -> UC: API call timeout
    UC -> UI: Network error
    UI -> UI: notificationService.showError("Error", "Unable to connect to server")
    UI -> A: Hiển thị network error
    
else Server error (500)
    UI -> UC: API call
    UC -> UI: 500 Internal Server Error
    UI -> UI: notificationService.showError("Error", "Server error occurred")
    UI -> A: Hiển thị server error
    
else Authorization error (403)
    UI -> UC: API call without proper permissions
    UC -> UI: 403 Forbidden
    UI -> UI: notificationService.showError("Error", "Access denied")
    UI -> A: Hiển thị authorization error
end

== Component Error Handling ==
UI -> UI: ngOnInit() encounters error
UI -> UI: try-catch block

alt Service initialization error
    UI -> UI: catch(error)
    UI -> UI: console.error("Component initialization failed")
    UI -> A: Hiển thị fallback UI hoặc error message
    
else Data loading error
    UI -> UC: fetchUsers() fails
    UI -> UI: Handle error in subscribe error callback
    UI -> UI: notificationService.showError("Error", error?.error?.message || "Unable to fetch users")
    UI -> A: Hiển thị data loading error
end

== Form Validation States ==
UI -> UI: Track form states
note over UI: - pristine/dirty\n- valid/invalid\n- touched/untouched\n- pending

alt Form is invalid
    UI -> UI: Disable submit button
    UI -> A: Visual feedback (red borders, error messages)
    
else Form is valid
    UI -> UI: Enable submit button
    UI -> A: Allow form submission
end

@enduml
```

## Các trường hợp đặc biệt

### 1. Authorization và Security
```typescript
// Component level - chỉ ADMIN và STOCKSTAFF có quyền truy cập
isAdmin(): boolean {
  return this.apiService.isAdmin();
}

// API level - different permissions cho different operations
@PreAuthorize("hasAnyAuthority({'ADMIN', 'STOCKSTAFF'})")  // View users
@PreAuthorize("hasAuthority('ADMIN')")  // Delete users
```

### 2. Data Security và Privacy
```java
// Service layer - Remove sensitive data
List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>() {}.getType());
userDTOS.forEach(userDTO -> userDTO.setTransactions(null));  // Security
```

### 3. Search và Filter Performance
```typescript
// Client-side filtering for better UX
applyFilters(): void {
  this.filteredUsers = this.users.filter(user => {
    const matchesSearch = // Multiple field search
    const matchesRole = // Role-based filtering
    return matchesSearch && matchesRole;
  });
}
```

### 4. User Experience Enhancements
```typescript
// Confirmation dialogs for destructive actions
handleDeleteUser(userId: string, userName: string): void {
  if (window.confirm(`Are you sure you want to delete user "${userName}"?`)) {
    // Proceed with deletion
  }
}

// Role-based UI styling
getRoleBadgeClass(role: string): string {
  const roleClasses = {
    'ADMIN': 'badge badge-danger',
    'STOCKSTAFF': 'badge badge-info',
    'CUSTOMER': 'badge badge-success',
    'SUPPLIER': 'badge badge-secondary'
  };
}
```

### 5. Error Handling Strategies
- **Client-side validation**: Immediate feedback
- **Server-side validation**: Business rule enforcement  
- **Database constraints**: Data integrity protection
- **Network error handling**: Graceful degradation
- **Authorization errors**: Proper access control

### 6. Performance Considerations
- **Pagination**: Có thể implement cho large datasets
- **Caching**: Cache user list để giảm API calls
- **Debouncing**: Cho search input
- **Lazy loading**: Cho user details

### 7. Audit và Logging
- **User actions**: Track who did what when
- **Error logging**: For debugging và monitoring
- **Security events**: Login/logout, permission changes

### 8. Integration Points
- **Authentication service**: JWT token management
- **Notification service**: User feedback
- **WebSocket**: Real-time updates (có thể extend)
- **Email service**: For user notifications