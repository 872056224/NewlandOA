<template>
  <div class="apple-page" style="max-width: 100%; padding: 40px 32px;">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">加班审批</b>

    <el-tabs v-model="activeTab" @tab-change="fetchData" class="apple-tabs">
      <el-tab-pane label="待审批" name="pending" />
      <el-tab-pane label="已审批" name="history" />
    </el-tabs>

    <el-table :data="tableData" v-loading="loading" :border="false" stripe
      header-cell-class-name="apple-table-header" class="el-table--borderless">
      <el-table-column label="申请人" prop="empName" width="100" align="center" />
      <el-table-column label="部门" prop="deptName" width="120" align="center" />
      <el-table-column label="加班日期" prop="overtimeDate" width="120" align="center" />
      <el-table-column label="时段" width="150" align="center">
        <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
      </el-table-column>
      <el-table-column label="申请时长" width="100" align="center">
        <template #default="{ row }">{{ row.totalHours }} 小时</template>
      </el-table-column>
      <el-table-column label="核定工时" width="120" align="center" v-if="activeTab === 'pending'">
        <template #default="{ row }">
          <el-input-number v-model="row._editHours" :min="0.5" :max="24" :step="0.5" size="small" style="width: 100px" />
        </template>
      </el-table-column>
      <el-table-column label="核定工时" width="100" align="center" v-else>
        <template #default="{ row }">{{ row.actualHours ?? row.totalHours }} 小时</template>
      </el-table-column>
      <el-table-column label="事由" prop="reason" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
            {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已批准' : '已拒绝' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" v-if="activeTab === 'pending'">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="handleApprove(row)">批准</el-button>
          <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination :current-page="pagination.currentPage" :page-size="pagination.pageSize"
      :page-sizes="[8, 20, 50]" :total="pagination.total"
      @current-change="p => { pagination.currentPage = p; fetchData() }"
      @size-change="s => { pagination.pageSize = s; pagination.currentPage = 1; fetchData() }"
      layout="total, sizes, prev, pager, next, jumper" style="margin-top: 20px;" />

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝加班申请" width="400px" :close-on-click-modal="false" destroy-on-close>
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因（可选）" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejectLoading">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const activeTab = ref('pending')
const loading = ref(false)
const rejectLoading = ref(false)
const tableData = ref<any[]>([])
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectId = ref<number | null>(null)

const pagination = reactive({ currentPage: 1, pageSize: 8, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const url = '/api/v1/admin/attendance/overtime/pending'
    const response = await axios.get(url, { params: pagination })
    if (response.data && response.data.data) {
      tableData.value = (response.data.data || []).map((r: any) => ({
        ...r,
        _editHours: r.totalHours
      }))
      pagination.total = response.data.total || 0
    } else {
      tableData.value = []
    }
  } catch { tableData.value = [] }
  finally { loading.value = false }
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      row._editHours !== row.totalHours
        ? `核定工时：${row._editHours} 小时（原申请 ${row.totalHours} 小时），确定批准吗？`
        : `核定工时 ${row._editHours} 小时，确定批准吗？`,
      '批准确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }

  try {
    const actualHours = row._editHours !== row.totalHours ? row._editHours : undefined
    const response = await axios.put(`/api/v1/admin/attendance/overtime/${row.id}/approve`, null, {
      params: actualHours !== undefined ? { actualHours } : {}
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success('已批准')
    } else {
      ElMessage.success('已批准')
    }
    fetchData()
  } catch { ElMessage.error('操作失败') }
}

const handleReject = (row: any) => {
  currentRejectId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!currentRejectId.value) return
  rejectLoading.value = true
  try {
    await axios.put(`/api/v1/admin/attendance/overtime/${currentRejectId.value}/reject`, null, {
      params: { reason: rejectReason.value || undefined }
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch { ElMessage.error('操作失败') }
  finally { rejectLoading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
:deep(.apple-tabs .el-tabs__item) { font-size: 15px; font-weight: 500; color: #86868b; padding: 0 20px; }
:deep(.apple-tabs .el-tabs__item.is-active) { color: #0071e3; }
:deep(.apple-tabs .el-tabs__active-bar) { background-color: #0071e3; }
:deep(.apple-table-header .cell) { color: #86868b; font-weight: 500; }
:deep(.el-table--borderless) { border: none; }
:deep(.el-table--borderless::before) { display: none; }
</style>
