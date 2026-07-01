<template>
  <el-card>
    <h2>帖子管理</h2>
    <el-table :data="posts">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="操作" width="120">
        <template slot-scope="{ row }">
          <el-button size="mini" type="danger" @click="remove(row.id)">删除</el-button>
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
    return { posts: [], total: 0, page: 1, size: 20 }
  },
  created() { this.load() },
  methods: {
    async load() {
      const res = await adminApi.posts({ page: this.page, size: this.size })
      this.posts = res.data.list || []
      this.total = res.data.total || 0
    },
    async remove(id) {
      await this.$confirm('确认删除该帖子？')
      await adminApi.deletePost(id)
      this.load()
    }
  }
}
</script>
