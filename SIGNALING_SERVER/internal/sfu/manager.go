package sfu

import (
	"context"
	"sync"
	"time"

	"github.com/rs/zerolog/log"
)

type Manager struct {
	mu      sync.RWMutex
	targets map[string]*Client
	health  map[string]bool
	load    map[string]int64
	stop    chan struct{}
}

func NewManager() *Manager {
	return &Manager{targets: make(map[string]*Client), health: make(map[string]bool), load: make(map[string]int64), stop: make(chan struct{})}
}
func (m *Manager) AddTarget(addr string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if _, ok := m.targets[addr]; ok {
		return
	}
	m.targets[addr] = nil
	m.health[addr] = false
	m.load[addr] = 0
}
func (m *Manager) RemoveTarget(addr string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if cli, ok := m.targets[addr]; ok && cli != nil {
		cli.Close()
	}
	delete(m.targets, addr)
	delete(m.health, addr)
	delete(m.load, addr)
}
func (m *Manager) Start(ctx context.Context) {
	go m.healthLoop(ctx)
}
func (m *Manager) Stop() {
	close(m.stop)
	m.mu.Lock()
	defer m.mu.Unlock()
	for _, c := range m.targets {
		if c != nil {
			c.Close()
		}
	}
}
func (m *Manager) healthLoop(ctx context.Context) {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			m.checkOnce(ctx)
		case <-m.stop:
			log.Info().Msg("sfu manager stopping")
			return
		}
	}
}
func (m *Manager) checkOnce(ctx context.Context) {
	m.mu.Lock()
	addrs := make([]string, 0, len(m.targets))
	for addr := range m.targets {
		addrs = append(addrs, addr)
	}
	m.mu.Unlock()
	for _, addr := range addrs {
		m.mu.Lock()
		cli := m.targets[addr]
		m.mu.Unlock()
		if cli == nil {
			c, err := NewClient(ctx, addr)
			if err != nil {
				m.setHealth(addr, false)
				log.Warn().Err(err).Str("sfu", addr).Msg("dial sfu failed")
				continue
			}
			m.mu.Lock()
			m.targets[addr] = c
			m.mu.Unlock()
			cli = c
		}
		ctx2, cancel := context.WithTimeout(ctx, 2*time.Second)
		_, _, err := cli.CreatePeer(ctx2, "__health__", "health", "")
		cancel()
		if err != nil {
			m.setHealth(addr, false)
			log.Warn().Err(err).Str("sfu", addr).Msg("sfu health check failed")
		} else {
			m.setHealth(addr, true)
		}
	}
}
func (m *Manager) setHealth(addr string, ok bool) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.health[addr] = ok
}

func (m *Manager) SelectNode() *Client {
	m.mu.RLock()
	defer m.mu.RUnlock()
	var bestAddr string
	var bestLoad int64 = -1
	for addr, ok := range m.health {
		if !ok {
			continue
		}
		cnt := m.load[addr]
		if bestLoad == -1 || cnt < bestLoad {
			bestLoad = cnt
			bestAddr = addr
		}
	}
	if bestAddr == "" {
		return nil
	}
	return m.targets[bestAddr]
}
func (m *Manager) GetClientForAddr(addr string) (*Client, error) {
	m.mu.RLock()
	cli, ok := m.targets[addr]
	m.mu.RUnlock()
	if !ok || cli == nil {
		c, err := NewClient(context.Background(), addr)
		if err != nil {
			return nil, err
		}
		m.mu.Lock()
		m.targets[addr] = c
		m.mu.Unlock()
		return c, nil
	}
	return cli, nil
}
func (m *Manager) IncrementLoad(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.load[addr]++
	return nil
}
func (m *Manager) DecrementLoad(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.load[addr]--
	if m.load[addr] < 0 {
		m.load[addr] = 0
	}
	return nil
}
