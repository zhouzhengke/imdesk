import http from './http'
import type {
  ApiResponse,
  PageData,
  Ticket,
  TicketCreateReq,
  TicketMessage,
  TicketReplyReq,
  TicketSearchQuery,
  TicketStateLog,
  TicketTransferReq,
  TicketRejectReq,
  TicketDeferReq
} from '@/types'

export const ticketApi = {
  // 工单列表（分页）
  list(params: TicketSearchQuery) {
    return http.get<ApiResponse<PageData<Ticket>>>('/tickets', { params })
  },

  // 工单详情
  detail(id: number) {
    return http.get<ApiResponse<Ticket>>(`/tickets/${id}`)
  },

  // 创建工单
  create(data: TicketCreateReq) {
    return http.post<ApiResponse<Ticket>>('/tickets', data)
  },

  // 回复工单
  reply(data: TicketReplyReq) {
    return http.post<ApiResponse<TicketMessage>>(`/tickets/${data.ticketId}/reply`, data)
  },

  // 领取工单
  accept(ticketId: number) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${ticketId}/accept`)
  },

  // 转交工单
  transfer(data: TicketTransferReq) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${data.ticketId}/transfer`, data)
  },

  // 驳回工单
  reject(data: TicketRejectReq) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${data.ticketId}/reject`, data)
  },

  // 延期工单
  defer(data: TicketDeferReq) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${data.ticketId}/defer`, data)
  },

  // 解决工单
  resolve(ticketId: number) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${ticketId}/resolve`)
  },

  // 关闭工单
  close(ticketId: number) {
    return http.put<ApiResponse<Ticket>>(`/tickets/${ticketId}/close`)
  },

  // 工单消息列表
  messages(ticketId: number) {
    return http.get<ApiResponse<TicketMessage[]>>(`/tickets/${ticketId}/messages`)
  },

  // 工单状态日志
  stateLogs(ticketId: number) {
    return http.get<ApiResponse<TicketStateLog[]>>(`/tickets/${ticketId}/state-logs`)
  }
}
