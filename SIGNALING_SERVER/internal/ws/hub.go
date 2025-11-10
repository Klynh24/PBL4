package ws

import (
	"sync"
	"sync/atomic"
	"time"

	"github.com/rs/zerolog/log"
)

type Hub struct {
	rooms         sync.Map // map[string]*Room - concurrent safe, no lock needed
	clients       sync.Map // map[*Client]bool - concurrent safe
	clientCount   atomic.Int64
	Register      chan *Client
	Unregister    chan *Client
	broadcast     chan []byte
	roomBroadcast chan *RoomMessage
}

type Room struct {
	ID          string
	clients     sync.Map // map[*Client]bool - concurrent safe
	clientCount atomic.Int32
}

type RoomMessage struct {
	RoomID  string
	Message []byte
	Exclude *Client
}

func NewHub() *Hub {
	return &Hub{
		Register:      make(chan *Client, 512), // Tăng buffer
		Unregister:    make(chan *Client, 512),
		broadcast:     make(chan []byte, 2048), // Tăng buffer
		roomBroadcast: make(chan *RoomMessage, 2048),
	}
}

func (h *Hub) Run() {
	// Sử dụng worker pool để xử lý parallel
	numWorkers := 4

	for i := 0; i < numWorkers; i++ {
		go h.broadcastWorker()
	}

	for {
		select {
		case client := <-h.Register:
			h.registerClient(client)
		case client := <-h.Unregister:
			h.unregisterClient(client)
		case message := <-h.broadcast:
			h.broadcastToAll(message)
		case roomMsg := <-h.roomBroadcast:
			h.broadcastToRoomOptimized(roomMsg)
		}
	}
}

func (h *Hub) broadcastWorker() {
	// Worker để xử lý broadcast song song
	for msg := range h.roomBroadcast {
		h.broadcastToRoomOptimized(msg)
	}
}

func (h *Hub) registerClient(client *Client) {
	h.clients.Store(client, true)
	h.clientCount.Add(1)

	if client.RoomID != "" {
		h.joinRoom(client)
	}

	log.Info().
		Str("clientID", client.ID).
		Str("roomID", client.RoomID).
		Int64("totalClients", h.clientCount.Load()).
		Msg("Client registered")
}

func (h *Hub) unregisterClient(client *Client) {
	if _, loaded := h.clients.LoadAndDelete(client); loaded {
		h.clientCount.Add(-1)
		close(client.Send)
	}

	if client.RoomID != "" {
		h.leaveRoom(client)
	}

	log.Info().
		Str("clientID", client.ID).
		Str("roomID", client.RoomID).
		Msg("Client unregistered")
}

func (h *Hub) broadcastToRoomOptimized(msg *RoomMessage) {
	val, ok := h.rooms.Load(msg.RoomID)
	if !ok {
		return
	}

	room := val.(*Room)

	// Batch send để giảm lock contention
	var wg sync.WaitGroup
	room.clients.Range(func(key, value interface{}) bool {
		client := key.(*Client)
		if client != msg.Exclude {
			wg.Add(1)
			go func(c *Client) {
				defer wg.Done()
				select {
				case c.Send <- msg.Message:
				case <-time.After(100 * time.Millisecond): // Timeout
					log.Warn().Str("clientID", c.ID).Msg("Send timeout, disconnecting")
					h.Unregister <- c
				}
			}(client)
		}
		return true
	})
	wg.Wait()
}

func (h *Hub) broadcastToAll(message []byte) {
	var wg sync.WaitGroup
	h.clients.Range(func(key, value interface{}) bool {
		client := key.(*Client)
		wg.Add(1)
		go func(c *Client) {
			defer wg.Done()
			select {
			case c.Send <- message:
			case <-time.After(100 * time.Millisecond):
				h.Unregister <- c
			}
		}(client)
		return true
	})
	wg.Wait()
}

func (h *Hub) joinRoom(client *Client) {
	val, _ := h.rooms.LoadOrStore(client.RoomID, &Room{
		ID: client.RoomID,
	})
	room := val.(*Room)

	room.clients.Store(client, true)
	count := room.clientCount.Add(1)

	log.Info().
		Str("clientID", client.ID).
		Str("roomID", client.RoomID).
		Int32("roomSize", count).
		Msg("Client joined room")
}

func (h *Hub) leaveRoom(client *Client) {
	val, ok := h.rooms.Load(client.RoomID)
	if !ok {
		return
	}

	room := val.(*Room)
	room.clients.Delete(client)
	count := room.clientCount.Add(-1)

	if count == 0 {
		h.rooms.Delete(client.RoomID)
		log.Info().Str("roomID", client.RoomID).Msg("Room deleted (empty)")
	}

	log.Info().
		Str("clientID", client.ID).
		Str("roomID", client.RoomID).
		Msg("Client left room")
}

func (h *Hub) BroadcastToRoom(roomID string, message []byte, exclude *Client) {
	select {
	case h.roomBroadcast <- &RoomMessage{
		RoomID:  roomID,
		Message: message,
		Exclude: exclude,
	}:
	case <-time.After(50 * time.Millisecond):
		log.Warn().Str("roomID", roomID).Msg("Broadcast channel full, dropping message")
	}
}

func (h *Hub) GetRoomStats(roomID string) (clientCount int32, exists bool) {
	val, ok := h.rooms.Load(roomID)
	if !ok {
		return 0, false
	}

	room := val.(*Room)
	return room.clientCount.Load(), true
}
