// src/pages/RegisterPage/RegisterPage.tsx
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../../api/apiService';
import styles from './RegisterPage.module.css'; // Sử dụng chung style với LoginPage
//import Footer from '../../components/layout/Footer/Footer';

const RegisterPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [role, setRole] = useState<'student' | 'teacher'>('student');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    try {
      await api.register({ email, password, role });
      // Đăng ký thành công, chuyển hướng đến trang đăng nhập
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
      console.error("Registration failed:", err);
    }
  };

  return (
    <div className={styles.pageWrapper}>
      <div className={styles.container}>
        <div className={styles.formBox}>
          <h1 className={styles.title}>Tạo tài khoản</h1>
          {error && <p className={styles.error}>{error}</p>}
          <form onSubmit={handleSubmit} className={styles.form}>
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={styles.input}
              required
            />
            <input
              type="password"
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
              required
            />
            <input
              type="password"
              placeholder="Xác nhận mật khẩu"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className={styles.input}
              required
            />
            <div className={styles.roleSelector}>
              <label>
                <input 
                  type="radio" 
                  name="role" 
                  value="student"
                  checked={role === 'student'} 
                  onChange={() => setRole('student')} 
                />
                Tôi là học viên
              </label>
              <label>
                <input 
                  type="radio" 
                  name="role" 
                  value="teacher"
                  checked={role === 'teacher'} 
                  onChange={() => setRole('teacher')} 
                />
                Tôi là giáo viên
              </label>
            </div>
            <button type="submit" className={styles.button}>
              Đăng Ký
            </button>
          </form>
          <p className={styles.footerText}>
            Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;