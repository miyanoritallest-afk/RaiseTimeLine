import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getFollowers, getFollowing } from '../api/users'
import type { FollowUserItem } from '../types/user'
import UserCard from './UserCard'

interface FollowListModalProps {
  userId: number
  mode: 'followers' | 'following'
  currentUserId: number
  onClose: () => void
}

export default function FollowListModal({ userId, mode, currentUserId, onClose }: FollowListModalProps) {
  const [users, setUsers] = useState<FollowUserItem[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const load = mode === 'followers' ? getFollowers : getFollowing
    load(userId)
      .then(setUsers)
      .finally(() => setLoading(false))
  }, [userId, mode])

  const handleFollowToggle = (targetUserId: number, newIsFollowing: boolean) => {
    setUsers((prev) =>
      prev.map((u) => (u.id === targetUserId ? { ...u, isFollowing: newIsFollowing } : u)),
    )
  }

  const handleUserClick = (targetUserId: number) => {
    onClose()
    navigate(`/users/${targetUserId}`)
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <button className="modal-close" onClick={onClose}>✕</button>
          <span className="modal-title">
            {mode === 'followers' ? 'フォロワー' : 'フォロー中'}
          </span>
        </div>
        <div className="modal-body">
          {loading ? (
            <div className="loading">読み込み中...</div>
          ) : users.length === 0 ? (
            <div className="empty-state">
              {mode === 'followers' ? 'フォロワーはいません' : 'フォロー中のユーザーはいません'}
            </div>
          ) : (
            users.map((u) => (
              <UserCard
                key={u.id}
                user={u}
                currentUserId={currentUserId}
                onFollowToggle={handleFollowToggle}
                onClick={handleUserClick}
              />
            ))
          )}
        </div>
      </div>
    </div>
  )
}
