import { request } from './http'
import type { UserInfo } from './user'

export interface LoginPayload {
  studentNo: string
  password: string
}

export interface RegisterPayload {
  studentNo: string
  nickname: string
  phone: string
  password: string
  confirmPassword: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

/** 登录（API 规格 2.2）：成功返回 token 与用户信息。 */
export function login(payload: LoginPayload): Promise<LoginResult> {
  return request.post<LoginResult>('/auth/login', payload)
}

/** 注册（API 规格 2.1）：成功返回新用户 id，不自动登录。 */
export function register(payload: RegisterPayload): Promise<{ userId: number }> {
  return request.post<{ userId: number }>('/auth/register', payload)
}

/** 退出（API 规格 2.3）：服务端只记日志，前端清 token。 */
export function logout(): Promise<null> {
  return request.post<null>('/auth/logout')
}
