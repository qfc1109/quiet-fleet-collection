import assert from 'node:assert/strict'
import { renderMarkdownWithToc, slugHeadingId } from '../src/utils/markdownToc.ts'

const markdown = [
  '# 交接文档',
  '',
  '## 服务器信息',
  '',
  '### **停服** `更新`',
  '',
  '## 服务器信息',
].join('\n')

const result = renderMarkdownWithToc(markdown)

assert.deepEqual(result.headings, [
  { id: 'heading-交接文档', level: 1, text: '交接文档' },
  { id: 'heading-服务器信息', level: 2, text: '服务器信息' },
  { id: 'heading-停服-更新', level: 3, text: '停服 更新' },
  { id: 'heading-服务器信息-2', level: 2, text: '服务器信息' },
])

assert.match(result.html, /<h1 id="heading-交接文档">交接文档<\/h1>/)
assert.match(result.html, /<h2 id="heading-服务器信息">服务器信息<\/h2>/)
assert.match(result.html, /<h3 id="heading-停服-更新"><strong>停服<\/strong> <code>更新<\/code><\/h3>/)
assert.match(result.html, /<h2 id="heading-服务器信息-2">服务器信息<\/h2>/)

const lineBreakResult = renderMarkdownWithToc(['专服1:http://192.168.1.42:9071', '专服2:http://192.168.1.42:9072'].join('\n'))

assert.match(lineBreakResult.html, /专服1:<a href="http:\/\/192\.168\.1\.42:9071">http:\/\/192\.168\.1\.42:9071<\/a><br>专服2:/)

const passwordResult = renderMarkdownWithToc('密码： ch~7Dj~c3t&?g7g')

assert.match(passwordResult.html, /密码： ch~7Dj~c3t&amp;\?g7g/)
assert.doesNotMatch(passwordResult.html, /<del>/)

const strikethroughResult = renderMarkdownWithToc('~~删除线~~')

assert.match(strikethroughResult.html, /<del>删除线<\/del>/)

assert.equal(slugHeadingId('  API Server 01  '), 'heading-api-server-01')
assert.equal(slugHeadingId('---'), 'heading-section')
