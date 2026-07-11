import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')

assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*'/)
assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*', redirect: '\/'/)
assert.ok(
  routerSource.indexOf("path: '/:pathMatch(.*)*'") > routerSource.indexOf("path: '/login'"),
  'fallback route should stay after concrete routes',
)

console.log('Admin web router fallback checks passed.')
