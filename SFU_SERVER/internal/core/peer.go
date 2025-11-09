package core

import (
	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// Peer đại diện cho một kết nối WebRTC của một user
type Peer struct {
	ID     string
	UserID string
	Room   *Room
	PC     *webrtc.PeerConnection

	// Danh sách các track local (được tạo ra từ track remote của peer khác)
	// Chúng ta cần giữ lại để có thể remove khi peer rời đi
	localTracks map[string]*webrtc.TrackLocalStaticRTP
}

func NewPeer(id, userID string, room *Room, pc *webrtc.PeerConnection) *Peer {
	return &Peer{
		ID:          id,
		UserID:      userID,
		Room:        room,
		PC:          pc,
		localTracks: make(map[string]*webrtc.TrackLocalStaticRTP),
	}
}

// Close đóng PeerConnection và dọn dẹp
func (p *Peer) Close() {
	if err := p.PC.Close(); err != nil {
		log.Error().Err(err).Str("peerId", p.ID).Msg("failed to close peer connection")
	}
	// Dọn dẹp các track mà peer này đang gửi
	p.Room.RemovePeerTracks(p)
}

// AddLocalTrack thêm một track local (từ một peer khác) vào PeerConnection này
func (p *Peer) AddLocalTrack(track *webrtc.TrackLocalStaticRTP) (*webrtc.RTPSender, error) {
	p.localTracks[track.ID()] = track
	return p.PC.AddTrack(track)
}

// RemoveLocalTrack xóa một track local khỏi PeerConnection
func (p *Peer) RemoveLocalTrack(track *webrtc.TrackLocalStaticRTP) {
	sender, ok := p.localTracks[track.ID()]
	if !ok {
		return
	}
	// Tìm RTP sender tương ứng
	for _, s := range p.PC.GetSenders() {
		if s.Track() == sender {
			if err := p.PC.RemoveTrack(s); err != nil {
				log.Error().Err(err).Str("peerId", p.ID).Str("trackId", track.ID()).Msg("failed to remove track")
			}
			break
		}
	}
	delete(p.localTracks, track.ID())
}
