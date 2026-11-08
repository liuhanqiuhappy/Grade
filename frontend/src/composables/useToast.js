import { reactive } from 'vue'

// 轻量全局提示（原型 toast 区），避免为几条提示引入额外依赖
const state = reactive({ toasts: [] })
let seq = 0

export function useToast() {
  function push(title, detail = '', type = 'success', ttl = 3200) {
    const id = ++seq
    state.toasts.push({ id, title, detail, type })
    setTimeout(() => {
      const i = state.toasts.findIndex((t) => t.id === id)
      if (i > -1) state.toasts.splice(i, 1)
    }, ttl)
  }

  return {
    toasts: state.toasts,
    success: (title, detail) => push(title, detail, 'success'),
    error: (title, detail) => push(title, detail, 'error', 5000)
  }
}
