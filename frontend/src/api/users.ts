import client from './client'
import type { UserResponse, FollowUserItem } from '../types/user'
import type { PagedResponse, PostResponse } from '../types/post'
import type { CommentResponse } from '../types/post'

export function getUser(id: number): Promise<UserResponse> {
  return client.get(`/users/${id}`).then((r) => r.data)
}

export function updateUser(
  id: number,
  data: { username?: string; bio?: string; avatarUrl?: string | null },
): Promise<UserResponse> {
  return client.patch(`/users/${id}`, data).then((r) => r.data)
}

export function getUserPosts(id: number): Promise<PostResponse[]> {
  return client.get(`/users/${id}/posts`).then((r) => r.data)
}

export function getUserLikedPosts(id: number): Promise<PostResponse[]> {
  return client.get(`/users/${id}/liked-posts`).then((r) => r.data)
}

export function getUserComments(id: number): Promise<CommentResponse[]> {
  return client.get(`/users/${id}/comments`).then((r) => r.data)
}

export function followUser(id: number): Promise<void> {
  return client.post(`/users/${id}/follow`).then(() => undefined)
}

export function unfollowUser(id: number): Promise<void> {
  return client.delete(`/users/${id}/follow`).then(() => undefined)
}

export function getFollowers(id: number): Promise<FollowUserItem[]> {
  return client.get(`/users/${id}/followers`).then((r) => r.data)
}

export function getFollowing(id: number): Promise<FollowUserItem[]> {
  return client.get(`/users/${id}/following`).then((r) => r.data)
}

export function searchUsers(q: string): Promise<UserResponse[]> {
  return client.get('/users', { params: { q } }).then((r) => r.data)
}

export function uploadImage(file: File, type: 'avatars' | 'posts' = 'posts'): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return client
    .post('/upload', formData, { headers: { 'Content-Type': undefined } })
    .then((r) => r.data)
}
