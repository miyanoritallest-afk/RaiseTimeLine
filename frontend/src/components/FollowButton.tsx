import { useState } from 'react'
import { followUser, unfollowUser } from '../api/users'

interface FollowButtonProps {
  targetUserId: number
  isFollowing: boolean
  onToggle: (newIsFollowing: boolean) => void
}

export default function FollowButton({ targetUserId, isFollowing, onToggle }: FollowButtonProps) {
  const [loading, setLoading] = useState(false)

  const handleClick = async () => {
    if (loading) return
    setLoading(true)
    onToggle(!isFollowing)
    try {
      if (isFollowing) {
        await unfollowUser(targetUserId)
      } else {
        await followUser(targetUserId)
      }
    } catch {
      onToggle(isFollowing)
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      className={`follow-btn${isFollowing ? ' following' : ''}`}
      onClick={handleClick}
      disabled={loading}
    >
      {isFollowing ? 'フォロー中' : 'フォローする'}
    </button>
  )
}
