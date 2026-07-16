import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const spaceApiSource = readFileSync(resolve('src/api/space.ts'), 'utf8')
const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')

assert.match(spaceApiSource, /\/space\/projects/)
assert.match(spaceApiSource, /\/space\/files/)
assert.match(spaceApiSource, /getSpaceUploadLimits/)
assert.match(spaceApiSource, /\/space\/upload-limits/)
assert.match(spaceApiSource, /downloadSpaceProjectFilesArchive/)
assert.match(spaceApiSource, /\/space\/projects\/\$\{projectId\}\/files\/archive/)
assert.match(spaceApiSource, /responseType: 'blob'/)
assert.match(spaceApiSource, /deleteSpaceProject/)
assert.match(spaceApiSource, /http\.delete<ApiResponse<boolean>>\(`\/space\/projects\/\$\{projectId\}`\)/)
assert.doesNotMatch(spaceApiSource, /\/admin\//)
assert.match(spaceViewSource, /from '\.\.\/api\/space'/)
assert.doesNotMatch(spaceViewSource, /getAdminProjects/)
assert.doesNotMatch(spaceViewSource, /getAdminProjectFiles/)
