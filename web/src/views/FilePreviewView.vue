<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import { getFilePreview } from '../api/public'
import type { FilePreview } from '../api/types'
import { renderMarkdownWithToc } from '../utils/markdownToc'

const route = useRoute()
const router = useRouter()
const fileId = computed(() => String(route.params.fileId || ''))
const projectPath = computed(() => `/p/${String(route.params.slug || '')}`)
const preview = ref<FilePreview | null>(null)
const loading = ref(true)
const error = ref('')
const markdownRender = computed(() => {
  const content = preview.value?.content || ''
  const result = renderMarkdownWithToc(content)
  return {
    html: DOMPurify.sanitize(result.html),
    headings: result.headings,
  }
})
const renderedMarkdown = computed(() => markdownRender.value.html)
const markdownHeadings = computed(() => markdownRender.value.headings)
const showMarkdownToc = computed(() => preview.value?.previewType === 'MARKDOWN' && markdownHeadings.value.length > 0)

function goBack() {
  router.push(projectPath.value)
}

async function loadPreview() {
  loading.value = true
  error.value = ''
  try {
    preview.value = await getFilePreview(fileId.value)
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '文件预览加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadPreview)
watch(fileId, loadPreview)
</script>

<template>
  <section class="page-section preview-hero">
    <button class="secondary-action preview-back-button" type="button" @click="goBack">返回</button>
    <div>
      <p class="eyebrow">Preview</p>
      <h1>{{ preview?.originalName || '文件预览' }}</h1>
      <p class="lead">{{ preview?.relativePath || `当前文件 ID：${fileId}` }}</p>
    </div>
  </section>

  <section class="page-section preview-layout" :class="{ 'without-toc': !showMarkdownToc }">
    <aside v-if="showMarkdownToc" class="markdown-toc" aria-label="Markdown 标题索引">
      <p class="markdown-toc-title">标题索引</p>
      <nav>
        <a
          v-for="heading in markdownHeadings"
          :key="heading.id"
          :href="`#${heading.id}`"
          :style="{ paddingLeft: `${8 + Math.max(0, heading.level - 1) * 12}px` }"
        >
          {{ heading.text }}
        </a>
      </nav>
    </aside>

    <div class="preview-box">
      <p v-if="loading" class="status">正在加载预览</p>
      <p v-else-if="error" class="status error">{{ error }}</p>
      <template v-else-if="preview">
        <article v-if="preview.previewType === 'MARKDOWN'" class="markdown-preview" v-html="renderedMarkdown"></article>
        <img v-else-if="preview.previewType === 'IMAGE'" class="image-preview" :src="preview.streamUrl" :alt="preview.originalName" />
        <iframe v-else-if="preview.previewType === 'PDF'" class="pdf-preview" :src="preview.streamUrl" title="PDF 预览"></iframe>
        <table v-else-if="preview.previewType === 'EXCEL'" class="excel-preview">
          <tbody>
            <tr v-for="(row, rowIndex) in preview.excel?.rows || []" :key="rowIndex">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
        <article v-else-if="preview.previewType === 'WORD'" class="word-preview">
          <div class="word-preview-notice" role="note">
            <strong>仅展示文本预览</strong>
            <span>Word 文档中的图片和复杂版式暂不在线展示，请下载原文件查看。</span>
          </div>
          <pre>{{ preview.content }}</pre>
        </article>
        <div v-else class="download-only">
          <p>这个文件暂不支持在线预览。</p>
        </div>
        <a class="secondary-action" :href="preview.downloadUrl">下载</a>
      </template>
    </div>
  </section>
</template>
