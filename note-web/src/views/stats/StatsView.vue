<template>
  <div class="stats-page" v-loading="loading">
    <div class="page-header">
      <h1 class="page-title">数据统计</h1>
      <p class="page-subtitle">记录你的每一步成长</p>
    </div>

    <!-- 概览卡片 -->
    <div class="stats-overview">
      <div class="stat-card" v-for="(stat, index) in statsData" :key="stat.label"
           :style="{ animationDelay: `${index * 0.1}s` }">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">笔记趋势</h3>
          <span class="chart-subtitle">近7天创作轨迹</span>
        </div>
        <div ref="trendChartRef" class="chart-container"></div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">标签分布</h3>
          <span class="chart-subtitle">使用频率排行</span>
        </div>
        <div ref="tagChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import statsApi from '@/api/stats'

const loading = ref(false)
const trendChartRef = ref(null)
const tagChartRef = ref(null)

const statsData = reactive([
  { icon: '◇', label: '笔记总数', value: 0 },
  { icon: '◈', label: '已发布', value: 0 },
  { icon: '◇', label: '标签总数', value: 0 }
])

const fetchStats = async () => {
  loading.value = true
  try {
    const userId = 1
    const overviewRes = await statsApi.getOverview(userId)
    const overview = overviewRes.data.data || {}

    statsData[0].value = overview.totalNotes || 0
    statsData[1].value = overview.publishedNotes || 0
    statsData[2].value = overview.totalTags || 0

    const trendRes = await statsApi.getNotesTrend(userId, 7)
    const tagRes = await statsApi.getTagsUsage(userId)

    await nextTick()
    renderTrendChart(trendRes.data.data || [])
    renderTagChart(tagRes.data.data || [])
  } catch (error) {
    console.error('获取统计数据失败', error)
  } finally {
    loading.value = false
  }
}

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e4df',
      textStyle: { color: '#2d2a26' }
    },
    grid: { left: '5%', right: '5%', bottom: '10%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date ? d.date.slice(5) : ''),
      axisLine: { lineStyle: { color: '#e8e4df' } },
      axisLabel: { color: '#6b6560' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0ede8' } },
      axisLabel: { color: '#6b6560' }
    },
    series: [{
      data: data.map(d => d.count),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: '#c4704b', width: 3 },
      itemStyle: { color: '#c4704b' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(196, 112, 75, 0.3)' },
            { offset: 1, color: 'rgba(196, 112, 75, 0)' }
          ]
        }
      }
    }]
  })
}

const renderTagChart = (data) => {
  if (!tagChartRef.value) return
  const chart = echarts.init(tagChartRef.value)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e8e4df',
      textStyle: { color: '#2d2a26' },
      formatter: '{b}: {c} 次 ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'outside',
        formatter: '{b}',
        color: '#6b6560',
        fontSize: 12
      },
      labelLine: { show: true, lineStyle: { color: '#e8e4df' } },
      data: data.map((d, i) => ({
        name: d.name,
        value: d.count,
        itemStyle: {
          color: ['#c4704b', '#e8a87c', '#f5d5c0', '#d4e4d9', '#9c9790'][i % 5]
        }
      }))
    }]
  })
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.stats-page {
  animation: fadeIn 0.5s ease-out;
  position: relative;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.page-title {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 42px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-muted);
}

/* 概览卡片 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-soft);
  animation: fadeInUp 0.5s ease-out both;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-tertiary), var(--accent-secondary));
  border-radius: var(--radius-md);
  font-size: 24px;
  color: var(--accent-primary);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-family: 'Crimson Pro', serif;
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 图表区域 */
.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-soft);
}

.chart-header {
  margin-bottom: 24px;
}

.chart-title {
  font-family: 'Crimson Pro', serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-subtitle {
  font-size: 13px;
  color: var(--text-muted);
}

.chart-container {
  height: 280px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
