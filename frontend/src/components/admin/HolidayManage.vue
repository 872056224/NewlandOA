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
          <!-- Empty cells before first day -->
          <div
            v-for="n in month.startOffset"
            :key="'empty-' + n"
            class="day-cell empty"
          ></div>
          <!-- Actual days -->
          <div
            v-for="day in month.days"
            :key="day.date"
            class="day-cell"
            :class="day.typeClass"
            @click="handleDayClick(day)"
            :title="day.description || day.typeLabel"
          >
            <span class="day-num">{{ day.day }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'

// ── Types ──
interface HolidayDay {
  date: string       // yyyy-MM-dd
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

// ── Constants ──
const MONTH_NAMES = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
const DAY_NAMES = ['一', '二', '三', '四', '五', '六', '日']

const TYPE_ORDER: Array<'WORKDAY' | 'HOLIDAY' | 'REST_DAY'> = ['WORKDAY', 'HOLIDAY', 'REST_DAY']

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

// ── State ──
const selectedYear = ref(2026)
const loading = ref(false)
const importing = ref(false)
const calendarData = ref<Record<string, { type: string; description?: string }>>({})

const dayNames = DAY_NAMES
const yearOptions = computed(() => {
  const years: number[] = []
  for (let y = 2020; y <= 2035; y++) years.push(y)
  return years
})

// ── Computed: Build month grids from flat calendar data ──
const months = computed<MonthData[]>(() => {
  const result: MonthData[] = []

  for (let m = 1; m <= 12; m++) {
    const year = selectedYear.value
    const firstDay = new Date(year, m - 1, 1)
    // getDay() returns 0=Sun, 1=Mon... We want Mon=0, Sun=6
    let startOffset = firstDay.getDay() - 1
    if (startOffset < 0) startOffset = 6 // Sunday

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

    result.push({
      index: m,
      name: MONTH_NAMES[m - 1],
      startOffset,
      days
    })
  }

  return result
})

// ── Fetch full-year calendar ──
const fetchCalendar = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/api/v1/admin/holidays/calendar/${selectedYear.value}`)
    if (response.data) {
      // RESP.ok(list) -> response.data: { data: [...], code: 200 }
      const list = response.data.data || response.data
      if (Array.isArray(list)) {
        const map: Record<string, { type: string; description?: string }> = {}
        for (const item of list) {
          // item.date could be a string "2026-01-01" or an array [2026,1,1] from JSON serialization of LocalDate
          let dateStr: string
          if (typeof item.date === 'string') {
            dateStr = item.date
          } else if (Array.isArray(item.date)) {
            const [y, mo, d] = item.date
            dateStr = `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')}`
          } else {
            continue
          }
          map[dateStr] = {
            type: item.type || 'WORKDAY',
            description: item.description
          }
        }
        calendarData.value = map
      }
    } else {
      ElMessage.error('获取日历数据失败')
    }
  } catch (error) {
    console.error('获取日历数据失败:', error)
    ElMessage.error('获取日历数据失败')
    calendarData.value = {}
  } finally {
    loading.value = false
  }
}

// ── Click day: toggle type ──
const handleDayClick = async (day: HolidayDay) => {
  const currentIdx = TYPE_ORDER.indexOf(day.type)
  const nextType = TYPE_ORDER[(currentIdx + 1) % TYPE_ORDER.length]

  try {
    await axios.put(`/api/v1/admin/holidays/${day.date}`, null, {
      params: { type: nextType }
    })
    // Update local state
    if (calendarData.value[day.date]) {
      calendarData.value[day.date].type = nextType
    } else {
      calendarData.value[day.date] = { type: nextType }
    }
  } catch (error) {
    console.error('更新日期类型失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
}

// ── Batch import ──
const handleBatchImport = async () => {
  try {
    await ElMessageBox.confirm(
      `将导入 ${selectedYear.value} 年缺失的节假日数据（周末自动标记为休息日，平日自动标记为工作日），是否继续？`,
      '批量导入',
      { confirmButtonText: '确定导入', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

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

// ── Init ──
onMounted(() => {
  fetchCalendar()
})
</script>

<style scoped>
.holiday-manage {
  max-width: 1200px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.year-select {
  width: 130px;
}

/* ── Legend ── */
.legend-bar {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1d1d1f;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  display: inline-block;
}

/* ── Loading ── */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
  color: #86868b;
  font-size: 15px;
}

/* ── Calendar Grid ── */
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 1000px) {
  .calendar-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 680px) {
  .calendar-grid {
    grid-template-columns: 1fr;
  }
}

.month-card {
  padding: 16px;
  cursor: default;
}

.month-header {
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e5e7;
}

/* ── Day headers (Mon-Sun) ── */
.day-headers {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 4px;
}

.day-header {
  text-align: center;
  font-size: 11px;
  font-weight: 500;
  color: #86868b;
  padding: 4px 0;
}

/* ── Days grid ── */
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.day-cell:hover {
  transform: scale(1.1);
  z-index: 2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.day-cell.empty {
  cursor: default;
  pointer-events: none;
}

.day-num {
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

/* ── Type colors ── */
.type-holiday {
  background: #34C759;
  color: #fff;
}

.type-rest {
  background: #FF3B30;
  color: #fff;
}

.type-workday {
  background: #0071E3;
  color: #fff;
}

/* Ensure readability */
.day-cell .day-num {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}
</style>
