<template>
  <el-card>
    <div slot="header" class="header">
      <span>关注</span>
    </div>
    <div v-loading="loading">
      <post-item v-for="p in posts" :key="p.id" :post="p" />
      <el-empty v-if="!loading && !posts.length" description="暂无关注动态，去用户主页关注吧" :image-size="80" />
      <el-pagination
        v-if="total > size"
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page.sync="page"
        style="margin-top:16px"
        @current-change="load"
      />
    </div>
  </el-card>
</template>

<script>
import { postApi } from '../../api/post'
import PostItem from '../../components/PostItem.vue'

export default {
  components: { PostItem },
  data() {
    return { loading: false, posts: [], total: 0, page: 1, size: 10 }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const res = await postApi.followingFeed({ page: this.page, size: this.size })
        this.posts = res.data.list || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.header { font-weight: 600; }
</style>
