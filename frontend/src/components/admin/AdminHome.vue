<template>
  <div class="admin-home">
    <el-container>
      <!-- Apple-style header: white, thin bottom border -->
      <el-header>
        <div class="header-content">
          <h2>管理员系统</h2>
          <div class="user-info">
            <span>欢迎，{{ userInfo.name || '管理员' }}</span>
            <el-button @click="logout" text size="small">退出登录</el-button>
          </div>
        </div>
      </el-header>

      <el-container>
        <!-- Sidebar: gray text, blue active text, no background blocks -->
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
            <el-menu-item index="/admin-home/sign-statistics">
              <el-icon><PieChart /></el-icon>
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
  ChatDotRound,
  Edit
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

    try {
      const response = await axios.post('/api/v1/admin/auth/logout')
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

/* ── Apple-style header ── */
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
}

.header-content h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #86868b;
}

.user-info .el-button {
  color: #86868b;
}

.user-info .el-button:hover {
  color: #1d1d1f;
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

/* Sidebar menu items: gray text, blue when active, no background blocks */
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

/* Icon spacing inside menu items */
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
