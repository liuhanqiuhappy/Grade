import request from '@/utils/request'

// 登录 POST /api/identity/login（契约 §3.2）
// silent：业务失败不弹全局 alert，由登录页自行展示错误
export function login(payload){
  return request({
    url:'/identity/login',
    method:'post',
    data: payload,
    silent: true
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
