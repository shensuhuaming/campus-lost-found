import { request } from './http'
import type { PageResult } from './user'

export interface MessageRow {
  id: number
  type: number
  typeText: string
  content: string
  itemId: number | null
  fromUserId: number
  fromNickname: string | null
  isRead: number
  createdAt: string
}

export interface MessageListParams {
  page?: number
  size?: number
  isRead?: number
  type?: number
}

/** 我的消息（API 6.3）。 */
export function listMessages(params: MessageListParams): Promise<PageResult<MessageRow>> {
  return request.get<PageResult<MessageRow>>('/messages', { params })
}

/** 标记单条已读（API 6.4）。 */
export function markMessageRead(id: number): Promise<{ id: number; isRead: number }> {
  return request.put<{ id: number; isRead: number }>(`/messages/${id}/read`)
}

/** 未读消息数（API 6.5）。 */
export function unreadCount(): Promise<number> {
  return request.get<{ count: number }>('/messages/unread-count').then((data) => data.count)
}
