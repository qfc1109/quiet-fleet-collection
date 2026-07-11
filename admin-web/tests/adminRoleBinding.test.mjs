import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const dashboardSource = readFileSync(resolve('src/views/AdminDashboardView.vue'), 'utf8')

assert.doesNotMatch(dashboardSource, /v-model="adminForm\.roleCodes\[0\]"/)
assert.match(dashboardSource, /<el-checkbox-group v-model="adminForm\.roleCodes"/)
assert.match(dashboardSource, /roleCodes:\s*adminForm\.value\.roleCodes/)

console.log('Admin role binding checks passed.')
