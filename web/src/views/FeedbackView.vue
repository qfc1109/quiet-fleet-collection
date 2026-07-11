<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createSiteFeedback } from '../api/public'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const feedbackTitle = ref('')
const feedbackContent = ref('')
const feedbackError = ref('')
const feedbackMessage = ref('')
const submittingFeedback = ref(false)

function goLoginForFeedback() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function submitFeedback() {
  if (!session.loggedIn) {
    goLoginForFeedback()
    return
  }
  feedbackError.value = ''
  feedbackMessage.value = ''
  submittingFeedback.value = true
  try {
    await createSiteFeedback(feedbackTitle.value, feedbackContent.value)
    feedbackTitle.value = ''
    feedbackContent.value = ''
    feedbackMessage.value = '反馈已提交，后台管理员会看到。'
  } catch (caught) {
    feedbackError.value = caught instanceof Error ? caught.message : '反馈提交失败'
  } finally {
    submittingFeedback.value = false
  }
}
</script>

<template>
  <section class="page-section feedback-hero">
    <div>
      <p class="eyebrow">Feedback</p>
      <h1>主站反馈</h1>
      <p class="lead">反馈主站体验、导航、阅读、登录或任何反人类的地方。</p>
    </div>
    <div class="feedback-note" aria-label="反馈处理说明">
      <strong>直接说问题</strong>
      <p>不用包装成正式需求，描述你被卡住的地方就行。</p>
    </div>
  </section>

  <section class="page-section issue-panel feedback-panel" aria-labelledby="site-feedback-title">
    <div class="section-heading">
      <div>
        <h2 id="site-feedback-title">反馈入口</h2>
        <p>提交后会进入后台反馈列表，管理员可以集中查看。</p>
      </div>
    </div>

    <div v-if="!session.loggedIn" class="issue-login-callout feedback-login-callout">
      <div>
        <strong>登录后可以反馈</strong>
        <p>注册或登录账号后，反馈会带上你的身份，方便后续沟通和定位。</p>
      </div>
      <button class="primary-action" type="button" @click="goLoginForFeedback">登录 / 注册</button>
    </div>

    <form v-else class="stack-form issue-form feedback-form" @submit.prevent="submitFeedback">
      <label>
        <span>反馈标题</span>
        <input v-model="feedbackTitle" required maxlength="120" placeholder="例如：主站导航太难找" />
      </label>
      <label>
        <span>反馈描述</span>
        <textarea
          v-model="feedbackContent"
          required
          rows="7"
          maxlength="2000"
          placeholder="描述你遇到的问题、感受、页面位置，或者你希望它变成什么样。"
        ></textarea>
      </label>
      <div class="form-actions">
        <button type="submit" :disabled="submittingFeedback">
          {{ submittingFeedback ? '提交中' : '提交反馈' }}
        </button>
      </div>
      <p v-if="feedbackError" class="status error" role="alert">{{ feedbackError }}</p>
      <p v-else-if="feedbackMessage" class="status success" aria-live="polite">{{ feedbackMessage }}</p>
    </form>
  </section>
</template>
