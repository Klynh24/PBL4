# Hướng dẫn Biên dịch và Chạy

## Yêu cầu môi trường
- CMake 3.4.1 trở lên
- Trình biên dịch C++ hỗ trợ chuẩn C++14 (g++/clang++)
- Thư viện Boost (yêu cầu tối thiểu 1.55, dùng các module `coroutine`, `filesystem`, `system`)
- Thư viện OpenSSL (cả `libssl` và `libcrypto`)
- Thư viện pthread (`Threads::Threads`)

Trên Ubuntu/Debian có thể cài nhanh bằng:
```bash
sudo apt install build-essential cmake libboost-all-dev libssl-dev
sudo apt-get install libboost-coroutine-dev
sudo apt-get install libboost-filesystem-dev
```

## Biên dịch dự án
```bash
rm -rf build                 # gỡ file biên dịch cũ (nếu có)
cmake -S . -B build          # bước cấu hình
cmake --build build          # biên dịch tất cả target (server + client)
```

Một số lệnh hữu ích khác:
```bash
cmake --build build --target server   # chỉ biên dịch server
cmake --build build --target client   # chỉ biên dịch client
```

Các tệp nhị phân thu được nằm trong:
- `build/server/server`
- `build/client/client`

## Chạy STUN server
Server hỗ trợ khá nhiều tham số dòng lệnh. Bạn có thể xem mô tả đầy đủ bằng:
```bash
./build/server/server --help
```

Lưu ý: Phiên bản này chỉ hỗ trợ UDP (đã loại bỏ TCP).

Ví dụ khởi động server chế độ cơ bản (lắng nghe UDP trên cổng mặc định 3478, mọi giao diện):
```bash
./build/server/server --mode basic --primaryinterface 0.0.0.0 --primaryport 3478
hoặc
./build/server/server --verbosity 2
```
Tùy chọn cụ thể phụ thuộc vào môi trường của bạn; xem tài liệu `--help` hoặc các file trong thư mục `resources` để biết thêm cấu hình mẫu.

Nhấn Ctrl + C để dừng Server

## Chạy STUN client
Mở terminal Ubuntu khác
Client dòng lệnh cũng có hướng dẫn kèm theo:
```bash
./build/client/client --help
```

Ví dụ nhanh:
```bash
# Lấy Public IP và cổng thông qua STUN công khai (chỉ cần "basic")
./build/client/client stun.l.google.com 19302 --mode basic
# Kết quả sẽ in dòng "Mapped address: <PUBLIC_IP>:<PORT>"

# Nếu bạn chạy server của dự án trên máy cục bộ (UDP 3478)
./build/client/client --mode basic --remote 127.0.0.1 --port 3478
```

Phát hiện loại NAT (Behavior/Filtering) yêu cầu STUN server hỗ trợ RFC 5780 ("full mode" với ít nhất 2 địa chỉ IP). Bạn có thể:
- Tự chạy server kèm `--mode full --primaryinterface <IP1> --altinterface <IP2>` trên máy có 2 IP công khai.
- Hoặc sử dụng một STUN server công khai có hỗ trợ RFC 5780 (không phải tất cả máy chủ STUN đều hỗ trợ).

Ví dụ với server cục bộ hỗ trợ full mode:
```bash
./build/client/client --mode behavior 127.0.0.1 3478   # kiểm tra NAT Behavior
./build/client/client --mode filtering 127.0.0.1 3478  # kiểm tra NAT Filtering (UDP)
```
Tham số chính xác (tên tùy chọn `--remote`, `--port`, `--protocol`, v.v.) được liệt kê chi tiết trong phần trợ giúp của chương trình.

## Ghi chú
- Nếu bạn thay đổi mã nguồn, hãy chạy lại lệnh `cmake --build build` để biên dịch lại.
- Khi chạy server và client trên cùng máy, nhớ mở quyền truy cập cổng UDP/TCP tương ứng trong firewall (nếu có).
