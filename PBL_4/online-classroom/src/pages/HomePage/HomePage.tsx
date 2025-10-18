import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import styles from './HomePage.module.css';

// Import icons
import {
  FiVideo,
  FiShare2,
  FiFileText,
  FiUserPlus,
  FiLink,
  FiSend,
} from "react-icons/fi";

// Import ảnh
import LogoImg from '../../assets/media/logo.png'; 
import OnlineLearningImg from '../../assets/media/online-learning.png';

// SỬA LỖI: Import component Footer để tái sử dụng
import Footer from '../../components/layout/Footer/Footer';

const HomePage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/'); // Chuyển về trang chủ sau khi đăng xuất
  };

  return (
    <div className={styles.homeContainer}>
      {/* Các blob trang trí */}
      <div className={`${styles.blob} ${styles.blob1}`}></div>
      <div className={`${styles.blob} ${styles.blob2}`}></div>

      {/* Header */}
      <header className={styles.homeHeader}>
        <div className={styles.headerLeft}>
          <img src={LogoImg} alt="Logo" className={styles.homeLogo} />
          <h1 className={styles.homeTitle}>Online learning</h1>
        </div>
        <div className={styles.headerRight}>
          {!user ? (
            <>
              <Link to="/login" className={`${styles.btn} ${styles.btnSecondary}`}>
                Đăng nhập
              </Link>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary}`}>
                Đăng ký
              </Link>
            </>
          ) : (
            <button className={`${styles.btn} ${styles.btnSecondary}`} onClick={handleLogout}>
              Đăng xuất
            </button>
          )}
        </div>
      </header>

      {/* Hero Section */}
      <main className={styles.homeMain}>
        <div className={styles.heroContent}>
          <h2 className={styles.heroHeadline}>Lớp học trực tuyến, dễ dàng và linh hoạt</h2>
          <p className={styles.heroSubheadline}>
            Tạo lớp, gọi video, chia sẻ tài liệu và kết nối với học viên mọi lúc mọi nơi.
          </p>

          {!user ? (
            <div className={styles.heroButtons}>
              <Link to="/register" className={`${styles.btn} ${styles.btnPrimary}`}>
                Bắt đầu miễn phí
              </Link>
              <Link to="/login" className={`${styles.btn} ${styles.btnSecondary}`}>
                Tham gia ngay
              </Link>
            </div>
          ) : (
            <div className={styles.heroButtons}>
              <p>Xin chào, {user.email}!</p>
              <Link to="/classes" className={`${styles.btn} ${styles.btnPrimary}`}>
                Vào lớp học
              </Link>
            </div>
          )}
        </div>

        <div className={styles.heroImageContainer}>
          <img
            src={OnlineLearningImg}
            alt="Online Learning"
            className={styles.heroImage}
          />
        </div>
      </main>

      {/* Features Section */}
      <section className={styles.featuresSection}>
        <div className={styles.featureCard}>
          <FiVideo className={styles.featureIcon} />
          <h3>Video call trực tiếp</h3>
          <p>Kết nối giáo viên và học viên qua lớp học ảo.</p>
        </div>
        <div className={styles.featureCard}>
          <FiShare2 className={styles.featureIcon} />
          <h3>Chia sẻ màn hình & tài liệu</h3>
          <p>Trực tiếp trình bày bài giảng và gửi file dễ dàng.</p>
        </div>
        <div className={styles.featureCard}>
          <FiFileText className={styles.featureIcon} />
          <h3>Giao bài & phản hồi</h3>
          <p>Giáo viên giao bài tập và chấm điểm trực tuyến.</p>
        </div>
      </section>

      {/* How it works */}
      <section className={styles.howItWorksSection}>
        <h2 className={styles.sectionTitle}>Chỉ 3 bước đơn giản</h2>
        <div className={styles.stepsContainer}>
          <div className={styles.stepCard}>
            <div className={styles.stepIconContainer}><FiUserPlus /></div>
            <h3>1. Đăng ký</h3>
            <p>Tạo tài khoản miễn phí trong vài giây.</p>
          </div>
          <div className={styles.stepCard}>
            <div className={styles.stepIconContainer}><FiLink /></div>
            <h3>2. Tạo lớp học</h3>
            <p>Khởi tạo phòng học và chia sẻ mã lớp.</p>
          </div>
          <div className={styles.stepCard}>
            <div className={styles.stepIconContainer}><FiSend /></div>
            <h3>3. Mời học viên</h3>
            <p>Gửi mã cho học sinh để tham gia lớp học.</p>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className={styles.ctaSection}>
        <h2>Bắt đầu lớp học trực tuyến ngay hôm nay</h2>
        <Link to="/register" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnLarge}`}>
          Đăng ký miễn phí
        </Link>
      </section>

      {/* SỬA LỖI: Dùng component Footer chung */}
      <Footer />
    </div>
  );
};

export default HomePage;

