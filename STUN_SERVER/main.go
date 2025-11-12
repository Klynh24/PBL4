package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"tangthetoan.com/stun/server"
)

func main() {
	port := flag.Int("port", 3478, "STUN server port")
	flag.Parse()

	log.Printf("🚀 Starting STUN Server on port %d...", *port)

	srv := server.NewServer(*port)

	// Graceful shutdown channel
	done := make(chan error, 1)
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	// Start server in goroutine
	go func() {
		done <- srv.Start()
	}()

	// Wait for signal or error
	select {
	case err := <-done:
		if err != nil {
			log.Fatalf("❌ Server error: %v", err)
		}
	case sig := <-sigChan:
		log.Printf("\n🛑 Received signal: %v", sig)
		log.Println("⏳ Shutting down gracefully...")

		// Stop server with timeout
		shutdownChan := make(chan error, 1)
		go func() {
			shutdownChan <- srv.Stop()
		}()

		select {
		case err := <-shutdownChan:
			if err != nil {
				log.Printf("⚠️  Shutdown error: %v", err)
			} else {
				log.Println("✅ Server stopped gracefully")
			}
		case <-time.After(5 * time.Second):
			log.Println("⚠️  Shutdown timeout, forcing exit")
		}
	}
}
