<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser, logout } from './api/auth'
import { registerSessionInvalidationHandler } from './api/http'
import { useSessionStore } from './stores/session'

const navItems = [
  { to: '/', label: '首页' },
  { to: '/explore', label: '项目广场' },
  { to: '/feedback', label: '反馈' },
]

const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const accountLabel = computed(() => session.displayName || session.username)
const visibleNavItems = computed(() => navItems)
const useWidePage = computed(() => route.path === '/space' || route.path.startsWith('/space/'))
const showElsewhereLoginDialog = ref(false)
const sessionInvalidatedElsewhere = ref(false)

const unregisterSessionInvalidationHandler = registerSessionInvalidationHandler(() => {
  if (sessionInvalidatedElsewhere.value) {
    return
  }
  sessionInvalidatedElsewhere.value = true
  session.clearSession()
  showElsewhereLoginDialog.value = true
})

watch(() => session.loggedIn, (loggedIn) => {
  if (loggedIn) {
    sessionInvalidatedElsewhere.value = false
    showElsewhereLoginDialog.value = false
  }
})

async function loadCurrentUser() {
  try {
    const user = await getCurrentUser()
    session.setSession(user)
  } catch {
    session.clearSession()
  }
}

async function handleLogout() {
  try {
    await logout()
  } finally {
    session.clearSession()
    router.push('/login')
  }
}

function confirmElsewhereLogin() {
  showElsewhereLoginDialog.value = false
  router.replace({ path: '/login', query: { reason: 'elsewhere' }})
}

onMounted(loadCurrentUser)
onUnmounted(unregisterSessionInvalidationHandler)
</script>

<template>
  <div class="app-shell">
    <header class="top-bar">
      <RouterLink class="brand" to="/" aria-label="返回首页">
        <span class="brand-mark">QFC</span>
      </RouterLink>

      <nav class="main-nav" aria-label="主导航">
        <RouterLink v-for="item in visibleNavItems" :key="item.to" :to="item.to">
          {{ item.label }}
        </RouterLink>
        <template v-if="session.loggedIn">
          <RouterLink class="account-badge" to="/space/account">{{ accountLabel }}</RouterLink>
          <button class="nav-button" type="button" @click="handleLogout">退出</button>
        </template>
        <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
      </nav>
    </header>

    <main class="page-main" :class="{ 'page-main-wide': useWidePage }">
      <RouterView />
    </main>

    <div v-if="showElsewhereLoginDialog" class="modal-backdrop" role="presentation">
      <section class="modal-panel" role="alertdialog" aria-modal="true" aria-labelledby="elsewhere-login-title">
        <div class="modal-heading">
          <h2 id="elsewhere-login-title">登录状态已失效</h2>
        </div>
        <p>账号已在其他设备或位置登录，请重新登录。</p>
        <button class="primary-action" type="button" @click="confirmElsewhereLogin">重新登录</button>
      </section>
    </div>
  </div>
</template>
