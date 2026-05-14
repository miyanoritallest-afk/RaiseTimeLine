import { useState } from 'react'
import type { CommentResponse } from '../types/post'

interface EditCommentModalProps {
  comment: CommentResponse
  onSave: (content: string) => Promise<void>
  onClose: () => void
}

export default function EditCommentModal({ comment, onSave, onClose }: EditCommentModalProps) {
  const [content, setContent] = useState(comment.content)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isEmpty = content.trim().length === 0

  const handleSave = async () => {
    if (isEmpty || saving) return
    setSaving(true)
    setError(null)
    try {
      await onSave(content.trim())
      onClose()
    } catch {
      setError('保存に失敗しました。もう一度お試しください。')
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div className="modal modal-edit">
        <div className="modal-header">
          <h3>コメントを編集</h3>
          <button className="modal-close-btn" onClick={onClose}>×</button>
        </div>
        <div style={{ padding: '16px' }}>
          <textarea
            className="edit-textarea"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            autoFocus
          />
          {error && <p style={{ color: 'var(--color-danger)', fontSize: '13px', marginBottom: '8px' }}>{error}</p>}
          <div className="edit-post-footer">
            <div />
            <div className="edit-post-actions">
              <button className="btn btn-outline" onClick={onClose}>キャンセル</button>
              <button
                className="btn btn-primary"
                onClick={handleSave}
                disabled={isEmpty || saving}
              >
                {saving ? '保存中...' : '保存する'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
