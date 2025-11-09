package main

import (
	"net"
	"os"
	"time"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	grpc_sfu "tangthetoan.com/sfu/internal/grpc"
	"tangthetoan.com/sfu/proto"
)

func main() {
	// Setup logging
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnixMs
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})

	// Lấy config từ Biến Môi trường (Environment Variables)
	listenAddr := getEnv("LISTEN_ADDR", ":50051")
	stunURL := getEnv("STUN_URL", "stun:stun.l.google.com:19302")

	lis, err := net.Listen("tcp", listenAddr)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to listen")
	}

	s := grpc.NewServer()
	sfuServer := grpc_sfu.NewServer(stunURL) // Khởi tạo SFU server

	proto.RegisterIonSFUServer(s, sfuServer) // Đăng ký service
	reflection.Register(s)                   // Bật gRPC reflection (tốt cho debug)

	log.Info().Str("addr", listenAddr).Str("stun", stunURL).Msg("gRPC SFU server starting")
	if err := s.Serve(lis); err != nil {
		log.Fatal().Err(err).Msg("failed to serve")
	}
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}
