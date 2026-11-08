// 「记住密码」是纯前端本地行为：只写 localStorage，登录校验仍全程走后端
const KEY = 'grade_remembered_credential'

export const rememberedCredential = {
  get() {
    try {
      return JSON.parse(localStorage.getItem(KEY)) || null
    } catch (e) {
      return null
    }
  },
  save(username, password) {
    localStorage.setItem(KEY, JSON.stringify({ username, password }))
  },
  clear() {
    localStorage.removeItem(KEY)
  }
}
