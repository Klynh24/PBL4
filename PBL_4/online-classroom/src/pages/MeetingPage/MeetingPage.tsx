import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import styles from './MeetingPage.module.css';
import * as api from '../../api/apiService';
import { useAuth } from '../../contexts/AuthContext';
import Modal from '../../components/common/Modal/Modal';
import {
  FiMic, FiMicOff, FiVideo, FiVideoOff, FiShare, FiMessageSquare,
  FiUsers, FiPhoneMissed, FiClipboard, FiSend
} from 'react-icons/fi';
import { FaHandPaper } from 'react-icons/fa';

// Định nghĩa kiểu dữ liệu cho người tham gia
interface Participant {
    id: number;
    name: string;
    isTeacher: boolean;
    handRaised: boolean;
}

const MeetingPage: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [meetingState, setMeetingState] = useState<'lobby' | 'in_call'>('lobby');
  const [classDetails, setClassDetails] = useState<any>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  const [isMicMuted, setMicMuted] = useState(false);
  const [isCameraOff, setCameraOff] = useState(false);
  const [pinnedParticipantId, setPinnedParticipantId] = useState<number | null>(null);
  const [sidePanel, setSidePanel] = useState<'chat' | 'participants' | null>(null);
  const [isLeaveModalOpen, setLeaveModalOpen] = useState(false);

  useEffect(() => {
    if (classId) {
      setIsLoading(true);
      api.getClassDetails(classId)
        .then(res => {
          const details = res.data;
          setClassDetails(details);
          const teacher: Participant = { id: details.teacherId || Date.now(), name: details.teacher, isTeacher: true, handRaised: false };
          const students: Participant[] = (details.students || []).map((s: any) => ({ ...s, isTeacher: false, handRaised: false }));
          setParticipants([teacher, ...students]);
          setPinnedParticipantId(teacher.id);
        })
        .finally(() => setIsLoading(false));
    }
  }, [classId]);

  const handleToggleHandRaise = () => {
      setParticipants(prev => 
        prev.map(p => 
            p.id === user?.id ? { ...p, handRaised: !p.handRaised } : p
        )
      );
  };

  const isCurrentUserHandRaised = participants.find(p => p.id === user?.id)?.handRaised || false;
  const handleJoinCall = () => setMeetingState('in_call');
  const handlePinParticipant = (id: number) => setPinnedParticipantId(prevId => prevId === id ? null : id);

  if (meetingState === 'lobby') {
    return (
        <div className={styles.lobbyContainer}>
            <div className={styles.videoPreview}>
                {isLoading ? <p>Đang tải...</p> : <div className={styles.avatarPreview}>{user?.name.charAt(0)}</div>}
                <div className={styles.lobbyControls}><button className={`${styles.controlButton} ${isMicMuted ? styles.toggledOff : ''}`} onClick={() => setMicMuted(!isMicMuted)}>{isMicMuted ? <FiMicOff /> : <FiMic />}</button><button className={`${styles.controlButton} ${isCameraOff ? styles.toggledOff : ''}`} onClick={() => setCameraOff(!isCameraOff)}>{isCameraOff ? <FiVideoOff /> : <FiVideo />}</button></div>
            </div>
            <div className={styles.joinSection}>
                <h2>{classDetails?.name || 'Sẵn sàng tham gia?'}</h2>
                <button className={styles.joinNowButton} onClick={handleJoinCall} disabled={isLoading}>{isLoading ? 'Đang tải...' : 'Tham gia ngay'}</button>
            </div>
        </div>
    );
  }

  const pinnedParticipant = participants.find(p => p.id === pinnedParticipantId);
  const otherParticipants = participants.filter(p => p.id !== pinnedParticipantId);

  return (
    <>
      <div className={styles.meetingContainer}>
        <header className={styles.header}><h3>{classDetails?.name || 'Đang tải...'}</h3></header>
        
        {/* SỬA LỖI: Cấu trúc lại layout chính */}
        <div className={`${styles.mainLayout} ${sidePanel ? styles.sidePanelOpen : ''}`}>
            <main className={`${styles.mainContent} ${pinnedParticipantId ? styles.pinnedLayout : ''}`}>
                <div className={styles.mainVideoArea}>
                    {pinnedParticipant ? (
                        <div className={`${styles.participant} ${pinnedParticipant.handRaised ? styles.handRaised : ''}`} key={pinnedParticipant.id}>
                            {pinnedParticipant.handRaised && <FaHandPaper className={styles.handRaisedIcon} />}
                            <div className={styles.avatar}>{pinnedParticipant.name.charAt(0)}</div>
                            <span className={styles.nameTag}>{pinnedParticipant.name} {pinnedParticipant.isTeacher && '(Giáo viên)'}</span>
                        </div>
                    ) : <div className={styles.galleryInfo}>Chế độ xem thư viện</div>}
                </div>
                
                <div className={styles.sideVideoArea}>
                    {(pinnedParticipantId ? otherParticipants : participants).map((p) => (
                        <div className={`${styles.participant} ${p.handRaised ? styles.handRaised : ''}`} key={p.id} onClick={() => handlePinParticipant(p.id)}>
                            {p.handRaised && <FaHandPaper className={styles.handRaisedIcon} />}
                            <div className={styles.avatar}>{p.name.charAt(0)}</div>
                            <span className={styles.nameTag}>{p.name} {p.id === user?.id && '(Bạn)'}</span>
                        </div>
                    ))}
                </div>
            </main>

            {sidePanel && (
                <aside className={styles.sidePanel}>
                    {sidePanel === 'participants' && (<><h4>Thành viên ({participants.length})</h4><ul className={styles.participantList}>{participants.map(p => <li key={p.id} className={styles.participantItem}>{p.name} {p.isTeacher && '(GV)'} {p.handRaised && <FaHandPaper />}</li>)}</ul></>)}
                    {sidePanel === 'chat' && (<><h4>Trò chuyện</h4><div className={styles.chatMessages}><p><strong>GV:</strong> Chào cả lớp!</p></div><div className={styles.chatInputContainer}><input type="text" placeholder="Gửi tin nhắn..." className={styles.chatInput} /><button><FiSend/></button></div></>)}
                </aside>
            )}
        </div>

        <footer className={styles.controlBar}>
          <button className={`${styles.controlButton} ${isMicMuted ? styles.toggledOff : ''}`} onClick={() => setMicMuted(!isMicMuted)}>{isMicMuted ? <FiMicOff /> : <FiMic />}</button>
          <button className={`${styles.controlButton} ${isCameraOff ? styles.toggledOff : ''}`} onClick={() => setCameraOff(!isCameraOff)}>{isCameraOff ? <FiVideoOff /> : <FiVideo />}</button>
          <button className={`${styles.controlButton} ${isCurrentUserHandRaised ? styles.toggledOn : ''}`} onClick={handleToggleHandRaise}><FaHandPaper /></button>
          <button className={styles.controlButton}><FiShare /></button>
          <button className={`${styles.controlButton} ${sidePanel === 'participants' ? styles.toggledOn : ''}`} onClick={() => setSidePanel(sidePanel === 'participants' ? null : 'participants')}><FiUsers /></button>
          <button className={`${styles.controlButton} ${sidePanel === 'chat' ? styles.toggledOn : ''}`} onClick={() => setSidePanel(sidePanel === 'chat' ? null : 'chat')}><FiMessageSquare /></button>
          <button className={`${styles.controlButton} ${styles.leaveButton}`} onClick={() => setLeaveModalOpen(true)}><FiPhoneMissed /></button>
        </footer>
      </div>

      <Modal isOpen={isLeaveModalOpen} onClose={() => setLeaveModalOpen(false)} onConfirm={() => navigate(`/classes/${classId}`)} title="Rời khỏi buổi học">
        <p>Bạn có chắc chắn muốn kết thúc và rời khỏi buổi học này không?</p>
      </Modal>
    </>
  );
};

export default MeetingPage;

