import React, { useState, useEffect } from 'react';
import { Link, useParams, useOutletContext } from 'react-router-dom';
import { User, Post } from '../../types'; 
import * as api from '../../api/apiService'; 
import styles from './ClassDetailsPage.module.css';
import { FiSend, FiVideo } from 'react-icons/fi';


interface ContextType {
    user: User | null;
    isMeetingActive: boolean;
}

const PostsTab: React.FC = () => {
    const { classId } = useParams<{ classId: string }>();
    const { user, isMeetingActive } = useOutletContext<ContextType>();

    const [posts, setPosts] = useState<Post[]>([]);
    const [newPostContent, setNewPostContent] = useState('');
    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false); 

    const fetchPosts = async () => {
        if (!classId) return;
        setLoading(true);
        try {
            const response = await api.getPosts(classId); 
            setPosts(response.data.data.sort((a: Post, b: Post) => b.id - a.id)); 
        } catch (error) {
            console.error("Không thể tải bài đăng:", api.getErrorMessage(error));
        } finally {
            setLoading(false);
        }
    };
    
    useEffect(() => {
        fetchPosts();
    }, [classId]);

    const handleNewPost = async () => {
        if (!newPostContent.trim() || !user || !classId || sending) return;

        setSending(true);
        const contentToSend = newPostContent.trim();
        setNewPostContent(''); 
        const tempId = Date.now();
        const tempPost: Post = {
            id: tempId,
            authorName: user.name || user.email,
            content: contentToSend,
            timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
            authorRole: user.role || 'student'
            };
        
        setPosts(prev => [tempPost, ...prev]);

        try {
            const response = await api.createPost(classId, { content: contentToSend });
            
            const finalPost = response.data.data;
            setPosts(prev => prev.map(p => p.id === tempId ? finalPost : p));

        } catch (error) {
            alert(`Gửi bài đăng thất bại: ${api.getErrorMessage(error)}`);
            setPosts(prev => prev.filter(p => p.id !== tempId)); 
            setNewPostContent(contentToSend); 
        } finally {
            setSending(false);
        }
    };

    if (loading) return <div className={styles.message}>Đang tải bài đăng...</div>;

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
                
                {posts.map(post => (
                    <div key={post.id} className={styles.postItem}>
                        <div className={styles.postAvatar}>{post.authorName.charAt(0).toUpperCase()}</div>
                        <div className={styles.postContent}>
                            <div className={styles.postHeader}>
                                <span className={styles.postAuthor}>
                                    {post.authorName}
                                    {(post.authorRole === 'teacher' || post.authorRole === 'admin') && 
                                        <span className={styles.roleTag}>{post.authorRole === 'admin' ? 'Admin' : 'GV'}</span>
                                    }
                                </span>
                                <span className={styles.postTime}>{post.timestamp}</span>
                            </div>
                            <p>{post.content}</p>
                        </div>
                    </div>
                ))}

                {posts.length === 0 && <p className={styles.message}>Chưa có bài đăng nào.</p>}
            </div>
            
            {}
            <div className={styles.newPostContainer}>
                <div className={styles.newPostInputWrapper}>
                    <textarea 
                        placeholder="Bắt đầu một bài đăng mới..." 
                        rows={3} 
                        value={newPostContent}
                        onChange={(e) => setNewPostContent(e.target.value)}
                        onKeyDown={(e) => { 
                            if (e.key === 'Enter' && e.ctrlKey) {
                                handleNewPost();
                            }
                        }}
                        disabled={sending}
                    />
                </div>
                <div className={styles.newPostActions}>
                    <button 
                        className={`${styles.actionButton} ${styles.sendButton}`}
                        onClick={handleNewPost}
                        disabled={!newPostContent.trim() || sending}
                    >
                        <FiSend /> {sending ? 'Đang gửi...' : 'Gửi'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PostsTab;