<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shiftApi } from '@/api/shift'
import { agentApi } from '@/api/agent'
import type { Shift, ShiftSchedule, Agent } from '@/types'

const loading = ref(false)
const shifts = ref<Shift[]>([])
const schedules = ref<ShiftSchedule[]>([])
const agents = ref<Agent[]>([])
const totalSchedules = ref(0)
const scheduleQuery = reactive({ page: 1, size: 20 })

// 班次对话框
const shiftDialogVisible = ref(false)
const shiftDialogTitle = ref('新增班次')
const shiftForm = reactive<Partial<Shift>>({})
const editingShiftId = ref<number | null>(null)

// 排班对话框
const scheduleDialogVisible = ref(false)
const scheduleForm = reactive({ agentId: undefined as number | undefined, shiftId: undefined as number | undefined, scheduleDate: '', isBackup: false })

async function fetchData() {
  loading.value = true
  try {
    const [shiftRes, schedRes, agentRes] = await Promise.all([
      shiftApi.listShifts({ page: 1, size: 50 }),
      shiftApi.listSchedules(scheduleQuery),
      agentApi.list({ page: 1, size: 100 })
    ])
    shifts.value = shiftRes.data.data.records
    schedules.value = schedRes.data.data.records
    totalSchedules.value = schedRes.data.data.total
    agents.value = agentRes.data.data.records
  } finally {
    loading.value = false
  }
}

// ===================== 班次操作 =====================
function openCreateShift() {
  shiftDialogTitle.value = '新增班次'
  editingShiftId.value = null
  Object.assign(shiftForm, { name: '', startTime: '09:00', endTime: '18:00', description: '' })
  shiftDialogVisible.value = true
}

function openEditShift(row: Shift) {
  shiftDialogTitle.value = '编辑班次'
  editingShiftId.value = row.id
  Object.assign(shiftForm, { ...row })
  shiftDialogVisible.value = true
}

async function handleSaveShift() {
  try {
    if (editingShiftId.value) {
      await shiftApi.updateShift(editingShiftId.value, shiftForm)
      ElMessage.success('更新成功')
    } else {
      await shiftApi.createShift(shiftForm)
      ElMessage.success('创建成功')
    }
    shiftDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteShift(row: Shift) {
  try {
    await ElMessageBox.confirm(`确认删除班次 "${row.name}"？`, '删除确认', { type: 'warning' })
    await shiftApi.deleteShift(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 取消 */ }
}

// ===================== 排班操作 =====================
function openCreateSchedule() {
  Object.assign(scheduleForm, { agentId: undefined, shiftId: undefined, scheduleDate: '', isBackup: false })
  scheduleDialogVisible.value = true
}

async function handleSaveSchedule() {
  try {
    if (!scheduleForm.agentId || !scheduleForm.shiftId || !scheduleForm.scheduleDate) {
      ElMessage.warning('请完善排班信息')
      return
    }
    await shiftApi.createSchedule({
      agentId: scheduleForm.agentId,
      shiftId: scheduleForm.shiftId,
      scheduleDate: scheduleForm.scheduleDate,
      isBackup: scheduleForm.isBackup
    })
    ElMessage.success('排班成功')
    scheduleDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('排班失败')
  }
}

async function handleDeleteSchedule(row: ShiftSchedule) {
  try {
    await ElMessageBox.confirm('确认删除此排班记录？', '删除确认', { type: 'warning' })
    await shiftApi.deleteSchedule(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 取消 */ }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">值班排班</h1>
      <p class="page-subtitle">管理客服班次与排班计划</p>
    </div>

    <div class="shift-layout" v-loading="loading">
      <!-- 班次表格 -->
      <el-card shadow="never">
        <div class="section-header">
          <span class="card-title">班次管理</span>
          <el-button :icon="Plus" type="primary" size="small" @click="openCreateShift">新增班次</el-button>
        </div>
        <el-table :data="shifts" stripe style="width: 100%">
          <el-table-column prop="name" label="班次名称" width="140" />
          <el-table-column prop="startTime" label="开始时间" width="120" />
          <el-table-column prop="endTime" label="结束时间" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button :icon="Edit" type="primary" link size="small" @click="openEditShift(row)">编辑</el-button>
              <el-button :icon="Delete" type="danger" link size="small" @click="handleDeleteShift(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 排班表格 -->
      <el-card shadow="never">
        <div class="section-header">
          <span class="card-title">排班计划</span>
          <el-button :icon="Plus" type="primary" size="small" @click="openCreateSchedule">新增排班</el-button>
        </div>
        <el-table :data="schedules" stripe style="width: 100%">
          <el-table-column prop="agentName" label="客服" width="120" />
          <el-table-column prop="shiftName" label="班次" width="120" />
          <el-table-column prop="scheduleDate" label="排班日期" width="140" />
          <el-table-column label="是否备班" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isBackup ? 'warning' : 'success'" size="small" effect="light">
                {{ row.isBackup ? '备班' : '主班' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button :icon="Delete" type="danger" link size="small" @click="handleDeleteSchedule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 班次对话框 -->
    <el-dialog v-model="shiftDialogVisible" :title="shiftDialogTitle" width="450px" destroy-on-close>
      <el-form :model="shiftForm" label-width="80px">
        <el-form-item label="班次名称" required>
          <el-input v-model="shiftForm.name" placeholder="如: 早班、晚班" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-picker v-model="shiftForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-picker v-model="shiftForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="shiftForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveShift">确定</el-button>
      </template>
    </el-dialog>

    <!-- 排班对话框 -->
    <el-dialog v-model="scheduleDialogVisible" title="新增排班" width="450px" destroy-on-close>
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="客服" required>
          <el-select v-model="scheduleForm.agentId" placeholder="选择客服" style="width: 100%" filterable>
            <el-option v-for="a in agents" :key="a.id" :label="a.realName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班次" required>
          <el-select v-model="scheduleForm.shiftId" placeholder="选择班次" style="width: 100%">
            <el-option v-for="s in shifts" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排班日期" required>
          <el-date-picker v-model="scheduleForm.scheduleDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="是否备班">
          <el-switch v-model="scheduleForm.isBackup" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSchedule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { max-width: 1400px; }
.shift-layout { display: flex; flex-direction: column; gap: 20px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-title { font-weight: 600; font-size: 15px; }
</style>
