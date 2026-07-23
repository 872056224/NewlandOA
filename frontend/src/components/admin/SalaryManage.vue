<template>
  <div class="apple-page" style="padding: 40px 32px; max-width: 1400px;">
    <div class="header-row">
      <b class="apple-title" style="margin: 0;">工资核算</b>
      <div class="header-actions">
        <el-select v-model="selectedMonth" class="month-select" size="large">
          <el-option v-for="m in monthOptions" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button class="apple-btn apple-btn-primary" @click="handleCalculate" :loading="calculating">
          核算当月
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="searchText" placeholder="按姓名模糊搜索" clearable prefix-icon="Search"
        style="width: 260px" @input="handleSearch" />
    </div>

    <el-table :data="pageData" v-loading="loading" :border="false" stripe
      header-cell-class-name="apple-table-header" class="el-table--borderless">
      <el-table-column label="姓名" prop="empName" width="100" align="center" />
      <el-table-column label="部门" prop="deptName" width="120" align="center" />
      <el-table-column label="职务" prop="dutyName" width="100" align="center" />
      <el-table-column label="基础月薪" width="120" align="right">
        <template #default="{ row }">{{ row.baseSalary?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="应出勤" prop="workDays" width="80" align="center" />
      <el-table-column label="实际出勤" width="90" align="center">
        <template #default="{ row }">{{ row.actualAttendanceDays ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="缺时(分)" prop="totalMissingMinutes" width="80" align="center" />
      <el-table-column label="缺时扣款" width="110" align="right">
        <template #default="{ row }">{{ row.missingDeduction?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="加班(时)" prop="overtimeHours" width="80" align="center" />
      <el-table-column label="加班工资" width="110" align="right">
        <template #default="{ row }">{{ row.overtimePay?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="请假(天)" prop="leaveDays" width="80" align="center" />
      <el-table-column label="请假扣款" width="110" align="right">
        <template #default="{ row }">{{ row.leaveDeduction?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="应发工资" width="130" align="right" fixed="right">
        <template #default="{ row }">
          <strong style="color:#0071e3">{{ row.finalSalary?.toFixed(2) }}</strong>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50]"
      :total="pagination.total"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: center;" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'

const selectedMonth = ref('2026-07')
const loading = ref(false)
const calculating = ref(false)
const allData = ref<any[]>([])
const searchText = ref('')

const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0 })

const monthOptions = computed(() => {
  const now = new Date()
  const options: string[] = []
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
})

const filteredData = computed(() => {
  if (!searchText.value) return allData.value
  const q = searchText.value.toLowerCase()
  return allData.value.filter((r: any) => (r.empName || '').toLowerCase().includes(q))
})

const pageData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  return filteredData.value.slice(start, start + pagination.pageSize)
})

// 搜索/数据变化时更新分页总数
watch(filteredData, (val) => {
  pagination.total = val.length
}, { immediate: true })

const fetchData = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/api/v1/admin/salary/list/${selectedMonth.value}`)
    allData.value = response.data?.data || []
  } catch { allData.value = [] }
  finally { loading.value = false }
}

const handlePageChange = (page: number) => { pagination.currentPage = page }
const handleSizeChange = (size: number) => { pagination.pageSize = size; pagination.currentPage = 1 }

const handleSearch = () => {
  pagination.currentPage = 1
}

const handleCalculate = async () => {
  try {
    await ElMessageBox.confirm(
      `将核算 ${selectedMonth.value} 月份所有员工的工资，是否继续？`,
      '工资核算', { confirmButtonText: '开始核算', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }

  calculating.value = true
  try {
    const response = await axios.post(`/api/v1/admin/salary/calculate/${selectedMonth.value}`)
    ElMessage.success(response.data?.data || '核算完成')
    await fetchData()
  } catch { ElMessage.error('核算失败') }
  finally { calculating.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.month-select { width: 140px; }
.search-bar { margin-bottom: 16px; }
:deep(.apple-table-header .cell) { color: #86868b; font-weight: 500; }
:deep(.el-table--borderless) { border: none; }
</style>
