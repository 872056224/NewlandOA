<template>
  <div class="dashboard apple-page">
    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="apple-card stat-card" v-loading="loading">
        <div class="stat-value">{{ stats.employeeCount }}</div>
        <div class="stat-label">员工总数</div>
      </div>
      <div class="apple-card stat-card" v-loading="loading">
        <div class="stat-value">{{ stats.departmentCount }}</div>
        <div class="stat-label">部门数量</div>
      </div>
      <div class="apple-card stat-card" v-loading="loading">
        <div class="stat-value">{{ stats.dutyCount }}</div>
        <div class="stat-label">职务数量</div>
      </div>
      <div class="apple-card stat-card" v-loading="loading">
        <div class="stat-value">{{ stats.todayAttendance }}</div>
        <div class="stat-label">今日签到</div>
      </div>
    </div>

    <!-- Quick Navigation -->
    <div class="apple-card section-card">
      <h3 class="section-title">快速导航</h3>
      <div class="quick-nav">
        <button class="nav-btn" @click="$router.push('/admin-home/emp-list')">
          <el-icon :size="18"><User /></el-icon>
          <span>员工管理</span>
        </button>
        <button class="nav-btn" @click="$router.push('/admin-home/dept-manage')">
          <el-icon :size="18"><OfficeBuilding /></el-icon>
          <span>部门管理</span>
        </button>
        <button class="nav-btn" @click="$router.push('/admin-home/duty-manage')">
          <el-icon :size="18"><Briefcase /></el-icon>
          <span>职务管理</span>
        </button>
        <button class="nav-btn" @click="$router.push('/admin-home/sign-list')">
          <el-icon :size="18"><Clock /></el-icon>
          <span>考勤管理</span>
        </button>
      </div>
    </div>

    <!-- System Info -->
    <div class="apple-card section-card">
      <h3 class="section-title">系统信息</h3>
      <div class="system-info">
        <div class="info-row">
          <span class="info-label">系统版本</span>
          <span class="info-value">OA办公系统 v2.0</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前时间</span>
          <span class="info-value">{{ currentTime }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">在线用户</span>
          <span class="info-value">{{ stats.onlineUsers }} 人</span>
        </div>
        <div class="info-row">
          <span class="info-label">系统状态</span>
          <span class="info-value"><el-tag type="success" size="small">运行正常</el-tag></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, OfficeBuilding, Clock, Briefcase } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const currentTime = ref('')
const loading = ref(false)
let timer: NodeJS.Timeout

const stats = reactive({
  employeeCount: 0,
  departmentCount: 0,
  todayAttendance: 0,
  dutyCount: 0,
  onlineUsers: 1
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN')
}

const loadStats = async () => {
  loading.value = true
  try {
    const [employeesRes, departmentsRes, dutiesRes, todaySignedRes] = await Promise.all([
      axios.get('/api/v1/admin/employees', { params: { currentPage: 1, pageSize: 1 } }),
      axios.get('/api/v1/admin/departments'),
      axios.get('/api/v1/admin/duties'),
      axios.get('/api/v1/admin/attendance/today/signed', { params: { currentPage: 1, pageSize: 1 } })
    ])

    if (employeesRes.data && employeesRes.data.total !== undefined) {
      stats.employeeCount = employeesRes.data.total
    }
    if (departmentsRes.data && departmentsRes.data.data) {
      stats.departmentCount = departmentsRes.data.data.length
    }
    if (dutiesRes.data && dutiesRes.data.data) {
      stats.dutyCount = dutiesRes.data.data.length
    }
    if (todaySignedRes.data && todaySignedRes.data.total !== undefined) {
      stats.todayAttendance = todaySignedRes.data.total
    }

    console.log('统计数据加载完成:', stats)
  } catch (error) {
    console.error('加载统计数据失败:', error)
    stats.employeeCount = 0
    stats.departmentCount = 0
    stats.todayAttendance = 0
    stats.dutyCount = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await loadStats()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.dashboard {
  padding-top: 32px;
  /* ECharts Apple palette (cascades to chart components) */
  --apple-chart-blue: #0071E3;
  --apple-chart-light-blue: #64B5F6;
  --apple-chart-cyan: #00C7BE;
  --apple-chart-green: #34C759;
  --apple-chart-orange: #FF9500;
  --apple-chart-red: #FF3B30;
  --apple-chart-purple: #AF52DE;
  --apple-chart-pink: #FF2D55;
  --apple-chart-gray: #8E8E93;
  --apple-chart-light-gray: #C7C7CC;
}

/* Stats Cards */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  padding: 28px 24px;
  text-align: center;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--apple-text);
  line-height: 1.2;
  letter-spacing: -0.5px;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
  font-weight: 400;
}

/* Section Cards */
.section-card {
  margin-bottom: 24px;
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text);
  margin: 0 0 20px;
}

/* Quick Navigation */
.quick-nav {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 12px;
  border-radius: var(--apple-radius);
  border: 1px solid var(--apple-border);
  background: var(--apple-white);
  color: var(--apple-text);
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  width: 100%;
}

.nav-btn:hover {
  border-color: var(--apple-blue);
  color: var(--apple-blue);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}

/* System Info */
.system-info {
  display: grid;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.info-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.info-value {
  font-size: 14px;
  color: var(--apple-text);
  font-weight: 500;
}
</style>
