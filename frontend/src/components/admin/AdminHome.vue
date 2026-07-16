<template>
  <div class="admin-home">
    <el-container>
      <!-- 头部 -->
      <el-header>
        <div class="header-content">
          <h2>管理员系统</h2>
          <div class="user-info">
            <span>欢迎，{{ userInfo.name || '管理员' }}</span>
            <el-button @click="logout" type="primary" plain size="small">退出登录</el-button>
          </div>
        </div>
      </el-header>
      
      <el-container>
        <!-- 侧边栏 -->
        <el-aside width="200px">
          <el-menu
            :default-active="$route.path"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
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
            <el-menu-item index="/admin-home/sign-statistics">
              <el-icon><PieChart /></el-icon>
              <span>考勤统计</span>
            </el-menu-item>
            <el-menu-item index="/admin-home/kb-manage">
              <el-icon><ChatDotRound /></el-icon>
              <span>知识库管理</span>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Odometer, 
  User, 
  OfficeBuilding, 
  Briefcase, 
  Clock, 
  PieChart,
  ChatDotRound
} from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userInfo = ref<any>({})

onMounted(async () => {
  try {
    const response = await axios.get('/api/v1/admin/auth/profile')
    if (response.data && response.data.data) {
      userInfo.value = response.data.data
    } else {
      // 如果获取不到管理员信息，设置默认值
      userInfo.value = { name: '管理员' }
    }
  } catch (error) {
    console.error('获取管理员信息失败:', error)
    // 如果请求失败，设置默认值
    userInfo.value = { name: '管理员' }
  }
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
    
    // 用户确认后执行logout
    try {
      const response = await axios.post('/api/v1/admin/auth/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      // 无论API调用是否成功，都清除本地状态并跳转
      userInfo.value = {}
      router.push('/admin-login')
    }
  } catch {
    // 用户取消了退出操作
    ElMessage.info('已取消退出')
  }
}
</script>

<style scoped>
.admin-home {
  height: 100vh;
  background-color: #f5f5f5;
  overflow: hidden;
}

.admin-home .el-container {
  height: 100vh;
}

.admin-home .el-container:nth-child(2) {
  height: calc(100vh - 60px);
}

.el-header {
  background-color: #022141;
  color: white;
  line-height: 60px;
  height: 60px !important;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info .el-button {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  color: white;
}

.user-info .el-button:hover {
  background-color: rgba(255, 255, 255, 0.3);
  border-color: white;
  color: white;
}

.el-aside {
  background-color: #304156;
  width: 200px !important;
  height: 100%;
}

.el-menu {
  border-right: none;
  height: 100%;
}

.el-main {
  background-color: #f5f5f5;
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}
</style> 