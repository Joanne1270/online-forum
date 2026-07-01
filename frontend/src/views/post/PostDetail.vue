<template>
  <el-card v-loading="loading" :body-style="{ padding: 0 }" class="post-detail-card">
    <div class="post-hero">
      <div class="hero-row">
        <el-button type="text" class="back-btn" icon="el-icon-arrow-left" @click="goBack">返回版块</el-button>
        <div class="actions">
          <span v-if="canEdit" class="action-text" @click="$router.push(`/post/${post.id}/edit`)">编辑</span>
          <span v-if="canEdit" class="action-text action-danger" @click="removePost">删除</span>
          <span
            v-if="isLogin && !isAdmin"
            class="action-icon"
            :class="{ 'is-active': post.liked }"
            @click="toggleLike"
          >
            <i class="menu-heart-icon">♡</i>
            <em v-if="post.likeCount" class="action-count">{{ post.likeCount }}</em>
          </span>
          <span
            v-if="isLogin && !isAdmin"
            class="action-icon"
            :class="{ 'is-active': post.favorited }"
            @click="toggleFavorite"
          >
            <i :class="post.favorited ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
          </span>
          <span v-if="isLogin && !canEdit && !isAdmin" class="action-text action-danger" @click="openReport">举报</span>
        </div>
      </div>
      <h2 class="post-title">
        {{ post.title }}
        <span v-if="post.featured" class="featured-tag">精华</span>
      </h2>
      <div class="meta">
        作者：
        <span v-if="post.official">{{ post.authorName }}</span>
        <span v-else-if="post.authorId" class="author-link" @click="goAuthor(post.authorId)">{{ post.authorName }}</span>
        <span v-else>{{ post.authorName }}</span>
        <span v-if="post.boardName"> · 分区：{{ post.boardName }}</span>
        · 浏览 {{ post.viewCount }} · {{ formatTime(post.createdAt) }}
      </div>
      <div v-if="displayTags.length" class="post-tags">
        <span
          v-for="tag in displayTags"
          :key="tag"
          class="post-tag"
          @click="searchTag(tag)"
        >#{{ tag }}</span>
      </div>
    </div>

    <div class="post-body">
      <rich-content
        v-if="hasPostContent"
        :content="post.content"
        :mention-user-ids="post.mentionedUserIds || []"
        class="content"
      />

      <el-divider>回复</el-divider>
      <div v-if="isLogin && !isAdmin" class="reply-box">
        <mention-input ref="replyInput" v-model="replyContent" :show-emoji="false" placeholder="写下回复，输入 @ 可提及用户" />
        <div class="reply-submit-row">
          <emoji-picker @select="insertReplyEmoji" />
          <el-button type="primary" size="mini" @click="submitReply">发表回复</el-button>
        </div>
      </div>
      <div class="reply-sort">
        <span class="sort-label">排序：</span>
        <span class="sort-toggle" @click="toggleReplySort">{{ replySortLabel }}</span>
      </div>
      <reply-tree
        :replies="replies"
        :is-login="isLogin && !isAdmin"
        :current-user-id="user && user.id"
        :is-post-owner="canEdit"
        :allow-pin="canEdit"
        :pinned-reply-id="post.pinnedReplyId"
        :active-reply-id="replyTo"
        :nested-reply="nestedReply"
        @like="toggleReplyLike"
        @dislike="toggleReplyDislike"
        @reply="startReply"
        @delete="deleteReply"
        @report="openReplyReport"
        @pin-reply="pinReply"
        @update:nestedReply="nestedReply = $event"
        @submit-nested="submitNestedReply"
      />
    </div>

    <el-dialog title="举报帖子" :visible.sync="reportVisible" width="480px">
      <el-input v-model="reportReason" type="textarea" :rows="4" placeholder="请说明举报原因" />
      <span slot="footer">
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReport">提交举报</el-button>
      </span>
    </el-dialog>

    <el-dialog title="举报回复" :visible.sync="replyReportVisible" width="480px">
      <el-input v-model="replyReportReason" type="textarea" :rows="4" placeholder="请说明举报原因" />
      <span slot="footer">
        <el-button @click="replyReportVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReplyReport">提交举报</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { mapGetters } from 'vuex'
import { Message } from 'element-ui'
import { postApi } from '../../api/post'
import MentionInput from '../../components/MentionInput.vue'
import ReplyTree from '../../components/ReplyTree.vue'
import RichContent from '../../components/RichContent.vue'
import EmojiPicker from '../../components/EmojiPicker.vue'
import { boardBackRoute } from '../../utils/board'
import { addViewHistory } from '../../utils/viewHistory'
import { extractCompletedTags } from '../../utils/content'

export default {
  components: { MentionInput, ReplyTree, RichContent, EmojiPicker },
  data() {
    return {
      loading: false,
      boards: [],
      post: {},
      replies: [],
      replySort: 'time',
      replyContent: '',
      nestedReply: '',
      replyTo: null,
      replyToAuthor: '',
      reportVisible: false,
      reportReason: '',
      replyReportVisible: false,
      replyReportReason: '',
      replyReportTarget: null
    }
  },
  computed: {
    ...mapGetters(['isLogin', 'isAdmin', 'user']),
    canEdit() {
      return this.isLogin && this.user.id === this.post.authorId
    },
    hasPostContent() {
      return !!(this.post.content && String(this.post.content).trim())
    },
    displayTags() {
      const fromContent = extractCompletedTags(this.post.content || '')
      if (fromContent.length) return fromContent
      return this.post.tags || []
    },
    replySortLabel() {
      return this.replySort === 'hot' ? '按热度' : '按时间'
    }
  },
  created() {
    this.load()
  },
  watch: {
    '$route.query.replyId'() {
      this.$nextTick(() => this.scrollToReply())
    }
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const id = this.$route.params.id
        const [boardsRes, postRes, replyRes] = await Promise.all([
          postApi.boards(),
          postApi.detail(id),
          postApi.replies(id, this.replySort)
        ])
        this.boards = boardsRes.data || []
        this.post = postRes.data
        this.replies = replyRes.data || []
        if (this.isLogin && this.user.id) {
          addViewHistory(this.post, this.user.id)
        }
      } finally {
        this.loading = false
        this.$nextTick(() => this.scrollToReply())
      }
    },
    async loadReplies() {
      const id = this.$route.params.id
      const replyRes = await postApi.replies(id, this.replySort)
      this.replies = replyRes.data || []
    },
    toggleReplySort() {
      this.replySort = this.replySort === 'hot' ? 'time' : 'hot'
      this.loadReplies()
    },
    scrollToReply() {
      const replyId = this.$route.query.replyId
      if (!replyId) return
      const el = document.getElementById(`reply-${replyId}`)
      if (el) {
        el.classList.add('reply-highlight')
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    },
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    goAuthor(id) {
      this.$router.push(`/user/${id}`)
    },
    searchTag(tag) {
      this.$router.push({ path: '/search', query: { keyword: `#${tag}` } })
    },
    goBack() {
      const route = boardBackRoute(this.post.boardId, this.boards)
      this.$router.push(route)
    },
    async toggleLike() {
      if (this.post.liked) {
        await postApi.unlike(this.post.id)
      } else {
        await postApi.like(this.post.id)
      }
      await this.load()
    },
    async toggleFavorite() {
      if (this.post.favorited) {
        await postApi.unfavorite(this.post.id)
        this.$message.success('已取消收藏')
      } else {
        await postApi.favorite(this.post.id)
        this.$message.success('收藏成功')
      }
      await this.load()
    },
    openReport() {
      this.reportReason = ''
      this.reportVisible = true
    },
    async submitReport() {
      if (!this.reportReason.trim()) {
        this.$message.warning('请填写举报原因')
        return
      }
      await postApi.report(this.post.id, { reason: this.reportReason.trim() })
      this.reportVisible = false
      this.$message.success('举报已提交，等待官方处理')
    },
    async submitReply() {
      const content = (this.replyContent || '').trim()
      if (!content) {
        Message({ message: '输入内容不能为空', type: 'warning', duration: 2500, showClose: false })
        return
      }
      await postApi.createReply(this.post.id, { content })
      this.replyContent = ''
      await this.loadReplies()
    },
    insertReplyEmoji(emoji) {
      const ref = this.$refs.replyInput
      const input = Array.isArray(ref) ? ref[0] : ref
      if (input && input.insertEmoji) input.insertEmoji(emoji)
    },
    startReply(reply) {
      if (this.replyTo === reply.id) {
        this.replyTo = null
        this.replyToAuthor = ''
        this.nestedReply = ''
        return
      }
      this.replyTo = reply.id
      this.replyToAuthor = reply.authorName
      this.nestedReply = ''
    },
    async submitNestedReply() {
      const content = (this.nestedReply || '').trim()
      if (!content) {
        Message({ message: '输入内容不能为空', type: 'warning', duration: 2500, showClose: false })
        return
      }
      await postApi.createReply(this.post.id, { content, parentId: this.replyTo })
      this.nestedReply = ''
      this.replyTo = null
      this.replyToAuthor = ''
      await this.loadReplies()
    },
    async toggleReplyLike(reply) {
      if (reply.liked) {
        await postApi.unlikeReply(reply.id)
      } else {
        await postApi.likeReply(reply.id)
      }
      await this.loadReplies()
    },
    async toggleReplyDislike(reply) {
      if (reply.disliked) {
        await postApi.undislikeReply(reply.id)
      } else {
        await postApi.dislikeReply(reply.id)
      }
      await this.loadReplies()
    },
    async deleteReply(id) {
      await this.$confirm('确认删除该回复？')
      await postApi.deleteReply(id)
      this.$message.success('已删除')
      await this.loadReplies()
    },
    async pinReply(reply) {
      try {
        if (reply.pinned) {
          await postApi.unpinReply(this.post.id)
          this.$message.success('已取消置顶回复')
        } else {
          await postApi.pinReply(this.post.id, reply.id)
          this.$message.success('已置顶该回复')
        }
        const postRes = await postApi.detail(this.post.id)
        this.post = postRes.data
        await this.loadReplies()
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || '操作失败')
      }
    },
    openReplyReport(reply) {
      this.replyReportTarget = reply
      this.replyReportReason = ''
      this.replyReportVisible = true
    },
    async submitReplyReport() {
      if (!this.replyReportReason.trim()) {
        this.$message.warning('请填写举报原因')
        return
      }
      await postApi.reportReply(this.replyReportTarget.id, { reason: this.replyReportReason.trim() })
      this.replyReportVisible = false
      this.$message.success('举报已提交，等待官方处理')
    },
    async removePost() {
      await this.$confirm('确认删除该帖子？')
      const boardId = this.post.boardId
      await postApi.remove(this.post.id)
      const route = boardBackRoute(boardId, this.boards)
      this.$router.push(route)
    }
  }
}
</script>

<style scoped>
.post-detail-card ::v-deep .el-card__header {
  display: none;
}
.post-hero {
  background: #409EFF;
  color: #fff;
  padding: 14px 20px 22px;
}
.hero-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.back-btn {
  color: rgba(255, 255, 255, 0.95) !important;
  padding-left: 0;
}
.post-title {
  margin: 14px 0 12px;
  color: #fff;
  font-size: 22px;
  line-height: 1.4;
  font-weight: 600;
}
.featured-tag {
  display: inline-block;
  margin-left: 8px;
  font-size: 12px;
  font-weight: normal;
  color: #fdf6ec;
  background: rgba(230, 162, 60, 0.35);
  border: 1px solid rgba(245, 218, 177, 0.8);
  padding: 2px 8px;
  border-radius: 4px;
  vertical-align: middle;
}
.meta {
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  line-height: 1.6;
}
.author-link {
  color: #fff;
  cursor: pointer;
  text-decoration: underline;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  justify-content: flex-end;
}
.action-text {
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}
.action-text:hover {
  color: #ffd04b;
}
.action-text.action-danger {
  color: #ffc9c9;
}
.action-text.action-danger:hover {
  color: #fff;
}
.action-icon {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  font-size: 18px;
  line-height: 1;
}
.action-icon:hover {
  color: #ffd04b;
}
.action-icon.is-active {
  color: #ffd04b;
}
.action-icon.is-active .menu-heart-icon {
  color: #ff8787;
}
.action-count {
  font-style: normal;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.88);
}
.menu-heart-icon {
  display: inline-block;
  width: 18px;
  text-align: center;
  font-style: normal;
  font-size: 18px;
  line-height: 1;
  color: inherit;
}
.post-body {
  padding: 20px;
}
.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.post-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}
.post-tag:hover {
  background: rgba(255, 255, 255, 0.32);
}
.content { min-height: 120px; }
.reply-box { margin-bottom: 16px; }
.reply-submit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.reply-sort { margin-bottom: 12px; font-size: 13px; }
.sort-label { color: #606266; }
.sort-toggle {
  color: #409EFF;
  cursor: pointer;
  font-weight: 600;
}
.sort-toggle:hover { color: #1864ab; }
::v-deep .mention,
::v-deep .mention-link {
  color: #0D47A1;
  font-weight: 500;
}
::v-deep .reply-highlight { background: #fdf6ec; transition: background 0.3s; }
</style>
