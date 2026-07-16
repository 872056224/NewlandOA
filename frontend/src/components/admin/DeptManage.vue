<template>
  <div class="dept-manage">
    <div class="page-header">
      <b class="page-title">部门信息管理</b>
      <div class="page-actions">
        <el-button @click="showAddDept" type="primary">添加部门</el-button>
      </div>
    </div>

    <!-- 数据展现表格 -->
    <el-table :data="tableData" v-loading="loading">
      <el-table-column 
        label="部门序号" 
        prop="dept_id" 
        min-width="130" 
        align="center" 
      />
      <el-table-column 
        label="部门名" 
        prop="dept_name" 
        min-width="150" 
        align="center" 
      />
      <el-table-column 
        label="部门人数" 
        prop="dept_num" 
        min-width="130" 
        align="center" 
      />
      <el-table-column label="操作" min-width="160" align="center">
        <template #default="{ row }">
          <el-button @click="showEditDept(row)" type="warning">修改部门</el-button>
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

    <!-- 添加部门对话框 -->
    <el-dialog 
      v-model="dialogVisible.add" 
      @close="resetForm('addForm')" 
      title="添加部门信息"
      width="400px"
    >
      <el-form 
        :model="formData" 
        :rules="rules" 
        ref="addFormRef" 
        label-width="100px"
      >
        <el-form-item label="部门名" prop="dept_name">
          <el-input v-model="formData.dept_name" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="addDept" type="primary" :loading="adding">添加</el-button>
        <el-button @click="resetForm('addForm')">重置</el-button>
      </template>
    </el-dialog>

    <!-- 编辑部门对话框 -->
    <el-dialog 
      v-model="dialogVisible.edit" 
      @close="resetForm('editForm')" 
      title="编辑部门信息"
      width="400px"
    >
      <el-form 
        :model="editFormData" 
        :rules="rules" 
        ref="editFormRef" 
        label-width="100px"
      >
        <el-form-item label="部门序号" prop="dept_id">
          <el-input :disabled="true" v-model="editFormData.dept_id" />
        </el-form-item>
        <el-form-item label="部门名" prop="dept_name">
          <el-input v-model="editFormData.dept_name" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button type="warning" @click="updateDept" :loading="updating">修改</el-button>
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
  dept_name: ''
})

const editFormData = reactive({
  dept_id: '',
  dept_name: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const rules = {
  dept_id: [
    { required: true, message: '请输入部门序号', trigger: 'blur' }
  ],
  dept_name: [
    { required: true, message: '请输入部门名', trigger: 'blur' }
  ]
}

const selectAllDeptAndNum = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/departments', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
    } else {
      ElMessage.error('获取部门列表失败')
    }
  } catch (error) {
    console.error('获取部门列表失败:', error)
    ElMessage.error('获取部门列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  selectAllDeptAndNum()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  selectAllDeptAndNum()
}

const showAddDept = () => {
  dialogVisible.add = true
}

const addDept = async () => {
  if (!addFormRef.value) return
  
  try {
    const valid = await addFormRef.value.validate()
    if (!valid) return
    
    adding.value = true
    const deptData = {
      dept_name: formData.dept_name
    }
    const response = await axios.post('/api/v1/admin/departments', deptData, {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      },
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
      ElMessage.success('添加部门成功')
      dialogVisible.add = false
      resetForm('addForm')
    } else {
      ElMessage.error('添加部门失败')
    }
  } catch (error) {
    console.error('添加部门失败:', error)
    ElMessage.error('添加部门失败')
  } finally {
    adding.value = false
  }
}

const showEditDept = (row: any) => {
  dialogVisible.edit = true
  Object.assign(editFormData, {
    dept_id: row.dept_id,
    dept_name: row.dept_name
  })
}

const updateDept = async () => {
  if (!editFormRef.value) return
  
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) return
    
    updating.value = true
    const deptData = {
      dept_name: editFormData.dept_name
    }
    const response = await axios.put(`/api/v1/admin/departments/${editFormData.dept_id}`, deptData, {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      },
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
      ElMessage.success('更新部门成功')
      dialogVisible.edit = false
      resetForm('editForm')
    } else {
      ElMessage.error('更新部门失败')
    }
  } catch (error) {
    console.error('更新部门失败:', error)
    ElMessage.error('更新部门失败')
  } finally {
    updating.value = false
  }
}

const resetForm = (formName: string) => {
  if (formName === 'addForm') {
    formData.dept_name = ''
    addFormRef.value?.resetFields()
  } else if (formName === 'editForm') {
    Object.assign(editFormData, {
      dept_id: '',
      dept_name: ''
    })
    editFormRef.value?.resetFields()
  }
}

onMounted(() => {
  selectAllDeptAndNum()
})
</script>

<style scoped>
.dept-manage {
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
  font-size: 20px;
}
</style> 