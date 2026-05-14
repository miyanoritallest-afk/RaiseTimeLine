import { useState } from 'react'
import type { PostResponse } from '../types/post'

interface EditPostModalProps {
  post: PostResponse
  onSave: (content: string) => Promise<void>
  onClose: () => void
}

export default function EditPostModal({ post, onSave, onClose }: EditPostModalProps) {
  const [content, setContent] = useState(post.content)
  const [saving, setSaving] = useState(false)

  const length = content.length
  const overLimit = length > 280
  const isEmpty = content.trim().length === 0
  const charCountClass = length >= 270 ? 'char-count danger' : length >= 260 ? 'char-count warning' : 'char-count'

  const handleSave = async () => {
    if (isEmpty || overLimit || saving) return
    setSaving(true)
    try {
      await onSave(content.trim())
      onClose()
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
