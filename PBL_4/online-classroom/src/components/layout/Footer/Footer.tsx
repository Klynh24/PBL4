// src/components/layout/Footer/Footer.tsx
import React from 'react';
import styles from './Footer.module.css';

const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className={styles.footer}>
      <div className={styles.container}>
        <p className={styles.text}>
          © {currentYear} Online Learn. All rights reserved.
        </p>
        <div className={styles.links}>
          <a href="/about" className={styles.link}>Về chúng tôi</a>
          <a href="/privacy" className={styles.link}>Chính sách</a>
          <a href="/contact" className={styles.link}>Liên hệ</a>
        </div>
      </div>
    </footer>
  );
};

export default Footer;