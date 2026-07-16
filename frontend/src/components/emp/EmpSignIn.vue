<template>
  <div class="sign-in">
    <div class="header-section">
      <h2 class="page-title">
        <el-icon class="title-icon"><Clock /></el-icon>
        今日签到
      </h2>
      <div class="current-time">
        <span>当前时间：{{ currentTime }}</span>
      </div>
    </div>

    <!-- 今日签到状态卡片 -->
    <div class="sign-cards">
      <el-row :gutter="20">
        <el-col :span="12" v-for="signRecord in todaySignData" :key="signRecord.type">
          <el-card class="sign-card" :class="getCardClass(signRecord)">
            <div class="card-header">
              <h3>{{ getSignTypeText(signRecord.type) }}</h3>
              <el-tag 
                :type="getTagType(signRecord.state)" 
                effect="dark"
                size="large"
              >
                {{ getStateText(signRecord) }}
              </el-tag>
            </div>
            
            <div class="card-content">
              <div class="time-info">
                <el-icon class="time-icon"><AlarmClock /></el-icon>
                <span>标准时间：{{ getStandardTime(signRecord.type) }}</span>
              </div>
              
              <div class="actual-time" v-if="signRecord.state === '已签到'">
                <el-icon class="actual-icon"><Check /></el-icon>
                <span>实际时间：{{ formatSignTime(signRecord.signDate) }}</span>
              </div>
              
              <div class="location-info" v-if="signRecord.sign_address">
                <el-icon class="location-icon"><Location /></el-icon>
                <span>签到地点：{{ signRecord.sign_address }}</span>
              </div>
            </div>
            
            <div class="card-footer">
                             <el-button 
                 :type="getButtonType(signRecord)"
                 :disabled="signRecord.state === '已签到'"
                 @click="handleSign(signRecord)"
                 :loading="signing && currentSignType === signRecord.type"
                 size="large"
                 class="sign-button"
               >
                 <el-icon>
                   <Check v-if="signRecord.state === '已签到'" />
                   <Clock v-else />
                 </el-icon>
                 {{ getButtonText(signRecord) }}
               </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 历史签到记录 -->
    <div class="history-section">
      <div class="section-header">
        <h3>最近签到记录</h3>
        <el-button @click="refreshData" :loading="loading" type="primary" plain>
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      
      <el-table 
        :data="historyData" 
        v-loading="loading" 
        stripe
        style="width: 100%"
        empty-text="暂无签到记录"
      >
        <el-table-column label="日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.signDate) }}
          </template>
        </el-table-column>
        
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'a' ? 'success' : 'warning'" size="small">
              {{ getSignTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            {{ formatSignTime(row.signDate) }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTagType(row.state)" size="small">
              {{ row.state }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="签到地点" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.sign_address || '未记录位置' }}</span>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
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

    <!-- 签到确认对话框 -->
    <el-dialog
      v-model="showSignDialog"
      :title="dialogTitle"
      width="450px"
      center
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <div class="confirm-icon">
          <el-icon size="48" color="#409EFF"><QuestionFilled /></el-icon>
        </div>
        
        <p class="confirm-text">
          确定要进行 <strong>{{ getSignTypeText(signInfo.type) }}</strong> 吗？
        </p>
        
        <div class="time-display">
          <p><strong>当前时间：</strong>{{ currentTime }}</p>
          <p><strong>标准时间：</strong>{{ getStandardTime(signInfo.type) }}</p>
          <p class="status-text" :class="getStatusClass()">
            <strong>状态：</strong>{{ getTimingStatus() }}
          </p>
        </div>
        
        <div class="location-section">
          <el-icon class="location-icon"><Location /></el-icon>
          <span v-if="!locationInfo">正在获取位置信息...</span>
          <span v-else-if="locationInfo.includes('失败')" class="location-error">{{ locationInfo }}</span>
          <span v-else class="location-success">{{ locationInfo }}</span>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showSignDialog = false" size="large">
            取消
          </el-button>
          <el-button 
            type="primary" 
            @click="confirmSign" 
            :loading="signing"
            size="large"
          >
            确认{{ getSignTypeText(signInfo.type) }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Clock, 
  AlarmClock, 
  Check, 
  Location, 
  Refresh, 
  QuestionFilled 
} from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const loading = ref(false)
const signing = ref(false)
const showSignDialog = ref(false)
const locationInfo = ref('')
const currentCoordinates = ref('')
const addressCache = ref('')
const currentSignType = ref('')
const currentTime = ref('')
const todaySignData = ref<any[]>([])
const historyData = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 定时器
let timeInterval: any = null

const signInfo = reactive({
  signDate: '',
  number: 0,
  type: 'a',
  state: '已签到'
})

// 计算属性
const dialogTitle = computed(() => {
  return `${getSignTypeText(signInfo.type)}确认`
})

// 时间格式化工具函数
const formatCurrentTime = (): string => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  return dateStr.split(' ')[0]
}

const formatSignTime = (dateStr: string): string => {
  if (!dateStr) return ''
  const parts = dateStr.split(' ')
  return parts.length > 1 ? parts[1].replace(/:\d{2}$/, '') : ''
}

// 获取签到类型文本
const getSignTypeText = (type: string): string => {
  return type === 'a' ? '上班签到' : '下班签退'
}

// 获取标准时间
const getStandardTime = (type: string): string => {
  return type === 'a' ? '08:30' : '17:30'
}

// 获取状态文本
const getStateText = (record: any): string => {
  if (record.state === '已签到') {
    return record.type === 'a' ? '已签到' : '已签退'
  }
  return record.type === 'a' ? '未签到' : '未签退'
}

// 获取标签类型
const getTagType = (state: string): string => {
  return state === '已签到' ? 'success' : 'warning'
}

// 获取卡片样式类
const getCardClass = (record: any): string => {
  return record.state === '已签到' ? 'signed' : 'unsigned'
}

// 获取按钮类型
const getButtonType = (record: any): string => {
  return record.state === '已签到' ? 'success' : 'primary'
}

// 获取按钮文本
const getButtonText = (record: any): string => {
  if (record.state === '已签到') {
    return record.type === 'a' ? '已签到' : '已签退'
  }
  return record.type === 'a' ? '立即签到' : '立即签退'
}

// 获取当前时间状态
const getTimingStatus = (): string => {
  const now = new Date()
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentTimeNum = currentHour * 60 + currentMinute
  
  if (signInfo.type === 'a') {
    // 上班签到：8:30之前正常，之后迟到
    const standardTime = 8 * 60 + 30 // 8:30
    if (currentTimeNum <= standardTime) {
      return '正常'
    } else {
      const lateMinutes = currentTimeNum - standardTime
      return `迟到 ${Math.floor(lateMinutes / 60)}小时${lateMinutes % 60}分钟`
    }
  } else {
    // 下班签退：17:30之前早退，之后正常
    const standardTime = 17 * 60 + 30 // 17:30
    if (currentTimeNum >= standardTime) {
      return '正常'
    } else {
      const earlyMinutes = standardTime - currentTimeNum
      return `早退 ${Math.floor(earlyMinutes / 60)}小时${earlyMinutes % 60}分钟`
    }
  }
}

// 获取状态样式类
const getStatusClass = (): string => {
  const status = getTimingStatus()
  if (status === '正常') return 'normal-status'
  if (status.includes('迟到')) return 'late-status'
  if (status.includes('早退')) return 'early-status'
  return ''
}

// 获取今日签到数据
const getTodaySignData = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/attendance/my-records')
    
    if (response.data && response.data.code === 200) {
      const records = response.data.data || []
      
      // 获取今日日期
      const today = new Date().toISOString().split('T')[0]
      
      // 过滤今日记录
      const todayRecords = records.filter((record: any) => {
        return record.signDate && record.signDate.startsWith(today)
      })
      
      // 确保有上班和下班两条记录
      const morningRecord = todayRecords.find((r: any) => r.type === 'a') || {
        type: 'a',
        signDate: `${today} 08:30:00:000`,
        state: '未签到',
        number: 0,
        name: '',
        dept_name: '',
        sign_address: ''
      }
      
      const eveningRecord = todayRecords.find((r: any) => r.type === 'p') || {
        type: 'p',
        signDate: `${today} 17:30:00:000`,
        state: '未签到',
        number: 0,
        name: '',
        dept_name: '',
        sign_address: ''
      }
      
      todaySignData.value = [morningRecord, eveningRecord]
    } else {
      ElMessage.error(response.data?.message || '获取签到数据失败')
    }
  } catch (error: any) {
    console.error('获取签到数据失败:', error)
    ElMessage.error('网络错误，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 获取历史签到数据（分页）
const getHistoryData = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/employee/attendance/my-records/page', {
      params: {
        currentPage: currentPage.value,
        pageSize: pageSize.value
      }
    })
    
    if (response.data && response.data.code === 200) {
      historyData.value = response.data.data || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error(response.data?.message || '获取历史记录失败')
    }
  } catch (error: any) {
    console.error('获取历史记录失败:', error)
    ElMessage.error('网络错误，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 处理签到
const handleSign = async (record: any) => {
  if (record.state === '已签到') {
    return
  }
  
  // 设置签到信息
  Object.assign(signInfo, {
    signDate: record.signDate,
    number: record.number,
    type: record.type,
    state: '已签到'
  })
  
  currentSignType.value = record.type
  locationInfo.value = '正在获取位置信息...'
  showSignDialog.value = true

  // 尝试获取位置（国内 Google 定位被墙会失败，不影响签到）
  try {
    const coordinates = await getCurrentLocation()
    currentCoordinates.value = coordinates
    await getLocationAddress(coordinates)
  } catch (error: any) {
    console.warn('获取位置失败，跳过定位:', error.message)
    locationInfo.value = '未获取位置（不影响签到）'
    currentCoordinates.value = ''
  }
}

// 确认签到
const confirmSign = async () => {
  if (signing.value) {
    return // 防止重复提交
  }
  
  signing.value = true
  
  try {
    // 使用缓存的坐标，如果没有则尝试获取
    let coordinates = currentCoordinates.value
    if (!coordinates) {
      try {
        coordinates = await getCurrentLocation()
        currentCoordinates.value = coordinates
      } catch (locError) {
        console.warn('获取位置失败，将使用空地址签到:', locError)
        coordinates = ''
        locationInfo.value = '位置获取失败，签到记录将不包含地址信息'
      }
    }

    // 构建签到数据
    const signData = {
      number: signInfo.number,
      signDate: signInfo.signDate,
      type: signInfo.type,
      state: '已签到'
    }
    
    // 发送签到请求
    const response = await axios.post(
      `/api/v1/employee/attendance/check-in?coordinates=${encodeURIComponent(coordinates)}`,
      signData,
      {
        headers: {
          'Content-Type': 'application/json'
        }
      }
    )
    
    if (response.data && response.data.code === 200) {
      ElMessage.success(`${getSignTypeText(signInfo.type)}成功！`)
      showSignDialog.value = false
      // 刷新数据
      await getTodaySignData()
      await getHistoryData()
    } else {
      ElMessage.error(response.data?.message || '签到失败，请重试')
    }
  } catch (error: any) {
    console.error('签到失败:', error)
    ElMessage.error(error.message || '签到失败，请重试')
  } finally {
    signing.value = false
    currentSignType.value = ''
  }
}

// 获取地理位置
const getCurrentLocation = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持地理定位'))
      return
    }
    
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const coords = `${position.coords.latitude},${position.coords.longitude}`
        resolve(coords)
      },
      (error) => {
        let message = '获取位置失败'
        switch (error.code) {
          case error.PERMISSION_DENIED:
            message = '用户拒绝了位置请求'
            break
          case error.POSITION_UNAVAILABLE:
            message = '位置信息不可用'
            break
          case error.TIMEOUT:
            message = '获取位置超时'
            break
        }
        reject(new Error(message))
      },
      {
        enableHighAccuracy: false,
        timeout: 15000,
        maximumAge: 600000
      }
    )
  })
}

// 根据坐标获取地址信息
const getLocationAddress = async (coordinates: string) => {
  try {
    const response = await axios.get('/api/v1/employee/location/address', {
      params: { coordinates }
    })
    
    if (response.data && response.data.code === 200) {
      addressCache.value = response.data.data
      locationInfo.value = `当前位置：${response.data.data}`
    } else {
      locationInfo.value = `位置解析失败：${response.data?.message || '未知错误'}`
    }
  } catch (error: any) {
    console.error('获取地址信息失败:', error)
    locationInfo.value = `地址解析失败：${error.message || '网络错误'}`
  }
}

// 刷新数据
const refreshData = async () => {
  await Promise.all([
    getTodaySignData(),
    getHistoryData()
  ])
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  getHistoryData()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  getHistoryData()
}

// 更新当前时间
const updateCurrentTime = () => {
  currentTime.value = formatCurrentTime()
}

// 组件挂载时执行
onMounted(() => {
  updateCurrentTime()
  timeInterval = setInterval(updateCurrentTime, 1000)
  refreshData()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.sign-in {
  padding: 24px;
  background: linear-gradient(135deg, #f1f1f3 0%, #010137 100%);
  min-height: 100vh;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.page-title {
  display: flex;
  align-items: center;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.title-icon {
  margin-right: 8px;
  color: #272728;
}

.current-time {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

.sign-cards {
  margin-bottom: 32px;
}

.sign-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  background: rgba(255, 255, 255, 0.95);
}

.sign-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.sign-card.signed {
  border-left: 4px solid #171717;
}

.sign-card.unsigned {
  border-left: 4px solid #222221;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.card-content {
  margin-bottom: 20px;
}

.time-info,
.actual-time,
.location-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: #606266;
}

.time-icon,
.actual-icon,
.location-icon {
  margin-right: 6px;
  color: #909399;
}

.location-success {
  color: #67C23A;
  font-weight: 500;
}

.location-error {
  color: #444343;
  font-weight: 500;
}

.card-footer {
  text-align: center;
}

.sign-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}

.history-section {
  background: rgba(255, 255, 255, 0.95);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.dialog-content {
  text-align: center;
  padding: 20px 0;
}

.confirm-icon {
  margin-bottom: 16px;
}

.confirm-text {
  font-size: 18px;
  color: #303133;
  margin-bottom: 20px;
}

.time-display {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.time-display p {
  margin: 8px 0;
  color: #606266;
}

.status-text.normal-status {
  color: #67C23A;
}

.status-text.late-status {
  color: #413f3f;
}

.status-text.early-status {
  color: #2f2e2e;
}

.location-section {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.location-section .location-icon {
  margin-right: 6px;
}

.dialog-footer {
  text-align: center;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-pagination) {
  margin-top: 20px;
}
</style> 