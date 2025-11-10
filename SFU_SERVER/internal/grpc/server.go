package grpc

import (
	"context"
	"encoding/json"
	"time"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"

	"tangthetoan.com/sfu/internal/core"
	"tangthetoan.com/sfu/proto"
)

// Server implement proto.IonSFUServer
type Server struct {
	proto.UnimplementedIonSFUServer
	manager   *core.Manager
	stunURL   string
	startTime time.Time
}

func NewServer(stunURL string) *Server {
	return &Server{
		manager:   core.NewManager(),
		stunURL:   stunURL,
		startTime: time.Now(),
	}
}

// ✅ THÊM: Graceful shutdown
func (s *Server) Shutdown() {
	log.Info().Msg("Shutting down SFU server...")
	s.manager.CloseAll()
}

func (s *Server) HealthCheck(ctx context.Context, req *proto.HealthCheckRequest) (*proto.HealthCheckResponse, error) {
	peerCount, roomCount := s.manager.GetStats()

	return &proto.HealthCheckResponse{
		Status:        "healthy",
		ActivePeers:   int32(peerCount),
		ActiveRooms:   int32(roomCount),
		UptimeSeconds: int64(time.Since(s.startTime).Seconds()),
	}, nil
}

// 1. CreatePeer: Nhận offer từ client, tạo PeerConnection, và trả về answer
func (s *Server) CreatePeer(ctx context.Context, req *proto.CreatePeerRequest) (*proto.CreatePeerResponse, error) {
	// Health check bypass
	log.Info().
		Str("room", req.Room).
		Str("user", req.UserId).
		Msg("CreatePeer request received")

	// Configure WebRTC
	config := webrtc.Configuration{
		ICEServers: []webrtc.ICEServer{
			{URLs: []string{s.stunURL}},
		},
	}

	pc, err := webrtc.NewPeerConnection(config)
	if err != nil {
		log.Error().Err(err).Msg("failed to create peer connection")
		return nil, err
	}

	peer := s.manager.CreatePeer(req.Room, req.UserId, pc)

	// Handle incoming tracks
	pc.OnTrack(func(track *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {
		log.Info().
			Str("peerId", peer.ID).
			Str("kind", track.Kind().String()).
			Str("id", track.ID()).
			Msg("OnTrack received")

		peer.Room.ForwardTrack(peer.ID, track)
	})

	// Handle ICE connection state changes
	pc.OnICEConnectionStateChange(func(state webrtc.ICEConnectionState) {
		log.Debug().
			Str("peerId", peer.ID).
			Str("state", state.String()).
			Msg("ICE connection state changed")

		if state == webrtc.ICEConnectionStateFailed ||
			state == webrtc.ICEConnectionStateDisconnected ||
			state == webrtc.ICEConnectionStateClosed {
			log.Warn().Str("peerId", peer.ID).Msg("Peer ICE connection closed/failed")
			s.manager.RemovePeer(peer)
		}
	})

	// Wait for ICE gathering to complete
	iceGatheringFinished := webrtc.GatheringCompletePromise(pc)

	// Set remote description
	offer := webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.Sdp}
	if err := pc.SetRemoteDescription(offer); err != nil {
		log.Error().Err(err).Msg("failed to set remote description")
		s.manager.RemovePeer(peer)
		return nil, err
	}

	// Create answer
	answer, err := pc.CreateAnswer(nil)
	if err != nil {
		log.Error().Err(err).Msg("failed to create answer")
		s.manager.RemovePeer(peer)
		return nil, err
	}

	// Set local description
	if err := pc.SetLocalDescription(answer); err != nil {
		log.Error().Err(err).Msg("failed to set local description")
		s.manager.RemovePeer(peer)
		return nil, err
	}

	// Wait for ICE gathering
	<-iceGatheringFinished

	finalAnswer := pc.LocalDescription()
	log.Info().
		Str("peerId", peer.ID).
		Str("room", req.Room).
		Int("peerCount", s.manager.GetRoomPeerCount(req.Room)).
		Msg("Peer created successfully")

	return &proto.CreatePeerResponse{
		PeerId:    peer.ID,
		AnswerSdp: finalAnswer.SDP,
	}, nil
}

// 2. AddICECandidate: Nhận candidate từ client (Trickle ICE)
func (s *Server) AddICECandidate(ctx context.Context, req *proto.AddCandidateRequest) (*proto.Ack, error) {
	log.Debug().Str("peerId", req.PeerId).Msg("AddICECandidate request received")

	peer := s.manager.GetPeer(req.PeerId)
	if peer == nil {
		log.Warn().Str("peerId", req.PeerId).Msg("peer not found for candidate")
		return &proto.Ack{Ok: false, Message: "peer not found"}, nil
	}

	var candidate webrtc.ICECandidateInit
	if err := json.Unmarshal([]byte(req.Candidate), &candidate); err != nil {
		log.Warn().Err(err).Msg("failed to unmarshal candidate")
		return &proto.Ack{Ok: false, Message: "invalid candidate format"}, err
	}

	if err := peer.PC.AddICECandidate(candidate); err != nil {
		log.Warn().Err(err).Str("peerId", req.PeerId).Msg("failed to add ice candidate")
		return &proto.Ack{Ok: false, Message: err.Error()}, err
	}

	return &proto.Ack{Ok: true, Message: "candidate added"}, nil
}

// 3. ClosePeer: Xử lý khi client chủ động rời đi
func (s *Server) ClosePeer(ctx context.Context, req *proto.ClosePeerRequest) (*proto.Ack, error) {
	log.Info().
		Str("peerId", req.PeerId).
		Str("reason", req.Reason).
		Msg("ClosePeer request received")

	peer := s.manager.GetPeer(req.PeerId)
	if peer != nil {
		s.manager.RemovePeer(peer)
		return &proto.Ack{Ok: true, Message: "peer removed"}, nil
	}

	return &proto.Ack{Ok: false, Message: "peer not found"}, nil
}
