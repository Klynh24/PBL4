package grpc

import (
	"context"
	"encoding/json"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"

	"tangthetoan.com/sfu/internal/core"
	"tangthetoan.com/sfu/proto"
)

// Server implement proto.IonSFUServer
type Server struct {
	proto.UnimplementedIonSFUServer
	manager *core.Manager
	stunURL string
}

func NewServer(stunURL string) *Server {
	return &Server{
		manager: core.NewManager(),
		stunURL: stunURL,
	}
};

// 1. CreatePeer: Nhận offer từ client, tạo PeerConnection, và trả về answer
func (s *Server) CreatePeer(ctx context.Context, req *proto.CreatePeerRequest) (*proto.CreatePeerResponse, error) {
	if req.Room == "__health__" {
		log.Info().Msg("Received health check ping")
		// Trả về OK ngay lập tức mà không tạo PeerConnection
		return &proto.CreatePeerResponse{PeerId: "health-ok", AnswerSdp: "health-ok"}, nil
	}
	log.Info().Str("room", req.Room).Str("user", req.UserId).Msg("CreatePeer request received")

	// Cấu hình STUN server
	config := webrtc.Configuration{
		ICEServers: []webrtc.ICEServer{
			{URLs: []string{s.stunURL}},
		},
	}

	// Tạo PeerConnection mới
	pc, err := webrtc.NewPeerConnection(config)
	if err != nil {
		log.Error().Err(err).Msg("failed to create peer connection")
		return nil, err
	}

	// Tạo Peer object để quản lý
	peer := s.manager.CreatePeer(req.Room, req.UserId, pc)

	// Xử lý khi có track từ client này
	pc.OnTrack(func(track *webrtc.TrackRemote, receiver *webrtc.RTPReceiver) {
		log.Info().Str("peerId", peer.ID).Str("kind", track.Kind().String()).Str("id", track.ID()).Msg("OnTrack received")
		// Thêm track này vào tất cả các peer khác trong phòng
		peer.Room.ForwardTrack(peer.ID, track)
	})

	// Xử lý khi kết nối ICE bị ngắt
	pc.OnICEConnectionStateChange(func(state webrtc.ICEConnectionState) {
		log.Debug().Str("peerId", peer.ID).Str("state", state.String()).Msg("ICE connection state changed")
		if state == webrtc.ICEConnectionStateFailed || state == webrtc.ICEConnectionStateDisconnected || state == webrtc.ICEConnectionStateClosed {
			log.Warn().Str("peerId", peer.ID).Msg("Peer ICE connection closed/failed")
			s.manager.RemovePeer(peer)
		}
	})

	// === XỬ LÝ NO-TRICKLE-ICE (cho chiều SFU -> Client) ===
	// Chúng ta phải đợi ICE gathering hoàn tất để gửi TẤT CẢ candidate
	// trong một SDP answer duy nhất, vì proto không có cách để SFU gửi candidate.
	iceGatheringFinished := webrtc.GatheringCompletePromise(pc)

	// Set remote offer
	offer := webrtc.SessionDescription{Type: webrtc.SDPTypeOffer, SDP: req.Sdp}
	if err := pc.SetRemoteDescription(offer); err != nil {
		log.Error().Err(err).Msg("failed to set remote description")
		s.manager.RemovePeer(peer) // Dọn dẹp nếu thất bại
		return nil, err
	}

	// Tạo answer
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

	// Đợi cho đến khi thu thập hết ICE candidate
	<-iceGatheringFinished

	// Lấy SDP cuối cùng (đã chứa tất cả candidate)
	finalAnswer := pc.LocalDescription()
	log.Info().Str("peerId", peer.ID).Msg("Peer created successfully, returning answer")

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
		return &proto.Ack{Ok: false}, nil
	}

	// Client gửi candidate dưới dạng JSON string, cần unmarshal
	var candidate webrtc.ICECandidateInit
	if err := json.Unmarshal([]byte(req.Candidate), &candidate); err != nil {
		log.Warn().Err(err).Msg("failed to unmarshal candidate string")
		return nil, err
	}

	if err := peer.PC.AddICECandidate(candidate); err != nil {
		log.Warn().Err(err).Str("peerId", req.PeerId).Msg("failed to add ice candidate")
		return nil, err
	}

	return &proto.Ack{Ok: true}, nil
}

// 3. ClosePeer: Xử lý khi client chủ động rời đi
func (s *Server) ClosePeer(ctx context.Context, req *proto.AddCandidateRequest) (*proto.Ack, error) {
	// Proto của bạn dùng AddCandidateRequest cho cả ClosePeer, nên ta lấy PeerId từ nó
	log.Info().Str("peerId", req.PeerId).Msg("ClosePeer request received")

	peer := s.manager.GetPeer(req.PeerId)
	if peer != nil {
		s.manager.RemovePeer(peer)
	}

	return &proto.Ack{Ok: true}, nil
}
