import { apiClient } from './client'
import type { components } from './generated/schema'

export type PostResponse = components['schemas']['PostResponse']
export type CreatePostRequest = components['schemas']['CreatePostRequest']

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
