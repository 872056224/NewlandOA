<template>
  <div class="admin-home">
    <el-container>
      <!-- Apple-style header -->
      <el-header>
        <div class="header-content">
          <h2>智慧OA · 管理端</h2>
          <div class="header-right">
            <!-- 通知铃铛 -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button @click="showNotifications = !showNotifications" text size="large" class="bell-btn">
                <el-icon :size="20"><Bell /></el-icon>
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
                <el-button text size="small" @click="goTo('/admin-home/notifications')">查看全部</el-button>
              </div>
            </div>
            <span class="user-info">欢迎，{{ userInfo.name || '管理员' }}</span>
            <el-button @click="logout" text size="small">退出登录</el-button>
          </div>
        </div>
      </el-header>

      <el-container>
        <!-- Sidebar -->
        <el-aside width="200px">
          <el-menu
            :default-active="$route.path"
            router
          >
            <el-menu-item index="/admin-home/dashboard">
              <el-icon><Odometer /></el-icon>
              <span>数据面板</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/emp-list">
              <el-icon><User /></el-icon>
              <span>员工管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/dept-manage">
              <el-icon><OfficeBuilding /></el-icon>
              <span>部门管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/duty-manage">
              <el-icon><Briefcase /></el-icon>
              <span>职务管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/sign-list">
              <el-icon><Clock /></el-icon>
              <span>考勤管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/monthly-statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>考勤统计</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/leave-approval">
              <el-icon><Edit /></el-icon>
              <span>请假审批</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/retroactive-approval">
              <el-icon><Clock /></el-icon>
              <span>补签审批</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/kb-manage">
              <el-icon><ChatDotRound /></el-icon>
              <span>知识库管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/notifications">
              <el-icon><Bell /></el-icon>
              <span>通知列表</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/holiday-manage">
              <el-icon><Calendar /></el-icon>
              <span>节假日管理</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/attendance-rule">
              <el-icon><Setting /></el-icon>
              <span>考勤规则</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区 -->
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer, User, OfficeBuilding, Briefcase, Clock,
  PieChart, DataAnalysis, ChatDotRound, Edit, Bell, Calendar, Setting
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userInfo = ref<any>({})
const unreadCount = ref(0)
const showNotifications = ref(false)
const notifList = ref<any[]>([])
let notifTimer: NodeJS.Timeout

const goTo = (path: string) => {
  showNotifications.value = false
  router.push(path)
}

onMounted(async () => {
  try {
    const response = await axios.get('/api/v1/admin/auth/profile')
    if (response.data && response.data.data) {
      userInfo.value = response.data.data
    } else {
      userInfo.value = { name: '管理员' }
    }
  } catch (error) {
    console.error('获取管理员信息失败:', error)
    userInfo.value = { name: '管理员' }
  }
  fetchNotifications()
  notifTimer = setInterval(fetchNotifications, 10000)
})

onUnmounted(() => {
  if (notifTimer) clearInterval(notifTimer)
})

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    try {
      await axios.post('/api/v1/admin/auth/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      userInfo.value = {}
      router.push('/admin-login')
    }
  } catch {
    ElMessage.info('已取消退出')
  }
}

// 通知相关
const fetchNotifications = async () => {
  try {
    const [unreadRes, listRes] = await Promise.all([
      axios.get('/api/v1/admin/notifications/unread-count'),
      axios.get('/api/v1/admin/notifications', { params: { currentPage: 1, pageSize: 5 } })
    ])
    if (unreadRes.data?.code === 200) unreadCount.value = unreadRes.data.data || 0
    if (listRes.data?.code === 200) notifList.value = listRes.data.data || []
  } catch (e) { /* ignore */ }
}

const markRead = async (item: any) => {
  if (!item.is_read) {
    await axios.put(`/api/v1/admin/notifications/${item.id}/read`)
    item.is_read = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

const markAllRead = async () => {
  await axios.put('/api/v1/admin/notifications/read-all')
  unreadCount.value = 0
  notifList.value.forEach((n: any) => n.is_read = 1)
}
</script>

<style scoped>
.admin-home {
  height: 100vh;
  background-color: #f5f5f7;
  overflow: hidden;
}

.admin-home .el-container {
  height: 100vh;
}

.admin-home .el-container:nth-child(2) {
  height: calc(100vh - 56px);
}

/* ── Header ── */
.el-header {
  background: #fff;
  border-bottom: 1px solid #e5e5e7;
  line-height: 56px;
  height: 56px !important;
  padding: 0 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #86868b;
}

.header-right .el-button {
  color: #86868b;
}

.header-right .el-button:hover {
  color: #1d1d1f;
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
  right: 180px;
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

/* ── Sidebar ── */
.el-aside {
  background: #fff;
  width: 200px !important;
  height: 100%;
  border-right: 1px solid #e5e5e7;
}

.el-menu {
  border-right: none;
  height: 100%;
  background: transparent;
}

.el-menu-item {
  color: #86868b !important;
  background: transparent !important;
  font-size: 14px;
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  border-radius: 8px;
}

.el-menu-item:hover {
  color: #1d1d1f !important;
  background: #f5f5f7 !important;
}

.el-menu-item.is-active {
  color: #0071e3 !important;
  background: transparent !important;
}

.el-menu-item.is-active:hover {
  background: #f5f5f7 !important;
}

.el-menu-item .el-icon {
  margin-right: 8px;
  font-size: 18px;
}

/* ── Main content ── */
.el-main {
  background-color: #f5f5f7;
  padding: 24px;
  height: 100%;
  overflow-y: auto;
}
</style>
