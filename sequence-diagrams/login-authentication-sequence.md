# Biểu đồ trình tự - Chức năng Đăng nhập và Quản lý Mật khẩu

```plantuml
@startuml
!theme plain
skinparam backgroundColor White
skinparam sequenceMessageAlign center

actor User
boundary "Login UI" as LoginUI
boundary "Forgot Password UI" as ForgotPWUI
boundary "Reset Password UI" as ResetPWUI
control "Auth Controller" as AuthController
entity "User Entity" as UserEntity
database "Database" as CSDL

== Quy trình đăng nhập thành công ==
User -> LoginUI: Nhập email và password
User -> LoginUI: Click "Đăng nhập"
LoginUI -> AuthController: POST /api/auth/login
AuthController -> CSDL: Tìm user theo email
CSDL --> AuthController: Trả về dữ liệu user
AuthController -> UserEntity: Tạo đối tượng User
AuthController -> AuthController: Kiểm tra password
AuthController -> AuthController: Tạo JWT token
AuthController --> LoginUI: Trả về token và role
LoginUI -> LoginUI: Lưu token vào storage
LoginUI -> User: Chuyển hướng đến dashboard

== Quy trình quên mật khẩu ==
User -> LoginUI: Click "Cấp mật khẩu mới"
LoginUI -> ForgotPWUI: Chuyển trang forgot-password
User -> ForgotPWUI: Nhập email
User -> ForgotPWUI: Click "Send Reset Link"
ForgotPWUI -> AuthController: POST /api/auth/forgot-password
AuthController -> CSDL: Tìm user theo email
CSDL --> AuthController: Trả về dữ liệu user
AuthController -> UserEntity: Tạo đối tượng User
AuthController -> AuthController: Tạo reset token
AuthController -> UserEntity: Cập nhật reset token
AuthController -> CSDL: Lưu reset token vào user
CSDL --> AuthController: Xác nhận lưu thành công
AuthController -> AuthController: Gửi email reset link
AuthController --> ForgotPWUI: Trả về thông báo thành công
ForgotPWUI -> User: Hiển thị "Email đã được gửi"
ForgotPWUI -> LoginUI: Chuyển về trang login

== Quy trình đặt lại mật khẩu ==
User -> User: Mở email và click reset link
User -> ResetPWUI: Truy cập với token
ResetPWUI -> AuthController: GET /api/auth/reset-password?token=xxx
AuthController -> CSDL: Kiểm tra token hợp lệ
CSDL --> AuthController: Trả về dữ liệu user
AuthController -> UserEntity: Tạo đối tượng User
AuthController --> ResetPWUI: Token hợp lệ
User -> ResetPWUI: Nhập mật khẩu mới
User -> ResetPWUI: Xác nhận mật khẩu
User -> ResetPWUI: Click "Đặt lại mật khẩu"
ResetPWUI -> AuthController: POST /api/auth/reset-password
AuthController -> CSDL: Kiểm tra token còn hiệu lực
CSDL --> AuthController: Trả về dữ liệu user
AuthController -> UserEntity: Tạo đối tượng User
AuthController -> AuthController: Mã hóa mật khẩu mới
AuthController -> UserEntity: Cập nhật mật khẩu mới
AuthController -> CSDL: Cập nhật mật khẩu và xóa token
CSDL --> AuthController: Xác nhận cập nhật thành công
AuthController --> ResetPWUI: Thông báo đặt lại thành công
ResetPWUI -> LoginUI: Chuyển về trang login
ResetPWUI -> User: Hiển thị "Mật khẩu đã được đặt lại"

@enduml
```

## Mô tả quy trình

### 1. Đăng nhập thành công
- User nhập thông tin đăng nhập
- Hệ thống xác thực và tạo JWT token
- Chuyển hướng đến dashboard

### 2. Quên mật khẩu
- User yêu cầu reset mật khẩu
- Hệ thống tạo token và gửi email
- Hiển thị thông báo thành công

### 3. Đặt lại mật khẩu
- User truy cập link từ email
- Xác thực token và cập nhật mật khẩu mới
- Chuyển về trang đăng nhập