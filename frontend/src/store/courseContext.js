import { defineStore } from 'pinia'

const KEY = 'gm.currentCourse'

// 当前课程上下文：跨页面常驻，刷新不丢失（原型 P0-04）
export const useCourseContextStore = defineStore('courseContext', {
  state: () => ({
    id: null,
    name: '',
    term: ''
  }),
  getters: {
    hasCourse: (s) => !!s.id,
    label: (s) => (s.id ? `${s.name}（${s.term}）` : '未选定课程')
  },
  actions: {
    restore() {
      try {
        const saved = JSON.parse(localStorage.getItem(KEY) || 'null')
        if (saved && saved.id) this.apply(saved)
      } catch (e) {
        localStorage.removeItem(KEY)
      }
    },
    apply(course) {
      this.id = course.id
      this.name = course.name
      this.term = course.term
      localStorage.setItem(KEY, JSON.stringify({ id: this.id, name: this.name, term: this.term }))
    },
    clear() {
      this.id = null
      this.name = ''
      this.term = ''
      localStorage.removeItem(KEY)
    }
  }
})
