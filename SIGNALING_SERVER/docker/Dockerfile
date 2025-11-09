FROM golang:1.25.3-alpine AS builder
RUN apk add --no-cache git protoc protobuf-dev

WORKDIR /app
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
RUN go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
COPY go.mod go.sum ./
RUN go mod download
COPY . .
ENV PATH="/root/go/bin:${PATH}"
RUN protoc --go_out=. --go-grpc_out=. ./proto/signal.proto
RUN CGO_ENABLED=0 go build -o /app/server cmd/server/main.go


FROM alpine:latest

WORKDIR /app
COPY --from=builder /app/server /app/server

COPY configs/ /app/configs/

EXPOSE 8082

CMD ["/app/server"]
