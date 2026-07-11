import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectViewSource = readFileSync(resolve('src/views/ProjectView.vue'), 'utf8')
const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')

assert.match(spaceViewSource, /useRoute/)
assert.match(spaceViewSource, /spaceProjectModuleQuery/)
assert.match(spaceViewSource, /from: 'space-project-management'/)
assert.match(spaceViewSource, /route\.query\.module === spaceProjectModuleQuery/)

assert.match(projectViewSource, /fromSpaceProjectManagement/)
assert.match(projectViewSource, /返回项目管理/)
assert.match(projectViewSource, /name: 'space'/)
assert.match(projectViewSource, /query: \{ module: 'projects' \}/)

console.log('Project visitor return checks passed.')
