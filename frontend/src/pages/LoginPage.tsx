import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import axios from 'axios'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMsg, setErrorMsg] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setErrorMsg('')

    if (!email || !password) {
      setErrorMsg('メールアドレスとパスワードを入力してください。')
      return
    }

    setLoading(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        setErrorMsg('メールアドレスまたはパスワードが正しくありません。')
      } else {
        setErrorMsg('ログインに失敗しました。しばらくしてから再試行してください。')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card page-fade">
        <div className="auth-logo">RaiseTimeLine</div>

        {errorMsg && (
          <div className="alert-error visible">{errorMsg}</div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="login-email">
              メールアドレス
            </label>
            <input
              id="login-email"
              className="form-input"
              type="email"
              placeholder="example@mail.com"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="login-password">
              パスワード
            </label>
            <input
              id="login-password"
              className="form-input"
              type="password"
              placeholder="パスワード"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button
            className="btn btn-primary btn-full"
            type="submit"
            disabled={loading}
          >
            {loading ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>

        <div className="auth-link">
          アカウントをお持ちでない方は{' '}
          <Link to="/register">新規登録</Link>
        </div>
      </div>
    </div>
  )
}
