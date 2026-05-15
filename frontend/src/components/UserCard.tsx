import Avatar from './Avatar'
import FollowButton from './FollowButton'
import type { UserResponse, FollowUserItem } from '../types/user'

interface UserCardProps {
  user: UserResponse | FollowUserItem
  currentUserId: number
  onFollowToggle: (userId: number, newIsFollowing: boolean) => void
  onClick: (userId: number) => void
}

export default function UserCard({ user, currentUserId, onFollowToggle, onClick }: UserCardProps) {
  const isOwn = user.id === currentUserId

  return (
    <div className="user-card" onClick={() => onClick(user.id)}>
      <Avatar avatarUrl={user.avatarUrl} username={user.username} size="md" />
      <div className="user-card-info">
        <span className="user-card-name">{user.username}</span>
        {user.bio && <span className="user-card-bio">{user.bio}</span>}
      </div>
      {!isOwn && (
        <div onClick={(e) => e.stopPropagation()}>
          <FollowButton
            targetUserId={user.id}
            isFollowing={user.isFollowing}
            onToggle={(newIsFollowing) => onFollowToggle(user.id, newIsFollowing)}
          />
        </div>
      )}
    </div>
  )
}
