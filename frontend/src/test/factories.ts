import type { Author, PostResponse, CommentResponse } from '../types/post'
import type { UserResponse } from '../types/user'

export function makeAuthor(overrides: Partial<Author> = {}): Author {
  return {
    id: 1,
    username: 'alice',
    avatarUrl: null,
    ...overrides,
  }
}

export function makeUser(overrides: Partial<UserResponse> = {}): UserResponse {
  return {
    id: 1,
    username: 'alice',
    email: 'alice@test.com',
    bio: null,
    avatarUrl: null,
    followersCount: 0,
    followingCount: 0,
    isFollowing: false,
    ...overrides,
  }
}

export function makePost(overrides: Partial<PostResponse> = {}): PostResponse {
  return {
    id: 1,
    content: 'test post content',
    imageUrls: [],
    author: makeAuthor(),
    likeCount: 0,
    commentCount: 0,
    likedByMe: false,
    createdAt: '2025-06-01T12:00:00Z',
    updatedAt: '2025-06-01T12:00:00Z',
    ...overrides,
  }
}

export function makeComment(overrides: Partial<CommentResponse> = {}): CommentResponse {
  return {
    id: 1,
    postId: 1,
    content: 'test comment',
    author: makeAuthor(),
    createdAt: '2025-06-01T12:00:00Z',
    updatedAt: '2025-06-01T12:00:00Z',
    ...overrides,
  }
}
