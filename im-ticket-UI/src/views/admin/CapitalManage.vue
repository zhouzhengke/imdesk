<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { capitalApi } from '@/api/capital'
import type { Capital } from '@/types'

const loading = ref(false)
const list = ref<Capital[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, keyword: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增资方')
const formData = reactive<Partial<Capital>>({})
const editingId = ref<number | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await capitalApi.list(query)
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogTitle.value = '新增资方'
  editingId.value = null
  Object.assign(formData, { name: '', contactPerson: '', contactPhone: '', contractStartDate: '', contractEndDate: '', status: 'ACTIVE' as const, remark: '' })
  dialogVisible.value = true
}

function openEdit(row: Capital) {
  dialogTitle.value = '编辑资方'
  editingId.value = row.id
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await capitalApi.update(editingId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await capitalApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: Capital) {
  try {
    await ElMessageBox.confirm(`确认删除资方 "${row.name}"？`, '删除确认', { type: 'warning' })
    await capitalApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // 取消
  }
}

function handlePageChange(page: number) { query.page = page; fetchList() }
function handleSizeChange(size: number) { query.size = size; query.page = 1; fetchList() }

onMounted(() => fetchList())
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">资方管理</h1>
      <p class="page-subtitle">管理对接的资方企业信息</p>
    </div>

    <el-card shadow="never" class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="query.keyword" placeholder="搜索资方名称..." :prefix-icon="Search" clearable style="width: 240px" @keyup.enter="fetchList" @clear="fetchList" />
          <el-button type="primary" :icon="Search" @click="fetchList">搜索</el-button>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增资方</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="资方名称" width="180" />
        <el-table-column prop="contactPerson" label="对接人" width="120" />
        <el-table-column prop="contactPhone" label="电话" width="140" />
        <el-table-column prop="contractStartDate" label="合同开始" width="120" />
        <el-table-column prop="contractEndDate" label="合同结束" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="formData" label-width="90px">
        <el-form-item label="资方名称" required>
          <el-input v-model="formData.name" placeholder="请输入资方名称" />
        </el-form-item>
        <el-form-item label="对接人">
          <el-input v-model="formData.contactPerson" placeholder="请输入对接人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="合同开始日期">
          <el-date-picker v-model="formData.contractStartDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="合同结束日期">
          <el-date-picker v-model="formData.contractEndDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息" />
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
