import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Ticket } from '@/types'

export const useWebSocketStore = defineStore('websocket', () => {
  const connected = ref(false)
  const newTickets = ref<Ticket[]>([])
  const unreadCount = ref(0)

  let stompClient: any = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = 1000

  function connect(token: string) {
    // 动态导入 SockJS 和 STOMP（避免构建依赖问题，使用浏览器原生 WebSocket + 简单 STOMP 模拟）
    // 实际项目中应使用 @stomp/stompjs + sockjs-client
    tryConnect(token)
  }

  function tryConnect(token: string) {
    const wsUrl = `ws://${window.location.host}/ws`
    const socket = new WebSocket(wsUrl)

    socket.onopen = () => {
      connected.value = true
      reconnectDelay = 1000
      // 订阅工单通知
      socket.send(JSON.stringify({ type: 'SUBSCRIBE', destination: '/topic/tickets/new', token }))
    }

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'NEW_TICKET') {
          newTickets.value.unshift(data.payload)
          unreadCount.value++
        }
      } catch {
        // ignore parse errors
      }
    }

    socket.onclose = () => {
      connected.value = false
      scheduleReconnect(token)
    }

    socket.onerror = () => {
      socket.close()
    }

    stompClient = socket
  }

  function scheduleReconnect(token: string) {
    if (reconnectTimer) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      reconnectDelay = Math.min(reconnectDelay * 2, 30000)
      tryConnect(token)
    }, reconnectDelay)
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (stompClient) {
      stompClient.close()
      stompClient = null
    }
    connected.value = false
  }

  function clearNotifications() {
    newTickets.value = []
    unreadCount.value = 0
  }

  return { connected, newTickets, unreadCount, connect, disconnect, clearNotifications }
})
