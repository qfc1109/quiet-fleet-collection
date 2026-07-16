import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const httpSource = readFileSync(resolve('src/api/http.ts'), 'utf8')

assert.match(httpSource, /\/auth\/csrf/)
assert.match(httpSource, /X-CSRF-Token/)
assert.match(httpSource, /interceptors\.request\.use/)
assert.match(httpSource, /readErrorResponseData/)
assert.match(httpSource, /readJsonBlob/)
assert.match(httpSource, /data instanceof Blob/)
assert.match(httpSource, /blob\.text\(\)/)
assert.match(httpSource, /CSRF_TOKEN_INVALID/)

console.log('Main web CSRF HTTP checks passed.')
