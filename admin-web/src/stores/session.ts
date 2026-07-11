import { defineStore } from 'pinia'
import type { LoginUser } from '../api/types'

export const useSessionStore = defineStore('session', {
  state: () => ({
    loggedIn: false,
    id: 0,
    displayName: '',
    username: '',
    accountType: '',
    status: '',
    roleCodes: [] as string[],
    permissionCodes: [] as string[],
  }),
  getters: {
    canViewUsers: (state) =>
      state.permissionCodes.includes('USER_VIEW') || state.permissionCodes.includes('USER_MANAGE'),
    canManageIssues: (state) => state.permissionCodes.includes('ISSUE_MANAGE'),
    canEnterAdmin: (state) =>
      state.accountType === 'ADMIN' &&
      (state.permissionCodes.includes('USER_VIEW') ||
        state.permissionCodes.includes('USER_MANAGE') ||
        state.permissionCodes.includes('ROLE_VIEW') ||
        state.permissionCodes.includes('ROLE_MANAGE') ||
        state.permissionCodes.includes('ISSUE_MANAGE')),
    canManageRoles: (state) => state.permissionCodes.includes('ROLE_MANAGE'),
  },
  actions: {
    setSession(user: LoginUser) {
      this.loggedIn = true
      this.id = user.id
      this.displayName = user.displayName
      this.username = user.username
      this.accountType = user.accountType || ''
      this.status = user.status || ''
      this.roleCodes = user.roleCodes || []
      this.permissionCodes = user.permissionCodes || []
    },
    clearSession() {
      this.loggedIn = false
      this.id = 0
      this.displayName = ''
      this.username = ''
      this.accountType = ''
      this.status = ''
      this.roleCodes = []
      this.permissionCodes = []
    },
  },
})
