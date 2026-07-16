<template>
  <div class="sign-statistics">
    <div class="header-section">
      <h1 class="chart-title">
        <el-icon class="title-icon"><TrendCharts /></el-icon>
        近五日签到情况统计视图
      </h1>
      <el-button 
        @click="refreshChart" 
        :loading="loading"
        type="primary" 
        class="refresh-btn"
      >
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>
    
    <el-card class="chart-card" v-loading="loading" element-loading-text="正在加载图表数据...">
      <div id="main" ref="chartContainer" class="chart-container"></div>
      
      <!-- 加载状态遮罩 -->
      <div v-if="loading" class="loading-overlay">
        <div class="loading-content">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p class="loading-text">正在获取统计数据...</p>
        </div>
      </div>
      
      <!-- 空数据状态 -->
      <div v-if="!loading && hasError" class="empty-state">
        <el-icon class="empty-icon"><DocumentRemove /></el-icon>
        <p class="empty-text">暂无统计数据</p>
        <el-button @click="refreshChart" type="primary">重新获取</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, Refresh, Loading, DocumentRemove } from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

const chartContainer = ref<HTMLElement>()
const loading = ref(false)
const hasError = ref(false)
let myChart: echarts.ECharts | null = null

const initChart = () => {
  if (chartContainer.value) {
    myChart = echarts.init(chartContainer.value)
    
    // 设置默认图表配置
    const defaultOption = {
      title: {
        text: '近五日签到图',
        textStyle: {
          color: '#333'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['已签到人数', '未签到人数', '需签到总人数']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: []
      },
      yAxis: {
        type: 'value',
        boundaryGap: [0, 0.01]
      },
      series: [
        {
          name: '已签到人数',
          type: 'bar',
          data: [],
          itemStyle: {
            color: '#67C23A'
          }
        },
        {
          name: '未签到人数',
          type: 'bar',
          data: [],
          itemStyle: {
            color: '#F56C6C'
          }
        },
        {
          name: '需签到总人数',
          type: 'bar',
          data: [],
          itemStyle: {
            color: '#409EFF'
          }
        }
      ]
    }
    
    myChart.setOption(defaultOption)
    
    // 监听窗口大小变化
    window.addEventListener('resize', () => {
      myChart?.resize()
    })
  }
}

const loadChartData = async () => {
  loading.value = true
  hasError.value = false
  
  try {
    const response = await axios.get('/api/v1/admin/attendance/statistics/chart')
    
    if (response.data) {
      // 数据直接在 response.data 中，不需要嵌套访问
      const dates = response.data.data || []        // 日期数组
      const signedData = response.data.data1 || []  // 已签到人数
      const unsignedData = response.data.data2 || [] // 未签到人数
      const totalData = response.data.data3 || []   // 需签到总人数
      
      // 检查数据是否有效
      if (dates.length === 0) {
        console.warn('日期数据为空')
        hasError.value = true
        ElMessage.warning('暂无统计数据')
        return
      }
      
      const option = {
        title: {
          text: '近五日签到图',
          textStyle: {
            color: '#333'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['已签到人数', '未签到人数', '需签到总人数']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: dates
        },
        yAxis: {
          type: 'value',
          boundaryGap: [0, 0.01]
        },
        series: [
          {
            name: '已签到人数',
            type: 'bar',
            data: signedData,
            itemStyle: {
              color: '#67C23A'
            }
          },
          {
            name: '未签到人数',
            type: 'bar',
            data: unsignedData,
            itemStyle: {
              color: '#F56C6C'
            }
          },
          {
            name: '需签到总人数',
            type: 'bar',
            data: totalData,
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      }
      
      if (myChart) {
        myChart.setOption(option)
        console.log('图表已更新')
      } else {
        console.error('图表实例不存在')
        hasError.value = true
      }
    } else {
      hasError.value = true
      ElMessage.error('获取统计数据失败')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    hasError.value = true
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新图表数据
const refreshChart = async () => {
  await loadChartData()
}

onMounted(async () => {
  await nextTick()
  initChart()
  loadChartData()
})
</script>

<style scoped>
.sign-statistics {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-title {
  display: flex;
  align-items: center;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.title-icon {
  margin-right: 8px;
  color: #409EFF;
}

.refresh-btn {
  margin-left: 16px;
}

.chart-card {
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-container {
  width: 100%;
  height: 600px;
  min-height: 400px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-content {
  text-align: center;
}

.loading-icon {
  font-size: 48px;
  color: #409EFF;
  animation: rotate 2s linear infinite;
  margin-bottom: 16px;
}

.loading-text {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #C0C4CC;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #909399;
  margin-bottom: 16px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style> 