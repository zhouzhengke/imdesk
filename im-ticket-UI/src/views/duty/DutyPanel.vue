<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { agentApi } from '@/api/agent'
import type { Agent } from '@/types'

const loading = ref(false)
const dutyAgents = ref<Agent[]>([])
const pendingCount = ref(0)
const timeoutAlerts = ref<{ agent: string; ticketNo: string; minutes: number }[]>([])

const agentStatusTagMap: Record<string, string> = {
  ONLINE: 'success',
  OFFLINE: 'info',
  BREAK: 'warning'
}

const agentStatusLabelMap: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  BREAK: '休息'
}

async function fetchDutyData() {
  loading.value = true
  try {
    // 获取在线和休息状态的客服作为值班人员
    const res = await agentApi.list({ page: 1, size: 50, status: '' })
    const allAgents = res.data.data.records
    dutyAgents.value = allAgents.filter(a => a.status === 'ONLINE' || a.status === 'BREAK')
    pendingCount.value = dutyAgents.value.reduce((sum, a) => sum + (a.currentLoad || 0), 0)
    // 模拟超时告警数据
    timeoutAlerts.value = dutyAgents.value
      .filter(a => a.currentLoad > 5)
      .map(a => ({ agent: a.realName, ticketNo: 'IM-20260528-' + String(a.id).padStart(4, '0'), minutes: Math.floor(Math.random() * 30) + 5 }))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDutyData()
})
</script>

<template>
  <div class="duty-panel-page">
    <div class="page-header">
      <h1 class="page-title">值班面板</h1>
      <p class="page-subtitle">实时监控值班状态与工单负载</p>
    </div>

    <div class="duty-grid" v-loading="loading">
      <!-- 当前值班人员 -->
      <el-card shadow="never" class="duty-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">当前值班人员</span>
            <el-tag size="small" round>{{ dutyAgents.length }} 人</el-tag>
          </div>
        </template>
        <div class="agent-list" v-if="dutyAgents.length">
          <div v-for="agent in dutyAgents" :key="agent.id" class="agent-row">
            <div class="agent-avatar">
              {{ agent.realName.charAt(0) }}
            </div>
            <div class="agent-info">
              <span class="agent-name">{{ agent.realName }}</span>
              <span class="agent-role">{{ agent.role === 'ADMIN' ? '管理员' : agent.role === 'SUPERVISOR' ? '主管' : '客服' }}</span>
            </div>
            <el-tag
              :type="agentStatusTagMap[agent.status] as any"
              size="small"
              effect="light"
            >
              {{ agentStatusLabelMap[agent.status] }}
            </el-tag>
          </div>
        </div>
        <div v-else class="empty-text">暂无值班人员在线</div>
      </el-card>

      <!-- 待分配工单数 -->
      <el-card shadow="never" class="duty-card metric-card">
        <template #header>
          <span class="card-title">待分配工单</span>
        </template>
        <div class="metric-value">
          <span class="metric-number">{{ pendingCount }}</span>
          <span class="metric-unit">个</span>
        </div>
        <div class="metric-desc">当前待处理的工单数量</div>
      </el-card>

      <!-- 超时未响应告警 -->
      <el-card shadow="never" class="duty-card alert-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">超时未响应告警</span>
            <el-tag v-if="timeoutAlerts.length" type="danger" size="small" round>{{ timeoutAlerts.length }} 条</el-tag>
            <el-tag v-else type="success" size="small" round>无告警</el-tag>
          </div>
        </template>
        <div class="alert-list" v-if="timeoutAlerts.length">
          <div v-for="(alert, idx) in timeoutAlerts" :key="idx" class="alert-row">
            <span class="alert-ticket">{{ alert.ticketNo }}</span>
            <span class="alert-agent">{{ alert.agent }}</span>
            <span class="alert-time">超时 {{ alert.minutes }} 分钟</span>
          </div>
        </div>
        <div v-else class="empty-text">当前无超时告警</div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.duty-panel-page {
  max-width: 1400px;
}

.duty-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
}

.duty-card {
  min-height: 280px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

/* ===================== 值班人员列表 ===================== */
.agent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agent-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--bg-primary);
  transition: background 0.2s;
}

.agent-row:hover {
  background: #e8edff;
}

.agent-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}

.agent-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.agent-name {
  font-weight: 600;
  font-size: 14px;
}

.agent-role {
  font-size: 12px;
  color: var(--text-muted);
}

/* ===================== 指标卡片 ===================== */
.metric-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 16px 0 8px;
}

.metric-number {
  font-size: 56px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
  letter-spacing: -2px;
}

.metric-unit {
  font-size: 18px;
  color: var(--text-muted);
  font-weight: 500;
}

.metric-desc {
  font-size: 13px;
  color: var(--text-muted);
}

/* ===================== 告警列表 ===================== */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.alert-ticket {
  font-family: 'SF Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.alert-agent {
  font-size: 13px;
  color: var(--text-secondary);
}

.alert-time {
  margin-left: auto;
  font-size: 12px;
  color: #dc2626;
  font-weight: 600;
}

.empty-text {
  color: var(--text-muted);
  text-align: center;
  padding: 40px 0;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .duty-grid {
    grid-template-columns: 1fr;
  }
}
</style>
