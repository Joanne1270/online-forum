<template>
  <el-card>
    <div slot="header" class="card-header">
      <span>违禁词管理</span>
    </div>
    <div class="toolbar">
      <el-input v-model="word" placeholder="输入违禁词" style="width:240px" @keyup.enter.native="add" />
      <el-button type="primary" @click="add">添加</el-button>
    </div>
    <el-table :data="words" style="margin-top:16px">
      <el-table-column prop="word" label="违禁词" />
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column label="操作" width="120">
        <template slot-scope="{ row }">
          <el-button size="mini" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
import { adminApi } from '../../api/admin'

export default {
  data() {
    return { words: [], word: '' }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      const res = await adminApi.bannedWords()
      this.words = res.data || []
    },
    async add() {
      const value = this.word.trim()
      if (!value) {
        this.$message.warning('请输入违禁词')
        return
      }
      await adminApi.addBannedWord({ word: value })
      this.word = ''
      this.$message.success('添加成功')
      this.load()
    },
    async remove(id) {
      await this.$confirm('确认删除该违禁词？')
      await adminApi.deleteBannedWord(id)
      this.$message.success('已删除')
      this.load()
    }
  }
}
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; align-items: center; }
</style>
