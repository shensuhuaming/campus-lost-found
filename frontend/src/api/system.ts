import axios from 'axios'

/**
 * 统一 axios 实例。
 * 约定：后端统一响应体 { code, message, data }，code=0 为成功。
 * 401/403 的跳转逻辑待鉴权方案确定后在此拦截器统一处理。
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error),
)

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface DbHealth {
  status: string
  error?: string
}

export interface HealthInfo {
  app: string
  status: string
  javaVersion: string
  timestamp: string
  database: DbHealth
}

/** 后端自检接口（环境验证用） */
export function getHealth(): Promise<ApiResult<HealthInfo>> {
  return http.get('/system/health')
}

export default http
