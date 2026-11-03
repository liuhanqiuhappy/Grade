<template>
  <div class="container vh-100 d-flex align-items-center justify-content-center">
    <div class="card shadow p-4" style="width: 420px;">
      <h2 class="text-center mb-4">登录</h2>
      <form @submit.prevent="onSubmit">
        <div class="mb-3">
          <label class="form-label">账号</label>
          <input v-model="form.username" type="text" class="form-control" placeholder="输入账号"/>
        </div>
        <div class="mb-3">
          <label class="form-label">密码</label>
          <input v-model="form.password" type="password" class="form-control" placeholder="输入密码"/>
        </div>
        <button class="btn btn-primary w-100" type="submit" :disabled="loading">
          {{ loading ? "登录中，请稍候..." : "登录" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/identity'
import { useIdentityStore } from '@/store/useIdentity'

const router = useRouter()
const identityStore = useIdentityStore()

const form = ref({
  username: "",
  password: ""
})
const loading = ref(false)

const onSubmit = async ()=>{
  // 前端简单校验（4001后端也会返回，前端先提示用户）
  if(!form.value.username?.trim()){
    window.alert("用户名不能为空")
    return
  }
  if(!form.value.password?.trim()){
    window.alert("密码不能为空")
    return
  }

  loading.value = true
  try{
    const res = await login({
      username: form.value.username.trim(),
      password: form.value.password
    })
    identityStore.setUser(res.data)
    router.push("/welcome")
  }catch(e){
    console.warn("登录异常", e)
  }finally {
    loading.value = false
  }
}
</script>
