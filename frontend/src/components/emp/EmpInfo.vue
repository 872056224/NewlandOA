<template>
  <div class="emp-info">
    <div class="info-container">
      <p class="info-title">个人信息</p>
      
      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="rules"
        :label-position="'right'"
        label-width="80px"
        v-loading="loading"
      >
        <el-form-item label="工号" prop="number">
          <el-input v-model="editFormData.number" :disabled="true" />
        </el-form-item>
        
        <el-form-item label="名字" prop="name">
          <el-input v-model="editFormData.name" />
        </el-form-item>
        
        <el-form-item label="出生" prop="birthday">
          <el-input v-model="editFormData.birthday" type="date" />
        </el-form-item>
        
        <el-form-item label="地址" prop="address">
          <el-input v-model="editFormData.address" />
        </el-form-item>
        
        <el-form-item label="部门" prop="dept_name">
          <el-input v-model="editFormData.dept_name" :disabled="true" />
        </el-form-item>
        
        <el-form-item label="职务" prop="duty_name">
          <el-input v-model="editFormData.duty_name" :disabled="true" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="warning" @click="updateEmp" :loading="saving">
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
import axios from 'axios'

const editFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

const editFormData = reactive({
  number: '',
  name: '',
  birthday: '',
  address: '',
  dept_name: '',
  duty_name: ''
})

const rules = {
  number: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '长度在 2 到 10 个字符', trigger: 'blur' }
  ],
  birthday: [
    { required: true, message: '请选择日期', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  dept_name: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  duty_name: [
    { required: true, message: '请选择职务', trigger: 'change' }
  ]
}

const updateEmp = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const response = await axios.put('/api/v1/employee/profile', {
          number: parseInt(editFormData.number),
          name: editFormData.name,
          birthday: editFormData.birthday,
          address: editFormData.address
        })
        
        if (response.data && response.data.data) {
          ElMessage.success('信息更新成功')
          Object.assign(editFormData, response.data.data)
        } else {
          ElMessage.error('更新失败')
        }
      } catch (error) {
        console.error('更新错误:', error)
        ElMessage.error('更新失败，请检查网络连接')
      } finally {
        saving.value = false
      }
    }
  })
}

const getEmpInfo = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/profile')
    
    if (response.data && response.data.data) {
      Object.assign(editFormData, response.data.data)
    } else {
      ElMessage.error('获取信息失败')
    }
  } catch (error) {
    console.error('获取信息错误:', error)
    ElMessage.error('获取信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getEmpInfo()
})
</script>

<style scoped>
.emp-info {
  padding: 20px;
  min-height: 100%;
  background-color: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.info-container {
  width: 850px;
  background-color: rgb(255, 255, 255);
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.info-title {
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