import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'

/** 后端统一响应体（API 规格 1.2）：code=200 成功。 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const SUCCESS_CODE = 200

/** 业务错误：code 为后端业务码，message 可直接展示。 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body.code !== SUCCESS_CODE) {
      return Promise.reject(new ApiError(body.code, body.message))
    }
    return body.data as never
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    // 401 或 2001：清除登录态并跳转登录页（带 redirect）
    if (status === 401 || body?.code === 2001) {
      useAuthStore().logout()
      if (window.location.pathname !== '/login') {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.assign(`/login?redirect=${redirect}`)
      }
    }
    const code = body?.code ?? status ?? 0
    const message = body?.message ?? error.message
    return Promise.reject(new ApiError(code, message))
  },
)

/** 统一请求入口：拦截器已把 Result.data 解包，这里只负责类型。 */
export const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.get(url, config) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.post(url, data, config) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return http.put(url, data, config) as unknown as Promise<T>
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return http.delete(url, config) as unknown as Promise<T>
  },
}

export default http
