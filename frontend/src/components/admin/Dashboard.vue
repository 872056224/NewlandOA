<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="header-left">
        <!-- <el-image class="header-img" src="/src/assets/emphand.jpg"/> -->
        <span class="header-text">后台管理</span>
      </div>
    </div>
    
    <div class="dashboard-content">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card" v-loading="loading">
            <div class="stat-item">
              <div class="stat-icon employee">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.employeeCount }}</div>
                <div class="stat-label">员工总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" v-loading="loading">
            <div class="stat-item">
              <div class="stat-icon department">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.departmentCount }}</div>
                <div class="stat-label">部门数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" v-loading="loading">
            <div class="stat-item">
              <div class="stat-icon attendance">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.todayAttendance }}</div>
                <div class="stat-label">今日签到</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card class="stat-card" v-loading="loading">
            <div class="stat-item">
              <div class="stat-icon duty">
                <el-icon><Briefcase /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stats.dutyCount }}</div>
                <div class="stat-label">职务数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>快速导航</span>
            </template>
            <div class="quick-nav">
              <el-button 
                type="primary" 
                @click="$router.push('/admin-home/emp-list')"
                :icon="User"
              >
                员工管理
              </el-button>
              <el-button 
                type="success" 
                @click="$router.push('/admin-home/dept-manage')"
                :icon="OfficeBuilding"
              >
                部门管理
              </el-button>
              <el-button 
                type="warning" 
                @click="$router.push('/admin-home/duty-manage')"
                :icon="Briefcase"
              >
                职务管理
              </el-button>
              <el-button 
                type="info" 
                @click="$router.push('/admin-home/sign-list')"
                :icon="Clock"
              >
                考勤管理
              </el-button>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>系统信息</span>
            </template>
            <div class="system-info">
              <p><strong>系统版本：</strong>OA办公系统 v2.0</p>
              <p><strong>当前时间：</strong>{{ currentTime }}</p>
              <p><strong>在线用户：</strong>{{ stats.onlineUsers }} 人</p>
              <p><strong>系统状态：</strong><el-tag type="success">运行正常</el-tag></p>
            </div>
          </el-card>
        </el-col>
      </el-row>
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
    // 并行获取各种统计数据
    const [employeesRes, departmentsRes, dutiesRes, todaySignedRes] = await Promise.all([
      axios.get('/api/v1/admin/employees', { params: { currentPage: 1, pageSize: 1 } }),
      axios.get('/api/v1/admin/departments'),
      axios.get('/api/v1/admin/duties'),
      axios.get('/api/v1/admin/attendance/today/signed', { params: { currentPage: 1, pageSize: 1 } })
    ])

    // 员工总数
    if (employeesRes.data && employeesRes.data.total !== undefined) {
      stats.employeeCount = employeesRes.data.total
    }

    // 部门数量
    if (departmentsRes.data && departmentsRes.data.data) {
      stats.departmentCount = departmentsRes.data.data.length
    }

    // 职务数量
    if (dutiesRes.data && dutiesRes.data.data) {
      stats.dutyCount = dutiesRes.data.data.length
    }

    // 今日签到人数
    if (todaySignedRes.data && todaySignedRes.data.total !== undefined) {
      stats.todayAttendance = todaySignedRes.data.total
    }

    console.log('统计数据加载完成:', stats)
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 如果获取失败，设置默认值
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
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-img {
  width: 107px;
  height: 39px;
  margin-right: 10px;
}

.header-text {
  font-size: 25px;
  font-weight: bold;
}

.stat-card {
  height: 120px;
}

.stat-item {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
}

.stat-icon.employee {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.department {
  background: linear-gradient(135deg, #161a51 0%, #3d246c 100%);
}

.stat-icon.attendance {
  background: linear-gradient(135deg, #363966 0%, #1a2b6a 100%);
}

.stat-icon.duty {
  background: linear-gradient(135deg, #1e182e 0%, #160e3a 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.quick-nav {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 15px;
  padding: 15px;
  align-items: stretch;
  justify-items: stretch;
  box-sizing: border-box;
}

.quick-nav .el-button {
  height: 40px;
  width: 100%;
  margin: 0;
  box-sizing: border-box;
}

.system-info p {
  margin: 10px 0;
  color: #606266;
}
</style> 