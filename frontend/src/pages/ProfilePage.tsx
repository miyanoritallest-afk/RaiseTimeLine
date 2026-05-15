import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { getUser, getUserPosts, getUserLikedPosts, getUserComments } from '../api/users'
import { updatePost, deletePost, toggleLike } from '../api/posts'
import type { UserResponse } from '../types/user'
import type { PostResponse, CommentResponse } from '../types/post'
import Avatar from '../components/Avatar'
import PostCard from '../components/PostCard'
import EditPostModal from '../components/EditPostModal'
import CommentPanel from '../components/CommentPanel'
import { CommentItem } from '../components/CommentPanel'
import FollowButton from '../components/FollowButton'
import FollowListModal from '../components/FollowListModal'
import { formatDate } from '../utils/formatDate'

type ProfileTab = 'posts' | 'comments' | 'likes'

export default function ProfilePage() {
  const { id } = useParams<{ id: string }>()
  const { user: currentUser } = useAuth()
  const navigate = useNavigate()

  const userId = Number(id)

  const [profileUser, setProfileUser] = useState<UserResponse | null>(null)
  const [posts, setPosts] = useState<PostResponse[]>([])
  const [likedPosts, setLikedPosts] = useState<PostResponse[]>([])
  const [userComments, setUserComments] = useState<CommentResponse[]>([])
  const [activeTab, setActiveTab] = useState<ProfileTab>('posts')
  const [tabLoading, setTabLoading] = useState(false)
  const [loading, setLoading] = useState(true)
  const [editingPost, setEditingPost] = useState<PostResponse | null>(null)
  const [commentPost, setCommentPost] = useState<PostResponse | null>(null)
  const [followListMode, setFollowListMode] = useState<'followers' | 'following' | null>(null)

  const loadProfile = useCallback(async () => {
    if (isNaN(userId)) { navigate('/'); return }
    setLoading(true)
    try {
      const [user, postsData] = await Promise.all([
        getUser(userId),
        getUserPosts(userId),
      ])
      setProfileUser(user)
      setPosts(postsData)
    } catch {
      navigate('/')
    } finally {
      setLoading(false)
    }
  }, [userId, navigate])

  useEffect(() => {
    loadProfile()
  }, [loadProfile])

  const handleTabChange = async (tab: ProfileTab) => {
    setActiveTab(tab)
    if (tab === 'likes' && likedPosts.length === 0) {
      setTabLoading(true)
      try { setLikedPosts(await getUserLikedPosts(userId)) } finally { setTabLoading(false) }
    }
    if (tab === 'comments' && userComments.length === 0) {
      setTabLoading(true)
      try { setUserComments(await getUserComments(userId)) } finally { setTabLoading(false) }
    }
  }

  const handleFollowToggle = (newIsFollowing: boolean) => {
    setProfileUser((prev) =>
      prev
        ? {
            ...prev,
            isFollowing: newIsFollowing,
            followersCount: prev.followersCount + (newIsFollowing ? 1 : -1),
          }
        : prev,
    )
  }

  const handleLike = async (postId: number, liked: boolean) => {
    setPosts((prev) =>
      prev.map((p) =>
        p.id === postId
          ? { ...p, likedByMe: !liked, likeCount: p.likeCount + (liked ? -1 : 1) }
          : p,
      ),
    )
    try {
      await toggleLike(postId, liked)
    } catch {
      setPosts((prev) =>
        prev.map((p) =>
          p.id === postId
            ? { ...p, likedByMe: liked, likeCount: p.likeCount + (liked ? 1 : -1) }
            : p,
        ),
      )
    }
  }

  const handleEditSave = async (postId: number, content: string) => {
    const updated = await updatePost(postId, content)
    setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)))
    setEditingPost(null)
  }

  const handleDelete = async (postId: number) => {
    await deletePost(postId)
    setPosts((prev) => prev.filter((p) => p.id !== postId))
  }

  if (loading) {
    return <div className="loading">読み込み中...</div>
  }

  if (!profileUser) return null

  const isOwn = currentUser?.id === profileUser.id

  return (
    <main className="profile-wrapper">
      <div className="profile-header">
        <div className="profile-top">
          <Avatar avatarUrl={profileUser.avatarUrl} username={profileUser.username} size="lg" />
          <div className="profile-actions">
            {isOwn ? (
              <button
                className="btn-outline"
                onClick={() => navigate(`/users/${profileUser.id}/edit`)}
              >
                プロフィールを編集
              </button>
            ) : (
              <FollowButton
                targetUserId={profileUser.id}
                isFollowing={profileUser.isFollowing}
                onToggle={handleFollowToggle}
              />
            )}
          </div>
        </div>

        <div className="profile-info">
          <span className="profile-username">{profileUser.username}</span>
          {profileUser.bio && <p className="profile-bio">{profileUser.bio}</p>}
          <div className="profile-stats">
            <button
              className="profile-stat"
              onClick={() => setFollowListMode('following')}
            >
              <span className="profile-stat-num">{profileUser.followingCount}</span>
              <span className="profile-stat-label">フォロー中</span>
            </button>
            <button
              className="profile-stat"
              onClick={() => setFollowListMode('followers')}
            >
              <span className="profile-stat-num">{profileUser.followersCount}</span>
              <span className="profile-stat-label">フォロワー</span>
            </button>
          </div>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab-btn${activeTab === 'posts' ? ' active' : ''}`} onClick={() => handleTabChange('posts')}>投稿</button>
        <button className={`tab-btn${activeTab === 'comments' ? ' active' : ''}`} onClick={() => handleTabChange('comments')}>コメント</button>
        <button className={`tab-btn${activeTab === 'likes' ? ' active' : ''}`} onClick={() => handleTabChange('likes')}>いいね</button>
      </div>

      {tabLoading ? (
        <div className="loading">読み込み中...</div>
      ) : activeTab === 'posts' ? (
        <div className="posts-list">
          {posts.length === 0 ? (
            <div className="empty-state">投稿がありません</div>
          ) : (
            posts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                currentUserId={currentUser!.id}
                onLike={handleLike}
                onEdit={setEditingPost}
                onDelete={handleDelete}
                onComment={setCommentPost}
              />
            ))
          )}
        </div>
      ) : activeTab === 'comments' ? (
        <div className="posts-list">
          {userComments.length === 0 ? (
            <div className="empty-state">コメントがありません</div>
          ) : (
            userComments.map((comment) => (
              <div key={comment.id} className="profile-comment-item" onClick={() => navigate(`/posts/${comment.postId}`)}>
                <CommentItem
                  comment={comment}
                  currentUserId={currentUser!.id}
                  onEdit={() => {}}
                  onDelete={() => {}}
                />
              </div>
            ))
          )}
        </div>
      ) : (
        <div className="posts-list">
          {likedPosts.length === 0 ? (
            <div className="empty-state">いいねした投稿がありません</div>
          ) : (
            likedPosts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                currentUserId={currentUser!.id}
                onLike={handleLike}
                onEdit={setEditingPost}
                onDelete={handleDelete}
                onComment={setCommentPost}
              />
            ))
          )}
        </div>
      )}

      {editingPost && (
        <EditPostModal
          post={editingPost}
          onSave={handleEditSave}
          onClose={() => setEditingPost(null)}
        />
      )}

      {commentPost && (
        <CommentPanel
          post={commentPost}
          currentUserId={currentUser!.id}
          onClose={() => setCommentPost(null)}
        />
      )}

      {followListMode && (
        <FollowListModal
          userId={profileUser.id}
          mode={followListMode}
          currentUserId={currentUser!.id}
          onClose={() => setFollowListMode(null)}
        />
      )}
    </main>
  )
}
