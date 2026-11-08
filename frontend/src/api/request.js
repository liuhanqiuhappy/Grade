import axios from 'axios'

// 统一 axios 封装：业务失败（code!=0）时以 reject 抛出 {code, message}
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data
      return Promise.reject({ code: body.code, message: body.message })
    }
    return body
  },
  (error) => {
    // 无 response 说明请求根本没到达后端（后端未启动 / 代理失败）
    const message = error && error.response
      ? '网络异常，请稍后重试'
      : '无法连接服务器，请确认后端服务已启动（localhost:8080）'
    return Promise.reject({ code: -1, message })
  }
)

export default request
