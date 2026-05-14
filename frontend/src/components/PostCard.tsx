import { useState, useRef, useCallback } from 'react'
import type { PostResponse } from '../types/post'
import Avatar from './Avatar'
import { useClickOutside } from '../hooks/useClickOutside'

interface PostCardProps {
  post: PostResponse
  currentUserId: number
  onLike: (id: number, liked: boolean) => void
  onEdit: (post: PostResponse) => void
  onDelete: (id: number) => void
}

function formatDate(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffSec = Math.floor(diffMs / 1000)
  if (diffSec < 60) return `${diffSec}秒`
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}分`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}時間`
  return d.toLocaleDateString('ja-JP', { month: 'numeric', day: 'numeric' })
}

export default function PostCard({ post, currentUserId, onLike, onEdit, onDelete }: PostCardProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const isOwn = post.author.id === currentUserId

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
      <Avatar avatarUrl={post.author.avatarUrl} username={post.author.username} />
      <div className="post-card-right">
        <div className="post-card-header">
          <span className="post-username">{post.author.username}</span>
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

        <div className="post-actions">
          <button
            className={`action-btn${post.likedByMe ? ' liked' : ''}`}
            onClick={() => onLike(post.id, post.likedByMe)}
          >
            <span className="action-icon">{post.likedByMe ? '♥' : '♡'}</span>
            {post.likeCount > 0 && <span className="action-count">{post.likeCount}</span>}
          </button>
          <button className="action-btn">
            <span className="action-icon">💬</span>
            {post.commentCount > 0 && <span className="action-count">{post.commentCount}</span>}
          </button>
        </div>
      </div>
    </article>
  )
}
