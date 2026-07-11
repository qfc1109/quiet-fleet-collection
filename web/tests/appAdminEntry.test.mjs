import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const appSource = readFileSync(resolve('src/App.vue'), 'utf8')

assert.doesNotMatch(appSource, /adminHomeUrl/)
assert.doesNotMatch(appSource, /VITE_QFC_ADMIN_URL/)
assert.doesNotMatch(appSource, /返回后台/)
assert.doesNotMatch(appSource, /session\.canEnterAdmin/)
