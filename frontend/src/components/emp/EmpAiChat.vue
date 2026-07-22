<template>
  <div class="ai-chat">
    <div class="chat-container">
      <!-- 头部 -->
      <div class="chat-header">
        <div class="header-left">
          <div class="bot-avatar header-avatar">
            <el-icon :size="22"><Service /></el-icon>
          </div>
          <div>
            <p class="title">AI 智能客服 · 小星</p>
            <p class="subtitle">
              <span :class="['status-dot', statusClass]"></span>
              {{ statusText }}
            </p>
          </div>
        </div>
        <el-button class="apple-btn" @click="clearChat">
          <el-icon><Delete /></el-icon>
          <span style="margin-left: 4px">新对话</span>
        </el-button>
      </div>

      <!-- 消息区 -->
      <div class="chat-messages" ref="messagesRef">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['msg-row', msg.role === 'user' ? 'msg-user' : 'msg-assistant']"
        >
          <div v-if="msg.role === 'assistant'" class="bot-avatar">
            <el-icon :size="18"><Service /></el-icon>
          </div>

          <div class="msg-body">
            <div :class="['bubble', msg.role, { 'bubble-error': msg.error }]">
              <template v-if="msg.streaming && !msg.content">
                <span class="typing-inline">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </span>
              </template>
              <template v-else>{{ msg.content }}</template>
            </div>

            <div v-if="msg.via === 'faq'" class="via-tag">来自本地知识库（FAQ 降级模式）</div>

            <!-- 相关问题 / 推荐问题 -->
            <div v-if="msg.related && msg.related.length" class="chips">
              <span class="chips-label">相关问题：</span>
              <el-tag
                v-for="r in msg.related"
                :key="r.question"
                class="chip"
                effect="plain"
                round
                @click="send(r.question)"
              >
                {{ r.question }}
              </el-tag>
            </div>
            <div v-if="msg.suggestions && msg.suggestions.length" class="chips">
              <el-tag
                v-for="s in msg.suggestions"
                :key="s"
                class="chip"
                type="primary"
                effect="plain"
                round
                @click="send(s)"
              >
                {{ s }}
              </el-tag>
            </div>

            <!-- 失败重试 -->
            <div v-if="msg.error && msg.retryQuestion" class="chips">
              <el-button size="small" type="warning" plain round @click="send(msg.retryQuestion)">
                <el-icon><RefreshRight /></el-icon>
                <span style="margin-left: 4px">点击重试</span>
              </el-button>
            </div>
          </div>

          <div v-if="msg.role === 'user'" class="user-avatar">
            <el-icon :size="18"><UserFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="500"
          show-word-limit
          placeholder="请输入您的问题，例如：公司有哪些员工福利？（Enter 发送，Shift+Enter 换行）"
          @keydown.enter.exact.prevent="send()"
        />
        <el-button
          class="send-btn apple-btn apple-btn-primary"
          :disabled="!input.trim() || loading"
          :loading="loading"
          @click="send()"
        >
          <el-icon v-if="!loading"><Promotion /></el-icon>
          <span style="margin-left: 4px">发送</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Service, UserFilled, Delete, Promotion, RefreshRight } from '@element-plus/icons-vue'
import axios from 'axios'

/** AI 对话 API 调用超时（毫秒） */
const AI_TIMEOUT = 60000

interface RelatedQuestion {
  question: string
  score: number
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
  via?: 'llm' | 'faq'
  related?: RelatedQuestion[]
  suggestions?: string[]
  error?: boolean
  retryQuestion?: string
}

const WELCOME_TEXT =
  '您好！我是星辰科技的 AI 智能客服小星~\n可以问我公司简介、主营业务、员工福利、考勤请假制度，以及 OA 系统的使用问题哦。'

const router = useRouter()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const openingSuggestions = ref<string[]>([])

// 大模型在线状态：null=检测中, true=在线, false=离线（FAQ 降级）
const aiOnline = ref<boolean | null>(null)
const ragReady = ref(false)
const modelName = ref('')
const sessionId = ref('emp-' + Math.random().toString(36).slice(2, 10))

const statusClass = computed(() =>
  aiOnline.value === null ? 'checking' : aiOnline.value ? 'online' : 'offline',
)
const statusText = computed(() => {
  if (aiOnline.value === null) return '正在检测大模型状态…'
  if (aiOnline.value) {
    return `大模型在线（${modelName.value || 'qwen2.5:7b'}）${ragReady.value ? '· Spring AI RAG 知识库' : '· 无知识库'}`
  }
  return 'AI 在线 · 知识库问答'
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const pushWelcome = () => {
  messages.value.push({
    role: 'assistant',
    content: WELCOME_TEXT,
    suggestions: openingSuggestions.value.length ? [...openingSuggestions.value] : undefined,
  })
}

/** 探测 OA-2 员工服务的本地大模型是否就绪 */
const checkHealth = async () => {
  try {
    const res = await axios.get('/api/v1/ai/chat/health', { timeout: 6000 })
    const d = res.data?.data
    aiOnline.value = !!(d && d.ollama && d.modelReady)
    ragReady.value = !!d?.ragReady
    modelName.value = d?.model || ''
  } catch {
    aiOnline.value = false
    ragReady.value = false
  }
}

/** 开场推荐问题（来自 oa-ai-service 知识库词条） */
const loadSuggestions = async () => {
  try {
    const res = await axios.get('/api/v1/ai/chat/suggestions')
    if (res.data?.code === 200 && Array.isArray(res.data.data)) {
      openingSuggestions.value = res.data.data
      const first = messages.value[0]
      if (first && first.role === 'assistant') {
        first.suggestions = [...openingSuggestions.value]
      }
    }
  } catch {
    /* 静默降级 */
  }
}

/** 用员工工号作为会话 ID，保证多轮记忆按人隔离 */
const loadProfile = async () => {
  try {
    const res = await axios.get('/api/v1/employee/profile')
    const number = res.data?.data?.number
    if (number) sessionId.value = 'emp-' + number
  } catch {
    /* 未取到工号时使用随机会话 ID */
  }
}

/** 大模型对话（调用 oa-ai-service Spring AI RAG 问答，非流式） */
const streamChat = async (question: string): Promise<void> => {
  const assistantMsg: ChatMessage = { role: 'assistant', content: '', streaming: true, via: 'llm' }
  messages.value.push(assistantMsg)
  scrollToBottom()

  try {
    const res = await axios.post('/api/v1/ai/chat', { message: question, sessionId: sessionId.value }, { timeout: AI_TIMEOUT })
    const body = res.data
    if (body && body.code === 200 && body.data) {
      assistantMsg.content = body.data.reply
      assistantMsg.streaming = false
      assistantMsg.via = 'llm'
      if (body.data.sessionId) {
        sessionId.value = body.data.sessionId
      }
      if (body.data.related?.length) {
        assistantMsg.related = body.data.related
      }
      if (body.data.suggestions?.length) {
        assistantMsg.suggestions = body.data.suggestions
      }
      scrollToBottom()
    } else {
      throw new Error(body?.message || 'AI 服务异常')
    }
  } catch (err) {
    const idx = messages.value.indexOf(assistantMsg)
    if (idx >= 0) {
      messages.value.splice(idx, 1)
    }
    throw err
  }
}

/** FAQ 降级：调用 oa-ai-service 同步对话接口 */
const faqChat = async (question: string): Promise<void> => {
  try {
    const res = await axios.post('/api/v1/ai/chat', { message: question, sessionId: sessionId.value }, { timeout: AI_TIMEOUT })
    const body = res.data
    if (body && body.code === 200 && body.data) {
      messages.value.push({
        role: 'assistant',
        content: body.data.reply,
        via: 'faq',
        related: body.data.related?.length ? body.data.related : undefined,
        suggestions: body.data.suggestions?.length ? body.data.suggestions : undefined,
      })
      if (body.data.sessionId) {
        sessionId.value = body.data.sessionId
      }
    } else {
      throw new Error(body?.message || 'FAQ 服务异常')
    }
  } catch (error: unknown) {
    const status = (error as { response?: { status?: number } })?.response?.status
    if (status === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      router.push('/emp-login')
      return
    }
    messages.value.push({
      role: 'assistant',
      content: 'AI 客服暂时不可用，请检查网络后重试',
      error: true,
      retryQuestion: question,
    })
  }
}

const send = async (text?: string) => {
  const question = (text ?? input.value).trim()
  if (!question || loading.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: question })
  loading.value = true
  scrollToBottom()

  try {
    if (aiOnline.value !== false) {
      try {
        await streamChat(question)
      } catch {
        // 大模型不可用：标记离线并降级 FAQ
        aiOnline.value = false
        try {
          await faqChat(question)
        } catch {
          // faqChat 内部已有错误提示，无需额外处理
        }
      }
    } else {
      try {
        await faqChat(question)
      } catch {
        // faqChat 内部已有错误提示
      }
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = async () => {
  messages.value = []
  try {
    await axios.delete(`/api/v1/ai/chat/${sessionId.value}`)
  } catch { /* 静默忽略 */ }
  pushWelcome()
  checkHealth()
  ElMessage.success('已开启新对话')
}

onMounted(() => {
  pushWelcome()
  loadProfile()
  checkHealth()
  loadSuggestions()
})
</script>

<style scoped>
.ai-chat {
  height: 100%;
  display: flex;
  justify-content: center;
  background: var(--apple-bg);
}

.chat-container {
  width: 100%;
  max-width: 860px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--apple-white);
  border-radius: var(--apple-radius-card);
  box-shadow: var(--apple-shadow);
  overflow: hidden;
}

/* 头部 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--apple-border);
  background: var(--apple-white);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header .title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text);
}

.chat-header .subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--apple-text-secondary);
  display: flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background-color: var(--apple-green);
  box-shadow: 0 0 4px var(--apple-green);
}

.status-dot.offline {
  background-color: var(--apple-orange);
}

.status-dot.checking {
  background-color: var(--apple-text-tertiary);
  animation: blink 1s infinite;
}

@keyframes blink {
  50% { opacity: 0.3; }
}

.chat-header .el-button {
  color: var(--apple-text-secondary);
}

/* 头像 */
.bot-avatar,
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.bot-avatar {
  background: var(--apple-blue);
}

.header-avatar {
  width: 40px;
  height: 40px;
  background: var(--apple-bg-secondary);
  color: var(--apple-text-secondary);
}

.user-avatar {
  background: var(--apple-text-tertiary);
}

/* 消息区 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--apple-bg);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg-user {
  justify-content: flex-end;
}

.msg-body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}

.msg-user .msg-body {
  align-items: flex-end;
}

/* 气泡 */
.bubble {
  padding: 12px 18px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.assistant {
  background: var(--apple-white);
  color: var(--apple-text);
  border-bottom-left-radius: 4px;
  box-shadow: var(--apple-shadow);
}

.bubble.user {
  background: var(--apple-blue);
  color: white;
  border-bottom-right-radius: 4px;
}

.bubble-error {
  background-color: #fef0f0 !important;
  border-color: #fbc4c4 !important;
  color: var(--apple-red) !important;
}

.via-tag {
  margin-top: 4px;
  font-size: 11px;
  color: var(--apple-text-tertiary);
}

/* 相关/推荐问题 chips */
.chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.chips-label {
  font-size: 12px;
  color: var(--apple-text-secondary);
}

.chip {
  cursor: pointer;
}

.chip:hover {
  opacity: 0.8;
}

/* 气泡内打字动画 */
.typing-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 40px;
}

.typing-inline .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: var(--apple-text-tertiary);
  animation: typing-bounce 1.2s infinite ease-in-out;
}

.typing-inline .dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-inline .dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* 输入区 - Apple clean style */
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--apple-border);
  background: var(--apple-white);
  flex-shrink: 0;
}

.chat-input :deep(.el-textarea__inner) {
  border: none !important;
  border-radius: var(--apple-radius) !important;
  padding: 10px 14px !important;
  font-size: 14px !important;
  background: var(--apple-bg) !important;
  box-shadow: none !important;
  resize: none;
}

.chat-input :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.15) !important;
}

.chat-input :deep(.el-input__count) {
  background: transparent !important;
  color: var(--apple-text-tertiary);
  font-size: 11px;
  bottom: 6px;
}

.send-btn {
  height: 40px;
  padding: 0 20px;
  border-radius: var(--apple-radius-button) !important;
  font-weight: 500;
  flex-shrink: 0;
}
</style>
