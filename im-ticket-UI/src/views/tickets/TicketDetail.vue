<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Promotion } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ticketApi } from '@/api/ticket'
import type { Ticket, TicketMessage, TicketStatus, TicketStateLog } from '@/types'

const route = useRoute()
const router = useRouter()
const ticketId = Number(route.params.id)

const ticket = ref<Ticket | null>(null)
const messages = ref<TicketMessage[]>([])
const stateLogs = ref<TicketStateLog[]>([])
const loading = ref(false)
const replyContent = ref('')
const sending = ref(false)

const statusLabelMap: Record<TicketStatus, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  TRANSFERRED: '已转交',
  REJECTED: '已驳回',
  DEFERRED: '已延期',
  WAITING_CONFIRM: '待确认'
}

const statusTagMap: Record<TicketStatus, string> = {
  PENDING: 'warning',
  IN_PROGRESS: 'primary',
  RESOLVED: 'success',
  CLOSED: 'info',
  TRANSFERRED: '',
  REJECTED: 'danger',
  DEFERRED: '',
  WAITING_CONFIRM: 'warning'
}

const channelLabelMap: Record<string, string> = {
  wecom: '企业微信',
  feishu: '飞书'
}

// 根据当前状态决定可用操作
const availableActions = computed(() => {
  if (!ticket.value) return []
  const status = ticket.value.status
  const actions: { key: string; label: string; type: string }[] = []

  if (status === 'PENDING') {
    actions.push({ key: 'accept', label: '领取工单', type: 'primary' })
  }
  if (status === 'IN_PROGRESS') {
    actions.push({ key: 'transfer', label: '转交', type: '' })
    actions.push({ key: 'reject', label: '驳回', type: 'warning' })
    actions.push({ key: 'defer', label: '延期', type: 'info' })
    actions.push({ key: 'resolve', label: '标记已解决', type: 'success' })
  }
  if (status === 'WAITING_CONFIRM') {
    actions.push({ key: 'close', label: '关闭工单', type: 'info' })
  }
  if (status === 'RESOLVED') {
    actions.push({ key: 'close', label: '关闭工单', type: 'info' })
  }
  return actions
})

async function fetchDetail() {
  loading.value = true
  try {
    const [ticketRes, msgRes, logRes] = await Promise.all([
      ticketApi.detail(ticketId),
      ticketApi.messages(ticketId),
      ticketApi.stateLogs(ticketId)
    ])
    ticket.value = ticketRes.data.data
    messages.value = msgRes.data.data
    stateLogs.value = logRes.data.data
  } finally {
    loading.value = false
  }
}

async function sendReply() {
  if (!replyContent.value.trim()) return
  sending.value = true
  try {
    await ticketApi.reply({ ticketId, content: replyContent.value })
    ElMessage.success('回复发送成功')
    replyContent.value = ''
    // 刷新消息列表
    const msgRes = await ticketApi.messages(ticketId)
    messages.value = msgRes.data.data
  } catch {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

async function handleAction(key: string) {
  try {
    switch (key) {
      case 'accept':
        await ticketApi.accept(ticketId)
        ElMessage.success('已领取工单')
        break
      case 'resolve':
        await ElMessageBox.confirm('确认标记此工单为已解决？', '操作确认', { type: 'warning' })
        await ticketApi.resolve(ticketId)
        ElMessage.success('工单已标记为已解决')
        break
      case 'close':
        await ElMessageBox.confirm('确认关闭此工单？', '操作确认', { type: 'warning' })
        await ticketApi.close(ticketId)
        ElMessage.success('工单已关闭')
        break
      case 'transfer':
        // 简化实现：弹出输入框获取目标客服ID
        try {
          const { value } = await ElMessageBox.prompt('请输入目标客服ID', '转交工单', {
            inputType: 'number',
            confirmButtonText: '确定',
            cancelButtonText: '取消'
          })
          await ticketApi.transfer({ ticketId, targetAgentId: Number(value), remark: '' })
          ElMessage.success('工单已转交')
        } catch {
          // 用户取消
          return
        }
        break
      case 'reject':
        try {
          const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回工单', {
            confirmButtonText: '确定',
            cancelButtonText: '取消'
          })
          await ticketApi.reject({ ticketId, reason: value })
          ElMessage.success('工单已驳回')
        } catch {
          return
        }
        break
      case 'defer':
        try {
          const { value } = await ElMessageBox.prompt('请输入延期截止日期 (YYYY-MM-DD)', '延期工单', {
            confirmButtonText: '确定',
            cancelButtonText: '取消'
          })
          await ticketApi.defer({ ticketId, deferTo: value + 'T23:59:59+08:00', reason: '' })
          ElMessage.success('工单已延期')
        } catch {
          return
        }
        break
    }
    await fetchDetail()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

function goBack() {
  router.push('/tickets')
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="ticket-detail-page" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="detail-header">
      <el-button :icon="ArrowLeft" text @click="goBack" class="back-btn">返回列表</el-button>
      <h1 class="page-title" v-if="ticket">
        {{ ticket.ticketNo }}
        <el-tag
          :type="statusTagMap[ticket.status] as any || 'info'"
          size="default"
          effect="light"
          style="margin-left: 12px; vertical-align: middle"
        >
          {{ statusLabelMap[ticket.status] }}
        </el-tag>
      </h1>
    </div>

    <div class="detail-body" v-if="ticket">
      <!-- 左侧：工单信息 -->
      <aside class="info-panel">
        <el-card shadow="never" class="info-card">
          <template #header>
            <span class="card-title">工单信息</span>
          </template>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">编号</span>
              <span class="info-value">{{ ticket.ticketNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">资方</span>
              <span class="info-value">{{ ticket.capitalName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">渠道</span>
              <span class="info-value">{{ channelLabelMap[ticket.channel] || ticket.channel }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">渠道用户</span>
              <span class="info-value mono">{{ ticket.channelUserId }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">优先级</span>
              <el-tag size="small" effect="light">{{ ticket.priority }}</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">负责客服</span>
              <span class="info-value">{{ ticket.assignedAgentName || '未分配' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">标签</span>
              <span class="info-value">{{ ticket.tags || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ ticket.createdAt }}</span>
            </div>
          </div>
        </el-card>

        <!-- 状态变更日志 -->
        <el-card shadow="never" class="log-card">
          <template #header>
            <span class="card-title">状态变更日志</span>
          </template>
          <el-timeline v-if="stateLogs.length">
            <el-timeline-item
              v-for="log in stateLogs"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
              size="small"
            >
              <div class="log-item">
                <span class="log-transition">
                  {{ statusLabelMap[log.fromStatus] }} → {{ statusLabelMap[log.toStatus] }}
                </span>
                <span class="log-operator">{{ log.operatorName }}</span>
                <span v-if="log.remark" class="log-remark">{{ log.remark }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="empty-text">暂无状态变更记录</div>
        </el-card>
      </aside>

      <!-- 右侧：对话区 -->
      <div class="conversation-area">
        <!-- 消息时间线 -->
        <el-card shadow="never" class="conversation-card">
          <template #header>
            <span class="card-title">会话记录</span>
          </template>
          <div class="message-list" v-if="messages.length">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-bubble"
              :class="{
                'msg-user': msg.messageType === 'USER',
                'msg-agent': msg.messageType === 'AGENT',
                'msg-system': msg.messageType === 'SYSTEM'
              }"
            >
              <div class="msg-meta">
                <span class="msg-sender">{{ msg.senderName }}</span>
                <span class="msg-time">{{ msg.createdAt }}</span>
              </div>
              <div class="msg-content">{{ msg.content }}</div>
            </div>
          </div>
          <div v-else class="empty-text">暂无会话记录</div>
        </el-card>

        <!-- 回复操作栏 -->
        <el-card shadow="never" class="reply-card" v-if="ticket.status !== 'CLOSED'">
          <div class="reply-bar">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="3"
              placeholder="输入回复内容..."
              resize="none"
              class="reply-input"
            />
            <div class="reply-actions">
              <el-button type="primary" :icon="Promotion" :loading="sending" @click="sendReply">
                发送回复
              </el-button>
              <template v-for="action in availableActions" :key="action.key">
                <el-button
                  :type="action.type as any"
                  @click="handleAction(action.key)"
                >
                  {{ action.label }}
                </el-button>
              </template>
            </div>
          </div>
        </el-card>

        <!-- 已关闭提示 -->
        <el-card v-else shadow="never" class="closed-notice">
          <div class="closed-text">此工单已关闭，无法继续回复</div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ticket-detail-page {
  max-width: 1400px;
}

.detail-header {
  margin-bottom: 20px;
}

.back-btn {
  font-size: 14px;
  margin-bottom: 8px;
  padding: 0;
}

.detail-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* ===================== 左侧信息面板 ===================== */
.info-panel {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card,
.log-card {
  overflow: visible;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.info-label {
  color: var(--text-muted);
  flex-shrink: 0;
  margin-right: 8px;
}

.info-value {
  color: var(--text-primary);
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.mono {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: var(--text-secondary);
}

.log-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
}

.log-transition {
  font-weight: 600;
  color: var(--text-primary);
}

.log-operator {
  color: var(--accent);
  font-size: 12px;
}

.log-remark {
  color: var(--text-muted);
  font-size: 12px;
}

/* ===================== 右侧对话区 ===================== */
.conversation-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.conversation-card {
  flex: 1;
  min-height: 400px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  max-width: 75%;
  position: relative;
}

.msg-user {
  align-self: flex-start;
  background: #fff;
  border: 1px solid var(--border-color);
  border-bottom-left-radius: 2px;
}

.msg-agent {
  align-self: flex-end;
  background: linear-gradient(135deg, #e8edff, #dde4ff);
  border-bottom-right-radius: 2px;
}

.msg-system {
  align-self: center;
  background: #f7f8fb;
  border: 1px dashed var(--border-color);
  text-align: center;
  max-width: 60%;
  font-size: 12px;
  color: var(--text-muted);
}

.msg-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.msg-sender {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.msg-time {
  font-size: 11px;
  color: var(--text-muted);
}

.msg-content {
  font-size: 14px;
  line-height: 1.65;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===================== 回复栏 ===================== */
.reply-card {
  overflow: visible;
}

.reply-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-input :deep(.el-textarea__inner) {
  border-radius: var(--radius-sm);
  border-color: var(--border-color);
  font-size: 14px;
  line-height: 1.6;
}

.reply-input :deep(.el-textarea__inner:focus) {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(79, 110, 247, 0.12);
}

.reply-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* ===================== 已关闭提示 ===================== */
.closed-notice {
  text-align: center;
}

.closed-text {
  color: var(--text-muted);
  font-size: 14px;
  padding: 12px 0;
}

.empty-text {
  color: var(--text-muted);
  text-align: center;
  padding: 32px 0;
  font-size: 14px;
}

:deep(.el-timeline-item__node) {
  background: var(--accent);
}
</style>
