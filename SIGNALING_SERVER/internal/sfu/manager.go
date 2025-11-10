package sfu

import (
	"context"
	"sync"
	"sync/atomic"
	"time"

	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"google.golang.org/grpc/connectivity"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/keepalive"
)

type Manager struct {
	pool     *ConnectionPool
	sfuAddr  string
	poolSize int

	// Health check
	healthCheckInterval time.Duration
	ctx                 context.Context
	cancel              context.CancelFunc
}

type ConnectionPool struct {
	conns   []*grpc.ClientConn
	clients []*Client
	current atomic.Uint32
	mu      sync.RWMutex

	// Connection state tracking
	healthy []atomic.Bool
}

func NewManager(sfuAddr string, poolSize int) *Manager {
	ctx, cancel := context.WithCancel(context.Background())

	return &Manager{
		sfuAddr:             sfuAddr,
		poolSize:            poolSize,
		healthCheckInterval: 10 * time.Second,
		ctx:                 ctx,
		cancel:              cancel,
	}
}

func (m *Manager) Start() error {
	m.pool = &ConnectionPool{
		conns:   make([]*grpc.ClientConn, m.poolSize),
		clients: make([]*Client, m.poolSize),
		healthy: make([]atomic.Bool, m.poolSize),
	}

	// Create connections with optimized settings
	opts := []grpc.DialOption{
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithKeepaliveParams(keepalive.ClientParameters{
			Time:                10 * time.Second,
			Timeout:             3 * time.Second,
			PermitWithoutStream: true,
		}),
		grpc.WithDefaultCallOptions(
			grpc.MaxCallRecvMsgSize(20*1024*1024), // 20MB
			grpc.MaxCallSendMsgSize(20*1024*1024),
		),
		// Connection pooling
		grpc.WithInitialWindowSize(1 << 20),     // 1MB
		grpc.WithInitialConnWindowSize(1 << 21), // 2MB
	}

	for i := 0; i < m.poolSize; i++ {
		conn, err := grpc.Dial(m.sfuAddr, opts...)
		if err != nil {
			m.cleanup(i)
			return err
		}

		m.pool.conns[i] = conn
		m.pool.clients[i] = NewClient(conn)
		m.pool.healthy[i].Store(true)
	}

	// Start health checker
	go m.healthChecker()

	log.Info().
		Int("poolSize", m.poolSize).
		Str("sfuAddr", m.sfuAddr).
		Msg("SFU connection pool initialized")

	return nil
}

func (m *Manager) GetClient() *Client {
	// Least-loaded selection với fallback to round-robin
	for i := 0; i < m.poolSize; i++ {
		idx := int(m.pool.current.Add(1)) % m.poolSize

		if m.pool.healthy[idx].Load() {
			return m.pool.clients[idx]
		}
	}

	// Fallback: return first available
	idx := int(m.pool.current.Add(1)) % m.poolSize
	return m.pool.clients[idx]
}

func (m *Manager) healthChecker() {
	ticker := time.NewTicker(m.healthCheckInterval)
	defer ticker.Stop()

	for {
		select {
		case <-m.ctx.Done():
			return
		case <-ticker.C:
			m.checkAllConnections()
		}
	}
}

func (m *Manager) checkAllConnections() {
	var wg sync.WaitGroup

	for i := 0; i < m.poolSize; i++ {
		wg.Add(1)
		go func(idx int) {
			defer wg.Done()

			conn := m.pool.conns[idx]
			state := conn.GetState()

			healthy := state == connectivity.Ready || state == connectivity.Idle
			m.pool.healthy[idx].Store(healthy)

			if !healthy {
				log.Warn().
					Int("poolIdx", idx).
					Str("state", state.String()).
					Msg("Unhealthy SFU connection detected")

				// Try to reconnect
				conn.Connect()
			}
		}(i)
	}

	wg.Wait()
}

func (m *Manager) cleanup(upTo int) {
	for i := 0; i < upTo; i++ {
		if m.pool.conns[i] != nil {
			m.pool.conns[i].Close()
		}
	}
}

func (m *Manager) Stop() {
	m.cancel()

	if m.pool == nil {
		return
	}

	for _, conn := range m.pool.conns {
		if conn != nil {
			conn.Close()
		}
	}

	log.Info().Msg("SFU connection pool closed")
}

func (m *Manager) GetPoolStats() map[string]interface{} {
	stats := make(map[string]interface{})

	healthyCount := 0
	for i := 0; i < m.poolSize; i++ {
		if m.pool.healthy[i].Load() {
			healthyCount++
		}
	}

	stats["pool_size"] = m.poolSize
	stats["healthy_connections"] = healthyCount
	stats["unhealthy_connections"] = m.poolSize - healthyCount

	return stats
}
