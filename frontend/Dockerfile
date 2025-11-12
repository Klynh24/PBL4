# Stage 1: Build Frontend (tạo ra folder dist)
FROM node:20-alpine AS builder
WORKDIR /app

# 1. Copy dependencies và cài đặt (lớp cache 1)
COPY package.json package-lock.json ./
RUN npm install

# 2. KHẮC PHỤC LỖI ESBUILD/VITE: Đảm bảo các gói native được biên dịch lại cho Alpine Linux
# Lệnh này giải quyết lỗi "Host version does not match binary version"
RUN npm rebuild esbuild --force || true
RUN npm cache clean --force

# 3. Copy code nguồn (lớp cache 2)
COPY . .

# 4. Lệnh build Vite
RUN npm run build

# Stage 2: Sử dụng NGINX để phục vụ các file tĩnh
FROM nginx:alpine

# Copy file tĩnh đã build vào thư mục mặc định của NGINX
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy cấu hình NGINX để thiết lập proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]