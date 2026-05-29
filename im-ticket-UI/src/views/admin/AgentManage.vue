<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentApi } from '@/api/agent'
import type { Agent } from '@/types'

const loading = ref(false)
const list = ref<Agent[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '', role: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增客服')
const formData = reactive<Partial<Agent>>({})
const editingId = ref<number | null>(null)

const roleLabelMap: Record<string, string> = { ADMIN: '管理员', AGENT: '客服', SUPERVISOR: '主管' }
const statusTagMap: Record<string, string> = { ONLINE: 'success', OFFLINE: 'info', BREAK: 'warning' }
const statusLabelMap: Record<string, string> = { ONLINE: '在线', OFFLINE: '离线', BREAK: '休息' }

async function fetchList() {
  loading.value = true
  try {
    const params: any = { ...query }
    if (!params.role) delete params.role
    if (!params.keyword) delete params.keyword
    const res = await agentApi.list(params)
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = '新增客服'
  editingId.value = null
  Object.assign(formData, { username: '', realName: '', email: '', phone: '', role: 'AGENT' as const, wecomUserId: '', feishuOpenId: '', maxConcurrent: 10 })
  dialogVisible.value = true
}

function openEdit(row: Agent) {
  dialogTitle.value = '编辑客服'
  editingId.value = row.id
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await agentApi.update(editingId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await agentApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: Agent) {
  try {
    await ElMessageBox.confirm(`确认删除客服 "${row.realName}"？`, '删除确认', { type: 'warning' })
    await agentApi.delete(row.id)
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
      <h1 class="page-title">客服管理</h1>
      <p class="page-subtitle">管理系统客服人员账号与权限</p>
    </div>

    <el-card shadow="never" class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索姓名/登录名..." :prefix-icon="Search" clearable style="width: 220px" @keyup.enter="fetchList" @clear="fetchList" />
          <el-select v-model="query.role" placeholder="角色" clearable style="width: 120px" @change="fetchList">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="客服" value="AGENT" />
            <el-option label="主管" value="SUPERVISOR" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="fetchList">搜索</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增客服</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="登录名" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">{{ roleLabelMap[row.role] || row.role }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] as any" size="small" effect="light">
              {{ statusLabelMap[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="wecomUserId" label="企微UserId" width="160" show-overflow-tooltip />
        <el-table-column prop="feishuOpenId" label="飞书OpenId" width="180" show-overflow-tooltip />
        <el-table-column label="负载" width="80">
          <template #default="{ row }">{{ row.currentLoad }}/{{ row.maxConcurrent }}</template>
        </el-table-column>
        <el-table-column prop="lastActiveAt" label="最后活跃" width="180" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="登录名" required>
          <el-input v-model="formData.username" placeholder="请输入登录名" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="formData.role" style="width: 100%">
            <el-option label="客服" value="AGENT" />
            <el-option label="主管" value="SUPERVISOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="企微UserId">
          <el-input v-model="formData.wecomUserId" placeholder="企业微信用户ID" />
        </el-form-item>
        <el-form-item label="飞书OpenId">
          <el-input v-model="formData.feishuOpenId" placeholder="飞书OpenId" />
        </el-form-item>
        <el-form-item label="最大并发数">
          <el-input-number v-model="formData.maxConcurrent" :min="1" :max="100" />
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
