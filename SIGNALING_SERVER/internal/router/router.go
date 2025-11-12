package router

import (
	"encoding/json"
	"sync"

	"github.com/rs/zerolog/log"

	"tangthetoan.com/signaling-server/internal/sfu"
	"tangthetoan.com/signaling-server/internal/ws"
)

type Router struct {
	hub        *ws.Hub
	sfuManager *sfu.Manager

	// Message buffer pool
	bufferPool sync.Pool

	// Peer connections tracking
	peerConnections sync.Map // map[clientID]map[remotePeerID]bool
}

type Message struct {
	Type      string                 `json:"type"`
	UserID    string                 `json:"userId,omitempty"`
	Room      string                 `json:"room,omitempty"`
	To        string                 `json:"to,omitempty"`
	From      string                 `json:"from,omitempty"`
	PeerID    string                 `json:"peerId,omitempty"`
	SDP       string                 `json:"sdp,omitempty"`
	Offer     json.RawMessage        `json:"offer,omitempty"`
	Answer    json.RawMessage        `json:"answer,omitempty"`
	Candidate interface{}            `json:"candidate,omitempty"`
	Extra     map[string]interface{} `json:"-"`
}

func NewRouter(hub *ws.Hub, sfuManager *sfu.Manager) *Router {
	return &Router{
		hub:        hub,
		sfuManager: sfuManager,
		bufferPool: sync.Pool{
			New: func() interface{} {
				return new(Message)
			},
		},
	}
}

func (r *Router) RouteMessage(client *ws.Client, message []byte) error {
	// Reuse message struct
	msg := r.bufferPool.Get().(*Message)
	defer func() {
		*msg = Message{} // Reset
		r.bufferPool.Put(msg)
	}()

	if err := json.Unmarshal(message, msg); err != nil {
		log.Warn().Err(err).Str("clientID", client.ID).Msg("Invalid JSON message")
		return err
	}

	log.Debug().
		Str("type", msg.Type).
		Str("clientID", client.ID).
		Str("userID", client.UserID).
		Msg("Routing message")

	// Fast path: type check
	switch msg.Type {
	case "ready":
		return r.handleReady(client, msg)
	case "offer":
		return r.handleOffer(client, msg)
	case "answer":
		return r.handleAnswer(client, msg)
	case "ice-candidate":
		return r.handleIceCandidate(client, msg)
	case "candidate": // Fallback
		return r.handleIceCandidate(client, msg)
	default:
		log.Debug().Str("type", msg.Type).Str("clientID", client.ID).Msg("Unknown message type")
	}

	return nil
}

// ✅ THÊM: Xử lý khi client disconnect
func (r *Router) HandleClientDisconnect(client *ws.Client) {
	log.Info().
		Str("clientID", client.ID).
		Str("userID", client.UserID).
		Str("roomID", client.RoomID).
		Msg("Client disconnecting")

	// Broadcast user-left to room
	if client.RoomID != "" {
		response := map[string]interface{}{
			"type":   "user-left",
			"userId": client.UserID,
			"from":   client.UserID,
		}

		data, _ := json.Marshal(response)
		r.hub.BroadcastToRoom(client.RoomID, data, nil)

		log.Info().
			Str("userID", client.UserID).
			Str("roomID", client.RoomID).
			Msg("Broadcasted user-left to room")
	}
}

// handleReady - Client báo sẵn sàng kết nối
func (r *Router) handleReady(client *ws.Client, msg *Message) error {
	log.Info().
		Str("clientID", client.ID).
		Str("userID", client.UserID).
		Str("roomID", client.RoomID).
		Msg("Client ready")

	// ✅ 1. Lấy danh sách người dùng hiện có trong phòng (trước khi broadcast)
	existingUsers := []string{}
	r.hub.ForEachClientInRoom(client.RoomID, func(c *ws.Client) bool {
		if c.ID != client.ID { // Không thêm chính mình
			existingUsers = append(existingUsers, c.UserID)
		}
		return true
	})

	// ✅ 2. Gửi danh sách người dùng hiện có cho người mới vào
	if len(existingUsers) > 0 {
		roomInfoResponse := map[string]interface{}{
			"type":  "room-users",
			"users": existingUsers,
		}
		roomInfoData, _ := json.Marshal(roomInfoResponse)

		select {
		case client.Send <- roomInfoData:
			log.Info().
				Str("clientID", client.ID).
				Interface("existingUsers", existingUsers).
				Int("count", len(existingUsers)).
				Msg("Sent existing users list to new client")
		default:
			log.Warn().Str("clientID", client.ID).Msg("Failed to send room users - channel full")
		}
	} else {
		log.Info().
			Str("clientID", client.ID).
			Str("roomID", client.RoomID).
			Msg("Client is first in room - no existing users to send")
	}

	// ✅ 3. Thông báo cho các peer khác về người mới vào
	response := map[string]interface{}{
		"type":   "user-joined",
		"userId": client.UserID,
		"from":   client.UserID,
	}

	data, _ := json.Marshal(response)
	r.hub.BroadcastToRoom(client.RoomID, data, client)

	log.Info().
		Str("clientID", client.ID).
		Str("roomID", client.RoomID).
		Int("existingUsersCount", len(existingUsers)).
		Msg("Broadcasted user-joined to existing room members")

	return nil
}

// handleOffer - Xử lý SDP offer từ peer
func (r *Router) handleOffer(client *ws.Client, msg *Message) error {
	// Kiểm tra có target peer không
	if msg.To != "" {
		// P2P mode: Forward offer đến peer cụ thể
		return r.forwardOfferToPeer(client, msg)
	}

	// SFU mode: Gửi offer đến SFU server
	return r.handleOfferToSFU(client, msg)
}

// forwardOfferToPeer - Forward offer đến peer khác (P2P)
func (r *Router) forwardOfferToPeer(client *ws.Client, msg *Message) error {
	// Tìm target client trong room
	targetClient := r.findClientInRoom(client.RoomID, msg.To)
	if targetClient == nil {
		log.Warn().
			Str("clientID", client.ID).
			Str("targetUserID", msg.To).
			Msg("Target peer not found in room")

		r.sendError(client, "Target peer not found")
		return nil
	}

	// Forward offer
	response := map[string]interface{}{
		"type":  "offer",
		"from":  client.UserID,
		"to":    msg.To,
		"offer": msg.Offer,
	}

	data, _ := json.Marshal(response)
	select {
	case targetClient.Send <- data:
		log.Info().
			Str("from", client.UserID).
			Str("to", msg.To).
			Msg("Forwarded offer to peer")
	default:
		log.Warn().Str("targetUserID", msg.To).Msg("Target peer send buffer full")
	}

	return nil
}

// handleOfferToSFU - Gửi offer đến SFU server
func (r *Router) handleOfferToSFU(client *ws.Client, msg *Message) error {
	sdp := msg.SDP
	if sdp == "" && msg.Offer != nil {
		// Extract SDP from offer object
		var offerObj map[string]interface{}
		if err := json.Unmarshal(msg.Offer, &offerObj); err == nil {
			if sdpStr, ok := offerObj["sdp"].(string); ok {
				sdp = sdpStr
			}
		}
	}

	if sdp == "" {
		log.Warn().Str("clientID", client.ID).Msg("Offer missing SDP")
		r.sendError(client, "Missing SDP in offer")
		return nil
	}

	// Use connection pool
	sfuClient := r.sfuManager.GetClient()

	// Async call để không block
	go func() {
		resp, err := sfuClient.CreatePeer(client.RoomID, client.UserID, sdp)
		if err != nil {
			log.Error().Err(err).Str("clientID", client.ID).Msg("Failed to create peer in SFU")
			r.sendError(client, "Failed to create peer")
			return
		}

		r.sendResponse(client, map[string]interface{}{
			"type":   "answer",
			"from":   "sfu",
			"peerId": resp.PeerId,
			"answer": map[string]interface{}{
				"type": "answer",
				"sdp":  resp.AnswerSdp,
			},
		})

		log.Info().
			Str("clientID", client.ID).
			Str("peerId", resp.PeerId).
			Str("room", client.RoomID).
			Msg("SFU peer created successfully")
	}()

	return nil
}

// handleAnswer - Xử lý SDP answer
func (r *Router) handleAnswer(client *ws.Client, msg *Message) error {
	if msg.To == "" {
		log.Warn().Str("clientID", client.ID).Msg("Answer missing 'to' field")
		return nil
	}

	// Tìm target peer
	targetClient := r.findClientInRoom(client.RoomID, msg.To)
	if targetClient == nil {
		log.Warn().
			Str("clientID", client.ID).
			Str("targetUserID", msg.To).
			Msg("Target peer not found for answer")
		return nil
	}

	// Forward answer
	response := map[string]interface{}{
		"type":   "answer",
		"from":   client.UserID,
		"to":     msg.To,
		"answer": msg.Answer,
	}

	data, _ := json.Marshal(response)
	select {
	case targetClient.Send <- data:
		log.Info().
			Str("from", client.UserID).
			Str("to", msg.To).
			Msg("Forwarded answer to peer")
	default:
		log.Warn().Str("targetUserID", msg.To).Msg("Target peer send buffer full")
	}

	return nil
}

// handleIceCandidate - Xử lý ICE candidate
func (r *Router) handleIceCandidate(client *ws.Client, msg *Message) error {
	// Nếu có target peer (P2P), forward candidate
	if msg.To != "" {
		return r.forwardCandidateToPeer(client, msg)
	}

	// Nếu có peerID (SFU), gửi đến SFU
	if msg.PeerID != "" {
		return r.sendCandidateToSFU(client, msg)
	}

	log.Warn().
		Str("clientID", client.ID).
		Msg("ICE candidate missing both 'to' and 'peerId'")

	return nil
}

// forwardCandidateToPeer - Forward ICE candidate đến peer
func (r *Router) forwardCandidateToPeer(client *ws.Client, msg *Message) error {
	targetClient := r.findClientInRoom(client.RoomID, msg.To)
	if targetClient == nil {
		log.Debug().
			Str("clientID", client.ID).
			Str("targetUserID", msg.To).
			Msg("Target peer not found for ICE candidate")
		return nil
	}

	response := map[string]interface{}{
		"type":      "ice-candidate",
		"from":      client.UserID,
		"to":        msg.To,
		"candidate": msg.Candidate,
	}

	data, _ := json.Marshal(response)
	select {
	case targetClient.Send <- data:
		log.Debug().
			Str("from", client.UserID).
			Str("to", msg.To).
			Msg("Forwarded ICE candidate to peer")
	default:
		log.Warn().Str("targetUserID", msg.To).Msg("Target peer send buffer full")
	}

	return nil
}

// sendCandidateToSFU - Gửi ICE candidate đến SFU
func (r *Router) sendCandidateToSFU(client *ws.Client, msg *Message) error {
	candidateJSON, err := json.Marshal(msg.Candidate)
	if err != nil {
		log.Warn().Err(err).Str("clientID", client.ID).Msg("Invalid candidate format")
		return err
	}

	sfuClient := r.sfuManager.GetClient()

	// Async để không block
	go func() {
		_, err := sfuClient.AddICECandidate(msg.PeerID, string(candidateJSON))
		if err != nil {
			log.Error().
				Err(err).
				Str("clientID", client.ID).
				Str("peerId", msg.PeerID).
				Msg("Failed to add ICE candidate to SFU")
		} else {
			log.Debug().
				Str("clientID", client.ID).
				Str("peerId", msg.PeerID).
				Msg("ICE candidate sent to SFU")
		}
	}()

	return nil
}

// findClientInRoom - Tìm client trong room theo userID
func (r *Router) findClientInRoom(roomID, userID string) *ws.Client {
	var targetClient *ws.Client

	r.hub.ForEachClientInRoom(roomID, func(client *ws.Client) bool {
		if client.UserID == userID {
			targetClient = client
			return false // Stop iteration
		}
		return true // Continue
	})

	return targetClient
}

func (r *Router) sendResponse(client *ws.Client, response map[string]interface{}) {
	data, _ := json.Marshal(response)
	select {
	case client.Send <- data:
	default:
		log.Warn().Str("clientID", client.ID).Msg("Client send buffer full")
	}
}

func (r *Router) sendError(client *ws.Client, errMsg string) {
	r.sendResponse(client, map[string]interface{}{
		"type":  "error",
		"error": errMsg,
	})
}
