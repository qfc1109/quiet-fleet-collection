import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const routerSource = readFileSync(resolve('src/router/index.ts'), 'utf8')

assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*'/)
assert.match(routerSource, /path: '\/:pathMatch\(\.\*\)\*', redirect: '\/'/)
assert.match(routerSource, /path: '\/space', redirect: '\/space\/account'/)
assert.match(routerSource, /path: '\/space\/account', name: 'space-account'/)
assert.match(routerSource, /path: '\/space\/projects', name: 'space-projects'/)
assert.match(routerSource, /path: '\/space\/projects\/:projectId\/files', name: 'space-project-files'/)
assert.match(routerSource, /path: '\/space\/projects\/:projectId\/edit', name: 'space-project-edit'/)
assert.match(routerSource, /path: '\/space\/projects\/:projectId\/files\/:fileId\/preview'[\s\S]*name: 'space-project-file-preview'/)
assert.match(routerSource, /path: '\/space\/projects\/:projectId\/visitor\/:slug'[\s\S]*name: 'space-project-visitor'/)
assert.match(routerSource, /to\.path === '\/space' \|\| to\.path\.startsWith\('\/space\/'\)/)
assert.ok(
  routerSource.indexOf("path: '/:pathMatch(.*)*'") > routerSource.indexOf("path: '/space/projects/:projectId/visitor/:slug'"),
  'fallback route should stay after concrete routes',
)

console.log('Main web router fallback checks passed.')
