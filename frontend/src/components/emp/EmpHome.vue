<template>
  <div class="emp-home">
    <div class="apple-page">
      <!-- Greeting + Time -->
      <div class="greeting-section">
        <h1 class="apple-title">{{ greeting }}, {{ userInfo.name || '员工' }}</h1>
        <p class="apple-subtitle">{{ currentTime }}</p>
      </div>

      <!-- Function Cards: 2x2 Grid -->
      <div class="card-grid">
        <div class="apple-card function-card" @click="goTo('/emp-home/sign-in')">
          <div class="card-icon clock-icon">
            <el-icon :size="28"><Clock /></el-icon>
          </div>
          <h3 class="card-name">员工签到</h3>
          <p class="card-desc">每日签到打卡</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/info')">
          <div class="card-icon user-icon">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <h3 class="card-name">个人信息</h3>
          <p class="card-desc">查看和编辑资料</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/sign-message')">
          <div class="card-icon doc-icon">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <h3 class="card-name">签到记录</h3>
          <p class="card-desc">查看签到历史</p>
        </div>
        <div class="apple-card function-card" @click="goTo('/emp-home/ai-chat')">
          <div class="card-icon service-icon">
            <el-icon :size="28"><Service /></el-icon>
          </div>
          <h3 class="card-name">AI 客服</h3>
          <p class="card-desc">智能在线客服</p>
        </div>
      </div>

      <!-- Child Routes -->
      <router-view />
    </div>

    <!-- Logout -->
    <div class="logout-area">
      <el-button @click="logout" class="apple-btn" size="small">退出登录</el-button>
    </div>
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
  background: var(--apple-bg);
  position: relative;
}

.greeting-section {
  margin-bottom: 32px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  max-width: 480px;
  margin-bottom: 40px;
}

.function-card {
  width: 100%;
  height: 140px;
  padding: 24px;
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
  color: #fff;
}

.clock-icon { background: var(--apple-blue); }
.user-icon { background: var(--apple-green); }
.doc-icon { background: var(--apple-orange); }
.service-icon { background: #5E5CE6; }

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text);
  margin: 0 0 4px;
}

.card-desc {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin: 0;
}

.logout-area {
  position: fixed;
  top: 24px;
  right: 32px;
  z-index: 10;
}
</style>
