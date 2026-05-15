export interface UserResponse {
  id: number
  username: string
  email: string
  bio: string | null
  avatarUrl: string | null
  followersCount: number
  followingCount: number
  isFollowing: boolean
}

export interface FollowUserItem {
  id: number
  username: string
  avatarUrl: string | null
  bio: string | null
  isFollowing: boolean
}
