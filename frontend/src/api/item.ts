import { request } from './http'
import type { PageResult } from './user'

export interface Category {
  id: number
  name: string
  sort: number
}

export interface ItemCard {
  id: number
  type: number
  typeText: string
  title: string
  coverImage: string | null
  categoryName: string | null
  happenPlace: string | null
  happenTime: string | null
  itemStatus: number
  itemStatusText: string
  createdAt: string
}

export interface ItemComment {
  id: number
  userId: number
  nickname: string | null
  content: string
  createdAt: string
}

export interface ItemDetail {
  id: number
  type: number
  typeText: string
  title: string
  description: string
  categoryId: number
  categoryName: string | null
  contact: string
  happenTime: string
  happenPlace: string
  auditStatus: number
  auditStatusText: string
  itemStatus: number
  itemStatusText: string
  closeReason: string | null
  publisher: { id: number; nickname: string; studentNo: string } | null
  images: string[]
  comments: ItemComment[]
  myClaimStatus: number | null
  createdAt: string
}

export interface ItemListParams {
  page?: number
  size?: number
  type?: number
  categoryId?: number
  keyword?: string
  itemStatus?: number
  sort?: string
}

export interface ItemSavePayload {
  type: number
  title: string
  description: string
  categoryId: number
  happenTime: string
  happenPlace: string
  contact: string
  imageUrls: string[]
}

export function listCategories(): Promise<Category[]> {
  return request.get<Category[]>('/categories')
}

export function listItems(params: ItemListParams): Promise<PageResult<ItemCard>> {
  return request.get<PageResult<ItemCard>>('/items', { params })
}

export function getItem(id: number): Promise<ItemDetail> {
  return request.get<ItemDetail>(`/items/${id}`)
}

export function createItem(payload: ItemSavePayload): Promise<{ id: number }> {
  return request.post<{ id: number }>('/items', payload)
}

export function updateItem(id: number, payload: ItemSavePayload): Promise<null> {
  return request.put<null>(`/items/${id}`, payload)
}

export function closeItem(id: number, reason?: string): Promise<null> {
  return request.put<null>(`/items/${id}/close`, { reason })
}

export function advanceItem(id: number, targetStatus: number): Promise<null> {
  return request.put<null>(`/items/${id}/status`, { targetStatus })
}

/** 上传单张图片，返回相对 URL。 */
export async function uploadImage(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const data = await request.post<{ url: string }>('/upload/image', form)
  return data.url
}
