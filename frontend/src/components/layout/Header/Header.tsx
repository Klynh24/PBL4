import React, { useState, useEffect } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import styles from './Header.module.css';
import { FiBell } from 'react-icons/fi'; 
import * as api from '../../../api/apiService'; 
import { Notification } from '../../../types'; 

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isProfileOpen, setProfileOpen] = useState(false);
  const [isNotifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const response = await api.getNotifications();
        const rawData = response.data.data;
        
        /**
         * XỬ LÝ CẤU TRÚC PAGEIMPL:
         * Backend trả về đối tượng Page, mảng thực tế nằm ở trường 'content'.
         */
        const actualArray = (rawData && typeof rawData === 'object' && 'content' in rawData)
          ? (rawData as any).content
          : (Array.isArray(rawData) ? rawData : []);

        // Sử dụng .slice() an toàn sau khi đã đảm bảo là mảng
        setNotifications(actualArray.slice(0, 3)); 
      } catch (error) {
        console.error("Lỗi nạp thông báo tại Header:", api.getErrorMessage(error));
        setNotifications([]); // Gán mảng rỗng để các hàm .filter, .map không bị crash
      }
    };

    if (user) fetchNotifications();
  }, [user]);

  const unreadCount = notifications.filter(n => !n.read).length;

  const handleLogout = () => {
    navigate('/'); 
    setTimeout(() => logout(), 0);
  };
  
  return (
    <header className={styles.header}>
      <div className={styles.container}>
        <Link to="/classes" className={styles.logo}>Online learning</Link>
        <nav className={styles.nav}>
          <NavLink to="/classes" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Lớp học</NavLink>
          <NavLink to="/chat" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Chat</NavLink>
          
          {/* Menu Quản lý cho Admin */}
          {user?.role === 'admin' && (
             <NavLink to="/admin/dashboard" className={({ isActive }) => isActive ? `${styles.navLink} ${styles.active}` : styles.navLink}>Quản lý</NavLink>
          )}

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
                  <p>Thông báo mới</p>
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
                  <p className={styles.noNotif}>Không có thông báo.</p>
                )}
              </div>
            )}
          </div>
          
          <div className={styles.profileWrapper} onMouseEnter={() => setProfileOpen(true)} onMouseLeave={() => setProfileOpen(false)}>
            <div className={styles.avatar} onClick={() => navigate('/profile')}>
              {user?.name?.charAt(0).toUpperCase() || '?'}
            </div>
            {isProfileOpen && (
              <div className={styles.dropdown}>
                <div className={styles.dropdownInfo}>
                  <p className={styles.dropdownName}>{user?.name}</p>
                  <p className={styles.dropdownEmail}>{user?.email}</p>
                </div>
                <Link to="/profile" className={styles.dropdownItem}>Cá nhân</Link>
                <button onClick={handleLogout} className={`${styles.dropdownItem} ${styles.logoutButton}`}>Đăng xuất</button>
              </div>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
};

export default Header;