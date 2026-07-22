<template>
  <div class="notif-page">
    <div class="page-header">
      <h2>我的通知</h2>
      <el-button v-if="unreadCount > 0" text @click="markAllRead">全部已读</el-button>
    </div>

    <div class="apple-card" style="padding: 0;">
      <el-table :data="list" v-loading="loading" style="width: 100%" empty-text="暂无通知" class="el-table--borderless">
        <el-table-column label="消息" min-width="300">
          <template #default="{ row }">
            <div class="msg-cell">
              <span class="msg-dot" v-if="!row.is_read"></span>
              <div class="msg-body">
                <div class="msg-title">{{ row.title }}</div>
                <div class="msg-content">{{ row.content }}</div>
                <div class="msg-time">{{ row.create_time }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="!row.is_read" size="small" type="danger">未读</el-tag>
            <span v-else class="read-text">已读</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage" v-model:page-size="pageSize"
          :total="total" layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const list = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const unreadCount = ref(0)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/v1/employee/notifications', {
      params: { currentPage: currentPage.value, pageSize: pageSize.value }
    })
    if (res.data?.code === 200) {
      list.value = res.data.data || []
      total.value = res.data.total || 0
    }
    const unreadRes = await axios.get('/api/v1/employee/notifications/unread-count')
    if (unreadRes.data?.code === 200) unreadCount.value = unreadRes.data.data || 0
  } catch (e) {
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

const markAllRead = async () => {
  await axios.put('/api/v1/employee/notifications/read-all')
  unreadCount.value = 0
  list.value.forEach((n: any) => n.is_read = 1)
  ElMessage.success('全部已读')
}

onMounted(() => fetchList())
</script>

<style scoped>
.notif-page { max-width: 1000px; margin: 0 auto; padding: 40px 32px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 24px; font-weight: 700; color: #1d1d1f; margin: 0; }
.msg-cell { display: flex; align-items: flex-start; gap: 10px; padding: 4px 0; }
.msg-dot { width: 8px; height: 8px; min-width: 8px; background: #0071e3; border-radius: 50%; margin-top: 8px; }
.msg-title { font-size: 14px; font-weight: 600; color: #1d1d1f; }
.msg-content { font-size: 13px; color: #86868b; margin-top: 4px; }
.msg-time { font-size: 12px; color: #aeaeb2; margin-top: 6px; }
.read-text { color: #aeaeb2; font-size: 13px; }
.pagination-wrap { display: flex; justify-content: center; padding: 20px; }
</style>
