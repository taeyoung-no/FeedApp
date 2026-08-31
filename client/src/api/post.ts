import { apiClient } from './client'
import type { components } from './generated/schema'

export type PostResponse = components['schemas']['PostResponse']
export type CreatePostRequest = components['schemas']['CreatePostRequest']
export type UpdatePostRequest = components['schemas']['UpdatePostRequest']

export async function getPosts(): Promise<PostResponse[]> {
  const response = await apiClient.get<PostResponse[]>('/posts')
  return response.data
}

export async function getPost(id: string): Promise<PostResponse> {
  const response = await apiClient.get<PostResponse>(`/posts/${id}`)
  return response.data
}

export async function createPost(data: CreatePostRequest): Promise<PostResponse> {
  const response = await apiClient.post<PostResponse>('/posts', data)
  return response.data
}

export async function updatePost(id: string, data: UpdatePostRequest): Promise<PostResponse> {
  const response = await apiClient.put<PostResponse>(`/posts/${id}`, data)
  return response.data
}

export async function deletePost(id: string): Promise<void> {
  await apiClient.delete(`/posts/${id}`)
}

export async function likePost(id: string): Promise<void> {
  await apiClient.post(`/posts/${id}/likes`)
}

export async function unlikePost(id: string): Promise<void> {
  await apiClient.delete(`/posts/${id}/likes`)
}
