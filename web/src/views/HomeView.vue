<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getSite, type SiteInfo } from '../api/public'

const site = ref<SiteInfo | null>(null)
const siteName = computed(() => site.value?.name || '轻帆集')
const siteEnglishName = computed(() => site.value?.englishName || 'Quiet Fleet Collection')
const siteDescription = computed(
  () => site.value?.description || '一个用于整理项目资料、展示文档和分享文件的个人空间。',
)

const highlights = [
  {
    label: '01',
    title: '项目成册',
    text: '把文档、表格、图片和 PDF 按项目收拢，访客进入后能直接顺着文件结构阅读。',
  },
  {
    label: '02',
    title: '公开可达',
    text: '公开项目从广场进入，适合同事查看说明、验收资料和沉淀下来的过程记录。',
  },
  {
    label: '03',
    title: '轻量维护',
    text: '登录后在个人空间维护项目和文件，首页只承担清晰入口，不打断浏览节奏。',
  },
]

const previewFiles = [
  { name: 'docs / requirements.md', type: 'Markdown' },
  { name: 'assets / flow.png', type: 'Image' },
  { name: 'reports / milestone.xlsx', type: 'Excel' },
]

onMounted(async () => {
  try {
    site.value = await getSite()
  } catch {
    site.value = null
  }
})
</script>

<template>
  <div class="home-page">
    <section class="page-section home-hero">
      <div class="home-hero-copy">
        <p class="eyebrow">{{ siteEnglishName }}</p>
        <h1>{{ siteName }}</h1>
        <p class="lead">{{ siteDescription }}</p>

        <div class="quick-actions" aria-label="首页入口">
          <RouterLink class="primary-action home-primary-action" to="/explore">进入项目广场</RouterLink>
        </div>
      </div>

      <div class="home-visual" aria-label="项目资料预览">
        <div class="home-visual-toolbar" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="home-visual-head">
          <div>
            <span class="home-visual-kicker">PUBLIC SPACE</span>
            <strong>release-notes</strong>
          </div>
          <span class="home-visual-status">Readable</span>
        </div>
        <div class="home-file-list">
          <div v-for="file in previewFiles" :key="file.name" class="home-file-row">
            <span class="home-file-mark" aria-hidden="true"></span>
            <span>{{ file.name }}</span>
            <small>{{ file.type }}</small>
          </div>
        </div>
        <div class="home-readme-preview" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </section>

    <section class="page-section home-highlights" aria-label="站点说明">
      <article v-for="item in highlights" :key="item.title" class="home-highlight">
        <span>{{ item.label }}</span>
        <h2>{{ item.title }}</h2>
        <p>{{ item.text }}</p>
      </article>
    </section>
  </div>
</template>
