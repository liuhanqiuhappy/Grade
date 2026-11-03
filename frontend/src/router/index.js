import { createRouter, createWebHistory } from 'vue-router'
import { useIdentityStore } from '@/store/useIdentity'
import { getMe } from '@/api/identity'

const routes = [
  {
    path:'/',
    redirect:'/login'
  },
  {
    path:'/login',
    name:'login',
    component: ()=>import("@/views/Login.vue")
  },
  {
    path:'/welcome',
    name:'welcome',
    component: ()=>import("@/views/Welcome.vue"),
    meta:{ requiresAuth:true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next)=>{
  const identityStore = useIdentityStore()
  if(to.meta.requiresAuth){
    try {
      const res = await getMe()
      identityStore.setUser(res.data)
      next()
    }catch(err){
      // err.code ===1002：未登录/会话过期
      identityStore.clearUser()
      next('/login')
    }
  }else{
    next()
  }
})

export default router
