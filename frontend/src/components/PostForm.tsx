import { useState, useRef } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { createPost } from '../api/posts'
import type { PostResponse } from '../types/post'
import Avatar from './Avatar'

interface PostFormProps {
  onPosted: (post: PostResponse) => void
}

export default function PostForm({ onPosted }: PostFormProps) {
  const { user } = useAuth()
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const length = content.length
  const overLimit = length > 280
  const isEmpty = content.trim().length === 0

  const charCountClass = length >= 270 ? 'char-count danger' : length >= 260 ? 'char-count warning' : 'char-count'

  const handleSubmit = async () => {
    if (isEmpty || overLimit || submitting) return
    setSubmitting(true)
    try {
      const post = await createPost(content.trim())
      setContent('')
      if (textareaRef.current) textareaRef.current.style.height = 'auto'
      onPosted(post)
    } finally {
      setSubmitting(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSubmit()
    }
  }

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value)
    const el = e.target
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }

  if (!user) return null

  return (
    <div className="post-form-wrapper">
      <Avatar avatarUrl={user.avatarUrl} username={user.username} />
      <div className="post-form-main">
        <textarea
          ref={textareaRef}
          className="post-textarea"
          placeholder="いまどうしてる？"
          value={content}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          rows={2}
        />
        <div className="post-form-divider" />
        <div className="post-form-footer">
          <div className="post-form-tools" />
          <div className="post-form-right">
            {length > 0 && (
              <span className={charCountClass}>{280 - length}</span>
            )}
            {length > 0 && <div className="char-divider" />}
            <button
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={isEmpty || overLimit || submitting}
            >
              {submitting ? '投稿中...' : '投稿する'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
