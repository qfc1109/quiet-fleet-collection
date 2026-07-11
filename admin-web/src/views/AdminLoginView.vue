<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Lock, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { login, logout } from '../api/auth'
import { useSessionStore } from '../stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const submitting = ref(false)
const loginNotice = computed(() => loginNoticeMessage(route.query.reason))
const form = reactive({
  username: '',
  password: '',
})

async function submitLogin() {
  submitting.value = true
  try {
    const user = await login(form.username, form.password)
    session.setSession(user)
    if (!session.canEnterAdmin) {
      await logout()
      session.clearSession()
      ElMessage.error('当前账号不是后台管理员')
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (caught) {
    ElMessage.error(caught instanceof Error ? caught.message : '登录失败')
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

onMounted(() => {
  if (loginNotice.value) {
    ElMessage.warning(loginNotice.value)
  }
})
</script>

<template>
  <section class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-heading">
        <span class="brand-mark large">QFC</span>
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>轻帆集后台</h1>
          <p>后台管理登录</p>
        </div>
      </div>

      <el-form class="login-form" label-position="top" :model="form" @submit.prevent="submitLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            size="large"
            type="password"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button class="login-submit" type="primary" size="large" :loading="submitting" @click="submitLogin">
          进入后台
        </el-button>
      </el-form>
    </el-card>
  </section>
</template>
