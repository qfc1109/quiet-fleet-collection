import { http } from './http'
import type { ApiResponse, FileView, ProjectIssueView, ProjectView, SpaceUploadLimitView } from './types'
import { unwrap } from './types'

export interface ProjectForm {
  name: string
  slug: string
  description: string
  visibility: string
  sortOrder: number
}

export async function getSpaceProjects(): Promise<ProjectView[]> {
  const response = await http.get<ApiResponse<ProjectView[]>>('/space/projects')
  return unwrap(response.data)
}

export async function createSpaceProject(form: ProjectForm): Promise<ProjectView> {
  const response = await http.post<ApiResponse<ProjectView>>('/space/projects', form)
  return unwrap(response.data)
}

export async function updateSpaceProject(projectId: number, form: ProjectForm): Promise<ProjectView> {
  const response = await http.put<ApiResponse<ProjectView>>(`/space/projects/${projectId}`, form)
  return unwrap(response.data)
}

export async function deleteSpaceProject(projectId: number): Promise<boolean> {
  const response = await http.delete<ApiResponse<boolean>>(`/space/projects/${projectId}`)
  return unwrap(response.data)
}

export async function getSpaceProjectFiles(projectId: number): Promise<FileView[]> {
  const response = await http.get<ApiResponse<FileView[]>>(`/space/projects/${projectId}/files`)
  return unwrap(response.data)
}

export async function getSpaceProjectIssues(projectId: number): Promise<ProjectIssueView[]> {
  const response = await http.get<ApiResponse<ProjectIssueView[]>>(`/space/projects/${projectId}/issues`)
  return unwrap(response.data)
}

export async function getSpaceUploadLimits(): Promise<SpaceUploadLimitView> {
  const response = await http.get<ApiResponse<SpaceUploadLimitView>>('/space/upload-limits')
  return unwrap(response.data)
}

export async function downloadSpaceProjectFilesArchive(projectId: number, fileIds: number[]): Promise<Blob> {
  const response = await http.post<Blob>(
    `/space/projects/${projectId}/files/archive`,
    { fileIds },
    { responseType: 'blob', timeout: 0 },
  )
  return response.data
}

export async function uploadSpaceProjectFile(projectId: number, file: File, relativePath?: string): Promise<FileView> {
  const formData = new FormData()
  formData.append('file', file)
  if (relativePath) {
    formData.append('relativePath', relativePath)
  }
  const response = await http.post<ApiResponse<FileView>>(`/space/projects/${projectId}/files`, formData)
  return unwrap(response.data)
}

export async function moveSpaceProjectFile(fileId: number, targetDirectory: string): Promise<FileView> {
  const response = await http.put<ApiResponse<FileView>>(`/space/files/${fileId}/path`, { targetDirectory })
  return unwrap(response.data)
}

export async function deleteSpaceProjectFile(fileId: number): Promise<boolean> {
  const response = await http.delete<ApiResponse<boolean>>(`/space/files/${fileId}`)
  return unwrap(response.data)
}
