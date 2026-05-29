import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/tickets'
    },
    {
      path: '/tickets',
      name: 'TicketList',
      component: () => import('@/views/tickets/TicketList.vue'),
      meta: { title: '工单工作台' }
    },
    {
      path: '/tickets/:id',
      name: 'TicketDetail',
      component: () => import('@/views/tickets/TicketDetail.vue'),
      meta: { title: '工单详情' }
    },
    {
      path: '/duty',
      name: 'DutyPanel',
      component: () => import('@/views/duty/DutyPanel.vue'),
      meta: { title: '值班面板' }
    },
    {
      path: '/admin/capitals',
      name: 'CapitalManage',
      component: () => import('@/views/admin/CapitalManage.vue'),
      meta: { title: '资方管理' }
    },
    {
      path: '/admin/agents',
      name: 'AgentManage',
      component: () => import('@/views/admin/AgentManage.vue'),
      meta: { title: '客服管理' }
    },
    {
      path: '/admin/shifts',
      name: 'ShiftManage',
      component: () => import('@/views/admin/ShiftManage.vue'),
      meta: { title: '值班排班' }
    },
    {
      path: '/admin/templates',
      name: 'TemplateManage',
      component: () => import('@/views/admin/TemplateManage.vue'),
      meta: { title: '通知模板' }
    },
    {
      path: '/admin/knowledge',
      name: 'KnowledgeManage',
      component: () => import('@/views/admin/KnowledgeManage.vue'),
      meta: { title: '知识库' }
    }
  ]
})

export default router
