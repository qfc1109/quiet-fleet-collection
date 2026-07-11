<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser, logout } from './api/auth'
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
const useWidePage = computed(() => route.name === 'space')

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

onMounted(loadCurrentUser)
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
          <RouterLink class="account-badge" to="/space">{{ accountLabel }}</RouterLink>
          <button class="nav-button" type="button" @click="handleLogout">退出</button>
        </template>
        <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
      </nav>
    </header>

    <main class="page-main" :class="{ 'page-main-wide': useWidePage }">
      <RouterView />
    </main>
  </div>
</template>
