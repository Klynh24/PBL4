package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"tangthetoan.com/stun/server"
)

func main() {
	port := flag.Int("port", 3478, "STUN server port")
	flag.Parse()

	srv := server.NewServer(*port)

	// Handle graceful shutdown
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	go func() {
		<-sigChan
		log.Println("\nShutting down server...")
		srv.Stop()
		os.Exit(0)
	}()

	log.Fatal(srv.Start())
}
