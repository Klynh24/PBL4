package main

import (
	"context"
	"flag"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/rs/zerolog/log"
	"gopkg.in/yaml.v3"
	"tangthetoan.com/signaling-server/internal/auth"
	"tangthetoan.com/signaling-server/internal/router"
	"tangthetoan.com/signaling-server/internal/sfu"
	"tangthetoan.com/signaling-server/internal/ws"
)

type Config struct {
	Server struct {
		Addr           string `yaml:"addr"`
		WsPath         string `yaml:"ws_path"`
		JwtPubKeyPath  string `yaml:"jwt_pubkey_path"`
		MaxMessageSize int    `yaml:"max_message_size"`
	} `yaml:"server"`
	SFU struct {
		Targets string `yaml:"targets"`
	} `yaml:"sfu"`
}

func main() {
	cfgPath := flag.String("c", "configs/config.yaml", "config file")
	flag.Parse()
	f, err := os.Open(*cfgPath)
	if err != nil {
		log.Fatal().Err(err).Msg("open config")
	}
	defer f.Close()
	var cfg Config
	if err := yaml.NewDecoder(f).Decode(&cfg); err != nil {
		log.Fatal().Err(err).Msg("decode config")
	}
	ctx := context.Background()
	targets := strings.Split(cfg.SFU.Targets, ",")
	mgr := sfu.NewManager()
	for _, t := range targets {
		if strings.TrimSpace(t) == "" {
			continue
		}
		mgr.AddTarget(strings.TrimSpace(t))
	}
	mgr.Start(ctx)
	defer mgr.Stop()

	jwtVerifier, err := auth.NewVerifierFromFile(cfg.Server.JwtPubKeyPath)
	if err != nil {
		log.Fatal().Err(err).Msg("init jwt verifier")
	}
	hub := ws.NewHub()
	go hub.Run()

	r := router.NewRouter(hub, mgr, jwtVerifier)

	hub.SetHandler(r)
	http.HandleFunc(cfg.Server.WsPath, func(w http.ResponseWriter, r *http.Request) {
		token := r.URL.Query().Get("token")
		room := r.URL.Query().Get("room")
		userId := r.URL.Query().Get("userId")

		log.Info().Str("token", token).Str("room", room).Str("userId", userId).Msg("WebSocket connection attempt with parameters")
		log.Info().Msg("Calling hub.ServeWS")
		_, err := hub.ServeWS(r.Context(), w, r, token, room, userId)
		if err != nil {
			log.Error().Err(err).Msg("serve ws")
			return
		}
		log.Info().Msg("hub.ServeWS returned without error")
	})
	srv := &http.Server{
		Addr:         cfg.Server.Addr,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
	}

	// Graceful shutdown
	idleConnsClosed := make(chan struct{})
	go func() {
		sigint := make(chan os.Signal, 1)
		signal.Notify(sigint, os.Interrupt, syscall.SIGTERM)
		<-sigint

		log.Info().Msg("Shutting down server...")

		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()

		if err := srv.Shutdown(ctx); err != nil {
			log.Error().Err(err).Msg("Server shutdown failed")
		}
		close(idleConnsClosed)
	}()

	log.Info().Str("addr", cfg.Server.Addr).Msg("starting Signaling server")
	if err := srv.ListenAndServe(); err != http.ErrServerClosed {
		log.Fatal().Err(err).Msg("server failed")
	}

	<-idleConnsClosed
	log.Info().Msg("Server stopped")
}
