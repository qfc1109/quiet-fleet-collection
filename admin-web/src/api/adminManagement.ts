import { http } from './http'
import type { AdminRoleView, ApiResponse, PermissionView, SiteFeedbackView, UserAccountView } from './types'
import { unwrap } from './types'

export interface CreateUserForm {
  username: string
  password: string
  displayName: string
  bio: string
  avatarUrl: string
}

export interface UpdateUserForm {
  displayName: string
  bio: string
  avatarUrl: string
  status: string
}

export interface CreateAdminUserForm extends CreateUserForm {
  roleCodes: string[]
}

export interface UpdateAdminUserForm extends UpdateUserForm {
  roleCodes: string[]
}

export interface UpdateRoleForm {
  name: string
  description: string
  permissionCodes: string[]
}

export async function getSiteUsers(): Promise<UserAccountView[]> {
  const response = await http.get<ApiResponse<UserAccountView[]>>('/admin/site-users')
  return unwrap(response.data)
}

export async function createSiteUser(form: CreateUserForm): Promise<UserAccountView> {
  const response = await http.post<ApiResponse<UserAccountView>>('/admin/site-users', form)
  return unwrap(response.data)
}

export async function updateSiteUser(userId: number, form: UpdateUserForm): Promise<UserAccountView> {
  const response = await http.put<ApiResponse<UserAccountView>>(`/admin/site-users/${userId}`, form)
  return unwrap(response.data)
}

export async function resetSiteUserPassword(userId: number, password: string): Promise<UserAccountView> {
  const response = await http.post<ApiResponse<UserAccountView>>(`/admin/site-users/${userId}/reset-password`, { password })
  return unwrap(response.data)
}

export async function getAdminUsers(): Promise<UserAccountView[]> {
  const response = await http.get<ApiResponse<UserAccountView[]>>('/admin/admin-users')
  return unwrap(response.data)
}

export async function createAdminUser(form: CreateAdminUserForm): Promise<UserAccountView> {
  const response = await http.post<ApiResponse<UserAccountView>>('/admin/admin-users', form)
  return unwrap(response.data)
}

export async function updateAdminUser(userId: number, form: UpdateAdminUserForm): Promise<UserAccountView> {
  const response = await http.put<ApiResponse<UserAccountView>>(`/admin/admin-users/${userId}`, form)
  return unwrap(response.data)
}

export async function resetAdminUserPassword(userId: number, password: string): Promise<UserAccountView> {
  const response = await http.post<ApiResponse<UserAccountView>>(`/admin/admin-users/${userId}/reset-password`, { password })
  return unwrap(response.data)
}

export async function getRoles(): Promise<AdminRoleView[]> {
  const response = await http.get<ApiResponse<AdminRoleView[]>>('/admin/roles')
  return unwrap(response.data)
}

export async function updateRole(roleId: number, form: UpdateRoleForm): Promise<AdminRoleView> {
  const response = await http.put<ApiResponse<AdminRoleView>>(`/admin/roles/${roleId}`, form)
  return unwrap(response.data)
}

export async function getPermissions(): Promise<PermissionView[]> {
  const response = await http.get<ApiResponse<PermissionView[]>>('/admin/permissions')
  return unwrap(response.data)
}

export async function getSiteFeedback(): Promise<SiteFeedbackView[]> {
  const response = await http.get<ApiResponse<SiteFeedbackView[]>>('/admin/feedback')
  return unwrap(response.data)
}
