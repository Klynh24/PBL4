import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { RegisterData } from '../../types'; 
import * as api from '../../api/apiService';
import styles from './RegisterPage.module.css'; 

// Xác định ID cho các vai trò
const ROLE_ID_MAP = {
    // 1. CẬP NHẬT: Thêm ID cho Admin (Giả sử Admin là 1)
    'admin': 1,
    'student': 3,
    'teacher': 2,
};

// Định nghĩa lại Type để bao gồm 'admin'
type UserRole = 'student' | 'teacher' | 'admin';

const RegisterPage: React.FC = () => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [address, setAddress] = useState('');
    const [phone, setPhone] = useState('');

    // 2. CẬP NHẬT: Thay đổi State Type thành UserRole
    const [role, setRole] = useState<UserRole>('student');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');


        if (password.trim().length < 6) {
            setError('Mật khẩu phải có tối thiểu 6 ký tự.');
            return;
        }

        if (password !== confirmPassword) {
            setError('Mật khẩu xác nhận không khớp.');
            return;
        }


        const userData: RegisterData = {
            email: email.trim(),
            password: password.trim(),
            name: name.trim(),
            address: address.trim(),
            phone: phone.trim(),

            // Role ID sẽ được lấy từ map
            userCatalogues: [ROLE_ID_MAP[role]],
        };

        try {
            await api.register(userData);
            alert("Đăng ký thành công! Vui lòng đăng nhập.");
            navigate('/login');
        } catch (err: any) {
            // Hiển thị lỗi cụ thể nếu có (ví dụ: Email đã tồn tại)
            setError(api.getErrorMessage(err) || 'Đã có lỗi xảy ra. Vui lòng thử lại.');
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
                            type="text"
                            placeholder="Họ và tên"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className={styles.input}
                            required
                        />
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.input}
                            required
                        />
                         <input
                            type="text"
                            placeholder="Địa chỉ"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            className={styles.input}
                            required
                        />
                         <input
                            type="tel"
                            placeholder="Số điện thoại"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            className={styles.input}
                            required
                        />

                        <input
                            type="password"
                            placeholder="Mật khẩu (Tối thiểu 8 ký tự)"
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
                            {/* 3. CẬP NHẬT: Thêm radio button cho Admin */}
                            <label>
                                <input
                                    type="radio"
                                    name="role"
                                    value="admin"
                                    checked={role === 'admin'}
                                    onChange={() => setRole('admin')}
                                />
                                **Tôi là Admin (Tạm thời)**
                            </label>

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