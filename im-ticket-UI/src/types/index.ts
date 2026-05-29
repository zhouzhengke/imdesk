// ===================== 通用类型 =====================

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PageData<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface PageQuery {
  page: number
  size: number
}

// ===================== 枚举 =====================

export type TicketStatus = 'PENDING' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'TRANSFERRED' | 'REJECTED' | 'DEFERRED' | 'WAITING_CONFIRM'

export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export type ChannelType = 'wecom' | 'feishu'

export type AgentStatus = 'ONLINE' | 'OFFLINE' | 'BREAK'

export type AgentRole = 'ADMIN' | 'AGENT' | 'SUPERVISOR'

export type MessageDirection = 'INBOUND' | 'OUTBOUND'

export type MessageType = 'USER' | 'AGENT' | 'SYSTEM'

export type NotificationDirection = 'INTERNAL' | 'EXTERNAL'

export type NotificationFormat = 'TEXT' | 'HTML' | 'MARKDOWN'

export type CapitalStatus = 'ACTIVE' | 'INACTIVE'

// ===================== 业务实体 =====================

export interface Ticket {
  id: number
  ticketNo: string
  capitalId: number
  capitalName?: string
  channel: ChannelType
  channelUserId: string
  channelGroupId: string
  subject: string
  status: TicketStatus
  priority: TicketPriority
  assignedAgentId: number
  assignedAgentName?: string
  tags: string
  sourceMessageId: number
  resolvedAt: string | null
  closedAt: string | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface TicketMessage {
  id: number
  ticketId: number
  ticketNo: string
  messageType: MessageType
  direction: MessageDirection
  content: string
  contentType: string
  senderId: number
  senderName: string
  channelMessageId: string
  createdAt: string
}

export interface Agent {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  role: AgentRole
  status: AgentStatus
  wecomUserId: string
  feishuOpenId: string
  maxConcurrent: number
  currentLoad: number
  lastActiveAt: string
  createdAt: string
  updatedAt: string
}

export interface Capital {
  id: number
  name: string
  contactPerson: string
  contactPhone: string
  contractStartDate: string
  contractEndDate: string
  status: CapitalStatus
  remark: string
  createdAt: string
  updatedAt: string
}

export interface CapitalChannelMapping {
  id: number
  capitalId: number
  channel: ChannelType
  channelUserId: string
  channelGroupId: string
  createdAt: string
}

export interface Shift {
  id: number
  name: string
  startTime: string
  endTime: string
  description: string
  createdAt: string
}

export interface ShiftSchedule {
  id: number
  agentId: number
  agentName?: string
  shiftId: number
  shiftName?: string
  scheduleDate: string
  isBackup: boolean
  createdAt: string
}

export interface KnowledgeFaq {
  id: number
  question: string
  answer: string
  keywords: string
  category: string
  enabled: boolean
  priority: number
  createdAt: string
  updatedAt: string
}

export interface KnowledgeDocument {
  id: number
  title: string
  fileName: string
  fileType: string
  fileSize: number
  content: string
  category: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface NotificationTemplate {
  id: number
  code: string
  name: string
  direction: NotificationDirection
  channel: ChannelType
  format: NotificationFormat
  title: string
  content: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface TicketStateLog {
  id: number
  ticketId: number
  ticketNo: string
  fromStatus: TicketStatus
  toStatus: TicketStatus
  operator: number
  operatorName: string
  remark: string
  createdAt: string
}

// ===================== 请求体 =====================

export interface TicketCreateReq {
  channel: ChannelType
  channelUserId: string
  channelGroupId: string
  subject: string
  priority: TicketPriority
  content: string
}

export interface TicketReplyReq {
  ticketId: number
  content: string
}

export interface TicketTransferReq {
  ticketId: number
  targetAgentId: number
  remark: string
}

export interface TicketRejectReq {
  ticketId: number
  reason: string
}

export interface TicketDeferReq {
  ticketId: number
  deferTo: string
  reason: string
}

export interface TicketSearchQuery extends PageQuery {
  channel?: ChannelType
  status?: TicketStatus
  keyword?: string
  capitalId?: number
  priority?: TicketPriority
  assignedAgentId?: number
  startDate?: string
  endDate?: string
}

export interface ShiftScheduleCreateReq {
  agentId: number
  shiftId: number
  scheduleDate: string
  isBackup: boolean
}
