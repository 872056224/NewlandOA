<template>
  <div class="emp-home">
    <!-- Apple-style header: white, thin bottom border -->
    <header class="app-header">
      <div class="header-inner">
        <h1 class="header-greeting">{{ greeting }}, {{ userInfo.name || '员工' }}</h1>
        <div class="header-actions">
          <span class="header-time">{{ currentTime }}</span>
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
import { User, Clock, Document, Service } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userInfo = ref<any>({})
const empName = ref<string>('')
const currentTime = ref('')
let timer: NodeJS.Timeout

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
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
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
