import type { FileView } from '../api/types'

export type FileTreeRow =
  | {
      type: 'directory'
      name: string
      path: string
      depth: number
      createdAt: string
    }
  | {
      type: 'file'
      name: string
      path: string
      depth: number
      file: FileView
    }

interface TreeDirectory {
  name: string
  path: string
  createdAt: string
  directories: Map<string, TreeDirectory>
  files: FileView[]
}

function createDirectory(name: string, path: string): TreeDirectory {
  return {
    name,
    path,
    createdAt: '',
    directories: new Map(),
    files: [],
  }
}

function compareName(left: string, right: string) {
  return left.localeCompare(right, 'zh-CN', { numeric: true, sensitivity: 'base' })
}

function cleanPath(path: string) {
  return path
    .replace(/\\/g, '/')
    .split('/')
    .map((part) => part.trim())
    .filter(Boolean)
    .join('/')
}

function earlierCreatedAt(left: string, right: string) {
  if (!left) {
    return right
  }
  if (!right) {
    return left
  }
  const leftTime = Date.parse(left)
  const rightTime = Date.parse(right)
  if (Number.isNaN(leftTime) || Number.isNaN(rightTime)) {
    return left <= right ? left : right
  }
  return leftTime <= rightTime ? left : right
}

function syncDirectoryCreatedAt(directory: TreeDirectory) {
  let createdAt = ''
  for (const child of directory.directories.values()) {
    createdAt = earlierCreatedAt(createdAt, syncDirectoryCreatedAt(child))
  }
  for (const file of directory.files) {
    createdAt = earlierCreatedAt(createdAt, file.createdAt)
  }
  directory.createdAt = createdAt
  return createdAt
}

export function fileDisplayName(file: Pick<FileView, 'relativePath' | 'originalName'>) {
  return cleanPath(file.relativePath || file.originalName)
}

export function fileBaseName(file: Pick<FileView, 'relativePath' | 'originalName'>) {
  const displayName = fileDisplayName(file)
  const slashIndex = displayName.lastIndexOf('/')
  return slashIndex >= 0 ? displayName.slice(slashIndex + 1) : displayName
}

export function fileDirectoryPath(file: Pick<FileView, 'relativePath' | 'originalName'>) {
  const displayName = fileDisplayName(file)
  const slashIndex = displayName.lastIndexOf('/')
  return slashIndex >= 0 ? displayName.slice(0, slashIndex) : ''
}

export function fileDirectoryOptions(files: Array<Pick<FileView, 'relativePath' | 'originalName'>>) {
  const directoryPaths = new Set<string>()
  for (const file of files) {
    const directoryPath = fileDirectoryPath(file)
    if (directoryPath) {
      const parts = directoryPath.split('/').filter(Boolean)
      for (let index = 0; index < parts.length; index += 1) {
        directoryPaths.add(parts.slice(0, index + 1).join('/'))
      }
    }
  }
  return [
    { label: '项目根目录', value: '' },
    ...Array.from(directoryPaths)
      .sort(compareName)
      .map((path) => ({ label: path, value: path })),
  ]
}

export function buildFileTreeRows(files: FileView[]): FileTreeRow[] {
  const root = createDirectory('', '')

  for (const file of files) {
    const displayName = fileDisplayName(file)
    const pathParts = displayName.split('/').filter(Boolean)
    if (pathParts.length === 0) {
      root.files.push(file)
      continue
    }

    let current = root
    for (const directoryName of pathParts.slice(0, -1)) {
      const directoryPath = current.path ? `${current.path}/${directoryName}` : directoryName
      let directory = current.directories.get(directoryName)
      if (!directory) {
        directory = createDirectory(directoryName, directoryPath)
        current.directories.set(directoryName, directory)
      }
      current = directory
    }
    current.files.push(file)
  }

  syncDirectoryCreatedAt(root)

  const rows: FileTreeRow[] = []

  function appendDirectory(directory: TreeDirectory, depth: number) {
    const directories = Array.from(directory.directories.values()).sort((left, right) =>
      compareName(left.name, right.name),
    )
    for (const child of directories) {
      rows.push({
        type: 'directory',
        name: child.name,
        path: child.path,
        depth,
        createdAt: child.createdAt,
      })
      appendDirectory(child, depth + 1)
    }

    const directoryFiles = [...directory.files].sort((left, right) => compareName(fileBaseName(left), fileBaseName(right)))
    for (const file of directoryFiles) {
      rows.push({
        type: 'file',
        name: fileBaseName(file),
        path: fileDisplayName(file),
        depth,
        file,
      })
    }
  }

  appendDirectory(root, 0)
  return rows
}

export function visibleFileTreeRows(rows: FileTreeRow[], collapsedDirectoryPaths: ReadonlySet<string>) {
  return rows.filter((row) => {
    for (const collapsedPath of collapsedDirectoryPaths) {
      if (row.path !== collapsedPath && row.path.startsWith(`${collapsedPath}/`)) {
        return false
      }
    }
    return true
  })
}

export function fileTreeDirectoryPaths(rows: FileTreeRow[]) {
  return rows.filter((row) => row.type === 'directory').map((row) => row.path)
}
