import { http } from './http'
import type { ApiResponse, FilePreview, FileView, ProjectIssueView, ProjectView, SiteFeedbackView } from './types'
import { unwrap } from './types'

export interface SiteInfo {
  name: string
  englishName: string
  shortName: string
  description: string
}

export async function getSite(): Promise<SiteInfo> {
  const response = await http.get<ApiResponse<SiteInfo>>('/public/site')
  return unwrap(response.data)
}

export async function getProjects(): Promise<ProjectView[]> {
  const response = await http.get<ApiResponse<ProjectView[]>>('/public/projects')
  return unwrap(response.data)
}

export async function getProject(slug: string): Promise<ProjectView> {
  const response = await http.get<ApiResponse<ProjectView>>(`/public/projects/${slug}`)
  return unwrap(response.data)
}

export async function getProjectFiles(slug: string): Promise<FileView[]> {
  const response = await http.get<ApiResponse<FileView[]>>(`/public/projects/${slug}/files`)
  return unwrap(response.data)
}

export async function downloadPublicProjectFilesArchive(slug: string, fileIds: number[]): Promise<Blob> {
  const response = await http.post<Blob>(
    `/public/projects/${encodeURIComponent(slug)}/files/archive`,
    { fileIds },
    { responseType: 'blob', timeout: 0 },
  )
  return response.data
}

export async function createProjectIssue(slug: string, title: string, content: string): Promise<ProjectIssueView> {
  const response = await http.post<ApiResponse<ProjectIssueView>>(`/public/projects/${slug}/issues`, { title, content })
  return unwrap(response.data)
}

export async function createSiteFeedback(title: string, content: string): Promise<SiteFeedbackView> {
  const response = await http.post<ApiResponse<SiteFeedbackView>>('/public/feedback', { title, content })
  return unwrap(response.data)
}

export async function getFilePreview(fileId: string): Promise<FilePreview> {
  const response = await http.get<ApiResponse<FilePreview>>(`/public/files/${fileId}/preview`)
  return unwrap(response.data)
}
