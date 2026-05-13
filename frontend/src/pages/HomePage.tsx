import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="home-wrapper">
      <div className="success-card page-fade">
        <div className="success-icon">🎉</div>
        <div className="success-title">ログイン成功！</div>
        <p className="success-subtitle">
          ようこそ、<strong>{user?.username}</strong> さん
        </p>
        <button className="btn btn-dark btn-full" onClick={handleLogout}>
          ログアウト
        </button>
      </div>
    </div>
  )
}
