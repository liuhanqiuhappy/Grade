import { defineStore } from 'pinia'
import { me as fetchMe, login as doLogin, logout as doLogout } from '../api/identity'

// 登录态（凭据校验、会话全部在后端，前端只保存展示用身份）
export const useIdentityStore = defineStore('identity', {
  state: () => ({
    userId: null,
    username: '',
    displayName: '',
    role: '',
    loaded: false
  }),
  getters: {
    isLoggedIn: (s) => s.loaded && !!s.userId,
    isTeacher: (s) => s.role === 'TEACHER'
  },
  actions: {
    async login(username, password) {
      const user = await doLogin({ username, password })
      this.apply(user)
    },
    async restore() {
      try {
        this.apply(await fetchMe())
      } catch (e) {
        this.reset()
      } finally {
        this.loaded = true
      }
    },
    async logout() {
      try {
        await doLogout()
      } finally {
        this.reset()
      }
    },
    apply(user) {
      this.userId = user.userId
      this.username = user.username
      this.displayName = user.displayName
      this.role = user.role
      this.loaded = true
    },
    reset() {
      this.userId = null
      this.username = ''
      this.displayName = ''
      this.role = ''
      this.loaded = true
    }
  }
})
