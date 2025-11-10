package core

import (
	"io"
	"sync"

	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

type Room struct {
	mu     sync.RWMutex
	ID     string
	Peers  map[string]*Peer
	tracks map[string][]*TrackInfo
}

type TrackInfo struct {
	RemoteTrack *webrtc.TrackRemote
	LocalTrack  *webrtc.TrackLocalStaticRTP
	stopChan    chan struct{} // ← THÊM: Signal để stop forwarder
}

func NewRoom(id string) *Room {
	return &Room{
		ID:     id,
		Peers:  make(map[string]*Peer),
		tracks: make(map[string][]*TrackInfo),
	}
}

func (r *Room) AddPeer(peer *Peer) {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.Peers[peer.ID] = peer
	log.Info().Str("room", r.ID).Str("peerId", peer.ID).Int("totalPeers", len(r.Peers)).Msg("Peer added to room")
}

func (r *Room) RemovePeer(peerID string) {
	r.mu.Lock()
	defer r.mu.Unlock()

	delete(r.Peers, peerID)
	log.Info().Str("room", r.ID).Str("peerId", peerID).Int("remainingPeers", len(r.Peers)).Msg("Peer removed from room")
}

// ForwardTrack
func (r *Room) ForwardTrack(senderID string, remoteTrack *webrtc.TrackRemote) {
	// 1. Tạo local track TRƯỚC khi lock
	localTrack, err := webrtc.NewTrackLocalStaticRTP(
		remoteTrack.Codec().RTPCodecCapability,
		remoteTrack.ID(),
		remoteTrack.StreamID(),
	)
	if err != nil {
		log.Error().Err(err).Msg("failed to create local track")
		return
	}

	info := &TrackInfo{
		RemoteTrack: remoteTrack,
		LocalTrack:  localTrack,
		stopChan:    make(chan struct{}),
	}

	r.mu.Lock()
	r.tracks[senderID] = append(r.tracks[senderID], info)

	// Clone peers list để iterate ngoài lock
	peers := make([]*Peer, 0, len(r.Peers))
	for _, peer := range r.Peers {
		if peer.ID != senderID {
			peers = append(peers, peer)
		}
	}
	r.mu.Unlock()

	for _, peer := range peers {
		if peer.IsClosed() {
			continue
		}
		if _, err := peer.AddLocalTrack(localTrack); err != nil {
			log.Error().Err(err).Str("peerId", peer.ID).Msg("failed to add local track to peer")
		}
	}

	go r.forwardRTP(info, senderID)

	log.Info().
		Str("room", r.ID).
		Str("sender", senderID).
		Str("trackID", remoteTrack.ID()).
		Str("kind", remoteTrack.Kind().String()).
		Int("receivers", len(peers)).
		Msg("Track forwarding started")
}

func (r *Room) forwardRTP(info *TrackInfo, senderID string) {
	// Buffer pooling để giảm GC pressure
	const bufferSize = 1500
	rtpBuf := make([]byte, bufferSize)

	for {
		select {
		case <-info.stopChan:
			log.Debug().Str("senderID", senderID).Msg("RTP forwarder stopped")
			return
		default:
		}

		n, _, err := info.RemoteTrack.Read(rtpBuf)
		if err != nil {
			if err == io.EOF {
				return
			}
			log.Error().Err(err).Str("senderID", senderID).Msg("failed to read RTP")
			return
		}

		if _, err := info.LocalTrack.Write(rtpBuf[:n]); err != nil {
			if err == io.ErrClosedPipe {
				return
			}
			// ✅ Rate limit error logs để tránh spam
			log.Debug().Err(err).Msg("failed to write RTP to local track")
		}
	}
}

// RemovePeerTracks xóa tất cả tracks của một peer
func (r *Room) RemovePeerTracks(peer *Peer) {
	r.mu.Lock()
	trackInfos, exists := r.tracks[peer.ID]
	if !exists {
		r.mu.Unlock()
		return
	}

	// Clone để iterate ngoài lock
	infos := make([]*TrackInfo, len(trackInfos))
	copy(infos, trackInfos)

	otherPeers := make([]*Peer, 0, len(r.Peers))
	for _, p := range r.Peers {
		if p.ID != peer.ID {
			otherPeers = append(otherPeers, p)
		}
	}

	delete(r.tracks, peer.ID)
	r.mu.Unlock()

	for _, info := range infos {
		close(info.stopChan)
	}

	// Remove tracks từ other peers
	for _, info := range infos {
		for _, otherPeer := range otherPeers {
			otherPeer.RemoveLocalTrack(info.LocalTrack)
		}
	}
}

func (r *Room) GetPeerIDs() []string {
	r.mu.RLock()
	defer r.mu.RUnlock()

	ids := make([]string, 0, len(r.Peers))
	for id := range r.Peers {
		ids = append(ids, id)
	}
	return ids
}
