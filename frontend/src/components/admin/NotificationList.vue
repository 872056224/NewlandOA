<template>
  <div class="notif-page">
    <h2 class="apple-title page-title">通知列表</h2>
    <p class="apple-subtitle">查看所有系统通知</p>

    <div class="apple-card table-wrapper">
      <el-table :data="tableData" v-loading="loading" class="el-table--borderless">
        <el-table-column label="标题" prop="title" min-width="160" />
        <el-table-column label="内容" prop="content" min-width="260" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ formatType(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="create_time" width="160" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.is_read" size="small" type="danger">未读</el-tag>
            <span v-else class="read-text">已读</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.is_read" size="small" text @click="markRead(row)">标为已读</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      class="pagination-area"
      layout="total, prev, pager, next"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref<any[]>([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/admin/notifications', {
      params: { currentPage: pagination.currentPage, pageSize: pagination.pageSize }
    })
    if (res.data?.data) {
      tableData.value = Array.isArray(res.data.data) ? res.data.data : []
      pagination.total = res.data.total || 0
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  fetchList()
}

const markRead = async (row: any) => {
  try {
    await axios.put(`/api/v1/admin/notifications/${row.id}/read`)
    row.is_read = 1
  } catch { /* ignore */ }
}

const formatType = (type: string) => {
  const map: Record<string, string> = {
    'leave_submitted': '请假提交',
    'leave_approved': '请假批准',
    'leave_rejected': '请假拒绝',
    'retroactive_submitted': '补签提交',
    'retroactive_approved': '补签批准',
    'retroactive_rejected': '补签拒绝',
  }
  return map[type] || type
}

onMounted(fetchList)
</script>

<style scoped>
.notif-page { max-width: 1100px; margin: 0 auto; padding: 0; }
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
  padding: 10px 0;
}
:deep(.el-table--borderless .el-table__cell) { border-bottom: 1px solid #e5e5e7; }
:deep(.el-table--borderless td.el-table__cell) { padding: 14px 0; color: #1d1d1f; font-size: 14px; }

.type-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 980px;
  background: #f5f5f7;
  color: #86868b;
}

.read-text { font-size: 13px; color: #86868b; }

.pagination-area { text-align: center; margin-top: 24px; }
:deep(.el-pagination) { --el-pagination-font-size: 13px; }
:deep(.el-pagination button:hover) { color: #0071e3; }
:deep(.el-pagination .el-pager li.active) { color: #0071e3; }
</style>
