## Chạy thử cục bộ

1. Cài Java 17 và Maven (ví dụ `sdk install java 17.0.10-tem` & `sudo apt install maven`).
2. Build dự án:
   ```bash
   mvn clean package
   ```
3. Khởi chạy ứng dụng:
   ```bash
   java -jar target/signaling-server-0.0.1-SNAPSHOT.jar
   ```
4. Kết nối WebSocket tới `ws://localhost:8080/ws`.

### Bật xác thực JWT

Đặt biến môi trường hoặc cấu hình:

```bash
export SIGNALING_AUTH_ENABLED=true
export SIGNALING_AUTH_JWT_SECRET='your-32-byte-secret..............'
export SIGNALING_AUTH_ISSUER='your-issuer'
```

Token phải được verify HMAC và chứa claim `sub`. Các claim hợp lệ sẽ xuất hiện trong handler qua `ClientPrincipal`.

### Bật Redis để scale

```bash
export SIGNALING_REDIS_ENABLED=true
export SPRING_REDIS_HOST=redis-host
export SPRING_REDIS_PORT=6379
```

Mọi instance dùng chung Redis và `channel-prefix` sẽ chuyển tiếp tín hiệu cho các peer còn lại.

## Lưu ý bảo mật

- Thay secret JWT mặc định trước khi bật xác thực.
- Thu hẹp `allowed-origins` về domain tin cậy trong môi trường production.
- Nên deploy sau reverse proxy/HTTPS; WebRTC yêu cầu origin an toàn.
- Kích hoạt logging/metrics (Micrometer có sẵn qua `micrometer-observation`).
