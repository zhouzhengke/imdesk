import http from './http'
import type { ApiResponse, PageData, Shift, ShiftSchedule, ShiftScheduleCreateReq, PageQuery } from '@/types'

export const shiftApi = {
  // 班次
  listShifts(params: PageQuery) {
    return http.get<ApiResponse<PageData<Shift>>>('/shifts', { params })
  },
  createShift(data: Partial<Shift>) {
    return http.post<ApiResponse<Shift>>('/shifts', data)
  },
  updateShift(id: number, data: Partial<Shift>) {
    return http.put<ApiResponse<Shift>>(`/shifts/${id}`, data)
  },
  deleteShift(id: number) {
    return http.delete<ApiResponse<null>>(`/shifts/${id}`)
  },

  // 排班
  listSchedules(params: PageQuery & { date?: string; agentId?: number }) {
    return http.get<ApiResponse<PageData<ShiftSchedule>>>('/shift-schedules', { params })
  },
  createSchedule(data: ShiftScheduleCreateReq) {
    return http.post<ApiResponse<ShiftSchedule>>('/shift-schedules', data)
  },
  deleteSchedule(id: number) {
    return http.delete<ApiResponse<null>>(`/shift-schedules/${id}`)
  }
}
