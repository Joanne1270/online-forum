<template>
  <el-card>
    <el-input v-model="keyword" placeholder="搜索帖子、标签（如 #问答）" @keyup.enter.native="search">
      <el-button slot="append" icon="el-icon-search" @click="search" />
    </el-input>
    <post-item v-for="p in posts" :key="p.id" :post="p" style="margin-top:12px" />
    <el-pagination layout="prev, pager, next" :total="total" :page-size="size" :current-page.sync="page" @current-change="search" />
  </el-card>
</template>

<script>
import { postApi } from '../../api/post'
import PostItem from '../../components/PostItem.vue'
import { addSearchHistory, getSearchHistoryEnabled } from '../../utils/searchHistory'

export default {
  components: { PostItem },
  data() {
    return { keyword: '', posts: [], total: 0, page: 1, size: 10 }
  },
  watch: {
    '$route.query.keyword'(val) {
      this.keyword = val || ''
      if (this.keyword.trim()) {
        this.page = 1
        this.search()
      }
    }
  },
  created() {
    if (this.$route.query.keyword) {
      this.keyword = this.$route.query.keyword
      this.search()
    }
  },
  mounted() {
    this.$root.$on('header-search', this.refreshSearch)
  },
  beforeDestroy() {
    this.$root.$off('header-search', this.refreshSearch)
  },
  methods: {
    refreshSearch() {
      if (this.$route.query.keyword) {
        this.keyword = this.$route.query.keyword
      }
      this.page = 1
      this.search()
    },
    async search() {
      const keyword = (this.keyword || '').trim()
      if (!keyword) return
      if (getSearchHistoryEnabled()) {
        addSearchHistory(keyword)
      }
      const res = await postApi.search({ keyword, page: this.page, size: this.size })
      this.posts = res.data.list || []
      this.total = res.data.total || 0
    }
  }
}
</script>
