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
  }>(),
  {
    projectSlug: '',
    canMove: false,
    canDelete: false,
    movingFileId: null,
    deletingFileId: null,
  },
)

const emit = defineEmits<{
  move: [file: FileView]
  delete: [file: FileView]
}>()

const collapsedDirectoryPaths = ref(new Set<string>())
const allRows = computed(() => buildFileTreeRows(props.files))
const directoryPaths = computed(() => fileTreeDirectoryPaths(allRows.value))
const rows = computed(() => visibleFileTreeRows(allRows.value, collapsedDirectoryPaths.value))
const hasDirectories = computed(() => directoryPaths.value.length > 0)
const hasCollapsedDirectories = computed(() =>
  directoryPaths.value.some((path) => collapsedDirectoryPaths.value.has(path)),
)
const allDirectoriesCollapsed = computed(
  () => hasDirectories.value && directoryPaths.value.every((path) => collapsedDirectoryPaths.value.has(path)),
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

function depthStyle(depth: number) {
  return {
    paddingLeft: `${12 + depth * 22}px`,
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
    <div v-if="hasDirectories" class="file-tree-toolbar" aria-label="文件夹展开控制">
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
          role="treeitem"
          :style="depthStyle(row.depth)"
        >
          <RouterLink class="file-tree-main" :to="`/p/${projectSlug}/files/${row.file.id}`" :title="row.path">
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
              :disabled="movingFileId === row.file.id"
              @click="emit('move', row.file)"
            >
              {{ movingFileId === row.file.id ? '移动中' : '移动' }}
            </button>
            <button
              v-if="canDelete"
              class="danger-button"
              type="button"
              :disabled="deletingFileId === row.file.id"
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
