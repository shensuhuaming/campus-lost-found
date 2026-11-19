import { request } from './http'

/** 用户信息（API 规格 2.4 / 2.2 的 user 字段）。 */
export interface UserInfo {
  id: number
  studentNo: string
  nickname: string
  phone?: string
  role: number
  status?: number
  createdAt?: string
}

export interface ProfileStats {
  published: number
  claiming: number
  claimingPending: number
  comments: number
}

export interface Profile extends UserInfo {
  stat: ProfileStats
}

/** 统一分页结构（API 规格 1.3）。 */
export interface PageResult<T> {
  total: number
  page: number
  size: number
  pages: number
  records: T[]
}

export interface ItemRow {
  id: number
  type: number
  title: string
  categoryName: string | null
  coverImage: string | null
  auditStatus: number
  auditStatusText: string
  rejectReason: string | null
  itemStatus: number
  itemStatusText: string
  claimCount: number
  createdAt: string
}

export interface ClaimRow {
  id: number
  itemId: number
  itemTitle: string | null
  itemStatus: number | null
  detailDesc: string
  status: number
  statusText: string
  reply: string | null
  createdAt: string
}

export interface CommentRow {
  id: number
  itemId: number
  itemTitle: string | null
  content: string
  createdAt: string
}

export interface PageParams {
  page?: number
  size?: number
}

/** 个人资料（含四项统计）。 */
export function getProfile(): Promise<Profile> {
  return request.get<Profile>('/user/profile')
}

/** 修改昵称与联系方式。 */
export function updateProfile(data: { nickname: string; phone: string }): Promise<UserInfo> {
  return request.put<UserInfo>('/user/profile', data)
}

/** 修改密码。 */
export function changePassword(data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}): Promise<null> {
  return request.put<null>('/user/password', data)
}

/** 我的发布。 */
export function myItems(params: PageParams & { auditStatus?: number; itemStatus?: number }): Promise<PageResult<ItemRow>> {
  return request.get<PageResult<ItemRow>>('/user/items', { params })
}

/** 我的认领。 */
export function myClaims(params: PageParams & { status?: number }): Promise<PageResult<ClaimRow>> {
  return request.get<PageResult<ClaimRow>>('/user/claims', { params })
}

/** 我的留言。 */
export function myComments(params: PageParams): Promise<PageResult<CommentRow>> {
  return request.get<PageResult<CommentRow>>('/user/comments', { params })
}
