<template>
  <div>
    <div class="login-top">
      <span class="logo" style="width: 40px; height: 40px"><Icon name="school" /></span>
      <div>
        <h1 style="font-size: 16px; font-weight: 600; color: #0f172a">高校学生成绩管理系统</h1>
        <p style="font-size: 12px; color: #64748b">登录 · 角色由服务端判定</p>
      </div>
    </div>

    <main style="display: flex; justify-content: center; padding: 36px 24px 48px">
      <section class="card" style="width: 420px; padding: 28px 28px 24px">
        <h2 style="font-size: 20px; font-weight: 600; color: #0f172a">登录</h2>
        <p style="font-size: 12px; color: #64748b; margin-top: 4px">
          角色由服务端判定，页面不自称身份；登录后按角色分流到对应首任务页。
        </p>

        <form @submit.prevent="onSubmit">
          <div style="margin-top: 22px">
            <label class="label" for="username">账号（登录名）</label>
            <input
              id="username"
              v-model.trim="username"
              class="input"
              :class="{ err: fieldError.username }"
              type="text"
              placeholder="请输入登录名"
              autocomplete="username"
            />
            <p v-if="fieldError.username" class="hint err">{{ fieldError.username }}</p>
          </div>

          <div style="margin-top: 16px">
            <label class="label" for="password">口令</label>
            <input
              id="password"
              v-model="password"
              class="input"
              :class="{ err: fieldError.password }"
              type="password"
              placeholder="请输入口令"
              autocomplete="current-password"
            />
            <p v-if="fieldError.password" class="hint err">{{ fieldError.password }}</p>
          </div>

          <label class="row-flex mt-12" style="font-size: 13px; color: #475569; cursor: pointer">
            <input v-model="remember" type="checkbox" />
            记住密码
            <span class="muted">（仅限个人设备：只存本机浏览器，仍由后端校验）</span>
          </label>

          <div v-if="error" class="banner b-rose mt-12">
            <Icon name="alert" />
            <span>{{ error }}</span>
          </div>

          <button class="btn btn-p" style="width: 100%; margin-top: 20px; height: 40px" :disabled="loading">
            <Icon name="lock" /> {{ loading ? '登录中…' : '登录' }}
          </button>
        </form>

        <p style="font-size: 11px; color: #94a3b8; margin-top: 14px; text-align: center">
          登录遇到问题？请联系教学管理员重置凭据。
        </p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { useIdentityStore } from '../store/identity'
import { rememberedCredential } from '../utils/rememberedCredential'

const route = useRoute()
const router = useRouter()
const store = useIdentityStore()

const username = ref('')
const password = ref('')
const remember = ref(false)
const error = ref('')
const loading = ref(false)
const fieldError = reactive({ username: '', password: '' })

onMounted(() => {
  const saved = rememberedCredential.get()
  if (saved) {
    username.value = saved.username
    password.value = saved.password
    remember.value = true
  }
})

function validate() {
  fieldError.username = username.value ? '' : '账号不能为空'
  fieldError.password = password.value ? '' : '口令不能为空'
  return !fieldError.username && !fieldError.password
}

async function onSubmit() {
  error.value = ''
  if (!validate()) return
  loading.value = true
  try {
    await store.login(username.value, password.value)
    if (remember.value) {
      rememberedCredential.save(username.value, password.value)
    } else {
      rememberedCredential.clear()
    }
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(target)
  } catch (e) {
    error.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-top {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 0;
}
</style>
