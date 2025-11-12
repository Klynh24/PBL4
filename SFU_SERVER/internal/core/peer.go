package core

import (
	"fmt"
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

type Peer struct {
	ID             string
	RoomID         string
	PeerConnection *webrtc.PeerConnection
	mu             sync.RWMutex
	tracks         []*webrtc.TrackLocalStaticRTP
}

// ✅ NewPeer tạo peer với optimized WebRTC API
func NewPeer(id, roomID string, config webrtc.Configuration, api *webrtc.API) (*Peer, error) {
	log.Debug().
		Str("peerID", id).
		Interface("iceServers", config.ICEServers).
		Msg("Creating peer with optimized API")

	// ✅ Sử dụng api.NewPeerConnection thay vì webrtc.NewPeerConnection
	pc, err := api.NewPeerConnection(config)
	if err != nil {
		return nil, err
	}

	peer := &Peer{
		ID:             id,
		RoomID:         roomID,
		PeerConnection: pc,
		tracks:         make([]*webrtc.TrackLocalStaticRTP, 0),
	}

	peer.setupEventHandlers()

	return peer, nil
}

func (p *Peer) setupEventHandlers() {
	// ICE Connection State
	p.PeerConnection.OnICEConnectionStateChange(func(state webrtc.ICEConnectionState) {
		log.Info().
			Str("peerID", p.ID).
			Str("roomID", p.RoomID).
			Str("state", state.String()).
			Msg("ICE connection state changed")
	})

	// ✅ ICE Candidate - Only log important ones
	p.PeerConnection.OnICECandidate(func(candidate *webrtc.ICECandidate) {
		if candidate != nil {
			log.Debug().
				Str("peerID", p.ID).
				Str("type", candidate.Typ.String()).
				Msg("New ICE candidate")
		}
	})

	// Connection State
	p.PeerConnection.OnConnectionStateChange(func(state webrtc.PeerConnectionState) {
		log.Info().
			Str("peerID", p.ID).
			Str("state", state.String()).
			Msg("Connection state changed")
	})

	// Track received
	p.PeerConnection.OnTrack(func(track *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {
		log.Info().
			Str("peerID", p.ID).
			Str("trackID", track.ID()).
			Str("kind", track.Kind().String()).
			Str("codec", track.Codec().MimeType).
			Msg("Track received")
	})
}

// CreateOffer tạo SDP offer
func (p *Peer) CreateOffer() (*webrtc.SessionDescription, error) {
	offer, err := p.PeerConnection.CreateOffer(nil)
	if err != nil {
		return nil, err
	}

	if err := p.PeerConnection.SetLocalDescription(offer); err != nil {
		return nil, err
	}

	return &offer, nil
}

// CreateAnswer tạo SDP answer
func (p *Peer) CreateAnswer() (string, error) {
	answer, err := p.PeerConnection.CreateAnswer(nil)
	if err != nil {
		return "", err
	}

	if err := p.PeerConnection.SetLocalDescription(answer); err != nil {
		return "", err
	}

	// ✅ Wait for ICE gathering (with timeout via channel)
	gatherComplete := webrtc.GatheringCompletePromise(p.PeerConnection)
	<-gatherComplete

	if p.PeerConnection.LocalDescription() == nil {
		return "", fmt.Errorf("local description is nil")
	}

	return p.PeerConnection.LocalDescription().SDP, nil
}

// SetRemoteDescription set remote SDP
func (p *Peer) SetRemoteDescription(sdp webrtc.SessionDescription) error {
	return p.PeerConnection.SetRemoteDescription(sdp)
}

// AddICECandidate thêm ICE candidate
func (p *Peer) AddICECandidate(candidate webrtc.ICECandidateInit) error {
	return p.PeerConnection.AddICECandidate(candidate)
}

// AddTrack thêm track để gửi
func (p *Peer) AddTrack(track *webrtc.TrackRemote) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	// Tạo local track để forward
	localTrack, err := webrtc.NewTrackLocalStaticRTP(
		track.Codec().RTPCodecCapability,
		track.ID(),
		track.StreamID(),
	)
	if err != nil {
		return err
	}

	// Add track vào peer connection
	_, err = p.PeerConnection.AddTrack(localTrack)
	if err != nil {
		return err
	}

	p.tracks = append(p.tracks, localTrack)

	log.Info().
		Str("peerID", p.ID).
		Str("trackID", track.ID()).
		Str("codec", track.Codec().MimeType).
		Msg("Track added to peer")

	return nil
}

// Close đóng peer connection
func (p *Peer) Close() error {
	p.mu.Lock()
	defer p.mu.Unlock()

	if p.PeerConnection != nil {
		return p.PeerConnection.Close()
	}
	return nil
}

// GetStats lấy thống kê
func (p *Peer) GetStats() map[string]interface{} {
	p.mu.RLock()
	defer p.mu.RUnlock()

	return map[string]interface{}{
		"id":              p.ID,
		"roomID":          p.RoomID,
		"connectionState": p.PeerConnection.ConnectionState().String(),
		"iceState":        p.PeerConnection.ICEConnectionState().String(),
		"totalTracks":     len(p.tracks),
	}
}
