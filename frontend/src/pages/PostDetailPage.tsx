import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import {
  getPost,
  getComments,
  createComment,
  updateComment,
  deleteComment,
  toggleLike,
} from '../api/posts'
import type { PostResponse, CommentResponse } from '../types/post'
import PostCard from '../components/PostCard'
import { CommentItem } from '../components/CommentPanel'
import Avatar from '../components/Avatar'
import EditCommentModal from '../components/EditCommentModal'

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>()
  const postId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()

  const [post, setPost] = useState<PostResponse | null>(null)
  const [comments, setComments] = useState<CommentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [editingComment, setEditingComment] = useState<CommentResponse | null>(null)

  useEffect(() => {
    Promise.all([getPost(postId), getComments(postId)])
      .then(([p, c]) => {
        setPost(p)
        setComments(c)
      })
      .catch(() => navigate('/'))
      .finally(() => setLoading(false))
  }, [postId, navigate])

  const handleLike = async (id: number, liked: boolean) => {
    setPost((prev) =>
      prev ? { ...prev, likedByMe: !liked, likeCount: prev.likeCount + (liked ? -1 : 1) } : prev
    )
    try {
      await toggleLike(id, liked)
    } catch {
      setPost((prev) =>
        prev ? { ...prev, likedByMe: liked, likeCount: prev.likeCount + (liked ? 1 : -1) } : prev
      )
    }
  }

  const handleSubmitComment = async () => {
    const trimmed = content.trim()
    if (!trimmed || submitting) return
    setSubmitting(true)
    try {
      const created = await createComment(postId, trimmed)
      setComments((prev) => [...prev, created])
      setPost((prev) => (prev ? { ...prev, commentCount: prev.commentCount + 1 } : prev))
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

  const handleDeleteComment = async (commentId: number) => {
    await deleteComment(commentId)
    setComments((prev) => prev.filter((c) => c.id !== commentId))
    setPost((prev) => (prev ? { ...prev, commentCount: Math.max(0, prev.commentCount - 1) } : prev))
  }

  if (loading) {
    return (
      <main id="app-main">
        <div className="empty-state"><p>読み込み中...</p></div>
      </main>
    )
  }

  if (!post || !user) return null

  return (
    <main id="app-main">
      <div className="post-detail-back">
        <button className="btn-ghost" onClick={() => navigate(-1)}>← 戻る</button>
      </div>

      <PostCard
        post={post}
        currentUserId={user.id}
        onLike={handleLike}
        onEdit={() => {}}
        onDelete={() => {}}
        onComment={() => {}}
      />

      <div className="post-detail-comments">
        <h4 className="comment-section-title">コメント ({comments.length}件)</h4>

        {comments.length === 0 ? (
          <p className="empty-comments" style={{ padding: '24px 16px' }}>
            コメントはまだありません。最初のコメントを投稿しましょう！
          </p>
        ) : (
          <div style={{ padding: '0 16px' }}>
            {comments.map((c) => (
              <CommentItem
                key={c.id}
                comment={c}
                currentUserId={user.id}
                onEdit={setEditingComment}
                onDelete={handleDeleteComment}
              />
            ))}
          </div>
        )}

        <div
          className="comment-form-wrapper"
          style={{
            borderTop: '1px solid var(--color-border)',
            padding: '12px 16px',
            display: 'flex',
            gap: '8px',
            alignItems: 'flex-start',
          }}
        >
          <Avatar avatarUrl={user.avatarUrl ?? null} username={user.username} size="sm" />
          <textarea
            className="comment-textarea"
            placeholder="コメントを入力..."
            rows={2}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={(e) => {
              if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') handleSubmitComment()
            }}
            style={{ flex: 1 }}
          />
          <button
            className="btn btn-primary comment-form-btn"
            onClick={handleSubmitComment}
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
    </main>
  )
}
