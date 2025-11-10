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

	// Rate limiting per client
	rateLimiters sync.Map // map[string]*rateLimiter
}

type Message struct {
	Type      string                 `json:"type"`
	PeerID    string                 `json:"peerId,omitempty"`
	SDP       string                 `json:"sdp,omitempty"`
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

	// Fast path: type check
	switch msg.Type {
	case "offer":
		return r.handleOffer(client, msg)
	case "answer":
		return r.handleAnswer(client, msg)
	case "candidate":
		return r.handleCandidate(client, msg)
	default:
		log.Debug().Str("type", msg.Type).Str("clientID", client.ID).Msg("Unknown message type")
	}

	return nil
}

func (r *Router) handleOffer(client *ws.Client, msg *Message) error {
	if msg.SDP == "" {
		log.Warn().Str("clientID", client.ID).Msg("Offer missing SDP")
		return nil
	}

	// Use connection pool
	sfuClient := r.sfuManager.GetClient()

	// Async call để không block
	go func() {
		resp, err := sfuClient.CreatePeer(client.RoomID, client.UserID, msg.SDP)
		if err != nil {
			log.Error().Err(err).Str("clientID", client.ID).Msg("Failed to create peer in SFU")

			r.sendError(client, "Failed to create peer")
			return
		}

		r.sendResponse(client, map[string]interface{}{
			"type":   "answer",
			"peerId": resp.PeerId,
			"sdp":    resp.AnswerSdp,
		})

		log.Info().
			Str("clientID", client.ID).
			Str("peerId", resp.PeerId).
			Str("room", client.RoomID).
			Msg("Peer created successfully")
	}()

	return nil
}

func (r *Router) handleCandidate(client *ws.Client, msg *Message) error {
	if msg.PeerID == "" {
		log.Warn().Str("clientID", client.ID).Msg("Candidate missing peerId")
		return nil
	}

	candidateJSON, err := json.Marshal(msg.Candidate)
	if err != nil {
		log.Warn().Err(err).Str("clientID", client.ID).Msg("Invalid candidate format")
		return err
	}

	sfuClient := r.sfuManager.GetClient()

	// Async
	go func() {
		_, err := sfuClient.AddICECandidate(msg.PeerID, string(candidateJSON))
		if err != nil {
			log.Error().Err(err).Str("clientID", client.ID).Str("peerId", msg.PeerID).Msg("Failed to add ICE candidate")
		}
	}()

	return nil
}

func (r *Router) handleAnswer(client *ws.Client, msg *Message) error {
	data, _ := json.Marshal(msg)
	r.hub.BroadcastToRoom(client.RoomID, data, client)

	log.Debug().Str("clientID", client.ID).Str("room", client.RoomID).Msg("Answer broadcasted to room")

	return nil
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
