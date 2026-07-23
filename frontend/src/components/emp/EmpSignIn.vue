<template>
  <div class="sign-in">
    <!-- 顶部签到大时钟 -->
    <div class="clock-section">
      <div class="clock-display">
        <div class="clock-time">{{ currentTime.split(' ')[1] || currentTime }}</div>
        <div class="clock-date">{{ currentTime.split(' ')[0] || '' }}</div>
        <div class="clock-label">今日签到</div>
      </div>
    </div>

    <!-- 请假状态提示 -->
    <div v-if="isOnLeave" class="leave-banner">
      <el-icon :size="20"><InfoFilled /></el-icon>
      <span>你今天有已批准的请假，无需签到打卡</span>
    </div>

    <!-- 上班/下班签到卡 -->
    <div class="sign-cards">
      <el-row :gutter="16">
        <el-col :span="12" v-for="signRecord in todaySignData" :key="signRecord.type">
          <div class="apple-card sign-card" :class="getCardClass(signRecord)">
            <div class="card-header">
              <h3 class="card-title">{{ getSignTypeText(signRecord.type) }}</h3>
              <span class="card-state" :class="isOnLeave ? 'state-leave' : (signRecord.state === '已签到' ? 'state-done' : 'state-pending')">
                {{ getStateText(signRecord) }}
              </span>
            </div>

            <div class="card-body">
              <div class="info-row">
                <span class="info-label">标准时间</span>
                <span class="info-value">{{ getStandardTime(signRecord.type) }}</span>
              </div>
              <div class="info-row" v-if="signRecord.state === '已签到'">
                <span class="info-label">实际时间</span>
                <span class="info-value signed-time">{{ formatSignTime(signRecord.signDate) }}</span>
              </div>
              <div class="info-row" v-if="signRecord.sign_address">
                <span class="info-label">签到地点</span>
                <span class="info-value location-text">{{ signRecord.sign_address }}</span>
              </div>
            </div>

            <div class="card-actions">
              <button
                :class="signRecord.state === '已签到' ? 'apple-btn btn-signed' : 'apple-btn apple-btn-primary'"
                :disabled="signRecord.state === '已签到' || signRecord.disabled"
                @click="handleSign(signRecord)"
              >
                {{ getButtonText(signRecord) }}
              </button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 本月缺时 -->
    <div class="apple-card missing-card">
      <div class="info-row">
        <span class="info-label">本月缺时</span>
        <span class="info-value" :class="employeeMissingMinutes > 0 ? 'text-warning' : 'text-success'">
          {{ employeeMissingMinutes }} 分钟
        </span>
      </div>
    </div>

    <!-- 最近签到记录 -->
    <div class="apple-card history-section">
      <div class="section-header">
        <h3 class="section-title">最近签到记录</h3>
        <el-button @click="refreshData" :loading="loading" text>
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <el-table
        :data="historyData"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无签到记录"
        class="el-table--borderless"
      >
        <el-table-column label="日期" width="110">
          <template #default="{ row }">
            <span class="cell-text">{{ row.date }}</span>
          </template>
        </el-table-column>

        <el-table-column label="签到" width="110" align="center">
          <template #default="{ row }">
            <span class="cell-text">{{ formatTimeOnly(row.checkInTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="签退" width="110" align="center">
          <template #default="{ row }">
            <span class="cell-text">{{ formatTimeOnly(row.checkOutTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="'status-' + (row.todayStatus === '已签退' ? 'signed' : row.todayStatus === '签到异常' ? 'anomaly' : row.todayStatus === '已请假' ? 'leave' : 'missed')">
              {{ row.todayStatus || '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="签到地址" min-width="160">
          <template #default="{ row }">
            <span class="cell-text location-cell">{{ row.checkInAddress || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="签退地址" min-width="160">
          <template #default="{ row }">
            <span class="cell-text location-cell">{{ row.checkOutAddress || '--' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
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

    <!-- 补签申请按钮 -->
    <div class="retroactive-section">
      <el-button type="warning" @click="openRetroactiveDialog" :disabled="isOnLeave" size="large">
        申请补签
      </el-button>
      <span class="retroactive-hint">漏签了？可申请补签本周内的签到记录</span>
    </div>

    <!-- 补签申请对话框 -->
    <el-dialog v-model="showRetroactiveDialog" title="申请补签" width="420px" center>
      <el-form :model="retroactiveForm" label-width="85px">
        <el-form-item label="补签日期">
          <el-date-picker
            v-model="retroactiveForm.date"
            type="date"
            placeholder="选择补签日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
            :disabled-date="disableRetroDate"
          />
        </el-form-item>
        <el-form-item label="补签类型">
          <el-select v-model="retroactiveForm.type" style="width: 100%">
            <el-option label="上班签到 (上午)" value="a" />
            <el-option label="下班签退 (下午)" value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="补签原因">
          <el-input v-model="retroactiveForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRetroactiveDialog = false">取消</el-button>
        <el-button type="warning" @click="submitRetroactive" :loading="retroactiveSubmitting">提交申请</el-button>
      </template>
    </el-dialog>

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
          <el-icon size="48" color="#0071E3"><QuestionFilled /></el-icon>
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
            class="apple-btn apple-btn-primary"
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
  QuestionFilled,
  InfoFilled
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
const pageSize = ref(8)
const total = ref(0)
const isOnLeave = ref(false)  // 今天是否有已批准的请假
const employeeMissingMinutes = ref(0)

// 补签相关
const showRetroactiveDialog = ref(false)
const retroactiveSubmitting = ref(false)
const retroactiveForm = reactive({
  date: '',
  type: 'a',
  reason: ''
})

// 计算当周的周一和周日
const getWeekRange = () => {
  const now = new Date()
  const dayOfWeek = now.getDay() // 0=Sun, 1=Mon...
  const monday = new Date(now)
  monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1))
  monday.setHours(0,0,0,0)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  sunday.setHours(23,59,59,999)
  return { monday, sunday }
}

// 限制补签日期只能在当周
const disableRetroDate = (time: Date) => {
  const { monday, sunday } = getWeekRange()
  return time.getTime() < monday.getTime() || time.getTime() > sunday.getTime()
}

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

const currentYearMonth = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}
`
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

// 从 ISO 时间戳取 HH:mm
const formatTimeOnly = (dt: string): string => {
  if (!dt) return '--'
  return dt.substring(11, 16)
}

// TodayStatus 转中文
const formatTodayStatus = (status: string): string => {
  const map: Record<string, string> = {
    'NOT_CHECKED_IN': '未签到',
    'CHECKED_IN': '已签到',
    'CHECKED_OUT': '已签退',
    'LEAVE': '已请假',
    'ANOMALY': '签到异常',
  }
  return map[status] || status || '--'
}

// 获取签到类型文本
const getSignTypeText = (type: string): string => {
  return type === 'a' ? '上班签到' : '下班签退'
}

// 获取标准时间
const getStandardTime = (type: string): string => {
  return type === 'a' ? '09:00' : '18:00'
}

// 获取状态文本
const getStateText = (record: any): string => {
  if (isOnLeave.value) {
    return '已请假'
  }
  if (record.state === '已签到') {
    return record.type === 'a' ? '已签到' : '已签退'
  }
  if (record.disabled) {
    return '未签到'
  }
  return record.type === 'a' ? '未签到' : '未签退'
}

// 获取标签类型
const getTagType = (state: string): string => {
  return state === '已签到' ? 'success' : 'warning'
}

// 获取卡片样式类
const getCardClass = (record: any): string => {
  if (isOnLeave.value) return 'on-leave'
  return record.state === '已签到' ? 'signed' : 'unsigned'
}

// 获取按钮类型
const getButtonType = (record: any): string => {
  return record.state === '已签到' ? 'success' : 'primary'
}

// 获取按钮文本
const getButtonText = (record: any): string => {
  if (isOnLeave.value) {
    return '已请假'
  }
  if (record.state === '已签到') {
    return record.type === 'a' ? '已签到' : '已签退'
  }
  if (record.disabled) {
    return '先签到'
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
    // 上班签到：09:00之前正常，之后迟到
    const standardTime = 9 * 60 + 0 // 9:00
    if (currentTimeNum <= standardTime) {
      return '正常'
    } else {
      const lateMinutes = currentTimeNum - standardTime
      return `迟到 ${Math.floor(lateMinutes / 60)}小时${lateMinutes % 60}分钟`
    }
  } else {
    // 下班签退：18:00之前早退，之后正常
    const standardTime = 18 * 60 + 0 // 18:00
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
    const response = await axios.get('/api/v1/employee/attendance/today')

    if (response.data && response.data.code === 200) {
      const d = response.data.data || {}
      const today = new Date().toISOString().split('T')[0]
      const status = d.todayStatus || '未签到'
      const hasCheckedIn = ['已签到', '已签退', '签到异常'].includes(status)

      // 上班卡（签到）
      const morningRecord = {
        type: 'a',
        signDate: d.checkInTime || `${today} 09:00:00`,
        state: hasCheckedIn ? '已签到' : '未签到',
        number: 0,
        name: '',
        dept_name: '',
        sign_address: d.checkInAddress || (d.checkInTime ? `签到 ${d.checkInTime.substring(11, 16)}` : '')
      }

      // 下班卡（签退），只有已签到才启用
      const eveningRecord = {
        type: 'p',
        signDate: d.checkOutTime || `${today} 18:00:00`,
        state: status === 'CHECKED_OUT' ? '已签到' : (hasCheckedIn ? '未签到' : '--'),
        disabled: !hasCheckedIn,
        number: 0,
        name: '',
        dept_name: '',
        sign_address: d.checkOutAddress || (d.checkOutTime ? `签退 ${d.checkOutTime.substring(11, 16)}` : '')
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
  if (isOnLeave.value || record.state === '已签到') {
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
  if (signing.value) return
  signing.value = true

  try {
    let coordinates = currentCoordinates.value
    if (!coordinates) {
      try {
        coordinates = await getCurrentLocation()
        currentCoordinates.value = coordinates
      } catch { /* 忽略定位失败 */ }
    }

    const isCheckIn = signInfo.type === 'a'
    const url = `/api/v1/employee/attendance/${isCheckIn ? 'check-in' : 'check-out'}?coordinates=${encodeURIComponent(coordinates || '')}`

    const response = await axios.post(url)
    if (response.data && response.data.code === 200) {
      ElMessage.success(`${getSignTypeText(signInfo.type)}成功！`)
      showSignDialog.value = false
      await getTodaySignData()
      await getHistoryData()
    } else {
      ElMessage.error(response.data?.message || '操作失败，请重试')
    }
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error.message || '操作失败，请重试')
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

// 获取今天是否有已批准的请假
const getLeaveStatus = async () => {
  try {
    const response = await axios.get('/api/v1/employee/leave/today-status')
    if (response.data && response.data.code === 200) {
      isOnLeave.value = response.data.data === true
    }
  } catch (error) {
    console.warn('获取请假状态失败:', error)
    isOnLeave.value = false
  }
}

// 获取本月缺时时长
async function loadEmployeeMissingDuration() {
  try {
    const resp = await axios.get('/api/v1/employee/attendance/missing-duration', {
      params: { yearMonth: currentYearMonth.value }
    })
    if (resp.data?.data) {
      employeeMissingMinutes.value = resp.data.data.totalMinutes || 0
    }
  } catch (e) {
    console.warn('Failed to load missing duration:', e)
  }
}

// 打开补签对话框（默认选中今天）
const openRetroactiveDialog = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  retroactiveForm.date = `${y}-${m}-${d}`
  showRetroactiveDialog.value = true
}

// 提交补签申请
const submitRetroactive = async () => {
  if (!retroactiveForm.date) {
    ElMessage.warning('请选择补签日期')
    return
  }
  if (!retroactiveForm.reason.trim()) {
    ElMessage.warning('请填写补签原因')
    return
  }
  retroactiveSubmitting.value = true
  try {
    const response = await axios.post('/api/v1/employee/attendance/retroactive/apply', {
      signDate: retroactiveForm.date,
      type: retroactiveForm.type,
      reason: retroactiveForm.reason
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success('补签申请已提交，等待管理员审批')
      showRetroactiveDialog.value = false
      retroactiveForm.date = ''
      retroactiveForm.reason = ''
    } else {
      ElMessage.error(response.data?.message || '提交失败')
    }
  } catch (error) {
    console.error('提交补签失败:', error)
    ElMessage.error('网络错误')
  } finally {
    retroactiveSubmitting.value = false
  }
}

// 刷新数据
const refreshData = async () => {
  await Promise.all([
    getTodaySignData(),
    getHistoryData(),
    getLeaveStatus(),
    loadEmployeeMissingDuration()
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
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 32px;
}

/* ============ 顶部门时钟 ============ */
.clock-section {
  text-align: center;
  margin-bottom: 40px;
}

.clock-display {
  display: inline-block;
}

.clock-time {
  font-size: 72px;
  font-weight: 700;
  color: var(--apple-text);
  letter-spacing: 2px;
  line-height: 1;
  margin-bottom: 8px;
  font-feature-settings: "tnum";
}

.clock-date {
  font-size: 18px;
  color: var(--apple-text-secondary);
  margin-bottom: 4px;
}

.clock-label {
  font-size: 14px;
  color: var(--apple-text-tertiary);
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* ============ 请假提示横幅 ============ */
.leave-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #F3E8FF;
  color: #6B21A8;
  padding: 14px 20px;
  border-radius: var(--apple-radius, 12px);
  margin-bottom: 24px;
  font-size: 15px;
  font-weight: 500;
}

/* ============ 签到卡片 ============ */
.sign-cards {
  margin-bottom: 40px;
}

.sign-card {
  padding: 28px 24px 24px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--apple-text);
}

.card-state {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 980px;
}

.state-done {
  background: #E8F5E9;
  color: var(--apple-green);
}

.state-pending {
  background: #E3F2FD;
  color: var(--apple-blue);
}

.state-leave {
  background: #F3E8FF;
  color: #9333EA;
}

/* 卡片信息行 */
.card-body {
  flex: 1;
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--apple-bg-secondary);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: var(--apple-text-secondary);
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text);
}

.signed-time {
  color: var(--apple-green);
  font-weight: 600;
}

.location-text {
  font-size: 13px;
  color: var(--apple-text-secondary);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 按钮区域 */
.card-actions {
  text-align: center;
}

.card-actions .apple-btn {
  width: 100%;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  border-radius: var(--apple-radius-button);
  transition: all 0.2s ease;
}

.btn-signed {
  background: var(--apple-bg-secondary);
  color: var(--apple-text-tertiary);
  cursor: not-allowed;
  pointer-events: none;
}

.card-actions .apple-btn-primary {
  background: var(--apple-blue);
  color: white;
}

.card-actions .apple-btn-primary:hover {
  background: var(--apple-blue-hover);
}

.card-actions .apple-btn-primary:active {
  background: var(--apple-blue-active);
}

/* ============ 历史记录 ============ */
.history-section {
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--apple-text);
}

/* 精简表格 */
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
  padding: 8px 0;
}

:deep(.el-table--borderless .el-table__cell) {
  border-bottom: 1px solid var(--apple-bg-secondary);
}

:deep(.el-table--borderless .el-table__row:last-child .el-table__cell) {
  border-bottom: none;
}

:deep(.el-table--borderless td.el-table__cell) {
  padding: 12px 0;
  color: var(--apple-text);
  font-size: 14px;
}

.cell-text {
  color: var(--apple-text);
  font-size: 14px;
}

.location-cell {
  color: var(--apple-text-secondary);
  font-size: 13px;
}

/* 类型标记 */
.type-tag {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.type-am {
  background: #E3F2FD;
  color: var(--apple-blue);
}

.type-pm {
  background: #FFF3E0;
  color: var(--apple-orange);
}

/* 状态文字 — 已签退🟢 / 签到异常🟡 / 未签到🔴 / 已请假🟣 */
.state-signed {
  color: var(--apple-green);
  font-size: 13px;
  font-weight: 500;
}
.state-missed {
  color: var(--apple-text-tertiary);
  font-size: 13px;
}
.status-signed { color: #34C759; font-weight: 600; font-size: 13px; }
.status-anomaly { color: #FF9500; font-weight: 600; font-size: 13px; }
.status-missed { color: #FF3B30; font-weight: 600; font-size: 13px; }
.status-leave { color: #AF52DE; font-weight: 600; font-size: 13px; }

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* ============ 补签申请 ============ */
.retroactive-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  padding: 16px 20px;
  background: #fff8e1;
  border-radius: var(--apple-radius, 12px);
}

.retroactive-hint {
  font-size: 13px;
  color: var(--apple-text-secondary, #86868b);
}

/* ============ 对话框 ============ */
.dialog-content {
  text-align: center;
  padding: 20px 0;
}

.confirm-icon {
  margin-bottom: 16px;
}

.confirm-text {
  font-size: 18px;
  color: var(--apple-text);
  margin-bottom: 20px;
}

.time-display {
  background: var(--apple-bg);
  padding: 16px;
  border-radius: var(--apple-radius);
  margin-bottom: 16px;
}

.time-display p {
  margin: 8px 0;
  color: var(--apple-text-secondary);
}

.status-text.normal-status {
  color: var(--apple-green);
}

.status-text.late-status {
  color: var(--apple-red);
}

.status-text.early-status {
  color: var(--apple-orange);
}

.location-section {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-tertiary);
  font-size: 14px;
}

.location-section .location-icon {
  margin-right: 6px;
}

.location-success {
  color: var(--apple-green);
}

.location-error {
  color: var(--apple-red);
}

.dialog-footer {
  text-align: center;
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

.text-warning { color: #FF9500; }
.text-success { color: #34C759; }

.missing-card {
  padding: 12px 24px;
  margin-bottom: 24px;
}
</style> 