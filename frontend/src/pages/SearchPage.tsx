import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { searchUsers } from '../api/users'
import { useDebounce } from '../hooks/useDebounce'
import UserCard from '../components/UserCard'
import type { UserResponse } from '../types/user'

export default function SearchPage() {
  const { user: currentUser } = useAuth()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const debouncedQuery = useDebounce(query, 400)

  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setResults([])
      setSearched(false)
      return
    }
    setLoading(true)
    setSearched(true)
    searchUsers(debouncedQuery.trim())
      .then((data) => {
        setResults(data.filter((u) => u.id !== currentUser?.id))
      })
      .catch(() => setResults([]))
      .finally(() => setLoading(false))
  }, [debouncedQuery, currentUser?.id])

  const handleFollowToggle = (userId: number, newIsFollowing: boolean) => {
    setResults((prev) =>
      prev.map((u) => (u.id === userId ? { ...u, isFollowing: newIsFollowing } : u)),
    )
  }

  return (
    <main className="search-wrapper">
      <div className="search-input-wrapper">
        <span className="search-icon">🔍</span>
        <input
          className="search-input"
          type="text"
          placeholder="ユーザーを検索"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />
      </div>

      <div className="search-results">
        {loading ? (
          <div className="loading">検索中...</div>
        ) : searched && results.length === 0 ? (
          <div className="search-empty">ユーザーが見つかりませんでした</div>
        ) : !searched ? (
          <div className="search-hint">ユーザー名を入力して検索</div>
        ) : (
          results.map((u) => (
            <UserCard
              key={u.id}
              user={u}
              currentUserId={currentUser!.id}
              onFollowToggle={handleFollowToggle}
              onClick={(id) => navigate(`/users/${id}`)}
            />
          ))
        )}
      </div>
    </main>
  )
}
