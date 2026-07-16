<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FileTreeList from '../components/FileTreeList.vue'
import { updateCurrentUserProfile, uploadCurrentUserAvatar } from '../api/auth'
import {
  createSpaceProject,
  deleteSpaceProject,
  deleteSpaceProjectFile,
  downloadSpaceProjectFilesArchive,
  getSpaceProjectFiles,
  getSpaceProjectIssues,
  getSpaceProjects,
  getSpaceUploadLimits,
  moveSpaceProjectFile,
  updateSpaceProject,
  uploadSpaceProjectFile,
  type ProjectForm,
} from '../api/space'
import type { FileView, ProjectIssueView, ProjectView } from '../api/types'
import { canUseSpaceProjectManagement } from '../router/spaceAccess'
import { useSessionStore } from '../stores/session'
import { fileDirectoryOptions, fileDirectoryPath, fileDisplayName } from '../utils/fileTree'
import { prepareSpaceProjectUploadFiles, type SpaceProjectSkippedFile } from '../utils/qfcIgnore'

function emptyProjectForm(): ProjectForm {
  return {
    name: '',
    slug: '',
    description: '',
    visibility: 'PUBLIC',
    sortOrder: 0,
  }
}

function formFromProject(project: ProjectView): ProjectForm {
  return {
    name: project.name,
    slug: project.slug,
    description: project.description || '',
    visibility: project.visibility || 'PUBLIC',
    sortOrder: project.sortOrder || 0,
  }
}

const projects = ref<ProjectView[]>([])
const files = ref<FileView[]>([])
const projectIssues = ref<ProjectIssueView[]>([])
const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const selectedProjectId = ref<number | null>(null)
const loading = ref(true)
const loadingFiles = ref(false)
const loadingIssues = ref(false)
const creating = ref(false)
const savingProject = ref(false)
const uploading = ref(false)
const savingProfile = ref(false)
const uploadingAvatar = ref(false)
const movingFileId = ref<number | null>(null)
const deletingFileId = ref<number | null>(null)
const deletingProjectId = ref<number | null>(null)
const bulkDeletingFiles = ref(false)
const downloadingSelectedFiles = ref(false)
const selectedFileIds = ref<number[]>([])
const showCreateDialog = ref(false)
const movingFile = ref<FileView | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const folderInput = ref<HTMLInputElement | null>(null)
const avatarInput = ref<HTMLInputElement | null>(null)
const message = ref('')
const error = ref('')
const issueError = ref('')
const uploadFailures = ref<UploadFailure[]>([])
const createForm = ref<ProjectForm>(emptyProjectForm())
const editForm = ref<ProjectForm>(emptyProjectForm())
const profileForm = ref({
  displayName: '',
  bio: '',
})

const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value) || null)
const hasProjects = computed(() => projects.value.length > 0)
const uploadDisabled = computed(() => !selectedProjectId.value || uploading.value || bulkDeletingFiles.value)
const avatarInitial = computed(() => (session.displayName || session.username || 'A').slice(0, 1).toUpperCase())
const accountTypeLabel = computed(() => (session.accountType === 'ADMIN' ? '后台管理员' : '网站用户'))
const movingFileName = computed(() => (movingFile.value ? fileDisplayName(movingFile.value) : ''))
const moveDirectoryOptions = computed(() => fileDirectoryOptions(files.value))
const canManageProjects = computed(() => canUseSpaceProjectManagement(session))
const selectedFiles = computed(() => {
  const selectedIds = new Set(selectedFileIds.value)
  return files.value.filter((file) => selectedIds.has(file.id))
})
const selectedFileCount = computed(() => selectedFiles.value.length)
const batchDeleteDisabled = computed(
  () =>
    selectedFileCount.value === 0 ||
    bulkDeletingFiles.value ||
    downloadingSelectedFiles.value ||
    deletingFileId.value !== null ||
    movingFileId.value !== null,
)
const selectedDownloadDisabled = computed(
  () =>
    selectedFileCount.value === 0 ||
    downloadingSelectedFiles.value ||
    bulkDeletingFiles.value ||
    deletingFileId.value !== null ||
    movingFileId.value !== null,
)
type SpaceModule = 'account' | 'projects'
type ProjectPanel = 'list' | 'edit' | 'files'
interface UploadFailure {
  relativePath: string
  reason: string
}
const activeSpaceModule = ref<SpaceModule>('account')
const activeProjectPanel = ref<ProjectPanel>('list')
const moveTargetDirectory = ref('')
const spaceProjectRouteName = computed(() => String(route.name || ''))
let messageTimer: number | undefined

function clearMessage() {
  if (messageTimer !== undefined) {
    window.clearTimeout(messageTimer)
    messageTimer = undefined
  }
  message.value = ''
}

function showSuccessMessage(text: string, autoHide = true) {
  clearMessage()
  message.value = text
  if (autoHide) {
    messageTimer = window.setTimeout(() => {
      message.value = ''
      messageTimer = undefined
    }, 2600)
  }
}

function formatFileSize(bytes: number) {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(1).replace(/\.0$/, '')} MB`
  }
  if (bytes >= 1024) {
    return `${(bytes / 1024).toFixed(1).replace(/\.0$/, '')} KB`
  }
  return `${bytes} B`
}

function skippedUploadFailure(file: SpaceProjectSkippedFile): UploadFailure {
  return {
    relativePath: file.relativePath,
    reason: `超过最大上传限制 ${formatFileSize(file.maxFileSizeBytes)}，文件大小 ${formatFileSize(file.fileSizeBytes)}`,
  }
}

function uploadErrorReason(caught: unknown) {
  return caught instanceof Error ? caught.message : '文件上传失败'
}

function syncProfileForm() {
  profileForm.value = {
    displayName: session.displayName || session.username,
    bio: session.bio || '',
  }
}

function resetCreateForm() {
  createForm.value = emptyProjectForm()
}

function openCreateDialog() {
  clearMessage()
  error.value = ''
  resetCreateForm()
  showCreateDialog.value = true
}

function closeCreateDialog() {
  if (!creating.value) {
    showCreateDialog.value = false
  }
}

function pruneSelectedFiles() {
  const availableIds = new Set(files.value.map((file) => file.id))
  selectedFileIds.value = selectedFileIds.value.filter((fileId) => availableIds.has(fileId))
}

async function loadProjects() {
  loading.value = true
  error.value = ''
  try {
    projects.value = await getSpaceProjects()
    if (selectedProjectId.value && !projects.value.some((project) => project.id === selectedProjectId.value)) {
      returnToProjectList()
    }
    if (selectedProject.value) {
      editForm.value = formFromProject(selectedProject.value)
    } else {
      files.value = []
      projectIssues.value = []
      issueError.value = ''
    }
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '管理数据加载失败'
  } finally {
    loading.value = false
  }
}

async function loadFiles() {
  if (!selectedProjectId.value) {
    files.value = []
    selectedFileIds.value = []
    return
  }
  loadingFiles.value = true
  try {
    files.value = await getSpaceProjectFiles(selectedProjectId.value)
    pruneSelectedFiles()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件列表加载失败'
  } finally {
    loadingFiles.value = false
  }
}

async function loadIssues() {
  if (!selectedProjectId.value) {
    projectIssues.value = []
    issueError.value = ''
    return
  }
  loadingIssues.value = true
  issueError.value = ''
  try {
    projectIssues.value = await getSpaceProjectIssues(selectedProjectId.value)
  } catch (caught) {
    projectIssues.value = []
    issueError.value = '提问记录暂不可用'
  } finally {
    loadingIssues.value = false
  }
}

function setSelectedProject(project: ProjectView) {
  if (selectedProjectId.value !== project.id) {
    selectedFileIds.value = []
  }
  selectedProjectId.value = project.id
  editForm.value = formFromProject(project)
}

function resetProjectSelection() {
  selectedProjectId.value = null
  activeProjectPanel.value = 'list'
  files.value = []
  projectIssues.value = []
  issueError.value = ''
  movingFile.value = null
  moveTargetDirectory.value = ''
  selectedFileIds.value = []
}

function routeProjectId() {
  const value = route.params.projectId
  const projectId = Number(Array.isArray(value) ? value[0] : value)
  return Number.isFinite(projectId) && projectId > 0 ? projectId : null
}

function syncSpaceModuleFromRoute() {
  if (spaceProjectRouteName.value === 'space-account' || !canManageProjects.value) {
    activeSpaceModule.value = 'account'
    return
  }
  activeSpaceModule.value = 'projects'
  if (spaceProjectRouteName.value === 'space-projects') {
    resetProjectSelection()
  }
}

async function syncProjectSelectionFromRoute() {
  if (activeSpaceModule.value !== 'projects' || !canManageProjects.value) {
    return
  }
  const projectId = routeProjectId()
  if (!projectId) {
    return
  }
  const project = projects.value.find((item) => item.id === projectId)
  if (!project) {
    returnToProjectList()
    return
  }
  setSelectedProject(project)
  activeProjectPanel.value = spaceProjectRouteName.value === 'space-project-edit' ? 'edit' : 'files'
  clearMessage()
  error.value = ''
  issueError.value = ''
  if (activeProjectPanel.value === 'files') {
    await Promise.all([loadFiles(), loadIssues()])
  }
}

function openSpaceAccount() {
  router.push({ name: 'space-account' })
}

function openSpaceProjects() {
  router.push({ name: 'space-projects' })
}

function returnToProjectList() {
  resetProjectSelection()
  router.push({ name: 'space-projects' })
}

function visitorPerspectiveRoute(project: ProjectView) {
  return {
    name: 'space-project-visitor',
    params: { projectId: project.id, slug: project.slug },
  }
}

function projectArchiveUrl(project: ProjectView) {
  return `/api/space/projects/${project.id}/download`
}

async function openProjectFiles(project: ProjectView) {
  clearMessage()
  error.value = ''
  issueError.value = ''
  await router.push({ name: 'space-project-files', params: { projectId: project.id } })
}

function openProjectEditor(project: ProjectView) {
  clearMessage()
  error.value = ''
  router.push({ name: 'space-project-edit', params: { projectId: project.id } })
}

function issueAuthor(issue: ProjectIssueView) {
  return issue.authorDisplayName || issue.authorUsername || `用户 ${issue.authorUserId}`
}

function issueStatusLabel(status: string) {
  return status === 'OPEN' ? '待处理' : status
}

function formatIssueTime(value: string) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function submitProject() {
  clearMessage()
  error.value = ''
  creating.value = true
  try {
    await createSpaceProject(createForm.value)
    showSuccessMessage('项目已创建')
    showCreateDialog.value = false
    resetCreateForm()
    returnToProjectList()
    await loadProjects()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '项目创建失败'
  } finally {
    creating.value = false
  }
}

async function saveSelectedProject() {
  if (!selectedProjectId.value) {
    return
  }
  clearMessage()
  error.value = ''
  savingProject.value = true
  try {
    const updated = await updateSpaceProject(selectedProjectId.value, editForm.value)
    projects.value = projects.value.map((project) => (project.id === updated.id ? updated : project))
    editForm.value = formFromProject(updated)
    showSuccessMessage('项目信息已保存')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '项目保存失败'
  } finally {
    savingProject.value = false
  }
}

async function handleDeleteProject(project: ProjectView) {
  if (!window.confirm(`确定隐藏项目 ${project.name}？`)) {
    return
  }

  clearMessage()
  error.value = ''
  deletingProjectId.value = project.id
  try {
    await deleteSpaceProject(project.id)
    projects.value = projects.value.filter((item) => item.id !== project.id)
    if (selectedProjectId.value === project.id) {
      returnToProjectList()
    }
    showSuccessMessage('项目已隐藏')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '项目隐藏失败'
  } finally {
    deletingProjectId.value = null
  }
}

async function saveProfile() {
  clearMessage()
  error.value = ''
  savingProfile.value = true
  try {
    const user = await updateCurrentUserProfile(profileForm.value.displayName, profileForm.value.bio)
    session.setSession(user)
    syncProfileForm()
    showSuccessMessage('账号资料已保存')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '账号资料保存失败'
  } finally {
    savingProfile.value = false
  }
}

function triggerAvatarInput() {
  if (uploadingAvatar.value) {
    error.value = '头像正在上传，请稍候'
    return
  }
  avatarInput.value?.click()
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  clearMessage()
  error.value = ''
  uploadingAvatar.value = true
  try {
    const user = await uploadCurrentUserAvatar(file)
    session.setSession(user)
    showSuccessMessage('头像已更新')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '头像上传失败'
  } finally {
    uploadingAvatar.value = false
    input.value = ''
  }
}

async function uploadSelectedFiles(input: HTMLInputElement, keepFolderPath: boolean) {
  const uploadFiles = Array.from(input.files || [])
  if (!uploadFiles.length || !selectedProjectId.value) {
    return
  }
  clearMessage()
  error.value = ''
  uploadFailures.value = []
  uploading.value = true
  try {
    const uploadLimits = await getSpaceUploadLimits()
    const preparedUpload = await prepareSpaceProjectUploadFiles(uploadFiles, keepFolderPath, uploadLimits.maxFileSizeBytes)
    const failures: UploadFailure[] = preparedUpload.skippedFiles.map(skippedUploadFailure)
    if (preparedUpload.files.length === 0) {
      uploadFailures.value = failures
      if (failures.length > 0) {
        error.value = `没有文件上传成功，${failures.length} 个文件未上传`
      } else {
        showSuccessMessage(
          preparedUpload.ignoredCount > 0
            ? `已忽略 ${preparedUpload.ignoredCount} 个文件，没有需要上传的文件`
            : '没有需要上传的文件',
        )
      }
      return
    }

    let uploadedCount = 0
    for (const file of preparedUpload.files) {
      try {
        await uploadSpaceProjectFile(selectedProjectId.value, file.file, file.relativePath)
        uploadedCount += 1
      } catch (caught) {
        failures.push({ relativePath: file.relativePath, reason: uploadErrorReason(caught) })
        continue
      }
      showSuccessMessage(`正在上传 ${uploadedCount}/${preparedUpload.files.length}`, false)
    }
    const ignoredSuffix = preparedUpload.ignoredCount > 0 ? `，已忽略 ${preparedUpload.ignoredCount} 个` : ''
    const failedSuffix = failures.length > 0 ? `，${failures.length} 个未上传` : ''
    uploadFailures.value = failures
    if (uploadedCount > 0) {
      showSuccessMessage(uploadedCount === 1 ? `已上传 1 个文件${ignoredSuffix}${failedSuffix}` : `已上传 ${uploadedCount} 个文件${ignoredSuffix}${failedSuffix}`)
      await loadFiles()
    } else {
      error.value = `没有文件上传成功，${failures.length} 个文件未上传`
    }
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件上传失败'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  await uploadSelectedFiles(input, false)
}

async function handleFolderChange(event: Event) {
  const input = event.target as HTMLInputElement
  await uploadSelectedFiles(input, true)
}

function triggerFileInput() {
  if (uploadDisabled.value) {
    error.value = selectedProjectId.value ? '文件正在上传，请稍候' : '请先选择项目'
    return
  }
  fileInput.value?.click()
}

function triggerFolderInput() {
  if (uploadDisabled.value) {
    error.value = selectedProjectId.value ? '文件正在上传，请稍候' : '请先选择项目'
    return
  }
  folderInput.value?.click()
}

async function handleDeleteFile(file: FileView) {
  const displayName = fileDisplayName(file)
  if (!window.confirm(`确定删除 ${displayName}？`)) {
    return
  }

  clearMessage()
  error.value = ''
  deletingFileId.value = file.id
  try {
    await deleteSpaceProjectFile(file.id)
    files.value = files.value.filter((item) => item.id !== file.id)
    selectedFileIds.value = selectedFileIds.value.filter((fileId) => fileId !== file.id)
    showSuccessMessage('文件已删除')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件删除失败'
  } finally {
    deletingFileId.value = null
  }
}

async function handleDeleteSelectedFiles() {
  const targets = selectedFiles.value
  if (targets.length === 0) {
    return
  }
  if (!window.confirm(`确定删除选中的 ${targets.length} 个文件？`)) {
    return
  }

  clearMessage()
  error.value = ''
  bulkDeletingFiles.value = true
  const deletedFileIds: number[] = []
  try {
    for (const file of targets) {
      await deleteSpaceProjectFile(file.id)
      deletedFileIds.push(file.id)
    }
    const deletedIdSet = new Set(deletedFileIds)
    files.value = files.value.filter((item) => !deletedIdSet.has(item.id))
    selectedFileIds.value = []
    showSuccessMessage(targets.length === 1 ? '文件已删除' : `已删除 ${targets.length} 个文件`)
  } catch (caught) {
    if (deletedFileIds.length > 0) {
      const deletedIdSet = new Set(deletedFileIds)
      files.value = files.value.filter((item) => !deletedIdSet.has(item.id))
      selectedFileIds.value = selectedFileIds.value.filter((fileId) => !deletedIdSet.has(fileId))
    }
    const reason = caught instanceof Error ? caught.message : '文件删除失败'
    error.value =
      deletedFileIds.length > 0 ? `已删除 ${deletedFileIds.length} 个文件，部分文件删除失败：${reason}` : reason
  } finally {
    bulkDeletingFiles.value = false
  }
}

function saveArchiveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function handleDownloadSelectedFiles() {
  const project = selectedProject.value
  const requestedFileIds = [...selectedFileIds.value]
  const requestedCount = requestedFileIds.length
  if (!project || requestedCount === 0) {
    return
  }

  clearMessage()
  error.value = ''
  downloadingSelectedFiles.value = true
  try {
    const blob = await downloadSpaceProjectFilesArchive(project.id, requestedFileIds)
    saveArchiveBlob(blob, `${project.slug || 'project'}-selected.zip`)
    showSuccessMessage(requestedCount === 1 ? '已开始下载 1 个文件' : `已开始下载 ${requestedCount} 个文件`)
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件下载失败'
  } finally {
    downloadingSelectedFiles.value = false
  }
}

function openMoveFileDialog(file: FileView) {
  movingFile.value = file
  moveTargetDirectory.value = fileDirectoryPath(file)
  clearMessage()
  error.value = ''
}

function closeMoveFileDialog() {
  if (!movingFileId.value) {
    movingFile.value = null
    moveTargetDirectory.value = ''
  }
}

async function submitMoveFile() {
  if (!movingFile.value) {
    return
  }

  const file = movingFile.value
  clearMessage()
  error.value = ''
  movingFileId.value = file.id
  try {
    const moved = await moveSpaceProjectFile(file.id, moveTargetDirectory.value)
    files.value = files.value.map((item) => (item.id === moved.id ? moved : item))
    movingFile.value = null
    moveTargetDirectory.value = ''
    showSuccessMessage(fileDirectoryPath(moved) ? '文件已移动' : '文件已移出到根目录')
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件移动失败'
  } finally {
    movingFileId.value = null
  }
}

onMounted(async () => {
  syncProfileForm()
  syncSpaceModuleFromRoute()
  if (canManageProjects.value) {
    await loadProjects()
    await syncProjectSelectionFromRoute()
  } else {
    loading.value = false
  }
})

watch(
  () => [route.name, route.params.projectId],
  async () => {
    syncSpaceModuleFromRoute()
    await syncProjectSelectionFromRoute()
  },
)
watch(files, pruneSelectedFiles)

onBeforeUnmount(clearMessage)
</script>

<template>
  <section class="page-section space-hero">
    <div>
      <p class="eyebrow">Workspace</p>
      <h1>个人空间</h1>
      <p class="lead">管理个人资料、头像和公开项目内容。</p>
    </div>
    <button
      v-if="canManageProjects && activeSpaceModule === 'projects' && hasProjects && activeProjectPanel === 'list'"
      class="primary-action"
      type="button"
      @click="openCreateDialog"
    >
      创建项目
    </button>
  </section>

  <div v-if="message" class="toast-region" aria-live="polite" aria-atomic="true">
    <p class="toast-message success">
      <span class="toast-icon" aria-hidden="true"></span>
      <span>{{ message }}</span>
    </p>
  </div>
  <p v-if="error" class="status error">{{ error }}</p>
  <section v-if="uploadFailures.length > 0" class="upload-failure-panel" aria-label="上传失败清单">
    <h2>上传失败清单</h2>
    <ul class="upload-failure-list">
      <li v-for="failure in uploadFailures" :key="failure.relativePath">
        <span>{{ failure.relativePath }}</span>
        <small>{{ failure.reason }}</small>
      </li>
    </ul>
  </section>

  <section class="page-section space-module-layout" aria-label="个人空间模块">
    <aside class="space-module-sidebar" aria-label="模块">
      <button
        class="space-module-tab"
        :class="{ active: activeSpaceModule === 'account' }"
        type="button"
        @click="openSpaceAccount"
      >
        <span>账号中心</span>
        <small>头像、昵称、简介</small>
      </button>
      <button
        v-if="canManageProjects"
        class="space-module-tab"
        :class="{ active: activeSpaceModule === 'projects' }"
        type="button"
        @click="openSpaceProjects"
      >
        <span>项目管理</span>
        <small>{{ loading ? '正在加载' : `${projects.length} 个项目` }}</small>
      </button>
    </aside>

    <div class="space-module-main">
      <section v-if="activeSpaceModule === 'account'" class="space-module-content account-profile-section">
        <div class="account-avatar-panel">
          <img
            v-if="session.avatarUrl"
            class="account-avatar"
            :src="session.avatarUrl"
            :alt="session.displayName || session.username"
          />
          <span v-else class="account-avatar placeholder">{{ avatarInitial }}</span>
          <div>
            <h2>{{ session.displayName || session.username }}</h2>
            <p>{{ accountTypeLabel }} · {{ session.username }}</p>
          </div>
          <button class="secondary-action" type="button" :disabled="uploadingAvatar" @click="triggerAvatarInput">
            {{ uploadingAvatar ? '上传中' : '上传头像' }}
          </button>
          <input
            ref="avatarInput"
            class="visually-hidden"
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            :disabled="uploadingAvatar"
            @change="handleAvatarChange"
          />
        </div>

        <form class="stack-form account-profile-form" @submit.prevent="saveProfile">
          <label>
            <span>昵称</span>
            <input v-model="profileForm.displayName" required maxlength="100" />
            <small class="field-hint">昵称会显示在个人中心和公开内容中，登录仍使用账号。</small>
          </label>
          <label>
            <span>简介</span>
            <textarea v-model="profileForm.bio" rows="4" maxlength="500"></textarea>
          </label>
          <div class="form-actions">
            <button type="submit" :disabled="savingProfile">{{ savingProfile ? '保存中' : '保存资料' }}</button>
          </div>
        </form>
      </section>

      <template v-else-if="activeSpaceModule === 'projects'">
        <p v-if="loading" class="status">正在加载项目管理。</p>

        <section v-else-if="!hasProjects" class="empty-workspace">
          <div class="empty-workspace-inner">
            <p class="eyebrow">No Projects</p>
            <h2>还没有项目</h2>
            <p>先创建一个项目，再上传 Markdown、图片、PDF 或 Excel 文件。</p>
            <button class="primary-action" type="button" @click="openCreateDialog">创建第一个项目</button>
            <ol class="empty-guide">
              <li>填写项目名和 slug。</li>
              <li>进入项目后上传文件或整个文件夹。</li>
              <li>用访客视角检查同事看到的项目页。</li>
            </ol>
          </div>
        </section>

        <section v-else-if="activeProjectPanel === 'list'" class="project-list-page" aria-label="项目列表">
          <div class="project-list-panel">
            <div class="section-heading compact-heading">
              <div>
                <h2>项目列表</h2>
                <p>{{ projects.length }} 个项目</p>
              </div>
            </div>
            <div class="space-project-list">
              <article v-for="project in projects" :key="project.id" class="space-project-item">
                <button class="project-list-main" type="button" @click="openProjectFiles(project)">
                  <span>{{ project.name }}</span>
                  <small>/p/{{ project.slug }}</small>
                </button>
                <div class="project-list-actions">
                  <button class="secondary-button" type="button" @click.stop="openProjectEditor(project)">编辑</button>
                  <button
                    class="danger-button"
                    type="button"
                    :disabled="deletingProjectId === project.id"
                    @click.stop="handleDeleteProject(project)"
                  >
                    {{ deletingProjectId === project.id ? '隐藏中' : '删除' }}
                  </button>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section v-else-if="activeProjectPanel === 'edit' && selectedProject" class="project-detail" aria-label="编辑项目">
          <section class="project-detail-section">
            <div class="section-heading">
              <div>
                <h2>{{ selectedProject.name }}</h2>
                <p>/p/{{ selectedProject.slug }}</p>
              </div>
              <div class="detail-heading-actions">
                <button class="secondary-button" type="button" @click="returnToProjectList">返回项目列表</button>
                <RouterLink class="secondary-action" :to="visitorPerspectiveRoute(selectedProject)">访客视角</RouterLink>
              </div>
            </div>

            <form class="stack-form project-edit-form" @submit.prevent="saveSelectedProject">
              <label>
                <span>项目名</span>
                <input v-model="editForm.name" required />
              </label>
              <label class="access-path-field">
                <span>访问路径</span>
                <div class="access-path-control">
                  <span class="access-path-prefix">/p/</span>
                  <input v-model="editForm.slug" required placeholder="project-path" />
                </div>
                <small class="field-hint">
                  公开项目地址：
                  <span class="access-path-preview">/p/{{ editForm.slug || 'project-path' }}</span>
                </small>
              </label>
              <label class="full-field">
                <span>简介</span>
                <textarea v-model="editForm.description" rows="4"></textarea>
              </label>
              <label>
                <span>排序</span>
                <input v-model.number="editForm.sortOrder" type="number" />
              </label>
              <div class="form-actions full-field">
                <button type="submit" :disabled="savingProject">{{ savingProject ? '保存中' : '保存项目' }}</button>
              </div>
            </form>
          </section>
        </section>

        <section v-else-if="activeProjectPanel === 'files' && selectedProject" class="project-detail" aria-label="项目文件">
          <section class="project-detail-section">
            <div class="section-heading">
              <div>
                <h2>{{ selectedProject.name }}</h2>
                <p>/p/{{ selectedProject.slug }}</p>
              </div>
              <div class="detail-heading-actions">
                <button class="secondary-button" type="button" @click="returnToProjectList">返回项目列表</button>
                <RouterLink class="secondary-action" :to="visitorPerspectiveRoute(selectedProject)">访客视角</RouterLink>
              </div>
            </div>
          </section>

          <section class="project-detail-section">
            <div class="section-heading">
              <div>
                <h2>文件</h2>
                <p>{{ loadingFiles ? '正在加载' : `${files.length} 个文件` }}</p>
              </div>
              <div class="file-management-actions">
                <div v-if="files.length > 0" class="bulk-file-actions" aria-label="批量文件操作">
                  <span v-if="selectedFileCount > 0">已选 {{ selectedFileCount }} 个</span>
                  <button
                    class="secondary-button bulk-download-button"
                    type="button"
                    :disabled="selectedDownloadDisabled"
                    @click="handleDownloadSelectedFiles"
                  >
                    {{ downloadingSelectedFiles ? '下载中' : '批量下载' }}
                  </button>
                  <button
                    class="danger-button bulk-delete-button"
                    type="button"
                    :disabled="batchDeleteDisabled"
                    @click="handleDeleteSelectedFiles"
                  >
                    {{ bulkDeletingFiles ? '删除中' : '批量删除' }}
                  </button>
                </div>
                <div class="upload-actions">
                  <a v-if="files.length > 0" class="secondary-action" :href="projectArchiveUrl(selectedProject)">下载整个项目</a>
                  <button type="button" :disabled="uploadDisabled" @click="triggerFileInput">上传文件</button>
                  <button type="button" :disabled="uploadDisabled" @click="triggerFolderInput">上传文件夹</button>
                </div>
              </div>
            </div>

            <input
              ref="fileInput"
              class="visually-hidden"
              :disabled="uploadDisabled"
              type="file"
              multiple
              @change="handleFileChange"
            />
            <input
              ref="folderInput"
              class="visually-hidden"
              :disabled="uploadDisabled"
              type="file"
              webkitdirectory
              multiple
              @change="handleFolderChange"
            />

            <p v-if="!loadingFiles && files.length === 0" class="status">这个项目还没有文件。</p>
            <FileTreeList
              v-else-if="files.length > 0"
              :files="files"
              :project-slug="selectedProject.slug"
              preview-route-name="space-project-file-preview"
              :preview-route-params="{ projectId: selectedProject.id }"
              v-model:selected-file-ids="selectedFileIds"
              selectable
              can-move
              can-delete
              :actions-disabled="bulkDeletingFiles"
              :selection-disabled="bulkDeletingFiles || downloadingSelectedFiles"
              :moving-file-id="movingFileId"
              :deleting-file-id="deletingFileId"
              @move="openMoveFileDialog"
              @delete="handleDeleteFile"
            />
          </section>

          <section class="project-detail-section issue-list-section">
            <div class="section-heading">
              <div>
                <h2>提问记录</h2>
                <p>{{ loadingIssues ? '正在加载' : `${projectIssues.length} 个问题` }}</p>
              </div>
            </div>

            <p v-if="loadingIssues" class="status">正在加载提问记录。</p>
            <p v-else-if="issueError" class="status error">{{ issueError }}</p>
            <p v-else-if="projectIssues.length === 0" class="status">还没有用户向这个项目提问。</p>
            <div v-else class="issue-list">
              <article v-for="issue in projectIssues" :key="issue.id" class="issue-list-item">
                <div class="issue-list-main">
                  <div>
                    <strong>{{ issue.title }}</strong>
                    <p>{{ issue.content }}</p>
                  </div>
                  <span class="issue-status">{{ issueStatusLabel(issue.status) }}</span>
                </div>
                <footer>
                  <span>{{ issueAuthor(issue) }}</span>
                  <span>{{ formatIssueTime(issue.createdAt) }}</span>
                </footer>
              </article>
            </div>
          </section>
        </section>

        <section v-else class="project-empty-detail">
          <h2>项目不可用</h2>
          <button class="secondary-button" type="button" @click="returnToProjectList">返回项目列表</button>
        </section>
      </template>
    </div>
  </section>

  <div v-if="showCreateDialog" class="modal-backdrop" @click.self="closeCreateDialog">
    <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="create-project-title">
      <div class="modal-heading">
        <h2 id="create-project-title">创建项目</h2>
        <button class="icon-button" type="button" aria-label="关闭" :disabled="creating" @click="closeCreateDialog">
          x
        </button>
      </div>
      <form class="stack-form" @submit.prevent="submitProject">
        <label>
          <span>项目名</span>
          <input v-model="createForm.name" required autofocus />
        </label>
        <label class="access-path-field">
          <span>访问路径</span>
          <div class="access-path-control">
            <span class="access-path-prefix">/p/</span>
            <input v-model="createForm.slug" required placeholder="project-path" />
          </div>
          <small class="field-hint">
            创建后公开项目地址：
            <span class="access-path-preview">/p/{{ createForm.slug || 'project-path' }}</span>
          </small>
        </label>
        <label>
          <span>简介</span>
          <textarea v-model="createForm.description" rows="4"></textarea>
        </label>
        <div class="form-actions">
          <button class="secondary-button" type="button" :disabled="creating" @click="closeCreateDialog">取消</button>
          <button type="submit" :disabled="creating">{{ creating ? '创建中' : '创建' }}</button>
        </div>
      </form>
    </section>
  </div>

  <div v-if="movingFile" class="modal-backdrop" @click.self="closeMoveFileDialog">
    <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="move-file-title">
      <div class="modal-heading">
        <h2 id="move-file-title">移动文件</h2>
        <button class="icon-button" type="button" aria-label="关闭" :disabled="movingFileId !== null" @click="closeMoveFileDialog">
          x
        </button>
      </div>
      <form class="stack-form" @submit.prevent="submitMoveFile">
        <label>
          <span>当前路径</span>
          <input :value="movingFileName" disabled />
        </label>
        <label>
          <span>目标目录</span>
          <select v-model="moveTargetDirectory" autofocus>
            <option v-for="option in moveDirectoryOptions" :key="option.value || 'root'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <small class="field-hint">选择项目根目录表示移出目录；保存后文件名保持不变。</small>
        </label>
        <div class="form-actions">
          <button class="secondary-button" type="button" :disabled="movingFileId !== null" @click="closeMoveFileDialog">取消</button>
          <button type="submit" :disabled="movingFileId !== null">{{ movingFileId !== null ? '移动中' : '保存移动' }}</button>
        </div>
      </form>
    </section>
  </div>
</template>
