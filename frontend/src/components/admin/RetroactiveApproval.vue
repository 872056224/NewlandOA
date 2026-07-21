<template>
  <div class="apple-page" style="max-width: 100%; padding: 40px 32px;">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">补签审批</b>

    <div class="apple-card" style="padding: 24px;">
      <el-table
        :data="list"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无补签申请"
        class="el-table--borderless"
      >
        <el-table-column label="员工编号" prop="number" width="100" align="center" />
        <el-table-column label="日期" prop="sign_date" width="150" align="center" />
        <el-table-column label="时段" width="80" align="center">
          <template #default="{ row }">
            <span class="type-tag">{{ row.type === 'a' ? '上午' : '下午' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '待审批' ? 'warning' : row.status === '已批准' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)" :disabled="row.status !== '待审批'">
              批准
            </el-button>
            <el-button type="danger" size="small" @click="handleReject(row)" :disabled="row.status !== '待审批'">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container" style="margin-top: 20px; display: flex; justify-content: center;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[8, 20, 50]"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const list = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance/retroactive/pending', {
      params: { currentPage: currentPage.value, pageSize: pageSize.value }
    })
    if (response.data && response.data.code === 200) {
      list.value = response.data.data || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('获取补签列表失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定批准该补签申请吗？`, '确认', { type: 'success' })
    const response = await axios.put(`/api/v1/admin/attendance/retroactive/${row.id}/approve`)
    if (response.data && response.data.code === 200) {
      ElMessage.success('已批准')
      fetchList()
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleReject = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定拒绝该补签申请吗？`, '确认', { type: 'warning' })
    const response = await axios.put(`/api/v1/admin/attendance/retroactive/${row.id}/reject`)
    if (response.data && response.data.code === 200) {
      ElMessage.success('已拒绝')
      fetchList()
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleSizeChange = (val: number) => { pageSize.value = val; currentPage.value = 1; fetchList() }
const handleCurrentChange = (val: number) => { currentPage.value = val; fetchList() }

onMounted(() => fetchList())
</script>

<style scoped>
.type-tag {
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
  background: #e8f0fe;
  color: #0071e3;
}
</style>
