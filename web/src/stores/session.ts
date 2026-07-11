import { defineStore } from 'pinia'
import type { LoginUser } from '../api/types'

export const useSessionStore = defineStore('session', {
  state: () => ({
    loggedIn: false,
    id: 0,
    displayName: '',
    bio: '',
    avatarUrl: '',
    username: '',
    accountType: '',
    status: '',
    roleCodes: [] as string[],
    permissionCodes: [] as string[],
  }),
  getters: {},
  actions: {
    setSession(user: LoginUser) {
      this.loggedIn = true
      this.id = user.id
      this.displayName = user.displayName
      this.bio = user.bio || ''
      this.avatarUrl = user.avatarUrl || ''
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
      this.bio = ''
      this.avatarUrl = ''
      this.username = ''
      this.accountType = ''
      this.status = ''
      this.roleCodes = []
      this.permissionCodes = []
    },
  },
})
