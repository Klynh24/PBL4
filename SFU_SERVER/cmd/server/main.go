package main

import (
	"context"
	"net"
	"os"
	"os/signal"
	"syscall"
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

	// Get config from environment variables
	listenAddr := getEnv("LISTEN_ADDR", ":50051")
	stunURL := getEnv("STUN_URL", "stun:stun.l.google.com:19302")

	lis, err := net.Listen("tcp", listenAddr)
	if err != nil {
		log.Fatal().Err(err).Msg("failed to listen")
	}

	// Create gRPC server
	s := grpc.NewServer(
		grpc.MaxRecvMsgSize(10*1024*1024), // 10MB
		grpc.MaxSendMsgSize(10*1024*1024),
	)

	sfuServer := grpc_sfu.NewServer(stunURL)
	proto.RegisterIonSFUServer(s, sfuServer)
	reflection.Register(s)

	// Channel to signal server shutdown
	errChan := make(chan error, 1)

	// Start server in goroutine
	go func() {
		log.Info().
			Str("addr", listenAddr).
			Str("stun", stunURL).
			Msg("gRPC SFU server starting")

		if err := s.Serve(lis); err != nil {
			errChan <- err
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)

	select {
	case err := <-errChan:
		log.Fatal().Err(err).Msg("server error")
	case sig := <-quit:
		log.Info().Str("signal", sig.String()).Msg("shutting down server...")

		sfuServer.Shutdown()

		// Graceful shutdown
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		stopped := make(chan struct{})
		go func() {
			s.GracefulStop()
			close(stopped)
		}()

		select {
		case <-ctx.Done():
			log.Warn().Msg("shutdown timeout, forcing stop")
			s.Stop()
		case <-stopped:
			log.Info().Msg("server stopped gracefully")
		}
	}
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}
