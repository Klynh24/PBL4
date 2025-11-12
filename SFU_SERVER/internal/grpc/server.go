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

type Server struct {
	proto.UnimplementedIonSFUServer
	manager   *core.Manager
	startTime time.Time
}

func NewServer(manager *core.Manager) *Server {
	return &Server{
		manager:   manager,
		startTime: time.Now(),
	}
}

func (s *Server) Shutdown() {
	log.Info().Msg("Shutting down SFU server...")
	stats := s.manager.GetStats()
	if rooms, ok := stats["rooms"].(map[string]interface{}); ok {
		for roomID := range rooms {
			s.manager.RemoveRoom(roomID)
		}
	}
}

func (s *Server) HealthCheck(ctx context.Context, req *proto.HealthCheckRequest) (*proto.HealthCheckResponse, error) {
	stats := s.manager.GetStats()
	totalRooms := stats["totalRooms"].(int)

	totalPeers := 0
	if rooms, ok := stats["rooms"].(map[string]interface{}); ok {
		for _, roomData := range rooms {
			if roomStats, ok := roomData.(map[string]interface{}); ok {
				if peerCount, ok := roomStats["totalPeers"].(int); ok {
					totalPeers += peerCount
				}
			}
		}
	}

	return &proto.HealthCheckResponse{
		Status:        "healthy",
		ActivePeers:   int32(totalPeers),
		ActiveRooms:   int32(totalRooms),
		UptimeSeconds: int64(time.Since(s.startTime).Seconds()),
	}, nil
}

func (s *Server) CreatePeer(ctx context.Context, req *proto.CreatePeerRequest) (*proto.CreatePeerResponse, error) {
	log.Info().
		Str("room", req.Room).
		Str("userId", req.UserId).
		Msg("CreatePeer request received")

	room := s.manager.GetOrCreateRoom(req.Room)

	peer, err := room.AddPeer(req.UserId)
	if err != nil {
		log.Error().Err(err).Msg("Failed to create peer")
		return nil, err
	}

	peer.PeerConnection.OnTrack(func(track *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {
		log.Info().
			Str("peerId", peer.ID).
			Str("kind", track.Kind().String()).
			Str("trackId", track.ID()).
			Msg("OnTrack received")

		room.BroadcastTrack(peer.ID, track)
	})

	peer.PeerConnection.OnICEConnectionStateChange(func(state webrtc.ICEConnectionState) {
		log.Debug().
			Str("peerId", peer.ID).
			Str("state", state.String()).
			Msg("ICE connection state changed")

		if state == webrtc.ICEConnectionStateFailed ||
			state == webrtc.ICEConnectionStateDisconnected ||
			state == webrtc.ICEConnectionStateClosed {
			log.Warn().Str("peerId", peer.ID).Msg("Peer disconnected")
			room.RemovePeer(peer.ID)
		}
	})

	offer := webrtc.SessionDescription{
		Type: webrtc.SDPTypeOffer,
		SDP:  req.Sdp,
	}
	if err := peer.SetRemoteDescription(offer); err != nil {
		log.Error().Err(err).Msg("Failed to set remote description")
		room.RemovePeer(peer.ID)
		return nil, err
	}

	answerSDP, err := peer.CreateAnswer()
	if err != nil {
		log.Error().Err(err).Msg("Failed to create answer")
		room.RemovePeer(peer.ID)
		return nil, err
	}

	log.Info().
		Str("peerId", peer.ID).
		Str("room", req.Room).
		Msg("Peer created successfully")

	return &proto.CreatePeerResponse{
		PeerId:    peer.ID,
		AnswerSdp: answerSDP,
	}, nil
}

func (s *Server) AddICECandidate(ctx context.Context, req *proto.AddCandidateRequest) (*proto.Ack, error) {
	log.Debug().
		Str("peerId", req.PeerId).
		Msg("AddICECandidate request received")

	var peer *core.Peer
	stats := s.manager.GetStats()
	if rooms, ok := stats["rooms"].(map[string]interface{}); ok {
		for roomID := range rooms {
			if room, exists := s.manager.GetRoom(roomID); exists {
				if p, found := room.GetPeer(req.PeerId); found {
					peer = p
					break
				}
			}
		}
	}

	if peer == nil {
		log.Warn().Str("peerId", req.PeerId).Msg("Peer not found")
		return &proto.Ack{Ok: false, Message: "peer not found"}, nil
	}

	var candidate webrtc.ICECandidateInit
	if err := json.Unmarshal([]byte(req.Candidate), &candidate); err != nil {
		log.Error().Err(err).Msg("Failed to unmarshal candidate")
		return &proto.Ack{Ok: false, Message: "invalid candidate format"}, err
	}

	if err := peer.AddICECandidate(candidate); err != nil {
		log.Error().Err(err).Str("peerId", req.PeerId).Msg("Failed to add ICE candidate")
		return &proto.Ack{Ok: false, Message: err.Error()}, err
	}

	log.Debug().Str("peerId", req.PeerId).Msg("ICE candidate added successfully")
	return &proto.Ack{Ok: true, Message: "candidate added"}, nil
}

func (s *Server) ClosePeer(ctx context.Context, req *proto.ClosePeerRequest) (*proto.Ack, error) {
	log.Info().
		Str("peerId", req.PeerId).
		Str("reason", req.Reason).
		Msg("ClosePeer request received")

	stats := s.manager.GetStats()
	if rooms, ok := stats["rooms"].(map[string]interface{}); ok {
		for roomID := range rooms {
			if room, exists := s.manager.GetRoom(roomID); exists {
				if _, found := room.GetPeer(req.PeerId); found {
					room.RemovePeer(req.PeerId)
					log.Info().Str("peerId", req.PeerId).Msg("Peer removed successfully")
					return &proto.Ack{Ok: true, Message: "peer removed"}, nil
				}
			}
		}
	}

	log.Warn().Str("peerId", req.PeerId).Msg("Peer not found")
	return &proto.Ack{Ok: false, Message: "peer not found"}, nil
}
