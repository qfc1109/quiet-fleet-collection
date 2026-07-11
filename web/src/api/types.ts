export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export class ApiError extends Error {
  code: string
  status: number

  constructor(code: string, message: string, status = 0) {
    super(message || code)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

export interface LoginUser {
  id: number
  username: string
  displayName: string
  bio: string
  avatarUrl: string
  accountType: string
  status: string
  roleCodes: string[]
  permissionCodes: string[]
}

export interface ProjectView {
  id: number
  ownerUserId: number | null
  name: string
  slug: string
  description: string
  coverUrl: string
  visibility: string
  sortOrder: number
  createdAt: string
  updatedAt: string
  deletedAt: string | null
  deletedByUserId: number | null
}

export interface ProjectIssueView {
  id: number
  projectId: number
  authorUserId: number
  authorUsername: string
  authorDisplayName: string
  title: string
  content: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface SiteFeedbackView {
  id: number
  authorUserId: number
  authorUsername: string
  authorDisplayName: string
  title: string
  content: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface FileView {
  id: number
  projectId: number
  originalName: string
  fileExt: string
  mimeType: string
  fileSize: number
  relativePath: string
  previewType: string
  downloadUrl: string
  previewUrl: string
  createdAt: string
  updatedAt: string
}

export interface ExcelPreview {
  rows: string[][]
}

export interface FilePreview {
  fileId: number
  originalName: string
  relativePath: string
  previewType: string
  content?: string
  excel?: ExcelPreview
  downloadUrl: string
  streamUrl: string
}

export interface UserAccountView {
  id: number
  username: string
  displayName: string
  bio: string
  avatarUrl: string
  accountType: string
  status: string
  roleCodes: string[]
  createdAt: string
  updatedAt: string
}

export interface PermissionView {
  id: number
  code: string
  name: string
  module: string
  description: string
}

export interface AdminRoleView {
  id: number
  code: string
  name: string
  description: string
  builtIn: boolean
  permissionCodes: string[]
}

export function unwrap<T>(response: ApiResponse<T>): T {
  if (response.code !== 'SUCCESS') {
    throw new ApiError(response.code, response.message || response.code)
  }
  return response.data
}
