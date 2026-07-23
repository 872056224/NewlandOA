<template>
  <div class="apple-page holiday-manage">
    <!-- Header -->
    <div class="header-row">
      <b class="apple-title" style="margin: 0;">节假日管理</b>
      <div class="header-actions">
        <el-select v-model="selectedYear" @change="fetchCalendar" class="year-select" size="large">
          <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
        </el-select>
        <el-button class="apple-btn" @click="handleBatchImport" :loading="importing">
          批量导入
        </el-button>
      </div>
    </div>

    <!-- Legend -->
    <div class="legend-bar">
      <div class="legend-item">
        <span class="legend-color" style="background: #34C759;"></span>
        <span>节假日</span>
      </div>
      <div class="legend-item">
        <span class="legend-color" style="background: #FF3B30;"></span>
        <span>休息日</span>
      </div>
      <div class="legend-item">
        <span class="legend-color" style="background: #0071E3;"></span>
        <span>工作日</span>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- Calendar Grid -->
    <div v-else class="calendar-grid">
      <div v-for="month in months" :key="month.index" class="month-card apple-card">
        <div class="month-header">{{ month.name }}</div>
        <div class="day-headers">
          <div v-for="d in dayNames" :key="d" class="day-header">{{ d }}</div>
        </div>
        <div class="days-grid">
          <div v-for="n in month.startOffset" :key="'empty-'+n" class="day-cell empty"></div>
          <div v-for="day in month.days" :key="day.date"
            class="day-cell" :class="day.typeClass"
            @click="showEditDialog(day)" :title="day.description || day.typeLabel">
            <span class="day-num">{{ day.day }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" title="修改日期类型" width="420px" :close-on-click-modal="false" destroy-on-close>
      <div class="dialog-body" v-if="selectedDay">
        <div class="dialog-date">{{ selectedDay.date }}</div>
        <div class="dialog-current">
          当前类型：<span class="type-badge" :class="selectedDay.typeClass">{{ selectedDay.typeLabel }}</span>
        </div>
        <div class="dialog-form">
          <div class="type-options">
            <label v-for="opt in typeOptions" :key="opt.value" class="type-option"
              :class="{ selected: editType === opt.value }" @click="editType = opt.value">
              <span class="option-color" :style="{ background: opt.color }"></span>
              <span class="option-label">{{ opt.label }}</span>
              <el-radio :value="opt.value" v-model="editType" class="option-radio" />
            </label>
          </div>
          <div class="description-input">
            <label>备注（可选）</label>
            <el-input v-model="editDescription" placeholder="例如：国庆节调休" maxlength="50" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button class="apple-btn" @click="dialogVisible = false">取消</el-button>
        <el-button class="apple-btn apple-btn-primary" @click="handleSave" :loading="saving">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'

interface HolidayDay {
  date: string
  day: number
  type: 'WORKDAY' | 'HOLIDAY' | 'REST_DAY'
  description?: string
  typeClass: string
  typeLabel: string
}

interface MonthData {
  index: number
  name: string
  startOffset: number
  days: HolidayDay[]
}

const MONTH_NAMES = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
const DAY_NAMES = ['一', '二', '三', '四', '五', '六', '日']

const TYPE_CLASS: Record<string, string> = {
  HOLIDAY: 'type-holiday',
  REST_DAY: 'type-rest',
  WORKDAY: 'type-workday'
}

const TYPE_LABEL: Record<string, string> = {
  HOLIDAY: '节假日',
  REST_DAY: '休息日',
  WORKDAY: '工作日'
}

const typeOptions = [
  { value: 'WORKDAY', label: '工作日', color: '#0071E3' },
  { value: 'HOLIDAY', label: '节假日', color: '#34C759' },
  { value: 'REST_DAY', label: '休息日', color: '#FF3B30' }
]

const selectedYear = ref(2026)
const loading = ref(false)
const importing = ref(false)
const saving = ref(false)
const calendarData = ref<Record<string, { type: string; description?: string }>>({})

const dialogVisible = ref(false)
const selectedDay = ref<HolidayDay | null>(null)
const editType = ref('WORKDAY')
const editDescription = ref('')

const dayNames = DAY_NAMES
const yearOptions = computed(() => {
  const years: number[] = []
  for (let y = 2020; y <= 2035; y++) years.push(y)
  return years
})

const months = computed<MonthData[]>(() => {
  const result: MonthData[] = []
  for (let m = 1; m <= 12; m++) {
    const year = selectedYear.value
    const firstDay = new Date(year, m - 1, 1)
    let startOffset = firstDay.getDay() - 1
    if (startOffset < 0) startOffset = 6
    const daysInMonth = new Date(year, m, 0).getDate()
    const days: HolidayDay[] = []
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
      const entry = calendarData.value[dateStr]
      const type = (entry?.type as 'WORKDAY' | 'HOLIDAY' | 'REST_DAY') || 'WORKDAY'
      days.push({
        date: dateStr,
        day: d,
        type,
        description: entry?.description || '',
        typeClass: TYPE_CLASS[type] || 'type-workday',
        typeLabel: TYPE_LABEL[type] || '工作日'
      })
    }
    result.push({ index: m, name: MONTH_NAMES[m - 1], startOffset, days })
  }
  return result
})

const fetchCalendar = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/api/v1/admin/holidays/calendar/${selectedYear.value}`)
    if (response.data) {
      const list = response.data.data || response.data
      if (Array.isArray(list)) {
        const map: Record<string, { type: string; description?: string }> = {}
        for (const item of list) {
          let dateStr: string
          if (typeof item.date === 'string') {
            dateStr = item.date
          } else if (Array.isArray(item.date)) {
            const [y, mo, d] = item.date
            dateStr = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
          } else continue
          map[dateStr] = { type: item.type || 'WORKDAY', description: item.description }
        }
        calendarData.value = map
      }
    }
  } catch (error) {
    console.error('获取日历数据失败:', error)
    ElMessage.error('获取日历数据失败')
    calendarData.value = {}
  } finally {
    loading.value = false
  }
}

const showEditDialog = (day: HolidayDay) => {
  selectedDay.value = day
  editType.value = day.type
  editDescription.value = day.description || ''
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!selectedDay.value) return
  saving.value = true
  try {
    await axios.put(`/api/v1/admin/holidays/${selectedDay.value.date}`, null, {
      params: { type: editType.value, description: editDescription.value || undefined }
    })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await fetchCalendar()
  } catch (error) {
    console.error('更新日期类型失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const handleBatchImport = async () => {
  try {
    await ElMessageBox.confirm(
      `将导入 ${selectedYear.value} 年缺失的节假日数据（周末→休息日，平日→工作日），是否继续？`,
      '批量导入',
      { confirmButtonText: '确定导入', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }

  importing.value = true
  try {
    const response = await axios.post(`/api/v1/admin/holidays/batch/${selectedYear.value}`)
    if (response.data) {
      ElMessage.success('导入成功')
      await fetchCalendar()
    } else {
      ElMessage.error('导入失败')
    }
  } catch (error) {
    console.error('批量导入失败:', error)
    ElMessage.error('批量导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(() => { fetchCalendar() })
</script>

<style scoped>
.holiday-manage { max-width: 1200px; }
.header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.year-select { width: 130px; }
.legend-bar { display: flex; gap: 24px; margin-bottom: 24px; padding: 12px 20px; background: #fff; border-radius: 12px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06); }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #1d1d1f; }
.legend-color { width: 16px; height: 16px; border-radius: 4px; display: inline-block; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; gap: 12px; color: #86868b; font-size: 15px; }
.calendar-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
@media (max-width: 1000px) { .calendar-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 680px) { .calendar-grid { grid-template-columns: 1fr; } }
.month-card { padding: 16px; cursor: default; }
.month-header { text-align: center; font-size: 16px; font-weight: 600; color: #1d1d1f; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #e5e5e7; }
.day-headers { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; margin-bottom: 4px; }
.day-header { text-align: center; font-size: 11px; font-weight: 500; color: #86868b; padding: 4px 0; }
.days-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; }
.day-cell { aspect-ratio: 1; display: flex; align-items: center; justify-content: center; border-radius: 6px; cursor: pointer; transition: all 0.15s ease; position: relative; }
.day-cell:hover { transform: scale(1.1); z-index: 2; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15); }
.day-cell.empty { cursor: default; pointer-events: none; }
.day-num { font-size: 12px; font-weight: 500; line-height: 1; }
.type-holiday { background: #34C759; color: #fff; }
.type-rest { background: #FF3B30; color: #fff; }
.type-workday { background: #0071E3; color: #fff; }
.day-cell .day-num { text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15); }

/* ── Dialog ── */
.dialog-body { padding: 8px 0; }
.dialog-date { font-size: 20px; font-weight: 700; color: #1d1d1f; margin-bottom: 12px; }
.dialog-current { font-size: 14px; color: #86868b; margin-bottom: 20px; }
.type-badge { display: inline-block; font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 10px; color: #fff; margin-left: 6px; }
.type-options { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.type-option { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 8px; border: 2px solid #e5e5e7; cursor: pointer; transition: all 0.15s; }
.type-option:hover { border-color: #0071e3; background: #f0f7ff; }
.type-option.selected { border-color: #0071e3; background: #f0f7ff; }
.option-color { width: 20px; height: 20px; border-radius: 6px; flex-shrink: 0; }
.option-label { flex: 1; font-size: 14px; font-weight: 500; color: #1d1d1f; }
.option-radio { flex-shrink: 0; }
.description-input { margin-bottom: 8px; }
.description-input label { display: block; font-size: 13px; color: #86868b; margin-bottom: 6px; }
</style>
