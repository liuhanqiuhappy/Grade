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
          <!-- 学生端消息入口：下拉展示教师发来的补考通知，点击单条才标记已读 -->
          <div v-if="!store.isTeacher" ref="msgWrap" class="msg-wrap">
            <button
              class="msg-btn"
              type="button"
              :title="unread > 0 ? `${unread} 条未读通知` : '通知'"
              @click.stop="togglePanel"
            >
              <Icon name="bell" />
              <span v-if="unread > 0" class="msg-dot">{{ unread > 99 ? '99+' : unread }}</span>
            </button>

            <div v-if="msgOpen" class="msg-panel" @click.stop>
              <div class="msg-h">
                <b>通知</b>
                <span v-if="unread > 0" class="chip c-rose">{{ unread }} 条未读</span>
                <button v-if="unread > 0" class="msg-link" @click="readAll">全部已读</button>
              </div>

              <div v-if="msgLoading" class="msg-state">正在加载…</div>
              <div v-else-if="msgError" class="msg-state err">
                <Icon name="alert" />{{ msgError }}
                <button class="msg-link" style="margin-left: 8px" @click="loadNotices(false)">重试</button>
              </div>
              <div v-else-if="notices.length === 0" class="msg-state">暂无通知</div>
              <ul v-else class="msg-list">
                <li
                  v-for="n in notices"
                  :key="n.id"
                  class="msg-item"
                  :class="{ unread: !n.read }"
                  @click="openNotice(n)"
                >
                  <div class="msg-item-top">
                    <span v-if="!n.read" class="msg-dot-mini"></span>
                    <span class="msg-title">{{ n.title }}</span>
                    <span class="chip c-slate" style="margin-left: auto">{{ n.courseName }}</span>
                  </div>
                  <div class="msg-meta">{{ fmt(n.createdAt) }} 发送</div>
                  <template v-if="expandedId === n.id">
                    <div v-if="n.examTime" class="msg-kv"><span>补考时间</span>{{ fmt(n.examTime) }}</div>
                    <div v-if="n.examPlace" class="msg-kv"><span>地点</span>{{ n.examPlace }}</div>
                    <div v-if="n.examScope" class="msg-kv"><span>范围</span>{{ n.examScope }}</div>
                    <div class="msg-body">{{ n.content }}</div>
                  </template>
                  <div v-else class="msg-meta" style="margin-top: 4px">点击查看详情</div>
                </li>
              </ul>

              <div class="msg-f">仅展示最近 20 条 · 站内通知，需登录查看</div>
            </div>
          </div>

          <span v-if="ctx.hasCourse" class="hchip cur" :title="ctx.label">
            <Icon name="book" /> 当前课程：{{ ctx.name }}
          </span>
          <span class="hchip">
            <Icon name="account" /> {{ store.displayName }}
            <span class="rolechip" :class="store.isTeacher ? 'role-t' : 'role-s'">
              {{ store.isTeacher ? '教师' : '学生' }}
            </span>
          </span>
          <router-link class="btn-exit" to="/change-password" style="text-decoration: none">
            <Icon name="lock" /> 改口令
          </router-link>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { useIdentityStore } from '../store/identity'
import { useCourseContextStore } from '../store/courseContext'
import { useToast } from '../composables/useToast'
import { ping } from '../api/identity'
import { markAllNoticesRead, markNoticeRead, myNotices } from '../api/notice'

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

/* ============ 学生端消息下拉 ============ */
const msgWrap = ref(null)
const msgOpen = ref(false)
const msgLoading = ref(false)
const msgError = ref('')
const notices = ref([])
const unread = ref(0)
const expandedId = ref(null)
let pollTimer = null

async function loadNotices(showLoading = true) {
  if (showLoading) msgLoading.value = true
  msgError.value = ''
  try {
    const data = await myNotices()
    unread.value = data.unreadCount || 0
    notices.value = data.items || []
  } catch (e) {
    // 静默轮询失败不清空列表、也不把未读数归零：让学生误以为没通知是最恶性的失败
    if (showLoading) msgError.value = e.message || '通知加载失败'
  } finally {
    msgLoading.value = false
  }
}

function togglePanel() {
  msgOpen.value = !msgOpen.value
  if (msgOpen.value) loadNotices(false)
}

function openNotice(n) {
  expandedId.value = expandedId.value === n.id ? null : n.id
  if (!n.read) {
    n.read = true
    unread.value = Math.max(0, unread.value - 1)
    // 已读是教师的真实送达依据，失败要回滚，不能前端假装已读
    markNoticeRead(n.id).catch(() => {
      n.read = false
      unread.value += 1
    })
  }
}

async function readAll() {
  try {
    await markAllNoticesRead()
    notices.value.forEach((n) => { n.read = true })
    unread.value = 0
  } catch (e) {
    msgError.value = e.message || '操作失败'
  }
}

function fmt(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}月${d.getDate()}日 ${p(d.getHours())}:${p(d.getMinutes())}`
}

function onDocClick(e) {
  if (!msgOpen.value) return
  if (msgWrap.value && !msgWrap.value.contains(e.target)) msgOpen.value = false
}

function onKeydown(e) {
  if (e.key === 'Escape') msgOpen.value = false
}

function onFocus() {
  if (!store.isTeacher) loadNotices(false)
}

/* ============ 顶栏杂项 ============ */
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
  if (!store.isTeacher) {
    loadNotices(false)
    pollTimer = setInterval(() => loadNotices(false), 30000)
    document.addEventListener('click', onDocClick)
    document.addEventListener('keydown', onKeydown)
    window.addEventListener('focus', onFocus)
  }
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('focus', onFocus)
})
</script>
