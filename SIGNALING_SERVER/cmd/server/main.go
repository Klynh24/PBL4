package main

import (
	"context"
	"encoding/json"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"

	"tangthetoan.com/signaling-server/internal/router"
	"tangthetoan.com/signaling-server/internal/sfu"
	"tangthetoan.com/signaling-server/internal/ws"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  4096, // Tăng buffer
	WriteBufferSize: 4096,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
	EnableCompression: true, // Enable WebSocket compression
}

func main() {
	// Optimize Go runtime
	runtime.GOMAXPROCS(runtime.NumCPU())

	zerolog.TimeFieldFormat = zerolog.TimeFormatUnixMs
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})

	httpAddr := getEnv("HTTP_ADDR", ":8080")
	sfuAddr := getEnv("SFU_ADDR", "localhost:50051")
	poolSize := 20 // Tăng pool size

	log.Info().
		Str("httpAddr", httpAddr).
		Str("sfuAddr", sfuAddr).
		Int("poolSize", poolSize).
		Int("numCPU", runtime.NumCPU()).
		Msg("Starting signaling server")

	hub := ws.NewHub()
	go hub.Run()

	sfuManager := sfu.NewManager(sfuAddr, poolSize)
	if err := sfuManager.Start(); err != nil {
		log.Fatal().Err(err).Msg("Failed to start SFU manager")
	}
	defer sfuManager.Stop()

	rt := router.NewRouter(hub, sfuManager)

	muxRouter := mux.NewRouter()

	muxRouter.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		serveWs(hub, rt, w, r)
	})

	muxRouter.HandleFunc("/health", healthHandler)
	muxRouter.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) {
		metricsHandler(w, r, hub, sfuManager)
	})

	muxRouter.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "index.html")
	})

	srv := &http.Server{
		Addr:         httpAddr,
		Handler:      muxRouter,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  120 * time.Second,

		// Performance tuning
		MaxHeaderBytes: 1 << 20, // 1MB
	}

	go func() {
		log.Info().Str("addr", httpAddr).Msg("HTTP server listening")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("Server error")
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit

	log.Info().Msg("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Error().Err(err).Msg("Server forced to shutdown")
	}

	log.Info().Msg("Server stopped")
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"ok"}`))
}

func metricsHandler(w http.ResponseWriter, r *http.Request, hub *ws.Hub, sfuMgr *sfu.Manager) {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	stats := map[string]interface{}{
		"goroutines": runtime.NumGoroutine(),
		"memory": map[string]interface{}{
			"alloc_mb":       m.Alloc / 1024 / 1024,
			"total_alloc_mb": m.TotalAlloc / 1024 / 1024,
			"sys_mb":         m.Sys / 1024 / 1024,
			"num_gc":         m.NumGC,
		},
		"sfu_pool": sfuMgr.GetPoolStats(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(stats)
}

func serveWs(hub *ws.Hub, rt *router.Router, w http.ResponseWriter, r *http.Request) {
	roomID := r.URL.Query().Get("room")
	userID := r.URL.Query().Get("userId")

	if userID == "" || roomID == "" {
		log.Warn().
			Str("userID", userID).
			Str("roomID", roomID).
			Msg("Missing userId or roomId")
		http.Error(w, "Missing userId or room", http.StatusBadRequest)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Error().Err(err).Msg("Failed to upgrade connection")
		return
	}

	clientID := uuid.New().String()
	client := ws.NewClient(clientID, userID, roomID, hub, conn)

	hub.Register <- client

	log.Info().
		Str("clientID", clientID).
		Str("userID", userID).
		Str("roomID", roomID).
		Msg("WebSocket connection established")

	go client.WritePump()
	client.ReadPump(rt)
}
