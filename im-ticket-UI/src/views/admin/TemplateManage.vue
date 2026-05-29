<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { notificationApi } from '@/api/notification'
import type { NotificationTemplate } from '@/types'

const loading = ref(false)
const list = ref<NotificationTemplate[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增模板')
const formData = reactive<Partial<NotificationTemplate>>({})
const editingId = ref<number | null>(null)

const directionLabelMap: Record<string, string> = { INTERNAL: '内部通知', EXTERNAL: '外部通知' }
const formatLabelMap: Record<string, string> = { TEXT: '纯文本', HTML: 'HTML', MARKDOWN: 'Markdown' }

async function fetchList() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (!params.keyword) delete params.keyword
    const res = await notificationApi.list(params)
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = '新增模板'
  editingId.value = null
  Object.assign(formData, { code: '', name: '', direction: 'INTERNAL' as const, channel: 'wecom' as const, format: 'TEXT' as const, title: '', content: '' })
  dialogVisible.value = true
}

function openEdit(row: NotificationTemplate) {
  dialogTitle.value = '编辑模板'
  editingId.value = row.id
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await notificationApi.update(editingId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await notificationApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: NotificationTemplate) {
  try {
    await ElMessageBox.confirm(`确认删除模板 "${row.name}"？`, '删除确认', { type: 'warning' })
    await notificationApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch { /* 取消 */ }
}

function handlePageChange(page: number) { query.page = page; fetchList() }
function handleSizeChange(size: number) { query.size = size; query.page = 1; fetchList() }

onMounted(() => fetchList())
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">通知模板</h1>
      <p class="page-subtitle">管理工单系统各类通知消息模板</p>
    </div>

    <el-card shadow="never" class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索模板名称/编码..." :prefix-icon="Search" clearable style="width: 240px" @keyup.enter="fetchList" @clear="fetchList" />
          <el-button type="primary" :icon="Search" @click="fetchList">搜索</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="code" label="模板编码" width="180" />
        <el-table-column prop="name" label="模板名称" width="160" />
        <el-table-column label="通知方向" width="100">
          <template #default="{ row }">{{ directionLabelMap[row.direction] || row.direction }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">
            <el-tag :type="row.channel === 'wecom' ? 'success' : 'primary'" size="small" effect="light">
              {{ row.channel === 'wecom' ? '企业微信' : '飞书' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="格式" width="100">
          <template #default="{ row }">{{ formatLabelMap[row.format] || row.format }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small" effect="light">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link size="small" @click.stop="openEdit(row)">编辑</el-button>
            <el-button :icon="Delete" type="danger" link size="small" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="formData" label-width="90px">
        <el-form-item label="模板编码" required>
          <el-input v-model="formData.code" placeholder="如: ticket_created_notify" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="formData.name" placeholder="如: 新工单通知" />
        </el-form-item>
        <el-form-item label="通知方向">
          <el-select v-model="formData.direction" style="width: 100%">
            <el-option label="内部通知" value="INTERNAL" />
            <el-option label="外部通知" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="formData.channel" style="width: 100%">
            <el-option label="企业微信" value="wecom" />
            <el-option label="飞书" value="feishu" />
          </el-select>
        </el-form-item>
        <el-form-item label="格式">
          <el-select v-model="formData.format" style="width: 100%">
            <el-option label="纯文本" value="TEXT" />
            <el-option label="HTML" value="HTML" />
            <el-option label="Markdown" value="MARKDOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="formData.title" placeholder="模板标题，支持变量 {{ticketNo}}" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="模板内容，支持变量插值" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { max-width: 1400px; }
.content-card { overflow: visible; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
