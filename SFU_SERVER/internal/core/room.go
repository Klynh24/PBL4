package core

import (
	"io"
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// TrackInfo lưu trữ một remote track và bản sao local của nó để forward
type TrackInfo struct {
	RemoteTrack *webrtc.TrackRemote
	LocalTrack  *webrtc.TrackLocalStaticRTP
}

// Room quản lý tất cả các peer trong một phòng
type Room struct {
	ID    string
	peers map[string]*Peer
	mu    sync.RWMutex

	// tracks lưu tất cả các track đang được publish trong phòng
	// key là peerId của người publish
	tracks map[string][]*TrackInfo
}

func NewRoom(id string) *Room {
	return &Room{
		ID:     id,
		peers:  make(map[string]*Peer),
		tracks: make(map[string][]*TrackInfo),
		mu:     sync.RWMutex{},
	}
}

// AddPeer thêm peer vào phòng và publish các track hiện có cho peer mới
func (r *Room) AddPeer(peer *Peer) {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.peers[peer.ID] = peer

	// Gửi tất cả các track *hiện có* trong phòng cho peer *mới* này
	for _, trackInfos := range r.tracks {
		for _, info := range trackInfos {
			if _, err := peer.AddLocalTrack(info.LocalTrack); err != nil {
				log.Error().Err(err).Str("roomId", r.ID).Str("peerId", peer.ID).Msg("failed to add existing track to new peer")
			}
		}
	}
}

// RemovePeer xóa peer khỏi phòng
func (r *Room) RemovePeer(peer *Peer) {
	r.mu.Lock()
	defer r.mu.Unlock()

	delete(r.peers, peer.ID)
}

// RemovePeerTracks dọn dẹp các track của peer rời đi
func (r *Room) RemovePeerTracks(peer *Peer) {
	r.mu.Lock()
	defer r.mu.Unlock()

	trackInfos, ok := r.tracks[peer.ID]
	if !ok {
		return // Peer này không publish track nào
	}

	// Thông báo cho tất cả các peer *khác* để xóa track này
	for _, info := range trackInfos {
		for _, otherPeer := range r.peers {
			if otherPeer.ID == peer.ID {
				continue
			}
			otherPeer.RemoveLocalTrack(info.LocalTrack)
		}
	}
	delete(r.tracks, peer.ID)
}

// ForwardTrack là logic SFU cốt lõi
// Nó nhận track từ `senderID` và phát nó đến TẤT CẢ các peer khác
func (r *Room) ForwardTrack(senderID string, remoteTrack *webrtc.TrackRemote) {
	r.mu.Lock()
	defer r.mu.Unlock()

	// 1. Tạo một "local track" mới từ "remote track"
	localTrack, err := webrtc.NewTrackLocalStaticRTP(remoteTrack.Codec().RTPCodecCapability, remoteTrack.ID(), remoteTrack.StreamID())
	if err != nil {
		log.Error().Err(err).Msg("failed to create local track")
		return
	}

	// 2. Lưu lại track này
	info := &TrackInfo{
		RemoteTrack: remoteTrack,
		LocalTrack:  localTrack,
	}
	r.tracks[senderID] = append(r.tracks[senderID], info)

	// 3. Gửi (AddTrack) track local mới này đến TẤT CẢ các peer khác (trừ người gửi)
	for _, peer := range r.peers {
		if peer.ID == senderID {
			continue
		}
		if _, err := peer.AddLocalTrack(localTrack); err != nil {
			log.Error().Err(err).Str("peerId", peer.ID).Msg("failed to add local track to peer")
		}
	}

	// 4. Bắt đầu vòng lặp copy RTP
	// Đọc RTP từ remote track và viết vào local track
	go func() {
		rtpBuf := make([]byte, 1500)
		for {
			i, _, readErr := remoteTrack.Read(rtpBuf)
			if readErr != nil {
				if readErr == io.EOF {
					return
				}
				log.Error().Err(readErr).Msg("failed to read rtp from remote track")
				return
			}
			if _, writeErr := localTrack.Write(rtpBuf[:i]); writeErr != nil && writeErr != io.ErrClosedPipe {
				log.Error().Err(writeErr).Msg("failed to write rtp to local track")
				return
			}
		}
	}()
}
