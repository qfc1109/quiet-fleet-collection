import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const loginSource = readFileSync(resolve('src/views/LoginView.vue'), 'utf8')

assert.doesNotMatch(loginSource, /VITE_QFC_ADMIN_URL/)
assert.doesNotMatch(loginSource, /adminLoginUrl/)
assert.doesNotMatch(loginSource, /进入后台登录/)
assert.doesNotMatch(loginSource, /后台管理系统/)
assert.match(loginSource, /const fallback = '\/space'/)
