package ws

import (
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/rs/zerolog/log"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 512 * 1024

	// Performance tuning
	sendBufferSize = 512 // Tăng buffer
	maxBatchSize   = 100 // Max messages per batch
	batchTimeout   = 5 * time.Millisecond
)

type Client struct {
	ID     string
	UserID string
	RoomID string

	hub  *Hub
	conn *websocket.Conn
	Send chan []byte

	// Performance tracking
	messagesSent uint64
	messagesRecv uint64
	writeMu      sync.Mutex // Protect concurrent writes
}

type MessageRouter interface {
	RouteMessage(client *Client, message []byte) error
}

var messagePool = sync.Pool{
	New: func() interface{} {
		return make([]byte, 0, 4096)
	},
}

func NewClient(id, userID, roomID string, hub *Hub, conn *websocket.Conn) *Client {
	return &Client{
		ID:     id,
		UserID: userID,
		RoomID: roomID,
		hub:    hub,
		conn:   conn,
		Send:   make(chan []byte, sendBufferSize),
	}
}

func (c *Client) ReadPump(router MessageRouter) {
	defer func() {
		c.hub.Unregister <- c
		c.conn.Close()
	}()

	c.conn.SetReadLimit(maxMessageSize)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	// Reuse buffer
	buf := messagePool.Get().([]byte)
	defer messagePool.Put(buf)

	for {
		messageType, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Error().Err(err).Str("clientID", c.ID).Msg("WebSocket error")
			}
			break
		}

		if messageType != websocket.TextMessage {
			continue
		}

		c.messagesRecv++

		// Route message asynchronously để không block read loop
		messageCopy := make([]byte, len(message))
		copy(messageCopy, message)

		go func() {
			if err := router.RouteMessage(c, messageCopy); err != nil {
				log.Error().Err(err).Str("clientID", c.ID).Msg("Failed to route message")
			}
		}()
	}
}

func (c *Client) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	batchTicker := time.NewTicker(batchTimeout)

	defer func() {
		ticker.Stop()
		batchTicker.Stop()
		c.conn.Close()
	}()

	batch := make([][]byte, 0, maxBatchSize)

	for {
		select {
		case message, ok := <-c.Send:
			if !ok {
				c.writeMessage(websocket.CloseMessage, []byte{})
				return
			}

			batch = append(batch, message)

			// Flush nếu batch đầy
			if len(batch) >= maxBatchSize {
				c.flushBatch(batch)
				batch = batch[:0]
			}

		case <-batchTicker.C:
			// Flush batch theo timeout
			if len(batch) > 0 {
				c.flushBatch(batch)
				batch = batch[:0]
			}

		case <-ticker.C:
			if err := c.writeMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (c *Client) flushBatch(batch [][]byte) {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()

	c.conn.SetWriteDeadline(time.Now().Add(writeWait))

	w, err := c.conn.NextWriter(websocket.TextMessage)
	if err != nil {
		return
	}

	for i, msg := range batch {
		if i > 0 {
			w.Write([]byte{'\n'})
		}
		w.Write(msg)
		c.messagesSent++
	}

	w.Close()
}

func (c *Client) writeMessage(messageType int, data []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()

	c.conn.SetWriteDeadline(time.Now().Add(writeWait))
	return c.conn.WriteMessage(messageType, data)
}

func (c *Client) GetStats() (sent, recv uint64) {
	return c.messagesSent, c.messagesRecv
}
