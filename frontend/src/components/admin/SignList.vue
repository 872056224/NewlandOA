<template>
  <div class="sign-list">
    <b class="page-title">签到时间列表</b>
    
    <!-- 数据展现表格 -->
    <el-table :data="tableData" v-loading="loading">
      <el-table-column 
        label="签到日期" 
        prop="date" 
        min-width="180" 
        align="center" 
      />
      <el-table-column label="操作" min-width="200" align="center">
        <template #default="{ row }">
          <el-button @click="showHistory(row)" type="warning">查看详细信息</el-button>
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
    />

    <!-- 查看具体签到信息对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="查看具体签到信息" 
      width="550px"
      @close="resetForm"
    >
      <el-form 
        :label-position="'right'" 
        :model="editFormData" 
        class="demo-ruleForm" 
        label-width="120px"
        ref="editFormRef"
        v-loading="detailLoading"
      >
        <el-form-item label="查看的日期：" prop="signDate">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.signDate"
          />
        </el-form-item>
        
        <el-divider content-position="left">上班签到统计</el-divider>
        <el-form-item label="已签到人数：" prop="morningSignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.morningSignedCount"
          />
          <el-button @click="showSignCount('a')" type="warning" style="margin-left: 10px;">
            查看详情
          </el-button>
        </el-form-item>
        <el-form-item label="未签到人数：" prop="morningUnsignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.morningUnsignedCount"
          />
          <el-button @click="showNoSignCount('a')" type="warning" style="margin-left: 10px;">
            查看详情
          </el-button>
        </el-form-item>
        
        <el-divider content-position="left">下班签退统计</el-divider>
        <el-form-item label="已签退人数：" prop="eveningSignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.eveningSignedCount"
          />
          <el-button @click="showSignCount('p')" type="warning" style="margin-left: 10px;">
            查看详情
          </el-button>
        </el-form-item>
        <el-form-item label="未签退人数：" prop="eveningUnsignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.eveningUnsignedCount"
          />
          <el-button @click="showNoSignCount('p')" type="warning" style="margin-left: 10px;">
            查看详情
          </el-button>
        </el-form-item>
        
        <el-divider content-position="left">总计</el-divider>
        <el-form-item label="总签到人次：" prop="totalSignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.totalSignedCount"
          />
        </el-form-item>
        <el-form-item label="总未签到人次：" prop="totalUnsignedCount">
          <el-input 
            :disabled="true" 
            style="background-color: white; width: 150px;"
            v-model="editFormData.totalUnsignedCount"
          />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const editFormRef = ref<FormInstance>()

const editFormData = reactive({
  signDate: '',
  signCount: '',
  noSignCount: '',
  morningSignedCount: 0,
  morningUnsignedCount: 0,
  eveningSignedCount: 0,
  eveningUnsignedCount: 0,
  totalSignedCount: 0,
  totalUnsignedCount: 0
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 8,
  total: 0
})

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
  Object.assign(editFormData, {
    signDate: row.date,
    signCount: row.yc,
    noSignCount: row.nc,
    morningSignedCount: 0,
    morningUnsignedCount: 0,
    eveningSignedCount: 0,
    eveningUnsignedCount: 0,
    totalSignedCount: 0,
    totalUnsignedCount: 0
  })
  
  try {
    // 获取详细的签到签退统计
    const response = await axios.get('/api/v1/admin/attendance/daily-details', {
      params: { date: row.date }
    })
    
    if (response.data && response.data.data) {
      const data = response.data.data
      Object.assign(editFormData, {
        morningSignedCount: data.morningSignedCount || 0,
        morningUnsignedCount: data.morningUnsignedCount || 0,
        eveningSignedCount: data.eveningSignedCount || 0,
        eveningUnsignedCount: data.eveningUnsignedCount || 0,
        totalSignedCount: data.totalSignedCount || 0,
        totalUnsignedCount: data.totalUnsignedCount || 0
      })
    }
  } catch (error) {
    console.error('获取详细统计失败:', error)
    ElMessage.error('获取详细统计失败')
  } finally {
    detailLoading.value = false
  }
}

const showSignCount = (type?: string) => {
  // 根据类型构建查询参数
  let queryParams = `type=signed&date=${editFormData.signDate}`
  if (type) {
    queryParams += `&signType=${type}`
  }
  
  // 在新标签页中打开已签到页面
  window.open(`/admin-home/sign-statistics?${queryParams}`, '_blank')
  dialogVisible.value = false
}

const showNoSignCount = (type?: string) => {
  // 根据类型构建查询参数
  let queryParams = `type=unsigned&date=${editFormData.signDate}`
  if (type) {
    queryParams += `&signType=${type}`
  }
  
  // 在新标签页中打开未签到页面
  window.open(`/admin-home/sign-statistics?${queryParams}`, '_blank')
  dialogVisible.value = false
}

const resetForm = () => {
  Object.assign(editFormData, {
    signDate: '',
    signCount: '',
    noSignCount: '',
    morningSignedCount: 0,
    morningUnsignedCount: 0,
    eveningSignedCount: 0,
    eveningUnsignedCount: 0,
    totalSignedCount: 0,
    totalUnsignedCount: 0
  })
}

onMounted(() => {
  selectByPage()
})
</script>

<style scoped>
.sign-list {
  padding: 20px;
}

.page-title {
  color: red;
  font-size: 20px;
  margin-bottom: 20px;
  display: block;
}
</style> 