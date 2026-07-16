<template>
  <div class="login-container">
    <div class="login-box">
      <h3 class="login-title">管理员登录</h3>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入管理员账号"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item>
          <div class="button-group">
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-btn"
            >
              登录
            </el-button>
            <el-button
              type="primary"
              @click="showRegisterDialog"
              class="register-btn"
            >
              注册
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <div class="login-links">
        <el-link @click="goToEmpLogin" type="primary">员工登录</el-link>
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
.login-container {
  height: 100vh;
  background: url('/src/assets/background.jpg') no-repeat center center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-box {
  border: 1px solid #DCDFE6;
  background: rgb(222, 227, 227);
  width: 650px;
  height: 360px;
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

.button-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

.login-btn, .register-btn {
  flex: 1;
}
</style> 