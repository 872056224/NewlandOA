<template>
  <div class="update-pwd">
    <div class="pwd-container">
      <p class="pwd-title">修改密码</p>
      
      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="rules"
        class="demo-ruleForm"
        label-width="80px"
        status-icon
        v-loading="loading"
      >
        <el-form-item label="员工工号" prop="number">
          <el-input :disabled="true" v-model="editFormData.number" />
        </el-form-item>
        
        <el-form-item label="旧密码" prop="old_pwd">
          <el-input
            autocomplete="off"
            placeholder="请输入旧密码"
            type="password"
            v-model="editFormData.old_pwd"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="pass">
          <el-input
            autocomplete="off"
            placeholder="请输入新的密码"
            type="password"
            v-model="editFormData.pass"
          />
        </el-form-item>
        
        <el-form-item label="确认密码" prop="checkPass">
          <el-input
            autocomplete="off"
            placeholder="请再次输入新的密码以核对"
            type="password"
            v-model="editFormData.checkPass"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button @click="updatePwd" type="primary" :loading="updating">
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const editFormRef = ref<FormInstance>()
const loading = ref(false)
const updating = ref(false)

const editFormData = reactive({
  number: '',
  old_pwd: '',
  pass: '',
  checkPass: '',
  pwd: '' // 用于存储原密码进行验证
})

// 自定义验证规则
const validatePass = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入新的密码'))
  } else {
    if (editFormData.checkPass !== '') {
      editFormRef.value?.validateField('checkPass')
    }
    callback()
  }
}

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新的密码'))
  } else if (value !== editFormData.pass) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  pass: [
    { validator: validatePass, trigger: 'blur' }
  ],
  checkPass: [
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const updatePwd = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        // 使用URL参数格式发送请求，因为后端使用了@RequestParam
        const params = new URLSearchParams()
        params.append('number', editFormData.number)
        params.append('pwd', editFormData.pass)
        params.append('oldPassword', editFormData.old_pwd)
        
        const response = await axios.put('/api/v1/employee/password?' + params.toString(), {
          number: parseInt(editFormData.number),
          pwd: editFormData.pass
        })
        
        if (response.data === 'true' || response.data === true) {
          ElMessage.success('密码修改成功')
          router.push('/emp-login')
        } else {
          ElMessage.error('密码修改失败，请检查旧密码是否正确')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改密码失败，请检查网络连接')
      } finally {
        updating.value = false
      }
    }
  })
}

const getEmpInfo = async () => {
  try {
    const response = await axios.get('/api/v1/employee/profile')
    if (response.data && response.data.data) {
      editFormData.number = response.data.data.number
      editFormData.pwd = response.data.data.pwd
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
    ElMessage.error('获取员工信息失败')
  }
}

onMounted(() => {
  getEmpInfo()
})
</script>

<style scoped>
.update-pwd {
  padding: 20px;
  min-height: 100%;
  background-color: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.pwd-container {
  width: 500px;
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.pwd-title {
  color: #606266;
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}

.el-form {
  margin-top: 20px;
}
</style> 