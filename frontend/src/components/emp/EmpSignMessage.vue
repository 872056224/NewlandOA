<template>
  <div class="sign-message">
    <h2 class="apple-title page-title">签到情况</h2>
    <p class="apple-subtitle">查看所有签到记录</p>

    <div class="apple-card table-wrapper">
      <el-table :data="tableData" v-loading="loading" class="el-table--borderless">
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column label="签到时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.checkInTime) }}
          </template>
        </el-table-column>
        <el-table-column label="签退时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatTime(row.checkOutTime) }}
          </template>
        </el-table-column>
        <el-table-column label="签到地址" min-width="180">
          <template #default="{ row }">
            {{ row.checkInAddress || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="签退地址" min-width="180">
          <template #default="{ row }">
            {{ row.checkOutAddress || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="getStatusClass(row.todayStatus)">
              {{ formatStatus(row.todayStatus) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[6, 10, 14]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      class="pagination-area"
      layout="total, sizes, prev, pager, next, jumper"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const tableData = ref<any[]>([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const selectByPage = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/attendance/my-records/page', {
      params: { currentPage: pagination.currentPage, pageSize: pagination.pageSize }
    })
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error('获取签到记录失败')
    }
  } catch (error) {
    console.error('获取签到记录失败:', error)
    ElMessage.error('获取签到记录失败')
  } finally {
    loading.value = false
  }
}

const formatTime = (dt: string): string => {
  if (!dt) return '--'
  return dt.substring(11, 16) // 取 HH:mm
}

const formatStatus = (status: string): string => {
  const map: Record<string, string> = {
    'NOT_CHECKED_IN': '未签到',
    'CHECKED_IN': '已签到',
    'CHECKED_OUT': '已签退',
    'LEAVE': '已请假',
    'MAKEUP_PENDING': '补卡审批中',
  }
  return map[status] || status || '--'
}

const getStatusClass = (status: string): string => {
  if (status === 'CHECKED_IN' || status === 'CHECKED_OUT') return 'state-badge state-signed'
  return 'state-badge state-unsigned'
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  selectByPage()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  selectByPage()
}

onMounted(() => { selectByPage() })
</script>

<style scoped>
.sign-message {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0;
}
.page-title { margin-bottom: 4px !important; }
.table-wrapper { padding: 0; overflow: hidden; }

:deep(.el-table--borderless) { border: none; }
:deep(.el-table--borderless::before) { display: none; }
:deep(.el-table--borderless th.el-table__cell) {
  border-bottom: 1px solid #e5e5e7;
  background: transparent;
  color: #86868b;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.5px;
  padding: 10px 0;
}
:deep(.el-table--borderless .el-table__cell) { border-bottom: 1px solid #e5e5e7; }
:deep(.el-table--borderless td.el-table__cell) { padding: 14px 0; color: #1d1d1f; font-size: 14px; }

.state-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 980px;
  display: inline-block;
}
.state-signed { background: #E8F5E9; color: #34C759; }
.state-unsigned { background: #FFEBEE; color: #FF3B30; }

.pagination-area { text-align: center; margin-top: 24px; }
:deep(.el-pagination) { --el-pagination-font-size: 13px; }
:deep(.el-pagination button:hover) { color: #0071E3; }
:deep(.el-pagination .el-pager li.active) { color: #0071E3; }
</style>
