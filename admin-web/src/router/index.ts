import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser } from '../api/auth'
import { ApiError } from '../api/types'
import { useSessionStore } from '../stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/site-users' },
    {
      path: '/site-users',
      name: 'site-users',
      component: () => import('../views/AdminDashboardView.vue'),
      props: { section: 'site-users' },
      meta: { title: '网站用户', description: '普通网站用户账号' },
    },
    {
      path: '/admin-users',
      name: 'admin-users',
      component: () => import('../views/AdminDashboardView.vue'),
      props: { section: 'admin-users' },
      meta: { title: '后台管理员', description: '后台登录账号与角色' },
    },
    {
      path: '/roles',
      name: 'roles',
      component: () => import('../views/AdminDashboardView.vue'),
      props: { section: 'roles' },
      meta: { title: '角色权限', description: '后台角色与权限配置', requiresRoleManage: true },
    },
    {
      path: '/feedback',
      name: 'feedback',
      component: () => import('../views/AdminDashboardView.vue'),
      props: { section: 'feedback' },
      meta: { title: '主站反馈', description: '主站体验与问题反馈', requiresIssueManage: true },
    },
    { path: '/login', name: 'login', component: () => import('../views/AdminLoginView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  if (to.path === '/login') {
    return true
  }
  const session = useSessionStore()
  if (!session.loggedIn) {
    try {
      const user = await getCurrentUser()
      session.setSession(user)
    } catch (caught) {
      session.clearSession()
      return { path: '/login', query: { redirect: to.fullPath, reason: authRedirectReason(caught) } }
    }
  }
  if (!session.canEnterAdmin) {
    return { path: '/login' }
  }
  if (to.name === 'site-users' && !session.canViewUsers) {
    return session.canManageIssues ? { path: '/feedback' } : { path: '/login' }
  }
  if ((to.name === 'admin-users' || to.meta.requiresRoleManage) && !session.canManageRoles) {
    return { path: '/site-users' }
  }
  if (to.meta.requiresIssueManage && !session.canManageIssues) {
    return session.canViewUsers ? { path: '/site-users' } : { path: '/login' }
  }
  return true
})

function authRedirectReason(caught: unknown) {
  if (caught instanceof ApiError) {
    if (caught.code === 'ACCOUNT_LOGGED_IN_ELSEWHERE') {
      return 'elsewhere'
    }
    if (caught.code === 'UNAUTHORIZED') {
      return 'expired'
    }
  }
  return 'required'
}

export default router
