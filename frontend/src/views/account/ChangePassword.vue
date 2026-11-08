<template>
  <div>
    <div class="cp-top">
      <span class="logo" style="width: 40px; height: 40px"><Icon name="school" /></span>
      <div>
        <h1 style="font-size: 16px; font-weight: 600; color: #0f172a">高校学生成绩管理系统</h1>
        <p style="font-size: 12px; color: #64748b">修改口令 · 身份由服务端会话判定</p>
      </div>
    </div>

    <main style="display: flex; justify-content: center; padding: 36px 24px 48px">
      <section class="card" style="width: 460px; padding: 28px 28px 24px">
        <h2 style="font-size: 20px; font-weight: 600; color: #0f172a">修改口令</h2>
        <p style="font-size: 12px; color: #64748b; margin-top: 4px">
          账号：{{ store.username }}（{{ store.isTeacher ? '教师' : '学生' }}）
        </p>

        <div v-if="store.mustChangePassword" class="banner b-amber mt-12">
          <Icon name="alert" />
          <span>当前使用的是初始口令或教师重置的临时口令，必须先设置新口令才能继续使用系统。</span>
        </div>

        <form @submit.prevent="onSubmit">
          <div style="margin-top: 18px">
            <label class="label" for="oldPassword">原口令<span class="req">*</span></label>
            <input
              id="oldPassword"
              v-model="oldPassword"
              class="input"
              type="password"
              autocomplete="current-password"
              placeholder="初始口令或被重置的临时口令"
            />
          </div>

          <div style="margin-top: 16px">
            <label class="label" for="newPassword">新口令<span class="req">*</span></label>
            <input
              id="newPassword"
              v-model="newPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              placeholder="8~32 位，需同时包含字母和数字"
            />
            <p class="hint">8~32 位，不能含空格，需同时包含字母和数字，且不能与原口令相同。</p>
          </div>

          <div style="margin-top: 16px">
            <label class="label" for="confirmPassword">确认新口令<span class="req">*</span></label>
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              class="input"
              type="password"
              autocomplete="new-password"
              placeholder="再输入一次新口令"
            />
            <p v-if="fieldError.confirmPassword" class="hint err">{{ fieldError.confirmPassword }}</p>
          </div>

          <div v-if="error" class="banner b-rose mt-12">
            <Icon name="alert" />
            <span>{{ error }}</span>
          </div>
          <div v-if="done" class="banner b-green mt-12">
            <Icon name="check-circle" />
            <span>口令已更新，请牢记新口令。正在返回…</span>
          </div>

          <button class="btn btn-p" style="width: 100%; margin-top: 20px; height: 40px" :disabled="loading || done">
            <Icon name="lock" /> {{ loading ? '提交中…' : '确认修改' }}
          </button>
        </form>

        <div class="cp-foot">
          <button class="link-btn" type="button" @click="onLogout">退出登录</button>
          <span class="muted">忘记口令请联系任课教师重置</span>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../../components/Icon.vue'
import { useIdentityStore } from '../../store/identity'
import { changePassword } from '../../api/identity'

const router = useRouter()
const store = useIdentityStore()

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const done = ref(false)
const loading = ref(false)
const fieldError = reactive({ confirmPassword: '' })

// 与后端 PasswordPolicy 同规则，仅做前置提示；最终以后端校验为准
function precheck() {
  error.value = ''
  fieldError.confirmPassword = ''
  if (!oldPassword.value) {
    error.value = '请输入原口令'
    return false
  }
  if (!newPassword.value) {
    error.value = '请输入新口令'
    return false
  }
  if (newPassword.value.length < 8 || newPassword.value.length > 32) {
    error.value = '新口令长度需为 8~32 位'
    return false
  }
  if (!/[A-Za-z]/.test(newPassword.value) || !/[0-9]/.test(newPassword.value)) {
    error.value = '新口令需同时包含字母和数字'
    return false
  }
  if (newPassword.value === oldPassword.value) {
    error.value = '新口令不能与原口令相同'
    return false
  }
  if (confirmPassword.value !== newPassword.value) {
    fieldError.confirmPassword = '两次输入的新口令不一致'
    return false
  }
  return true
}

async function onSubmit() {
  if (!precheck()) return
  loading.value = true
  try {
    await changePassword({ oldPassword: oldPassword.value, newPassword: newPassword.value })
    // 后端已清除强制改密标记，前端同步，路由守卫才会放行其他页面
    store.mustChangePassword = false
    done.value = true
    setTimeout(() => {
      router.replace(store.isTeacher ? '/courses' : '/my-scores')
    }, 600)
  } catch (e) {
    error.value = e.message || '修改失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function onLogout() {
  await store.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.cp-top {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 0;
}
.cp-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  font-size: 11px;
}
.link-btn {
  border: none;
  background: none;
  padding: 0;
  font-size: 12px;
  color: #4f46e5;
  cursor: pointer;
}
.link-btn:hover {
  text-decoration: underline;
}
</style>
