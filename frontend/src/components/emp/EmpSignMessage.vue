<template>
  <div class="sign-message">
    <a class="page-title">签到情况</a>
    
    <!-- 数据展现表格 -->
    <el-table 
      :data="tableData" 
      :span-method="objectSpanMethod"
      v-loading="loading"
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
          <el-tag 
            effect="dark"
            :type="row.state === '已签到' ? 'success' : 'danger'"
            disable-transitions
          >
            {{ row.state }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    
    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[6, 10, 14]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      style="text-align: center; margin-top: 20px;"
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
  padding: 20px;
  min-height: 100%;
  background-color: #f5f5f5;
}

.page-title {
  color: #606266;
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
  display: block;
}
</style> 