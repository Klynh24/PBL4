import React, { useState, useEffect } from 'react';
import * as api from '../../api/apiService';
import styles from './NotificationsPage.module.css';
import { Notification } from '../../types';

const NotificationsPage: React.FC = () => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchNotifications = async () => {
      setLoading(true);
      try {
        const res = await api.getNotifications();
        const rawData = res.data.data;

        // Xử lý lấy mảng content từ PageImpl
        const actualData = (rawData && typeof rawData === 'object' && 'content' in rawData)
          ? (rawData as any).content
          : (Array.isArray(rawData) ? rawData : []);
          
        setNotifications(actualData);
      } catch (err) {
        setError(api.getErrorMessage(err));
        setNotifications([]);
      } finally {
        setLoading(false);
      }
    };
    fetchNotifications();
  }, []);

  const handleMarkAsRead = async (id: number) => {
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
    try {
      await api.markNotificationAsRead(id);
    } catch (err) {
      console.error("Lỗi đánh dấu đã đọc:", err);
    }
  };

  if (loading) return <div className={styles.message}>Đang tải...</div>;

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Thông báo</h1>
      {error && <p className={styles.error}>{error}</p>}
      <div className={styles.list}>
        {notifications.length === 0 ? (
          <p className={styles.message}>Bạn không có thông báo nào.</p>
        ) : (
          notifications.map((notif) => (
            <div 
              key={notif.id} 
              className={`${styles.notificationItem} ${notif.read ? styles.read : ''}`}
              onClick={() => !notif.read && handleMarkAsRead(notif.id)}
            >
              {!notif.read && <div className={styles.dot}></div>}
              <div className={styles.content}>
                <p>{notif.message}</p>
                <span className={styles.time}>
                  {(notif as any).createAt ? new Date((notif as any).createAt).toLocaleString('vi-VN') : ''}
                </span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;