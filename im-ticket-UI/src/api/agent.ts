import http from './http'
import type { ApiResponse, PageData, Agent, PageQuery } from '@/types'

export const agentApi = {
  list(params: PageQuery & { keyword?: string; role?: string; status?: string }) {
    return http.get<ApiResponse<PageData<Agent>>>('/agents', { params })
  },
  detail(id: number) {
    return http.get<ApiResponse<Agent>>(`/agents/${id}`)
  },
  create(data: Partial<Agent>) {
    return http.post<ApiResponse<Agent>>('/agents', data)
  },
  update(id: number, data: Partial<Agent>) {
    return http.put<ApiResponse<Agent>>(`/agents/${id}`, data)
  },
  delete(id: number) {
    return http.delete<ApiResponse<null>>(`/agents/${id}`)
  }
}
