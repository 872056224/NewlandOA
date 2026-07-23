<template>
  <div class="apple-page">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <b class="apple-title" style="margin: 0;">加班记录</b>
      <div class="monthly-stat">
        本月加班：<strong>{{ monthlyHours }}</strong> 小时
      </div>
    </div>

    <div class="apple-card" style="padding: 0; overflow: hidden;">
      <el-table :data="tableData" v-loading="loading" class="el-table--borderless" empty-text="暂无加班记录">
        <el-table-column label="加班日期" prop="overtimeDate" width="120" align="center" />
        <el-table-column label="时段" width="160" align="center">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="申请时长" width="100" align="center">
          <template #default="{ row }">{{ row.totalHours }} 小时</template>
        </el-table-column>
        <el-table-column label="核定工时" width="100" align="center">
          <template #default="{ row }">{{ row.actualHours ?? '-' }} 小时</template>
        </el-table-column>
        <el-table-column label="事由" prop="reason" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已批准' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination :current-page="pagination.currentPage" :page-size="pagination.pageSize"
      :total="pagination.total" @current-change="handleCurrentChange"
      layout="total, prev, pager, next" style="text-align: center; margin-top: 20px;" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref<any[]>([])
const monthlyHours = ref(0)

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

const fetchData = async () => {
  loading.value = true
  try {
    const [listRes, hoursRes] = await Promise.all([
      axios.get('/api/v1/employee/attendance/overtime/my-list', { params: pagination }),
      axios.get('/api/v1/employee/attendance/overtime/monthly-hours')
    ])
    if (listRes.data?.data) {
      tableData.value = listRes.data.data
      pagination.total = listRes.data.total || 0
    }
    if (hoursRes.data?.data !== undefined) {
      monthlyHours.value = hoursRes.data.data
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

const handleCurrentChange = (p: number) => { pagination.currentPage = p; fetchData() }

onMounted(fetchData)
</script>

<style scoped>
.monthly-stat { font-size: 15px; color: #1d1d1f; background: #e8f0fe; padding: 8px 16px; border-radius: 8px; }
.monthly-stat strong { color: #0071e3; font-size: 20px; }
</style>
