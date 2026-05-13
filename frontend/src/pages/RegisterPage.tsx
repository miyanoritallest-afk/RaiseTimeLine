import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from 'axios'

interface FieldErrors {
  username: string
  email: string
  password: string
  password2: string
}

const emptyErrors: FieldErrors = { username: '', email: '', password: '', password2: '' }

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [password2, setPassword2] = useState('')
  const [errors, setErrors] = useState<FieldErrors>(emptyErrors)
  const [globalError, setGlobalError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = (): boolean => {
    const next: FieldErrors = { ...emptyErrors }
    let valid = true

    if (!username.trim()) {
      next.username = 'ユーザー名を入力してください。'
      valid = false
    } else if (username.trim().length > 50) {
      next.username = 'ユーザー名は50文字以内で入力してください。'
      valid = false
    }

    if (!email.trim()) {
      next.email = 'メールアドレスを入力してください。'
      valid = false
    }

    if (password.length < 8) {
      next.password = 'パスワードは8文字以上で入力してください。'
      valid = false
    }

    if (password !== password2) {
      next.password2 = 'パスワードが一致しません。'
      valid = false
    }

    setErrors(next)
    return valid
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setGlobalError('')
    if (!validate()) return

    setLoading(true)
    try {
      await register(username.trim(), email.trim(), password)
      navigate('/')
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setErrors((prev) => ({ ...prev, email: 'このメールアドレスは既に使用されています。' }))
      } else {
        setGlobalError('登録に失敗しました。しばらくしてから再試行してください。')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card page-fade">
        <div className="auth-logo">RaiseTimeLine</div>
        <div className="auth-title">新規アカウント登録</div>

        {globalError && (
          <div className="alert-error visible">{globalError}</div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="reg-username">
              ユーザー名 *
            </label>
            <input
              id="reg-username"
              className={`form-input${errors.username ? ' error' : ''}`}
              type="text"
              maxLength={50}
              placeholder="ユーザー名（最大50文字）"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            {errors.username && (
              <div className="form-error visible">{errors.username}</div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-email">
              メールアドレス *
            </label>
            <input
              id="reg-email"
              className={`form-input${errors.email ? ' error' : ''}`}
              type="email"
              placeholder="example@mail.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            {errors.email && (
              <div className="form-error visible">{errors.email}</div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-password">
              パスワード *
            </label>
            <input
              id="reg-password"
              className={`form-input${errors.password ? ' error' : ''}`}
              type="password"
              placeholder="8文字以上"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {errors.password && (
              <div className="form-error visible">{errors.password}</div>
            )}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-password2">
              パスワード（確認）*
            </label>
            <input
              id="reg-password2"
              className={`form-input${errors.password2 ? ' error' : ''}`}
              type="password"
              placeholder="パスワードを再入力"
              value={password2}
              onChange={(e) => setPassword2(e.target.value)}
            />
            {errors.password2 && (
              <div className="form-error visible">{errors.password2}</div>
            )}
          </div>

          <button
            className="btn btn-primary btn-full"
            type="submit"
            disabled={loading}
          >
            {loading ? '登録中...' : '登録する'}
          </button>
        </form>

        <div className="auth-link">
          すでにアカウントをお持ちの方は{' '}
          <Link to="/login">ログイン</Link>
        </div>
      </div>
    </div>
  )
}
