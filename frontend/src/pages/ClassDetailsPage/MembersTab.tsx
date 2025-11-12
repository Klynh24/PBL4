import React from 'react';
import { useOutletContext } from 'react-router-dom';
import { User, ClassDetails } from '../../types'; 
import styles from './ClassDetailsPage.module.css';


interface ContextType { 
    user: User | null; 
    classDetails: ClassDetails | null; 
}

const MembersTab: React.FC = () => {
    const { classDetails } = useOutletContext<ContextType>();
    
    if (!classDetails) return <p className={styles.message}>Đang tải danh sách thành viên...</p>;

    return (
        <ul className={styles.memberList}>
            {}
            <li className={styles.memberItem}>
                <div className={styles.memberAvatar}>GV</div>
                <span className={styles.memberName}>{classDetails.teacher} (Giáo viên)</span>
            </li>
            
            {}
            {classDetails.students?.map(s => 
                <li key={s.id} className={styles.memberItem}>
                    {}
                    <div className={styles.memberAvatar}>{s.name.charAt(0).toUpperCase()}</div> 
                    <span className={styles.memberName}>{s.name}</span>
                </li>
            )}
        </ul>
    );
};

export default MembersTab;