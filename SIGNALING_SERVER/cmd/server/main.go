package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"runtime"
	"syscall"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"gopkg.in/yaml.v3"

	"tangthetoan.com/signaling-server/internal/router"
	"tangthetoan.com/signaling-server/internal/sfu"
	"tangthetoan.com/signaling-server/internal/ws"
)

// Config structure
type Config struct {
	Server struct {
		Addr           string `yaml:"addr"`
		WSPath         string `yaml:"ws_path"`
		JWTPubKeyPath  string `yaml:"jwt_pubkey_path"`
		MaxMessageSize int    `yaml:"max_message_size"`
	} `yaml:"server"`
	SFU struct {
		Addr     string `yaml:"addr"`
		PoolSize int    `yaml:"pool_size"`
	} `yaml:"sfu"`
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
	EnableCompression: true,
}

func main() {
	// Command-line flags
	configFile := flag.String("config", "configs/config.yaml", "Path to config file")
	flag.Parse()

	// Optimize Go runtime
	runtime.GOMAXPROCS(runtime.NumCPU())

	// Setup logger
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnixMs
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stderr, TimeFormat: time.RFC3339})

	// Load config
	config, err := loadConfig(*configFile)
	if err != nil {
		log.Fatal().Err(err).Str("file", *configFile).Msg("Failed to load config file")
	}

	// Override with environment variables if set
	httpAddr := getEnv("HTTP_ADDR", config.Server.Addr)
	sfuAddr := getEnv("SFU_ADDR", config.SFU.Addr)
	poolSize := config.SFU.PoolSize

	// Update upgrader buffer sizes from config
	if config.Server.MaxMessageSize > 0 {
		upgrader.ReadBufferSize = config.Server.MaxMessageSize
		upgrader.WriteBufferSize = config.Server.MaxMessageSize
	}

	log.Info().
		Str("configFile", *configFile).
		Str("httpAddr", httpAddr).
		Str("sfuAddr", sfuAddr).
		Int("poolSize", poolSize).
		Int("maxMessageSize", config.Server.MaxMessageSize).
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

	// WebSocket endpoint (use config path)
	wsPath := config.Server.WSPath
	if wsPath == "" {
		wsPath = "/ws"
	}
	muxRouter.HandleFunc(wsPath, func(w http.ResponseWriter, r *http.Request) {
		serveWs(hub, rt, w, r)
	})

	// Health check endpoint
	muxRouter.HandleFunc("/health", healthHandler)

	// Metrics endpoint
	muxRouter.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) {
		metricsHandler(w, r, hub, sfuManager)
	})

	// Serve static files
	publicDir := filepath.Join("..", "..", "public")

	muxRouter.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		indexPath := filepath.Join(publicDir, "Test-WebRTC.html")

		// Check if file exists
		if _, err := os.Stat(indexPath); os.IsNotExist(err) {
			log.Error().Str("path", indexPath).Msg("Test-WebRTC.html not found")
			http.Error(w, "Test-WebRTC.html not found", http.StatusNotFound)
			return
		}

		http.ServeFile(w, r, indexPath)
	})

	// Serve other static files
	fileServer := http.FileServer(http.Dir(publicDir))
	muxRouter.PathPrefix("/").Handler(fileServer)

	// HTTP Server
	srv := &http.Server{
		Addr:           httpAddr,
		Handler:        muxRouter,
		ReadTimeout:    15 * time.Second,
		WriteTimeout:   15 * time.Second,
		IdleTimeout:    120 * time.Second,
		MaxHeaderBytes: 1 << 20, // 1MB
	}

	// Start server
	go func() {
		log.Info().
			Str("addr", httpAddr).
			Str("wsPath", wsPath).
			Str("publicDir", publicDir).
			Msg("HTTP server listening")

		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatal().Err(err).Msg("Server error")
		}
	}()

	// Graceful shutdown
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

// loadConfig loads configuration from YAML file
func loadConfig(filename string) (*Config, error) {
	data, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read config file: %w", err)
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse YAML: %w", err)
	}

	// Validate config
	if err := validateConfig(&config); err != nil {
		return nil, err
	}

	return &config, nil
}

// validateConfig validates the configuration
func validateConfig(config *Config) error {
	if config.Server.Addr == "" {
		return fmt.Errorf("server.addr is required")
	}

	if config.SFU.Addr == "" {
		return fmt.Errorf("sfu.addr is required")
	}

	if config.SFU.PoolSize <= 0 {
		return fmt.Errorf("sfu.pool_size must be greater than 0")
	}

	return nil
}

// getEnv gets environment variable or returns fallback
func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

// healthHandler handles health check requests
func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"status":    "ok",
		"timestamp": time.Now().Unix(),
	})
}

// metricsHandler handles metrics requests
func metricsHandler(w http.ResponseWriter, r *http.Request, hub *ws.Hub, sfuMgr *sfu.Manager) {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	stats := map[string]interface{}{
		"timestamp":  time.Now().Unix(),
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

// serveWs handles WebSocket connection requests
func serveWs(hub *ws.Hub, rt *router.Router, w http.ResponseWriter, r *http.Request) {
	roomID := r.URL.Query().Get("room")
	userID := r.URL.Query().Get("userId")

	if userID == "" || roomID == "" {
		log.Warn().
			Str("userID", userID).
			Str("roomID", roomID).
			Msg("Missing userId or roomId")
		http.Error(w, "Missing userId or room parameter", http.StatusBadRequest)
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
		Str("remoteAddr", r.RemoteAddr).
		Msg("WebSocket connection established")

	go client.WritePump()
	client.ReadPump(rt)
}
