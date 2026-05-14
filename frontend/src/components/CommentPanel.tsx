import { useState, useEffect, useRef, useCallback } from 'react'
import type { PostResponse, CommentResponse } from '../types/post'
import { getComments, createComment, updateComment, deleteComment } from '../api/posts'
import { useAuth } from '../contexts/AuthContext'
import { useClickOutside } from '../hooks/useClickOutside'
import Avatar from './Avatar'
import EditCommentModal from './EditCommentModal'

interface CommentPanelProps {
  post: PostResponse
  currentUserId: number
  onClose: () => void
  onCommentCountChange: (postId: number, delta: number) => void
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

interface CommentItemProps {
  comment: CommentResponse
  currentUserId: number
  onEdit: (comment: CommentResponse) => void
  onDelete: (commentId: number) => void
}

function CommentItem({ comment, currentUserId, onEdit, onDelete }: CommentItemProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const isOwn = comment.author.id === currentUserId

  const closeMenu = useCallback(() => setMenuOpen(false), [])
  useClickOutside(menuRef, closeMenu)

  const handleDelete = () => {
    setMenuOpen(false)
    if (window.confirm('このコメントを削除しますか？')) {
      onDelete(comment.id)
    }
  }

  return (
    <div className="comment-item">
      <Avatar avatarUrl={comment.author.avatarUrl} username={comment.author.username} size="sm" />
      <div className="comment-right">
        <div className="comment-header">
          <span className="comment-username">{comment.author.username}</span>
          <span className="comment-date">{formatDate(comment.createdAt)}</span>
          {isOwn && (
            <div ref={menuRef} style={{ marginLeft: 'auto', position: 'relative' }}>
              <button
                className="comment-menu-btn"
                onClick={(e) => { e.stopPropagation(); setMenuOpen((o) => !o) }}
              >
                ···
              </button>
              {menuOpen && (
                <div className="post-menu-dropdown">
                  <button className="post-menu-item" onClick={() => { setMenuOpen(false); onEdit(comment) }}>編集</button>
                  <button className="post-menu-item danger" onClick={handleDelete}>削除</button>
                </div>
              )}
            </div>
          )}
        </div>
        <div className="comment-content">{comment.content}</div>
      </div>
    </div>
  )
}

export default function CommentPanel({ post, currentUserId, onClose, onCommentCountChange }: CommentPanelProps) {
  const { user } = useAuth()
  const [comments, setComments] = useState<CommentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [editingComment, setEditingComment] = useState<CommentResponse | null>(null)

  useEffect(() => {
    let cancelled = false
    getComments(post.id).then((data) => {
      if (!cancelled) {
        setComments(data)
        setLoading(false)
      }
    }).catch(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  }, [post.id])

  const handleSubmit = async () => {
    const trimmed = content.trim()
    if (!trimmed || submitting) return
    setSubmitting(true)
    try {
      const created = await createComment(post.id, trimmed)
      setComments((prev) => [...prev, created])
      onCommentCountChange(post.id, 1)
      setContent('')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEditSave = async (newContent: string) => {
    if (!editingComment) return
    const updated = await updateComment(editingComment.id, newContent)
    setComments((prev) => prev.map((c) => (c.id === updated.id ? updated : c)))
  }

  const handleDelete = async (commentId: number) => {
    await deleteComment(commentId)
    setComments((prev) => prev.filter((c) => c.id !== commentId))
    onCommentCountChange(post.id, -1)
  }

  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div
        className="modal modal-edit"
        style={{ width: '90%', maxWidth: '560px', maxHeight: '80vh', display: 'flex', flexDirection: 'column', padding: 0 }}
      >
        <div className="modal-header">
          <h3>コメント ({comments.length}件)</h3>
          <button className="modal-close-btn" onClick={onClose}>×</button>
        </div>

        <div style={{ overflowY: 'auto', flex: 1, padding: '0 16px' }}>
          {loading ? (
            <p style={{ padding: '16px 0', color: 'var(--color-text-secondary)', textAlign: 'center' }}>読み込み中...</p>
          ) : comments.length === 0 ? (
            <p className="empty-comments" style={{ padding: '24px 0' }}>
              コメントはまだありません。最初のコメントを投稿しましょう！
            </p>
          ) : (
            comments.map((c) => (
              <CommentItem
                key={c.id}
                comment={c}
                currentUserId={currentUserId}
                onEdit={setEditingComment}
                onDelete={handleDelete}
              />
            ))
          )}
        </div>

        <div
          className="comment-form-wrapper"
          style={{ borderTop: '1px solid var(--color-border)', padding: '12px 16px', display: 'flex', gap: '8px', alignItems: 'flex-start' }}
        >
          <Avatar avatarUrl={user?.avatarUrl ?? null} username={user?.username ?? ''} size="sm" />
          <textarea
            className="comment-textarea"
            placeholder="コメントを入力..."
            rows={2}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={(e) => { if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') handleSubmit() }}
            style={{ flex: 1 }}
          />
          <button
            className="btn btn-primary comment-form-btn"
            onClick={handleSubmit}
            disabled={!content.trim() || submitting}
          >
            {submitting ? '投稿中...' : 'コメントする'}
          </button>
        </div>
      </div>

      {editingComment && (
        <EditCommentModal
          comment={editingComment}
          onSave={handleEditSave}
          onClose={() => setEditingComment(null)}
        />
      )}
    </>
  )
}
