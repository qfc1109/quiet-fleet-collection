import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const httpSource = readFileSync(resolve('src/api/http.ts'), 'utf8')
const appSource = readFileSync(resolve('src/App.vue'), 'utf8')

assert.match(httpSource, /export function registerSessionInvalidationHandler/)
assert.match(httpSource, /ACCOUNT_LOGGED_IN_ELSEWHERE/)
assert.match(appSource, /registerSessionInvalidationHandler/)
assert.match(appSource, /v-if="showElsewhereLoginDialog"/)
assert.match(appSource, /router\.replace\(\{ path: '\/login', query: \{ reason: 'elsewhere' \}\s*\}\)/)
assert.match(appSource, /账号已在其他设备或位置登录/)
assert.match(appSource, /重新登录/)

console.log('Account logged-in-elsewhere modal checks passed.')
