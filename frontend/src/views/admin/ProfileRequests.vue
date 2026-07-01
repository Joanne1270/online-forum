<template>
  <el-card>
    <div slot="header" class="card-header">
      <span>资料审核</span>
    </div>
    <el-table :data="requests">
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="当前昵称" width="120" />
      <el-table-column label="类型" width="100">
        <template slot-scope="{ row }">{{ row.fieldType === 'NICKNAME' ? '昵称' : '头像' }}</template>
      </el-table-column>
      <el-table-column prop="oldValue" label="原内容" />
      <el-table-column label="新内容">
        <template slot-scope="{ row }">
          <img v-if="row.fieldType === 'AVATAR'" :src="row.newValue" class="avatar-preview">
          <span v-else>{{ row.newValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="160">
        <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template slot-scope="{ row }">
          <el-button size="mini" type="success" @click="approve(row.id)">通过</el-button>
          <el-button size="mini" type="danger" @click="reject(row.id)">拒绝</el-button>
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
    return { requests: [], total: 0, page: 1, size: 20 }
  },
  created() {
    this.load()
  },
  methods: {
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    async load() {
      const res = await adminApi.profileRequests({ page: this.page, size: this.size })
      this.requests = res.data.list || []
      this.total = res.data.total || 0
    },
    async approve(id) {
      await adminApi.approveProfileRequest(id)
      this.$message.success('已通过')
      this.load()
    },
    async reject(id) {
      await adminApi.rejectProfileRequest(id)
      this.$message.success('已拒绝')
      this.load()
    }
  }
}
</script>

<style scoped>
.avatar-preview { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; }
</style>
