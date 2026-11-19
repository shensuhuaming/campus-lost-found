import { request } from './http'

export interface CommentCreated {
  id: number
  createdAt: string
}

/** 发表留言（API 6.1）。 */
export function createComment(itemId: number, content: string): Promise<CommentCreated> {
  return request.post<CommentCreated>('/comments', { itemId, content })
}

/** 删除留言（API 6.2）。 */
export function deleteComment(id: number): Promise<null> {
  return request.delete<null>(`/comments/${id}`)
}
