<template>
  <div class="apple-page">
    <div class="header-row">
      <b class="apple-title" style="margin: 0;">考勤规则设置</b>
    </div>

    <div v-if="loading" class="loading-container">加载中...</div>

    <div v-else-if="rule" class="apple-card rule-card">
      <div class="rule-header">
        <span class="rule-badge">全局通用</span>
        <span class="rule-name">{{ rule.ruleName }}</span>
      </div>

      <div class="rule-details">
        <div class="detail-item">
          <span class="detail-label">上班时间</span>
          <span class="detail-value">
            <el-time-picker v-model="editWorkStart" format="HH:mm" value-format="HH:mm" style="width: 120px" />
          </span>
        </div>
        <div class="detail-item">
          <span class="detail-label">下班时间</span>
          <span class="detail-value">
            <el-time-picker v-model="editWorkEnd" format="HH:mm" value-format="HH:mm" style="width: 120px" />
          </span>
        </div>
        <div class="detail-item">
          <span class="detail-label">缺时宽限</span>
          <span class="detail-value">
            <el-input-number v-model="editTolerance" :min="0" :max="120" style="width: 120px" /> 分钟
          </span>
        </div>
      </div>

      <div class="rule-actions">
        <el-button class="apple-btn apple-btn-primary" @click="handleSave" :loading="saving">保存修改</el-button>
        <el-button class="apple-btn" @click="resetForm">重置</el-button>
      </div>
    </div>

    <div v-else class="empty-card apple-card">
      <p>未配置考勤规则，请先创建</p>
      <el-button class="apple-btn apple-btn-primary" @click="showCreate" :loading="saving">创建默认规则</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const saving = ref(false)
const rule = ref<any>(null)
const editWorkStart = ref('09:00')
const editWorkEnd = ref('18:00')
const editTolerance = ref(30)

const fetchRule = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance-rules/default')
    if (response.data && response.data.data) {
      rule.value = response.data.data
      editWorkStart.value = formatTimeBack(response.data.data.workStartTime)
      editWorkEnd.value = formatTimeBack(response.data.data.workEndTime)
      editTolerance.value = response.data.data.missingToleranceMin ?? 30
    } else {
      rule.value = null
    }
  } catch { rule.value = null }
  finally { loading.value = false }
}

function formatTimeBack(time: any): string {
  if (!time) return '09:00'
  if (typeof time === 'string') return time.slice(0, 5)
  if (Array.isArray(time)) return `${String(time[0]).padStart(2, '0')}:${String(time[1]).padStart(2, '0')}`
  return '09:00'
}

function timeToArray(timeStr: string): number[] {
  return timeStr.split(':').map(Number)
}

const handleSave = async () => {
  if (!rule.value) return
  saving.value = true
  try {
    await axios.put(`/api/v1/admin/attendance-rules/${rule.value.id}`, {
      ruleName: rule.value.ruleName,
      deptId: null,
      workStartTime: timeToArray(editWorkStart.value),
      workEndTime: timeToArray(editWorkEnd.value),
      missingToleranceMin: editTolerance.value,
      enabled: true
    })
    ElMessage.success('保存成功')
    await fetchRule()
  } catch {
    ElMessage.error('保存失败')
  } finally { saving.value = false }
}

const resetForm = () => {
  if (rule.value) {
    editWorkStart.value = formatTimeBack(rule.value.workStartTime)
    editWorkEnd.value = formatTimeBack(rule.value.workEndTime)
    editTolerance.value = rule.value.missingToleranceMin ?? 30
  }
}

const showCreate = async () => {
  saving.value = true
  try {
    await axios.post('/api/v1/admin/attendance-rules', {
      ruleName: '默认规则',
      deptId: null,
      workStartTime: [9, 0],
      workEndTime: [18, 0],
      missingToleranceMin: 30,
      enabled: true
    })
    ElMessage.success('创建成功')
    await fetchRule()
  } catch { ElMessage.error('创建失败') }
  finally { saving.value = false }
}

onMounted(fetchRule)
</script>

<style scoped>
.header-row { margin-bottom: 20px; }
.loading-container { padding: 60px 0; text-align: center; color: #86868b; }
.rule-card { padding: 28px 32px; max-width: 600px; }
.rule-header { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.rule-badge {
  font-size: 12px; font-weight: 600; color: #fff; background: #0071e3;
  padding: 2px 10px; border-radius: 10px; line-height: 20px;
}
.rule-name { font-size: 18px; font-weight: 600; color: #1d1d1f; }
.rule-details { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; }
.detail-item { display: flex; align-items: center; gap: 16px; }
.detail-label { font-size: 14px; color: #86868b; width: 80px; flex-shrink: 0; }
.detail-value { font-size: 15px; color: #1d1d1f; display: flex; align-items: center; gap: 8px; }
.rule-actions { display: flex; gap: 12px; padding-top: 16px; border-top: 1px solid #e5e5e7; }
.empty-card { padding: 40px; text-align: center; }
.empty-card p { color: #86868b; margin-bottom: 16px; }
</style>
