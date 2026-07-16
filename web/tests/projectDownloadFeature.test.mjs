import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectViewSource = readFileSync(resolve('src/views/ProjectView.vue'), 'utf8')
const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')
const publicApiSource = readFileSync(resolve('src/api/public.ts'), 'utf8')
const spaceApiSource = readFileSync(resolve('src/api/space.ts'), 'utf8')

assert.match(publicApiSource, /downloadPublicProjectFilesArchive/)
assert.match(publicApiSource, /http\.post<Blob>\([\s\S]*`\/public\/projects\/\$\{encodeURIComponent\(slug\)\}\/files\/archive`[\s\S]*\{ fileIds \}/)
assert.match(publicApiSource, /responseType: 'blob'/)

assert.match(projectViewSource, /projectArchiveUrl/)
assert.match(projectViewSource, /\/api\/public\/projects\/\$\{encodeURIComponent\(slug\.value\)\}\/download/)
assert.match(projectViewSource, /downloadPublicProjectFilesArchive/)
assert.match(projectViewSource, /const selectedFileIds = ref<number\[\]>\(\[\]\)/)
assert.match(projectViewSource, /const selectedFiles = computed/)
assert.match(projectViewSource, /const requestedFileIds = \[\.\.\.selectedFileIds\.value\]/)
assert.match(projectViewSource, /v-model:selected-file-ids="selectedFileIds"/)
assert.match(projectViewSource, /:selection-disabled="downloadingSelectedFiles"/)
assert.match(projectViewSource, /selectable/)
assert.match(projectViewSource, /批量下载/)
assert.match(projectViewSource, /下载整个项目/)

assert.match(spaceApiSource, /downloadSpaceProjectFilesArchive/)
assert.match(spaceApiSource, /http\.post<Blob>\([\s\S]*`\/space\/projects\/\$\{projectId\}\/files\/archive`[\s\S]*\{ fileIds \}/)
assert.match(spaceApiSource, /responseType: 'blob'/)

assert.match(spaceViewSource, /projectArchiveUrl\(project: ProjectView\)/)
assert.match(spaceViewSource, /saveArchiveBlob/)
assert.match(spaceViewSource, /handleDownloadSelectedFiles/)
assert.match(spaceViewSource, /批量下载/)
assert.match(spaceViewSource, /下载整个项目/)

console.log('Project download feature checks passed.')
