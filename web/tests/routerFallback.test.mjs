import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')

assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*'/)
assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*', redirect: '\/'/)
assert.ok(
  routerSource.indexOf("path: '/:pathMatch(.*)*'") > routerSource.indexOf("path: '/space'"),
  'fallback route should stay after concrete routes',
)

console.log('Main web router fallback checks passed.')
