<template>
  <div>
    <header class="hdr">
      <div class="hdr-in">
        <div class="brand">
          <span class="logo"><Icon name="school" /></span>
          <div>
            <h1>高校学生成绩管理系统</h1>
            <p>{{ store.isTeacher ? '教师端' : '学生端' }}</p>
          </div>
        </div>

        <nav class="nav">
          <router-link
            v-for="item in navItems"
            :key="item.label"
            :to="item.to"
            :class="{ on: item.active }"
          >{{ item.label }}</router-link>
        </nav>

        <div class="hdr-right">
          <span v-if="ctx.hasCourse" class="hchip cur" :title="ctx.label">
            <Icon name="book" /> 当前课程：{{ ctx.name }}
          </span>
          <span class="hchip">
            <Icon name="account" /> {{ store.displayName }}
            <span class="rolechip" :class="store.isTeacher ? 'role-t' : 'role-s'">
              {{ store.isTeacher ? '教师' : '学生' }}
            </span>
          </span>
          <button class="btn-exit" type="button" @click="onLogout">
            <Icon name="exit" /> 退出
          </button>
        </div>
      </div>
    </header>

    <main class="main">
      <div v-if="offline" class="banner b-rose mt-12">
        <Icon name="alert" />
        <span>
          <b>无法连接后端服务（localhost:8080）。</b>
          页面已加载，但登录与数据请求都会失败。请先启动后端：在 <code>backend/</code> 下设置
          <code>DB_PASSWORD</code> 后执行 <code>mvn spring-boot:run</code>。
        </span>
        <button class="btn btn-o btn-sm ml-auto" @click="checkBackend">重试</button>
      </div>
      <router-view />
    </main>

    <div class="toast">
      <div v-for="t in toasts" :key="t.id" class="t">
        <Icon :name="t.type === 'error' ? 'alert' : 'check-circle'" :style="{ color: t.type === 'error' ? '#e11d48' : '#059669' }" />
        <div>
          <div class="tt">{{ t.title }}</div>
          <div v-if="t.detail" class="td">{{ t.detail }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { useIdentityStore } from '../store/identity'
import { useCourseContextStore } from '../store/courseContext'
import { useToast } from '../composables/useToast'
import { ping } from '../api/identity'

const route = useRoute()
const router = useRouter()
const store = useIdentityStore()
const ctx = useCourseContextStore()
const { toasts } = useToast()

const offline = ref(false)

const navItems = computed(() => {
  if (!store.isTeacher) {
    return [{ label: '我的成绩', to: '/my-scores', active: route.name === 'my-scores' }]
  }
  const id = ctx.id
  return [
    { label: '我的课程', to: '/courses', active: route.name === 'courses' },
    { label: '共享目录', to: '/catalog', active: route.name === 'catalog' },
    { label: '课程成员', to: id ? `/courses/${id}/members` : '/courses', active: route.name === 'course-members' },
    { label: '成绩录入', to: id ? `/courses/${id}/scores` : '/courses', active: route.name === 'score-entry' },
    { label: '确认与统计', to: id ? `/courses/${id}/confirm` : '/courses', active: route.name === 'confirm-stats' }
  ]
})

async function checkBackend() {
  try {
    await ping()
    offline.value = false
  } catch (e) {
    offline.value = true
  }
}

async function onLogout() {
  await store.logout()
  ctx.clear()
  router.push({ name: 'login' })
}

onMounted(() => {
  ctx.restore()
  checkBackend()
})
</script>
