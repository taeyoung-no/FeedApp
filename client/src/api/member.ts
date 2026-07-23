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

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  id: number
  username: string
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/members/login', data)
  return response.data
}

