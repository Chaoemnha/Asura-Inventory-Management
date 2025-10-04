# Asura Inventory Management 📂

### Hệ thống quản lý kho tích hợp trợ lý ảo, đồng bộ thời gian thực Websocket, tạo hóa đơn từ khung docx, ZXing tự động nhận diện QR và xử lý đơn hàng.

##  Tính năng độc đáo
Quản lý sản phẩm, Quản lý giao dịch. Theo dõi tồn kho, doanh thu qua số liệu, biểu đồ.
-  **Trợ lý ảo**: Trợ lý ảo cấu hình với mô hình RAG, phân quyền truy cập dữ liệu theo role.
-  **Đồng bộ dữ liệu client**: Thông báo WebSocket và đồng bộ tgian thực.
-  **Xác nhận giao dịch**: Quét QR để hoàn thành 1 giao dịch
-  **Cấp mật khẩu mới**: Sử dụng JavaMail gửi email reset mật khẩu
##  Tech Stack

### Backend
- **Framework**: Spring Boot
- **Ngôn ngữ**: Java
- **Database**: MySQL
- **Build**: Apache Maven
- **Tích hợp trợ lý ảo**: OpenAI GPT-4o-mini + LangChain4j
- **Đồng bộ**: WebSocket
- **Bảo mật**: JWT + Spring Security

### Frontend  
- **Framework**: Angular
- **Language**: TypeScript
- **Styling**: Bootstrap + Custom CSS
- **Biểu đồ**: ngx-charts
- **QR Scanner**: ZXing
- **Build**: Angular CLI
## Gỡ lỗi Angular
Gỡ lỗi Angular qua Edge
```
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Launch Edge",
            "request": "launch",
            "type": "msedge",
            "url": "http://localhost:4200",
            "webRoot": "${workspaceFolder}/angular",
            "sourceMaps": true,
            "sourceMapPathOverrides": {
                "webpack:/*": "${webRoot}/*",
                "/./*": "${webRoot}/*",
                "/src/*": "${webRoot}/src/*",
                "/*": "*",
                "/./~/*": "${webRoot}/node_modules/*"
            },
            "smartStep": true,
            "skipFiles": [
                "<node_internals>/**",
                "node_modules/**"
            ]
        }
    ]
}
```
Tôi luôn muốn củng cố dự án của mình, để lại issue giúp tôi củng cố dự án ạ, cảm ơn.
## Hãy cho tôi 1 ⭐ nếu bạn thấy nó hay, cảm ơn.