import { createRouter, createWebHistory } from 'vue-router'
import { useIdentityStore } from '../store/identity'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
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
  if (to.meta.role && store.role !== to.meta.role) {
    return { name: 'home' }
  }
  if (to.name === 'home') {
    return store.isTeacher ? { name: 'courses' } : { name: 'my-scores' }
  }
  return true
})

export default router
