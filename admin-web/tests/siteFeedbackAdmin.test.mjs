import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const typesSource = readFileSync(resolve('src/api/types.ts'), 'utf8')
const apiSource = readFileSync(resolve('src/api/adminManagement.ts'), 'utf8')
const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')
const appSource = readFileSync(resolve('src/App.vue'), 'utf8')
const sessionSource = readFileSync(resolve('src/stores/session.ts'), 'utf8')
const dashboardSource = readFileSync(resolve('src/views/AdminDashboardView.vue'), 'utf8')

assert.match(typesSource, /interface SiteFeedbackView/)
assert.match(apiSource, /getSiteFeedback/)
assert.match(apiSource, /\/admin\/feedback/)

assert.match(routerSource, /path: '\/feedback'/)
assert.match(routerSource, /requiresIssueManage/)
assert.match(appSource, /主站反馈/)
assert.match(sessionSource, /canManageIssues/)
assert.match(sessionSource, /ISSUE_MANAGE/)

assert.match(dashboardSource, /'feedback'/)
assert.match(dashboardSource, /feedbackList/)
assert.match(dashboardSource, /getSiteFeedback/)
assert.match(dashboardSource, /反馈内容/)
