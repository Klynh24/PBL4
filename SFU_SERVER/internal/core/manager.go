package core

import (
	"sync"

	"github.com/google/uuid"
	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// Manager là đối tượng singleton quản lý tất cả các phòng và peer
type Manager struct {
	mu    sync.RWMutex
	rooms map[string]*Room
	peers map[string]*Peer
}

func NewManager() *Manager {
	return &Manager{
		rooms: make(map[string]*Room),
		peers: make(map[string]*Peer),
	}
}

// CreatePeer tạo một peer mới và thêm vào phòng
func (m *Manager) CreatePeer(roomID, userID string, pc *webrtc.PeerConnection) *Peer {
	m.mu.Lock()
	defer m.mu.Unlock()

	// Lấy hoặc tạo phòng
	room, exists := m.rooms[roomID]
	if !exists {
		room = NewRoom(roomID)
		m.rooms[roomID] = room
		log.Info().Str("roomID", roomID).Msg("Room created")
	}

	// Sử dụng NewPeer constructor
	peer := NewPeer(uuid.New().String(), userID, room, pc)

	m.peers[peer.ID] = peer
	room.AddPeer(peer)

	return peer
}

// GetPeer tìm một peer bằng ID
func (m *Manager) GetPeer(id string) *Peer {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.peers[id]
}

// RemovePeer xóa một peer khỏi manager và phòng
func (m *Manager) RemovePeer(peer *Peer) {
	m.mu.Lock()

	delete(m.peers, peer.ID)

	var roomToDelete string
	if peer.Room != nil {
		peer.Room.RemovePeer(peer.ID)
		if len(peer.Room.Peers) == 0 {
			roomToDelete = peer.Room.ID
		}
	}
	m.mu.Unlock()

	// Close peer BÊN NGOÀI lock
	peer.Close()

	// Cleanup empty room
	if roomToDelete != "" {
		m.mu.Lock()
		delete(m.rooms, roomToDelete)
		m.mu.Unlock()
		log.Info().Str("roomID", roomToDelete).Msg("Room deleted (empty)")
	}
}

// GetRoomPeerCount đếm số lượng peer trong một phòng
func (m *Manager) GetRoomPeerCount(roomID string) int {
	m.mu.RLock()
	defer m.mu.RUnlock()

	if room, exists := m.rooms[roomID]; exists {
		return len(room.Peers)
	}
	return 0
}

// GetStats trả về số lượng peer và phòng hiện có
func (m *Manager) GetStats() (peerCount int, roomCount int) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return len(m.peers), len(m.rooms)
}

// Cleanup tất cả peers khi shutdown
func (m *Manager) CloseAll() {
	m.mu.Lock()
	peers := make([]*Peer, 0, len(m.peers))
	for _, peer := range m.peers {
		peers = append(peers, peer)
	}
	m.mu.Unlock()

	// Close all peers bên ngoài lock
	for _, peer := range peers {
		peer.Close()
	}

	m.mu.Lock()
	m.peers = make(map[string]*Peer)
	m.rooms = make(map[string]*Room)
	m.mu.Unlock()

	log.Info().Msg("All peers and rooms closed")
}
