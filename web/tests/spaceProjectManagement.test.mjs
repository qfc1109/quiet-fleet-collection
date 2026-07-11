import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')

assert.match(spaceViewSource, /type ProjectPanel = 'list' \| 'edit' \| 'files'/)
assert.match(spaceViewSource, /const activeProjectPanel = ref<ProjectPanel>\('list'\)/)
assert.match(spaceViewSource, /const issueError = ref\(''\)/)
assert.match(spaceViewSource, /function returnToProjectList\(\)/)
assert.match(spaceViewSource, /async function openProjectFiles\(project: ProjectView\)/)
assert.match(spaceViewSource, /function openProjectEditor\(project: ProjectView\)/)
assert.match(spaceViewSource, /async function handleDeleteProject\(project: ProjectView\)/)
assert.match(spaceViewSource, /deleteSpaceProject/)
assert.match(spaceViewSource, />编辑</)
assert.match(spaceViewSource, /'删除'/)
assert.match(spaceViewSource, />返回项目列表</)
assert.match(spaceViewSource, /activeProjectPanel === 'list'/)
assert.match(spaceViewSource, /activeProjectPanel === 'edit'/)
assert.match(spaceViewSource, /activeProjectPanel === 'files'/)
assert.match(spaceViewSource, /@click\.stop="openProjectEditor\(project\)"/)
assert.match(spaceViewSource, /@click\.stop="handleDeleteProject\(project\)"/)
assert.doesNotMatch(spaceViewSource, /error\.value = caught instanceof Error \? caught\.message : '提问记录加载失败'/)
assert.match(spaceViewSource, /issueError\.value = '提问记录暂不可用'/)

console.log('Space project management checks passed.')
