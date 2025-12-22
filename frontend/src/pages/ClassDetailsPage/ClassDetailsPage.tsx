import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Outlet, NavLink, useOutletContext } from 'react-router-dom';
import * as api from '../../api/apiService';
import { useAuth } from '../../contexts/AuthContext';
import { User, ClassDetails } from '../../types';
import styles from './ClassDetailsPage.module.css';
import { FiVideo, FiFileText, FiUsers, FiMessageSquare, FiFolder, FiClipboard, FiCheck } from 'react-icons/fi';

export function useClassDetailsContext() {
  return useOutletContext<{ user: User | null; classDetails: ClassDetails | null; roomId: number | null; }>();
}

const ClassDetailsPage: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [classDetails, setClassDetails] = useState<ClassDetails | null>(null);
  const [roomId, setRoomId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [isCopied, setIsCopied] = useState(false);
  const [isPreparingMeeting, setIsPreparingMeeting] = useState(false);

  useEffect(() => {
    const init = async () => {
        if (!classId || !user) return;
        try {
            setLoading(true);
            const classRes = await api.getClassDetails(classId);
            const data = classRes.data.data;
            setClassDetails(data);

            // THAY ĐỔI: Chỉ đi TÌM phòng đang có sẵn, KHÔNG tự ý tạo mới ở đây
            const roomRes = await api.getActiveRoom(classId); // Giả sử bạn đã thêm API này
            if (roomRes.data && roomRes.data.data) {
                setRoomId(roomRes.data.data.id);
            }
        } catch (err) { console.error(err); } finally { setLoading(false); }
    };
    init();
}, [classId, user]);

const handleStartOrJoinMeeting = async () => {
    if (!classId || !classDetails || !user) return;
    try {
        setIsPreparingMeeting(true);
        
        // Nếu đã có roomId từ useEffect, vào thẳng luôn
        if (roomId) {
            navigate(`/classes/${classId}/meet`);
            return;
        }

        // Chỉ tạo phòng nếu roomId chưa tồn tại (Dành cho Giáo viên)
        const roomRes = await api.createRoom({ 
            classId: parseInt(classId), 
            name: classDetails.name,
            userId: user.id
        });
        setRoomId(roomRes.data.data.id);
        navigate(`/classes/${classId}/meet`);
    } catch (err) { alert(api.getErrorMessage(err)); } finally { setIsPreparingMeeting(false); }
};

  

  const handleCopyCode = () => {
    if (classDetails?.code) {
      navigator.clipboard.writeText(classDetails.code);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    }
  };

  if (loading) return <div className={styles.message}>Đang tải...</div>;
  if (!classDetails) return <div className={styles.message}>Không tìm thấy lớp học.</div>;

  return (
    <div className={styles.pageLayout}>
      <aside className={styles.sidebar}>
        <div className={styles.classInfo}>
          <h1 className={styles.className}>{classDetails.name}</h1>
          <p className={styles.teacherName}>GV: {classDetails.user?.name || "Chưa cập nhật"}</p>
          {user?.role === 'teacher' && (
            <div className={styles.classCodeContainer}>
              <div className={styles.codeInputWrapper}>
                <input type="text" value={classDetails.code} readOnly className={styles.classCodeInput} />
                <button onClick={handleCopyCode} className={styles.copyButton}>
                  {isCopied ? <FiCheck /> : <FiClipboard />}
                </button>
              </div>
            </div>
          )}
        </div>
        
        {/* KHÔI PHỤC ĐẦY ĐỦ MENU TẠI ĐÂY */}
        <nav className={styles.sidebarNav}>
          <NavLink to="posts" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}><FiMessageSquare /> Bài đăng</NavLink>
          <NavLink to="files" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}><FiFolder /> Tài liệu</NavLink>
          <NavLink to="assignments" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}><FiFileText /> Bài tập</NavLink>
          <NavLink to="members" className={({ isActive }) => `${styles.navLink} ${isActive ? styles.active : ''}`}><FiUsers /> Thành viên</NavLink>
        </nav>
      </aside>

      <main className={styles.mainContent}>
        <header className={styles.contentHeader}>
          <span className={styles.headerText}>Kênh chung</span>
          <button onClick={handleStartOrJoinMeeting} className={styles.joinMeetingButton} disabled={isPreparingMeeting}>
            <FiVideo /> {isPreparingMeeting ? " Đang kết nối..." : (user?.role === 'teacher' ? " Bắt đầu cuộc họp" : " Tham gia họp")}
          </button>
        </header>
        <div className={styles.tabContent}>
          <Outlet context={{ user, classDetails, roomId }} />
        </div>
      </main>
    </div>
  );
};

export default ClassDetailsPage;