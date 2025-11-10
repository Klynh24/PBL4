package stun

import (
	"encoding/binary"
	"fmt"
	"net"
)

const (
	// Attribute Types (RFC 5389)
	AttrMappedAddress    uint16 = 0x0001
	AttrXorMappedAddress uint16 = 0x0020
	AttrSoftware         uint16 = 0x8022
	AttrFingerprint      uint16 = 0x8028
)

// Attribute represents a STUN attribute
type Attribute struct {
	Type   uint16
	Length uint16
	Value  []byte
}

// NewXorMappedAddress creates XOR-MAPPED-ADDRESS attribute
func NewXorMappedAddress(addr *net.UDPAddr, transactionID [12]byte) Attribute {
	family := uint16(0x01) // IPv4
	if addr.IP.To4() == nil {
		family = 0x02 // IPv6
	}

	var value []byte
	ip := addr.IP.To4()
	if ip != nil {
		// IPv4: 8 bytes total
		value = make([]byte, 8)
		binary.BigEndian.PutUint16(value[0:2], family)

		// XOR port with most significant 16 bits of magic cookie
		xorPort := uint16(addr.Port) ^ uint16(MagicCookie>>16)
		binary.BigEndian.PutUint16(value[2:4], xorPort)

		// XOR IP with magic cookie
		xorIP := make([]byte, 4)
		magicBytes := make([]byte, 4)
		binary.BigEndian.PutUint32(magicBytes, MagicCookie)
		for i := 0; i < 4; i++ {
			xorIP[i] = ip[i] ^ magicBytes[i]
		}
		copy(value[4:8], xorIP)
	} else {
		// IPv6: 20 bytes total
		ip = addr.IP.To16()
		value = make([]byte, 20)
		binary.BigEndian.PutUint16(value[0:2], family)

		// XOR port
		xorPort := uint16(addr.Port) ^ uint16(MagicCookie>>16)
		binary.BigEndian.PutUint16(value[2:4], xorPort)

		// XOR IP with magic cookie + transaction ID
		xorKey := make([]byte, 16)
		binary.BigEndian.PutUint32(xorKey[0:4], MagicCookie)
		copy(xorKey[4:16], transactionID[:])

		xorIP := make([]byte, 16)
		for i := 0; i < 16; i++ {
			xorIP[i] = ip[i] ^ xorKey[i]
		}
		copy(value[4:20], xorIP)
	}

	return Attribute{
		Type:   AttrXorMappedAddress,
		Length: uint16(len(value)),
		Value:  value,
	}
}

// NewSoftwareAttribute creates SOFTWARE attribute
func NewSoftwareAttribute(software string) Attribute {
	value := []byte(software)
	return Attribute{
		Type:   AttrSoftware,
		Length: uint16(len(value)),
		Value:  value,
	}
}

// String returns a string representation of the attribute
func (a *Attribute) String() string {
	return fmt.Sprintf("Attribute{Type: 0x%04x, Length: %d}", a.Type, a.Length)
}
