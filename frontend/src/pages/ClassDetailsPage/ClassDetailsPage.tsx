import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Outlet, NavLink, useOutletContext } from 'react-router-dom';
import * as api from '../../api/apiService';
import { useAuth } from '../../contexts/AuthContext';
import { User, ClassDetails } from '../../types';
import styles from './ClassDetailsPage.module.css';
import { FiVideo, FiFileText, FiUsers, FiMessageSquare, FiFolder } from 'react-icons/fi';

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

  useEffect(() => {
    setLoading(true);
    if (classId) {
      api.getClassDetails(classId)
        .then(res => {
            // ⭐ FIX LỖI TS2345: Thêm ': any' để ép kiểu, tránh lỗi TypeScript
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

  if (loading) return <div className={styles.message}>Đang tải...</div>;
  if (!classDetails) return <div className={styles.message}>Không tìm thấy lớp học.</div>;

  return (
    <div className={styles.pageLayout}>
      <aside className={styles.sidebar}>
        <div className={styles.classInfo}>
            <h1 className={styles.className}>{classDetails.name}</h1>
            {/* Hiển thị tên giáo viên */}
            <p className={styles.teacherName}>
                GV: {classDetails.user ? classDetails.user.name : "Chưa cập nhật"}
            </p>
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
              {isMeetingActive ? "Vào lại cuộc họp" : (user?.role === 'teacher' ? "Bắt đầu cuộc họp" : "Tham gia họp")}
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