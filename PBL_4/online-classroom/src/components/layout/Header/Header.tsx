import React, { useState, useEffect } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import styles from './Header.module.css';
import { FiBell } from 'react-icons/fi'; // Import icon chuông
import * as api from '../../../api/apiService'; // Import api service

// Định nghĩa kiểu dữ liệu cho thông báo
interface Notification {
  id: number;
  message: string;
  read: boolean;
}

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isProfileOpen, setProfileOpen] = useState(false);
  const [isNotifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);

  useEffect(() => {
    // Lấy danh sách thông báo khi component được render
    const fetchNotifications = async () => {
      try {
        const response = await api.getNotifications();
        // Chỉ lấy 3 thông báo gần nhất để hiển thị preview
        setNotifications(response.data.slice(0, 3)); 
      } catch (error) {
        console.error("Không thể tải thông báo:", error);
      }
    };
    fetchNotifications();
  }, []);

  // Đếm số thông báo chưa đọc
  const unreadCount = notifications.filter(n => !n.read).length;

  const handleLogout = () => {
    // SỬA LỖI: Chuyển hướng về trang chủ TRƯỚC
    navigate('/'); 

    // Dùng setTimeout để đảm bảo việc chuyển hướng hoàn tất
    // trước khi trạng thái logout được cập nhật, tránh race condition.
    setTimeout(() => {
      logout();
    }, 0);
  };
  
  return (
    <header className={styles.header}>
      <div className={styles.container}>
        <Link to="/classes" className={styles.logo}>
          Online learning
        </Link>
        <nav className={styles.nav}>
          <NavLink to="/classes" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Lớp học</NavLink>
          <NavLink to="/chat" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Chat</NavLink>
          {user?.role === 'admin' && (
             <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Quản lý</NavLink>
          )}

          {/* === PHẦN THÔNG BÁO === */}
          <div 
            className={styles.iconWrapper}
            onMouseEnter={() => setNotifOpen(true)}
            onMouseLeave={() => setNotifOpen(false)}
          >
            <button onClick={() => navigate('/notifications')} className={styles.iconButton}>
              <FiBell />
              {unreadCount > 0 && <span className={styles.notifBadge}>{unreadCount}</span>}
            </button>
            {isNotifOpen && (
              <div className={`${styles.dropdown} ${styles.notifDropdown}`}>
                <div className={styles.dropdownHeader}>
                  <p>Thông báo</p>
                  <Link to="/notifications">Xem tất cả</Link>
                </div>
                {notifications.length > 0 ? (
                  notifications.map(notif => (
                    <div key={notif.id} className={`${styles.notifItem} ${!notif.read ? styles.unread : ''}`}>
                      {!notif.read && <div className={styles.unreadDot}></div>}
                      <p>{notif.message}</p>
                    </div>
                  ))
                ) : (
                  <p className={styles.noNotif}>Không có thông báo mới.</p>
                )}
              </div>
            )}
          </div>
          
          {/* === PHẦN HỒ SƠ CÁ NHÂN === */}
          <div 
            className={styles.profileWrapper}
            onMouseEnter={() => setProfileOpen(true)}
            onMouseLeave={() => setProfileOpen(false)}
          >
            <div className={styles.avatar} onClick={() => navigate('/profile')}>
              {/* Thêm phương án dự phòng để không bị lỗi */}
              {user?.name?.charAt(0).toUpperCase() || '?'}
            </div>
            {isProfileOpen && (
              <div className={styles.dropdown}>
                <div className={styles.dropdownInfo}>
                  <p className={styles.dropdownName}>{user?.name}</p>
                  <p className={styles.dropdownEmail}>{user?.email}</p>
                </div>
                <Link to="/profile" className={styles.dropdownItem}>Thông tin cá nhân</Link>
                <button onClick={handleLogout} className={`${styles.dropdownItem} ${styles.logoutButton}`}>
                  Đăng xuất
                </button>
              </div>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
};

export default Header;

