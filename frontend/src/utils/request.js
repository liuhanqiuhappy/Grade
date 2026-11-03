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
      window.alert(res.message || "业务异常")
      return Promise.reject(res)
    }
    return res
  },
  error => {
    // HTTP层面异常：后端宕机、网络断开
    window.alert("网络异常，无法连接后端服务，请确认后端已启动 /api/ping")
    return Promise.reject(error)
  }
)

export default service
