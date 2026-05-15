import client from './client'
import type { PostResponse, PagedResponse, CommentResponse } from '../types/post'

export function getTimeline(
  feed: 'all' | 'following',
  cursor?: number,
): Promise<PagedResponse<PostResponse>> {
  const params: Record<string, string | number> = { feed }
  if (cursor != null) params.cursor = cursor
  return client.get('/posts', { params }).then((r) => r.data)
}

export function getPost(id: number): Promise<PostResponse> {
  return client.get(`/posts/${id}`).then((r) => r.data)
}

export function createPost(content: string, imageUrls?: string[]): Promise<PostResponse> {
  return client.post('/posts', { content, imageUrls }).then((r) => r.data)
}

export function updatePost(id: number, content: string): Promise<PostResponse> {
  return client.patch(`/posts/${id}`, { content }).then((r) => r.data)
}

export function deletePost(id: number): Promise<void> {
  return client.delete(`/posts/${id}`).then(() => undefined)
}

export function toggleLike(id: number, liked: boolean): Promise<void> {
  const req = liked
    ? client.delete(`/posts/${id}/likes`)
    : client.post(`/posts/${id}/likes`)
  return req.then(() => undefined)
}

export function getComments(postId: number): Promise<CommentResponse[]> {
  return client.get(`/posts/${postId}/comments`).then((r) => r.data)
}

export function createComment(postId: number, content: string): Promise<CommentResponse> {
  return client.post(`/posts/${postId}/comments`, { content }).then((r) => r.data)
}

export function updateComment(commentId: number, content: string): Promise<CommentResponse> {
  return client.patch(`/comments/${commentId}`, { content }).then((r) => r.data)
}

export function deleteComment(commentId: number): Promise<void> {
  return client.delete(`/comments/${commentId}`).then(() => undefined)
}
