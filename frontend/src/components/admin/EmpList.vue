<template>
  <div class="emp-list">
    <div class="page-header">
      <b class="page-title">员工信息管理</b>
      <div class="page-actions">
        <el-button @click="showAddEmp" type="primary">添加员工</el-button>
      </div>
    </div>

    <!-- 数据展现表格 -->
    <el-table 
      :data="filteredTableData" 
      v-loading="loading"
    >
      <el-table-column align="center">
        <template #header>
          <div class="table-header">
            <el-icon><Menu /></el-icon>
            <el-input
              placeholder="请输员工名字"
              size="small"
              style="width: 200px; margin-left: 10px;"
              v-model="searchUsers"
            />
          </div>
        </template>
        
        <el-table-column align="center" label="工号" min-width="120" prop="number" />
        <el-table-column align="center" label="姓名" min-width="120" prop="name" />
        <el-table-column align="center" label="生日" min-width="140" prop="birthday" />
        <el-table-column align="center" label="地址" min-width="150" prop="address" />
        <el-table-column align="center" label="所属部门" min-width="140" prop="dept_name" />
        <el-table-column align="center" label="职务" min-width="140" prop="duty_name" />
        <el-table-column align="center" label="操作" min-width="180">
          <template #default="{ row }">
            <el-button @click="showEditEmp(row)" type="warning">编辑</el-button>
            <el-button @click="showDeleteDialog(row)" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[5, 8, 12]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="text-align: center; margin-top: 20px;"
    />

    <!-- 添加员工对话框 -->
    <el-dialog 
      v-model="dialogVisible.add" 
      @close="resetForm('addForm')" 
      title="添加员工信息"
      width="500px"
    >
      <el-form 
        :model="formData" 
        :rules="rules" 
        ref="addFormRef" 
        label-width="100px"
      >
        <el-form-item label="员工姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="员工生日" prop="birthday">
          <el-input type="date" v-model="formData.birthday" />
        </el-form-item>
        <el-form-item label="员工地址" prop="address">
          <el-input v-model="formData.address" />
        </el-form-item>
        <el-form-item label="所属部门" prop="dept_id">
          <el-select placeholder="---请选择--" style="width: 100%" v-model="formData.dept_id">
            <el-option
              :key="item.dept_id"
              :label="item.dept_name"
              :value="item.dept_id"
              v-for="item in deptData"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="员工职务" prop="duty_id">
          <el-select placeholder="---请选择--" style="width: 100%" v-model="formData.duty_id">
            <el-option
              :key="item.duty_id"
              :label="item.duty_name"
              :value="item.duty_id"
              v-for="item in dutyData"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button type="primary" @click="addEmp" :loading="adding">添加</el-button>
        <el-button @click="resetForm('addForm')">重置</el-button>
      </template>
    </el-dialog>

    <!-- 编辑员工对话框 -->
    <el-dialog 
      v-model="dialogVisible.edit" 
      @close="resetForm('editForm')" 
      title="编辑员工信息"
      width="500px"
    >
      <el-form 
        :model="editFormData" 
        :rules="rules" 
        ref="editFormRef" 
        label-width="100px"
      >
        <el-form-item label="员工工号" prop="number">
          <el-input :disabled="true" v-model="editFormData.number" />
        </el-form-item>
        <el-form-item label="员工姓名" prop="name">
          <el-input v-model="editFormData.name" />
        </el-form-item>
        <el-form-item label="员工生日" prop="birthday">
          <el-input type="date" v-model="editFormData.birthday" />
        </el-form-item>
        <el-form-item label="家庭地址" prop="address">
          <el-input v-model="editFormData.address" />
        </el-form-item>
        <el-form-item label="所属部门" prop="dept_id">
          <el-select placeholder="---请选择--" style="width: 100%" v-model="editFormData.dept_id">
            <el-option
              :key="item.dept_id"
              :label="item.dept_name"
              :value="item.dept_id"
              v-for="item in deptData"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="员工职务" prop="duty_id">
          <el-select placeholder="---请选择--" style="width: 100%" v-model="editFormData.duty_id">
            <el-option
              :key="item.duty_id"
              :label="item.duty_name"
              :value="item.duty_id"
              v-for="item in dutyData"
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="updateEmp" type="warning" :loading="updating">修改</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="dialogVisible.delete"
      title="警告"
      width="400px"
    >
      <span>将同时清除员工的【考勤】数据！</span>
      <template #footer>
        <el-button @click="dialogVisible.delete = false">取 消</el-button>
        <el-button @click="deleteEmp" type="danger" :loading="deleting">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { Menu } from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(false)
const adding = ref(false)
const updating = ref(false)
const deleting = ref(false)
const searchUsers = ref('')
const tableData = ref<any[]>([])
const deptData = ref<any[]>([])
const dutyData = ref<any[]>([])

const addFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const dialogVisible = reactive({
  add: false,
  edit: false,
  delete: false
})

const formData = reactive({
  name: '',
  birthday: '',
  address: '',
  dept_id: '',
  duty_id: ''
})

const editFormData = reactive({
  number: '',
  name: '',
  birthday: '',
  address: '',
  dept_id: '',
  duty_id: '',
  dept_name: '',
  duty_name: ''
})

const currentEmp = ref<any>({})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  birthday: [
    { required: true, message: '请选择日期', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' }
  ],
  dept_id: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  duty_id: [
    { required: true, message: '请选择职务', trigger: 'change' }
  ]
}

const filteredTableData = computed(() => {
  if (!searchUsers.value) return tableData.value
  return tableData.value.filter(data => 
    data.name.toLowerCase().includes(searchUsers.value.toLowerCase())
  )
})

const selectByPage = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/employees', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error('获取员工列表失败')
    }
  } catch (error) {
    console.error('获取员工列表失败:', error)
    ElMessage.error('获取员工列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  selectByPage()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  selectByPage()
}

const showAddEmp = async () => {
  try {
    // 获取部门数据
    const deptResponse = await axios.get('/api/v1/admin/employees/departments')
    deptData.value = deptResponse.data.data || []
    
    // 获取职务数据
    const dutyResponse = await axios.get('/api/v1/admin/employees/duties')
    dutyData.value = dutyResponse.data.data || []
    
    dialogVisible.add = true
  } catch (error) {
    console.error('获取部门职务数据失败:', error)
    ElMessage.error('获取部门职务数据失败')
  }
}

const getDeptAndDutyData = async () => {
  try {
    const [deptResponse, dutyResponse] = await Promise.all([
      axios.get('/api/v1/admin/employees/departments'),
      axios.get('/api/v1/admin/employees/duties')
    ])
    
    if (deptResponse.data && deptResponse.data.data) {
      deptData.value = deptResponse.data.data || []
    }
    
    if (dutyResponse.data && dutyResponse.data.data) {
      dutyData.value = dutyResponse.data.data || []
    }
  } catch (error) {
    console.error('获取部门职务数据失败:', error)
    ElMessage.error('获取部门职务数据失败')
  }
}

const addEmp = async () => {
  if (!addFormRef.value) return
  
  try {
    const valid = await addFormRef.value.validate()
    if (!valid) return
    
    const empData = {
      name: formData.name,
      birthday: formData.birthday,
      address: formData.address,
      dept_id: formData.dept_id,
      duty_id: formData.duty_id
    }
    const response = await axios.post('/api/v1/admin/employees', empData, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.data === 'true' || response.data === true || (response.data && response.data.data)) {
      ElMessage.success('添加员工成功')
      dialogVisible.add = false
      resetForm('addForm')
      selectByPage()
    } else {
      ElMessage.error('添加员工失败')
    }
  } catch (error) {
    console.error('添加员工失败:', error)
    ElMessage.error('添加员工失败')
  }
}

const showEditEmp = async (row: any) => {
  try {
    // 获取部门数据
    const deptResponse = await axios.get('/api/v1/admin/employees/departments')
    deptData.value = deptResponse.data.data || []
    
    // 获取职务数据
    const dutyResponse = await axios.get('/api/v1/admin/employees/duties')
    dutyData.value = dutyResponse.data.data || []
    
    // 设置表单数据
    Object.assign(editFormData, {
      number: row.number,
      name: row.name,
      birthday: row.birthday,
      address: row.address,
      dept_id: row.dept_id,
      duty_id: row.duty_id,
      dept_name: row.dept_name,
      duty_name: row.duty_name
    })
    
    dialogVisible.edit = true
  } catch (error) {
    console.error('获取部门职务数据失败:', error)
    ElMessage.error('获取部门职务数据失败')
  }
}

const updateEmp = async () => {
  if (!editFormRef.value) return
  
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) return
    
    updating.value = true
    const empData = {
      name: editFormData.name,
      birthday: editFormData.birthday,
      address: editFormData.address,
      dept_id: editFormData.dept_id,
      duty_id: editFormData.duty_id
    }
    const response = await axios.put(`/api/v1/admin/employees/${editFormData.number}`, empData, {
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
      ElMessage.success('更新员工信息成功')
      dialogVisible.edit = false
    } else {
      ElMessage.error('更新员工信息失败')
    }
  } catch (error) {
    console.error('更新员工信息失败:', error)
    ElMessage.error('更新员工信息失败')
  } finally {
    updating.value = false
  }
}

const showDeleteDialog = (row: any) => {
  currentEmp.value = row
  dialogVisible.delete = true
}

const deleteEmp = async () => {
  deleting.value = true
  try {
    const response = await axios.delete(`/api/v1/admin/employees/${currentEmp.value.number}`)
    
    if (response.data === 'true' || response.data === true || (response.data && response.data.data)) {
      ElMessage.success('删除员工成功')
      selectByPage()
    } else {
      ElMessage.error('删除员工失败')
    }
  } catch (error) {
    console.error('删除员工失败:', error)
    ElMessage.error('删除员工失败')
  } finally {
    deleting.value = false
  }
}

const resetForm = (formName: string) => {
  if (formName === 'addForm') {
    Object.assign(formData, {
      name: '',
      birthday: '',
      address: '',
      dept_id: '',
      duty_id: ''
    })
    addFormRef.value?.resetFields()
  } else if (formName === 'editForm') {
    Object.assign(editFormData, {
      number: '',
      name: '',
      birthday: '',
      address: '',
      dept_id: '',
      duty_id: '',
      dept_name: '',
      duty_name: ''
    })
    editFormRef.value?.resetFields()
  }
}

onMounted(() => {
  selectByPage()
})
</script>

<style scoped>
.emp-list {
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

.table-header {
  display: flex;
  align-items: center;
}
</style> 