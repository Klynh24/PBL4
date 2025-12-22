import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import styles from './MeetingPage.module.css';
import * as api from '../../api/apiService';
import Modal from '../../components/common/Modal/Modal';
import { useAuth } from '../../contexts/AuthContext';
import { User, ClassDetails } from '../../types'; 
import { websocketService, RoomEventPayload } from '../../services/websocket';
import { NetworkProxyClient } from '../../network/NetworkProxyClient';
import FrameReassembler from '../../network/frame-reassembler.js';
import H264Decoder from '../../network/h264-decoder.js';
import { 
    FiMic, FiMicOff, FiVideo, FiVideoOff, FiShare, 
    FiMessageSquare, FiUsers, FiPhoneMissed, FiSend 
} from 'react-icons/fi';

interface Participant {
    id: number;
    name: string;
    isTeacher: boolean;
}

interface ChatMessage {
    id: number;
    sender: string;
    text: string;
    time: string;
}

const MeetingPage: React.FC = () => {
    const { classId } = useParams<{ classId: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();
    
    const [classDetails, setClassDetails] = useState<ClassDetails | null>(null);
    const [roomId, setRoomId] = useState<number | null>(null);
    const [meetingState, setMeetingState] = useState<'lobby' | 'in_call'>('lobby');
    const [participants, setParticipants] = useState<Participant[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    
    const [isMicMuted, setMicMuted] = useState(false);
    const [isCameraOff, setCameraOff] = useState(false);
    const [isSharingScreen, setIsSharingScreen] = useState(false);
    const [screenStream, setScreenStream] = useState<MediaStream | null>(null);
    const screenVideoRef = useRef<HTMLVideoElement | null>(null);
    const screenSendTimerRef = useRef<number | null>(null);
    const screenSendInFlightRef = useRef(false);
    const screenSendCanvasRef = useRef<HTMLCanvasElement | null>(null);

    // Network (Tier2 proxy) receiver: remote shared screen
    const [remoteRenderMode, setRemoteRenderMode] = useState<'jpeg' | 'h264' | null>(null);
    const [remoteJpegUrl, setRemoteJpegUrl] = useState<string | null>(null);
    const remoteJpegUrlRef = useRef<string | null>(null);
    const remoteCanvasRef = useRef<HTMLCanvasElement | null>(null);
    const h264DecoderRef = useRef<any>(null);
    const networkProxyRef = useRef<NetworkProxyClient | null>(null);
    const frameReassemblerRef = useRef<any>(null);
    const [sidePanel, setSidePanel] = useState<'chat' | 'participants' | null>(null);
    const [isLeaveModalOpen, setLeaveModalOpen] = useState(false);

    const [messageInput, setMessageInput] = useState('');
    const [chatHistory, setChatHistory] = useState<ChatMessage[]>([
        { id: 1, sender: 'Hệ thống', text: 'Chào mừng bạn đến với cuộc họp!', time: 'Vừa xong' }
    ]);

    const sanitizeNetworkId = (v: string) =>
        v
            .trim()
            .replace(/\s+/g, '_')
            .replace(/[^a-zA-Z0-9._\-@]/g, '_')
            .slice(0, 64);

    const getNetworkUsername = () => {
        const usernameRaw = user?.email || user?.name || (user?.id != null ? `user-${user.id}` : 'guest');
        return sanitizeNetworkId(usernameRaw);
    };

    const buildLegacyMediaPacket = (clientId: string, room: string, mediaType: number, mediaPayload: ArrayBuffer) => {
        const enc = new TextEncoder();
        const clientBytes = enc.encode(clientId);
        const roomBytes = enc.encode(room);
        const payloadBytes = new Uint8Array(mediaPayload);

        const totalLen = 4 + clientBytes.length + 4 + roomBytes.length + 1 + payloadBytes.length;
        const out = new Uint8Array(totalLen);
        const view = new DataView(out.buffer);

        let o = 0;
        view.setUint32(o, clientBytes.length, false);
        o += 4;
        out.set(clientBytes, o);
        o += clientBytes.length;

        view.setUint32(o, roomBytes.length, false);
        o += 4;
        out.set(roomBytes, o);
        o += roomBytes.length;

        out[o] = mediaType & 0xff;
        o += 1;
        out.set(payloadBytes, o);

        return out;
    };

    // Connect to Tier2 proxy (ws) to receive binary screen frames
    useEffect(() => {
        if (meetingState !== 'in_call' || !roomId) return;

        const proxyUrl = import.meta.env.VITE_NETWORK_PROXY_URL as string | undefined;
        if (!proxyUrl) {
            return;
        }

        const usernameRaw = user?.email || user?.name || (user?.id != null ? `user-${user.id}` : 'guest');
        const username = sanitizeNetworkId(usernameRaw);
        const passwordStorageKey = `networkProxyPassword:${username}`;
        let password = localStorage.getItem(passwordStorageKey);
        if (!password) {
            password = (typeof crypto !== 'undefined' && 'randomUUID' in crypto)
                ? crypto.randomUUID()
                : Math.random().toString(36).slice(2) + Date.now().toString(36);
            localStorage.setItem(passwordStorageKey, password);
        }

        const roomName = `room-${roomId}`;

        const proxy = new NetworkProxyClient();
        networkProxyRef.current = proxy;

        const ws = proxy.connect(proxyUrl, {
            onOpen: () => {
                // Try register (idempotent-ish), then login; join_room after LOGIN_SUCCESS
                proxy.sendJson({ type: 'register', username, password });
                proxy.sendJson({ type: 'login', username, password });
            },
            onText: (json: any, raw: string) => {
                const type = (json?.type || '').toString().toLowerCase();
                if (type === 'register_success') {
                    proxy.sendJson({ type: 'login', username, password });
                    return;
                }
                if (type === 'login_success') {
                    proxy.sendJson({ type: 'join_room', room: roomName });
                    return;
                }
                if (!json) {
                    console.debug('[NetworkProxy] text:', raw);
                }
            },
            onBinary: (data: ArrayBuffer) => {
                const reassembler = frameReassemblerRef.current;
                if (reassembler) {
                    reassembler.processPacket(data);
                }
            },
            onClose: () => {
                // noop
            },
            onError: () => {
                // noop
            }
        });

        const reassembler = new (FrameReassembler as any)(ws);
        frameReassemblerRef.current = reassembler;

        reassembler.setOnFrameReady((frame: any) => {
            try {
                const bytes: Uint8Array | undefined = frame?.data;
                if (!bytes || bytes.length < 2) return;

                const mediaType = bytes[0];
                // 2 = SCREEN, 1 = VOICE
                if (mediaType !== 2) return;

                const payload = bytes.subarray(1);

                // H.264 path (WebCodecs)
                if ((H264Decoder as any).isH264(payload)) {
                    if (remoteJpegUrlRef.current) {
                        URL.revokeObjectURL(remoteJpegUrlRef.current);
                        remoteJpegUrlRef.current = null;
                        setRemoteJpegUrl(null);
                    }
                    setRemoteRenderMode('h264');

                    const canvas = remoteCanvasRef.current;
                    if (!canvas) return;

                    if (!h264DecoderRef.current) {
                        // Reasonable default; can be resized later if needed
                        canvas.width = 1280;
                        canvas.height = 720;
                        h264DecoderRef.current = new (H264Decoder as any)(canvas);
                    }

                    const decoder = h264DecoderRef.current;
                    const isKeyframe = !!frame?.isKeyframe;
                    if (isKeyframe) {
                        const cfg = (H264Decoder as any).extractConfig(payload);
                        if (cfg?.description) {
                            decoder.configure(cfg);
                        }
                    }

                    const ok = decoder.decode(payload, isKeyframe);
                    if (!ok) {
                        // If decode fails, fall back to JPEG attempt
                        // eslint-disable-next-line no-console
                        console.warn('[Meeting] H264 decode failed, trying JPEG fallback');
                    } else {
                        return;
                    }
                }

                // JPEG fallback
                setRemoteRenderMode('jpeg');
                // TS strict: ensure BlobPart is a real ArrayBuffer (not ArrayBufferLike/SharedArrayBuffer)
                const jpegBuffer = new ArrayBuffer(payload.byteLength);
                new Uint8Array(jpegBuffer).set(payload);
                const blob = new Blob([jpegBuffer], { type: 'image/jpeg' });
                const url = URL.createObjectURL(blob);

                if (remoteJpegUrlRef.current) {
                    URL.revokeObjectURL(remoteJpegUrlRef.current);
                }
                remoteJpegUrlRef.current = url;
                setRemoteJpegUrl(url);
            } catch (e) {
                console.error('Render remote screen frame failed:', e);
            }
        });

        return () => {
            try {
                proxy.close();
            } catch {
                // ignore
            }

            if (h264DecoderRef.current && typeof h264DecoderRef.current.close === 'function') {
                try {
                    h264DecoderRef.current.close();
                } catch {
                    // ignore
                }
            }
            h264DecoderRef.current = null;
            frameReassemblerRef.current = null;
            networkProxyRef.current = null;

            if (remoteJpegUrlRef.current) {
                URL.revokeObjectURL(remoteJpegUrlRef.current);
                remoteJpegUrlRef.current = null;
            }
            setRemoteJpegUrl(null);
            setRemoteRenderMode(null);
        };
    }, [meetingState, roomId, user?.email, user?.name, user?.id]);

    // 2b. Realtime room chat (STOMP)
    useEffect(() => {
        if (meetingState !== 'in_call' || !roomId) return;

        try {
            websocketService.connect();
        } catch (e) {
            console.warn('WebSocket connect failed:', e);
            return;
        }

        const unsubscribe = websocketService.subscribeRoom(roomId, (evt: RoomEventPayload) => {
            if (evt.roomId !== roomId) return;

            if (evt.type === 'CHAT') {
                const senderId = evt.userId;
                const isMe = senderId && user?.id != null && senderId === String(user.id);
                const senderName = isMe ? (user?.name || 'Tôi') : (senderId ? `User ${senderId}` : 'Người dùng');
                const ts = typeof evt.timestamp === 'number' ? evt.timestamp : Date.now();
                const time = new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                setChatHistory((prev) => [
                    ...prev,
                    { id: ts, sender: senderName, text: evt.text || '', time },
                ]);
                return;
            }

            if (evt.type === 'JOIN' || evt.type === 'LEAVE') {
                const ts = typeof evt.timestamp === 'number' ? evt.timestamp : Date.now();
                const time = new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                const actionText = evt.type === 'JOIN' ? 'đã tham gia' : 'đã rời';
                const who = evt.userId ? `User ${evt.userId}` : 'Một người dùng';
                setChatHistory((prev) => [
                    ...prev,
                    { id: ts, sender: 'Hệ thống', text: `${who} ${actionText} phòng.`, time },
                ]);
            }
        });

        try {
            websocketService.joinRoom(roomId);
        } catch (e) {
            console.warn('WebSocket join room failed:', e);
        }

        return () => {
            try {
                websocketService.leaveRoom(roomId);
            } catch {
                // ignore
            }
            unsubscribe();
        };
    }, [meetingState, roomId, user?.id, user?.name]);

    // 1. Khởi tạo dữ liệu lớp và phòng
    useEffect(() => {
        const init = async () => {
            if (!classId) return;
            try {
                setIsLoading(true);
                const classRes = await api.getClassDetails(classId);
                setClassDetails(classRes.data.data);

                const roomRes = await api.getActiveRoom(classId);
                if (roomRes.data && roomRes.data.data) {
                    setRoomId(roomRes.data.data.id);
                } else {
                    alert("Không tìm thấy cuộc họp đang diễn ra.");
                    navigate(`/classes/${classId}`);
                }
            } catch (err) {
                console.error("Lỗi khởi tạo:", err);
            } finally {
                setIsLoading(false);
            }
        };
        init();
    }, [classId, navigate]);

    // 2. Cập nhật danh sách thành viên (Polling)
    const fetchParticipants = useCallback(async () => {
        if (!roomId || meetingState !== 'in_call') return;
        try {
            const res = await api.getParticipants(roomId);
            const onlineUsers: User[] = res.data.data;
            setParticipants(onlineUsers.map(u => ({
                id: u.id,
                name: u.name,
                isTeacher: u.id === classDetails?.user?.id
            })));
        } catch (err) {
            console.error("Lỗi cập nhật thành viên:", err);
        }
    }, [roomId, meetingState, classDetails]);

    useEffect(() => {
        let interval: any;
        if (meetingState === 'in_call' && roomId) {
            fetchParticipants();
            interval = setInterval(fetchParticipants, 5000);
        }
        return () => clearInterval(interval);
    }, [meetingState, roomId, fetchParticipants]);

    // 3. Xử lý sự kiện
    const handleJoinCall = async () => {
        if (!roomId) return;
        try {
            await api.joinRoom(roomId);
            setMeetingState('in_call');
        } catch (err) {
            alert("Lỗi gia nhập: " + api.getErrorMessage(err));
        }
    };

    const handleSendMessage = () => {
        if (!messageInput.trim()) return;
        if (roomId) {
            try {
                websocketService.sendRoomMessage(roomId, messageInput);
            } catch (e) {
                console.warn('Send room message failed:', e);
            }
        }
        setMessageInput('');
    };

    const stopScreenShare = () => {
        if (screenSendTimerRef.current != null) {
            window.clearInterval(screenSendTimerRef.current);
            screenSendTimerRef.current = null;
        }
        screenSendInFlightRef.current = false;

        if (screenStream) {
            screenStream.getTracks().forEach((t) => t.stop());
        }
        setScreenStream(null);
        setIsSharingScreen(false);
    };

    const handleToggleScreenShare = async () => {
        if (isSharingScreen) {
            stopScreenShare();
            return;
        }

        try {
            const stream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
            setScreenStream(stream);
            setIsSharingScreen(true);

            const track = stream.getVideoTracks()[0];
            if (track) {
                track.onended = () => {
                    stopScreenShare();
                };
            }
        } catch (e) {
            console.error('Screen share failed:', e);
            setIsSharingScreen(false);
        }
    };

    useEffect(() => {
        const videoEl = screenVideoRef.current;
        if (!videoEl) return;

        if (screenStream) {
            videoEl.srcObject = screenStream;
            videoEl.play().catch(() => {});
        } else {
            videoEl.srcObject = null;
        }
    }, [screenStream]);

    // Sender: capture local shared screen -> JPEG -> send to Tier2 proxy as legacy binary
    useEffect(() => {
        if (!isSharingScreen || !screenStream || meetingState !== 'in_call' || !roomId) {
            if (screenSendTimerRef.current != null) {
                window.clearInterval(screenSendTimerRef.current);
                screenSendTimerRef.current = null;
            }
            screenSendInFlightRef.current = false;
            return;
        }

        const proxy = networkProxyRef.current;
        const rawSocket = proxy?.getRawSocket?.();
        const proxyUrl = import.meta.env.VITE_NETWORK_PROXY_URL as string | undefined;
        if (!proxyUrl || !rawSocket) {
            // Local preview still works; remote share requires Tier2 proxy URL + connection
            return;
        }

        const clientId = getNetworkUsername();
        const roomName = `room-${roomId}`;

        const ensureCanvas = () => {
            if (!screenSendCanvasRef.current) {
                screenSendCanvasRef.current = document.createElement('canvas');
            }
            return screenSendCanvasRef.current;
        };

        const captureAndSendOnce = async () => {
            try {
                const socket = networkProxyRef.current?.getRawSocket?.();
                if (!socket || socket.readyState !== WebSocket.OPEN) return;

                if (screenSendInFlightRef.current) return;
                const videoEl = screenVideoRef.current;
                if (!videoEl || !videoEl.videoWidth || !videoEl.videoHeight) return;

                screenSendInFlightRef.current = true;

                // Scale down to reduce bandwidth
                const maxW = 1280;
                const scale = Math.min(1, maxW / videoEl.videoWidth);
                const w = Math.max(1, Math.floor(videoEl.videoWidth * scale));
                const h = Math.max(1, Math.floor(videoEl.videoHeight * scale));

                const canvas = ensureCanvas();
                if (canvas.width !== w) canvas.width = w;
                if (canvas.height !== h) canvas.height = h;

                const ctx = canvas.getContext('2d');
                if (!ctx) return;

                ctx.drawImage(videoEl, 0, 0, w, h);

                const blob: Blob | null = await new Promise((resolve) => {
                    canvas.toBlob(resolve, 'image/jpeg', 0.7);
                });
                if (!blob) return;

                const buf = await blob.arrayBuffer();
                const packet = buildLegacyMediaPacket(clientId, roomName, 2, buf);
                socket.send(packet);
            } catch (e) {
                console.warn('Send screen frame failed:', e);
            } finally {
                screenSendInFlightRef.current = false;
            }
        };

        // 10 fps sender loop
        screenSendTimerRef.current = window.setInterval(() => {
            void captureAndSendOnce();
        }, 100);

        return () => {
            if (screenSendTimerRef.current != null) {
                window.clearInterval(screenSendTimerRef.current);
                screenSendTimerRef.current = null;
            }
            screenSendInFlightRef.current = false;
        };
    }, [isSharingScreen, screenStream, meetingState, roomId]);

    const handleConfirmLeave = async () => {
        if (roomId) {
            try { await api.leaveRoom(roomId); } catch (e) { console.error(e); }
        }
        navigate(`/classes/${classId}`);
    };

    if (isLoading) return <div className={styles.lobbyContainer}><h2>Đang kết nối...</h2></div>;

    // --- Giao diện Lobby ---
    if (meetingState === 'lobby') {
        return (
            <div className={styles.lobbyContainer}>
                <div className={styles.videoPreview}>
                    <div className={styles.avatarPreview}>{user?.name.charAt(0)}</div>
                    <div className={styles.lobbyControls}>
                        <button className={`${styles.controlButton} ${isMicMuted ? styles.toggledOff : ''}`} onClick={() => setMicMuted(!isMicMuted)}>
                            {isMicMuted ? <FiMicOff /> : <FiMic />}
                        </button>
                        <button className={`${styles.controlButton} ${isCameraOff ? styles.toggledOff : ''}`} onClick={() => setCameraOff(!isCameraOff)}>
                            {isCameraOff ? <FiVideoOff /> : <FiVideo />}
                        </button>
                    </div>
                </div>
                <div className={styles.joinSection}>
                    <h2>{classDetails?.name}</h2>
                    <button className={styles.joinNowButton} onClick={handleJoinCall}>Tham gia ngay</button>
                </div>
            </div>
        );
    }

    // --- Giao diện Cuộc họp ---
    return (
        <div className={styles.meetingContainer}>
            <header className={styles.header}>
                <h3>{classDetails?.name} | Cuộc họp trực tuyến</h3>
                <div className={styles.galleryInfo}>Thành viên: {participants.length}</div>
            </header>

            <div className={`${styles.mainLayout} ${sidePanel ? styles.sidePanelOpen : ''}`}>
                <main className={styles.mainContent}>
                    <div className={styles.mainVideoArea}>
                        {/* Remote shared screen (Tier2 proxy stream) */}
                        {remoteRenderMode && (
                            <div className={styles.participant}>
                                {remoteRenderMode === 'jpeg' && remoteJpegUrl ? (
                                    <img src={remoteJpegUrl} className={styles.mediaVideo} alt="Shared screen" />
                                ) : (
                                    <canvas ref={remoteCanvasRef} className={styles.mediaVideo} />
                                )}
                                <div className={styles.nameTag}>Màn hình chia sẻ</div>
                            </div>
                        )}
                        {/* Video của bạn */}
                        <div className={styles.participant}>
                            {screenStream ? (
                                <video ref={screenVideoRef} className={styles.mediaVideo} autoPlay muted playsInline />
                            ) : (
                                <div className={styles.avatar}>{user?.name.charAt(0)}</div>
                            )}
                            <div className={styles.nameTag}>{user?.name} (Bạn)</div>
                        </div>
                        {/* Video người khác */}
                        {participants.filter(p => p.id !== user?.id).map(p => (
                            <div key={p.id} className={styles.participant}>
                                <div className={styles.avatar}>{p.name.charAt(0)}</div>
                                <div className={styles.nameTag}>{p.name} {p.isTeacher && '(GV)'}</div>
                            </div>
                        ))}
                    </div>
                </main>

                {sidePanel && (
                    <aside className={styles.sidePanel}>
                        <h4>{sidePanel === 'chat' ? 'Trò chuyện' : 'Thành viên'}</h4>
                        
                        {sidePanel === 'chat' && (
                            <div className={styles.chatContainer}>
                                <div className={styles.chatMessages}>
                                    {chatHistory.map(msg => (
                                        <div key={msg.id} className={styles.messageRow}>
                                            <small>{msg.sender} • {msg.time}</small>
                                            <div>{msg.text}</div>
                                        </div>
                                    ))}
                                </div>
                                <div className={styles.chatInputContainer}>
                                    <input 
                                        className={styles.chatInput} 
                                        placeholder="Nhập tin nhắn..." 
                                        value={messageInput}
                                        onChange={(e) => setMessageInput(e.target.value)}
                                        onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                                    />
                                    <button onClick={handleSendMessage}><FiSend size={20}/></button>
                                </div>
                            </div>
                        )}

                        {sidePanel === 'participants' && (
                            <ul className={styles.participantList}>
                                {participants.map(p => (
                                    <li key={p.id} className={styles.participantItem}>
                                        <span>{p.name} {p.isTeacher && '(GV)'}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </aside>
                )}
            </div>

            <footer className={styles.controlBar}>
                <button className={`${styles.controlButton} ${isMicMuted ? styles.toggledOff : ''}`} onClick={() => setMicMuted(!isMicMuted)}>
                    {isMicMuted ? <FiMicOff /> : <FiMic />}
                </button>
                <button className={`${styles.controlButton} ${isCameraOff ? styles.toggledOff : ''}`} onClick={() => setCameraOff(!isCameraOff)}>
                    {isCameraOff ? <FiVideoOff /> : <FiVideo />}
                </button>
                <button className={`${styles.controlButton} ${isSharingScreen ? styles.toggledOn : ''}`} onClick={handleToggleScreenShare}>
                    <FiShare />
                </button>
                <button className={styles.controlButton} onClick={() => setSidePanel(sidePanel === 'chat' ? null : 'chat')}>
                    <FiMessageSquare />
                </button>
                <button className={styles.controlButton} onClick={() => setSidePanel(sidePanel === 'participants' ? null : 'participants')}>
                    <FiUsers />
                </button>
                <button className={`${styles.controlButton} ${styles.leaveButton}`} onClick={() => setLeaveModalOpen(true)}>
                    <FiPhoneMissed />
                </button>
            </footer>

            <Modal isOpen={isLeaveModalOpen} onClose={() => setLeaveModalOpen(false)} onConfirm={handleConfirmLeave} title="Rời cuộc họp">
                <p>Bạn có chắc chắn muốn rời khỏi cuộc họp này?</p>
            </Modal>
        </div>
    );
};

export default MeetingPage;