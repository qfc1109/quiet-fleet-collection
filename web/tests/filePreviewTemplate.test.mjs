import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/views/FilePreviewView.vue', import.meta.url), 'utf8')

assert.match(source, /preview\.previewType === 'WORD'/)
assert.match(source, /仅展示文本预览/)
assert.match(source, /图片和复杂版式暂不在线展示/)
assert.doesNotMatch(source, /<div v-else class="download-only">[\s\S]*class="primary-action"[\s\S]*<\/div>/)
assert.match(
  source,
  /<nav>[\s\S]*<\/nav>\s*<a v-if="preview" class="secondary-action markdown-toc-download" :href="preview\.downloadUrl">下载<\/a>/,
)
assert.doesNotMatch(
  source,
  /<div class="preview-box">[\s\S]*<a class="secondary-action" :href="preview\.downloadUrl">下载<\/a>/,
)
