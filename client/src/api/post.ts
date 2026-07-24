import { apiClient } from './client'

export type PostResponse = {
  id: number
  title: string
  content: string
  author: string
  createdAt: string
}

export type CreatePostRequest = {
  title: string
  content: string
}

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
