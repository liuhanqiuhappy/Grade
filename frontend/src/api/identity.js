import request from '@/utils/request'

// 登录 POST /api/identity/login
export function login(payload){
  return request({
    url:'/identity/login',
    method:'post',
    data: payload
  })
}

// 获取当前身份 GET /api/identity/me
export function getMe(){
  return request({
    url:'/identity/me',
    method:'get'
  })
}

// 退出登录 POST /api/identity/logout
export function logout(){
  return request({
    url:'/identity/logout',
    method:'post'
  })
}
