import { apiClient } from './client'
import type { components } from './generated/schema'

export type SignupRequest = components['schemas']['SignupRequest']
export type MemberResponse = components['schemas']['MemberResponse']
export type LoginRequest = components['schemas']['LoginRequest']
export type LoginResponse = components['schemas']['LoginResponse']

export async function signup(data: SignupRequest): Promise<MemberResponse> {
  const response = await apiClient.post<MemberResponse>('/members/signup', data)
  return response.data
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/members/login', data)
  return response.data
}

export async function getMe(): Promise<LoginResponse> {
  const response = await apiClient.get<LoginResponse>('/members/me')
  return response.data
}

export async function logout(): Promise<void> {
  await apiClient.post('/members/logout')
}
