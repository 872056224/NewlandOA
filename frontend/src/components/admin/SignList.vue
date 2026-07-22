<template>
  <div class="apple-page">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">考勤统计（仅工作日）</b>

    <!-- 数据展现表格 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      :border="false"
      stripe
      header-cell-class-name="apple-table-header"
      class="el-table--borderless"
    >
      <el-table-column label="日期" prop="date" min-width="120" align="center" />
      <el-table-column label="总人数" prop="totalEmployees" min-width="80" align="center" />
      <el-table-column label="请假人数" prop="onLeave" min-width="90" align="center" />
      <el-table-column label="应签到" min-width="90" align="center">
        <template #default="{ row }">
          {{ row.totalEmployees - row.onLeave }}
        </template>
      </el-table-column>
      <el-table-column label="已签到" prop="signed" min-width="80" align="center" />
      <el-table-column label="未签到" prop="unsigned" min-width="80" align="center" />
      <el-table-column label="出勤率" min-width="100" align="center">
        <template #default="{ row }">
          <span :style="rateStyle(row)">
            {{ computeRate(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="140" align="center">
        <template #default="{ row }">
          <el-button @click="showHistory(row)" type="warning" size="small">查看详情</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="考勤详情"
      width="500px"
      @close="resetForm"
    >
      <el-form
        :label-position="'right'"
        :model="detailData"
        class="demo-ruleForm"
        label-width="120px"
        ref="editFormRef"
        v-loading="detailLoading"
      >
        <el-form-item label="日期：">
          <el-input :disabled="true" v-model="detailData.date" style="width: 200px;" />
        </el-form-item>
        <el-divider content-position="left">考勤统计</el-divider>
        <el-form-item label="总人数：">
          <el-input :disabled="true" v-model="detailData.totalEmployees" style="width: 100px;" />
        </el-form-item>
        <el-form-item label="请假人数：">
          <el-input :disabled="true" v-model="detailData.onLeave" style="width: 100px;" />
        </el-form-item>
        <el-form-item label="应签到：">
          <el-input :disabled="true" v-model="detailData.expected" style="width: 100px;" />
        </el-form-item>
        <el-form-item label="已签到：">
          <el-input :disabled="true" v-model="detailData.signed" style="width: 100px;" />
        </el-form-item>
        <el-form-item label="未签到：">
          <el-input :disabled="true" v-model="detailData.unsigned" style="width: 100px;" />
        </el-form-item>
        <el-form-item label="出勤率：">
          <el-input :disabled="true" :value="detailData.rate" style="width: 100px;" />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const detailLoading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const editFormRef = ref<FormInstance>()

const detailData = reactive({
  date: '',
  totalEmployees: 0,
  onLeave: 0,
  expected: 0,
  signed: 0,
  unsigned: 0,
  rate: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

const computeRate = (row: any): string => {
  const expected = row.totalEmployees - row.onLeave
  if (expected <= 0) return '--'
  return ((row.signed / expected) * 100).toFixed(1) + '%'
}

const rateStyle = (row: any): Record<string, string> => {
  const expected = row.totalEmployees - row.onLeave
  if (expected <= 0) return {}
  const rate = (row.signed / expected) * 100
  if (rate >= 90) return { color: '#67c23a', fontWeight: 'bold' }
  if (rate >= 70) return { color: '#e6a23c', fontWeight: 'bold' }
  return { color: '#f56c6c', fontWeight: 'bold' }
}

const selectByPage = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance/daily-statistics', {
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
    })
    if (response.data && response.data.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
    } else {
      ElMessage.error('获取考勤统计失败')
    }
  } catch (error) {
    console.error('获取考勤统计失败:', error)
    ElMessage.error('获取考勤统计失败')
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

const showHistory = async (row: any) => {
  dialogVisible.value = true
  detailLoading.value = true

  // 设置基本信息
  Object.assign(detailData, {
    date: row.date,
    totalEmployees: row.totalEmployees,
    onLeave: row.onLeave,
    expected: row.totalEmployees - row.onLeave,
    signed: row.signed,
    unsigned: row.unsigned,
    rate: computeRate(row)
  })

  try {
    // 获取详细的考勤统计
    const response = await axios.get('/api/v1/admin/attendance/daily-details', {
      params: { date: row.date }
    })
    if (response.data && response.data.data) {
      const data = response.data.data
      const expected = data.totalEmployees - data.onLeave
      const rate = expected > 0 ? ((data.signed / expected) * 100).toFixed(1) + '%' : '--'
      Object.assign(detailData, {
        totalEmployees: data.totalEmployees,
        onLeave: data.onLeave,
        expected: expected,
        signed: data.signed,
        unsigned: data.unsigned,
        rate: rate
      })
    }
  } catch (error) {
    console.error('获取详细统计失败:', error)
    ElMessage.error('获取详细统计失败')
  } finally {
    detailLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(detailData, {
    date: '',
    totalEmployees: 0,
    onLeave: 0,
    expected: 0,
    signed: 0,
    unsigned: 0,
    rate: ''
  })
}

onMounted(() => {
  selectByPage()
})
</script>

<style scoped>
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

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: transparent;
  --el-pagination-border-radius: 0;
  --el-pagination-button-color: var(--apple-text);
  --el-pagination-hover-color: var(--apple-blue);
  --el-pagination-disabled-bg-color: transparent;
}
</style>
