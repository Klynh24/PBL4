package ws

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/zerolog/log"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
	//Chỉ cho phép từ domain
	// CheckOrigin: func(r *http.Request) bool {
	// 	origin := r.Header.Get("Origin")
	// 	if origin == "" {return true}
	// 	return strigns.Contains(origin, "yourdomain.com")
	// },
}

type Handler interface {
	Handle(conn *Connection, raw []byte)
}

type Connection struct {
	Conn   *websocket.Conn
	Send   chan []byte
	UserID string
	RoomID string
	PeerID string
	Mu     sync.Mutex
	hub    *Hub
}
type Hub struct {
	Conns   map[*Connection]bool
	Mu      sync.Mutex
	Handler Handler
}

func NewHub() *Hub {
	return &Hub{
		Conns: make(map[*Connection]bool),
	}
}
func (h *Hub) SetHandler(hdl Handler) {
	h.Handler = hdl
}
func (h *Hub) Run() {
	//placeholder for future broadcast worker
	for {
		time.Sleep(5 * time.Minute)
	}
}
func (h *Hub) ServeWS(ctx context.Context, w http.ResponseWriter, r *http.Request, token, room, userId string) (*Connection, error) {
	wsConn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Error().Err(err).Msg("upgrade failed")
		return nil, err
	}
	c := &Connection{
		Conn:   wsConn,
		Send:   make(chan []byte, 256),
		UserID: userId,
		RoomID: room,
		hub:    h,
	}
	h.Mu.Lock()
	h.Conns[c] = true
	h.Mu.Unlock()

	go c.writePump()
	go c.readPump(h)
	return c, nil
}
func (c *Connection) readPump(h *Hub) {
	defer h.RemoveConnection(c)
	c.Conn.SetReadLimit(65536)
	c.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	c.Conn.SetPongHandler(func(string) error {
		c.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})
	for {
		_, message, err := c.Conn.ReadMessage()
		if err != nil {
			log.Debug().Err(err).Msg("read ws")
			break
		}
		if c.hub != nil && c.hub.Handler != nil {
			//dispatch to handler (router)
			c.hub.Handler.Handle(c, message)
		} else {
			log.Debug().Msgf("no handler registered to process message")
		}
	}
}
func (c *Connection) writePump() {
	ticker := time.NewTicker(54 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case message, ok := <-c.Send:
			if !ok {
				_ = c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				c.Conn.Close()
				return
			}
			c.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
		case <-ticker.C:
			c.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}
func (h *Hub) RemoveConnection(c *Connection) {
	h.Mu.Lock()
	defer h.Mu.Unlock()
	if _, ok := h.Conns[c]; ok {
		delete(h.Conns, c)
		close(c.Send)
	}
}
