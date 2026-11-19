import { request } from './http'

export { SUCCESS_CODE } from './http'

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

/** 后端自检接口（环境验证用）。 */
export function getHealth(): Promise<HealthInfo> {
  return request.get<HealthInfo>('/system/health')
}

export default request
