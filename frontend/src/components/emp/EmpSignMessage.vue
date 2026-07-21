<template>
  <div class="sign-message">
    <h2 class="apple-title page-title">签到情况</h2>
    <p class="apple-subtitle">查看所有签到记录</p>

    <!-- 数据展现表格 -->
    <div class="apple-card table-wrapper">
      <el-table
        :data="tableData"
        :span-method="objectSpanMethod"
        v-loading="loading"
        class="el-table--borderless"
      >
        <el-table-column prop="date" label="日期" width="80" />
        <el-table-column
          label="签到时间"
          prop="signDate"
          min-width="180"
          align="center"
        />
        <el-table-column
          label="签到地址"
          prop="sign_address"
          min-width="200"
          align="center"
        />
        <el-table-column
          label="工号"
          prop="number"
          min-width="80"
          align="center"
        />
        <el-table-column
          label="姓名"
          prop="name"
          min-width="80"
          align="center"
        />
        <el-table-column
          label="部门"
          prop="dept_name"
          min-width="80"
          align="center"
        />
        <el-table-column label="签到状态" min-width="80" align="center">
          <template #default="{ row }">
            <span :class="row.state === '已签到' ? 'state-badge state-signed' : 'state-badge state-unsigned'">
              {{ row.state }}
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
      params: {
        currentPage: pagination.currentPage,
        pageSize: pagination.pageSize
      }
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

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  selectByPage()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  selectByPage()
}

const lengthO = (o: any): number => {
  const t = typeof o
  if (t === 'string') {
    return o.length
  } else if (t === 'object') {
    let n = 0
    for (const i in o) {
      n++
    }
    return n
  }
  return 0
}

const handleTableData = (data: any[]): any[] => {
  const arr: any[] = []
  let spanNum = 0
  
  for (let i = 0; i < data.length; i++) {
    const info = data[i]
    const info1 = {
      spanNum: spanNum,
      signDate: info.signDate.substring(
        info.signDate.lastIndexOf(' ') + 1,
        info.signDate.length
      ),
      number: info.number,
      name: info.name,
      dept_name: info.dept_name,
      state: info.state,
      sign_address: info.sign_address,
      date: data[i].signDate.substring(0, data[i].signDate.indexOf(' '))
    }
    spanNum++
    arr.push(info1)
  }
  return arr
}

const objectSpanMethod = ({ rowIndex, columnIndex }: any) => {
  if (columnIndex === 0) {
    if (rowIndex % 2 === 0) {
      return {
        rowspan: 2,
        colspan: 1
      }
    } else {
      return {
        rowspan: 0,
        colspan: 0
      }
    }
  }
}

onMounted(() => {
  selectByPage()
})
</script>

<style scoped>
.sign-message {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0;
}

.page-title {
  margin-bottom: 4px !important;
}

.table-wrapper {
  padding: 0;
  overflow: hidden;
}

/* 去边框表格 + 表头灰色小字 */
:deep(.el-table--borderless) {
  border: none;
}

:deep(.el-table--borderless::before) {
  display: none;
}

:deep(.el-table--borderless th.el-table__cell) {
  border-bottom: 1px solid var(--apple-bg-secondary);
  background: transparent;
  color: var(--apple-text-tertiary);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 10px 0;
}

:deep(.el-table--borderless .el-table__cell) {
  border-bottom: 1px solid var(--apple-bg-secondary);
}

:deep(.el-table--borderless td.el-table__cell) {
  padding: 14px 0;
  color: var(--apple-text);
  font-size: 14px;
}

/* 状态标记 */
.state-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 980px;
  display: inline-block;
}

.state-signed {
  background: #E8F5E9;
  color: var(--apple-green);
}

.state-unsigned {
  background: #FFEBEE;
  color: var(--apple-red);
}

.pagination-area {
  text-align: center;
  margin-top: 24px;
}

:deep(.el-pagination) {
  --el-pagination-font-size: 13px;
}

:deep(.el-pagination button:hover) {
  color: var(--apple-blue);
}

:deep(.el-pagination .el-pager li.active) {
  color: var(--apple-blue);
}
</style> 