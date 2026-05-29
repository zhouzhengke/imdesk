import http from './http'
import type { ApiResponse, PageData, Capital, PageQuery } from '@/types'

export const capitalApi = {
  list(params: PageQuery & { keyword?: string; status?: string }) {
    return http.get<ApiResponse<PageData<Capital>>>('/capitals', { params })
  },
  all() {
    return http.get<ApiResponse<Capital[]>>('/capitals/all')
  },
  create(data: Partial<Capital>) {
    return http.post<ApiResponse<Capital>>('/capitals', data)
  },
  update(id: number, data: Partial<Capital>) {
    return http.put<ApiResponse<Capital>>(`/capitals/${id}`, data)
  },
  delete(id: number) {
    return http.delete<ApiResponse<null>>(`/capitals/${id}`)
  }
}
