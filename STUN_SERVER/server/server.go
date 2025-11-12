package server

import (
	"fmt"
	"log"
	"net"
	"sync"
	"sync/atomic"
	"time"

	"tangthetoan.com/stun/stun"
)

const (
	DefaultPort     = 3478
	MaxMessageSize  = 1500
	SoftwareVersion = "GoSTUN/1.0"

	// Performance tuning
	ReadBufferSize  = 4 * 1024 * 1024 // 4MB
	WriteBufferSize = 4 * 1024 * 1024 // 4MB
)

// Server represents a STUN server
type Server struct {
	conn *net.UDPConn
	port int

	// Shutdown control
	done chan struct{}
	wg   sync.WaitGroup

	// Metrics
	requestCount  uint64
	responseCount uint64
	errorCount    uint64

	// Buffer pool for memory optimization
	bufferPool *sync.Pool
}

// NewServer creates a new STUN server
func NewServer(port int) *Server {
	if port == 0 {
		port = DefaultPort
	}

	return &Server{
		port: port,
		done: make(chan struct{}),
		bufferPool: &sync.Pool{
			New: func() interface{} {
				return make([]byte, MaxMessageSize)
			},
		},
	}
}

// Start starts the STUN server
func (s *Server) Start() error {
	addr := &net.UDPAddr{
		Port: s.port,
		IP:   net.ParseIP("0.0.0.0"), // ✅ Listen on all interfaces
	}

	conn, err := net.ListenUDP("udp", addr)
	if err != nil {
		return fmt.Errorf("failed to listen on UDP: %w", err)
	}

	s.conn = conn

	// ✅ Set socket buffer sizes for better performance
	if err := conn.SetReadBuffer(ReadBufferSize); err != nil {
		log.Printf("Warning: failed to set read buffer: %v", err)
	}
	if err := conn.SetWriteBuffer(WriteBufferSize); err != nil {
		log.Printf("Warning: failed to set write buffer: %v", err)
	}

	log.Printf("✅ STUN Server started on 0.0.0.0:%d", s.port)
	log.Printf("📊 Buffer sizes: Read=%dMB, Write=%dMB",
		ReadBufferSize/1024/1024, WriteBufferSize/1024/1024)

	// ✅ Start metrics logger
	go s.logMetrics()

	return s.handleRequests()
}

// handleRequests processes incoming STUN requests
func (s *Server) handleRequests() error {
	for {
		select {
		case <-s.done:
			log.Println("📴 Stopping request handler...")
			s.wg.Wait() // Wait for all goroutines to finish
			return nil
		default:
		}

		// ✅ Get buffer from pool
		buffer := s.bufferPool.Get().([]byte)

		// ✅ Set read deadline to prevent blocking forever
		s.conn.SetReadDeadline(time.Now().Add(1 * time.Second))

		n, clientAddr, err := s.conn.ReadFromUDP(buffer)

		if err != nil {
			s.bufferPool.Put(buffer) // Return buffer to pool

			// Check if it's a timeout (expected during shutdown)
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}

			// Check if server is shutting down
			select {
			case <-s.done:
				return nil
			default:
				log.Printf("⚠️  Error reading UDP: %v", err)
				atomic.AddUint64(&s.errorCount, 1)
				continue
			}
		}

		atomic.AddUint64(&s.requestCount, 1)

		// ✅ Make a copy of data for the goroutine
		data := make([]byte, n)
		copy(data, buffer[:n])
		s.bufferPool.Put(buffer) // Return buffer immediately

		// Process in goroutine for concurrent handling
		s.wg.Add(1)
		go func(d []byte, addr *net.UDPAddr) {
			defer s.wg.Done()
			s.handleRequest(d, addr)
		}(data, clientAddr)
	}
}

// handleRequest processes a single STUN request
func (s *Server) handleRequest(data []byte, clientAddr *net.UDPAddr) {
	// Parse STUN message
	msg, err := stun.Parse(data)
	if err != nil {
		log.Printf("❌ Error parsing STUN message from %s: %v", clientAddr, err)
		atomic.AddUint64(&s.errorCount, 1)
		return
	}

	log.Printf("📥 Received %s from %s (TxID: %x)",
		msg.TypeString(), clientAddr, msg.TransactionID[:8])

	// Only handle Binding Requests
	if !msg.IsBindingRequest() {
		log.Printf("⏭️  Ignoring non-binding request: %s (0x%04x)",
			msg.TypeString(), msg.Type)
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
		log.Printf("❌ Error serializing response: %v", err)
		atomic.AddUint64(&s.errorCount, 1)
		return
	}

	// Send response
	_, err = s.conn.WriteToUDP(responseData, clientAddr)
	if err != nil {
		log.Printf("❌ Error sending response to %s: %v", clientAddr, err)
		atomic.AddUint64(&s.errorCount, 1)
		return
	}

	atomic.AddUint64(&s.responseCount, 1)
	log.Printf("📤 Sent Binding Response to %s (%d bytes)", clientAddr, len(responseData))
}

// logMetrics periodically logs server statistics
func (s *Server) logMetrics() {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-s.done:
			return
		case <-ticker.C:
			requests := atomic.LoadUint64(&s.requestCount)
			responses := atomic.LoadUint64(&s.responseCount)
			errors := atomic.LoadUint64(&s.errorCount)

			log.Printf("📊 Stats: Requests=%d, Responses=%d, Errors=%d, Success Rate=%.1f%%",
				requests, responses, errors,
				float64(responses)/float64(requests+1)*100)
		}
	}
}

// Stop gracefully stops the STUN server
func (s *Server) Stop() error {
	log.Println("🛑 Stopping STUN server...")

	// Signal shutdown
	close(s.done)

	// Close connection
	if s.conn != nil {
		if err := s.conn.Close(); err != nil {
			return err
		}
	}

	// Wait for all goroutines
	s.wg.Wait()

	// Log final metrics
	log.Printf("📊 Final Stats: Requests=%d, Responses=%d, Errors=%d",
		atomic.LoadUint64(&s.requestCount),
		atomic.LoadUint64(&s.responseCount),
		atomic.LoadUint64(&s.errorCount))

	log.Println("✅ STUN server stopped")
	return nil
}
