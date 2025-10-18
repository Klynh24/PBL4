import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import styles from './LoginPage.module.css';
import LogoImg from '../../assets/media/logo.png';

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const trimmedEmail = email.trim();
    const trimmedPassword = password.trim();

    if (!trimmedEmail || !trimmedPassword) {
      setError('Vui lòng nhập đầy đủ email và mật khẩu.');
      return;
    }
    
    setIsLoading(true);
    try {
      await login({ email: trimmedEmail, password: trimmedPassword });
      navigate('/classes');
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Email hoặc mật khẩu không chính xác.';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.pageWrapper}>
        <div className={styles.container}>
            <div className={styles.formBox}>
                <Link to="/">
                  <img src={LogoImg} alt="Logo" className={styles.logo} />
                </Link>
                <h1 className={styles.title}>Đăng Nhập</h1>
                {error && <p className={styles.error}>{error}</p>}
                <form onSubmit={handleSubmit} className={styles.form}>
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className={styles.input}
                        required
                        disabled={isLoading}
                    />
                    <input
                        type="password"
                        placeholder="Mật khẩu"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className={styles.input}
                        required
                        disabled={isLoading}
                    />
                    <button type="submit" className={styles.button} disabled={isLoading}>
                        {isLoading ? 'Đang đăng nhập...' : 'Đăng Nhập'}
                    </button>
                </form>
                <p className={styles.footerText}>
                    Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
                </p>
            </div>
        </div>
    </div>
  );
};

export default LoginPage;