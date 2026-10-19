<template>
  <el-drawer v-model="visible" class="business-agent-drawer" modal-class="agent-drawer-overlay"
    :size="`${drawerWidth}px`" :modal="false"
    :close-on-click-modal="false" :close-on-press-escape="false" append-to-body>
    <template #header>
      <div class="agent-header">
        <div class="agent-brand">
          <span class="agent-brand-icon"><svg-icon icon-class="message" /></span>
          <div>
            <div class="agent-title">业务 Agent</div>
            <div class="agent-subtitle"><span class="status-dot" />已连接 digital-sales</div>
          </div>
        </div>
        <div class="agent-header-actions">
          <el-button text @click="showSessions = !showSessions">{{ showSessions ? '隐藏会话' : '显示会话' }}</el-button>
          <el-button type="primary" plain @click="newConversation">新建对话</el-button>
        </div>
      </div>
    </template>

    <div class="agent-workspace" :class="{ 'sessions-hidden': !showSessions }">
      <aside v-if="showSessions" class="agent-sidebar">
        <div class="sidebar-heading">
          <span>最近会话</span>
          <span class="session-count">{{ sessions.length }}</span>
        </div>
        <div class="session-list">
          <div v-for="session in sessions" :key="session.id" class="agent-session-item"
            :class="{ active: session.id === activeSessionId }" @click="switchSession(session.id)">
            <span class="session-icon"><svg-icon icon-class="message" /></span>
            <div class="session-copy">
              <span class="agent-session-name">{{ session.title }}</span>
              <span class="session-meta">{{ session.messages?.length || 0 }} 条消息</span>
            </div>
            <el-dropdown trigger="click" @command="handleSessionCommand" @click.stop>
              <button class="session-more" title="会话操作" @click.stop>•••</button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'rename', session }">重命名</el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', session }" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <el-button class="sidebar-new" type="primary" plain @click="newConversation">+ 新建对话</el-button>
      </aside>

      <main class="agent-chat">
        <div class="agent-chat-head">
          <div>
            <div class="chat-title">{{ sessions.find(item => item.id === activeSessionId)?.title || '新对话' }}</div>
          </div>
          <el-button v-if="messages.length" text type="danger" @click="clearConversation">清空消息</el-button>
        </div>

        <div class="agent-messages">
          <div v-if="!messages.length" class="agent-empty">
            <div class="empty-icon"><svg-icon icon-class="message" /></div>
            <h3>有什么可以帮你？</h3>
            <p>可以查询库存、订单、客户和售后，也可以发起需要确认的业务操作。</p>
            <div class="quick-actions">
              <button @click="useExample('查询库存低于预警值的商品')">查询低库存商品</button>
              <button @click="useExample('查询今天的销售订单')">查询今日订单</button>
              <button @click="useExample('查询待处理的售后')">查询待处理售后</button>
            </div>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-row"
            :class="[message.role, message.type ? `message-${message.type}` : '']">
            <div class="message-avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>
            <div class="agent-message">
              <div v-if="!['progress', 'confirm'].includes(message.type)">{{ message.text }}</div>
              <div v-if="message.type === 'result' && message.payload?.rows?.length" class="result-table">
                <div class="result-row result-header">
                  <span v-for="column in resultColumns(message.payload.rows)" :key="column">
                    {{ message.payload.columns?.find(item => item.field === column)?.label || resultLabel(column) }}
                  </span>
                </div>
                <div v-for="(row, rowIndex) in message.payload.rows" :key="rowIndex" class="result-row">
                  <span v-for="column in resultColumns(message.payload.rows)" :key="column">
                    {{ row[column] ?? '-' }}
                  </span>
                </div>
              </div>
              <div v-if="message.type === 'progress'" class="processing-note">
                <span class="processing-dot" />
                {{ message.slow ? (message.timedOut ? '查询时间较长，任务仍在后台运行…' : '查询时间较长，Agent 仍在处理…') : '正在查询，请稍候…' }}
              </div>
              <div v-if="message.type === 'missing' && message.payload?.type === 'clarification' && message.payload?.options?.length && !message.payload?.selected" class="clarification-options">
                <el-button v-for="option in message.payload.options" :key="option.value" size="small"
                  @click="selectClarification(option)">{{ option.label }}</el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="taskId && [100, 'WAIT_CONFIRM'].includes(taskStatus)" class="agent-confirm">
          <div class="confirmation-content">
            <strong>操作等待确认</strong>
            <div v-if="confirmationPayload" class="confirmation-detail">
              <div class="confirmation-row"><span>操作</span><b>{{ confirmationPayload.summary?.title || confirmationPayload.tool }}</b></div>
              <div v-for="(value, key) in confirmationSummaryRows(confirmationPayload)" :key="key" class="confirmation-row">
                <span>{{ key }}</span><b>{{ value }}</b>
              </div>
              <div v-if="confirmationPayload.summary?.items?.length" class="confirmation-items">
                <div v-for="(item, index) in confirmationPayload.summary.items" :key="index">
                  {{ item.productName || item.skuName || item.skuId }} × {{ item.quantity }}，单价 {{ item.salePrice }}
                </div>
              </div>
            </div>
            <span>确认后将按服务端保存的参数执行。</span>
          </div>
          <div class="confirm-actions">
            <el-button size="small" @click="cancelTask">取消</el-button>
            <el-button type="primary" size="small" @click="confirmTask">确认执行</el-button>
          </div>
        </div>

        <div class="agent-composer">
          <el-input v-model="query" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" maxlength="500"
            resize="none" :placeholder="missingPayload ? '请补充上方要求的信息，Ctrl + Enter 发送' : '输入业务问题，Ctrl + Enter 发送'" @keydown.ctrl.enter="submit" />
          <div class="composer-footer">
            <span>Agent 可能出错，写操作请确认后执行</span>
        <el-button type="primary" :loading="loading" :disabled="!query.trim() || isTaskActive" @click="submit">发送</el-button>
          </div>
        </div>
      </main>
    </div>
    <div class="agent-resize-handle" title="拖动调整宽度" @mousedown="startResize" />
  </el-drawer>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import { createAgentTask, inputAgentTask, getAgentTask, confirmAgentTask, cancelAgentTask, streamAgentTask } from '@/api/agent'

const visible = ref(false)
const query = ref('')
const loading = ref(false)
const taskId = ref('')
const taskStatus = ref('')
const confirmationPayload = ref(null)
const missingPayload = ref(null)
const pendingSelection = ref('')
const messages = ref([])
const drawerWidth = ref(720)
const showSessions = ref(true)
const sessions = ref([])
const activeSessionId = ref('')
let timer
let stopStream
let resizing = false
let confirmingTask = false
let slowTimer
let timeoutTimer

const isTaskActive = computed(() => Boolean(taskId.value)
  && !missingPayload.value
  && ![-1, 'FINISH', 'CANCELLED', 'WAIT_CONFIRM', 'WAIT_INPUT'].includes(taskStatus.value))

function createSession() {
  return { id: `${Date.now()}-${Math.random().toString(16).slice(2)}`, title: '新对话', messages: [], conversationState: {} }
}

function persistSessions() {
  localStorage.setItem('digital-sales-agent-sessions', JSON.stringify(sessions.value))
}

function ensureSession() {
  if (!sessions.value.length) sessions.value.push(createSession())
  if (!activeSessionId.value) activeSessionId.value = sessions.value[0].id
  persistSessions()
}

function saveActiveSession() {
  const session = sessions.value.find(item => item.id === activeSessionId.value)
  if (!session) return
  session.messages = messages.value
  session.taskId = taskId.value
  session.taskStatus = taskStatus.value
  session.confirmationPayload = confirmationPayload.value
  session.missingPayload = missingPayload.value
  session.conversationState = session.conversationState || {}
  const firstUserMessage = messages.value.find(item => item.role === 'user')
  if (firstUserMessage) session.title = firstUserMessage.text.slice(0, 24)
  persistSessions()
}

function loadSession(id) {
  const session = sessions.value.find(item => item.id === id)
  if (!session) return
  activeSessionId.value = id
  messages.value = session.messages || []
  taskId.value = session.taskId || ''
  taskStatus.value = session.taskStatus || ''
  confirmationPayload.value = session.confirmationPayload || null
  missingPayload.value = session.missingPayload || ([101, 'WAIT_INPUT'].includes(taskStatus.value) ? { type: 'missing_scope' } : null)
  confirmingTask = false
  query.value = ''
  if (taskId.value && ![-1, 'FINISH', 'CANCELLED'].includes(taskStatus.value)) poll()
}

function switchSession(id) {
  if (id === activeSessionId.value) return
  if (stopStream) stopStream()
  clearTimeout(timer)
  clearTaskTimers()
  saveActiveSession()
  loadSession(id)
}

function deleteSession(id) {
  const index = sessions.value.findIndex(item => item.id === id)
  if (index < 0) return
  sessions.value.splice(index, 1)
  if (id === activeSessionId.value) {
    const next = sessions.value[index] || sessions.value[index - 1]
    if (next) loadSession(next.id)
    else {
      const session = createSession()
      sessions.value.push(session)
      loadSession(session.id)
    }
  }
  persistSessions()
}

function renameSession(session) {
  ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
    inputValue: session.title,
    inputPlaceholder: '会话名称',
    inputValidator: value => value?.trim() ? true : '会话名称不能为空',
    confirmButtonText: '保存',
    cancelButtonText: '取消'
  }).then(({ value }) => {
    session.title = value.trim().slice(0, 30)
    persistSessions()
  }).catch(() => {})
}

function handleSessionCommand(command) {
  if (command.action === 'rename') {
    renameSession(command.session)
    return
  }
  ElMessageBox.confirm(`确定删除会话“${command.session.title}”吗？`, '删除会话', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => deleteSession(command.session.id)).catch(() => {})
}

function resetConversation() {
  if (stopStream) stopStream()
  clearTimeout(timer)
  clearTaskTimers()
  query.value = ''
  taskId.value = ''
  taskStatus.value = ''
  confirmationPayload.value = null
  missingPayload.value = null
  confirmingTask = false
  messages.value = []
  const session = sessions.value.find(item => item.id === activeSessionId.value)
  if (session) session.conversationState = {}
  loading.value = false
  saveActiveSession()
}

function newConversation() {
  if (stopStream) stopStream()
  clearTimeout(timer)
  clearTaskTimers()
  saveActiveSession()
  const session = createSession()
  sessions.value.unshift(session)
  loadSession(session.id)
  persistSessions()
}

function clearConversation() {
  resetConversation()
}

function startResize(event) {
  resizing = true
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', resizeDrawer)
  document.addEventListener('mouseup', stopResize, { once: true })
  event.preventDefault()
}

function resizeDrawer(event) {
  if (!resizing) return
  const maxWidth = Math.min(window.innerWidth - 240, 1000)
  drawerWidth.value = Math.max(420, Math.min(maxWidth, window.innerWidth - event.clientX))
}

function stopResize() {
  resizing = false
  document.body.style.userSelect = ''
  document.removeEventListener('mousemove', resizeDrawer)
}

function open() {
  const stored = localStorage.getItem('digital-sales-agent-sessions')
  try { sessions.value = stored ? JSON.parse(stored) : [] } catch { sessions.value = [] }
  ensureSession()
  loadSession(activeSessionId.value)
  visible.value = true
}

function useExample(text) {
  query.value = text
}

const resultLabels = {
  productName: '商品名称', skuName: '规格名称', skuCode: 'SKU编码',
  stock: '当前库存', warnStock: '预警库存', customerName: '客户名称',
  phone: '联系电话', orderNo: '订单编号', status: '状态', totalAmount: '金额'
}

function resultColumns(rows) {
  return Object.keys(rows?.[0] || {}).filter(key => !['createBy', 'createTime', 'updateBy', 'updateTime', 'remark'].includes(key))
}

function resultLabel(column) {
  return resultLabels[column] || column
}

function buildAgentContext() {
  const contextMessages = messages.value
    .filter(message => ['user', 'result'].includes(message.role) && message.type !== 'progress' && message.type !== 'confirm')
    .slice(-8)
    .map(message => ({
      role: message.role === 'user' ? 'user' : 'assistant',
      content: message.text,
      data: message.payload?.rows ? {
        total: message.payload.total,
        rows: message.payload.rows.slice(0, 30)
      } : undefined
    }))
  return {
    contexts: contextMessages,
    isContext: contextMessages.some(message => message.role === 'assistant'),
    contextNumber: contextMessages.length,
    conversationState: sessions.value.find(item => item.id === activeSessionId.value)?.conversationState || {}
  }
}

function updateConversationState(payload) {
  const rows = payload?.rows || []
  const session = sessions.value.find(item => item.id === activeSessionId.value)
  if (!session) return
  const nextState = { ...(session.conversationState || {}) }
  if (payload?.queryPlan) nextState.lastQueryPlan = payload.queryPlan
  if (rows.length === 1 && rows[0]) {
    const customer = rows[0]
    nextState.currentCustomer = { customerId: customer.customerId, customerName: customer.customerName }
  }
  session.conversationState = nextState
}

function submit() {
  if (!query.value.trim() || loading.value || isTaskActive.value) return
  const text = query.value.trim()
  messages.value.push({ role: 'user', text })
  const context = buildAgentContext()
  const continuationTaskId = missingPayload.value && taskId.value && [101, 'WAIT_INPUT'].includes(taskStatus.value)
    ? taskId.value : ''
  const selection = pendingSelection.value
  missingPayload.value = null
  pendingSelection.value = ''
  saveActiveSession()
  query.value = ''
  loading.value = true
  const request = continuationTaskId
    ? inputAgentTask(continuationTaskId, text, selection)
    : createAgentTask(text, context)
  request.then(res => {
    taskId.value = res.task_id || res.data?.task_id
    taskStatus.value = 'RUNNING'
    startTaskTimers(taskId.value)
    poll()
    stopStream = streamAgentTask(taskId.value, updateTask, () => poll())
    saveActiveSession()
  }).catch(() => {
    messages.value.push({ role: 'error', text: 'Agent 暂时不可用，请稍后重试。' })
  }).finally(() => { loading.value = false })
}

function selectClarification(option) {
  const clarification = messages.value.find(message => message.payload?.type === 'clarification' && !message.payload?.selected)
  if (clarification) clarification.payload.selected = option.value || option.label
  query.value = option.label
  pendingSelection.value = option.value || ''
  submit()
}

function updateTaskMessage(task) {
  const currentTaskId = task.task_id || taskId.value
  const status = task.status ?? task.task_status ?? ''
  const text = task.systemOutput || task.result || task.message
  if (!text) return

  const isWaiting = [100, 'WAIT_CONFIRM'].includes(status)
  const isMissing = [101, 'WAIT_INPUT'].includes(status)
  const isFinished = [-1, 'FINISH', 'CANCELLED'].includes(status)
  const taskMessageIndex = messages.value.findIndex(item => item.taskId === currentTaskId && item.role === 'assistant')
  const payload = task.resultPayload || task.result_payload

  if (isFinished) {
    if (taskMessageIndex >= 0) messages.value.splice(taskMessageIndex, 1)
    const hasResult = messages.value.some(item => item.taskId === currentTaskId && item.type === 'result')
    updateConversationState(payload)
    if (!hasResult) messages.value.push({ role: 'assistant', type: 'result', taskId: currentTaskId, text, payload })
  } else if (taskMessageIndex >= 0) {
    messages.value[taskMessageIndex].text = text
    messages.value[taskMessageIndex].payload = payload
    messages.value[taskMessageIndex].type = isWaiting ? 'confirm' : (isMissing ? 'missing' : 'progress')
  } else {
    messages.value.push({ role: 'assistant', type: isWaiting ? 'confirm' : (isMissing ? 'missing' : 'progress'), taskId: currentTaskId, text, payload })
  }
  saveActiveSession()
}

function clearTaskTimers() {
  clearTimeout(slowTimer)
  clearTimeout(timeoutTimer)
  slowTimer = undefined
  timeoutTimer = undefined
}

function markTaskSlow(taskId, timedOut = false) {
  const message = messages.value.find(item => item.taskId === taskId && item.type === 'progress')
  if (message) {
    message.slow = true
    message.timedOut = timedOut
  }
}

function startTaskTimers(currentTaskId) {
  clearTaskTimers()
  slowTimer = setTimeout(() => markTaskSlow(currentTaskId), 60000)
  timeoutTimer = setTimeout(() => markTaskSlow(currentTaskId, true), 120000)
}

function confirmationSummaryRows(payload) {
  const summary = payload?.summary || {}
  return Object.fromEntries(Object.entries(summary).filter(([key, value]) => key !== 'title' && key !== 'items' && value !== undefined && value !== null && value !== ''))
}

function updateTask(payload) {
  const task = payload.task || payload
  if (confirmingTask && [100, 'WAIT_CONFIRM'].includes(task.status ?? task.task_status)) return
  confirmingTask = false
  taskStatus.value = task.status ?? task.task_status ?? ''
  if (task.confirmationPayload || task.confirmation_payload) confirmationPayload.value = task.confirmationPayload || task.confirmation_payload
  const resultPayload = task.resultPayload || task.result_payload
  if (['missing_parameters', 'missing_scope', 'pending_agent_query', 'clarification'].includes(resultPayload?.type)
      && [101, 'WAIT_INPUT'].includes(taskStatus.value)) {
    missingPayload.value = resultPayload
  } else if (![101, 'WAIT_INPUT'].includes(taskStatus.value)) {
    missingPayload.value = null
  }
  updateTaskMessage(task)
  if ([100, 101, -1, 'WAIT_CONFIRM', 'WAIT_INPUT', 'FINISH', 'CANCELLED'].includes(taskStatus.value)) {
    clearTimeout(timer)
    clearTaskTimers()
    if (stopStream) stopStream()
  }
}

function poll() {
  if (!taskId.value) return
  clearTimeout(timer)
  const currentTaskId = taskId.value
  getAgentTask(currentTaskId).then(res => {
    if (currentTaskId !== taskId.value) return
    updateTask(res.data || res)
  }).catch(() => {}).finally(() => {
    if (currentTaskId === taskId.value && ![100, 101, -1, 'WAIT_CONFIRM', 'WAIT_INPUT', 'FINISH', 'CANCELLED'].includes(taskStatus.value)) {
      timer = setTimeout(poll, 1200)
    }
  })
}

function confirmTask() {
  loading.value = true
  confirmAgentTask(taskId.value, '确认执行').then(() => {
    confirmingTask = true
    taskStatus.value = 'RUNNING'
    startTaskTimers(taskId.value)
    poll()
    stopStream = streamAgentTask(taskId.value, updateTask, () => poll())
  }).finally(() => { loading.value = false })
}

function cancelTask() {
  cancelAgentTask(taskId.value).then(() => {
    if (stopStream) stopStream()
    taskStatus.value = 'CANCELLED'
    updateTaskMessage({ task_id: taskId.value, status: 'CANCELLED', message: '任务已取消。' })
  })
}

defineExpose({ open })
</script>

<style scoped>
:global(.business-agent-drawer .el-drawer__header) { margin: 0; padding: 16px 20px; border-bottom: 1px solid #e8edf3; }
:global(.business-agent-drawer .el-drawer__body) { padding: 0; overflow: hidden; }
:global(.agent-drawer-overlay) { pointer-events: none !important; background: transparent !important; }
:global(.agent-drawer-overlay .el-drawer) { pointer-events: auto !important; }
.agent-header { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.agent-brand { display: flex; align-items: center; gap: 10px; min-width: 0; }
.agent-brand-icon { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; color: #fff; background: linear-gradient(135deg, #409eff, #6c63ff); font-size: 18px; }
.agent-title { color: #1f2937; font-size: 16px; font-weight: 600; line-height: 1.4; }
.agent-subtitle { display: flex; align-items: center; gap: 5px; color: #8a94a3; font-size: 12px; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 0 3px rgba(34, 197, 94, .12); }
.agent-header-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.agent-workspace { height: 100%; min-height: 0; display: grid; grid-template-columns: 220px minmax(0, 1fr); background: #f7f9fc; }
.agent-workspace.sessions-hidden { grid-template-columns: minmax(0, 1fr); }
.agent-sidebar { min-width: 0; min-height: 0; padding: 16px 12px; display: flex; flex-direction: column; gap: 12px; overflow: hidden; border-right: 1px solid #e8edf3; background: #f7f9fc; }
.sidebar-heading { padding: 0 6px; display: flex; align-items: center; justify-content: space-between; color: #667085; font-size: 12px; font-weight: 600; }
.session-count { min-width: 22px; padding: 1px 6px; text-align: center; border-radius: 10px; background: #e8eef7; color: #718096; }
.session-list { flex: 1; min-height: 0; overflow-y: scroll; display: flex; flex-direction: column; gap: 4px; scrollbar-width: thin; scrollbar-color: #98a9bd #e8eef5; }
.session-list::-webkit-scrollbar { width: 6px; }
.session-list::-webkit-scrollbar-track { border-radius: 6px; background: #e8eef5; }
.session-list::-webkit-scrollbar-thumb { border-radius: 6px; background: #c4cfdd; }
.session-list::-webkit-scrollbar-thumb:hover { background: #98a9bd; }
.agent-session-item { position: relative; min-height: 58px; padding: 8px 7px 8px 9px; display: flex; align-items: center; gap: 9px; border: 1px solid transparent; border-radius: 10px; cursor: pointer; transition: .18s ease; }
.agent-session-item::before { content: ''; position: absolute; left: -1px; top: 14px; bottom: 14px; width: 3px; border-radius: 0 3px 3px 0; background: transparent; }
.agent-session-item:hover { background: #fff; border-color: #e5eaf2; box-shadow: 0 3px 10px rgba(31, 41, 55, .04); }
.agent-session-item.active { background: #edf5ff; border-color: #d7e8ff; }
.agent-session-item.active::before { background: #409eff; }
.session-icon { width: 30px; height: 30px; flex: 0 0 30px; display: grid; place-items: center; border-radius: 8px; color: #667085; background: #eef1f5; font-size: 14px; }
.agent-session-item.active .session-icon { color: #409eff; background: #dcecff; }
.session-copy { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.agent-session-name { overflow: hidden; color: #344054; font-size: 13px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.session-meta { color: #98a2b3; font-size: 11px; }
.session-more { width: 28px; height: 28px; flex: 0 0 28px; padding: 0 0 7px; border: 0; border-radius: 7px; color: #98a2b3; background: transparent; font-size: 15px; letter-spacing: 1px; cursor: pointer; opacity: 0; transition: .18s; }
.session-more:hover { color: #409eff; background: rgba(64, 158, 255, .1); }
.agent-session-item:hover .session-more, .agent-session-item.active .session-more { opacity: 1; }
.sidebar-new { width: 100%; }
.agent-chat { min-width: 0; min-height: 0; display: flex; flex-direction: column; background: #fff; }
.agent-chat-head { padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; gap: 12px; border-bottom: 1px solid #edf0f5; }
.chat-title { max-width: 360px; overflow: hidden; color: #344054; font-size: 14px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.agent-messages { flex: 1; min-height: 0; overflow-y: auto; padding: 20px 24px; }
.agent-empty { height: 100%; min-height: 320px; display: flex; align-items: center; justify-content: center; flex-direction: column; text-align: center; color: #667085; }
.empty-icon { width: 58px; height: 58px; display: grid; place-items: center; border-radius: 18px; color: #409eff; background: #edf6ff; font-size: 26px; }
.agent-empty h3 { margin: 16px 0 6px; color: #1f2937; font-size: 18px; }
.agent-empty p { max-width: 410px; margin: 0; color: #98a2b3; font-size: 13px; line-height: 1.7; }
.quick-actions { margin-top: 18px; display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
.quick-actions button { padding: 7px 11px; border: 1px solid #d9e6f5; border-radius: 16px; color: #527096; background: #fff; cursor: pointer; transition: .18s; }
.quick-actions button:hover { color: #409eff; border-color: #9ecbff; background: #f5faff; }
.message-row { margin: 12px 0; display: flex; align-items: flex-start; gap: 9px; }
.message-row.user { flex-direction: row-reverse; }
.message-avatar { width: 30px; height: 30px; flex: 0 0 30px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: #667085; font-size: 11px; font-weight: 600; }
.message-row.user .message-avatar { background: #409eff; }
.agent-message { max-width: 78%; padding: 10px 13px; border-radius: 4px 13px 13px 13px; color: #344054; background: #f2f4f7; white-space: pre-wrap; line-height: 1.65; word-break: break-word; }
.message-row.user .agent-message { border-radius: 13px 4px 13px 13px; color: #fff; background: #409eff; }
.message-row.error .agent-message { color: #b42318; background: #fef3f2; }
.message-row.message-missing .agent-message { color: #7a4b00; background: #fff8e8; border: 1px solid #f6d58d; }
.message-row.message-progress .agent-message { color: #667085; background: #f8fafc; font-size: 12px; }
.clarification-options { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 6px; }
.clarification-options .el-button { min-width: 0; margin: 0; padding-left: 6px; padding-right: 6px; }
.processing-note { margin-top: 6px; display: flex; align-items: center; gap: 6px; color: #667085; }
.processing-dot { width: 6px; height: 6px; border-radius: 50%; background: #409eff; animation: processing-pulse 1s ease-in-out infinite alternate; }
@keyframes processing-pulse { to { opacity: .3; } }
.message-row.message-confirm { display: none; }
.result-summary { margin-top: 10px; color: #344054; font-size: 12px; font-weight: 600; }
.result-table { width: 100%; margin-top: 8px; }
.result-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(88px, 1fr)); gap: 8px; padding: 6px 0; border-top: 1px solid #e4e7ed; font-size: 12px; line-height: 1.45; }
.result-row span { min-width: 0; overflow-wrap: anywhere; }
.result-header { color: #667085; font-weight: 600; }
.result-complete { margin-top: 7px; color: #98a2b3; font-size: 11px; }
.agent-confirm { margin: 0 20px 12px; padding: 12px 14px; display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; border: 1px solid #fedf89; border-radius: 10px; color: #7a2e0e; background: #fffaeb; }
.confirmation-content { min-width: 0; display: flex; flex-direction: column; gap: 7px; }
.confirmation-detail { padding: 8px 10px; border: 1px solid #f7d48a; border-radius: 8px; background: rgba(255,255,255,.7); }
.confirmation-row { display: flex; justify-content: space-between; gap: 16px; line-height: 1.7; font-size: 12px; }
.confirmation-row span { color: #8b5e34; }
.confirmation-row b { color: #5f371c; text-align: right; word-break: break-word; }
.confirmation-items { margin-top: 5px; padding-top: 5px; border-top: 1px dashed #e8c77d; color: #6f4524; font-size: 12px; line-height: 1.7; }
.agent-confirm span { color: #a15c22; font-size: 12px; }
.confirm-actions { display: flex; flex-shrink: 0; }
.agent-composer { margin: 0 20px 18px; padding: 10px 12px 9px; border: 1px solid #d7deea; border-radius: 12px; background: #fff; box-shadow: 0 6px 20px rgba(31, 41, 55, .07); transition: border-color .2s, box-shadow .2s; }
.agent-composer:focus-within { border-color: #87bfff; box-shadow: 0 6px 22px rgba(64, 158, 255, .13); }
.agent-composer :deep(.el-textarea__inner) { padding: 0; border: 0; box-shadow: none; }
.composer-footer { margin-top: 8px; display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #98a2b3; font-size: 11px; }
.agent-resize-handle { position: absolute; left: 0; top: 0; bottom: 0; width: 7px; cursor: ew-resize; z-index: 10; transition: background .2s; }
.agent-resize-handle:hover { background: rgba(64, 158, 255, .22); }
@media (max-width: 720px) {
  .agent-workspace { grid-template-columns: 170px minmax(0, 1fr); }
  .agent-header-actions .el-button:first-child { display: none; }
  .agent-messages { padding: 16px; }
  .agent-composer { margin: 0 14px 14px; }
}
</style>
