import { request } from './http'
import type { PageResult } from './user'

export interface AuditItem {
  id: number
  title: string
  type: number
  typeText: string
  categoryId: number | null
  categoryName: string | null
  publisherNickname: string | null
  description: string
  images: string[]
  auditStatus: number
  auditStatusText: string
  createdAt: string
}

export interface CategoryAdmin {
  id: number
  name: string
  sort: number
  itemCount: number
}

export interface AdminUser {
  id: number
  studentNo: string
  nickname: string
  phone: string | null
  role: number
  roleText: string
  status: number
  statusText: string
  itemCount: number
  claimCount: number
  createdAt: string
}

export interface Stats {
  totalItems: number
  pendingAudit: number
  completedClaims: number
  returnRate: number
  categoryStats: { categoryId: number; categoryName: string; itemCount: number; completedCount: number; returnRate: number }[]
  trend: { date: string; count: number }[]
}

export interface AuditPageParams {
  page?: number
  size?: number
  auditStatus?: number
  keyword?: string
}

export interface AdminUserParams {
  page?: number
  size?: number
  keyword?: string
  role?: number
  status?: number
}

/** U6.1 待审核列表（API 7.1）。 */
export function auditPage(params: AuditPageParams): Promise<PageResult<AuditItem>> {
  return request.get<PageResult<AuditItem>>('/admin/items/audit', { params })
}

/** U6.1 审核（API 7.2）。 */
export function auditItem(id: number, auditStatus: number, rejectReason?: string): Promise<unknown> {
  return request.put(`/admin/items/${id}/audit`, { auditStatus, rejectReason })
}

/** U6.2 分类列表（API 7.3）。 */
export function adminCategories(): Promise<CategoryAdmin[]> {
  return request.get<CategoryAdmin[]>('/admin/categories')
}

export function createCategory(name: string, sort: number): Promise<CategoryAdmin> {
  return request.post<CategoryAdmin>('/admin/categories', { name, sort })
}

export function updateCategory(id: number, name: string, sort: number): Promise<CategoryAdmin> {
  return request.put<CategoryAdmin>(`/admin/categories/${id}`, { name, sort })
}

export function deleteCategory(id: number): Promise<null> {
  return request.delete<null>(`/admin/categories/${id}`)
}

/** U6.3 用户列表（API 7.7）。 */
export function adminUsers(params: AdminUserParams): Promise<PageResult<AdminUser>> {
  return request.get<PageResult<AdminUser>>('/admin/users', { params })
}

export function updateUserStatus(id: number, status: number): Promise<unknown> {
  return request.put(`/admin/users/${id}/status`, { status })
}

/** U6.4 数据统计（API 7.9）。 */
export function adminStats(days?: number): Promise<Stats> {
  return request.get<Stats>('/admin/stats', { params: { days } })
}
