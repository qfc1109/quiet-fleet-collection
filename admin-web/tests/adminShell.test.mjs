import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const appSource = readFileSync(resolve('src/App.vue'), 'utf8')
const authSource = readFileSync(resolve('src/api/auth.ts'), 'utf8')

assert.doesNotMatch(appSource, /VITE_QFC_MAIN_SITE_URL/)
assert.doesNotMatch(appSource, /mainSiteUrl/)
assert.doesNotMatch(appSource, /打开主站/)
assert.match(authSource, /\/auth\/admin\/me/)
assert.match(authSource, /\/auth\/admin\/logout/)
assert.doesNotMatch(authSource, /\/auth\/me/)
assert.doesNotMatch(authSource, /\/auth\/logout/)
