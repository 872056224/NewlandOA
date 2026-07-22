<template>
  <div class="apple-page">
    <div class="header-row">
      <b class="apple-title" style="margin: 0;">考勤规则管理</b>
      <el-button class="apple-btn apple-btn-primary" @click="showCreateDialog">
        新增规则
      </el-button>
    </div>

    <!-- 规则列表 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      :border="false"
      stripe
      header-cell-class-name="apple-table-header"
      class="el-table--borderless"
    >
      <el-table-column label="规则名称" prop="ruleName" min-width="140" align="center" />
      <el-table-column label="适用部门" min-width="120" align="center">
        <template #default="{ row }">
          {{ getDeptName(row.deptId) }}
        </template>
      </el-table-column>
      <el-table-column label="上班时间" min-width="110" align="center">
        <template #default="{ row }">
          {{ formatTime(row.workStartTime) }}
        </template>
      </el-table-column>
      <el-table-column label="下班时间" min-width="110" align="center">
        <template #default="{ row }">
          {{ formatTime(row.workEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="迟到宽限(分)" prop="lateThresholdMin" min-width="120" align="center" />
      <el-table-column label="早退宽限(分)" prop="earlyThresholdMin" min-width="120" align="center" />
      <el-table-column label="状态" min-width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button @click="showEditDialog(row)" type="primary" size="small">编辑</el-button>
          <el-button
            @click="handleDelete(row)"
            type="danger"
            size="small"
            :disabled="row.deptId === null"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑考勤规则' : '新增考勤规则'"
      width="520px"
      @close="resetForm"
    >
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="例如：默认规则" />
        </el-form-item>
        <el-form-item label="适用部门" prop="deptId">
          <el-select
            v-model="formData.deptId"
            placeholder="不选则为全局默认规则"
            clearable
            style="width: 100%"
            :disabled="isEdit && formData.deptId === null"
          >
            <el-option
              v-for="d in deptOptions"
              :key="d.dept_id"
              :label="d.dept_name"
              :value="d.dept_id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上班时间" prop="workStartTime">
          <el-time-picker
            v-model="formData.workStartTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择上班时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="下班时间" prop="workEndTime">
          <el-time-picker
            v-model="formData.workEndTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择下班时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="迟到宽限(分)" prop="lateThresholdMin">
          <el-input-number
            v-model="formData.lateThresholdMin"
            :min="0"
            :max="120"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="早退宽限(分)" prop="earlyThresholdMin">
          <el-input-number
            v-model="formData.earlyThresholdMin"
            :min="0"
            :max="120"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="enabled">
          <el-switch
            v-model="formData.enabled"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button class="apple-btn apple-btn-primary" @click="handleSave" :loading="saving">
          {{ isEdit ? '保存修改' : '创建' }}
        </el-button>
        <el-button class="apple-btn" @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import axios from 'axios'

// ── Types ──
interface RuleItem {
  id: number
  ruleName: string
  deptId: number | null
  workStartTime: string  // "HH:mm" or array from backend
  workEndTime: string
  lateThresholdMin: number
  earlyThresholdMin: number
  enabled: boolean
}

interface DeptOption {
  dept_id: number
  dept_name: string
  dept_num: number
}

// ── State ──
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const tableData = ref<RuleItem[]>([])
const deptOptions = ref<DeptOption[]>([])
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  id: null as number | null,
  ruleName: '',
  deptId: null as number | null,
  workStartTime: '09:00' as string | null,
  workEndTime: '18:00' as string | null,
  lateThresholdMin: 0,
  earlyThresholdMin: 0,
  enabled: true
})

const formData = reactive(defaultForm())

const formRules = {
  ruleName: [
    { required: true, message: '请输入规则名称', trigger: 'blur' }
  ]
}

// ── Helper: format time from backend ──
function formatTime(time: any): string {
  if (!time) return '-'
  if (typeof time === 'string') return time.slice(0, 5)
  if (Array.isArray(time)) {
    const [h, m] = time
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
  }
  return String(time)
}

// ── Helper: get dept name ──
function getDeptName(deptId: number | null): string {
  if (deptId === null || deptId === undefined) return '全局'
  const dept = deptOptions.value.find(d => d.dept_id === deptId)
  return dept ? dept.dept_name : `部门#${deptId}`
}

// ── Convert time string to array for backend ──
function timeToArray(timeStr: string | null): number[] | null {
  if (!timeStr) return null
  const parts = timeStr.split(':')
  return [parseInt(parts[0]), parseInt(parts[1])]
}

// ── Fetch rules ──
async function fetchRules() {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/attendance-rules')
    if (response.data && response.data.code === 200) {
      tableData.value = response.data.data || []
    } else if (response.data && response.data.data) {
      tableData.value = response.data.data
    } else {
      tableData.value = []
    }
  } catch (error) {
    console.error('获取考勤规则失败:', error)
    ElMessage.error('获取考勤规则失败')
    tableData.value = []
  } finally {
    loading.value = false
  }
}

// ── Fetch departments for selector ──
async function fetchDepts() {
  try {
    const response = await axios.get('/api/v1/admin/departments', {
      params: { currentPage: 1, pageSize: 100 }
    })
    if (response.data && response.data.data) {
      deptOptions.value = response.data.data
    }
  } catch (error) {
    console.error('获取部门列表失败:', error)
  }
}

// ── Show create dialog ──
function showCreateDialog() {
  isEdit.value = false
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

// ── Show edit dialog ──
function showEditDialog(row: RuleItem) {
  isEdit.value = true
  formData.id = row.id
  formData.ruleName = row.ruleName
  formData.deptId = row.deptId
  formData.workStartTime = formatTime(row.workStartTime)
  formData.workEndTime = formatTime(row.workEndTime)
  formData.lateThresholdMin = row.lateThresholdMin ?? 0
  formData.earlyThresholdMin = row.earlyThresholdMin ?? 0
  formData.enabled = row.enabled
  dialogVisible.value = true
}

// ── Save ──
async function handleSave() {
  if (!formRef.value) return
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

  saving.value = true
  try {
    const payload = {
      ruleName: formData.ruleName,
      deptId: formData.deptId,
      workStartTime: timeToArray(formData.workStartTime),
      workEndTime: timeToArray(formData.workEndTime),
      lateThresholdMin: formData.lateThresholdMin,
      earlyThresholdMin: formData.earlyThresholdMin,
      enabled: formData.enabled
    }

    let response
    if (isEdit.value && formData.id) {
      response = await axios.put(`/api/v1/admin/attendance-rules/${formData.id}`, payload)
    } else {
      response = await axios.post('/api/v1/admin/attendance-rules', payload)
    }

    if (response.data && (response.data.code === 200 || response.data.data)) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      await fetchRules()
    } else {
      ElMessage.error(response.data?.message || '操作失败')
    }
  } catch (error: any) {
    console.error('保存规则失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// ── Delete ──
async function handleDelete(row: RuleItem) {
  if (row.deptId === null) {
    ElMessage.warning('全局默认规则不可删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除规则「${row.ruleName}」吗？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    const response = await axios.delete(`/api/v1/admin/attendance-rules/${row.id}`)
    if (response.data && (response.data.code === 200 || response.data.data)) {
      ElMessage.success('删除成功')
      await fetchRules()
    } else {
      ElMessage.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    console.error('删除规则失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// ── Reset form ──
function resetForm() {
  formRef.value?.resetFields()
  Object.assign(formData, defaultForm())
}

// ── Init ──
onMounted(() => {
  fetchRules()
  fetchDepts()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

:deep(.apple-table-header .cell) {
  color: var(--apple-text-secondary);
  font-weight: 500;
}

:deep(.el-table--borderless) {
  border: none;
}
:deep(.el-table--borderless::before) {
  display: none;
}

:deep(.el-dialog__body) {
  padding-top: 20px;
}
</style>
