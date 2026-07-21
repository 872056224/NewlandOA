<template>
  <div class="apple-login-page">
    <div class="apple-card login-card">
      <div class="login-icon">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </div>
      <h1 class="apple-title">欢迎回来</h1>
      <p class="apple-subtitle">登录你的账号</p>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
      >
        <el-form-item prop="number">
          <el-input
            v-model="loginForm.number"
            type="text"
            placeholder="请输入员工账号"
            class="apple-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码（初始密码为：123）"
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
        <span class="link-text" @click="goToAdminLogin">切换至管理员登录</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  number: '',
  password: ''
})

const rules = {
  number: [
    { required: true, message: '请输入您的账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入您的密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/v1/employee/login', {
          number: parseInt(loginForm.number),
          pwd: loginForm.password
        }, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (response.data === 'true' || response.data === true) {
          ElMessage.success('登录成功')
          router.push('/emp-home/info')
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

const goToAdminLogin = () => {
  router.push('/admin-login')
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
</style> 