<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div slot="header">版块</div>
          <el-menu :default-active="activeBoard" @select="goBoard">
            <template v-for="b in boards">
              <el-submenu v-if="b.children && b.children.length" :key="'cat-' + b.id" :index="'sub-' + b.id">
                <template slot="title">{{ b.name }}</template>
                <el-menu-item :index="String(b.id)">全部</el-menu-item>
                <el-menu-item v-for="c in b.children" :key="c.id" :index="String(c.id)">{{ c.name }}</el-menu-item>
              </el-submenu>
              <el-menu-item v-else :key="b.id" :index="String(b.id)">{{ b.name }}</el-menu-item>
            </template>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <div slot="header" class="card-header">
            <span>{{ panelTitle }}</span>
            <el-button v-if="canPostHere" type="primary" size="mini" @click="goCreate">发帖</el-button>
          </div>
          <div v-if="isAllBoard" class="post-toolbar">
            <span class="toolbar-label">筛选</span>
            <el-radio-group v-model="featuredFilter" size="small" @change="loadPosts">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="featured">仅精华</el-radio-button>
            </el-radio-group>
          </div>
          <div v-else-if="showBoardSort" class="post-toolbar">
            <span class="toolbar-label">排序</span>
            <el-select v-model="postSort" size="small" class="sort-select" @change="loadPosts">
              <el-option label="推荐" value="hot" />
              <el-option label="最新" value="time_desc" />
              <el-option label="最早" value="time_asc" />
            </el-select>
          </div>
          <post-item
            v-for="p in posts"
            :key="p.id"
            :post="p"
            :show-official-pin="showOfficialPinActions"
            @toggle-official-pin="toggleOfficialPin"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { postApi } from '../../api/post'
import PostItem from '../../components/PostItem.vue'
import { findBoardName, flattenBoards, isHomeSectionBoard } from '../../utils/board'

export default {
  components: { PostItem },
  data() {
    return { boards: [], posts: [], activeBoard: '1', postSort: 'hot', featuredFilter: 'all' }
  },
  computed: {
    ...mapGetters(['isLogin', 'isAdmin']),
    currentBoard() {
      return flattenBoards(this.boards).find(b => String(b.id) === this.activeBoard)
    },
    isAllBoard() {
      return this.currentBoard && this.currentBoard.boardType === 'ALL'
    },
    isCategoryBoard() {
      return this.currentBoard && this.currentBoard.boardType === 'CATEGORY'
    },
    isHomeBoard() {
      return this.currentBoard && this.currentBoard.boardType === 'HOME'
    },
    showBoardSort() {
      return this.isCategoryBoard || this.isHomeBoard
    },
    showOfficialPinActions() {
      return this.isAdmin && this.isHomeBoard
    },
    panelTitle() {
      const board = this.currentBoard
      if (!board) return '帖子'
      if (board.boardType === 'HOME') return '首页 · 官方公告'
      if (board.boardType === 'ALL') return '综合讨论（全部）'
      if (board.boardType === 'CATEGORY') return board.name + ' · 全部'
      const parent = flattenBoards(this.boards).find(b => b.id === board.parentId)
      return parent ? `${parent.name} · ${board.name}` : board.name
    },
    canPostHere() {
      if (!this.isLogin) return false
      if (this.isAdmin) return true
      const board = this.currentBoard
      return !isHomeSectionBoard(board, flattenBoards(this.boards))
    }
  },
  async created() {
    const boardsRes = await postApi.boards()
    this.boards = boardsRes.data || []
    if (this.$route.query.boardId) {
      this.activeBoard = String(this.$route.query.boardId)
    } else if (this.boards.length) {
      this.activeBoard = String(this.boards[0].id)
    }
    await this.loadPosts()
  },
  watch: {
    '$route.query.boardId'(id) {
      if (id) {
        this.activeBoard = String(id)
        this.loadPosts()
      }
    }
  },
  methods: {
    async loadPosts() {
      const boardId = Number(this.activeBoard)
      const params = { boardId, page: 1, size: 10 }
      if (this.isAllBoard) {
        params.featuredOnly = this.featuredFilter === 'featured'
      } else if (this.showBoardSort) {
        params.sort = this.postSort
      }
      const res = await postApi.list(params)
      this.posts = res.data.list || []
    },
    goBoard(id) {
      this.activeBoard = id
      const board = flattenBoards(this.boards).find(b => String(b.id) === id)
      if (board && board.boardType === 'ALL') {
        this.featuredFilter = 'all'
      } else if (board && (board.boardType === 'CATEGORY' || board.boardType === 'HOME')) {
        this.postSort = 'hot'
      }
      if (board && (board.boardType === 'ALL' || board.boardType === 'HOME' || board.boardType === 'CATEGORY')) {
        this.$router.replace({ path: '/', query: { boardId: id } }).catch(() => {})
        this.loadPosts()
        return
      }
      this.$router.push(`/board/${id}`)
    },
    goCreate() {
      this.$router.push({ path: '/post/create', query: { boardId: this.activeBoard } })
    },
    async toggleOfficialPin(post) {
      try {
        if (post.officialPinned) {
          await postApi.unpinOfficial(post.id)
          post.officialPinned = false
          this.$message.success('已取消置顶')
        } else {
          await postApi.pinOfficial(post.id)
          post.officialPinned = true
          this.$message.success('已置顶')
        }
        await this.loadPosts()
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || '操作失败')
      }
    },
    findBoardName
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.post-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  flex-wrap: wrap;
}
.toolbar-label { color: #606266; font-size: 13px; }
.sort-select { width: 120px; }
</style>
