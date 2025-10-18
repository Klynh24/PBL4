import React, { useEffect } from 'react';
import styles from './Toast.module.css';
import { FiCheckCircle } from 'react-icons/fi';

interface ToastProps {
  message: string;
  onClose: () => void;
}

const Toast: React.FC<ToastProps> = ({ message, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 3000); // Tự động đóng sau 3 giây

    return () => {
      clearTimeout(timer);
    };
  }, [onClose]);

  return (
    <div className={styles.toastContainer}>
      <FiCheckCircle className={styles.icon} />
      <span>{message}</span>
    </div>
  );
};

export default Toast;