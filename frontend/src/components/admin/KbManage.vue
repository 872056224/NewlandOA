<template>
  <div class="kb-manage">
    <div class="page-header">
      <b class="page-title">AI 客服知识库管理</b>
      <div class="page-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索问题/答案/关键词"
          clearable
          style="width: 240px; margin-right: 12px;"
          @keyup.enter="handleSearch"
        />
        <el-button @click="handleSearch">搜索</el-button>
        <el-button @click="handleReloadIndex" :loading="reloading">重建索引</el-button>
        <el-button @click="showAdd" type="primary">添加知识</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" row-key="id">
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="标准问题" prop="question" min-width="180" show-overflow-tooltip />
      <el-table-column label="答案" prop="answer" min-width="260" show-overflow-tooltip />
      <el-table-column label="关键词" prop="keywords" min-width="140" show-overflow-tooltip />
      <el-table-column label="热门" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hot ? 'danger' : 'info'" size="small">
            {{ row.hot ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'warning'" size="small">
            {{ row.enabled ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button @click="showEdit(row)" type="warning" size="small">编辑</el-button>
          <el-button @click="handleDelete(row)" type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :page-sizes="[8, 10, 20]"
      :total="pagination.total"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px;"
    />

    <el-dialog
      v-model="dialogVisible.add"
      @close="resetForm('addForm')"
      title="添加知识库条目"
      width="560px"
    >
      <el-form :model="formData" :rules="rules" ref="addFormRef" label-width="100px">
        <el-form-item label="标准问题" prop="question">
          <el-input v-model="formData.question" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input
            v-model="formData.answer"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input
            v-model="formData.keywords"
            placeholder="空格分隔，如：签到 打卡 考勤"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="热门推荐">
          <el-switch v-model="formData.hot" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDoc" type="primary" :loading="submitting">添加</el-button>
        <el-button @click="resetForm('addForm')">重置</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible.edit"
      @close="resetForm('editForm')"
      title="编辑知识库条目"
      width="560px"
    >
      <el-form :model="editFormData" :rules="rules" ref="editFormRef" label-width="100px">
        <el-form-item label="ID">
          <el-input :disabled="true" v-model="editFormData.id" />
        </el-form-item>
        <el-form-item label="标准问题" prop="question">
          <el-input v-model="editFormData.question" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input
            v-model="editFormData.answer"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="editFormData.keywords" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="热门推荐">
          <el-switch v-model="editFormData.hot" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editFormData.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="warning" @click="updateDoc" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import axios from 'axios'

interface KbDocRow {
  id: number
  question: string
  answer: string
  keywords?: string
  hot: boolean
  enabled: boolean
  createTime?: string
}

const loading = ref(false)
const submitting = ref(false)
const reloading = ref(false)
const searchKeyword = ref('')
const tableData = ref<KbDocRow[]>([])

const addFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const dialogVisible = reactive({
  add: false,
  edit: false
})

const formData = reactive({
  question: '',
  answer: '',
  keywords: '',
  hot: false,
  enabled: true
})

const editFormData = reactive({
  id: '',
  question: '',
  answer: '',
  keywords: '',
  hot: false,
  enabled: true
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const rules = {
  question: [{ required: true, message: '请输入标准问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

const listParams = () => ({
  currentPage: pagination.currentPage,
  pageSize: pagination.pageSize,
  keyword: searchKeyword.value.trim() || undefined
})

const fetchList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/v1/admin/kb-docs', { params: listParams() })
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '获取知识库列表失败')
      return
    }
    if (response.data) {
      tableData.value = response.data.data || []
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
    }
  } catch (error) {
    console.error('获取知识库列表失败:', error)
    ElMessage.error('获取知识库列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  fetchList()
}

const handleSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  fetchList()
}

const handleCurrentChange = (pageNum: number) => {
  pagination.currentPage = pageNum
  fetchList()
}

const showAdd = () => {
  dialogVisible.add = true
}

const showEdit = (row: KbDocRow) => {
  dialogVisible.edit = true
  Object.assign(editFormData, {
    id: String(row.id),
    question: row.question,
    answer: row.answer,
    keywords: row.keywords || '',
    hot: !!row.hot,
    enabled: row.enabled !== false
  })
}

const addDoc = async () => {
  if (!addFormRef.value) return
  try {
    const valid = await addFormRef.value.validate()
    if (!valid) return

    submitting.value = true
    const response = await axios.post('/api/v1/admin/kb-docs', { ...formData }, {
      params: listParams(),
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '添加失败')
      return
    }
    if (response.data?.data) {
      tableData.value = response.data.data
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
      ElMessage.success('添加成功，向量索引已同步')
      dialogVisible.add = false
      resetForm('addForm')
    }
  } catch (error) {
    console.error('添加知识库条目失败:', error)
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

const updateDoc = async () => {
  if (!editFormRef.value) return
  try {
    const valid = await editFormRef.value.validate()
    if (!valid) return

    submitting.value = true
    const payload = {
      question: editFormData.question,
      answer: editFormData.answer,
      keywords: editFormData.keywords,
      hot: editFormData.hot,
      enabled: editFormData.enabled
    }
    const response = await axios.put(`/api/v1/admin/kb-docs/${editFormData.id}`, payload, {
      params: listParams(),
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '更新失败')
      return
    }
    if (response.data?.data) {
      tableData.value = response.data.data
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
      ElMessage.success('更新成功，向量索引已同步')
      dialogVisible.edit = false
      resetForm('editForm')
    }
  } catch (error) {
    console.error('更新知识库条目失败:', error)
    ElMessage.error('更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: KbDocRow) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.question}」吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await axios.delete(`/api/v1/admin/kb-docs/${row.id}`, {
      params: listParams()
    })

    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '删除失败')
      return
    }
    if (response.data?.data) {
      tableData.value = response.data.data
      pagination.total = response.data.total || 0
      pagination.currentPage = response.data.pageNum || pagination.currentPage
      ElMessage.success('删除成功，向量索引已同步')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除知识库条目失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleReloadIndex = async () => {
  reloading.value = true
  try {
    const response = await axios.post('/api/v1/admin/kb-docs/reload-index')
    if (response.data?.code && response.data.code !== 200) {
      ElMessage.error(response.data.message || '重建索引失败')
      return
    }
    ElMessage.success('向量索引重建成功')
  } catch (error) {
    console.error('重建索引失败:', error)
    ElMessage.error('重建索引失败，请确认员工服务已启动')
  } finally {
    reloading.value = false
  }
}

const resetForm = (formName: string) => {
  if (formName === 'addForm') {
    Object.assign(formData, {
      question: '',
      answer: '',
      keywords: '',
      hot: false,
      enabled: true
    })
    addFormRef.value?.resetFields()
  } else if (formName === 'editForm') {
    Object.assign(editFormData, {
      id: '',
      question: '',
      answer: '',
      keywords: '',
      hot: false,
      enabled: true
    })
    editFormRef.value?.resetFields()
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.kb-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  color: #c0392b;
  font-size: 20px;
}

.page-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
