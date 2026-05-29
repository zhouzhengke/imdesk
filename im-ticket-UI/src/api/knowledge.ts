import http from './http'
import type { ApiResponse, PageData, KnowledgeFaq, KnowledgeDocument, PageQuery } from '@/types'

export const knowledgeApi = {
  // FAQ
  listFaqs(params: PageQuery & { keyword?: string; category?: string }) {
    return http.get<ApiResponse<PageData<KnowledgeFaq>>>('/knowledge/faqs', { params })
  },
  createFaq(data: Partial<KnowledgeFaq>) {
    return http.post<ApiResponse<KnowledgeFaq>>('/knowledge/faqs', data)
  },
  updateFaq(id: number, data: Partial<KnowledgeFaq>) {
    return http.put<ApiResponse<KnowledgeFaq>>(`/knowledge/faqs/${id}`, data)
  },
  deleteFaq(id: number) {
    return http.delete<ApiResponse<null>>(`/knowledge/faqs/${id}`)
  },

  // 文档
  listDocuments(params: PageQuery & { keyword?: string; category?: string }) {
    return http.get<ApiResponse<PageData<KnowledgeDocument>>>('/knowledge/documents', { params })
  },
  uploadDocument(formData: FormData) {
    return http.post<ApiResponse<KnowledgeDocument>>('/knowledge/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteDocument(id: number) {
    return http.delete<ApiResponse<null>>(`/knowledge/documents/${id}`)
  }
}
