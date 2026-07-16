<template>
  <div class="emp-home">
    <el-container>
      <!-- 头部 -->
      <el-header>
        <div class="header-content">
          <h2>员工办公系统</h2>
          <div class="user-info">
            <span>欢迎，{{ userInfo.name || '员工' }}</span>
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
            <el-menu-item index="/emp-home/info">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="/emp-home/sign-in">
              <el-icon><Clock /></el-icon>
              <span>员工签到</span>
            </el-menu-item>
            <el-menu-item index="/emp-home/sign-message">
              <el-icon><Document /></el-icon>
              <span>签到记录</span>
            </el-menu-item>
            <el-menu-item index="/emp-home/update-pwd">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item index="/emp-home/ai-chat">
              <el-icon><Service /></el-icon>
              <span>AI 客服</span>
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
import { User, Clock, Document, Lock, Service } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userInfo = ref<any>({})
const empName = ref<string>('')

onMounted(async () => {
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data && response.data.data) {
      userInfo.value = response.data.data
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
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
      const response = await axios.post('/api/v1/employee/logout')
      ElMessage.success('退出登录成功')
    } catch (error) {
      console.error('退出登录失败:', error)
      ElMessage.warning('退出登录失败，但将跳转到登录页')
    } finally {
      // 无论API调用是否成功，都清除本地状态并跳转
      userInfo.value = {}
      router.push('/emp-login')
    }
  } catch {
    // 用户取消了退出操作
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
  height: 100vh;
  background-color: #1f1f20;
  overflow: hidden;
}

.emp-home .el-container {
  height: 100vh;
}

.emp-home .el-container:nth-child(2) {
  height: calc(100vh - 60px);
}

.el-header {
  background-color: #033363;
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
  background-color: #052042;
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