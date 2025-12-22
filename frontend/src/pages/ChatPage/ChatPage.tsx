import React, { useMemo, useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext'; 
import * as api from '../../api/apiService'; 
import styles from './ChatPage.module.css';
import { User } from '../../types'; 
import { websocketService, ChatMessagePayload } from '../../services/websocket';


const ChatPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true); 
  const [messageText, setMessageText] = useState('');
  const [messages, setMessages] = useState<ChatMessagePayload[]>([]);

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

  useEffect(() => {
    if (!currentUser) return;

    try {
      websocketService.connect();
    } catch (e) {
      console.error('Không thể kết nối WebSocket:', e);
      return;
    }

    const unsubscribe = websocketService.addMessageHandler((msg) => {
      setMessages((prev) => [...prev, msg]);
    });

    return () => {
      unsubscribe();
    };
  }, [currentUser]);

  const currentUserIdStr = currentUser ? String(currentUser.id) : null;

  const visibleMessages = useMemo(() => {
    if (!currentUserIdStr || !selectedUser) return [];
    const otherIdStr = String(selectedUser.id);

    return messages.filter((m) => {
      const sender = String(m.senderId);
      const recipient = String(m.recipientId);
      return (
        (sender === currentUserIdStr && recipient === otherIdStr) ||
        (sender === otherIdStr && recipient === currentUserIdStr)
      );
    });
  }, [messages, currentUserIdStr, selectedUser]);

  const handleSend = () => {
    if (!selectedUser) return;
    try {
      websocketService.sendPrivateMessage(selectedUser.id, messageText);
      setMessageText('');
    } catch (e) {
      console.error('Gửi tin nhắn thất bại:', e);
    }
  };

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
              {visibleMessages.map((m, idx) => {
                const isSent = currentUserIdStr && String(m.senderId) === currentUserIdStr;
                return (
                  <div
                    key={`${m.timestamp}-${idx}`}
                    className={`${styles.message} ${isSent ? styles.sent : styles.received}`}
                  >
                    {m.text}
                  </div>
                );
              })}
            </main>
            <footer className={styles.chatFooter}>
              <input
                type="text"
                placeholder="Nhập tin nhắn..."
                className={styles.messageInput}
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSend();
                }}
              />
              <button className={styles.sendButton} onClick={handleSend}>Gửi</button>
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