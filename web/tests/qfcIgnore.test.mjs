import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { pathToFileURL } from 'node:url'
import ts from 'typescript'

const source = readFileSync(resolve('src/utils/qfcIgnore.ts'), 'utf8')
const output = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.ES2022,
    target: ts.ScriptTarget.ES2022,
    verbatimModuleSyntax: true,
  },
}).outputText

const tempDir = mkdtempSync(join(tmpdir(), 'qfcignore-test-'))
const tempModule = join(tempDir, 'qfcIgnore.mjs')
writeFileSync(tempModule, output)

try {
  const { prepareSpaceProjectUploadFiles } = await import(pathToFileURL(tempModule).href)

  function uploadFile(relativePath, content = '', size = content.length) {
    const pathParts = relativePath.split('/')
    return {
      name: pathParts[pathParts.length - 1],
      size,
      webkitRelativePath: relativePath,
      text: async () => content,
    }
  }

  const ignoreFile = uploadFile(
    'Project/.qfcignore',
    `
# comments and blank lines are ignored
*.mp4
dist/
cache/big.zip
node_modules
`,
  )
  const result = await prepareSpaceProjectUploadFiles(
    [
      ignoreFile,
      uploadFile('Project/readme.md'),
      uploadFile('Project/video.mp4'),
      uploadFile('Project/dist/bundle.js'),
      uploadFile('Project/cache/big.zip'),
      uploadFile('Project/src/node_modules/library.js'),
      uploadFile('Project/src/app.ts'),
    ],
    true,
  )

  assert.equal(result.ignoreFileFound, true)
  assert.equal(result.ignoredCount, 5)
  assert.deepEqual(
    result.files.map((file) => file.relativePath),
    ['Project/readme.md', 'Project/src/app.ts'],
  )

  const ordinaryFiles = await prepareSpaceProjectUploadFiles([uploadFile('plain.mp4')], false)
  assert.equal(ordinaryFiles.ignoreFileFound, false)
  assert.equal(ordinaryFiles.ignoredCount, 0)
  assert.deepEqual(
    ordinaryFiles.files.map((file) => file.relativePath),
    ['plain.mp4'],
  )

  const sizeLimitedFiles = await prepareSpaceProjectUploadFiles(
    [
      uploadFile('Limited/readme.md', '', 5),
      uploadFile('Limited/archive.zip', '', 6),
    ],
    true,
    5,
  )
  assert.deepEqual(
    sizeLimitedFiles.files.map((file) => file.relativePath),
    ['Limited/readme.md'],
  )
  assert.deepEqual(
    sizeLimitedFiles.skippedFiles.map((file) => ({
      relativePath: file.relativePath,
      reason: file.reason,
      fileSizeBytes: file.fileSizeBytes,
      maxFileSizeBytes: file.maxFileSizeBytes,
    })),
    [
      {
        relativePath: 'Limited/archive.zip',
        reason: 'too-large',
        fileSizeBytes: 6,
        maxFileSizeBytes: 5,
      },
    ],
  )
} finally {
  rmSync(tempDir, { recursive: true, force: true })
}

console.log('QFC ignore checks passed.')
