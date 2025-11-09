package core

import (
	"sync"

	"github.com/google/uuid"
	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// Manager là đối tượng singleton quản lý tất cả các phòng và peer
type Manager struct {
	rooms map[string]*Room
	rMu   sync.RWMutex

	peers map[string]*Peer
	pMu   sync.RWMutex
}

func NewManager() *Manager {
	return &Manager{
		rooms: make(map[string]*Room),
		peers: make(map[string]*Peer),
	}
}

// GetOrCreateRoom tìm hoặc tạo một phòng mới
func (m *Manager) GetOrCreateRoom(roomID string) *Room {
	m.rMu.Lock()
	defer m.rMu.Unlock()

	if room, ok := m.rooms[roomID]; ok {
		return room
	}

	room := NewRoom(roomID)
	m.rooms[roomID] = room
	log.Info().Str("roomId", roomID).Msg("new room created")
	return room
}

// CreatePeer tạo một PeerConnection mới và đăng ký nó
func (m *Manager) CreatePeer(roomID, userID string, pc *webrtc.PeerConnection) *Peer {
	peerID := uuid.NewString()
	room := m.GetOrCreateRoom(roomID)

	peer := NewPeer(peerID, userID, room, pc)

	m.pMu.Lock()
	m.peers[peerID] = peer
	m.pMu.Unlock()

	room.AddPeer(peer)

	log.Info().Str("peerId", peerID).Str("userId", userID).Str("roomId", roomID).Msg("new peer created")
	return peer
}

// GetPeer tìm một peer bằng ID
func (m *Manager) GetPeer(peerID string) *Peer {
	m.pMu.RLock()
	defer m.pMu.RUnlock()
	return m.peers[peerID]
}

// RemovePeer xóa một peer khỏi manager và phòng
func (m *Manager) RemovePeer(peer *Peer) {
	m.pMu.Lock()
	delete(m.peers, peer.ID)
	m.pMu.Unlock()

	peer.Room.RemovePeer(peer)
	peer.Close() // Dọn dẹp PeerConnection

	log.Info().Str("peerId", peer.ID).Str("roomId", peer.Room.ID).Msg("peer removed")

	// Dọn dẹp phòng nếu rỗng
	m.rMu.Lock()
	if len(peer.Room.peers) == 0 {
		delete(m.rooms, peer.Room.ID)
		log.Info().Str("roomId", peer.Room.ID).Msg("empty room cleaned up")
	}
	m.rMu.Unlock()
}
