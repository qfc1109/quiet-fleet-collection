import { http } from './http'
import type { ApiResponse, LoginUser } from './types'
import { unwrap } from './types'

export async function login(username: string, password: string): Promise<LoginUser> {
  const response = await http.post<ApiResponse<LoginUser>>('/auth/login', { username, password })
  return unwrap(response.data)
}

export async function register(username: string, password: string, displayName: string): Promise<LoginUser> {
  const response = await http.post<ApiResponse<LoginUser>>('/auth/register', { username, password, displayName })
  return unwrap(response.data)
}

export async function logout(): Promise<boolean> {
  const response = await http.post<ApiResponse<boolean>>('/auth/logout')
  return unwrap(response.data)
}

export async function getCurrentUser(): Promise<LoginUser> {
  const response = await http.get<ApiResponse<LoginUser>>('/auth/me')
  return unwrap(response.data)
}

export async function updateCurrentUserProfile(displayName: string, bio: string): Promise<LoginUser> {
  const response = await http.put<ApiResponse<LoginUser>>('/auth/profile', { displayName, bio })
  return unwrap(response.data)
}

export async function uploadCurrentUserAvatar(file: File): Promise<LoginUser> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post<ApiResponse<LoginUser>>('/auth/avatar', formData)
  return unwrap(response.data)
}
