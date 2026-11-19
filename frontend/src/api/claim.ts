import { request } from './http'
import type { PageResult } from './user'

export interface ClaimResult {
  claimId: number
  itemStatus: number
}

export interface ClaimApplicant {
  id: number
  nickname: string
  studentNo: string
}

export interface ClaimAdminRow {
  id: number
  itemId: number
  itemTitle: string | null
  itemType: number | null
  applicant: ClaimApplicant | null
  detailDesc: string
  status: number
  statusText: string
  reply: string | null
  createdAt: string
}

export interface ClaimListParams {
  page?: number
  size?: number
  status?: number
  keyword?: string
}

/** 发起认领（API 5.1）。 */
export function createClaim(itemId: number, detailDesc: string): Promise<ClaimResult> {
  return request.post<ClaimResult>('/claims', { itemId, detailDesc })
}

/** 核验列表（API 5.2，管理员）。 */
export function listClaims(params: ClaimListParams): Promise<PageResult<ClaimAdminRow>> {
  return request.get<PageResult<ClaimAdminRow>>('/claims', { params })
}

/** 核验通过（API 5.3，管理员）。 */
export function approveClaim(id: number, reply?: string): Promise<ClaimResult> {
  return request.put<ClaimResult>(`/claims/${id}/approve`, { reply })
}

/** 核验拒绝（API 5.4，管理员）。 */
export function rejectClaim(id: number, reply: string): Promise<ClaimResult> {
  return request.put<ClaimResult>(`/claims/${id}/reject`, { reply })
}
