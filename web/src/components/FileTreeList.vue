<script setup lang="ts">
import { computed, ref } from 'vue'
import type { FileView } from '../api/types'
import { buildFileTreeRows, fileTreeDirectoryPaths, visibleFileTreeRows } from '../utils/fileTree'

const props = withDefaults(
  defineProps<{
    files: FileView[]
    projectSlug?: string
    canMove?: boolean
    canDelete?: boolean
    movingFileId?: number | null
    deletingFileId?: number | null
    previewRouteName?: string
    previewRouteParams?: Record<string, string | number>
    selectable?: boolean
    selectedFileIds?: number[]
    selectionDisabled?: boolean
    actionsDisabled?: boolean
  }>(),
  {
    projectSlug: '',
    canMove: false,
    canDelete: false,
    movingFileId: null,
    deletingFileId: null,
    previewRouteName: 'file-preview',
    previewRouteParams: () => ({}),
    selectable: false,
    selectedFileIds: () => [],
    selectionDisabled: false,
    actionsDisabled: false,
  },
)

const emit = defineEmits<{
  move: [file: FileView]
  delete: [file: FileView]
  'update:selectedFileIds': [fileIds: number[]]
}>()

const collapsedDirectoryPaths = ref(new Set<string>())
const allRows = computed(() => buildFileTreeRows(props.files))
const directoryPaths = computed(() => fileTreeDirectoryPaths(allRows.value))
const rows = computed(() => visibleFileTreeRows(allRows.value, collapsedDirectoryPaths.value))
const selectableFiles = computed(() => allRows.value.filter((row) => row.type === 'file').map((row) => row.file))
const selectedFileIdSet = computed(() => new Set(props.selectedFileIds))
const selectedCount = computed(() => selectableFiles.value.filter((file) => selectedFileIdSet.value.has(file.id)).length)
const hasDirectories = computed(() => directoryPaths.value.length > 0)
const hasCollapsedDirectories = computed(() =>
  directoryPaths.value.some((path) => collapsedDirectoryPaths.value.has(path)),
)
const allDirectoriesCollapsed = computed(
  () => hasDirectories.value && directoryPaths.value.every((path) => collapsedDirectoryPaths.value.has(path)),
)
const allFilesSelected = computed(
  () => selectableFiles.value.length > 0 && selectedCount.value === selectableFiles.value.length,
)

function isCollapsed(path: string) {
  return collapsedDirectoryPaths.value.has(path)
}

function toggleDirectory(path: string) {
  const next = new Set(collapsedDirectoryPaths.value)
  if (next.has(path)) {
    next.delete(path)
  } else {
    next.add(path)
  }
  collapsedDirectoryPaths.value = next
}

function expandAllDirectories() {
  collapsedDirectoryPaths.value = new Set()
}

function collapseAllDirectories() {
  collapsedDirectoryPaths.value = new Set(directoryPaths.value)
}

function isFileSelected(fileId: number) {
  return selectedFileIdSet.value.has(fileId)
}

function selectAllFiles() {
  emit(
    'update:selectedFileIds',
    selectableFiles.value.map((file) => file.id),
  )
}

function clearFileSelection() {
  emit('update:selectedFileIds', [])
}

function updateFileSelection(fileId: number, checked: boolean) {
  const next = new Set(props.selectedFileIds)
  if (checked) {
    next.add(fileId)
  } else {
    next.delete(fileId)
  }
  emit('update:selectedFileIds', Array.from(next))
}

function handleFileSelectionChange(fileId: number, event: Event) {
  const input = event.target as HTMLInputElement
  updateFileSelection(fileId, input.checked)
}

function depthStyle(depth: number) {
  return {
    paddingLeft: `${12 + depth * 22}px`,
  }
}

function filePreviewRoute(file: FileView) {
  const params: Record<string, string | number> = { ...props.previewRouteParams, fileId: file.id }
  if (props.previewRouteName === 'file-preview') {
    params.slug = props.projectSlug
  }
  return {
    name: props.previewRouteName,
    params,
  }
}

function formatFileAddedTime(value: string) {
  if (!value) {
    return '时间未知'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}
</script>

<template>
  <div class="file-tree-shell">
    <div v-if="hasDirectories || selectable" class="file-tree-toolbar" aria-label="文件列表操作">
      <div v-if="selectable" class="file-tree-selection-toolbar">
        <button
          class="secondary-button small-button"
          type="button"
          :disabled="selectionDisabled || selectableFiles.length === 0 || allFilesSelected"
          @click="selectAllFiles"
        >
          全选
        </button>
        <button
          class="secondary-button small-button"
          type="button"
          :disabled="selectionDisabled || selectedCount === 0"
          @click="clearFileSelection"
        >
          清空
        </button>
      </div>
      <div v-if="hasDirectories" class="file-tree-folder-toolbar">
        <button
          class="file-tree-toolbar-button"
          type="button"
          :disabled="!hasCollapsedDirectories"
          aria-label="全部展开"
          title="全部展开"
          @click="expandAllDirectories"
        >
          <svg class="file-tree-toolbar-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M6 7l6 6 6-6" />
            <path d="M6 12l6 6 6-6" />
          </svg>
        </button>
        <button
          class="file-tree-toolbar-button"
          type="button"
          :disabled="allDirectoriesCollapsed"
          aria-label="全部收起"
          title="全部收起"
          @click="collapseAllDirectories"
        >
          <svg class="file-tree-toolbar-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M6 17l6-6 6 6" />
            <path d="M6 12l6-6 6 6" />
          </svg>
        </button>
      </div>
    </div>

    <div class="file-tree" role="tree">
      <template v-for="row in rows" :key="`${row.type}:${row.path}`">
        <div
          v-if="row.type === 'directory'"
          class="file-tree-row file-tree-directory"
          role="treeitem"
          :style="depthStyle(row.depth)"
        >
          <button
            class="file-tree-directory-button"
            type="button"
            :aria-expanded="!isCollapsed(row.path)"
            @click="toggleDirectory(row.path)"
          >
            <span class="file-tree-toggle" aria-hidden="true">{{ isCollapsed(row.path) ? '+' : '-' }}</span>
            <span class="file-tree-marker directory-marker" aria-hidden="true"></span>
            <span class="file-tree-name" :title="row.path">{{ row.name }}</span>
            <span class="file-tree-added-at">添加于 {{ formatFileAddedTime(row.createdAt) }}</span>
          </button>
        </div>
        <div
          v-else
          class="file-tree-row file-tree-file"
          :class="{ 'file-tree-file-selectable': selectable }"
          role="treeitem"
          :style="depthStyle(row.depth)"
        >
          <label v-if="selectable" class="file-tree-select">
            <input
              type="checkbox"
              :checked="isFileSelected(row.file.id)"
              :disabled="selectionDisabled"
              :aria-label="`选择 ${row.name}`"
              @change="handleFileSelectionChange(row.file.id, $event)"
            />
          </label>
          <RouterLink class="file-tree-main" :to="filePreviewRoute(row.file)" :title="row.path">
            <span class="file-tree-marker file-marker" aria-hidden="true"></span>
            <span class="file-tree-name">{{ row.name }}</span>
          </RouterLink>
          <span class="file-tree-added-at">添加于 {{ formatFileAddedTime(row.file.createdAt) }}</span>
          <span class="file-tree-type">{{ row.file.previewType }}</span>
          <div v-if="canMove || canDelete" class="file-tree-actions">
            <button
              v-if="canMove"
              class="secondary-button small-button file-tree-move-button"
              type="button"
              :disabled="actionsDisabled || movingFileId === row.file.id"
              @click="emit('move', row.file)"
            >
              {{ movingFileId === row.file.id ? '移动中' : '移动' }}
            </button>
            <button
              v-if="canDelete"
              class="danger-button"
              type="button"
              :disabled="actionsDisabled || deletingFileId === row.file.id"
              @click="emit('delete', row.file)"
            >
              {{ deletingFileId === row.file.id ? '删除中' : '删除' }}
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
