export interface Author {
  id: number
  username: string
  avatarUrl: string | null
}

export interface PostResponse {
  id: number
  author: Author
  content: string
  imageUrls: string[]
  likeCount: number
  commentCount: number
  likedByMe: boolean
  createdAt: string
  updatedAt: string
}

export interface PagedResponse<T> {
  items: T[]
  nextCursor: number | null
  hasMore: boolean
}
