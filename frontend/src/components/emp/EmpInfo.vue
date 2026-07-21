<template>
  <div class="apple-page emp-info">
    <div class="apple-card info-card">
      <h2 class="apple-title">个人信息</h2>

      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="rules"
        label-position="top"
        v-loading="loading"
      >
        <el-form-item label="工号" prop="number">
          <el-input v-model="editFormData.number" :disabled="true" class="apple-input" />
        </el-form-item>

        <el-form-item label="名字" prop="name">
          <el-input v-model="editFormData.name" class="apple-input" />
        </el-form-item>

        <el-form-item label="出生" prop="birthday">
          <el-input v-model="editFormData.birthday" type="date" class="apple-input" />
        </el-form-item>

        <el-form-item label="地址" prop="address">
          <el-input v-model="editFormData.address" class="apple-input" />
        </el-form-item>

        <el-form-item label="部门" prop="dept_name">
          <el-input v-model="editFormData.dept_name" :disabled="true" class="apple-input" />
        </el-form-item>

        <el-form-item label="职务" prop="duty_name">
          <el-input v-model="editFormData.duty_name" :disabled="true" class="apple-input" />
        </el-form-item>

        <el-form-item>
          <el-button class="apple-btn apple-btn-primary" @click="updateEmp" :loading="saving">
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
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.info-card {
  width: 100%;
  max-width: 640px;
  padding: 40px;
}

.info-card :deep(.apple-title) {
  margin-bottom: 28px;
}

.info-card :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  padding-bottom: 4px;
}

.info-card :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: var(--apple-bg);
}
</style>