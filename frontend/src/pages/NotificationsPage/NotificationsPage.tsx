// src/pages/NotificationsPage/NotificationsPage.tsx

import React, { useState, useEffect } from 'react';
import * as api from '../../api/apiService';
import styles from './NotificationsPage.module.css';
import { Notification } from '../../types'; // <-- 1. IMPORT TYPE

const NotificationsPage: React.FC = () => { // <-- 2. SỬ DỤNG React.FC
 const [notifications, setNotifications] = useState<Notification[]>([]); // <-- 3. KHAI BÁO KIỂU STATE
 const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

 useEffect(() => {
    setError('');
  api.getNotifications()
   .then(res => {
        // 4. SỬA LỖI TRUY CẬP DATA
    setNotifications(res.data.data); 
   })
   .catch(err => {
        console.error(err);
        setError("Không thể tải thông báo.");
    })
   .finally(() => setLoading(false));
 }, []);

 const handleMarkAsRead = async (id: number) => { // <-- 5. THÊM ASYNC VÀ TYPE
  
    // Cập nhật "lạc quan" (Optimistic Update): Đổi UI ngay lập tức
  setNotifications(prevNotifs => 
   prevNotifs.map(n => 
    n.id === id ? { ...n, read: true } : n
   )
  );
    
    // 6. GỌI API ĐỂ LƯU THAY ĐỔI
    try {
        await api.markNotificationAsRead(id);
    } catch (err) {
        console.error("Lỗi khi đánh dấu đã đọc:", api.getErrorMessage(err));
        // Nếu API lỗi, hoàn tác lại thay đổi trên UI
    setNotifications(prevNotifs => 
     prevNotifs.map(n => 
      n.id === id ? { ...n, read: false } : n
     )
    );
        alert("Đã có lỗi, không thể đánh dấu đã đọc.");
    }
 };
 
 if (loading) return <div className={styles.message}>Đang tải thông báo...</div>;

 return (
  <div className={styles.container}>
   <h1 className={styles.title}>Thông báo</h1>
      {error && <p className={`${styles.message} ${styles.error}`}>{error}</p>}
   <div className={styles.list}>
        {/* Chỉ hiển thị "không có" nếu không loading VÀ không có lỗi */}
    {notifications.length === 0 && !loading && !error ? (
     <p className={styles.message}>Bạn không có thông báo nào.</p>
    ) : (
     notifications.map((notif) => ( // Type 'notif' được tự suy ra
      <div 
       key={notif.id} 
       className={`${styles.notificationItem} ${notif.read ? styles.read : ''}`}
              // Chỉ cho phép click nếu chưa đọc
       onClick={() => !notif.read && handleMarkAsRead(notif.id)}
      >
              {/* Chỉ hiển thị dấu chấm nếu chưa đọc */}
       {!notif.read && <div className={styles.dot}></div>}
       <div className={styles.content}>
        <p className={styles.messageText}>{notif.message}</p>
        <span className={styles.time}>{}</span>
       </div>
      </div>
     ))
    )}
   </div>
  </div>
 );
};

export default NotificationsPage;