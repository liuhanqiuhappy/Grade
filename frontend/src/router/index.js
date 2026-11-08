import { createRouter, createWebHistory } from 'vue-router'
import { useIdentityStore } from '../store/identity'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  // 改密页独立于 AppShell：强制改密时不该加载导航与轮询，也避免未改密时其他接口报 1004
  { path: '/change-password', name: 'change-password', component: () => import('../views/account/ChangePassword.vue') },
  {
    path: '/',
    component: () => import('../views/AppShell.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/Home.vue') },
      { path: 'courses', name: 'courses', component: () => import('../views/course/CourseList.vue'), meta: { role: 'TEACHER' } },
      { path: 'catalog', name: 'catalog', component: () => import('../views/catalog/SharedCatalog.vue'), meta: { role: 'TEACHER' } },
      { path: 'courses/:id/members', name: 'course-members', component: () => import('../views/course/CourseMembers.vue'), meta: { role: 'TEACHER' } },
      { path: 'courses/:id/scores', name: 'score-entry', component: () => import('../views/course/ScoreEntry.vue'), meta: { role: 'TEACHER' } },
      { path: 'courses/:id/confirm', name: 'confirm-stats', component: () => import('../views/course/ConfirmStats.vue'), meta: { role: 'TEACHER' } },
      { path: 'courses/:id/makeup', name: 'makeup-notice', component: () => import('../views/course/MakeupNotice.vue'), meta: { role: 'TEACHER' } },
      { path: 'my-scores', name: 'my-scores', component: () => import('../views/student/MyScores.vue'), meta: { role: 'STUDENT' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const store = useIdentityStore()
  if (!store.loaded) {
    await store.restore()
  }
  if (to.name === 'login') {
    return store.isLoggedIn ? { name: 'home' } : true
  }
  if (!store.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  // 未完成强制改密（1004）：除改密页外一律拉回改密页，防止拿着初始口令直接看数据
  if (store.mustChangePassword && to.name !== 'change-password') {
    return { name: 'change-password' }
  }
  if (to.meta.role && store.role !== to.meta.role) {
    return { name: 'home' }
  }
  if (to.name === 'home') {
    return store.isTeacher ? { name: 'courses' } : { name: 'my-scores' }
  }
  return true
})

export default router
