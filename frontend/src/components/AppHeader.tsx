import { useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import Avatar from './Avatar'
import { useClickOutside } from '../hooks/useClickOutside'

export default function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const closeMenu = useCallback(() => setMenuOpen(false), [])
  useClickOutside(menuRef, closeMenu)

  const handleLogout = async () => {
    setMenuOpen(false)
    await logout()
    navigate('/login')
  }

  return (
    <header id="app-header">
      <div className="header-inner">
        <span className="header-logo" onClick={() => navigate('/')}>
          RaiseTimeLine
        </span>
        <div className="header-actions">
          {user && (
            <>
              <button
                className="header-search-btn"
                onClick={() => navigate('/search')}
                aria-label="ユーザー検索"
              >
                🔍
              </button>
              <div className="user-menu-wrapper" ref={menuRef}>
                <button
                  className="user-menu-trigger"
                  onClick={() => setMenuOpen((o) => !o)}
                >
                  <Avatar avatarUrl={user.avatarUrl} username={user.username} size="sm" />
                  <span className="user-menu-name">{user.username}</span>
                  <span className="user-menu-caret">▾</span>
                </button>
                {menuOpen && (
                  <div className="dropdown-menu">
                    <button
                      className="dropdown-item"
                      onClick={() => { setMenuOpen(false); navigate(`/users/${user.id}`) }}
                    >
                      プロフィール
                    </button>
                    <button className="dropdown-item danger" onClick={handleLogout}>
                      ログアウト
                    </button>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
