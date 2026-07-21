<template>
  <div class="leave-list">
    <div class="page-header">
      <h2>我的请假记录</h2>
    </div>

    <div class="apple-card table-card">
      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无请假记录"
        class="el-table--borderless"
      >
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.type }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ row.start_date }}</template>
        </el-table-column>

        <el-table-column label="结束时间" width="150">
          <template #default="{ row }">{{ row.end_date }}</template>
        </el-table-column>

        <el-table-column label="请假事由" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const list = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getStatusType = (status: string) => {
  switch (status) {
    case '待审批': return 'warning'
    case '已批准': return 'success'
    case '已拒绝': return 'danger'
    default: return 'info'
  }
}

const getList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/leave/my-list', {
      params: { currentPage: currentPage.value, pageSize: pageSize.value }
    })
    if (response.data && response.data.code === 200) {
      list.value = response.data.data || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('获取请假记录失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  getList()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  getList()
}

onMounted(() => getList())
</script>

<style scoped>
.leave-list {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text, #1d1d1f);
  margin: 0;
}

.table-card {
  padding: 24px;
}

.type-tag {
  background: #e8f4fd;
  color: #0071e3;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-table--borderless::before) { display: none; }
:deep(.el-table--borderless th.el-table__cell) {
  border-bottom: 1px solid #e5e5e7;
  background: transparent;
  color: #86868b;
  font-size: 12px;
  font-weight: 500;
}
:deep(.el-table--borderless td.el-table__cell) {
  padding: 12px 0;
  color: #1d1d1f;
  font-size: 14px;
}
</style>
