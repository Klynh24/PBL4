import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import styles from './MeetingPage.module.css';
import * as api from '../../api/apiService';
import Modal from '../../components/common/Modal/Modal';
import { useAuth } from '../../contexts/AuthContext';
import { User, ClassDetails } from '../../types'; 
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
    const [sidePanel, setSidePanel] = useState<'chat' | 'participants' | null>(null);
    const [isLeaveModalOpen, setLeaveModalOpen] = useState(false);

    const [messageInput, setMessageInput] = useState('');
    const [chatHistory, setChatHistory] = useState<ChatMessage[]>([
        { id: 1, sender: 'Hệ thống', text: 'Chào mừng bạn đến với cuộc họp!', time: 'Vừa xong' }
    ]);

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
        const newMsg = {
            id: Date.now(),
            sender: user?.name || 'Tôi',
            text: messageInput,
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };
        setChatHistory([...chatHistory, newMsg]);
        setMessageInput('');
    };

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
                        {/* Video của bạn */}
                        <div className={styles.participant}>
                            <div className={styles.avatar}>{user?.name.charAt(0)}</div>
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
                <button className={`${styles.controlButton} ${isSharingScreen ? styles.toggledOn : ''}`} onClick={() => setIsSharingScreen(!isSharingScreen)}>
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