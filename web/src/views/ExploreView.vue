<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProjects } from '../api/public'
import type { ProjectView } from '../api/types'
import { filterProjectsByName } from '../utils/projectSearch'

const projects = ref<ProjectView[]>([])
const searchKeyword = ref('')
const loading = ref(true)
const error = ref('')
const filteredProjects = computed(() => filterProjectsByName(projects.value, searchKeyword.value))

onMounted(async () => {
  try {
    projects.value = await getProjects()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '项目加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="page-section explore-hero">
    <div class="explore-copy">
      <p class="eyebrow">Explore</p>
      <h1>项目广场</h1>
      <p class="lead">浏览大家公开出来的项目资料，像逛 GitHub 仓库一样找到可以查看和参考的内容。</p>
    </div>

    <label class="explore-search">
      <span class="visually-hidden">搜索项目名</span>
      <input v-model="searchKeyword" type="search" autocomplete="off" placeholder="搜索项目名" />
    </label>
  </section>

  <section class="page-section">
    <p v-if="loading" class="status">正在加载项目</p>
    <p v-else-if="error" class="status error">{{ error }}</p>
    <p v-else-if="projects.length === 0" class="status">还没有公开项目。</p>
    <p v-else-if="filteredProjects.length === 0" class="status">没有找到匹配的项目。</p>
    <div v-else class="project-grid">
      <RouterLink
        v-for="project in filteredProjects"
        :key="project.id"
        class="project-card"
        :to="`/p/${project.slug}`"
        :aria-label="`打开项目 ${project.name}`"
      >
        <strong>{{ project.name }}</strong>
        <p>{{ project.description || '暂无简介' }}</p>
        <span class="project-card-action">打开项目</span>
      </RouterLink>
    </div>
  </section>
</template>
