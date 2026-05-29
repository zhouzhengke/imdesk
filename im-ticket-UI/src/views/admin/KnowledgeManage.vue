<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Edit, Delete, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import type { KnowledgeFaq, KnowledgeDocument } from '@/types'

const loading = ref(false)
const activeTab = ref('faq')

// ===================== FAQ =====================
const faqs = ref<KnowledgeFaq[]>([])
const faqTotal = ref(0)
const faqQuery = reactive({ page: 1, size: 20, keyword: '', category: '' })
const faqDialogVisible = ref(false)
const faqDialogTitle = ref('新增FAQ')
const faqForm = reactive<Partial<KnowledgeFaq>>({})
const editingFaqId = ref<number | null>(null)

async function fetchFaqs() {
  loading.value = true
  try {
    const params: any = { ...faqQuery }
    if (!params.keyword) delete params.keyword
    if (!params.category) delete params.category
    const res = await knowledgeApi.listFaqs(params)
    faqs.value = res.data.data.records
    faqTotal.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function openCreateFaq() {
  faqDialogTitle.value = '新增FAQ'
  editingFaqId.value = null
  Object.assign(faqForm, { question: '', answer: '', keywords: '', category: '', priority: 0, enabled: true })
  faqDialogVisible.value = true
}

function openEditFaq(row: KnowledgeFaq) {
  faqDialogTitle.value = '编辑FAQ'
  editingFaqId.value = row.id
  Object.assign(faqForm, { ...row })
  faqDialogVisible.value = true
}

async function handleSaveFaq() {
  try {
    if (editingFaqId.value) {
      await knowledgeApi.updateFaq(editingFaqId.value, faqForm)
      ElMessage.success('更新成功')
    } else {
      await knowledgeApi.createFaq(faqForm)
      ElMessage.success('创建成功')
    }
    faqDialogVisible.value = false
    fetchFaqs()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteFaq(row: KnowledgeFaq) {
  try {
    await ElMessageBox.confirm(`确认删除FAQ "${row.question}"？`, '删除确认', { type: 'warning' })
    await knowledgeApi.deleteFaq(row.id)
    ElMessage.success('删除成功')
    fetchFaqs()
  } catch { /* 取消 */ }
}

// ===================== 文档 =====================
const docs = ref<KnowledgeDocument[]>([])
const docTotal = ref(0)
const docQuery = reactive({ page: 1, size: 20, keyword: '', category: '' })
const uploadRef = ref<any>(null)

async function fetchDocs() {
  loading.value = true
  try {
    const params: any = { ...docQuery }
    if (!params.keyword) delete params.keyword
    if (!params.category) delete params.category
    const res = await knowledgeApi.listDocuments(params)
    docs.value = res.data.data.records
    docTotal.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  if (tab === 'faq') fetchFaqs()
  else fetchDocs()
}

async function handleUploadFile(file: any) {
  const formData = new FormData()
  formData.append('file', file.raw || file)
  try {
    await knowledgeApi.uploadDocument(formData)
    ElMessage.success('上传成功')
    fetchDocs()
  } catch {
    ElMessage.error('上传失败')
  }
  uploadRef.value?.clearFiles()
}

async function handleDeleteDoc(row: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(`确认删除文档 "${row.title}"？`, '删除确认', { type: 'warning' })
    await knowledgeApi.deleteDocument(row.id)
    ElMessage.success('删除成功')
    fetchDocs()
  } catch { /* 取消 */ }
}

function handleFaqPageChange(page: number) { faqQuery.page = page; fetchFaqs() }
function handleFaqSizeChange(size: number) { faqQuery.size = size; faqQuery.page = 1; fetchFaqs() }
function handleDocPageChange(page: number) { docQuery.page = page; fetchDocs() }
function handleDocSizeChange(size: number) { docQuery.size = size; docQuery.page = 1; fetchDocs() }

onMounted(() => fetchFaqs())
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">知识库</h1>
      <p class="page-subtitle">管理FAQ问答对和知识文档</p>
    </div>

    <el-card shadow="never" class="content-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- FAQ 标签页 -->
        <el-tab-pane label="FAQ管理" name="faq">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="faqQuery.keyword" placeholder="搜索问题..." :prefix-icon="Search" clearable style="width: 220px" @keyup.enter="fetchFaqs" @clear="fetchFaqs" />
              <el-button type="primary" :icon="Search" @click="fetchFaqs">搜索</el-button>
            </div>
            <el-button type="primary" :icon="Plus" @click="openCreateFaq">新增FAQ</el-button>
          </div>

          <el-table :data="faqs" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="answer" label="答案" min-width="280" show-overflow-tooltip />
            <el-table-column prop="keywords" label="关键词" width="160" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="priority" label="优先级" width="80" />
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
                <el-button :icon="Edit" type="primary" link size="small" @click.stop="openEditFaq(row)">编辑</el-button>
                <el-button :icon="Delete" type="danger" link size="small" @click.stop="handleDeleteFaq(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="faqQuery.page"
              v-model:page-size="faqQuery.size"
              :total="faqTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="handleFaqPageChange"
              @size-change="handleFaqSizeChange"
            />
          </div>
        </el-tab-pane>

        <!-- 文档标签页 -->
        <el-tab-pane label="文档管理" name="doc">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="docQuery.keyword" placeholder="搜索文档标题..." :prefix-icon="Search" clearable style="width: 220px" @keyup.enter="fetchDocs" @clear="fetchDocs" />
              <el-button type="primary" :icon="Search" @click="fetchDocs">搜索</el-button>
            </div>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleUploadFile"
              accept=".pdf,.doc,.docx,.txt,.md,.html"
            >
              <el-button type="primary" :icon="Upload">上传文档</el-button>
            </el-upload>
          </div>

          <el-table :data="docs" v-loading="loading" stripe style="width: 100%">
            <el-table-column prop="title" label="文档标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileName" label="文件名" width="220" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="80" />
            <el-table-column label="大小" width="100">
              <template #default="{ row }">
                {{ (row.fileSize / 1024).toFixed(1) }} KB
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button :icon="Delete" type="danger" link size="small" @click.stop="handleDeleteDoc(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="docQuery.page"
              v-model:page-size="docQuery.size"
              :total="docTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="handleDocPageChange"
              @size-change="handleDocSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- FAQ 对话框 -->
    <el-dialog v-model="faqDialogVisible" :title="faqDialogTitle" width="560px" destroy-on-close>
      <el-form :model="faqForm" label-width="80px">
        <el-form-item label="问题" required>
          <el-input v-model="faqForm.question" placeholder="常见问题" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="faqForm.answer" type="textarea" :rows="4" placeholder="问题答案" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="faqForm.keywords" placeholder="逗号分隔的关键词" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="faqForm.category" placeholder="如: 审批、账户、功能" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="faqForm.priority" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="faqForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faqDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFaq">确定</el-button>
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
