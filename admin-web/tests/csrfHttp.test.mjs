import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const httpSource = readFileSync(resolve('src/api/http.ts'), 'utf8')

assert.match(httpSource, /\/auth\/csrf/)
assert.match(httpSource, /X-CSRF-Token/)
assert.match(httpSource, /interceptors\.request\.use/)

console.log('Admin web CSRF HTTP checks passed.')
