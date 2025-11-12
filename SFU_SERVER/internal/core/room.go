package core

import (
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

type Room struct {
	id         string
	peers      map[string]*Peer
	mu         sync.RWMutex
	iceServers []webrtc.ICEServer
	webrtcAPI  *webrtc.API // ✅ THÊM
}

// ✅ NewRoom tạo room mới với WebRTC API
func NewRoom(id string, iceServers []webrtc.ICEServer, api *webrtc.API) *Room {
	return &Room{
		id:         id,
		peers:      make(map[string]*Peer),
		iceServers: iceServers,
		webrtcAPI:  api, // ✅ LƯU API
	}
}

// AddPeer thêm peer vào room
func (r *Room) AddPeer(peerID string) (*Peer, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if peer, exists := r.peers[peerID]; exists {
		return peer, nil
	}

	// ✅ Tạo WebRTC configuration
	config := webrtc.Configuration{
		ICEServers: r.iceServers,
	}

	// ✅ Tạo peer với optimized API
	peer, err := NewPeer(peerID, r.id, config, r.webrtcAPI)
	if err != nil {
		log.Error().
			Err(err).
			Str("peerID", peerID).
			Str("roomID", r.id).
			Msg("Failed to create peer")
		return nil, err
	}

	r.peers[peerID] = peer

	log.Info().
		Str("peerID", peerID).
		Str("roomID", r.id).
		Int("totalPeers", len(r.peers)).
		Msg("Peer added to room")

	return peer, nil
}

// RemovePeer xóa peer khỏi room
func (r *Room) RemovePeer(peerID string) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if peer, exists := r.peers[peerID]; exists {
		peer.Close()
		delete(r.peers, peerID)

		log.Info().
			Str("peerID", peerID).
			Str("roomID", r.id).
			Int("remainingPeers", len(r.peers)).
			Msg("Peer removed from room")
	}
}

// GetPeer lấy peer theo ID
func (r *Room) GetPeer(peerID string) (*Peer, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	peer, exists := r.peers[peerID]
	return peer, exists
}

// GetPeers lấy tất cả peers
func (r *Room) GetPeers() []*Peer {
	r.mu.RLock()
	defer r.mu.RUnlock()

	peers := make([]*Peer, 0, len(r.peers))
	for _, peer := range r.peers {
		peers = append(peers, peer)
	}
	return peers
}

// BroadcastTrack broadcast track tới tất cả peers khác
func (r *Room) BroadcastTrack(fromPeerID string, track *webrtc.TrackRemote) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for peerID, peer := range r.peers {
		if peerID != fromPeerID {
			if err := peer.AddTrack(track); err != nil {
				log.Error().
					Err(err).
					Str("fromPeerID", fromPeerID).
					Str("toPeerID", peerID).
					Msg("Failed to broadcast track")
			}
		}
	}
}

// Close đóng room
func (r *Room) Close() {
	r.mu.Lock()
	defer r.mu.Unlock()

	for _, peer := range r.peers {
		peer.Close()
	}
	r.peers = make(map[string]*Peer)

	log.Info().
		Str("roomID", r.id).
		Msg("Room closed")
}

// GetStats lấy thống kê room
func (r *Room) GetStats() map[string]interface{} {
	r.mu.RLock()
	defer r.mu.RUnlock()

	return map[string]interface{}{
		"id":         r.id,
		"totalPeers": len(r.peers),
		"peerIDs":    r.getPeerIDs(),
	}
}

func (r *Room) getPeerIDs() []string {
	ids := make([]string, 0, len(r.peers))
	for id := range r.peers {
		ids = append(ids, id)
	}
	return ids
}
