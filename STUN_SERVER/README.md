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
./build/server/server --verbosity 2
```
