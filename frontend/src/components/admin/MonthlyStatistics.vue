<template>
  <div class="monthly-statistics">
    <!-- Page Header -->
    <div class="header-section">
      <h1 class="page-title">
        <el-icon class="title-icon"><DataAnalysis /></el-icon>
        考勤统计
      </h1>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" type="card" class="stats-tabs">
      <!-- ─── Tab 1: 近期统计（旧考勤统计的柱状图） ─── -->
      <el-tab-pane label="近期统计" name="recent">
        <div class="tab-content">
          <el-card class="apple-card" v-loading="barLoading">
            <template #header>
              <span class="card-title">近期统计（近4工作日）</span>
            </template>
            <div id="recentBarChart" ref="recentChartContainer" style="width:100%;height:400px"></div>
            <div v-if="!barLoading && barEmpty" class="chart-empty">
              <el-icon class="empty-icon"><DocumentRemove /></el-icon>
              <p>暂无数据</p>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ─── Tab 2: 概览（签到趋势 + 明细） ─── -->
      <el-tab-pane label="概览" name="overview">
        <div class="tab-content">
          <!-- Controls Row -->
          <div class="controls-row">
            <div class="control-group">
              <span class="control-label">月份</span>
              <el-date-picker
                v-model="trendMonth"
                type="month"
                placeholder="选择月份"
                value-format="YYYY-MM"
                class="apple-input month-picker"
                @change="onOverviewMonthChange"
              />
            </div>
            <el-button
              @click="refreshOverview"
              :loading="overviewLoading"
              class="apple-btn apple-btn-primary"
            >
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>

          <!-- Line Chart -->
          <el-card class="apple-card" v-loading="chartLoading" element-loading-text="加载图表中...">
            <template #header>
              <span class="card-title">{{ trendMonthLabel }} 签到趋势</span>
            </template>
            <div id="overviewChart" ref="chartContainer" style="width:100%;height:400px"></div>
            <div v-if="!chartLoading && chartEmpty" class="chart-empty">
              <el-icon class="empty-icon"><DocumentRemove /></el-icon>
              <p>暂无图表数据</p>
            </div>
          </el-card>

          <!-- Daily Stats Table -->
          <el-card class="apple-card table-card">
            <template #header>
              <div class="card-header-row">
                <span class="card-title">每日签到明细</span>
                <el-tag type="info" size="small">共 {{ dailyTotal }} 条记录</el-tag>
              </div>
            </template>
            <el-table
              :data="dailyStats"
              v-loading="dailyStatsLoading"
              stripe
              style="width: 100%"
              empty-text="暂无数据"
            >
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column prop="totalEmployees" label="总人数" width="70" align="center" />
              <el-table-column prop="onLeave" label="请假" width="70" align="center" />
              <el-table-column label="应签到" width="80" align="center">
                <template #default="{ row }">
                  {{ row.totalEmployees - row.onLeave }}
                </template>
              </el-table-column>
              <el-table-column prop="signed" label="已签到" width="70" align="center" />
              <el-table-column prop="unsigned" label="未签到" width="70" align="center" />
              <el-table-column prop="anomaly" label="打卡异常" width="80" align="center" />
              <el-table-column prop="missingDuration" label="缺时" width="60" align="center" />
              <el-table-column label="签到率" min-width="100" align="center">
                <template #default="{ row }">
                  <span :class="rateClass2(row.signed, row.totalEmployees - row.onLeave)">
                    {{ calcRate2(row.signed, row.totalEmployees - row.onLeave) }}%
                  </span>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrap" v-if="dailyTotal > 0">
              <el-pagination
                v-model:current-page="dailyPage"
                v-model:page-size="dailyPageSize"
                :total="dailyTotal"
                :page-sizes="[8, 16, 32]"
                layout="total, sizes, prev, pager, next"
                @size-change="loadDailyStats"
                @current-change="loadDailyStats"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ─── Tab 2: 月度统计 ─── -->
      <el-tab-pane label="月度统计" name="monthly">
        <div class="tab-content">
          <!-- Controls Row -->
          <div class="controls-row">
            <div class="control-group">
              <span class="control-label">年月</span>
              <el-date-picker
                v-model="monthlyYearMonth"
                type="month"
                placeholder="选择月份"
                value-format="YYYY-MM"
                class="apple-input month-picker"
              />
            </div>
            <div class="control-group">
              <span class="control-label">员工</span>
              <el-select
                v-model="monthlyEmpId"
                filterable
                clearable
                placeholder="选择员工（可搜索）"
                class="apple-input emp-select"
                @change="onEmpSelectChange"
              >
                <el-option
                  v-for="emp in employeeList"
                  :key="emp.number"
                  :label="`${emp.name} (${emp.number})`"
                  :value="emp.number"
                />
              </el-select>
            </div>
            <el-button
              @click="loadMonthlyStats"
              :loading="monthlyLoading"
              class="apple-btn apple-btn-primary"
            >
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button
              @click="generateMonthlyReport"
              :loading="generating"
              class="apple-btn"
              type="warning"
            >
              <el-icon><Files /></el-icon>
              生成报表
            </el-button>
          </div>

          <!-- Monthly Stats Table -->
          <el-card class="apple-card table-card">
            <template #header>
              <div class="card-header-row">
                <span class="card-title">个人月度考勤</span>
                <el-tag v-if="monthlyData" type="info" size="small">
                  {{ monthlyData.empName || monthlyData.empId || '' }}
                </el-tag>
              </div>
            </template>
            <div v-if="!monthlyData" class="empty-hint">
              <el-icon><InfoFilled /></el-icon>
              <span>请选择员工和月份后点击"查询"</span>
            </div>
            <div v-else>
              <!-- Personal Info Header -->
              <div class="personal-summary-header">
                <el-descriptions :column="4" border size="small">
                  <el-descriptions-item label="姓名">{{ monthlyData.empName || '--' }}</el-descriptions-item>
                  <el-descriptions-item label="员工编号">{{ monthlyData.empId || '--' }}</el-descriptions-item>
                  <el-descriptions-item label="年份月份">{{ monthlyData.yearMonth || monthlyYearMonth }}</el-descriptions-item>
                  <el-descriptions-item label="出勤率">
                    <el-tag :type="attendanceRateType(monthlyData.attendanceRate)" size="small">
                      {{ monthlyData.attendanceRate != null ? monthlyData.attendanceRate + '%' : '--' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
              <!-- Daily Detail Table -->
              <el-table
                :data="monthlyDailyRecords"
                v-loading="monthlyLoading"
                stripe
                style="width: 100%; margin-top: 16px;"
                empty-text="暂无每日记录"
              >
                <el-table-column prop="date" label="日期" width="120" />
                <el-table-column label="签到时间" width="120">
                  <template #default="{ row }">
                    {{ formatTime(row.checkIn) }}
                  </template>
                </el-table-column>
                <el-table-column label="签退时间" width="120">
                  <template #default="{ row }">
                    {{ formatTime(row.checkOut) }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag
                      :type="row.status === '已签到' ? 'success' : row.status === '异常' ? 'warning' : 'danger'"
                      size="small"
                    >
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="missingDuration" label="缺时(分钟)" width="100" align="center">
                  <template #default="{ row }">
                    <span :class="row.missingDuration > 0 ? 'color-danger' : 'color-success'">
                      {{ row.missingDuration != null ? row.missingDuration : 0 }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
              <!-- Summary Row -->
              <div class="monthly-summary-bar" v-if="monthlyDailyRecords.length > 0">
                <div class="summary-stat">
                  <span class="summary-stat-label">工作日</span>
                  <span class="summary-stat-value">{{ monthlyData.workDays ?? '--' }}</span>
                </div>
                <div class="summary-stat">
                  <span class="summary-stat-label">出勤天数</span>
                  <span class="summary-stat-value">{{ monthlyData.actualDays ?? '--' }}</span>
                </div>
                <div class="summary-stat">
                  <span class="summary-stat-label">累计缺时(分钟)</span>
                  <span class="summary-stat-value">{{ monthlyData.missingDuration ?? 0 }}</span>
                </div>
                <div class="summary-stat">
                  <span class="summary-stat-label">出勤率</span>
                  <span class="summary-stat-value" :class="rateColor(monthlyData.attendanceRate)">
                    {{ monthlyData.attendanceRate != null ? monthlyData.attendanceRate + '%' : '--' }}
                  </span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ─── Tab 3: 部门统计 ─── -->
      <el-tab-pane label="部门统计" name="department">
        <div class="tab-content">
          <!-- Controls Row -->
          <div class="controls-row">
            <div class="control-group">
              <span class="control-label">年月</span>
              <el-date-picker
                v-model="deptYearMonth"
                type="month"
                placeholder="选择月份"
                value-format="YYYY-MM"
                class="apple-input month-picker"
              />
            </div>
            <div class="control-group">
              <span class="control-label">部门</span>
              <el-select
                v-model="deptId"
                filterable
                placeholder="选择部门"
                class="apple-input emp-select"
              >
                <el-option
                  v-for="dept in departmentList"
                  :key="dept.dept_id"
                  :label="dept.dept_name"
                  :value="dept.dept_id"
                />
              </el-select>
            </div>
            <el-button
              @click="loadDeptStats"
              :loading="deptLoading"
              class="apple-btn apple-btn-primary"
            >
              <el-icon><Search /></el-icon>
              查询
            </el-button>
          </div>

          <!-- Department Stats Table -->
          <el-card class="apple-card table-card">
            <template #header>
              <div class="card-header-row">
                <span class="card-title">部门月度考勤汇总</span>
                <el-tag v-if="deptStatsData" type="info" size="small">
                  {{ deptStatsData.yearMonth }}
                </el-tag>
              </div>
            </template>
            <div v-if="!deptStatsData" class="empty-hint">
              <el-icon><InfoFilled /></el-icon>
              <span>请选择部门和月份后点击"查询"</span>
            </div>
            <template v-else>
              <!-- Summary Card -->
              <div class="dept-summary-grid">
                <div class="summary-item">
                  <div class="summary-value">{{ deptStatsData.totalEmployees }}</div>
                  <div class="summary-label">总人数</div>
                </div>
                <div class="summary-item">
                  <div class="summary-value" :class="rateColor(deptStatsData.attendanceRate)">
                    {{ deptStatsData.attendanceRate != null ? deptStatsData.attendanceRate + '%' : '--' }}
                  </div>
                  <div class="summary-label">平均出勤率</div>
                </div>
                <div class="summary-item">
                  <div class="summary-value">{{ deptStatsData.totalWorkDays || 0 }}</div>
                  <div class="summary-label">总应出勤</div>
                </div>
                <div class="summary-item">
                  <div class="summary-value">{{ deptStatsData.totalActualDays || 0 }}</div>
                  <div class="summary-label">总实际出勤</div>
                </div>
              </div>

              <!-- Pie Chart -->
              <div id="deptPieChart" ref="deptChartContainer" style="width:100%;height:350px;margin-top:8px;"></div>
              <div v-if="deptLoading" class="chart-empty">
                <p>加载中...</p>
              </div>
            </template>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis, Refresh, Search, Files, InfoFilled, DocumentRemove
} from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

// ─── Reactive State ───

const activeTab = ref('recent')

// Overview tab
const trendMonth = ref(formatMonth(new Date()))
const trendMonthLabel = computed(() => {
  const [year, month] = trendMonth.value.split('-')
  return `${year}年${month}月`
})
const overviewLoading = ref(false)
const chartLoading = ref(false)
const chartEmpty = ref(false)
const chartContainer = ref<HTMLElement>()
let myChart: echarts.ECharts | null = null

// Recent bar chart (left side)
const recentChartContainer = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
const barLoading = ref(false)
const barEmpty = ref(false)

const dailyStats = ref<any[]>([])
const dailyStatsLoading = ref(false)
const dailyTotal = ref(0)
const dailyPage = ref(1)
const dailyPageSize = ref(8)

// Monthly tab
const monthlyYearMonth = ref(formatMonth(new Date()))
const monthlyEmpId = ref<number | null>(null)
const monthlyLoading = ref(false)
const generating = ref(false)
const monthlyData = ref<any>(null)
const monthlyDailyRecords = ref<any[]>([])
const employeeList = ref<any[]>([])

// Department tab
const deptYearMonth = ref(formatMonth(new Date()))
const deptId = ref<number | null>(null)
const deptLoading = ref(false)
const deptStatsData = ref<any>(null)
const departmentList = ref<any[]>([])

// Department pie chart
const deptChartContainer = ref<HTMLElement>()
let deptPieChart: echarts.ECharts | null = null

// ─── Helpers ───

function formatMonth(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

function calcRate2(signed: number, expected: number): string {
  if (expected <= 0) return '0.00'
  return ((signed / expected) * 100).toFixed(2)
}

function rateClass2(signed: number, expected: number): string {
  if (expected <= 0) return 'rate-low'
  const rate = parseFloat(calcRate2(signed, expected))
  if (rate >= 90) return 'rate-high'
  if (rate >= 70) return 'rate-mid'
  return 'rate-low'
}

function formatTime(isoStr: string | null): string {
  if (!isoStr) return '--'
  const parts = isoStr.split('T')
  return parts.length > 1 ? parts[1].substring(0, 5) : isoStr
}

function attendanceRateType(rate: any): string {
  if (rate == null) return 'info'
  const num = typeof rate === 'number' ? rate : parseFloat(rate)
  if (num >= 80) return 'success'
  if (num >= 60) return 'warning'
  return 'danger'
}

function rateColor(rate: any): string {
  if (rate == null) return ''
  const num = typeof rate === 'number' ? rate : parseFloat(rate)
  if (num >= 80) return 'color-success'
  if (num >= 60) return 'color-warning'
  return 'color-danger'
}

// ─── Chart ───

function initChart() {
  if (!chartContainer.value) return
  myChart = echarts.init(chartContainer.value)

  const defaultOption = {
    title: {
      text: '',
      textStyle: { color: '#1D1D1F', fontSize: 16 }
    },
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%', right: '4%', bottom: '3%', containLabel: true
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: '#86868B' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#86868B' }
    },
    series: [
      {
        name: '已签到',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#0071E3', width: 2 },
        itemStyle: { color: '#0071E3' },
        symbol: 'circle',
        symbolSize: 6,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 113, 227, 0.2)' },
            { offset: 1, color: 'rgba(0, 113, 227, 0.02)' }
          ])
        }
      }
    ]
  }

  myChart.setOption(defaultOption)
  window.addEventListener('resize', () => myChart?.resize())
}

async function loadMonthlyTrend() {
  chartLoading.value = true
  chartEmpty.value = false
  try {
    const response = await axios.get('/api/v1/admin/statistics/monthly/trend', {
      params: { yearMonth: trendMonth.value }
    })
    if (response.data && response.data.data) {
      const { dates, signed } = response.data.data
      if (!dates || dates.length === 0) {
        chartEmpty.value = true
        return
      }
      myChart?.setOption({
        title: {
          text: `${trendMonthLabel.value} 签到趋势（工作日）`
        },
        xAxis: { data: dates },
        series: [{ data: signed }]
      })
      chartEmpty.value = false
    } else {
      chartEmpty.value = true
    }
  } catch (error) {
    console.error('加载趋势图数据失败:', error)
    chartEmpty.value = true
  } finally {
    chartLoading.value = false
  }
}

// ─── Recent Bar Chart (近期统计) ───

function initBarChart() {
  if (!recentChartContainer.value) return
  barChart = echarts.init(recentChartContainer.value)
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['已签到', '未签到', '请假'], textStyle: { color: '#86868B' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: [], axisLabel: { color: '#86868B' } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#86868B' } },
    series: [
      { name: '已签到', type: 'bar', data: [], itemStyle: { color: '#34C759' } },
      { name: '未签到', type: 'bar', data: [], itemStyle: { color: '#FF9500' } },
      { name: '请假', type: 'bar', data: [], itemStyle: { color: '#AF52DE' } }
    ]
  }
  barChart.setOption(option)
  window.addEventListener('resize', () => barChart?.resize())
}

function initDeptPieChart() {
  if (!deptChartContainer.value) return
  deptPieChart = echarts.init(deptChartContainer.value)
  const option = {
    title: { text: '出勤率分布', left: 'center', textStyle: { color: '#1D1D1F', fontSize: 16 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#86868B' } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}\n{c}人', color: '#1D1D1F' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: []
    }]
  }
  deptPieChart.setOption(option)
  window.addEventListener('resize', () => deptPieChart?.resize())
}

function updateDeptPieChart() {
  if (!deptPieChart || !deptStatsData.value) return
  const details = deptStatsData.value.details || []
  // Categorize by attendance rate
  let excellent = 0, good = 0, fair = 0, poor = 0
  details.forEach((r: any) => {
    const rate = r.attendanceRate != null ? parseFloat(r.attendanceRate) : 0
    if (rate >= 90) excellent++
    else if (rate >= 80) good++
    else if (rate >= 60) fair++
    else poor++
  })
  deptPieChart.setOption({
    series: [{
      data: [
        { value: excellent, name: '优秀(>=90%)', itemStyle: { color: '#34C759' } },
        { value: good, name: '良好(>=80%)', itemStyle: { color: '#0071E3' } },
        { value: fair, name: '合格(>=60%)', itemStyle: { color: '#FF9500' } },
        { value: poor, name: '不合格(<60%)', itemStyle: { color: '#FF3B30' } }
      ]
    }]
  })
}

async function loadRecentStats() {
  barLoading.value = true
  barEmpty.value = false
  try {
    const response = await axios.get('/api/v1/admin/attendance/statistics/chart')
    if (response.data) {
      const dates = response.data.data || []
      const signed = response.data.data1 || []
      const unsigned = response.data.data2 || []
      const leave = response.data.data3 || []
      if (!dates.length) { barEmpty.value = true; return }
      barChart?.setOption({
        xAxis: { data: dates },
        series: [
          { data: signed },
          { data: unsigned },
          { data: leave }
        ]
      })
    }
  } catch (error) {
    console.error('加载近期统计失败:', error)
    barEmpty.value = true
  } finally {
    barLoading.value = false
  }
}

// ─── Daily Stats ───

async function loadDailyStats() {
  dailyStatsLoading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance/daily-statistics', {
      params: {
        currentPage: 1,
        pageSize: 100
      }
    })
    if (response.data && response.data.data) {
      let allStats = response.data.data || []
      // Filter by selected month for overview
      if (trendMonth.value) {
        allStats = allStats.filter((d: any) => d.date && d.date.startsWith(trendMonth.value))
      }
      dailyStats.value = allStats
      dailyTotal.value = allStats.length
    } else {
      dailyStats.value = []
      dailyTotal.value = 0
    }
  } catch (error) {
    console.error('加载每日统计失败:', error)
    ElMessage.error('获取每日统计失败')
    dailyStats.value = []
  } finally {
    dailyStatsLoading.value = false
  }
}

// ─── Employees ───

async function loadEmployees() {
  try {
    const response = await axios.get('/api/v1/admin/employees', {
      params: { currentPage: 1, pageSize: 999 }
    })
    if (response.data && response.data.data) {
      employeeList.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
  }
}

// ─── Departments ───

async function loadDepartments() {
  try {
    const response = await axios.get('/api/v1/admin/employees/departments')
    if (response.data && response.data.data) {
      departmentList.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

// ─── Monthly Stats ───

async function loadMonthlyStats() {
  if (!monthlyEmpId.value) {
    ElMessage.warning('请选择员工')
    return
  }
  monthlyLoading.value = true
  try {
    const response = await axios.get('/api/v1/admin/statistics/personal/monthly-detail', {
      params: {
        empId: monthlyEmpId.value,
        yearMonth: monthlyYearMonth.value
      }
    })
    if (response.data && response.data.data) {
      monthlyData.value = response.data.data.summary
      monthlyDailyRecords.value = response.data.data.dailyRecords || []
    } else if (response.data && response.data.code === 500) {
      ElMessage.warning(response.data.message || '未找到该员工考勤数据')
      monthlyData.value = null
      monthlyDailyRecords.value = []
    } else {
      ElMessage.warning('未找到该员工考勤数据')
      monthlyData.value = null
      monthlyDailyRecords.value = []
    }
  } catch (error) {
    console.error('加载月度统计失败:', error)
    ElMessage.error('获取月度统计失败')
    monthlyData.value = null
    monthlyDailyRecords.value = []
  } finally {
    monthlyLoading.value = false
  }
}

async function generateMonthlyReport() {
  generating.value = true
  try {
    const ym = monthlyYearMonth.value || formatMonth(new Date())
    const [year, month] = ym.split('-').map(Number)
    const response = await axios.post('/api/v1/admin/statistics/monthly/generate', null, {
      params: { year, month }
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success(response.data.data || '报表生成完成')
      // Refresh stats
      await loadMonthlyStats()
    } else {
      ElMessage.warning(response.data?.message || '生成报表失败')
    }
  } catch (error: any) {
    console.error('生成报表失败:', error)
    ElMessage.error(error.response?.data?.message || '生成报表失败')
  } finally {
    generating.value = false
  }
}

function onEmpSelectChange(val: number | null) {
  if (val != null) {
    loadMonthlyStats()
  }
}

function onOverviewMonthChange() {
  loadMonthlyTrend()
  loadDailyStats()
}

// ─── Department Stats ───

async function loadDeptStats() {
  if (!deptId.value) {
    ElMessage.warning('请选择部门')
    return
  }
  deptLoading.value = true
  try {
    const response = await axios.get('/api/v1/admin/statistics/department/monthly', {
      params: {
        deptId: deptId.value,
        yearMonth: deptYearMonth.value
      }
    })
    if (response.data && response.data.data) {
      deptStatsData.value = response.data.data
      nextTick(() => {
        if (!deptPieChart) initDeptPieChart()
        updateDeptPieChart()
      })
    } else if (response.data && response.data.code === 500) {
      ElMessage.warning(response.data.message || '未找到该部门考勤数据')
      deptStatsData.value = null
    } else {
      ElMessage.warning('未找到该部门考勤数据')
      deptStatsData.value = null
    }
  } catch (error) {
    console.error('加载部门统计失败:', error)
    ElMessage.error('获取部门统计失败')
    deptStatsData.value = null
  } finally {
    deptLoading.value = false
  }
}

// ─── Refresh Overview ───

async function refreshOverview() {
  overviewLoading.value = true
  await Promise.all([loadMonthlyTrend(), loadRecentStats(), loadDailyStats()])
  overviewLoading.value = false
}

// ─── Lifecycle ───

onMounted(async () => {
  await nextTick()
  initChart()
  initBarChart()
  initDeptPieChart()
  loadMonthlyTrend()
  loadRecentStats()
  loadDailyStats()
  loadEmployees()
  loadDepartments()
})

// 切换标签页时初始化对应图表
watch(activeTab, (tab) => {
  nextTick(() => {
    if (tab === 'recent' && barChart) { barChart.resize() }
    if (tab === 'overview' && myChart) { myChart.resize() }
    if (tab === 'department' && deptPieChart) { deptPieChart.resize() }
  })
})
</script>

<style scoped>
.monthly-statistics {
  padding: 32px;
  background: var(--apple-bg);
  min-height: 100vh;
}

/* ─── Header ─── */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  display: flex;
  align-items: center;
  font-size: 24px;
  font-weight: 600;
  color: var(--apple-text);
  margin: 0;
}

.title-icon {
  margin-right: 10px;
  color: var(--apple-blue);
  font-size: 26px;
}

/* ─── Tabs ─── */
.stats-tabs {
  background: transparent;
}

.stats-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
  border-bottom: none;
}

.stats-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  border: 1px solid var(--apple-border);
  border-radius: 8px 8px 0 0;
  padding: 10px 24px;
  background: var(--apple-bg-secondary);
  margin-right: 4px;
  transition: all 0.2s ease;
}

.stats-tabs :deep(.el-tabs__item:hover) {
  color: var(--apple-blue);
}

.stats-tabs :deep(.el-tabs__item.is-active) {
  color: var(--apple-blue);
  background: var(--apple-white);
  border-bottom-color: var(--apple-white);
  font-weight: 600;
}

.stats-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ─── Controls ─── */
.controls-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  background: var(--apple-white);
  padding: 16px 20px;
  border-radius: var(--apple-radius-card);
  box-shadow: var(--apple-shadow);
}

.control-group,
.date-range-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  white-space: nowrap;
}

.month-picker {
  width: 160px;
}

.emp-select {
  width: 220px;
}

/* ─── Cards ─── */
.chart-card,
.table-card {
  border-radius: var(--apple-radius-card);
  box-shadow: var(--apple-shadow);
  border: none;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text);
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-container {
  width: 100%;
  height: 400px;
  min-height: 300px;
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--apple-text-secondary);
}

.chart-empty .empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  color: var(--apple-text-tertiary);
}

.chart-empty p {
  font-size: 15px;
  margin: 0;
}

/* ─── Rate Colors ─── */
.rate-high {
  color: var(--apple-green);
  font-weight: 600;
}

.rate-mid {
  color: var(--apple-orange);
  font-weight: 600;
}

.rate-low {
  color: var(--apple-red);
  font-weight: 600;
}

.color-success {
  color: var(--apple-green);
  font-weight: 600;
}

.color-warning {
  color: var(--apple-orange);
  font-weight: 600;
}

.color-danger {
  color: var(--apple-red);
  font-weight: 600;
}

/* ─── Empty Hint ─── */
.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  color: var(--apple-text-secondary);
  font-size: 15px;
}

/* ─── Personal Monthly Summary ─── */
.personal-summary-header {
  margin-bottom: 8px;
}

.personal-summary-header :deep(.el-descriptions__title) {
  font-size: 14px;
}

.monthly-summary-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  background: var(--apple-bg);
  border-radius: var(--apple-radius);
}

.summary-stat {
  text-align: center;
}

.summary-stat-label {
  display: block;
  font-size: 12px;
  color: var(--apple-text-secondary);
  margin-bottom: 4px;
}

.summary-stat-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  color: var(--apple-text);
}

/* ─── Department Summary Grid ─── */
.dept-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 8px;
}

.summary-item {
  text-align: center;
  padding: 20px 12px;
  background: var(--apple-bg);
  border-radius: var(--apple-radius);
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text);
  line-height: 1.2;
  margin-bottom: 4px;
}

.summary-label {
  font-size: 13px;
  color: var(--apple-text-secondary);
}

/* ─── Pagination ─── */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ─── Table Overrides ─── */
:deep(.el-table th.el-table__cell) {
  background-color: var(--apple-bg) !important;
  color: var(--apple-text-secondary);
  font-weight: 600;
  font-size: 13px;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #f0f7ff !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background-color: var(--apple-bg);
}

/* ─── Responsive ─── */
@media (max-width: 768px) {
  .controls-row {
    flex-direction: column;
    align-items: stretch;
  }

  .control-group,
  .date-range-picker {
    flex-direction: column;
    align-items: stretch;
  }

  .month-picker,
  .emp-select {
    width: 100%;
  }

  .dept-summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
