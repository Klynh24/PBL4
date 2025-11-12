package stun

import (
	"crypto/rand"
	"encoding/binary"
	"errors"
	"fmt"
)

const (
	// Message Types
	BindingRequest       uint16 = 0x0001
	BindingResponse      uint16 = 0x0101
	BindingErrorResponse uint16 = 0x0111

	// Magic Cookie (RFC 5389)
	MagicCookie uint32 = 0x2112A442

	// Header size
	HeaderSize = 20
)

// Message represents a STUN message
type Message struct {
	Type          uint16
	Length        uint16
	MagicCookie   uint32
	TransactionID [12]byte
	Attributes    []Attribute
}

// NewMessage creates a new STUN message
func NewMessage(msgType uint16) *Message {
	msg := &Message{
		Type:        msgType,
		MagicCookie: MagicCookie,
	}
	rand.Read(msg.TransactionID[:])
	return msg
}

// Parse parses a STUN message from raw bytes
func Parse(data []byte) (*Message, error) {
	if len(data) < HeaderSize {
		return nil, errors.New("message too short")
	}

	msg := &Message{}

	// Parse header
	msg.Type = binary.BigEndian.Uint16(data[0:2])
	msg.Length = binary.BigEndian.Uint16(data[2:4])
	msg.MagicCookie = binary.BigEndian.Uint32(data[4:8])
	copy(msg.TransactionID[:], data[8:20])

	// Verify magic cookie
	if msg.MagicCookie != MagicCookie {
		return nil, errors.New("invalid magic cookie")
	}

	// Verify message length
	if len(data) < HeaderSize+int(msg.Length) {
		return nil, errors.New("incomplete message")
	}

	// Parse attributes
	offset := HeaderSize
	for offset < HeaderSize+int(msg.Length) {
		if offset+4 > len(data) {
			return nil, errors.New("incomplete attribute header")
		}

		attrType := binary.BigEndian.Uint16(data[offset : offset+2])
		attrLength := binary.BigEndian.Uint16(data[offset+2 : offset+4])
		offset += 4

		// Calculate padded length (align to 4 bytes)
		paddedLength := (int(attrLength) + 3) & ^3

		if offset+paddedLength > len(data) {
			return nil, errors.New("incomplete attribute value")
		}

		attr := Attribute{
			Type:   attrType,
			Length: attrLength,
			Value:  make([]byte, attrLength),
		}
		copy(attr.Value, data[offset:offset+int(attrLength)])

		msg.Attributes = append(msg.Attributes, attr)
		offset += paddedLength
	}

	return msg, nil
}

// Serialize converts the message to bytes
func (m *Message) Serialize() ([]byte, error) {
	// Calculate total attributes length
	attrsLen := 0
	for _, attr := range m.Attributes {
		// 4 bytes header + value + padding
		paddedLength := (int(attr.Length) + 3) & ^3
		attrsLen += 4 + paddedLength
	}

	m.Length = uint16(attrsLen)
	data := make([]byte, HeaderSize+attrsLen)

	// Write header
	binary.BigEndian.PutUint16(data[0:2], m.Type)
	binary.BigEndian.PutUint16(data[2:4], m.Length)
	binary.BigEndian.PutUint32(data[4:8], m.MagicCookie)
	copy(data[8:20], m.TransactionID[:])

	// Write attributes
	offset := HeaderSize
	for _, attr := range m.Attributes {
		binary.BigEndian.PutUint16(data[offset:offset+2], attr.Type)
		binary.BigEndian.PutUint16(data[offset+2:offset+4], attr.Length)
		offset += 4

		copy(data[offset:offset+int(attr.Length)], attr.Value)

		// Add padding
		paddedLength := (int(attr.Length) + 3) & ^3
		offset += paddedLength
	}

	return data, nil
}

// AddAttribute adds an attribute to the message
func (m *Message) AddAttribute(attr Attribute) {
	m.Attributes = append(m.Attributes, attr)
}

// IsBindingRequest checks if message is a binding request
func (m *Message) IsBindingRequest() bool {
	return m.Type == BindingRequest
}

// TypeString returns a human-readable string for the message type
func (m *Message) TypeString() string {
	switch m.Type {
	case BindingRequest:
		return "Binding Request"
	case BindingResponse:
		return "Binding Response"
	case BindingErrorResponse:
		return "Binding Error Response"
	default:
		return fmt.Sprintf("Unknown (0x%04x)", m.Type)
	}
}

// String returns a string representation
func (m *Message) String() string {
	return fmt.Sprintf("STUN Message{Type: %s (0x%04x), Length: %d, TransactionID: %x, Attributes: %d}",
		m.TypeString(), m.Type, m.Length, m.TransactionID, len(m.Attributes))
}
