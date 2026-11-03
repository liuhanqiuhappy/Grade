import axios from 'axios'

const service = axios.create({
  baseURL: '/api',
  timeout: 6000,
  withCredentials: true
})

service.interceptors.request.use(config => {
  return config
})

service.interceptors.response.use(
  response => {
    const res = response.data
    // 契约规则：code===0代表业务成功；非0全部业务异常
    if(res.code !== 0){
      // silent 的请求（登录）由页面自己展示错误，避免重复弹窗
      if(!response.config?.silent){
        window.alert(res.message || "业务异常")
      }
      return Promise.reject(res)
    }
    return res
  },
  error => {
    // HTTP 层面异常：后端宕机、网络断开、路径写错
    const message = "网络异常，无法连接后端服务，请确认后端已启动（/api/ping）"
    if(!error.config?.silent){
      window.alert(message)
    }
    // 统一成 {code, message, data} 形状，页面只按 message 展示即可
    return Promise.reject({ code: -1, message, data: null })
  }
)

export default service
