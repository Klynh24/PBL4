// src/pages/ForgotPasswordPage/ForgotPasswordPage.tsx

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../../api/apiService';
import { PasswordResetRequestData } from '../../types'; // Import kiểu dữ liệu
import styles from './ForgotPasswordPage.module.css';
import LogoImg from '../../assets/media/logo.png'; // Giả sử logo tồn tại

const ForgotPasswordPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [step, setStep] = useState<'request' | 'success'>('request');
    const [message, setMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleRequestReset = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage('');
        if (!email.trim()) {
            setMessage('Vui lòng nhập email.');
            return;
        }

        setIsLoading(true);

        try {
            const data: PasswordResetRequestData = { email: email.trim() };
            // 🛑 Gọi API yêu cầu đặt lại mật khẩu
            await api.requestPasswordReset(data);
            
            setMessage('Yêu cầu đã được gửi. Vui lòng kiểm tra email của bạn để nhận hướng dẫn đặt lại mật khẩu.');
            setStep('success');
            
        } catch (err) {
            const errorMessage = api.getErrorMessage(err);
            setMessage(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    if (step === 'success') {
        return (
            <div className={styles.pageWrapper}>
                <div className={styles.container}>
                    <div className={styles.formBox}>
                        <h1 className={styles.title}>Yêu cầu đã được gửi</h1>
                        <p className={styles.successMessage}>{message}</p>
                        <button onClick={() => navigate('/login')} className={styles.button}>Quay lại Đăng nhập</button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.pageWrapper}>
            <div className={styles.container}>
                <div className={styles.formBox}>
                    <img src={LogoImg} alt="Logo" className={styles.logo} />
                    <h1 className={styles.title}>Quên Mật khẩu</h1>
                    <p className={styles.subtitle}>Nhập email của bạn để nhận liên kết đặt lại mật khẩu.</p>
                    {message && <p className={styles.errorMessage}>{message}</p>}
                    
                    <form onSubmit={handleRequestReset} className={styles.form}>
                        <input
                            type="email"
                            placeholder="Email của bạn"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.input}
                            required
                            disabled={isLoading}
                        />
                        <button type="submit" className={styles.button} disabled={isLoading}>
                            {isLoading ? 'Đang xử lý...' : 'Gửi yêu cầu'}
                        </button>
                    </form>
                    <button onClick={() => navigate('/login')} className={styles.backButton}>Hủy</button>
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordPage;