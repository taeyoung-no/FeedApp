import { apiClient } from './client'
import type { components } from './generated/schema'

export type CommentResponse = components['schemas']['CommentResponse']
export type CreateCommentRequest = components['schemas']['CreateCommentRequest']
export type UpdateCommentRequest = { content: string }

export async function getComments(postId: string): Promise<CommentResponse[]> {
  const response = await apiClient.get<CommentResponse[]>(`/posts/${postId}/comments`)
  return response.data
}

export async function createComment(postId: string, data: CreateCommentRequest): Promise<CommentResponse> {
  const response = await apiClient.post<CommentResponse>(`/posts/${postId}/comments`, data)
  return response.data
}

export async function updateComment(id: number, data: UpdateCommentRequest): Promise<CommentResponse> {
  const response = await apiClient.put<CommentResponse>(`/comments/${id}`, data)
  return response.data
}

export async function deleteComment(id: number): Promise<void> {
  await apiClient.delete(`/comments/${id}`)
}
