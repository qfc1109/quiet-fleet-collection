import { clearAuthTokens, http, setAuthTokens } from './http'
import type { ApiResponse, LoginUser } from './types'
import { unwrap } from './types'

export interface LoginResult {
  user: LoginUser
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface AuthDevice {
  deviceId: string
  clientIp: string
  userAgent: string
  current: boolean
}

export async function login(username: string, password: string): Promise<LoginUser> {
  const result = await tokenLogin(username, password)
  setAuthTokens(result)
  return result.user
}

export async function tokenLogin(username: string, password: string): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/token', { username, password })
  return unwrap(response.data)
}

export async function register(username: string, password: string, displayName: string): Promise<LoginUser> {
  const response = await http.post<ApiResponse<LoginUser>>('/auth/register', { username, password, displayName })
  return unwrap(response.data)
}

export async function logout(): Promise<boolean> {
  try {
    await http.post<ApiResponse<void>>('/auth/revoke')
  } finally {
    clearAuthTokens()
  }
  return true
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

export async function listAuthSessions(): Promise<AuthDevice[]> {
  const response = await http.get<ApiResponse<AuthDevice[]>>('/auth/sessions')
  return unwrap(response.data)
}

export async function revokeOtherSessions(): Promise<void> {
  const response = await http.post<ApiResponse<void>>('/auth/sessions/revoke-others')
  unwrap(response.data)
}
