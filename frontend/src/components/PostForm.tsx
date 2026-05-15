import { useState, useRef } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { createPost } from '../api/posts'
import { uploadImage } from '../api/users'
import type { PostResponse } from '../types/post'
import Avatar from './Avatar'
import { getCharCountClass, POST_MAX_LENGTH } from '../utils/charCount'

interface PostFormProps {
  onPosted: (post: PostResponse) => void
}

export default function PostForm({ onPosted }: PostFormProps) {
  const { user } = useAuth()
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [imageFiles, setImageFiles] = useState<File[]>([])
  const [imagePreviews, setImagePreviews] = useState<string[]>([])
  const [uploading, setUploading] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)

  const length = content.length
  const overLimit = length > POST_MAX_LENGTH
  const isEmpty = content.trim().length === 0 && imageFiles.length === 0
  const charCountClass = getCharCountClass(length)

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files) return
    const remaining = 4 - imageFiles.length
    const allowed = Array.from(files).slice(0, remaining)
    const newPreviews = allowed.map((f) => URL.createObjectURL(f))
    setImageFiles((prev) => [...prev, ...allowed])
    setImagePreviews((prev) => [...prev, ...newPreviews])
    e.target.value = ''
  }

  const handleRemoveImage = (index: number) => {
    URL.revokeObjectURL(imagePreviews[index])
    setImageFiles((prev) => prev.filter((_, i) => i !== index))
    setImagePreviews((prev) => prev.filter((_, i) => i !== index))
  }

  const handleSubmit = async () => {
    if ((isEmpty && imageFiles.length === 0) || overLimit || submitting || uploading) return
    if (content.trim().length === 0 && imageFiles.length === 0) return
    setSubmitting(true)
    try {
      let imageUrls: string[] | undefined
      if (imageFiles.length > 0) {
        setUploading(true)
        imageUrls = await Promise.all(imageFiles.map((f) => uploadImage(f, 'posts').then((r) => r.url)))
        setUploading(false)
      }
      const post = await createPost(content.trim(), imageUrls)
      setContent('')
      imagePreviews.forEach((url) => URL.revokeObjectURL(url))
      setImageFiles([])
      setImagePreviews([])
      if (textareaRef.current) textareaRef.current.style.height = 'auto'
      onPosted(post)
    } catch {
      setUploading(false)
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

  const isSubmitDisabled = (content.trim().length === 0 && imageFiles.length === 0) || overLimit || submitting || uploading

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

        {imagePreviews.length > 0 && (
          <div className="post-image-previews">
            {imagePreviews.map((src, i) => (
              <div key={i} className="post-image-preview-item">
                <img src={src} alt="" />
                <button
                  className="post-image-preview-remove"
                  onClick={() => handleRemoveImage(i)}
                  type="button"
                >
                  ×
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="post-form-divider" />
        <div className="post-form-footer">
          <div className="post-form-tools">
            <input
              ref={imageInputRef}
              type="file"
              accept="image/*"
              multiple
              style={{ display: 'none' }}
              onChange={handleImageSelect}
            />
            <button
              type="button"
              className="post-image-btn"
              onClick={() => imageInputRef.current?.click()}
              disabled={imageFiles.length >= 4}
              title="画像を追加"
            >
              🖼
            </button>
          </div>
          <div className="post-form-right">
            {length > 0 && (
              <span className={charCountClass}>{280 - length}</span>
            )}
            {length > 0 && <div className="char-divider" />}
            <button
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={isSubmitDisabled}
            >
              {uploading ? 'アップロード中...' : submitting ? '投稿中...' : '投稿する'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
