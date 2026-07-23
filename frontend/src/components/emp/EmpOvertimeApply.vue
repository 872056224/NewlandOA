<template>
  <div class="apple-page">
    <b class="apple-title" style="margin-bottom: 20px; display: block;">加班申请</b>

    <div class="apple-card" style="padding: 24px; max-width: 500px;">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="加班日期" prop="overtimeDate">
          <el-date-picker v-model="form.overtimeDate" type="date" placeholder="选择加班日期"
            :disabled-date="disabledDate" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm"
            placeholder="开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm"
            placeholder="结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="加班时长">
          <span v-if="duration > 0" class="duration-text">{{ duration }} 小时</span>
          <span v-else class="duration-hint">选择日期和时间后自动计算</span>
        </el-form-item>
        <el-form-item label="加班事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明加班原因" maxlength="200" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" class="apple-btn apple-btn-primary">
            提交申请
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import axios from 'axios'

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  overtimeDate: '',
  startTime: '',
  endTime: '',
  reason: ''
})

const rules = {
  overtimeDate: [{ required: true, message: '请选择加班日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const duration = computed(() => {
  if (!form.startTime || !form.endTime) return 0
  const [sh, sm] = form.startTime.split(':').map(Number)
  const [eh, em] = form.endTime.split(':').map(Number)
  const total = (eh * 60 + em) - (sh * 60 + sm)
  return total > 0 ? Math.round(total / 6) / 10 : 0
})

const disabledDate = (date: Date) => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  // 只能选今天及以后的非工作日
  if (date < now) return true
  const day = date.getDay()
  return day !== 0 && day !== 6  // 只允许周末（周六=6, 周日=0）
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (duration.value <= 0) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  submitting.value = true
  try {
    const response = await axios.post('/api/v1/employee/attendance/overtime/apply', null, {
      params: {
        overtimeDate: form.overtimeDate,
        startTime: form.startTime,
        endTime: form.endTime,
        reason: form.reason
      }
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success('加班申请已提交，等待管理员审批')
      formRef.value.resetFields()
    } else {
      ElMessage.error(response.data?.message || '提交失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.duration-text { font-size: 18px; font-weight: 700; color: #0071e3; }
.duration-hint { font-size: 13px; color: #86868b; }
</style>
