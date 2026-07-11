<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ChatDotRound, Key, SwitchButton, User, UserFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser, logout } from './api/auth'
import { useSessionStore } from './stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const accountLabel = computed(() => session.displayName || session.username)
const showAdminShell = computed(() => route.name !== 'login')
const routeTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : '后台管理'))
const menuItems = computed(() => {
  const items = []
  if (session.canViewUsers) {
    items.push({ path: '/site-users', label: '网站用户', icon: User })
  }
  if (session.canManageRoles) {
    items.push(
      { path: '/admin-users', label: '后台管理员', icon: UserFilled },
      { path: '/roles', label: '角色权限', icon: Key },
    )
  }
  if (session.canManageIssues) {
    items.push({ path: '/feedback', label: '主站反馈', icon: ChatDotRound })
  }
  return items
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

onMounted(loadCurrentUser)
</script>

<template>
  <div class="app-root">
    <el-container v-if="showAdminShell" class="admin-shell">
      <el-aside class="admin-aside" width="240px">
        <RouterLink class="brand" to="/site-users" aria-label="后台首页">
          <span class="brand-mark">QFC</span>
          <span>
            <strong>轻帆集后台</strong>
            <small>Admin Console</small>
          </span>
        </RouterLink>

        <el-menu class="admin-menu" :default-active="route.path" router>
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container class="admin-main">
        <el-header class="workspace-bar" height="72px">
          <div class="workspace-title">
            <h1>{{ routeTitle }}</h1>
          </div>

          <el-space wrap>
            <el-tag v-if="session.loggedIn" effect="light" size="large">{{ accountLabel }}</el-tag>
            <el-button v-if="session.loggedIn" :icon="SwitchButton" @click="handleLogout">退出</el-button>
          </el-space>
        </el-header>

        <el-main class="workspace-content">
          <RouterView />
        </el-main>
      </el-container>
    </el-container>

    <main v-else class="auth-main">
      <RouterView />
    </main>
  </div>
</template>
