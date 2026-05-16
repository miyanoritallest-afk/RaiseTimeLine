import { http, HttpResponse } from 'msw'

const BASE = '/api'

export const handlers = [
  http.post(`${BASE}/auth/login`, () =>
    HttpResponse.json({
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 900,
      user: { id: 1, username: 'alice', email: 'alice@test.com', bio: null, avatarUrl: null },
    })
  ),
  http.post(`${BASE}/auth/register`, () =>
    HttpResponse.json({
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 900,
      user: { id: 1, username: 'alice', email: 'alice@test.com', bio: null, avatarUrl: null },
    }, { status: 201 })
  ),
  http.post(`${BASE}/auth/logout`, () => new HttpResponse(null, { status: 204 })),
  http.get(`${BASE}/posts`, () =>
    HttpResponse.json({ items: [], nextCursor: null, hasMore: false })
  ),
  http.post(`${BASE}/posts`, () =>
    HttpResponse.json({
      id: 1, content: 'test post', imageUrls: [],
      author: { id: 1, username: 'alice', email: 'alice@test.com', bio: null, avatarUrl: null, followersCount: 0, followingCount: 0, isFollowing: false },
      likeCount: 0, commentCount: 0, likedByMe: false,
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    }, { status: 201 })
  ),
  http.post(`${BASE}/posts/:postId/likes`, ({ params }) =>
    HttpResponse.json({ postId: Number(params.postId), likeCount: 1, likedByMe: true })
  ),
  http.delete(`${BASE}/posts/:postId/likes`, ({ params }) =>
    HttpResponse.json({ postId: Number(params.postId), likeCount: 0, likedByMe: false })
  ),
  http.post(`${BASE}/users/:id/follow`, ({ params }) =>
    HttpResponse.json({ userId: Number(params.id), followersCount: 1, followingCount: 0, isFollowing: true }, { status: 201 })
  ),
  http.delete(`${BASE}/users/:id/follow`, ({ params }) =>
    HttpResponse.json({ userId: Number(params.id), followersCount: 0, followingCount: 0, isFollowing: false })
  ),
  http.get(`${BASE}/posts/:postId/comments`, () => HttpResponse.json([])),
  http.post(`${BASE}/posts/:postId/comments`, () =>
    HttpResponse.json({
      id: 1, postId: 1, content: 'test comment',
      author: { id: 1, username: 'alice', email: 'alice@test.com', bio: null, avatarUrl: null, followersCount: 0, followingCount: 0, isFollowing: false },
      createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
    }, { status: 201 })
  ),
]
