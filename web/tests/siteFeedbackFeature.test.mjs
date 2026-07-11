import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const typesSource = readFileSync(resolve('src/api/types.ts'), 'utf8')
const publicApiSource = readFileSync(resolve('src/api/public.ts'), 'utf8')
const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')
const appSource = readFileSync(resolve('src/App.vue'), 'utf8')
const feedbackViewSource = readFileSync(resolve('src/views/FeedbackView.vue'), 'utf8')

assert.match(typesSource, /interface SiteFeedbackView/)
assert.match(publicApiSource, /createSiteFeedback/)
assert.match(publicApiSource, /\/public\/feedback/)

assert.match(routerSource, /path: '\/feedback'/)
assert.match(appSource, /label: '反馈'/)

assert.match(feedbackViewSource, /useSessionStore/)
assert.match(feedbackViewSource, /createSiteFeedback/)
assert.match(feedbackViewSource, /redirect: route\.fullPath/)
assert.match(feedbackViewSource, /feedback-form/)
assert.match(feedbackViewSource, /主站反馈/)
