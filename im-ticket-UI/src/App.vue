<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Document,
  Monitor,
  Setting,
  OfficeBuilding,
  User,
  Clock,
  Bell,
  Collection,
  Fold,
  Expand
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const isCollapsed = ref(false)
const activeMenu = ref('/tickets')

watch(() => route.path, (path) => {
  activeMenu.value = path
}, { immediate: true })

const menuItems = computed(() => [
  { path: '/tickets', title: '工单工作台', icon: Document },
  { path: '/duty', title: '值班面板', icon: Monitor },
  {
    title: '系统管理',
    icon: Setting,
    children: [
      { path: '/admin/capitals', title: '资方管理', icon: OfficeBuilding },
      { path: '/admin/agents', title: '客服管理', icon: User },
      { path: '/admin/shifts', title: '值班排班', icon: Clock },
      { path: '/admin/templates', title: '通知模板', icon: Bell },
      { path: '/admin/knowledge', title: '知识库', icon: Collection }
    ]
  }
])

function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo-area" v-if="!isCollapsed">
          <span class="logo-icon">IM</span>
          <span class="logo-text">工单系统</span>
        </div>
        <div class="logo-area logo-compact" v-else>
          <span class="logo-icon">IM</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.75)"
        active-text-color="#ffffff"
        :collapse="isCollapsed"
        class="sidebar-menu"
      >
        <template v-for="item in menuItems" :key="item.title">
          <el-sub-menu v-if="item.children" :index="item.title">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              @click="navigate(child.path)"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item
            v-else
            :index="item.path"
            @click="navigate(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer">
        <el-button
          :icon="isCollapsed ? Expand : Fold"
          text
          class="collapse-btn"
          @click="isCollapsed = !isCollapsed"
        />
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-area">
      <router-view v-slot="{ Component: ViewComponent }">
        <transition name="page-fade" mode="out-in">
          <component :is="ViewComponent" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style>
/* ===================== 全局重置 ===================== */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --sidebar-width: 220px;
  --sidebar-collapsed-width: 64px;
  --sidebar-bg: #0d1b3e;
  --sidebar-hover: rgba(255, 255, 255, 0.06);
  --sidebar-active: rgba(99, 130, 255, 0.18);
  --accent: #4f6ef7;
  --accent-light: #7b93fa;
  --bg-primary: #f0f2f7;
  --bg-card: #ffffff;
  --text-primary: #1a1f36;
  --text-secondary: #697386;
  --text-muted: #8b95ad;
  --border-color: #e3e8f0;
  --shadow-sm: 0 1px 3px rgba(13, 27, 62, 0.06);
  --shadow-md: 0 4px 16px rgba(13, 27, 62, 0.08);
  --shadow-lg: 0 8px 32px rgba(13, 27, 62, 0.12);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
}

html, body {
  height: 100%;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', -apple-system, sans-serif;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100%;
}

/* ===================== 布局 ===================== */
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ===================== 侧边栏 ===================== */
.sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 100;
  box-shadow: 2px 0 24px rgba(13, 27, 62, 0.15);
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
  min-width: var(--sidebar-collapsed-width);
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-compact {
  gap: 0;
}

.logo-icon {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, var(--accent), var(--accent-light));
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none !important;
  padding: 8px 0;
}

.sidebar-menu .el-menu-item,
.sidebar-menu .el-sub-menu__title {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-sub-menu__title:hover {
  background: var(--sidebar-hover) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: var(--sidebar-active) !important;
  border-left: 3px solid var(--accent);
}

.sidebar-menu .el-sub-menu .el-menu-item {
  padding-left: 52px !important;
}

.sidebar-footer {
  padding: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  justify-content: center;
}

.collapse-btn {
  color: rgba(255, 255, 255, 0.5) !important;
  font-size: 18px;
}

.collapse-btn:hover {
  color: rgba(255, 255, 255, 0.85) !important;
  background: var(--sidebar-hover) !important;
}

/* ===================== 主区域 ===================== */
.main-area {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: var(--bg-primary);
  padding: 24px;
}

/* ===================== 页面过渡 ===================== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===================== Element Plus 全局覆盖 ===================== */
.el-card {
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: var(--shadow-sm) !important;
  transition: box-shadow 0.25s ease;
}

.el-card:hover {
  box-shadow: var(--shadow-md) !important;
}

.el-button--primary {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-hover-bg-color: var(--accent-light);
  --el-button-hover-border-color: var(--accent-light);
  border-radius: var(--radius-sm) !important;
}

.el-button {
  border-radius: var(--radius-sm) !important;
  font-weight: 500;
}

.el-input__wrapper,
.el-select .el-input__wrapper,
.el-date-editor .el-input__wrapper {
  border-radius: var(--radius-sm) !important;
  box-shadow: none !important;
  border: 1px solid var(--border-color);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.el-input__wrapper:hover,
.el-select:hover .el-input__wrapper {
  border-color: var(--accent-light);
}

.el-input.is-focus .el-input__wrapper,
.el-select.is-focus .el-input__wrapper {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(79, 110, 247, 0.12) !important;
}

.el-table {
  --el-table-border-color: var(--border-color);
  --el-table-header-bg-color: #f7f8fb;
  border-radius: var(--radius-md);
  overflow: hidden;
}

.el-table th.el-table__cell {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.el-pagination {
  font-weight: 500;
}

.el-dialog {
  border-radius: var(--radius-lg) !important;
}

.el-tag {
  border-radius: 4px !important;
  font-weight: 500;
}

/* ===================== 页面通用标题 ===================== */
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* ===================== 滚动条 ===================== */
::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>
