import assert from 'node:assert/strict'
import { pathToFileURL } from 'node:url'

const modulePath = process.argv[2]
if (!modulePath) {
  throw new Error('Missing compiled fileTree module path.')
}

const {
  buildFileTreeRows,
  fileDirectoryOptions,
  fileDirectoryPath,
  fileDisplayName,
  fileTreeDirectoryPaths,
  visibleFileTreeRows,
} = await import(pathToFileURL(modulePath).href)

const files = [
  {
    id: 3,
    originalName: 'image-1.png',
    relativePath: 'docs/images/image-1.png',
    previewType: 'IMAGE',
  },
  {
    id: 1,
    originalName: 'README.md',
    relativePath: 'docs/README.md',
    previewType: 'MARKDOWN',
  },
  {
    id: 2,
    originalName: 'guide.pdf',
    relativePath: '',
    previewType: 'PDF',
  },
]

assert.equal(fileDisplayName(files[0]), 'docs/images/image-1.png')
assert.equal(fileDisplayName(files[2]), 'guide.pdf')
assert.equal(fileDirectoryPath(files[0]), 'docs/images')
assert.equal(fileDirectoryPath(files[2]), '')
assert.deepEqual(fileDirectoryOptions(files), [
  { label: '项目根目录', value: '' },
  { label: 'docs', value: 'docs' },
  { label: 'docs/images', value: 'docs/images' },
])

assert.deepEqual(
  buildFileTreeRows(files).map((row) => ({
    type: row.type,
    name: row.name,
    path: row.path,
    depth: row.depth,
    fileId: row.type === 'file' ? row.file.id : undefined,
  })),
  [
    { type: 'directory', name: 'docs', path: 'docs', depth: 0, fileId: undefined },
    { type: 'directory', name: 'images', path: 'docs/images', depth: 1, fileId: undefined },
    { type: 'file', name: 'image-1.png', path: 'docs/images/image-1.png', depth: 2, fileId: 3 },
    { type: 'file', name: 'README.md', path: 'docs/README.md', depth: 1, fileId: 1 },
    { type: 'file', name: 'guide.pdf', path: 'guide.pdf', depth: 0, fileId: 2 },
  ],
)

const rows = buildFileTreeRows(files)

assert.deepEqual(fileTreeDirectoryPaths(rows), ['docs', 'docs/images'])

assert.deepEqual(
  visibleFileTreeRows(rows, new Set(['docs'])).map((row) => row.path),
  ['docs', 'guide.pdf'],
)

assert.deepEqual(
  visibleFileTreeRows(rows, new Set(['docs/images'])).map((row) => row.path),
  ['docs', 'docs/images', 'docs/README.md', 'guide.pdf'],
)
