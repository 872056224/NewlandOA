<template>
  <div class="apple-page">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">我的工资</b>

    <div class="month-selector">
      <el-select v-model="selectedMonth" @change="fetchData" class="month-select">
        <el-option v-for="m in monthOptions" :key="m" :label="m" :value="m" />
      </el-select>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>

    <div v-else-if="errorMsg" class="empty-state">{{ errorMsg }}</div>

    <div v-else-if="detail" class="salary-card apple-card">
      <div class="card-header">
        <div>
          <div class="emp-name">{{ detail.empName }}</div>
          <div class="emp-dept">{{ detail.deptName }} · {{ detail.dutyName }}</div>
        </div>
        <div class="final-salary">
          <div class="salary-label">应发工资</div>
          <div class="salary-value">{{ detail.finalSalary?.toFixed(2) }}</div>
        </div>
      </div>

      <div class="detail-grid">
        <div class="detail-row">
          <span class="dl">基础月薪</span>
          <span class="dv">{{ detail.baseSalary?.toFixed(2) }}</span>
        </div>
        <div class="detail-row">
          <span class="dl">应出勤天数</span>
          <span class="dv">{{ detail.workDays }} 天</span>
        </div>
        <div class="detail-row">
          <span class="dl">日工资</span>
          <span class="dv">{{ detail.dailyWage?.toFixed(2) }}</span>
        </div>
        <div class="detail-row">
          <span class="dl">小时工资</span>
          <span class="dv">{{ detail.hourlyWage?.toFixed(2) }}</span>
        </div>
      </div>

      <div class="section-title">扣减项</div>
      <div class="detail-grid">
        <div class="detail-row">
          <span class="dl">缺时分钟数</span>
          <span class="dv">{{ detail.totalMissingMinutes }} 分钟</span>
        </div>
        <div class="detail-row">
          <span class="dl">缺时扣款</span>
          <span class="dv" style="color:#FF3B30">-{{ detail.missingDeduction?.toFixed(2) }}</span>
        </div>
        <div class="detail-row">
          <span class="dl">请假天数</span>
          <span class="dv">{{ detail.leaveDays }} 天</span>
        </div>
        <div class="detail-row">
          <span class="dl">请假扣款</span>
          <span class="dv" style="color:#FF3B30">-{{ detail.leaveDeduction?.toFixed(2) }}</span>
        </div>
      </div>

      <div class="section-title">加项</div>
      <div class="detail-grid">
        <div class="detail-row">
          <span class="dl">加班时长</span>
          <span class="dv">{{ detail.overtimeHours }} 小时（双倍）</span>
        </div>
        <div class="detail-row">
          <span class="dl">加班工资</span>
          <span class="dv" style="color:#34C759">+{{ detail.overtimePay?.toFixed(2) }}</span>
        </div>
      </div>

      <div class="total-row">
        <span>合计</span>
        <span class="total-value">{{ detail.finalSalary?.toFixed(2) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const selectedMonth = ref('2026-07')
const loading = ref(false)
const errorMsg = ref('')
const detail = ref<any>(null)

const monthOptions = computed(() => {
  const now = new Date()
  const options: string[] = []
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
})

const fetchData = async () => {
  loading.value = true
  errorMsg.value = ''
  detail.value = null
  try {
    const response = await axios.get(`/api/v1/employee/salary/my/${selectedMonth.value}`)
    if (response.data?.code === 200 && response.data?.data) {
      detail.value = response.data.data
    } else {
      errorMsg.value = response.data?.message || '暂未核算'
    }
  } catch { errorMsg.value = '获取失败' }
  finally { loading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.month-selector { margin-bottom: 20px; }
.month-select { width: 140px; }
.loading-state, .empty-state { padding: 60px; text-align: center; color: #86868b; font-size: 15px; }
.salary-card { padding: 28px 32px; max-width: 600px; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid #e5e5e7; }
.emp-name { font-size: 20px; font-weight: 700; color: #1d1d1f; }
.emp-dept { font-size: 13px; color: #86868b; margin-top: 4px; }
.final-salary { text-align: right; }
.salary-label { font-size: 13px; color: #86868b; }
.salary-value { font-size: 28px; font-weight: 700; color: #0071e3; }
.detail-grid { display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px; }
.detail-row { display: flex; justify-content: space-between; align-items: center; }
.dl { font-size: 14px; color: #86868b; }
.dv { font-size: 15px; color: #1d1d1f; font-weight: 500; }
.section-title { font-size: 14px; font-weight: 600; color: #1d1d1f; margin-bottom: 12px; padding-top: 8px; border-top: 1px solid #e5e5e7; }
.total-row { display: flex; justify-content: space-between; align-items: center; padding: 16px 0 0; margin-top: 16px; border-top: 2px solid #1d1d1f; font-size: 18px; font-weight: 700; color: #1d1d1f; }
.total-value { font-size: 24px; color: #0071e3; }
</style>
