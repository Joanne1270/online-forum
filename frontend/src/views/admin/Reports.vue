<template>
  <el-card>
    <div slot="header" class="card-header">
      <span>举报管理</span>
    </div>
    <el-table :data="reports">
      <el-table-column prop="postTitle" label="被举报帖子" />
      <el-table-column prop="reporterName" label="举报人" width="120" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="createdAt" label="时间" width="160">
        <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="{ row }">
          <el-button size="mini" type="danger" @click="handle(row.id, 'DELETE_POST')">删帖</el-button>
          <el-button size="mini" @click="handle(row.id, 'DISMISS')">忽略</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination layout="prev, pager, next" :total="total" :page-size="size" :current-page.sync="page" @current-change="load" />
  </el-card>
</template>

<script>
import { adminApi } from '../../api/admin'

export default {
  data() {
    return { reports: [], total: 0, page: 1, size: 20 }
  },
  created() { this.load() },
  methods: {
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    async load() {
      const res = await adminApi.reports({ page: this.page, size: this.size })
      this.reports = res.data.list || []
      this.total = res.data.total || 0
    },
    async handle(id, action) {
      const msg = action === 'DELETE_POST' ? '确认删除被举报的帖子？' : '确认忽略该举报？'
      await this.$confirm(msg)
      await adminApi.handleReport(id, action)
      this.$message.success('处理完成')
      this.load()
    }
  }
}
</script>
