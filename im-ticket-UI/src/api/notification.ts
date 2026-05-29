import http from './http'
import type { ApiResponse, PageData, NotificationTemplate, PageQuery } from '@/types'

export const notificationApi = {
  list(params: PageQuery & { keyword?: string; direction?: string }) {
    return http.get<ApiResponse<PageData<NotificationTemplate>>>('/notification/templates', { params })
  },
  detail(id: number) {
    return http.get<ApiResponse<NotificationTemplate>>(`/notification/templates/${id}`)
  },
  create(data: Partial<NotificationTemplate>) {
    return http.post<ApiResponse<NotificationTemplate>>('/notification/templates', data)
  },
  update(id: number, data: Partial<NotificationTemplate>) {
    return http.put<ApiResponse<NotificationTemplate>>(`/notification/templates/${id}`, data)
  },
  delete(id: number) {
    return http.delete<ApiResponse<null>>(`/notification/templates/${id}`)
  }
}
