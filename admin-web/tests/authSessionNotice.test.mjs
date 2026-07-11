import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const httpSource = readFileSync(resolve('src/api/http.ts'), 'utf8')
const typeSource = readFileSync(resolve('src/api/types.ts'), 'utf8')
const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')
const loginViewSource = readFileSync(resolve('src/views/AdminLoginView.vue'), 'utf8')

assert.match(typeSource, /export class ApiError extends Error/)
assert.match(httpSource, /new ApiError\(String\(data\.code\)/)
assert.match(routerSource, /ACCOUNT_LOGGED_IN_ELSEWHERE/)
assert.match(routerSource, /reason: authRedirectReason\(caught\)/)
assert.match(loginViewSource, /const loginNotice = computed/)
assert.match(loginViewSource, /账号已在其他设备或位置登录，请重新登录/)
assert.match(loginViewSource, /登录已过期，请重新登录/)

console.log('Admin auth session notice checks passed.')
