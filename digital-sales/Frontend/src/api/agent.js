import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function createAgentTask(query, context = {}) {
  return request({
    url: '/agent/tasks',
    method: 'post',
    data: {
      query,
      contexts: context.contexts || [],
      isCopilot: true,
      isContext: Boolean(context.isContext),
      contextNumber: context.contextNumber || 0,
      conversationState: context.conversationState || {}
    }
  })
}

export function getAgentTask(taskId) {
  return request({ url: `/agent/tasks/${taskId}`, method: 'get' })
}

export function confirmAgentTask(taskId, query) {
  return request({ url: `/agent/tasks/${taskId}/confirm`, method: 'post', data: { query } })
}

export function inputAgentTask(taskId, query, selection = '') {
  return request({
    url: `/agent/tasks/${taskId}/input`,
    method: 'post',
    data: { query, ...(selection ? { selection } : {}) }
  })
}

export function cancelAgentTask(taskId) {
  return request({ url: `/agent/tasks/${taskId}/cancel`, method: 'post' })
}

export function streamAgentTask(taskId, onTask, onError) {
  const controller = new AbortController()
  fetch(`${import.meta.env.VITE_APP_BASE_API}/agent/tasks/${taskId}/events`, {
    headers: { Authorization: `Bearer ${getToken()}` },
    signal: controller.signal
  }).then(async response => {
    if (!response.ok || !response.body) throw new Error('SSE unavailable')
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      events.forEach(event => {
        const line = event.split('\n').find(item => item.startsWith('data:'))
        if (line) onTask(JSON.parse(line.slice(5).trim()))
      })
    }
  }).catch(error => {
    if (error.name !== 'AbortError') onError(error)
  })
  return () => controller.abort()
}
