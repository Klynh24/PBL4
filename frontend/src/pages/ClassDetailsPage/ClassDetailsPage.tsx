import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Outlet, NavLink, useOutletContext } from 'react-router-dom';
// SỬA ĐƯỜNG DẪN IMPORT: Dùng ../ thay vì ../../ nếu file nằm trong src/pages
import * as api from '../../api/apiService';
import { useAuth } from '../../contexts/AuthContext';
import { User, ClassDetails } from '../../types';
import styles from './ClassDetailsPage.module.css';
import { FiVideo, FiFileText, FiUsers, FiMessageSquare, FiFolder, FiClipboard, FiCheck } from 'react-icons/fi';

export function useClassDetailsContext() {
  return useOutletContext<{ user: User | null; isMeetingActive: boolean; classDetails: ClassDetails | null }>();
}

const ClassDetailsPage: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [classDetails, setClassDetails] = useState<ClassDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [isMeetingActive, setMeetingActive] = useState(false);

  // State hiệu ứng copy
  const [isCopied, setIsCopied] = useState(false);

  useEffect(() => {
    setLoading(true);
    if (classId) {
      api.getClassDetails(classId)
        .then(res => {
            const data: any = res.data.data ? res.data.data : res.data;
            setClassDetails(data);
            setMeetingActive(false);
        })
        .catch(err => {
            console.error(api.getErrorMessage(err));
        })
        .finally(() => setLoading(false));
    }
  }, [classId]);

  const handleStartMeeting = () => {
    setMeetingActive(true);
    navigate(`/classes/${classId}/meet`);
  };

  const handleCopyCode = () => {
    if (classDetails?.code) {
      navigator.clipboard.writeText(classDetails.code);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000); // Reset icon sau 2s
    }
  };

  if (loading) return <div className={styles.message}>Đang tải...</div>;
  if (!classDetails) return <div className={styles.message}>Không tìm thấy lớp học.</div>;

  const isTeacher = user?.role === 'teacher';

  return (
    <div className={styles.pageLayout}>
      <aside className={styles.sidebar}>
        <div className={styles.classInfo}>
            <h1 className={styles.className}>{classDetails.name}</h1>
            <p className={styles.teacherName}>
                GV: {classDetails.user ? classDetails.user.name : "Chưa cập nhật"}
            </p>

            {/* --- MÃ LỚP HỌC (Giao diện mới: Input Readonly + Nút Hồng) --- */}
            {isTeacher && (
              <div className={styles.classCodeContainer}>
                <label className={styles.classCodeLabel}>Mã lớp:</label>
                <div className={styles.codeInputWrapper}>
                    <input
                        type="text"
                        value={classDetails.code}
                        readOnly
                        className={styles.classCodeInput}
                        onClick={(e) => e.currentTarget.select()} // Click là bôi đen toàn bộ
                    />
                    <button
                      onClick={handleCopyCode}
                      className={`${styles.copyButton} ${isCopied ? styles.copied : ''}`}
                      title="Sao chép"
                    >
                      {isCopied ? <FiCheck /> : <FiClipboard />}
                    </button>
                </div>
              </div>
            )}
            {/* ----------------------------------------------------------- */}

        </div>
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
            <button onClick={handleStartMeeting} className={styles.joinMeetingButton}><FiVideo />
              {isMeetingActive ? "Vào lại cuộc họp" : (isTeacher ? "Bắt đầu cuộc họp" : "Tham gia họp")}
            </button>
        </header>
        <div className={styles.tabContent}>
            <Outlet context={{ user, isMeetingActive, classDetails }} />
        </div>
      </main>
    </div>
  );
};

export default ClassDetailsPage;