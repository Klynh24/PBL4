# Signaling Server (Spring Boot)

## Tính năng

- **Quản lý phòng:** Client phải `join` vào phòng trước khi gửi `offer`, `answer`, `candidate`. Tự động dọn kết nối khi client ngắt hoặc `leave`.
- **Gửi đích danh:** Trường `target` tùy chọn cho phép gửi tín hiệu tới đúng peer cần thiết, cô lập các thành viên khác.
- **Giới hạn kích thước:** Tham số `signaling.max-payload-bytes` cấu hình kích cỡ tối đa cho frame WebSocket; vượt ngưỡng sẽ bị từ chối.
- **Thông báo vòng đời:** Sự kiện `peer-joined`, `peer-left`, `joined` giúp client đồng bộ danh sách thành viên.
- **Hook xác thực:** Bật JWT qua `signaling.auth.enabled=true`. Token HMAC kiểm tra bằng `signaling.auth.jwt-secret`. Khi tắt xác thực, có thể truyền `clientId` ở query param hoặc header để nhận diện client.
- **Scale với Redis:** Khi `signaling.redis.enabled=true`, server dùng Redis Pub/Sub để phát tán tín hiệu giữa nhiều instance.

## Cấu hình

Các tham số chính đặt dưới prefix `signaling.*`. Có thể override trong `application.yml`, biến môi trường hoặc flag dòng lệnh (`--signaling.auth.enabled=true`).

| Thuộc tính | Mặc định | Mô tả |
| --- | --- | --- |
| `signaling.max-payload-bytes` | `65536` | Giới hạn kích thước frame WebSocket. |
| `signaling.auth.enabled` | `false` | Bật/tắt xác thực JWT. |
| `signaling.auth.jwt-secret` | `change-me` | Secret HMAC để verify JWT (tối thiểu 32 ký tự). |
| `signaling.auth.issuer` | `signaling-server` | Issuer kỳ vọng của JWT. |
| `signaling.redis.enabled` | `false` | Bật phân phối tín hiệu qua Redis Pub/Sub. |
| `signaling.redis.channel-prefix` | `signaling.room.` | Tiền tố kênh Pub/Sub. |
| `signaling.websocket.path` | `/ws` | Đường dẫn endpoint WebSocket. |
| `signaling.websocket.allowed-origins` | `["*"]` | Danh sách origin cho phép kết nối. |

Khi tắt auth vẫn có thể định danh client bằng query param `clientId` hoặc header `X-Client-Id` trong handshake.

### Redis

Khi scale nhiều instance:

1. Bật Redis: `signaling.redis.enabled=true`.
2. Cấu hình `spring.redis.host` / `spring.redis.port`.
3. Các instance dùng chung prefix kênh sẽ tự động chia sẻ sự kiện phòng.

Thông điệp Redis chứa `roomId`, `payload`, `targetSubject` và metadata nguồn để tránh broadcast vòng lặp.

## Giao thức tin nhắn

Client trao đổi JSON thuần như sau:

```json
// Join
{ "type": "join", "roomId": "room-42" }

// Offer / Answer / Candidate
{
  "type": "offer",
  "roomId": "room-42",
  "target": "peer-b",
  "payload": { /* SDP */ }
}

// Leave
{ "type": "leave" }
```

Server phát các sự kiện:

```json
{ "type": "joined", "roomId": "room-42", "subject": "peer-a" }
{ "type": "peer-joined", "roomId": "room-42", "subject": "peer-b" }
{ "type": "peer-left", "roomId": "room-42", "subject": "peer-b" }
{ "type": "error", "message": "Join a room before exchanging signaling data" }
{ "type": "pong" }
```

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

## Kiểm thử gợi ý

1. **Broadcast nội bộ:** Mở hai tab trình duyệt, join cùng phòng, kiểm tra offer truyền qua.
2. **Tín hiệu đích danh:** Gửi tin có `target`, đảm bảo chỉ peer target nhận được.
3. **Giới hạn kích thước:** Gửi payload quá lớn để xác nhận server đóng kết nối đúng chính sách.
4. **Fan-out Redis:** Chạy hai instance chung Redis, kiểm tra tín hiệu chéo instance.

## Lưu ý bảo mật

- Thay secret JWT mặc định trước khi bật xác thực.
- Thu hẹp `allowed-origins` về domain tin cậy trong môi trường production.
- Nên deploy sau reverse proxy/HTTPS; WebRTC yêu cầu origin an toàn.
- Kích hoạt logging/metrics (Micrometer có sẵn qua `micrometer-observation`).

## Hướng phát triển tiếp

- Thêm integration test cho luồng signaling.
- Mở endpoint giám sát health/membership nếu cần.
- Xem xét rate limiting hoặc củng cố auth cho môi trường nhiều rủi ro.
