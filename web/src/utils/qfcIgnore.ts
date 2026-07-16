export const QFC_IGNORE_FILE_NAME = '.qfcignore'

export interface SpaceProjectUploadFile {
  file: File
  relativePath: string
}

export interface SpaceProjectSkippedFile {
  file: File
  relativePath: string
  reason: 'too-large'
  fileSizeBytes: number
  maxFileSizeBytes: number
}

export interface SpaceProjectUploadPreparation {
  files: SpaceProjectUploadFile[]
  skippedFiles: SpaceProjectSkippedFile[]
  ignoredCount: number
  ignoreFileFound: boolean
}

export interface QfcIgnoreRule {
  pattern: string
  negated: boolean
  directoryOnly: boolean
  anchored: boolean
  hasSlash: boolean
}

function normalizeUploadPath(path: string) {
  return path.replace(/\\/g, '/').split('/').filter(Boolean).join('/')
}

function escapeRegExp(value: string) {
  return value.replace(/[|\\{}()[\]^$+?.]/g, '\\$&')
}

function globToRegExp(pattern: string) {
  let source = ''
  for (let index = 0; index < pattern.length; index += 1) {
    const char = pattern[index]
    if (char === '*') {
      if (pattern[index + 1] === '*') {
        source += '.*'
        index += 1
      } else {
        source += '[^/]*'
      }
    } else if (char === '?') {
      source += '[^/]'
    } else {
      source += escapeRegExp(char)
    }
  }
  return new RegExp(`^${source}$`)
}

function matchesGlob(pattern: string, value: string) {
  return globToRegExp(pattern).test(value)
}

function directoryPrefixes(path: string) {
  const parts = path.split('/')
  const prefixes: string[] = []
  for (let index = 1; index < parts.length; index += 1) {
    prefixes.push(parts.slice(0, index).join('/'))
  }
  return prefixes
}

function matchesRule(rule: QfcIgnoreRule, path: string) {
  if (rule.hasSlash || rule.anchored) {
    if (rule.directoryOnly) {
      return directoryPrefixes(path).some((prefix) => matchesGlob(rule.pattern, prefix))
    }
    return matchesGlob(rule.pattern, path)
  }

  const segments = path.split('/')
  if (rule.directoryOnly) {
    return segments.slice(0, -1).some((segment) => matchesGlob(rule.pattern, segment))
  }
  return segments.some((segment) => matchesGlob(rule.pattern, segment))
}

function folderRelativePath(relativePath: string, rootName: string) {
  const parts = normalizeUploadPath(relativePath).split('/').filter(Boolean)
  if (rootName && parts[0] === rootName) {
    return parts.slice(1).join('/')
  }
  return parts.join('/')
}

function uploadRelativePath(file: File, keepFolderPath: boolean) {
  if (!keepFolderPath) {
    return normalizeUploadPath(file.name)
  }
  return normalizeUploadPath(file.webkitRelativePath || file.name)
}

function rootIgnoreFilePathParts(relativePath: string) {
  const parts = normalizeUploadPath(relativePath).split('/').filter(Boolean)
  return parts.length === 2 && parts[1] === QFC_IGNORE_FILE_NAME ? parts : null
}

function applyMaxFileSize(files: SpaceProjectUploadFile[], maxFileSizeBytes?: number) {
  const keptFiles: SpaceProjectUploadFile[] = []
  const skippedFiles: SpaceProjectSkippedFile[] = []

  for (const file of files) {
    if (maxFileSizeBytes && maxFileSizeBytes > 0 && file.file.size > maxFileSizeBytes) {
      skippedFiles.push({
        file: file.file,
        relativePath: file.relativePath,
        reason: 'too-large',
        fileSizeBytes: file.file.size,
        maxFileSizeBytes,
      })
    } else {
      keptFiles.push(file)
    }
  }

  return { keptFiles, skippedFiles }
}

export function parseQfcIgnore(content: string) {
  const rules: QfcIgnoreRule[] = []
  for (const line of content.split(/\r?\n/)) {
    let pattern = line.trim()
    if (!pattern || pattern.startsWith('#')) {
      continue
    }

    const negated = pattern.startsWith('!')
    if (negated) {
      pattern = pattern.slice(1).trim()
    }

    const directoryOnly = pattern.endsWith('/')
    pattern = pattern.replace(/^\/+/, '').replace(/\/+$/, '')
    pattern = normalizeUploadPath(pattern)
    if (!pattern) {
      continue
    }

    rules.push({
      pattern,
      negated,
      directoryOnly,
      anchored: line.trim().startsWith('/'),
      hasSlash: pattern.includes('/'),
    })
  }
  return rules
}

export function isPathIgnoredByQfcIgnore(path: string, rules: QfcIgnoreRule[]) {
  const normalizedPath = normalizeUploadPath(path)
  let ignored = false
  for (const rule of rules) {
    if (matchesRule(rule, normalizedPath)) {
      ignored = !rule.negated
    }
  }
  return ignored
}

export async function prepareSpaceProjectUploadFiles(
  uploadFiles: File[],
  keepFolderPath: boolean,
  maxFileSizeBytes?: number,
): Promise<SpaceProjectUploadPreparation> {
  const files = uploadFiles.map((file) => ({
    file,
    relativePath: uploadRelativePath(file, keepFolderPath),
  }))

  if (!keepFolderPath) {
    const limitedFiles = applyMaxFileSize(files, maxFileSizeBytes)
    return {
      files: limitedFiles.keptFiles,
      skippedFiles: limitedFiles.skippedFiles,
      ignoredCount: 0,
      ignoreFileFound: false,
    }
  }

  const rootIgnoreFile = files.find((item) => rootIgnoreFilePathParts(item.relativePath))
  const rootName = rootIgnoreFile ? rootIgnoreFilePathParts(rootIgnoreFile.relativePath)?.[0] || '' : ''
  const rules = rootIgnoreFile ? parseQfcIgnore(await rootIgnoreFile.file.text()) : []
  const filteredFiles: SpaceProjectUploadFile[] = []
  let ignoredCount = 0

  for (const file of files) {
    const relativePath = folderRelativePath(file.relativePath, rootName)
    if (relativePath === QFC_IGNORE_FILE_NAME || isPathIgnoredByQfcIgnore(relativePath, rules)) {
      ignoredCount += 1
    } else {
      filteredFiles.push(file)
    }
  }

  const limitedFiles = applyMaxFileSize(filteredFiles, maxFileSizeBytes)
  return {
    files: limitedFiles.keptFiles,
    skippedFiles: limitedFiles.skippedFiles,
    ignoredCount,
    ignoreFileFound: Boolean(rootIgnoreFile),
  }
}
