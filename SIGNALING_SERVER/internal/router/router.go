package router

import (
	"context"
	"encoding/json"
	"sync"

	"github.com/rs/zerolog/log"
	"tangthetoan.com/signaling-server/internal/auth"
	"tangthetoan.com/signaling-server/internal/sfu"
	"tangthetoan.com/signaling-server/internal/ws"
)

type Room struct {
	ID    string
	Peers map[string]*ws.Connection
	SFU   *sfu.Client
}

type Router struct {
	hub      *ws.Hub
	sfuMgr   *sfu.Manager
	verifier *auth.Verifier
	rooms    map[string]*Room
	peers    map[string]string
	stateMu  sync.RWMutex
}

func NewRouter(h *ws.Hub, mgr *sfu.Manager, v *auth.Verifier) *Router {
	r := &Router{
		hub:      h,
		sfuMgr:   mgr,
		verifier: v,
		rooms:    make(map[string]*Room),
		peers:    make(map[string]string),
	}
	return r
}

var _ ws.Handler = (*Router)(nil)

type Msg struct {
	Type      string `json:"type"`
	Room      string `json:"room,omitempty"`
	UserId    string `json:"userId,omitempty"`
	Sdp       string `json:"sdp,omitempty"`
	Candidate string `json:"candidate,omitempty"`
	Token     string `json:"token,omitempty"`
}

func (r *Router) Handle(conn *ws.Connection, raw []byte) {
	var m Msg
	if err := json.Unmarshal(raw, &m); err != nil {
		log.Warn().Err(err).Msg("invalid message")
		return
	}
	switch m.Type {
	case "join":
		r.handleJoin(conn, &m)
	case "offer":
		r.handleOffer(conn, &m)
	case "candidate":
		r.handleCandidate(conn, &m)
	case "leave":
		r.handleLeave(conn, &m)
	default:
		log.Warn().Msgf("unknown message type: %s", m.Type)
	}
}
func (r *Router) handleJoin(conn *ws.Connection, m *Msg) {
	// uid, err := r.verifier.Verify(m.Token)
	// if err != nil {
	// 	log.Warn().Err(err).Msg("auth failed")
	// 	r.sendError(conn, 401, "auth failes")
	// 	return
	// }
	if m.UserId == "" {
		r.sendError(conn, 400, "userId is required")
		return
	}
	uid := m.UserId
	conn.UserID = uid
	conn.RoomID = m.Room
	r.stateMu.Lock()
	room, ok := r.rooms[m.Room]
	if !ok {
		node := r.sfuMgr.SelectNode()
		if node == nil {
			r.stateMu.Unlock() // Unlock before sending error
			log.Error().Msg("no healthy sfu nodes")
			r.sendError(conn, 503, "no sfu available")
			return
		}
		room = &Room{
			ID:    m.Room,
			Peers: make(map[string]*ws.Connection),
			SFU:   node, // Assign SFU to the room
		}
		r.rooms[m.Room] = room
	}
	peers := make([]string, 0, len(room.Peers))
	for userID := range room.Peers {
		peers = append(peers, userID)
	}

	// Add new peer to room
	room.Peers[conn.UserID] = conn
	evt := map[string]interface{}{"type": "peer-joined", "userId": uid}
	payload, _ := json.Marshal(evt)
	for _, pConn := range room.Peers {
		if pConn.UserID == conn.UserID {
			continue
		}
		pConn.Send <- payload
	}
	r.stateMu.Unlock()
	resp := map[string]interface{}{
		"type":  "joined",
		"room":  m.Room,
		"peers": peers,
	}
	b, _ := json.Marshal(resp)
	conn.Send <- b
}
func (r *Router) handleOffer(conn *ws.Connection, m *Msg) {
	if conn.RoomID == "" || conn.UserID == "" {
		r.sendError(conn, 400, "not joined")
		return
	}
	r.stateMu.RLock()
	room, ok := r.rooms[conn.RoomID]
	r.stateMu.RUnlock()
	if !ok {
		r.sendError(conn, 400, "room not found")
		return
	}
	nodeClient := room.SFU
	if nodeClient == nil {
		r.sendError(conn, 503, "no sfu assigned to room")
		return
	}
	sfuAddr := nodeClient.Addr()
	peerId, answer, err := nodeClient.CreatePeer(context.Background(), conn.RoomID, conn.UserID, m.Sdp)
	if err != nil {
		r.sendError(conn, 500, "create peer failed")
		return
	}
	conn.PeerID = peerId
	r.stateMu.Lock()
	r.peers[peerId] = conn.RoomID
	r.stateMu.Unlock()
	if err := r.sfuMgr.IncrementLoad(sfuAddr); err != nil {
		log.Warn().Err(err).Msg("increment sfu load failed")
	}
	resp := map[string]interface{}{
		"type": "answer",
		"sdp":  answer,
	}
	b, _ := json.Marshal(resp)
	conn.Send <- b
}
func (r *Router) handleCandidate(conn *ws.Connection, m *Msg) {
	if conn.PeerID == "" {
		r.sendError(conn, 400, "peer not created")
		return
	}
	r.stateMu.RLock()
	roomID, ok := r.peers[conn.PeerID] // Find room from PeerID
	if !ok {
		r.stateMu.RUnlock()
		r.sendError(conn, 400, "peer not found")
		return
	}
	room := r.rooms[roomID]
	r.stateMu.RUnlock()

	if room == nil {
		r.sendError(conn, 500, "internal: room for peer not found")
		return
	}
	nodeClient := room.SFU
	err := nodeClient.AddICECandidate(context.Background(), conn.PeerID, m.Candidate)
	if err != nil {
		r.sendError(conn, 500, "add candidate failed")
		return
	}
}
func (r *Router) handleLeave(conn *ws.Connection, m *Msg) {
	r.stateMu.Lock()
	room, ok := r.rooms[conn.RoomID]
	if !ok {
		r.stateMu.Unlock()
		return // Room already gone
	}

	if conn.PeerID != "" {
		nodeClient := room.SFU // Get SFU from room
		if nodeClient != nil {
			// Replicating your original code's logic of calling AddICECandidate for leave
			_ = nodeClient.AddICECandidate(context.Background(), conn.PeerID, "")
			_ = r.sfuMgr.DecrementLoad(nodeClient.Addr())
		}
		delete(r.peers, conn.PeerID) // Delete from peer map
	}

	// Remove peer from room
	delete(room.Peers, conn.UserID)

	// CHANGED: In-memory broadcast (replaces Pub/Sub)
	evt := map[string]interface{}{
		"type":   "peer-left",
		"userId": conn.UserID,
	}
	payload, _ := json.Marshal(evt)
	for _, pConn := range room.Peers {
		pConn.Send <- payload // Tell remaining peers
	}

	// CHANGED: Clean up empty room from memory
	if len(room.Peers) == 0 {
		log.Info().Str("room", conn.RoomID).Msg("cleaned up empty room")
		delete(r.rooms, conn.RoomID)
	}

	r.stateMu.Unlock() // Unlock before sending response

	// ... (Send "left" response is the same)
	resp := map[string]interface{}{
		"type": "left",
		"room": conn.RoomID,
	}
	b, _ := json.Marshal(resp)
	conn.Send <- b
}
func (r *Router) sendError(conn *ws.Connection, code int, msg string) {
	b, _ := json.Marshal(map[string]interface{}{
		"type":    "error",
		"code":    code,
		"message": msg,
	})
	conn.Send <- b
}
