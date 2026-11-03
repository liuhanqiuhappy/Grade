<template>
  <div class="container mt-5">
    <div style="max-width:500px;margin:0 auto;">
      <h4>欢迎，{{ identityStore.displayName }}</h4>
      <p>角色：{{ identityStore.role }}</p>
      <p>登录账号：{{ identityStore.username }}</p>
      <hr/>
      <button class="btn btn-danger" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { useIdentityStore } from '@/store/useIdentity'
import { useRouter } from 'vue-router'
import { logout } from '@/api/identity'

const identityStore = useIdentityStore()
const router = useRouter()

const handleLogout = async ()=>{
  await logout()
  identityStore.clearUser()
  router.replace("/login")
}
</script>
