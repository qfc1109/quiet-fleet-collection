import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectViewSource = readFileSync(resolve('src/views/ProjectView.vue'), 'utf8')
const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')
const fileTreeSource = readFileSync(resolve('src/components/FileTreeList.vue'), 'utf8')
const filePreviewSource = readFileSync(resolve('src/views/FilePreviewView.vue'), 'utf8')

assert.match(spaceViewSource, /useRouter/)
assert.match(spaceViewSource, /spaceProjectRouteName/)
assert.match(spaceViewSource, /routeProjectId/)
assert.match(spaceViewSource, /syncProjectSelectionFromRoute/)
assert.match(spaceViewSource, /name: 'space-project-files'/)
assert.match(spaceViewSource, /name: 'space-project-edit'/)
assert.match(spaceViewSource, /name: 'space-project-visitor'/)
assert.match(spaceViewSource, /preview-route-name="space-project-file-preview"/)
assert.doesNotMatch(spaceViewSource, /spaceProjectModuleQuery/)
assert.doesNotMatch(spaceViewSource, /spaceProjectPanelQuery/)
assert.doesNotMatch(spaceViewSource, /route\.query\.module/)
assert.doesNotMatch(spaceViewSource, /from: 'space-project-management'/)

assert.match(projectViewSource, /fromSpaceProjectVisitor/)
assert.match(projectViewSource, /返回项目管理/)
assert.match(projectViewSource, /name: 'space-project-files'/)
assert.match(projectViewSource, /route\.params\.projectId/)
assert.doesNotMatch(projectViewSource, /from === 'space-project-management'/)
assert.doesNotMatch(projectViewSource, /query: \{ module: 'projects' \}/)

assert.match(fileTreeSource, /filePreviewRoute/)
assert.match(fileTreeSource, /previewRouteName/)
assert.match(fileTreeSource, /previewRouteParams/)
assert.doesNotMatch(fileTreeSource, /:to="`\/p\/\$\{projectSlug\}\/files\/\$\{row\.file\.id\}`"/)

assert.match(filePreviewSource, /projectRoute/)
assert.match(filePreviewSource, /spaceProjectFilePreview/)
assert.match(filePreviewSource, /spaceProjectManagementRoute/)
assert.match(filePreviewSource, /route\.params\.projectId/)
assert.match(filePreviewSource, /router\.push\(projectRoute\.value\)/)
assert.doesNotMatch(filePreviewSource, /fromSpaceProjectManagement/)
assert.doesNotMatch(filePreviewSource, /router\.push\(projectPath\.value\)/)

console.log('Project visitor return checks passed.')
