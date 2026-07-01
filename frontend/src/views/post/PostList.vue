<template>
  <el-card>
    <div slot="header" class="card-header">
      <span>{{ boardName }} - 帖子列表</span>
      <el-button v-if="canPostHere" type="primary" size="mini" @click="goCreate">发帖</el-button>
    </div>
    <div class="post-toolbar">
      <span class="toolbar-label">排序</span>
      <el-select v-model="postSort" size="small" class="sort-select" @change="reload">
        <el-option label="推荐" value="hot" />
        <el-option label="最新" value="time_desc" />
        <el-option label="最早" value="time_asc" />
      </el-select>
    </div>
    <post-item v-for="p in posts" :key="p.id" :post="p" />
    <el-pagination layout="prev, pager, next" :total="total" :page-size="size" :current-page.sync="page" @current-change="load" />
  </el-card>
</template>

<script>
import { mapGetters } from 'vuex'
import { postApi } from '../../api/post'
import PostItem from '../../components/PostItem.vue'
import { findBoardName, canPostInBoard } from '../../utils/board'

export default {
  components: { PostItem },
  data() {
    return { posts: [], total: 0, page: 1, size: 10, boardName: '', boards: [], postSort: 'hot' }
  },
  computed: {
    ...mapGetters(['isLogin', 'isAdmin']),
    canPostHere() {
      return canPostInBoard(this.$route.params.id, this.boards, this.isAdmin)
    }
  },
  watch: { '$route.params.id': 'load' },
  async created() {
    const boardsRes = await postApi.boards()
    this.boards = boardsRes.data || []
    this.load()
  },
  methods: {
    reload() {
      this.page = 1
      this.load()
    },
    async load() {
      const boardId = this.$route.params.id
      this.boardName = findBoardName(this.boards, boardId)
      const res = await postApi.list({ boardId, page: this.page, size: this.size, sort: this.postSort })
      this.posts = res.data.list || []
      this.total = res.data.total || 0
    },
    goCreate() {
      this.$router.push({ path: '/post/create', query: { boardId: this.$route.params.id } })
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.post-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.toolbar-label { color: #606266; font-size: 13px; }
.sort-select { width: 120px; }
</style>
