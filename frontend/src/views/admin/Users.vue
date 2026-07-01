<template>
  <el-card>
    <h2>用户管理</h2>
    <el-table :data="users">
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column label="状态" width="120">
        <template slot-scope="{ row }">
          <span v-if="row.status === 1">正常</span>
          <span v-else-if="row.status === 2">已注销</span>
          <span v-else>封禁</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="400">
        <template slot-scope="{ row }">
          <template v-if="row.status !== 2">
            <el-button v-if="row.status === 1" size="mini" type="danger" @click="openBan(row)">封禁</el-button>
            <el-button v-else size="mini" type="success" @click="unban(row.id)">解封</el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              size="mini"
              type="warning"
              @click="setRole(row.id, 'ADMIN')"
            >设为管理员</el-button>
            <el-button
              v-else-if="row.phone !== '13800000001'"
              size="mini"
              @click="setRole(row.id, 'USER')"
            >取消管理员</el-button>
            <el-button
              v-if="row.role !== 'ADMIN'"
              size="mini"
              type="danger"
              plain
              @click="deactivate(row)"
            >注销账号</el-button>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="封禁用户" :visible.sync="banVisible" width="400px">
      <p>用户：{{ banTarget && banTarget.nickname }}（{{ banTarget && banTarget.phone }}）</p>
      <el-radio-group v-model="banDays">
        <el-radio :label="1">1 天</el-radio>
        <el-radio :label="7">7 天</el-radio>
        <el-radio :label="30">30 天</el-radio>
        <el-radio :label="-1">永久</el-radio>
      </el-radio-group>
      <span slot="footer">
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBan">确认封禁</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { adminApi } from '../../api/admin'

export default {
  data() {
    return {
      users: [],
      banVisible: false,
      banTarget: null,
      banDays: 7
    }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await adminApi.users({ page: 1, size: 100 })
      this.users = res.data || []
    },
    openBan(row) {
      this.banTarget = row
      this.banDays = 7
      this.banVisible = true
    },
    async confirmBan() {
      await adminApi.ban(this.banTarget.id, { banDays: this.banDays })
      this.banVisible = false
      this.$message.success('封禁成功')
      this.load()
    },
    async unban(id) {
      await adminApi.unban(id)
      this.$message.success('已解封')
      this.load()
    },
    async setRole(id, role) {
      const label = role === 'ADMIN' ? '设为管理员' : '取消管理员'
      await this.$confirm(`确认${label}？`)
      await adminApi.setRole(id, role)
      this.$message.success('操作成功')
      this.load()
    },
    async deactivate(row) {
      await this.$confirm(
        `确认注销用户「${row.nickname}」（${row.phone}）？注销后该账号将无法登录，手机号可重新注册。`,
        '注销账号',
        { type: 'warning' }
      )
      await adminApi.deactivateUser(row.id)
      this.$message.success('账号已注销')
      this.load()
    }
  }
}
</script>

<style scoped>
.muted { color: #909399; font-size: 13px; }
</style>
