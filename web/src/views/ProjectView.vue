<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createProjectIssue, getProject, getProjectFiles } from '../api/public'
import type { FileView, ProjectView } from '../api/types'
import FileTreeList from '../components/FileTreeList.vue'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const slug = computed(() => String(route.params.slug || ''))
const fromSpaceProjectManagement = computed(() => route.query.from === 'space-project-management')
const projectManagementRoute = { name: 'space', query: { module: 'projects' } }
const issueDialogOpen = computed(() => route.name === 'project-issue-new')
const issueEntryRoute = computed(() => ({
  name: 'project-issue-new',
  params: { slug: slug.value },
  query: route.query,
}))
const project = ref<ProjectView | null>(null)
const files = ref<FileView[]>([])
const loading = ref(true)
const error = ref('')
const issueTitle = ref('')
const issueContent = ref('')
const issueError = ref('')
const issueMessage = ref('')
const submittingIssue = ref(false)

async function loadProject() {
  loading.value = true
  error.value = ''
  try {
    const [projectInfo, fileList] = await Promise.all([
      getProject(slug.value),
      getProjectFiles(slug.value),
    ])
    project.value = projectInfo
    files.value = fileList
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '项目加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadProject)
watch(slug, loadProject)

function goLoginForIssue() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

function closeIssueDialog() {
  router.push({ name: 'project', params: { slug: slug.value }, query: route.query })
}

async function submitIssue() {
  if (!session.loggedIn) {
    goLoginForIssue()
    return
  }
  issueError.value = ''
  issueMessage.value = ''
  submittingIssue.value = true
  try {
    await createProjectIssue(slug.value, issueTitle.value, issueContent.value)
    issueTitle.value = ''
    issueContent.value = ''
    issueMessage.value = '问题已提交，项目作者会在个人空间中看到。'
  } catch (caught) {
    issueError.value = caught instanceof Error ? caught.message : '问题提交失败'
  } finally {
    submittingIssue.value = false
  }
}
</script>

<template>
  <section class="page-section project-hero">
    <div>
      <p class="eyebrow">Project</p>
      <h1>{{ project?.name || slug }}</h1>
      <p class="lead">{{ project?.description || '项目详情和文件列表会在公开接口接入后展示。' }}</p>
    </div>
    <RouterLink v-if="fromSpaceProjectManagement" class="secondary-action" :to="projectManagementRoute">
      返回项目管理
    </RouterLink>
  </section>

  <section class="page-section">
    <div class="section-heading">
      <div>
        <h2>文件</h2>
        <p>{{ loading ? '正在加载' : `${files.length} 个文件` }}</p>
      </div>
      <RouterLink class="primary-action issue-entry-action" :to="issueEntryRoute">提出问题</RouterLink>
    </div>
    <p v-if="error" class="status error">{{ error }}</p>
    <p v-else-if="!loading && files.length === 0" class="status">这个项目还没有文件。</p>
    <FileTreeList v-else :files="files" :project-slug="slug" />
  </section>

  <Teleport to="body">
    <div v-if="issueDialogOpen" class="modal-backdrop issue-modal-backdrop" @click.self="closeIssueDialog">
      <section
        class="modal-panel issue-modal-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="project-issue-title"
      >
        <div class="modal-heading">
          <div>
            <p class="eyebrow">Issue</p>
            <h2 id="project-issue-title">提出问题</h2>
            <p>把文档缺口、配置疑问或使用问题发给项目作者。</p>
          </div>
          <button class="icon-button" type="button" aria-label="关闭提出问题弹窗" @click="closeIssueDialog">×</button>
        </div>

        <div v-if="!session.loggedIn" class="issue-login-callout issue-modal-login">
          <div>
            <strong>登录后可以提问</strong>
            <p>注册或登录账号后，问题会带上你的身份并同步到项目作者的个人空间。</p>
          </div>
          <button class="primary-action" type="button" @click="goLoginForIssue">登录 / 注册</button>
        </div>

        <form v-else class="stack-form issue-form" @submit.prevent="submitIssue">
          <label>
            <span>问题标题</span>
            <input v-model="issueTitle" required maxlength="120" placeholder="例如：登录流程的配置示例缺失" />
          </label>
          <label>
            <span>问题描述</span>
            <textarea
              v-model="issueContent"
              required
              rows="6"
              maxlength="2000"
              placeholder="描述你遇到的问题、期望看到的说明，或复现步骤。"
            ></textarea>
          </label>
          <div class="form-actions">
            <button type="submit" :disabled="submittingIssue">{{ submittingIssue ? '提交中' : '提交问题' }}</button>
            <button class="secondary-button" type="button" @click="closeIssueDialog">取消</button>
          </div>
          <p v-if="issueError" class="status error" role="alert">{{ issueError }}</p>
          <p v-else-if="issueMessage" class="status success" aria-live="polite">{{ issueMessage }}</p>
        </form>
      </section>
    </div>
  </Teleport>
</template>
