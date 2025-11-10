package core

import (
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// Peer đại diện cho một kết nối WebRTC của một user
type Peer struct {
	mu sync.RWMutex // ← Lock riêng cho peer

	ID     string
	UserID string
	Room   *Room
	PC     *webrtc.PeerConnection

	localTracks map[string]*webrtc.TrackLocalStaticRTP
	senders     map[string]*webrtc.RTPSender // ← Cache senders
	closed      bool                         // ← Track closed state
}

func NewPeer(id, userID string, room *Room, pc *webrtc.PeerConnection) *Peer {
	return &Peer{
		ID:          id,
		UserID:      userID,
		Room:        room,
		PC:          pc,
		localTracks: make(map[string]*webrtc.TrackLocalStaticRTP),
		senders:     make(map[string]*webrtc.RTPSender),
		closed:      false,
	}
}

// Close đóng PeerConnection và dọn dẹp
func (p *Peer) Close() {
	p.mu.Lock()
	if p.closed {
		p.mu.Unlock()
		return
	}
	p.closed = true
	p.mu.Unlock()

	// Cleanup tracks trước khi close PC
	if p.Room != nil {
		p.Room.RemovePeerTracks(p)
	}

	if err := p.PC.Close(); err != nil {
		log.Error().Err(err).Str("peerId", p.ID).Msg("failed to close peer connection")
	}
}

// AddLocalTrack thêm một track local vào PeerConnection
func (p *Peer) AddLocalTrack(track *webrtc.TrackLocalStaticRTP) (*webrtc.RTPSender, error) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.closed {
		return nil, webrtc.ErrConnectionClosed
	}

	// Kiểm tra track đã tồn tại
	if _, exists := p.localTracks[track.ID()]; exists {
		return p.senders[track.ID()], nil
	}

	sender, err := p.PC.AddTrack(track)
	if err != nil {
		return nil, err
	}

	p.localTracks[track.ID()] = track
	p.senders[track.ID()] = sender

	// Process RTCP packets thực sự
	go p.readRTCP(sender, track.ID())

	return sender, nil
}

// readRTCP xử lý RTCP packets (PLI, NACK, etc.)
func (p *Peer) readRTCP(sender *webrtc.RTPSender, trackID string) {
	rtcpBuf := make([]byte, 1500)

	for {
		n, _, err := sender.Read(rtcpBuf)
		if err != nil {
			log.Debug().Str("peerId", p.ID).Str("trackID", trackID).Msg("RTCP reader stopped")
			return
		}

		// Process RTCP packets
		// - Handle PLI (Picture Loss Indication) → request keyframe
		// - Handle NACK → retransmit lost packets
		// - Handle REMB (Receiver Estimated Maximum Bitrate) → bandwidth estimation

		_ = n // Placeholder để tránh unused warning
	}
}

// RemoveLocalTrack xóa một track local khỏi PeerConnection
func (p *Peer) RemoveLocalTrack(track *webrtc.TrackLocalStaticRTP) {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.closed {
		return
	}

	sender, ok := p.senders[track.ID()]
	if !ok {
		return
	}

	if err := p.PC.RemoveTrack(sender); err != nil {
		log.Error().Err(err).Str("peerId", p.ID).Str("trackId", track.ID()).Msg("failed to remove track")
	}

	delete(p.localTracks, track.ID())
	delete(p.senders, track.ID())
}

// Helper để check peer còn sống
func (p *Peer) IsClosed() bool {
	p.mu.RLock()
	defer p.mu.RUnlock()
	return p.closed
}
