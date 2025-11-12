### 1. Cài đặt Go (Golang)

#### Windows:

**Cách 1: Download từ trang chính thức**

```powershell
# 1. Tải Go từ: https://go.dev/dl/
# 2. Chọn file .msi phù hợp (VD: go1.21.5.windows-amd64.msi)
# 3. Chạy file installer và làm theo hướng dẫn

# 4. Kiểm tra cài đặt
go version
# Output: go version go1.21.5 windows/amd64
```

**Cách 2: Sử dụng Chocolatey**

```powershell
# Cài Chocolatey trước (nếu chưa có)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Cài Go
choco install golang -y

# Kiểm tra
go version
```

#### Linux (Ubuntu/Debian):

```bash
# Cập nhật package list
sudo apt update

# Cài đặt Go
sudo apt install golang-go -y

# Hoặc cài bản mới nhất
wget https://go.dev/dl/go1.21.5.linux-amd64.tar.gz
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go1.21.5.linux-amd64.tar.gz

# Thêm vào PATH
echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
source ~/.bashrc

# Kiểm tra
go version
```

### 2. Cài đặt Caddy Web Server

#### Windows:

**Sử dụng Chocolatey**

```powershell
choco install caddy -y
caddy version
```

#### Linux:

```bash
# Debian/Ubuntu
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install caddy

# Kiểm tra
caddy version
```

### 2. Build các server

```powershell
# Build SIGNALING_SERVER
cd SIGNALING_SERVER
go mod tidy
go build -o bin/server.exe cmd/server/main.go

# Build SFU_SERVER
cd ..\SFU_SERVER
go mod tidy
go build -o bin/server.exe cmd/server/main.go

# Build STUN_SERVER
cd ..\STUN_SERVER
go mod tidy
go build -o stun_server.exe main.go

cd ..
```

---

## 🚀 Chạy dự án

### Cách 1: Chạy thủ công từng server

#### Terminal 1 - Caddy (Reverse Proxy & HTTPS)

```powershell
# Chạy từ thư mục Realtime
caddy run --config Caddyfile

# Hoặc chạy ở background
caddy start --config Caddyfile
```

#### Terminal 2 - SIGNALING_SERVER (Port 8082)

```powershell
cd SIGNALING_SERVER
.\bin\server.exe --config configs/config.yaml

# Hoặc
go run cmd/server/main.go --config configs/config.yaml
```

#### Terminal 3 - SFU_SERVER (Port 50051)

```powershell
cd SFU_SERVER
.\bin\server.exe

# Hoặc
go run cmd/server/main.go
```

#### Terminal 4 - STUN_SERVER (Port 3478)

```powershell
cd STUN_SERVER
.\stun_server.exe

# Hoặc
go run main.go
```

### Cách 2: Script tự động (Windows)

```powershell
.\run.ps1 #chạy STUN, SIGNALING, SFU, Caddy
.\stop.ps1 #dừng STUN, SIGNALING, SFU, Caddy
```

### Cách 3: Script tự động (Linux/Mac)

Tạo file `start-all.sh`:

```bash
#!/bin/bash

echo "Starting all servers..."

# Start Caddy
caddy start --config Caddyfile &
sleep 2

# Start SIGNALING_SERVER
cd SIGNALING_SERVER && go run cmd/server/main.go &
sleep 2

# Start SFU_SERVER
cd ../SFU_SERVER && go run cmd/server/main.go &
sleep 2

# Start STUN_SERVER
cd ../STUN_SERVER && go run main.go &

echo "All servers started!"
```

Chạy:

```bash
chmod +x start-all.sh
./start-all.sh
```

---

## 🔄 Quản lý Caddy

### Chạy Caddy

```powershell
# Chạy foreground (xem logs trực tiếp)
caddy run --config Caddyfile

# Chạy background
caddy start --config Caddyfile
```

### Reload Caddy (sau khi sửa Caddyfile)

```powershell
# Reload không downtime
caddy reload --config Caddyfile

# Kiểm tra config hợp lệ trước khi reload
caddy validate --config Caddyfile
```

### Restart Caddy

```powershell
# Stop Caddy
caddy stop

# Start lại
caddy start --config Caddyfile
```

## 🌐 Thay đổi IP máy

Khi IP máy thay đổi (VD: từ `192.168.98.51` → `192.168.5.244`), bạn cần sửa ở **4 chỗ**:

### 1. ✏️ Caddyfile

```caddyfile
# filepath: Caddyfile

# Sửa IP LAN
192.168.5.244:443 {  # ← THAY ĐỔI IP MỚI
    tls internal
    # ...
}
```

### 2. ✏️ SIGNALING_SERVER/configs/config.yaml

```yaml
# filepath: SIGNALING_SERVER/configs/config.yaml

server:
  addr: "0.0.0.0:8082" # Không cần đổi, bind tất cả interfaces

sfu:
  addr: "192.168.5.244:50051" # ← THAY ĐỔI IP MỚI
```

### 3. ✏️ SFU_SERVER/config.yaml

```yaml
# filepath: SFU_SERVER/config.yaml

server:
  grpc_addr: "0.0.0.0:50051" # Không cần đổi

webrtc:
  ice_servers:
    - urls:
        - "stun:192.168.5.244:3478" # ← THAY ĐỔI IP MỚI
        - "stun:stun.l.google.com:19302" # Backup
```

### 4. ✏️ public/Test-WebRTC.html

```javascript
// filepath: public/Test-WebRTC.html

const iceServers = {
  iceServers: [
    { urls: "stun:192.168.5.244:3478" }, // ← THAY ĐỔI IP MỚI
    { urls: "stun:stun.l.google.com:19302" },
  ],
};
```

### Sau khi sửa:

```powershell
# 1. Validate Caddyfile
caddy validate --config Caddyfile

# 2. Reload Caddy
caddy reload --config Caddyfile
```

---
