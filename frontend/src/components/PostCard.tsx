import { useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import type { PostResponse } from '../types/post'
import Avatar from './Avatar'
import { useClickOutside } from '../hooks/useClickOutside'
import { formatDate } from '../utils/formatDate'

interface PostCardProps {
  post: PostResponse
  currentUserId: number
  onLike: (id: number, liked: boolean) => void
  onEdit: (post: PostResponse) => void
  onDelete: (id: number) => void
  onComment: (post: PostResponse) => void
}

export default function PostCard({ post, currentUserId, onLike, onEdit, onDelete, onComment }: PostCardProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const isOwn = post.author.id === currentUserId
  const navigate = useNavigate()

  const closeMenu = useCallback(() => setMenuOpen(false), [])
  useClickOutside(menuRef, closeMenu)

  const handleDelete = () => {
    setMenuOpen(false)
    if (window.confirm('この投稿を削除しますか？')) {
      onDelete(post.id)
    }
  }

  const handleEdit = () => {
    setMenuOpen(false)
    onEdit(post)
  }

  return (
    <article className="post-card">
      <Avatar
        avatarUrl={post.author.avatarUrl}
        username={post.author.username}
        onClick={() => navigate(`/users/${post.author.id}`)}
      />
      <div className="post-card-right">
        <div className="post-card-header">
          <span
            className="post-username"
            style={{ cursor: 'pointer' }}
            onClick={() => navigate(`/users/${post.author.id}`)}
          >
            {post.author.username}
          </span>
          <span className="post-date">· {formatDate(post.createdAt)}</span>
          {isOwn && (
            <div ref={menuRef} style={{ marginLeft: 'auto', position: 'relative' }}>
              <button
                className="post-menu-btn"
                onClick={(e) => { e.stopPropagation(); setMenuOpen((o) => !o) }}
              >
                ···
              </button>
              {menuOpen && (
                <div className="post-menu-dropdown">
                  <button className="post-menu-item" onClick={handleEdit}>編集</button>
                  <button className="post-menu-item danger" onClick={handleDelete}>削除</button>
                </div>
              )}
            </div>
          )}
        </div>

        <div
          className="post-content-area"
          onClick={() => navigate(`/posts/${post.id}`)}
          style={{ cursor: 'pointer' }}
        >
          <p className="post-content">{post.content}</p>

          {post.imageUrls.length > 0 && (
            <div className="post-images" data-count={post.imageUrls.length}>
              {post.imageUrls.map((url, i) => (
                <div key={i} className="post-img-wrap">
                  <img src={url} alt="" className="post-img" />
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="post-actions">
          <button
            className={`action-btn${post.likedByMe ? ' liked' : ''}`}
            onClick={() => onLike(post.id, post.likedByMe)}
          >
            <span className="action-icon">{post.likedByMe ? '♥' : '♡'}</span>
            {post.likeCount > 0 && <span className="action-count">{post.likeCount}</span>}
          </button>
          <button className="action-btn" onClick={() => onComment(post)}>
            <span className="action-icon">💬</span>
            {post.commentCount > 0 && <span className="action-count">{post.commentCount}</span>}
          </button>
        </div>
      </div>
    </article>
  )
}
