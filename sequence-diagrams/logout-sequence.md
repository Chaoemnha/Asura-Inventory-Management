# Biểu đồ trình tự - Chức năng Đăng xuất

```plantuml
@startuml
!theme plain
skinparam backgroundColor White
skinparam sequenceMessageAlign center

actor User
boundary "App Component UI" as AppUI
boundary "Login UI" as LoginUI
control "API Service" as APIService
control "Router" as Router
control "WebSocket Service" as WSService
entity "Storage" as Storage

== Quy trình đăng xuất thành công ==
User -> AppUI: Click "Đăng xuất"
AppUI -> AppUI: Đặt user = "Loading..."
AppUI -> APIService: logout()
APIService -> Storage: Xóa token khỏi localStorage
APIService -> Storage: Xóa role khỏi localStorage
Storage --> APIService: Xác nhận xóa thành công
APIService -> APIService: Phát tín hiệu authStatuschanged
APIService --> AppUI: Xác nhận đăng xuất
AppUI -> Router: navigate(['/login'])
Router -> LoginUI: Chuyển đến trang login
AppUI -> WSService: disconnect()
WSService --> AppUI: Ngắt kết nối WebSocket
AppUI -> AppUI: Cập nhật UI (user = Guest, hashEmail = '')
AppUI -> User: Hiển thị trang đăng nhập

@enduml
```

## Mô tả quy trình

### Quy trình đăng xuất
1. **User tương tác**: Click nút "Đăng xuất" trên giao diện
2. **Cập nhật UI**: Hiển thị trạng thái "Loading..." 
3. **Xóa dữ liệu**: Xóa token và role khỏi localStorage
4. **Phát tín hiệu**: Thông báo thay đổi trạng thái đăng nhập
5. **Chuyển hướng**: Điều hướng về trang đăng nhập
6. **Ngắt kết nối**: Đóng kết nối WebSocket
7. **Reset dữ liệu**: Đặt lại thông tin user về Guest
8. **Hoàn thành**: Hiển thị trang đăng nhập cho user