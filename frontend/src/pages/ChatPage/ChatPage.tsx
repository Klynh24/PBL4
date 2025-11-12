import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext'; 
import * as api from '../../api/apiService'; 
import styles from './ChatPage.module.css';
import { User } from '../../types'; 


const ChatPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true); 

  useEffect(() => {
    if (!currentUser) return; 

    api.getUsers()
      .then(res => {
        const otherUsers = res.data.data.filter((u: User) => u.id !== currentUser?.id);
        setUsers(otherUsers);
      })
      .catch(err => {
        console.error("Không thể tải danh sách người dùng:", api.getErrorMessage(err));
      })
      .finally(() => {
        setLoading(false);
      });
  }, [currentUser]); 

  if (loading) {
    return <div className={styles.loadingMessage}>Đang tải danh sách...</div>;
  }

  return (
    <div className={styles.chatContainer}>
      <div className={styles.sidebar}>
        <h2 className={styles.sidebarTitle}>Danh sách người dùng</h2>
        <ul className={styles.userList}>
          {users.map(user => (
            <li 
              key={user.id} 
              className={`${styles.userItem} ${selectedUser?.id === user.id ? styles.active : ''}`}
              onClick={() => setSelectedUser(user)}
            >
              <div className={styles.avatar}>{user.name.charAt(0).toUpperCase()}</div>
              {user.name}
            </li>
          ))}
        </ul>
      </div>
      <div className={styles.chatWindow}>
        {selectedUser ? (
          <>
            <header className={styles.chatHeader}>
              <h3>{selectedUser.name}</h3>
            </header>
            <main className={styles.messageArea}>
              {}
              <div className={`${styles.message} ${styles.received}`}>Chào bạn!</div>
              <div className={`${styles.message} ${styles.sent}`}>Chào, mình có thể giúp gì cho bạn?</div>
            </main>
            <footer className={styles.chatFooter}>
              <input type="text" placeholder="Nhập tin nhắn..." className={styles.messageInput}/>
              <button className={styles.sendButton}>Gửi</button>
            </footer>
          </>
        ) : (
          <div className={styles.noChatSelected}>
            <p>Chọn một người dùng để bắt đầu trò chuyện</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatPage;