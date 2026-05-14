import { useState } from 'react'
import type { PostResponse } from '../types/post'
import { getCharCountClass, POST_MAX_LENGTH } from '../utils/charCount'

interface EditPostModalProps {
  post: PostResponse
  onSave: (content: string) => Promise<void>
  onClose: () => void
}

export default function EditPostModal({ post, onSave, onClose }: EditPostModalProps) {
  const [content, setContent] = useState(post.content)
  const [saving, setSaving] = useState(false)

  const [error, setError] = useState<string | null>(null)
  const length = content.length
  const overLimit = length > POST_MAX_LENGTH
  const isEmpty = content.trim().length === 0
  const charCountClass = getCharCountClass(length)

  const handleSave = async () => {
    if (isEmpty || overLimit || saving) return
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
          <h3>投稿を編集</h3>
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
            <span className={charCountClass}>{280 - length}</span>
            <div className="edit-post-actions">
              <button className="btn btn-outline" onClick={onClose}>キャンセル</button>
              <button
                className="btn btn-primary"
                onClick={handleSave}
                disabled={isEmpty || overLimit || saving}
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
