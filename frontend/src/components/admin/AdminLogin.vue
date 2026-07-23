<template>
  <div class="apple-login-page">
    <div class="apple-card login-card">
      <div class="login-icon">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
      </div>
      <h1 class="apple-title">管理员登录</h1>
      <p class="apple-subtitle">登录管理系统</p>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入管理员账号"
            class="apple-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            class="apple-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            class="apple-btn apple-btn-primary"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-links">
        <span class="link-text" @click="showRegisterDialog">注册账号</span>
        <span class="link-separator">|</span>
        <span class="link-text" @click="goToEmpLogin">切换至员工登录</span>
      </div>
    </div>

    <!-- 注册对话框 -->
    <el-dialog
      v-model="registerDialogVisible"
      title="注册页面"
      width="400px"
      @close="resetRegisterForm"
    >
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="姓名" prop="username">
          <el-input
            v-model="registerForm.username"
            type="text"
            placeholder="请输入姓名"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="registerLoading">
            注册
          </el-button>
          <el-button @click="resetRegisterForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerLoading = ref(false)
const registerDialogVisible = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/v1/admin/auth/login', {
          name: loginForm.username,
          pwd: loginForm.password
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (response.data === 'true' || response.data === true) {
          ElMessage.success('登录成功')
          router.push('/admin-home/dashboard')
        } else if (response.data === 'no_emp_binding') {
          ElMessage.error('该管理员账号未绑定员工，请联系管理员配置')
        } else if (response.data === 'emp_not_found') {
          ElMessage.error('关联的员工信息不存在')
        } else if (response.data === 'no_permission') {
          ElMessage.error('该账号无管理端权限（仅部长/副部长/董事长可登录）')
        } else {
          ElMessage.error('登录失败，请检查用户名和密码')
        }
      } catch (error) {
        console.error('登录错误:', error)
        ElMessage.error('登录失败，请检查网络连接')
      } finally {
        loading.value = false
      }
    }
  })
}

const showRegisterDialog = () => {
  registerDialogVisible.value = true
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      try {
        const response = await axios.post('/api/v1/admin/auth/register', {
          name: registerForm.username,
          pwd: registerForm.password
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (response.data === 'true' || response.data === true) {
          ElMessage.success('注册成功')
          registerDialogVisible.value = false
          resetRegisterForm()
        } else {
          ElMessage.error('注册失败，用户名重复')
        }
      } catch (error) {
        console.error('注册错误:', error)
        ElMessage.error('注册失败，请检查网络连接')
      } finally {
        registerLoading.value = false
      }
    }
  })
}

const resetRegisterForm = () => {
  registerForm.username = ''
  registerForm.password = ''
  if (registerFormRef.value) {
    registerFormRef.value.resetFields()
  }
}

const goToEmpLogin = () => {
  router.push('/emp-login')
}
</script>

<style scoped>
.apple-login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--apple-bg);
}

.login-card {
  width: 400px;
  padding: 40px;
  text-align: center;
}

.login-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  background: var(--apple-bg-secondary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-secondary);
}

.login-links {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.link-text {
  color: var(--apple-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.link-text:hover {
  color: var(--apple-blue);
}

.link-separator {
  color: var(--apple-text-tertiary);
  font-size: 14px;
}
</style> 