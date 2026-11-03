<template>
  <div class="container vh-100 d-flex align-items-center justify-content-center">
    <div class="card shadow p-4" style="width: 460px;">
      <h2 class="text-center mb-4">注册</h2>

      <!-- 业务失败提示：4001 / 1004 / 5000 都直接展示后端 message -->
      <div v-if="errorMessage" class="alert alert-danger py-2">{{ errorMessage }}</div>

      <form @submit.prevent="onSubmit" novalidate>
        <div class="mb-3">
          <label class="form-label">账号</label>
          <input v-model="form.username" type="text" class="form-control" placeholder="字母开头，4-32 位"
                 :class="{ 'is-invalid': fieldErrors.username }" :disabled="loading"/>
          <div v-if="fieldErrors.username" class="invalid-feedback">{{ fieldErrors.username }}</div>
          <div v-else class="form-text">字母开头，仅字母、数字、下划线，4-32 位</div>
        </div>

        <div class="mb-3">
          <label class="form-label">姓名</label>
          <input v-model="form.displayName" type="text" class="form-control" placeholder="真实姓名"
                 :class="{ 'is-invalid': fieldErrors.displayName }" :disabled="loading"/>
          <div v-if="fieldErrors.displayName" class="invalid-feedback">{{ fieldErrors.displayName }}</div>
        </div>

        <div class="mb-3">
          <label class="form-label">密码</label>
          <input v-model="form.password" type="password" class="form-control" placeholder="8-64 位"
                 :class="{ 'is-invalid': fieldErrors.password }" :disabled="loading"/>
          <div v-if="fieldErrors.password" class="invalid-feedback">{{ fieldErrors.password }}</div>
        </div>

        <div class="mb-4">
          <label class="form-label">确认密码</label>
          <input v-model="form.confirmPassword" type="password" class="form-control" placeholder="再次输入密码"
                 :class="{ 'is-invalid': fieldErrors.confirmPassword }" :disabled="loading"/>
          <div v-if="fieldErrors.confirmPassword" class="invalid-feedback">{{ fieldErrors.confirmPassword }}</div>
        </div>

        <button class="btn btn-primary w-100" type="submit" :disabled="loading">
          {{ loading ? "注册中，请稍候..." : "注册" }}
        </button>
      </form>

      <div class="text-center mt-3">
        已有账号？
        <router-link to="/login">返回登录</router-link>
      </div>
      <div class="form-text text-center mt-2">注册只创建学生账号，教师账号由管理员分配。</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/identity'

const router = useRouter()

const form = ref({
  username: "",
  displayName: "",
  password: "",
  confirmPassword: ""
})
const loading = ref(false)
const errorMessage = ref("")
const fieldErrors = ref({})

// 与契约 §3.6 保持一致（前端只能更严，不能更松）
const USERNAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]{3,31}$/

function validate() {
  const errors = {}
  const username = form.value.username?.trim() || ""
  const displayName = form.value.displayName?.trim() || ""
  const password = form.value.password || ""
  const confirmPassword = form.value.confirmPassword || ""

  if (!username) {
    errors.username = "用户名不能为空"
  } else if (!USERNAME_PATTERN.test(username)) {
    errors.username = "用户名需以字母开头，由字母、数字、下划线组成，长度 4-32 个字符"
  }

  if (!displayName) {
    errors.displayName = "姓名不能为空"
  } else if (displayName.length > 50) {
    errors.displayName = "姓名长度需为 1-50 个字符"
  }

  if (!password) {
    errors.password = "密码不能为空"
  } else if (password.length < 8 || password.length > 64) {
    errors.password = "密码长度需为 8-64 个字符"
  }

  if (!confirmPassword) {
    errors.confirmPassword = "请再次输入密码"
  } else if (confirmPassword !== password) {
    errors.confirmPassword = "两次输入的密码不一致"
  }

  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

const onSubmit = async ()=>{
  errorMessage.value = ""
  if (!validate()) {
    return
  }

  loading.value = true
  try{
    // 契约 §3.5：只提交三个字段，confirmPassword 不发后端
    await register({
      username: form.value.username.trim(),
      password: form.value.password,
      displayName: form.value.displayName.trim()
    })
    // 注册不自动登录（契约 Q7）：跳登录页并回填账号
    router.replace({ name: 'login', query: { username: form.value.username.trim(), registered: '1' } })
  }catch(e){
    // 1004 定位到账号字段，其余 4001 直接展示 message
    if (e?.code === 1004) {
      fieldErrors.value = { ...fieldErrors.value, username: e.message || "用户名已被注册" }
      errorMessage.value = e.message || "用户名已被注册"
    } else {
      errorMessage.value = e?.message || "注册失败，请稍后重试"
    }
  }finally {
    loading.value = false
  }
}
</script>
