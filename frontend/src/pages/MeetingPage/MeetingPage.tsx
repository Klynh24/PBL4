import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import styles from './MeetingPage.module.css';
import * as api from '../../api/apiService';
import Modal from '../../components/common/Modal/Modal';
import { User, ClassDetails } from '../../types'; 
import { FiMic, FiMicOff, FiVideo, FiVideoOff, FiShare, FiMessageSquare, FiUsers, FiPhoneMissed, FiSend } from 'react-icons/fi';
import { FaHandPaper } from 'react-icons/fa';

interface Participant {
    id: number;
    name: string;
    isTeacher: boolean;
    handRaised: boolean;
}

const MeetingPage: React.FC = () => {
    const { classId } = useParams<{ classId: string }>();
    const navigate = useNavigate();
    
    // Lấy dữ liệu đã chuẩn bị sẵn từ ClassDetailsPage
    const { user, classDetails, roomId } = useOutletContext<{ 
        user: User | null; 
        classDetails: ClassDetails | null; 
        roomId: number | null; 
    }>();
    
    const [meetingState, setMeetingState] = useState<'lobby' | 'in_call'>('lobby');
    const [participants, setParticipants] = useState<Participant[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    
    const [isMicMuted, setMicMuted] = useState(false);
    const [isCameraOff, setCameraOff] = useState(false);
    const [pinnedParticipantId, setPinnedParticipantId] = useState<number | null>(null);
    const [sidePanel, setSidePanel] = useState<'chat' | 'participants' | null>(null);
    const [isLeaveModalOpen, setLeaveModalOpen] = useState(false);

    // 1. CẬP NHẬT THÀNH VIÊN THỰC TẾ: Gọi API mỗi 5 giây
    const fetchParticipants = useCallback(async () => {
        if (!roomId || meetingState !== 'in_call') return;
        try {
            const res = await api.getParticipants(roomId); // GET /api/v1/rooms/{roomId}/participants
            const onlineUsers: User[] = res.data.data;
            setParticipants(onlineUsers.map(u => ({
                id: u.id,
                name: u.name,
                isTeacher: u.id === classDetails?.user?.id,
                handRaised: false 
            })));
        } catch (err) { console.error("Lỗi cập nhật thành viên:", err); }
    }, [roomId, meetingState, classDetails]);

    useEffect(() => {
        let interval: any;
        if (meetingState === 'in_call' && roomId) {
            fetchParticipants();
            interval = setInterval(fetchParticipants, 5000);
        }
        return () => clearInterval(interval);
    }, [meetingState, roomId, fetchParticipants]);

    // 2. GIA NHẬP PHÒNG
    const handleJoinCall = async () => {
        if (!roomId) return;
        try {
            setIsLoading(true);
            await api.joinRoom(roomId); // POST /api/v1/rooms/{roomId}/join
            setMeetingState('in_call');
        } catch (err) {
            alert("Không thể vào phòng: " + api.getErrorMessage(err));
        } finally { setIsLoading(false); }
    };

    // 3. RỜI PHÒNG
    const handleConfirmLeave = async () => {
        if (roomId) {
            try { await api.leaveRoom(roomId); } catch (e) { console.error(e); } // POST /api/v1/rooms/{roomId}/leave
        }
        navigate(`/classes/${classId}/posts`);
    };

    const isCurrentUserHandRaised = participants.find(p => p.id === user?.id)?.handRaised || false;

    if (meetingState === 'lobby') {
        return (
            <div className={styles.lobbyContainer}>
                <div className={styles.videoPreview}>
                    <div className={styles.avatarPreview}>{user?.name.charAt(0)}</div>
                    <div className={styles.lobbyControls}>
                        <button className={`${styles.controlButton} ${isMicMuted ? styles.toggledOff : ''}`} onClick={() => setMicMuted(!isMicMuted)}>{isMicMuted ? <FiMicOff /> : <FiMic />}</button>
                        <button className={`${styles.controlButton} ${isCameraOff ? styles.toggledOff : ''}`} onClick={() => setCameraOff(!isCameraOff)}>{isCameraOff ? <FiVideoOff /> : <FiVideo />}</button>
                    </div>
                </div>
                <div className={styles.joinSection}>
                    <h2>{classDetails?.name || 'Sẵn sàng?'}</h2>
                    <button className={styles.joinNowButton} onClick={handleJoinCall} disabled={isLoading || !roomId}>
                        {isLoading ? 'Đang vào...' : 'Tham gia ngay'}
                    </button>
                </div>
            </div>
        );
    }

    const pinnedParticipant = participants.find(p => p.id === pinnedParticipantId) || participants[0];

    return (
        <div className={styles.meetingContainer}>
            <header className={styles.header}><h3>{classDetails?.name}</h3></header>
            <div className={`${styles.mainLayout} ${sidePanel ? styles.sidePanelOpen : ''}`}>
                <main className={styles.mainContent}>
                    <div className={styles.mainVideoArea}>
                        {pinnedParticipant && (
                            <div className={styles.participant} key={pinnedParticipant.id}>
                                <div className={styles.avatar}>{pinnedParticipant.name.charAt(0)}</div>
                                <span className={styles.nameTag}>{pinnedParticipant.name} {pinnedParticipant.isTeacher && '(GV)'}</span>
                            </div>
                        )}
                    </div>
                    <div className={styles.sideVideoArea}>
                        {participants.filter(p => p.id !== pinnedParticipant?.id).map(p => (
                            <div className={styles.participant} key={p.id} onClick={() => setPinnedParticipantId(p.id)}>
                                <div className={styles.avatar}>{p.name.charAt(0)}</div>
                                <span className={styles.nameTag}>{p.name}</span>
                            </div>
                        ))}
                    </div>
                </main>
                {sidePanel && (
                    <aside className={styles.sidePanel}>
                        {sidePanel === 'participants' && (<><h4>Thành viên ({participants.length})</h4><ul>{participants.map(p => <li key={p.id}>{p.name}</li>)}</ul></>)}
                        {sidePanel === 'chat' && <><h4>Trò chuyện</h4><div className={styles.chatMessages}><p>Hệ thống: Chào mừng!</p></div></>}
                    </aside>
                )}
            </div>
            <footer className={styles.controlBar}>
                <button className={styles.controlButton} onClick={() => setMicMuted(!isMicMuted)}>{isMicMuted ? <FiMicOff /> : <FiMic />}</button>
                <button className={styles.controlButton} onClick={() => setCameraOff(!isCameraOff)}>{isCameraOff ? <FiVideoOff /> : <FiVideo />}</button>
                <button className={styles.controlButton} onClick={() => setSidePanel(sidePanel === 'participants' ? null : 'participants')}><FiUsers /></button>
                <button className={styles.controlButton} onClick={() => setSidePanel(sidePanel === 'chat' ? null : 'chat')}><FiMessageSquare /></button>
                <button className={`${styles.controlButton} ${styles.leaveButton}`} onClick={() => setLeaveModalOpen(true)}><FiPhoneMissed /></button>
            </footer>
            <Modal isOpen={isLeaveModalOpen} onClose={() => setLeaveModalOpen(false)} onConfirm={handleConfirmLeave} title="Rời cuộc họp"><p>Bạn có chắc muốn rời khỏi phòng họp này?</p></Modal>
        </div>
    );
};

export default MeetingPage;