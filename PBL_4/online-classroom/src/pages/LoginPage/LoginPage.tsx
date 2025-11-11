import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import styles from './LoginPage.module.css';
import LogoImg from '../../assets/media/logo.png';
import * as api from '../../api/apiService';

const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    
    const { login, isAuthenticated } = useAuth(); 
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
            const success = await login({ email: trimmedEmail, password: trimmedPassword });
            
            if (!success) {
                setError('Email hoặc mật khẩu không chính xác hoặc không hợp lệ.');
            }
        } catch (err: any) {
            const errorMessage = api.getErrorMessage(err) || 'Có lỗi xảy ra khi kết nối đến máy chủ.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    if (isAuthenticated) {
        return <div style={{textAlign: 'center', padding: '50px'}}>Đang chuyển hướng đến trang của bạn...</div>;
    }
    
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
                        
                        {/* Link Quên mật khẩu */}
                        <Link to="/forgot-password" className={styles.forgotPasswordLink}>
                            Quên mật khẩu?
                        </Link>
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