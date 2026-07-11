import { http } from './http'
import type { ApiResponse, LoginUser } from './types'
import { unwrap } from './types'

export async function login(username: string, password: string): Promise<LoginUser> {
  const response = await http.post<ApiResponse<LoginUser>>('/auth/admin/login', { username, password })
  return unwrap(response.data)
}

export async function logout(): Promise<boolean> {
  const response = await http.post<ApiResponse<boolean>>('/auth/admin/logout')
  return unwrap(response.data)
}

export async function getCurrentUser(): Promise<LoginUser> {
  const response = await http.get<ApiResponse<LoginUser>>('/auth/admin/me')
  return unwrap(response.data)
}
