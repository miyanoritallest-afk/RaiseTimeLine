import { useState, useRef } from 'react'
import { useNavigate, useParams, Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { updateUser, uploadImage } from '../api/users'
import Avatar from '../components/Avatar'

export default function EditProfilePage() {
  const { id } = useParams<{ id: string }>()
  const { user: currentUser, updateUser: updateAuthUser } = useAuth()
  const navigate = useNavigate()

  const userId = Number(id)
  const isOwn = !!currentUser && currentUser.id === userId

  const [username, setUsername] = useState(currentUser?.username ?? '')
  const [bio, setBio] = useState(currentUser?.bio ?? '')
  const [avatarPreview, setAvatarPreview] = useState<string | null>(currentUser?.avatarUrl ?? null)
  const [avatarFile, setAvatarFile] = useState<File | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  if (!isOwn) {
    return <Navigate to="/" replace />
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setAvatarFile(file)
    const reader = new FileReader()
    reader.onload = (ev) => setAvatarPreview(ev.target?.result as string)
    reader.readAsDataURL(file)
  }

  const handleSave = async () => {
    if (!username.trim()) {
      setError('ユーザー名は必須です')
      return
    }
    if (username.length > 50) {
      setError('ユーザー名は50文字以内で入力してください')
      return
    }
    if (bio.length > 160) {
      setError('自己紹介は160文字以内で入力してください')
      return
    }

    setSaving(true)
    setError(null)
    try {
      let avatarUrl: string | null = currentUser.avatarUrl
      if (avatarFile) {
        const uploaded = await uploadImage(avatarFile, 'avatars')
        avatarUrl = uploaded.url
      }
      const updated = await updateUser(userId, {
        username: username.trim(),
        bio: bio.trim() || undefined,
        avatarUrl: avatarUrl ?? undefined,
      })
      updateAuthUser({
        username: updated.username,
        bio: updated.bio,
        avatarUrl: updated.avatarUrl,
      })
      navigate(`/users/${userId}`)
    } catch {
      setError('保存に失敗しました。もう一度お試しください。')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="edit-profile-wrapper">
      <div className="edit-profile-header">
        <button className="btn-ghost" onClick={() => navigate(`/users/${userId}`)}>
          ← 戻る
        </button>
        <h2 className="edit-profile-title">プロフィールを編集</h2>
      </div>

      <div className="edit-avatar-section">
        <div className="edit-avatar-row">
          <Avatar avatarUrl={avatarPreview} username={username} size="lg" />
          <label htmlFor="avatar-file-input" className="change-avatar-btn">
            画像を変更
          </label>
          <input
            id="avatar-file-input"
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ visibility: 'hidden', width: 0, height: 0, position: 'absolute' }}
            onChange={handleFileChange}
          />
        </div>
      </div>

      <div className="edit-form">
        <div className="form-group">
          <label className="form-label">ユーザー名 *</label>
          <input
            className="form-input"
            type="text"
            value={username}
            maxLength={50}
            onChange={(e) => setUsername(e.target.value)}
          />
          <span className="bio-char-count">{username.length} / 50</span>
        </div>

        <div className="form-group">
          <label className="form-label">自己紹介</label>
          <textarea
            className="form-input form-textarea"
            value={bio}
            maxLength={160}
            rows={4}
            onChange={(e) => setBio(e.target.value)}
            placeholder="自己紹介を入力してください"
          />
          <span className="bio-char-count">{bio.length} / 160</span>
        </div>

        {error && <div className="form-error">{error}</div>}

        <div className="edit-form-actions">
          <button
            className="btn-secondary"
            onClick={() => navigate(`/users/${userId}`)}
            disabled={saving}
          >
            キャンセル
          </button>
          <button className="btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </button>
        </div>
      </div>
    </main>
  )
}
