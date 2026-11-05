import { createRouter, createWebHistory } from 'vue-router'
import { useIdentityStore } from '@/store/useIdentity'
import { getMe } from '@/api/identity'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/welcome',
    name: 'welcome',
    component: () => import('@/views/Welcome.vue'),
    meta: { requiresAuth: true }
  },
  {
    // 带导航壳的应用布局：仅教师可进入（模块二班级/学生管理）
    path: '/app',
    component: () => import('@/views/AppShell.vue'),
    meta: { requiresAuth: true, requiresTeacher: true },
    children: [
      // ===== 模块二：班级与学生 =====
      {
        path: 'classes',
        name: 'class-list',
        component: () => import('@/views/class/ClassList.vue')
      },
      {
        path: 'classes/:classId/students',
        name: 'class-students',
        component: () => import('@/views/class/ClassStudents.vue')
      },
      {
        path: 'students',
        name: 'student-search',
        component: () => import('@/views/student/StudentSearch.vue')
      },
      {
        path: 'students/:studentId',
        name: 'student-detail',
        component: () => import('@/views/student/StudentDetail.vue')
      },
      // ===== 模块三：课程管理（M3A）=====
      {
        path: 'courses',
        name: 'course-list',
        component: () => import('@/views/course/CourseList.vue')
      }
      // ===== 后续模块在此追加 =====
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const identityStore = useIdentityStore()

  if (to.meta.requiresAuth) {
    try {
      const res = await getMe()
      identityStore.setUser(res.data)

      // 教师专用页面：学生身份一律挡回欢迎页（菜单已隐藏，这里做兜底）
      if (to.meta.requiresTeacher && identityStore.role !== 'TEACHER') {
        return next('/welcome')
      }
      return next()
    } catch (err) {
      // 未登录/会话过期：统一回登录页，由守卫处理，页面不各自写跳转
      identityStore.clearUser()
      return next('/login')
    }
  }
  return next()
})

export default router
