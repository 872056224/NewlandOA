<template>
  <div class="leave-apply">
    <div class="page-header">
      <h2>请假申请</h2>
      <p class="page-desc">填写请假信息并提交，等待管理员审批</p>
    </div>

    <div class="apple-card form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="leave-form"
      >
        <el-form-item label="请假类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择请假类型" style="width: 100%">
            <el-option label="事假" value="事假" />
            <el-option label="病假" value="病假" />
            <el-option label="年假" value="年假" />
            <el-option label="调休" value="调休" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始时间" prop="startDate">
          <el-date-picker
            v-model="form.startDate"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disableEndDate"
          />
        </el-form-item>

        <el-form-item label="请假事由" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明请假原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button @click="$router.push('/emp-home')" size="large">取消</el-button>
            <el-button
              type="primary"
              @click="submitApply"
              :loading="submitting"
              size="large"
              class="apple-btn-primary"
            >
              提交申请
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  type: '',
  startDate: '',
  endDate: '',
  reason: ''
})

const rules: FormRules = {
  type: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请填写请假事由', trigger: 'blur' }]
}

const disableEndDate = (time: Date) => {
  if (form.startDate) {
    return time.getTime() <= new Date(form.startDate).getTime()
  }
  return false
}

const submitApply = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const response = await axios.post('/api/v1/employee/leave/apply', {
      type: form.type,
      startDate: form.startDate,
      endDate: form.endDate,
      reason: form.reason
    })
    if (response.data && response.data.code === 200) {
      ElMessage.success('请假申请提交成功！')
      router.push('/emp-home')
    } else {
      ElMessage.error(response.data?.message || '提交失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.leave-apply {
  max-width: 680px;
  margin: 0 auto;
  padding: 40px 32px;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--apple-text, #1d1d1f);
  margin: 0 0 8px;
}

.page-desc {
  font-size: 15px;
  color: var(--apple-text-secondary, #86868b);
  margin: 0;
}

.form-card {
  padding: 40px 36px;
}

.leave-form {
  max-width: 480px;
  margin: 0 auto;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.apple-btn-primary {
  background: var(--apple-blue, #0071e3);
  border: none;
  color: #fff;
  padding: 10px 28px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 980px;
  transition: all 0.2s ease;
}

.apple-btn-primary:hover {
  background: var(--apple-blue-hover, #0077ed);
}

.apple-btn-primary:active {
  background: var(--apple-blue-active, #006edb);
}
</style>
