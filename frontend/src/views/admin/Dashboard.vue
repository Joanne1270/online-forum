<template>
  <el-card>
    <h2>数据概览</h2>
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="stat">
          <div class="label">用户数</div>
          <div class="value">{{ stats.userCount }}</div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="stat">
          <div class="label">帖子数</div>
          <div class="value">{{ stats.postCount }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="online-section">
      <div class="online-header">
        <div class="online-title">
          <span>实时在线用户</span>
          <span class="online-current">{{ onlineStats.currentOnline }} 人</span>
        </div>
        <el-radio-group v-model="range" size="small" @change="loadOnlineStats">
          <el-radio-button label="day">本日</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="year">本年</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chart" class="online-chart" v-loading="chartLoading"></div>
    </div>
  </el-card>
</template>

<script>
import * as echarts from 'echarts'
import { adminApi } from '../../api/admin'

export default {
  data() {
    return {
      stats: { userCount: 0, postCount: 0 },
      range: 'day',
      onlineStats: { currentOnline: 0, points: [] },
      chartLoading: false,
      chart: null,
      pollTimer: null
    }
  },
  async created() {
    const res = await adminApi.stats()
    this.stats = res.data
    await this.loadOnlineStats()
    this.pollTimer = setInterval(this.loadOnlineStats, 60000)
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    clearInterval(this.pollTimer)
    window.removeEventListener('resize', this.handleResize)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    async loadOnlineStats() {
      this.chartLoading = true
      try {
        const res = await adminApi.onlineStats(this.range)
        this.onlineStats = res.data || { currentOnline: 0, points: [] }
        this.$nextTick(() => this.renderChart())
      } finally {
        this.chartLoading = false
      }
    },
    renderChart() {
      const el = this.$refs.chart
      if (!el) return
      if (!this.chart) {
        this.chart = echarts.init(el)
      }
      const points = this.onlineStats.points || []
      const labels = points.map(p => p.label)
      const values = points.map(p => p.count || 0)
      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: params => {
            const item = params[0]
            return `${item.axisValue}<br/>在线人数：${item.data} 人`
          }
        },
        grid: { left: 48, right: 24, top: 32, bottom: 40 },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: labels,
          axisLabel: {
            color: '#606266',
            interval: this.range === 'day' ? 1 : 'auto'
          },
          axisLine: { lineStyle: { color: '#dcdfe6' } }
        },
        yAxis: {
          type: 'value',
          name: '人数',
          minInterval: 1,
          axisLabel: { color: '#606266' },
          splitLine: { lineStyle: { color: '#ebeef5' } }
        },
        series: [{
          name: '在线人数',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: values,
          lineStyle: { color: '#409EFF', width: 2 },
          itemStyle: { color: '#409EFF' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.28)' },
              { offset: 1, color: 'rgba(64,158,255,0.02)' }
            ])
          }
        }]
      }, true)
    },
    handleResize() {
      if (this.chart) this.chart.resize()
    }
  }
}
</script>

<style scoped>
.stat {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}
.label { color: #909399; margin-bottom: 8px; }
.value { font-size: 28px; font-weight: bold; color: #409EFF; }
.online-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.online-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.online-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.online-current {
  font-size: 14px;
  font-weight: 500;
  color: #409EFF;
}
.online-chart {
  width: 100%;
  height: 360px;
}
</style>
