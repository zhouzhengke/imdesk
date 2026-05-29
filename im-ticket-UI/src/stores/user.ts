import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Agent, AgentStatus } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const currentUser = ref<Agent | null>(null)
  const isLoggedIn = ref(!!token.value)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
    isLoggedIn.value = true
  }

  function setUser(user: Agent) {
    currentUser.value = user
  }

  function updateStatus(status: AgentStatus) {
    if (currentUser.value) {
      currentUser.value.status = status
    }
  }

  function logout() {
    token.value = ''
    currentUser.value = null
    isLoggedIn.value = false
    localStorage.removeItem('token')
  }

  return { token, currentUser, isLoggedIn, setToken, setUser, updateStatus, logout }
})
