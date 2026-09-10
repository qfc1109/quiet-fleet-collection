import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/components/FileTreeList.vue', import.meta.url), 'utf8')

assert.match(source, /const collapsedDirectoryPaths = ref\(new Set\(directoryPaths\.value\)\)/)

console.log('File tree defaults to collapsed directories.')
