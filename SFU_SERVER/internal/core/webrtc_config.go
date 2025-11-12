package core

import (
	"github.com/pion/webrtc/v3"
	"github.com/rs/zerolog/log"
)

// WebRTCConfig holds WebRTC configuration
type WebRTCConfig struct {
	UDPPortMin uint16
	UDPPortMax uint16
	ICEServers []webrtc.ICEServer
}

// CreateWebRTCAPI creates optimized WebRTC API
func CreateWebRTCAPI(config WebRTCConfig) *webrtc.API {
	// Setting Engine for UDP port range and performance
	settingEngine := webrtc.SettingEngine{}

	// ✅ Configure UDP port range
	if config.UDPPortMin > 0 && config.UDPPortMax > 0 {
		log.Info().
			Uint16("min", config.UDPPortMin).
			Uint16("max", config.UDPPortMax).
			Msg("Setting UDP port range")

		settingEngine.SetEphemeralUDPPortRange(config.UDPPortMin, config.UDPPortMax)
	}

	// ✅ Performance optimizations
	// Enable network types (UDP only for better performance)
	settingEngine.SetNetworkTypes([]webrtc.NetworkType{
		webrtc.NetworkTypeUDP4,
		webrtc.NetworkTypeUDP6,
	})

	// ✅ Disable TCP candidates (UDP only = faster)
	settingEngine.SetICETimeouts(
		10*1000, // disconnectedTimeout (10s)
		25*1000, // failedTimeout (25s)
		5*1000,  // keepAliveInterval (5s)
	)

	// ✅ Lite ICE agent (less CPU usage)
	settingEngine.SetLite(false) // Keep full ICE for better NAT traversal

	// Media Engine with optimized codecs
	mediaEngine := &webrtc.MediaEngine{}

	// ✅ Register only H.264 and Opus (skip VP8/VP9 for CPU efficiency)
	if err := registerOptimizedCodecs(mediaEngine); err != nil {
		log.Error().Err(err).Msg("Failed to register codecs")
	}

	// Create API
	api := webrtc.NewAPI(
		webrtc.WithSettingEngine(settingEngine),
		webrtc.WithMediaEngine(mediaEngine),
	)

	log.Info().Msg("WebRTC API created with optimizations")
	return api
}

// registerOptimizedCodecs registers only efficient codecs
func registerOptimizedCodecs(m *webrtc.MediaEngine) error {
	// ✅ Video: H.264 (hardware accelerated on most devices)
	videoRTCPFeedback := []webrtc.RTCPFeedback{
		{Type: "goog-remb", Parameter: ""},
		{Type: "ccm", Parameter: "fir"},
		{Type: "nack", Parameter: ""},
		{Type: "nack", Parameter: "pli"},
	}

	if err := m.RegisterCodec(webrtc.RTPCodecParameters{
		RTPCodecCapability: webrtc.RTPCodecCapability{
			MimeType:     webrtc.MimeTypeH264,
			ClockRate:    90000,
			Channels:     0,
			SDPFmtpLine:  "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
			RTCPFeedback: videoRTCPFeedback,
		},
		PayloadType: 102,
	}, webrtc.RTPCodecTypeVideo); err != nil {
		return err
	}

	// ✅ Audio: Opus (efficient and high quality)
	if err := m.RegisterCodec(webrtc.RTPCodecParameters{
		RTPCodecCapability: webrtc.RTPCodecCapability{
			MimeType:  webrtc.MimeTypeOpus,
			ClockRate: 48000,
			Channels:  2,
		},
		PayloadType: 111,
	}, webrtc.RTPCodecTypeAudio); err != nil {
		return err
	}

	// ✅ Fallback: VP8 (if H.264 not supported)
	if err := m.RegisterCodec(webrtc.RTPCodecParameters{
		RTPCodecCapability: webrtc.RTPCodecCapability{
			MimeType:     webrtc.MimeTypeVP8,
			ClockRate:    90000,
			Channels:     0,
			RTCPFeedback: videoRTCPFeedback,
		},
		PayloadType: 96,
	}, webrtc.RTPCodecTypeVideo); err != nil {
		return err
	}

	return nil
}
