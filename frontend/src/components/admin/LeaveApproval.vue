<template>
  <div class="apple-page">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">请假审批管理</b>

    <!-- Tabs: 待审批 / 已审批 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="apple-tabs">
      <el-tab-pane label="待审批" name="pending" />
      <el-tab-pane label="已审批" name="history" />
    </el-tabs>

    <!-- 数据表格 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      :border="false"
      stripe
      header-cell-class-name="apple-table-header"
      class="el-table--borderless"
    >
      <el-table-column align="center" label="申请人" prop="name" width="80" />
      <el-table-column align="center" label="部门" prop="dept_name" width="110" />
      <el-table-column align="center" label="类型" width="70">
        <template #default="{ row }">
          <span class="type-tag">{{ row.type || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="开始时间" prop="start_date" width="155" />
      <el-table-column align="center" label="结束时间" prop="end_date" width="155" />
      <el-table-column align="left" label="事由" prop="reason" min-width="120" show-overflow-tooltip />
      <el-table-column align="center" label="状态" width="75">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" effect="plain" class="status-tag">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 操作列：仅待审批页签显示 -->
      <el-table-column align="center" label="操作" width="150" v-if="activeTab === 'pending'">
        <template #default="{ row }">
          <el-button
            class="apple-btn apple-btn-primary"
            size="small"
            @click="handleApprove(row)"
          >
            批准
          </el-button>
          <el-button
            class="apple-btn apple-btn-danger"
            size="small"
            @click="handleReject(row)"
          >
            拒绝
          </el-button>
        </template>
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
      style="margin-top: 20px;"
      background="false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const activeTab = ref('pending')
const loading = ref(false)
const tableData = ref<any[]>([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const getStatusType = (status: string): string => {
  switch (status) {
    case '待审批': return 'warning'
    case '已批准': return 'success'
    case '已拒绝': return 'danger'
    default: return 'info'
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    let url = ''
    const params: Record<string, any> = {
      currentPage: pagination.currentPage,
      pageSize: pagination.pageSize
    }

    if (activeTab.value === 'pending') {
      url = '/api/v1/admin/leave/pending'
    } else {
      url = '/api/v1/admin/leave/list'
      params.status = '已批准'
    }

    const response = await axios.get(url, { params })

    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('获取请假列表失败:', error)
    ElMessage.error('获取请假列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pagination.currentPage = 1
  fetchData()
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.currentPage = 1
  fetchData()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  fetchData()
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要批准【${row.name}】的请假申请吗？`,
      '批准确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    loading.value = true
    try {
      const response = await axios.put(`/api/v1/admin/leave/${row.id}/approve`)
      if (response.data && response.data.code === 200) {
        ElMessage.success('已批准该请假申请')
      } else {
        ElMessage.success('已批准该请假申请')
      }
      fetchData()
    } catch (error) {
      console.error('批准失败:', error)
      ElMessage.error('批准失败，请重试')
    } finally {
      loading.value = false
    }
  } catch {
    // 用户取消操作，不做处理
  }
}

const handleReject = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝【${row.name}】的请假申请吗？`,
      '拒绝确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    try {
      const response = await axios.put(`/api/v1/admin/leave/${row.id}/reject`)
      if (response.data && response.data.code === 200) {
        ElMessage.success('已拒绝该请假申请')
      } else {
        ElMessage.success('已拒绝该请假申请')
      }
      fetchData()
    } catch (error) {
      console.error('拒绝失败:', error)
      ElMessage.error('拒绝失败，请重试')
    } finally {
      loading.value = false
    }
  } catch {
    // 用户取消操作，不做处理
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* ── Tabs ── */
:deep(.apple-tabs .el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  padding: 0 20px;
}

:deep(.apple-tabs .el-tabs__item.is-active) {
  color: var(--apple-blue, #0071e3);
}

:deep(.apple-tabs .el-tabs__active-bar) {
  background-color: var(--apple-blue, #0071e3);
  height: 2px;
}

:deep(.apple-tabs .el-tabs__header) {
  margin-bottom: 20px;
  border-bottom: 1px solid #e5e5e7;
}

/* ── Table header ── */
:deep(.apple-table-header .cell) {
  color: var(--apple-text-secondary);
  font-weight: 500;
}

:deep(.el-table--borderless) {
  border: none;
}

:deep(.el-table--borderless::before) {
  display: none;
}

/* ── Status tags ── */
:deep(.status-tag) {
  font-size: 13px;
  font-weight: 500;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
}

:deep(.status-tag.el-tag--warning) {
  background-color: #fff3e0;
  color: #f59e0b;
}

:deep(.status-tag.el-tag--success) {
  background-color: #e8f5e9;
  color: #10b981;
}

:deep(.status-tag.el-tag--danger) {
  background-color: #fce4ec;
  color: #ef4444;
}

/* ── Type tag ── */
.type-tag {
  display: inline-block;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 4px;
  background-color: #e8f0fe;
  color: #0071e3;
}

/* ── Pagination ── */
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-border-radius: 0;
  --el-pagination-button-color: var(--apple-text);
  --el-pagination-hover-color: var(--apple-blue);
  --el-pagination-disabled-bg-color: transparent;
}

/* ── Apple buttons ── */
.apple-btn {
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  padding: 6px 16px;
  border: none;
  transition: all 0.2s ease;
}

.apple-btn-primary {
  background-color: #0071e3;
  color: #fff;
}

.apple-btn-primary:hover {
  background-color: #0077ed;
}

.apple-btn-danger {
  background-color: #ff3b30;
  color: #fff;
}

.apple-btn-danger:hover {
  background-color: #ff453a;
}
</style>
