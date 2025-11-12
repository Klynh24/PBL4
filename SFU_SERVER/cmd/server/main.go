package main

import (
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"

	"google.golang.org/grpc"
	"gopkg.in/yaml.v3"
	"tangthetoan.com/sfu/internal/core"
	grpcserver "tangthetoan.com/sfu/internal/grpc"
	pb "tangthetoan.com/sfu/proto"
)

// Config structure
type Config struct {
	Server struct {
		GrpcAddr   string `yaml:"grpc_addr"`
		UDPPortMin uint16 `yaml:"udp_port_min"`
		UDPPortMax uint16 `yaml:"udp_port_max"`
	} `yaml:"server"`
	IceServers []struct {
		URLs       string `yaml:"urls"`
		Username   string `yaml:"username,omitempty"`
		Credential string `yaml:"credential,omitempty"`
	} `yaml:"ice_servers"`
	Logging struct {
		Level  string `yaml:"level"`
		Format string `yaml:"format"`
	} `yaml:"logging"`
}

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())

	configFile := flag.String("config", "config.yaml", "Path to config file")
	flag.Parse()

	config, err := loadConfig(*configFile)
	if err != nil {
		log.Printf("⚠️  Warning: Failed to load config file '%s': %v", *configFile, err)
		log.Println("📋 Using default configuration...")
		config = getDefaultConfig()
	} else {
		log.Printf("✅ Loaded config from '%s'", *configFile)
	}
	if err := validateConfig(config); err != nil {
		log.Fatalf("❌ Invalid config: %v", err)
	}
	grpcAddr := getEnv("GRPC_ADDR", config.Server.GrpcAddr)

	// Extract ICE server URLs
	var iceServers []string
	for _, server := range config.IceServers {
		if server.URLs != "" {
			iceServers = append(iceServers, server.URLs)
		}
	}

	// Override STUN server from env
	if stunEnv := os.Getenv("STUN_SERVER"); stunEnv != "" {
		log.Printf("🔧 Overriding ICE servers with env: %s", stunEnv)
		iceServers = []string{stunEnv}
	}

	// Validate ICE servers
	if len(iceServers) == 0 {
		log.Fatal("❌ No ICE servers configured")
	}

	log.Printf("🚀 Starting SFU Server...")
	log.Printf("  💻 CPU Cores: %d (GOMAXPROCS=%d)", runtime.NumCPU(), runtime.GOMAXPROCS(0))
	log.Printf("  📡 gRPC Address: %s", grpcAddr)
	log.Printf("  🎯 ICE Servers (%d):", len(iceServers))
	for i, server := range iceServers {
		log.Printf("     [%d] %s", i+1, server)
	}

	// ✅ LOG UDP PORT RANGE
	if config.Server.UDPPortMin > 0 && config.Server.UDPPortMax > 0 {
		portCount := config.Server.UDPPortMax - config.Server.UDPPortMin + 1
		log.Printf("  🔌 UDP Port Range: %d-%d (%d ports)",
			config.Server.UDPPortMin,
			config.Server.UDPPortMax,
			portCount)
	} else {
		log.Printf("  ⚠️  UDP Port Range: System default (ephemeral)")
	}

	log.Printf("  📝 Logging Level: %s", config.Logging.Level)
	log.Printf("  📄 Logging Format: %s", config.Logging.Format)

	// ✅ Create Manager with UDP ports
	manager := core.NewManagerWithPorts(
		iceServers,
		config.Server.UDPPortMin,
		config.Server.UDPPortMax,
	)

	lis, err := net.Listen("tcp", grpcAddr)
	if err != nil {
		log.Fatalf("❌ Failed to listen on %s: %v", grpcAddr, err)
	}

	// ✅ gRPC server with performance options
	grpcServer := grpc.NewServer(
		grpc.MaxRecvMsgSize(1024*1024*10),               // 10MB
		grpc.MaxSendMsgSize(1024*1024*10),               // 10MB
		grpc.MaxConcurrentStreams(1000),                 // ✅ Handle more concurrent streams
		grpc.NumStreamWorkers(uint32(runtime.NumCPU())), // ✅ Worker threads = CPU cores
	)

	sfuServer := grpcserver.NewServer(manager)
	pb.RegisterIonSFUServer(grpcServer, sfuServer)

	// Graceful shutdown
	go func() {
		sigChan := make(chan os.Signal, 1)
		signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
		sig := <-sigChan

		log.Printf("\n🛑 Received signal: %v", sig)
		log.Println("⏳ Shutting down gracefully...")

		// Shutdown with timeout
		done := make(chan struct{})
		go func() {
			sfuServer.Shutdown()
			grpcServer.GracefulStop()
			close(done)
		}()

		select {
		case <-done:
			log.Println("✅ Server stopped gracefully")
		case <-time.After(10 * time.Second):
			log.Println("⚠️  Shutdown timeout, forcing stop...")
			grpcServer.Stop()
		}

		os.Exit(0)
	}()

	log.Printf("✅ SFU Server listening on %s", grpcAddr)
	log.Println("📡 Ready to accept connections...")

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("❌ Failed to serve: %v", err)
	}
}

func loadConfig(filename string) (*Config, error) {
	data, err := os.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse YAML: %w", err)
	}

	return &config, nil
}

func validateConfig(config *Config) error {
	if config.Server.GrpcAddr == "" {
		return fmt.Errorf("server.grpc_addr is required")
	}

	if len(config.IceServers) == 0 {
		return fmt.Errorf("at least one ICE server is required")
	}

	validLevels := map[string]bool{"debug": true, "info": true, "warn": true, "error": true}
	if !validLevels[config.Logging.Level] {
		return fmt.Errorf("invalid logging level: %s (must be debug/info/warn/error)", config.Logging.Level)
	}

	// ✅ Validate UDP ports
	if config.Server.UDPPortMin > 0 && config.Server.UDPPortMax > 0 {
		if config.Server.UDPPortMin >= config.Server.UDPPortMax {
			return fmt.Errorf("udp_port_min (%d) must be less than udp_port_max (%d)",
				config.Server.UDPPortMin, config.Server.UDPPortMax)
		}
		if config.Server.UDPPortMin < 1024 {
			return fmt.Errorf("udp_port_min must be >= 1024 (got %d)", config.Server.UDPPortMin)
		}
		if config.Server.UDPPortMax > 65535 {
			return fmt.Errorf("udp_port_max must be <= 65535 (got %d)", config.Server.UDPPortMax)
		}
	}

	return nil
}

func getDefaultConfig() *Config {
	return &Config{
		Server: struct {
			GrpcAddr   string `yaml:"grpc_addr"`
			UDPPortMin uint16 `yaml:"udp_port_min"`
			UDPPortMax uint16 `yaml:"udp_port_max"`
		}{
			GrpcAddr:   ":50051",
			UDPPortMin: 50000, // ✅ DEFAULT
			UDPPortMax: 50100, // ✅ DEFAULT
		},
		IceServers: []struct {
			URLs       string `yaml:"urls"`
			Username   string `yaml:"username,omitempty"`
			Credential string `yaml:"credential,omitempty"`
		}{
			{URLs: "stun:stun.l.google.com:19302"},
		},
		Logging: struct {
			Level  string `yaml:"level"`
			Format string `yaml:"format"`
		}{
			Level:  "info",
			Format: "console",
		},
	}
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}
