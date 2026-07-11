<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '../api/auth'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const displayName = ref('')
const error = ref('')
const submitting = ref(false)
const loginNotice = computed(() => loginNoticeMessage(route.query.reason))

async function submitLogin() {
  error.value = ''
  submitting.value = true
  try {
    const user = mode.value === 'login'
      ? await login(username.value, password.value)
      : await register(username.value, password.value, displayName.value)
    session.setSession(user)
    const fallback = '/space'
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : fallback
    router.push(redirect)
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : '操作失败'
  } finally {
    submitting.value = false
  }
}

function loginNoticeMessage(reason: unknown) {
  if (reason === 'elsewhere') {
    return '账号已在其他设备或位置登录，请重新登录'
  }
  if (reason === 'expired') {
    return '登录已过期，请重新登录'
  }
  return ''
}
</script>

<template>
  <section class="auth-layout">
    <div class="auth-panel">
      <p class="eyebrow">Account</p>
      <h1>{{ mode === 'login' ? '用户登录' : '注册账号' }}</h1>

      <div class="auth-switch" aria-label="账号操作">
        <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <form class="login-form" @submit.prevent="submitLogin">
        <label>
          <span>账号</span>
          <input v-model="username" autocomplete="username" />
        </label>

        <label v-if="mode === 'register'">
          <span>昵称</span>
          <input v-model="displayName" autocomplete="nickname" />
          <small class="field-hint">昵称会显示在个人中心和公开内容中，登录仍使用账号。</small>
        </label>

        <label>
          <span>密码</span>
          <input v-model="password" type="password" autocomplete="current-password" />
        </label>

        <button type="submit">{{ mode === 'login' ? '登录' : '注册并登录' }}</button>
        <p v-if="error" class="status error">{{ error }}</p>
        <p v-else-if="loginNotice" class="status warning">{{ loginNotice }}</p>
        <p v-else-if="submitting" class="status">{{ mode === 'login' ? '正在登录' : '正在注册' }}</p>
      </form>
    </div>
  </section>
</template>
