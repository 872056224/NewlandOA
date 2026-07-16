<template>
  <div class="unsigned-list">
    <b class="page-title">未签到员工</b>
    
    <div class="toolbar">
      <el-button @click="showToday" type="primary">查看今日</el-button>
    </div>

    <!-- 数据展现表格 -->
    <el-table :data="filteredTableData" v-loading="loading">
      <el-table-column align="center">
        <template #header>
          <div class="header-content">
            <el-icon><Menu /></el-icon>
            <el-input
              v-model="searchUsers"
              placeholder="请输入员工工号"
              size="small"
              style="width: 200px; margin-left: 10px"
            />
          </div>
        </template>
        
        <el-table-column label="日期" prop="signDate" min-width="200" align="center" />
        <el-table-column label="工号" prop="number" min-width="120" align="center" />
        <el-table-column label="姓名" prop="name" min-width="120" align="center" />
        <el-table-column label="部门" prop="dept_name" min-width="120" align="center" />
        <el-table-column label="签到状态" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag
              effect="dark"
              :type="row.state === '已签到' ? 'success' : 'danger'"
            >
              {{ row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" align="center">
          <template #default="{ row }">
            <el-button
              :disabled="row.tag === 0"
              :type="row.tag === 0 ? 'info' : 'success'"
              @click="handleMakeUp(row)"
            >
              {{ row.tag === 0 ? '超期不可修改' : '补签' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[6, 8, 10]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px"
    />

    <!-- 补签确认对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="警告"
      width="400px"
    >
      <span>确定要补签吗？</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="success" @click="confirmMakeUp">补签</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Menu } from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(false)
const dialogVisible = ref(false)
const searchUsers = ref('')
const showTodayOnly = ref(false)
const currentEmp = ref<any>({})
const tableData = ref<any[]>([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const filteredTableData = computed(() => {
  if (!searchUsers.value) return tableData.value
  return tableData.value.filter(data => 
    data.number.toLowerCase().includes(searchUsers.value.toLowerCase())
  )
})

const selectByPage = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance/unsigned', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error('获取未签到记录失败')
    }
  } catch (error) {
    console.error('获取未签到记录失败:', error)
    ElMessage.error('获取未签到记录失败')
  } finally {
    loading.value = false
  }
}

const selectTodayByPage = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance/today/unsigned', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error('获取今日未签到记录失败')
    }
  } catch (error) {
    console.error('获取今日未签到记录失败:', error)
    ElMessage.error('获取今日未签到记录失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  if (showTodayOnly.value) {
    selectTodayByPage()
  } else {
    selectByPage()
  }
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  if (showTodayOnly.value) {
    selectTodayByPage()
  } else {
    selectByPage()
  }
}

const showToday = () => {
  showTodayOnly.value = !showTodayOnly.value
  pagination.currentPage = 1
  if (showTodayOnly.value) {
    selectTodayByPage()
  } else {
    selectByPage()
  }
}

const handleMakeUp = (row: any) => {
  if (row.tag === 0) return
  currentEmp.value = row
  dialogVisible.value = true
}

const confirmMakeUp = async () => {
  dialogVisible.value = false
  try {
    const response = await axios.put(`/api/v1/admin/attendance/${currentEmp.value.id}/approve`)
    
    if (response.data === 'true' || response.data === true || (response.data && response.data.data)) {
      ElMessage.success('补签成功')
      if (showTodayOnly.value) {
        selectTodayByPage()
      } else {
        selectByPage()
      }
    } else {
      ElMessage.error('补签失败')
    }
  } catch (error) {
    console.error('补签失败:', error)
    ElMessage.error('补签失败')
  }
}

onMounted(() => {
  selectByPage()
})
</script>

<style scoped>
.unsigned-list {
  padding: 20px;
}

.page-title {
  color: red;
  font-size: 20px;
  margin-bottom: 20px;
  display: block;
}

.toolbar {
  float: right;
  margin-bottom: 10px;
}

.header-content {
  display: flex;
  align-items: center;
}

.dialog-footer {
  text-align: right;
}
</style> 