import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser } from '../api/auth'
import { ApiError } from '../api/types'
import { useSessionStore } from '../stores/session'
import { canEnterSpace } from './spaceAccess'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/explore', name: 'explore', component: () => import('../views/ExploreView.vue') },
    { path: '/feedback', name: 'feedback', component: () => import('../views/FeedbackView.vue') },
    { path: '/u', redirect: '/explore' },
    { path: '/p/:slug', name: 'project', component: () => import('../views/ProjectView.vue') },
    { path: '/p/:slug/issues/new', name: 'project-issue-new', component: () => import('../views/ProjectView.vue') },
    { path: '/p/:slug/files/:fileId', name: 'file-preview', component: () => import('../views/FilePreviewView.vue') },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/space', redirect: '/space/account' },
    { path: '/space/account', name: 'space-account', component: () => import('../views/SpaceView.vue') },
    { path: '/space/projects', name: 'space-projects', component: () => import('../views/SpaceView.vue') },
    { path: '/space/projects/:projectId/files', name: 'space-project-files', component: () => import('../views/SpaceView.vue') },
    { path: '/space/projects/:projectId/edit', name: 'space-project-edit', component: () => import('../views/SpaceView.vue') },
    {
      path: '/space/projects/:projectId/files/:fileId/preview',
      name: 'space-project-file-preview',
      component: () => import('../views/FilePreviewView.vue'),
    },
    {
      path: '/space/projects/:projectId/visitor/:slug',
      name: 'space-project-visitor',
      component: () => import('../views/ProjectView.vue'),
    },
    {
      path: '/space/projects/:projectId/visitor/:slug/issues/new',
      name: 'space-project-visitor-issue-new',
      component: () => import('../views/ProjectView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const protectedRoute = to.path === '/space' || to.path.startsWith('/space/')
  if (!protectedRoute) {
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
  if (canEnterSpace(session)) {
    return true
  }
  return { path: '/login', query: { redirect: to.fullPath } }
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
