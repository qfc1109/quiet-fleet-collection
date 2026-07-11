import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const typesSource = readFileSync(resolve('src/api/types.ts'), 'utf8')
const publicApiSource = readFileSync(resolve('src/api/public.ts'), 'utf8')
const spaceApiSource = readFileSync(resolve('src/api/space.ts'), 'utf8')
const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')
const projectViewSource = readFileSync(resolve('src/views/ProjectView.vue'), 'utf8')
const spaceViewSource = readFileSync(resolve('src/views/SpaceView.vue'), 'utf8')

assert.match(typesSource, /interface ProjectIssueView/)
assert.match(publicApiSource, /createProjectIssue/)
assert.match(publicApiSource, /\/public\/projects\/\$\{slug\}\/issues/)
assert.match(spaceApiSource, /getSpaceProjectIssues/)
assert.match(spaceApiSource, /\/space\/projects\/\$\{projectId\}\/issues/)
assert.match(routerSource, /path: '\/p\/:slug\/issues\/new'/)
assert.match(routerSource, /name: 'project-issue-new'/)

assert.match(projectViewSource, /useSessionStore/)
assert.match(projectViewSource, /createProjectIssue/)
assert.match(projectViewSource, /redirect: route\.fullPath/)
assert.match(projectViewSource, /issueEntryRoute/)
assert.match(projectViewSource, /issue-modal-panel/)
assert.match(projectViewSource, /issue-form/)
assert.doesNotMatch(projectViewSource, /<section class="page-section issue-panel"/)

assert.match(spaceViewSource, /getSpaceProjectIssues/)
assert.match(spaceViewSource, /projectIssues/)
assert.match(spaceViewSource, /issue-list/)
assert.match(spaceViewSource, /提问记录/)
