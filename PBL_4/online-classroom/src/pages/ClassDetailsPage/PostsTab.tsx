import React from 'react';
import { Link, useParams, useOutletContext } from 'react-router-dom';
import { User } from '../../contexts/AuthContext';
import styles from '../ClassDetailsPage.module.css';
import { FiSend, FiVideo } from 'react-icons/fi';

// Interface để định nghĩa kiểu dữ liệu được truyền từ Route cha
interface ContextType {
  user: User | null;
  isMeetingActive: boolean;
}

const PostsTab: React.FC = () => {
    const { classId } = useParams<{ classId: string }>();
    // Lấy dữ liệu được truyền từ Outlet của ClassDetailsPage
    const { user, isMeetingActive } = useOutletContext<ContextType>();

    return (
        <div className={styles.postsContainer}>
            <div className={styles.postsList}>
                {isMeetingActive && (
                    <div className={styles.meetingPost}>
                        <div className={styles.meetingInfo}>
                            <FiVideo className={styles.meetingIcon}/>
                            <div>
                                <h4>Cuộc họp đã bắt đầu</h4>
                                <p>Tham gia ngay để không bỏ lỡ buổi học.</p>
                            </div>
                        </div>
                        <Link to={`/classes/${classId}/meet`} className={styles.joinButton}>
                            Tham gia
                        </Link>
                    </div>
                )}
                <div className={styles.postItem}>
                    <div className={styles.postAvatar}>A</div>
                    <div className={styles.postContent}>
                        <div className={styles.postHeader}><span className={styles.postAuthor}>Trần Văn An</span><span className={styles.postTime}>Hôm qua</span></div>
                        <p>Chào cả lớp, tôi đã đăng bài tập chương 1 trong tab "Bài tập". Các bạn nhớ hoàn thành trước hạn nhé.</p>
                    </div>
                </div>
                <div className={styles.postItem}>
                    <div className={styles.postAvatar}>B</div>
                    <div className={styles.postContent}>
                        <div className={styles.postHeader}><span className={styles.postAuthor}>Nguyễn Thị Bích</span><span className={styles.postTime}>Hôm qua</span></div>
                        <p>Dạ em cảm ơn thầy ạ.</p>
                    </div>
                </div>
            </div>
            <div className={styles.newPostContainer}>
                <div className={styles.newPostInputWrapper}><textarea placeholder="Bắt đầu một bài đăng mới..." rows={3} /></div>
                <div className={styles.newPostActions}><button className={`${styles.actionButton} ${styles.sendButton}`}><FiSend /> Gửi</button></div>
            </div>
        </div>
    );
};

export default PostsTab;