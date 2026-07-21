<template>
  <div class="emp-home">
    <!-- Apple-style header: white, thin bottom border -->
    <header class="app-header">
      <div class="header-inner">
        <h1 class="header-greeting">{{ greeting }}, {{ userInfo.name || '员工' }}</h1>
        <div class="header-actions">
          <span class="header-time">{{ currentTime }}</span>
          <!-- 通知铃铛 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
            <el-button @click="showNotifications = !showNotifications" text size="large" class="bell-btn">
              <el-icon :size="22"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <!-- 通知下拉 -->
          <div v-if="showNotifications" class="notification-dropdown">
            <div class="notif-header">
              <span class="notif-title">通知</span>
              <el-button text size="small" @click="markAllRead" v-if="unreadCount > 0">全部已读</el-button>
            </div>
            <div class="notif-list" v-if="notifList.length > 0">
              <div v-for="item in notifList" :key="item.id"
                   class="notif-item" :class="{ 'unread': !item.is_read }"
                   @click="markRead(item)">
                <div class="notif-dot" v-if="!item.is_read"></div>
                <div class="notif-content">
                  <div class="notif-title-text">{{ item.title }}</div>
                  <div class="notif-body">{{ item.content }}</div>
                  <div class="notif-time">{{ item.create_time }}</div>
                </div>
              </div>
            </div>
            <div class="notif-empty" v-else>暂无通知</div>
            <div class="notif-footer">
              <el-button text size="small" @click="goTo('/emp-home/notifications')">查看全部</el-button>
            </div>
          </div>
          <el-button @click="logout" text bg size="small">退出登录</el-button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="app-main">
      <!-- Function Cards: 2x2 Grid -->
      <div class="card-grid">
        <div class="apple-card function-card" @click="goTo('/emp-home/sign-in')">
          <div class="card-icon">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <h3 class="card-name">员工签到</h3>
          <p class="card-desc">每日签到打卡</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/info')">
          <div class="card-icon">
            <el-icon :size="24"><User /></el-icon>
          </div>
          <h3 class="card-name">个人信息</h3>
          <p class="card-desc">查看和编辑资料</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/sign-message')">
          <div class="card-icon">
            <el-icon :size="24"><Document /></el-icon>
          </div>
          <h3 class="card-name">签到记录</h3>
          <p class="card-desc">查看签到历史</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/leave-apply')">
          <div class="card-icon">
            <el-icon :size="24"><Edit /></el-icon>
          </div>
          <h3 class="card-name">请假申请</h3>
          <p class="card-desc">提交请假申请</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/ai-chat')">
          <div class="card-icon">
            <el-icon :size="24"><Service /></el-icon>
          </div>
          <h3 class="card-name">AI 客服</h3>
          <p class="card-desc">智能在线客服</p>
        </div>
      </div>

      <!-- Child Routes -->
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Clock, Document, Service, Edit, Bell } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userInfo = ref<any>({})
const empName = ref<string>('')
const currentTime = ref('')
const unreadCount = ref(0)
const showNotifications = ref(false)
const notifList = ref<any[]>([])
let timer: NodeJS.Timeout
let notifTimer: NodeJS.Timeout

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const updateTime = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}年${m}月${d}日 ${h}:${mi}:${s}`
}

const goTo = (path: string) => {
  router.push(path)
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data && response.data.data) {
      userInfo.value = response.data.data
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
  }
  // 启动通知轮询
  fetchNotifications()
  notifTimer = setInterval(fetchNotifications, 10000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (notifTimer) clearInterval(notifTimer)
})

const logout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    try {
      const response = await axios.post('/api/v1/employee/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      userInfo.value = {}
      router.push('/emp-login')
    }
  } catch {
    ElMessage.info('已取消退出')
  }
}

// 通知相关
const fetchNotifications = async () => {
  try {
    const [unreadRes, listRes] = await Promise.all([
      axios.get('/api/v1/employee/notifications/unread-count'),
      axios.get('/api/v1/employee/notifications', { params: { currentPage: 1, pageSize: 5 } })
    ])
    if (unreadRes.data?.code === 200) unreadCount.value = unreadRes.data.data || 0
    if (listRes.data?.code === 200) notifList.value = listRes.data.data || []
  } catch (e) { /* ignore */ }
}

const markRead = async (item: any) => {
  if (!item.is_read) {
    await axios.put(`/api/v1/employee/notifications/${item.id}/read`)
    item.is_read = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

const markAllRead = async () => {
  await axios.put('/api/v1/employee/notifications/read-all')
  unreadCount.value = 0
  notifList.value.forEach((n: any) => n.is_read = 1)
}

const getEmpName = async () => {
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data && response.data.data && response.data.data.name) {
      empName.value = response.data.data.name
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
  }
}
</script>

<style scoped>
.emp-home {
  min-height: 100vh;
  background: #f5f5f7;
}

/* ── Apple-style header ── */
.app-header {
  background: #fff;
  border-bottom: 1px solid #e5e5e7;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 56px;
  padding: 0 24px;
}

.header-greeting {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-time {
  font-size: 13px;
  color: #86868b;
}

/* ── 通知铃铛 ── */
.notification-badge {
  margin: 0 4px;
}

.bell-btn {
  border: none;
  background: transparent;
  color: #86868b;
  padding: 4px;
}

.bell-btn:hover {
  color: #1d1d1f;
}

/* ── 通知下拉 ── */
.notification-dropdown {
  position: absolute;
  top: 56px;
  right: 80px;
  width: 360px;
  max-height: 420px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  z-index: 200;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px 10px;
  border-bottom: 1px solid #e5e5e7;
}

.notif-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.notif-list {
  flex: 1;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f7;
  transition: background 0.15s;
}

.notif-item:hover {
  background: #f5f5f7;
}

.notif-item.unread {
  background: #f0f7ff;
}

.notif-dot {
  width: 8px;
  height: 8px;
  min-width: 8px;
  background: #0071e3;
  border-radius: 50%;
  margin-top: 6px;
  margin-right: 10px;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.notif-body {
  font-size: 13px;
  color: #86868b;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-time {
  font-size: 11px;
  color: #aeaeb2;
  margin-top: 4px;
}

.notif-empty {
  padding: 40px 16px;
  text-align: center;
  color: #86868b;
  font-size: 14px;
}

.notif-footer {
  padding: 8px 16px;
  border-top: 1px solid #e5e5e7;
  text-align: center;
}

/* ── Main content ── */
.app-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  max-width: 860px;
  margin: 0 auto 40px;
}

.function-card {
  width: 100%;
  height: 130px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  cursor: pointer;
  user-select: none;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  background: #e8e8ed;
  color: #6e6e73;
}

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 4px;
}

.card-desc {
  font-size: 13px;
  color: #86868b;
  margin: 0;
}

/* Override Element Plus card style */
.apple-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04);
  transition: all 0.2s ease;
}

.apple-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1), 0 2px 4px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}
</style>
