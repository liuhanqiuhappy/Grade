<template>
  <!-- 模块二/后续模块的导航壳（仅 TEACHER 可见，守卫在路由 meta.requiresTeacher 判定） -->
  <div>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary mb-3">
      <div class="container-fluid">
        <span class="navbar-brand">成绩管理系统</span>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMain"
                aria-controls="navMain" aria-expanded="false" aria-label="切换导航">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navMain">
          <ul class="navbar-nav me-auto">
            <!-- ===== 模块二：班级与学生 ===== -->
            <li class="nav-item">
              <router-link class="nav-link" :class="{ active: isActive('/app/classes') }" to="/app/classes">班级管理</router-link>
            </li>
            <li class="nav-item">
              <router-link class="nav-link" :class="{ active: isActive('/app/students') }" to="/app/students">学生查询</router-link>
            </li>
            <!-- ===== 模块三：课程管理 ===== -->
            <li class="nav-item">
              <router-link class="nav-link" :class="{ active: isActive('/app/courses') }" to="/app/courses">课程管理</router-link>
            </li>
          </ul>
          <div class="d-flex align-items-center text-white">
            <span class="me-3">
              {{ identityStore.displayName || identityStore.username }}
              <span class="badge bg-light text-primary ms-1">{{ roleLabel }}</span>
            </span>
            <button class="btn btn-outline-light btn-sm" @click="handleLogout">退出登录</button>
          </div>
        </div>
      </div>
    </nav>

    <div class="container-fluid">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIdentityStore } from '@/store/useIdentity'
import { logout } from '@/api/identity'

const identityStore = useIdentityStore()
const route = useRoute()
const router = useRouter()

const roleLabel = computed(() =>
  identityStore.role === 'TEACHER' ? '教师' : identityStore.role === 'STUDENT' ? '学生' : ''
)

// 高亮当前导航（考虑子路由前缀匹配）
function isActive(prefix) {
  return route.path === prefix || route.path.startsWith(prefix + '/')
}

const handleLogout = async () => {
  try {
    await logout()
  } catch (e) {
    // 退出幂等，失败也继续清空本地态
  }
  identityStore.clearUser()
  router.replace('/login')
}
</script>
