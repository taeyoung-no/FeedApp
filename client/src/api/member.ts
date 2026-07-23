import { apiClient } from './client'

export type SignupRequest = {
  username: string
  password: string
}

export type MemberResponse = {
  id: number
  username: string
}

export async function signup(data: SignupRequest): Promise<MemberResponse> {
  const response = await apiClient.post<MemberResponse>('/members/signup', data)
  return response.data
}
