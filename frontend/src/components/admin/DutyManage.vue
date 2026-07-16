<template>
  <div class="duty-manage">
    <div class="page-header">
      <b class="page-title">职务信息管理</b>
      <div class="page-actions">
        <el-button @click="showAddDuty" type="primary">添加职务</el-button>
      </div>
    </div>

    <!-- 数据展现表格 -->
    <el-table :data="tableData" v-loading="loading">
      <el-table-column 
        label="职务序号" 
        prop="duty_id" 
        min-width="130" 
        align="center" 
      />
      <el-table-column 
        label="职务名" 
        prop="duty_name" 
        min-width="150" 
        align="center" 
      />
      <el-table-column 
        label="职务人数" 
        prop="duty_num" 
        min-width="130" 
        align="center" 
      />
      <el-table-column label="操作" min-width="160" align="center">
        <template #default="{ row }">
          <el-button @click="showEditDuty(row)" type="warning">修改职务</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[8, 10, 12]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px;"
    />

    <!-- 添加职务对话框 -->
    <el-dialog 
      v-model="dialogVisible.add" 
      @close="resetForm('addForm')" 
      title="添加职务信息"
      width="400px"
    >
      <el-form 
        :model="formData" 
        :rules="rules" 
        ref="addFormRef" 
        label-width="100px"
      >
        <el-form-item label="职务名" prop="duty_name">
          <el-input v-model="formData.duty_name" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="addDuty" type="primary" :loading="adding">添加</el-button>
        <el-button @click="resetForm('addForm')">重置</el-button>
      </template>
    </el-dialog>

    <!-- 编辑职务对话框 -->
    <el-dialog 
      v-model="dialogVisible.edit" 
      @close="resetForm('editForm')" 
      title="编辑职务信息"
      width="400px"
    >
      <el-form 
        :model="editFormData" 
        :rules="rules" 
        ref="editFormRef" 
        label-width="100px"
      >
        <el-form-item label="职务序号" prop="duty_id">
          <el-input :disabled="true" v-model="editFormData.duty_id" />
        </el-form-item>
        <el-form-item label="职务名" prop="duty_name">
          <el-input v-model="editFormData.duty_name" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button type="warning" @click="updateDuty" :loading="updating">修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const adding = ref(false)
const updating = ref(false)
const tableData = ref<any[]>([])

const addFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const dialogVisible = reactive({
  add: false,
  edit: false
})

const formData = reactive({
  duty_name: ''
})

const editFormData = reactive({
  duty_id: '',
  duty_name: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const rules = {
  duty_id: [
    { required: true, message: '请输入职务序号', trigger: 'blur' }
  ],
  duty_name: [
    { required: true, message: '请输入职务名', trigger: 'blur' }
  ]
}

const selectAllDutyAndNum = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/duties')
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
    } else {
      ElMessage.error('获取职务列表失败')
    }
  } catch (error) {
    console.error('获取职务列表失败:', error)
    ElMessage.error('获取职务列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  selectAllDutyAndNum()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  selectAllDutyAndNum()
}

const showAddDuty = () => {
  dialogVisible.add = true
}

const addDuty = async () => {
  try {
    const dutyData = {
      duty_name: formData.duty_name
    }
    const response = await axios.post('/api/v1/admin/duties', dutyData, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.data === 'true' || response.data === true || (response.data && response.data.data)) {
      ElMessage.success('添加职务成功')
      dialogVisible.add = false
      resetForm('addForm')
      selectAllDutyAndNum()
    } else {
      ElMessage.error('添加职务失败')
    }
  } catch (error) {
    console.error('添加职务失败:', error)
    ElMessage.error('添加职务失败')
  }
}

const showEditDuty = (row: any) => {
  dialogVisible.edit = true
  Object.assign(editFormData, {
    duty_id: row.duty_id,
    duty_name: row.duty_name
  })
}

const updateDuty = async () => {
  try {
    const dutyData = {
      duty_name: editFormData.duty_name
    }
    const response = await axios.put(`/api/v1/admin/duties/${editFormData.duty_id}`, dutyData, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.data === 'true' || response.data === true || (response.data && response.data.data)) {
      ElMessage.success('更新职务成功')
      dialogVisible.edit = false
      resetForm('editForm')
      selectAllDutyAndNum()
    } else {
      ElMessage.error('更新职务失败')
    }
  } catch (error) {
    console.error('更新职务失败:', error)
    ElMessage.error('更新职务失败')
  }
}

const resetForm = (formName: string) => {
  if (formName === 'addForm') {
    formData.duty_name = ''
    addFormRef.value?.resetFields()
  } else if (formName === 'editForm') {
    Object.assign(editFormData, {
      duty_id: '',
      duty_name: ''
    })
    editFormRef.value?.resetFields()
  }
}

onMounted(() => {
  selectAllDutyAndNum()
})
</script>

<style scoped>
.duty-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  color: red;
}
</style> 