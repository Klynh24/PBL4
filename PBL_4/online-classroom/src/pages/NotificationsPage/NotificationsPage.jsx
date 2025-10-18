// src/pages/NotificationsPage/NotificationsPage.jsx
import React, { useState, useEffect } from 'react';
import * as api from '../../api/apiService';
import styles from './NotificationsPage.module.css';

const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getNotifications()
      .then(res => setNotifications(res.data))
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const handleMarkAsRead = (id) => {
    // Gọi API để đánh dấu đã đọc
    // api.markNotificationAsRead(id);
    setNotifications(notifications.map(n => 
      n.id === id ? { ...n, read: true } : n
    ));
  };
  
  if (loading) return <div className={styles.message}>Đang tải thông báo...</div>;

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Thông báo</h1>
      <div className={styles.list}>
        {notifications.length === 0 ? (
          <p className={styles.message}>Bạn không có thông báo nào.</p>
        ) : (
          notifications.map(notif => (
            <div 
              key={notif.id} 
              className={`${styles.notificationItem} ${notif.read ? styles.read : ''}`}
              onClick={() => !notif.read && handleMarkAsRead(notif.id)}
            >
              <div className={styles.dot}></div>
              <div className={styles.content}>
                <p className={styles.messageText}>{notif.message}</p>
                <span className={styles.time}>{/* Thêm timestamp nếu có */}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;