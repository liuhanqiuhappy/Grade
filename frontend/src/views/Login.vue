<template>
  <div class="container vh-100 d-flex align-items-center justify-content-center">
    <div class="card shadow p-4" style="width: 420px;">
      <h2 class="text-center mb-4">登录</h2>

      <!-- 注册成功回来的一次性提示 -->
      <div v-if="registeredTip" class="alert alert-success py-2">注册成功，请使用新账号登录</div>
      <!-- 业务失败提示：直接展示后端 message（契约 §5.4） -->
      <div v-if="errorMessage" class="alert alert-danger py-2">{{ errorMessage }}</div>

      <form @submit.prevent="onSubmit">
        <div class="mb-3">
          <label class="form-label">账号</label>
          <input v-model="form.username" type="text" class="form-control" placeholder="输入账号"
                 :disabled="loading"/>
        </div>
        <div class="mb-3">
          <label class="form-label">密码</label>
          <input v-model="form.password" type="password" class="form-control" placeholder="输入密码"
                 :disabled="loading"/>
        </div>

        <div class="form-check mb-2">
          <input id="rememberPassword" v-model="remember" class="form-check-input" type="checkbox"
                 :disabled="loading"/>
          <label class="form-check-label" for="rememberPassword">记住密码</label>
        </div>
        <div class="form-text mb-3">勾选后在本机保留账号密码，仅限个人设备；公共电脑请勿勾选。</div>

        <button class="btn btn-primary w-100" type="submit" :disabled="loading">
          {{ loading ? "登录中，请稍候..." : "登录" }}
        </button>
      </form>

      <div class="text-center mt-3">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '@/api/identity'
import { useIdentityStore } from '@/store/useIdentity'
import { loadCredential, saveCredential, clearCredential } from '@/utils/rememberedCredential'

const router = useRouter()
const route = useRoute()
const identityStore = useIdentityStore()

const form = ref({
  username: "",
  password: ""
})
const remember = ref(false)
const loading = ref(false)
const errorMessage = ref("")
const registeredTip = ref(route.query.registered === '1')

onMounted(() => {
  // 1) 从注册页带回来的账号优先回填
  const queryUsername = typeof route.query.username === 'string' ? route.query.username.trim() : ""
  if (queryUsername) {
    form.value.username = queryUsername
  }
  // 2) 有本地记住的凭据则回填账号密码并自动勾选
  //    若刚注册的账号与记住的账号不是同一个，就不能拿旧密码去填新账号
  const saved = loadCredential()
  if (saved && (!queryUsername || queryUsername === saved.username)) {
    form.value.username = saved.username
    form.value.password = saved.password
    remember.value = true
  }
})

const onSubmit = async ()=>{
  errorMessage.value = ""
  registeredTip.value = false

  const username = form.value.username?.trim() || ""
  const password = form.value.password || ""

  // 前端先拦一遍，缺失字段直接指出（后端同样会返回 4001 兜底）
  if(!username){
    errorMessage.value = "用户名不能为空"
    return
  }
  if(!password){
    errorMessage.value = "密码不能为空"
    return
  }

  loading.value = true
  try{
    // 契约 §3.2：请求体只有 username/password，不带 remember 字段
    const res = await login({ username, password })
    identityStore.setUser(res.data)

    // 契约 §3.8：只有「勾选 + 登录成功」才写本地凭据；未勾选则清掉旧记录
    if (remember.value) {
      saveCredential(username, password)
    } else {
      clearCredential()
    }

    router.push("/welcome")
  }catch(e){
    // e 是后端响应体 {code, message, data}；1001 与 4001 都直接展示 message
    errorMessage.value = e?.message || "登录失败，请稍后重试"
  }finally {
    loading.value = false
  }
}
</script>
