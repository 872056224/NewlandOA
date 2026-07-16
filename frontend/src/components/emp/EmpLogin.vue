<template>
  <div class="login-container">
    <div class="login-box">
      <h3 class="login-title">员工登录</h3>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="账号" prop="number">
          <el-input
            v-model="loginForm.number"
            type="text"
            placeholder="请输入员工账号"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码（初始密码为：123）"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleLogin"
            :loading="loading"
            style="width: 100%"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-links">
        <el-link @click="goToAdminLogin" type="primary">管理员登录</el-link>
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
.login-container {
  height: 100vh;
  background: url('/src/assets/empLogin.jpg') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  border: 1px solid #7a7b7e;
  background: rgb(195, 204, 215);
  width: 600px;
  height: 310px;
  padding: 35px 35px 15px 35px;
  border-radius: 5px;
  box-shadow: 0 0 25px #909399;
}

.login-title {
  text-align: center;
  margin: 0 auto 40px auto;
  color: #303133;
}

.login-links {
  text-align: center;
  margin-top: 20px;
}
</style> 