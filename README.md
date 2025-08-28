# Asura Inventory Management 📂

Backend🖥️ dự án này được sinh ra từ [Spring Initializer](https://start.spring.io/index.html) phiên bản 3.5.4.

Frontend dự án này được sinh ra từ [Angular CLI](https://github.com/angular/angular-cli) version 19.2.15.

## API server🖥️

Chạy lệnh `mvn spring-boot:run` _(yêu cầu đã cài Apache Maven 3.9.11 với java phiên bản 21)_ trên command prompt để khởi chạy ứng dụng, 
tôi khuyên bạn nên sử dụng IDE cho Java dev để cài các thư viện và những thứ tôi liệt kê trong .gitignore, 
sau đó nhấn nút "Run"▶️ Spring Boot Application để khởi chạy API server.

## Angular Development server

Chạy lệnh `cd .\frontend` và `ng serve` _(yêu cầu đã cài npm install -g @angular/cli)_ để chạy server Angular dev. Điều hướng đến `http://localhost:4200/`.

## Debug🪲 Angular

Để chạy được dự án Angular ở chế độ debug trên Visual Studio Code thì nhấn vào tab "Run and Debug"▶️🪲 trên Activity bar ở góc trái, yêu cầu tạo file `launch.json`, rồi dán nội dung sau vào
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
Trước khi debug, bạn hãy chạy câu lệnh `cd .\frontend` và `npm start` để chạy dự án.
Sau đó chọn cấu hình debug Launch Edge ở dropdown để attach debugger!

## Build🏗️

Chạy `ng build` để xây dựng dự án. Các thứ dựng nên sẽ được lưu trữ ở thư mục `dist/`.

## Running unit tests📝

Chạy lệnh `ng test` để kích hoạt unit test qua [Karma](https://karma-runner.github.io).

## Running end-to-end tests📝

Chạy lệnh `ng e2e` để thực hiện các end-to-end test thông qua một nền tảng bạn chọn. Để sử dụng lệnh này, trước tiên bạn cần thêm một gói thực hiện các end-to-end testing capability.

## Cần sự giúp đỡ😵‍💫

Nhắn tin/để lại bình luận trong dự án của tôi.
Để tìm kiếm thêm sự giúp đỡ trên Angular CLI hãy chạy `ng help` hoặc xem xét trang [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli).

## Bạn đã đọc được đến đây thì tôi đoán bạn có ít hứng thú đến dự án này, còn chờ gì nữa mà không để lại 1 star ⭐, tôi rất cảm kích. Bạn có thể bình luận khoe điều đó để tôi cảm tạ 🫠