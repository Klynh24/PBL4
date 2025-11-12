package core

import (
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

type Manager struct {
	rooms      map[string]*Room
	mu         sync.RWMutex
	iceServers []webrtc.ICEServer
	webrtcAPI  *webrtc.API // ✅ THÊM
}

// NewManager khởi tạo manager với default ports
func NewManager(stunServers []string) *Manager {
	return NewManagerWithPorts(stunServers, 0, 0)
}

// ✅ NewManagerWithPorts khởi tạo với custom UDP ports (OPTIMIZED)
func NewManagerWithPorts(stunServers []string, udpMin, udpMax uint16) *Manager {
	// Parse STUN/TURN servers
	var iceServers []webrtc.ICEServer
	for _, server := range stunServers {
		iceServers = append(iceServers, webrtc.ICEServer{
			URLs: []string{server},
		})
	}

	// Fallback to localhost STUN
	if len(iceServers) == 0 {
		log.Warn().Msg("No STUN servers configured, using localhost:3478")
		iceServers = []webrtc.ICEServer{
			{URLs: []string{"stun:localhost:3478"}},
		}
	}

	// ✅ Create optimized WebRTC API
	config := WebRTCConfig{
		UDPPortMin: udpMin,
		UDPPortMax: udpMax,
		ICEServers: iceServers,
	}
	api := CreateWebRTCAPI(config)

	log.Info().
		Int("iceServers", len(iceServers)).
		Uint16("udpMin", udpMin).
		Uint16("udpMax", udpMax).
		Msg("Manager initialized with optimizations")

	return &Manager{
		rooms:      make(map[string]*Room),
		iceServers: iceServers,
		webrtcAPI:  api, // ✅ LƯU API
	}
}

// GetOrCreateRoom tìm hoặc tạo room mới
func (m *Manager) GetOrCreateRoom(roomID string) *Room {
	m.mu.Lock()
	defer m.mu.Unlock()

	if room, exists := m.rooms[roomID]; exists {
		return room
	}

	// ✅ Truyền webrtcAPI vào room
	room := NewRoom(roomID, m.iceServers, m.webrtcAPI)
	m.rooms[roomID] = room

	log.Info().
		Str("roomID", roomID).
		Int("totalRooms", len(m.rooms)).
		Msg("Created new room")

	return room
}

// GetRoom lấy room theo ID
func (m *Manager) GetRoom(roomID string) (*Room, bool) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	room, exists := m.rooms[roomID]
	return room, exists
}

// RemoveRoom xóa room
func (m *Manager) RemoveRoom(roomID string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if room, exists := m.rooms[roomID]; exists {
		room.Close()
		delete(m.rooms, roomID)

		log.Info().
			Str("roomID", roomID).
			Int("remainingRooms", len(m.rooms)).
			Msg("Room removed")
	}
}

// GetStats lấy thống kê
func (m *Manager) GetStats() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	stats := map[string]interface{}{
		"totalRooms": len(m.rooms),
		"rooms":      make(map[string]interface{}),
	}

	for roomID, room := range m.rooms {
		stats["rooms"].(map[string]interface{})[roomID] = room.GetStats()
	}

	return stats
}
