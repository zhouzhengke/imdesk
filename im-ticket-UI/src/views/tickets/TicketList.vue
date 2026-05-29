<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { ticketApi } from '@/api/ticket'
import type { Ticket, TicketStatus, ChannelType, TicketSearchQuery } from '@/types'

const router = useRouter()

// 筛选条件
const query = reactive<TicketSearchQuery>({
  page: 1,
  size: 20,
  channel: undefined,
  status: undefined,
  keyword: ''
})

// 数据
const loading = ref(false)
const ticketList = ref<Ticket[]>([])
const total = ref(0)

// 渠道选项
const channelOptions = [
  { label: '全部渠道', value: '' },
  { label: '企业微信', value: 'wecom' },
  { label: '飞书', value: 'feishu' }
]

// 状态选项
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'IN_PROGRESS' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已转交', value: 'TRANSFERRED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已延期', value: 'DEFERRED' },
  { label: '待确认', value: 'WAITING_CONFIRM' }
]

// 状态标签映射
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

// 渠道标签映射
const channelTagMap: Record<ChannelType, string> = {
  wecom: 'success',
  feishu: 'primary'
}

const channelLabelMap: Record<ChannelType, string> = {
  wecom: '企业微信',
  feishu: '飞书'
}

// 优先级标签映射
const priorityTagMap: Record<string, string> = {
  LOW: 'info',
  NORMAL: '',
  HIGH: 'warning',
  URGENT: 'danger'
}

const priorityLabelMap: Record<string, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

async function fetchTickets() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (!params.channel) delete params.channel
    if (!params.status) delete params.status
    if (!params.keyword) delete params.keyword
    const res = await ticketApi.list(params)
    ticketList.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchTickets()
}

function handleReset() {
  query.channel = undefined
  query.status = undefined
  query.keyword = ''
  query.page = 1
  fetchTickets()
}

function handlePageChange(page: number) {
  query.page = page
  fetchTickets()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  fetchTickets()
}

function goDetail(row: Ticket) {
  router.push(`/tickets/${row.id}`)
}

onMounted(() => {
  fetchTickets()
})
</script>

<template>
  <div class="ticket-list-page">
    <div class="page-header">
      <h1 class="page-title">工单工作台</h1>
      <p class="page-subtitle">管理和处理所有渠道的客户工单</p>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <div class="filter-left">
          <el-select
            v-model="query.channel"
            placeholder="渠道"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in channelOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value || undefined"
            />
          </el-select>
          <el-select
            v-model="query.status"
            placeholder="状态"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value || undefined"
            />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="搜索工单编号/问题摘要..."
            clearable
            style="width: 260px"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 工单表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="ticketList"
        v-loading="loading"
        stripe
        highlight-current-row
        style="width: 100%"
        @row-click="goDetail"
      >
        <el-table-column prop="ticketNo" label="工单编号" width="180" />
        <el-table-column prop="capitalName" label="资方" width="140" />
        <el-table-column label="渠道" width="100">
          <template #default="{ row }: { row: Ticket }">
            <el-tag
              :type="channelTagMap[row.channel]"
              size="small"
              effect="light"
            >
              {{ channelLabelMap[row.channel] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subject" label="问题摘要" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }: { row: Ticket }">
            <el-tag
              :type="(statusTagMap[row.status] || 'info') as any"
              size="small"
              effect="light"
            >
              {{ statusLabelMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }: { row: Ticket }">
            <el-tag
              :type="(priorityTagMap[row.priority] || 'info') as any"
              size="small"
              effect="light"
            >
              {{ priorityLabelMap[row.priority] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignedAgentName" label="负责客服" width="120">
          <template #default="{ row }">
            <span v-if="row.assignedAgentName">{{ row.assignedAgentName }}</span>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.ticket-list-page {
  max-width: 1400px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  overflow: visible;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.text-muted {
  color: var(--text-muted);
  font-style: italic;
}

:deep(.el-table__row) {
  cursor: pointer;
  transition: background-color 0.15s ease;
}
</style>
