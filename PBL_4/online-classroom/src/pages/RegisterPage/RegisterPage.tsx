import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { RegisterData } from '../../types'; 
import * as api from '../../api/apiService';
import styles from './RegisterPage.module.css'; 

const ROLE_ID_MAP = {
    'student': 3, 
    'teacher': 2, 
    'admin': 1, 
};

const RegisterPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    
    const [name, setName] = useState('');
    const [dob, setDob] = useState('');
    const [gender, setGender] = useState('Khác');

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
        
        const userData: RegisterData = {
            email: email.trim(),
            password: password.trim(),
            name: name.trim(),
            dob: dob,
            gender: gender,
            userCatalogueId: ROLE_ID_MAP[role], 
        };

        try {
            await api.register(userData);
            alert("Đăng ký thành công! Vui lòng đăng nhập.");
            navigate('/login');
        } catch (err: any) {
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
                        {}
                        <input
                            type="text"
                            placeholder="Họ và tên"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className={styles.input}
                            required
                        />
                        {}
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.input}
                            required
                        />
                        {}
                        <input
                            type="password"
                            placeholder="Mật khẩu"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className={styles.input}
                            required
                        />
                        {}
                        <input
                            type="password"
                            placeholder="Xác nhận mật khẩu"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className={styles.input}
                            required
                        />
                         {}
                        <input
                            type="date"
                            placeholder="Ngày sinh"
                            value={dob}
                            onChange={(e) => setDob(e.target.value)}
                            className={styles.input}
                            required
                        />
                        {}
                        <select
                            name="gender"
                            value={gender}
                            onChange={(e) => setGender(e.target.value as 'Nam' | 'Nữ' | 'Khác')}
                            className={styles.input}
                            required
                        >
                            <option value="" disabled>-- Giới tính --</option>
                            <option value="Nam">Nam</option>
                            <option value="Nữ">Nữ</option>
                            <option value="Khác">Khác</option>
                        </select>
                        
                        {}
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