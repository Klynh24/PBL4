package server

import (
	"fmt"
	"log"
	"net"

	"tangthetoan.com/stun/stun"
)

const (
	DefaultPort     = 3478
	MaxMessageSize  = 1500
	SoftwareVersion = "GoSTUN/1.0"
)

// Server represents a STUN server
type Server struct {
	conn *net.UDPConn
	port int
}

// NewServer creates a new STUN server
func NewServer(port int) *Server {
	if port == 0 {
		port = DefaultPort
	}
	return &Server{port: port}
}

// Start starts the STUN server
func (s *Server) Start() error {
	addr := &net.UDPAddr{
		Port: s.port,
		IP:   net.ParseIP("0.0.0.0"),
	}

	conn, err := net.ListenUDP("udp", addr)
	if err != nil {
		return fmt.Errorf("failed to listen on UDP: %w", err)
	}

	s.conn = conn
	log.Printf("STUN Server started on %s", addr)

	return s.handleRequests()
}

// handleRequests processes incoming STUN requests
func (s *Server) handleRequests() error {
	buffer := make([]byte, MaxMessageSize)

	for {
		n, clientAddr, err := s.conn.ReadFromUDP(buffer)
		if err != nil {
			log.Printf("Error reading UDP: %v", err)
			continue
		}

		log.Printf("Received %d bytes from %s", n, clientAddr)

		// Process in goroutine for concurrent handling
		go s.handleRequest(buffer[:n], clientAddr)
	}
}

// handleRequest processes a single STUN request
func (s *Server) handleRequest(data []byte, clientAddr *net.UDPAddr) {
	// Parse STUN message
	msg, err := stun.Parse(data)
	if err != nil {
		log.Printf("Error parsing STUN message: %v", err)
		return
	}

	log.Printf("Parsed message: %s from %s", msg, clientAddr)

	// Only handle Binding Requests
	if !msg.IsBindingRequest() {
		log.Printf("Ignoring non-binding request: 0x%04x", msg.Type)
		return
	}

	// Create Binding Response
	response := stun.NewMessage(stun.BindingResponse)
	response.TransactionID = msg.TransactionID

	// Add XOR-MAPPED-ADDRESS attribute
	xorMappedAddr := stun.NewXorMappedAddress(clientAddr, msg.TransactionID)
	response.AddAttribute(xorMappedAddr)

	// Add SOFTWARE attribute
	software := stun.NewSoftwareAttribute(SoftwareVersion)
	response.AddAttribute(software)

	// Serialize response
	responseData, err := response.Serialize()
	if err != nil {
		log.Printf("Error serializing response: %v", err)
		return
	}

	// Send response
	_, err = s.conn.WriteToUDP(responseData, clientAddr)
	if err != nil {
		log.Printf("Error sending response: %v", err)
		return
	}

	log.Printf("Sent Binding Response to %s", clientAddr)
}

// Stop stops the STUN server
func (s *Server) Stop() error {
	if s.conn != nil {
		return s.conn.Close()
	}
	return nil
}
