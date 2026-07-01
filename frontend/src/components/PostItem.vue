<template>
  <div class="post-item" @click="handleClick">
    <div class="title-row">
      <span class="title">{{ post.title }}</span>
      <span v-if="post.featured" class="tag featured">精华</span>
      <span v-if="post.officialPinned" class="tag pinned">置顶</span>
      <span v-if="post.profilePinned" class="tag profile-pin">置顶</span>
    </div>
    <div v-if="contentPreview" class="excerpt">{{ contentPreview }}</div>
    <div class="meta">
      <span
        v-if="post.official"
        class="author"
      >{{ post.authorName }}</span>
      <span
        v-else-if="post.authorId"
        class="author link"
        @click.stop="goAuthor(post.authorId)"
      >{{ post.authorName }}</span>
      <span v-else class="author">{{ post.authorName }}</span>
      <span>浏览 {{ post.viewCount }}</span>
      <span>回复 {{ post.replyCount }}</span>
      <span>点赞 {{ post.likeCount }}</span>
      <span>{{ formatTime(post.createdAt) }}</span>
    </div>
    <div v-if="showActions" class="actions" @click.stop>
      <el-button
        v-if="showProfilePin"
        type="text"
        size="mini"
        @click="$emit('toggle-profile-pin', post)"
      >{{ post.profilePinned ? '取消置顶' : '置顶' }}</el-button>
      <el-button
        v-if="showOfficialPin"
        type="text"
        size="mini"
        @click="$emit('toggle-official-pin', post)"
      >{{ post.officialPinned ? '取消置顶' : '公告置顶' }}</el-button>
    </div>
  </div>
</template>

<script>
import { postContentPreview } from '../utils/content'

export default {
  props: {
    post: { type: Object, required: true },
    showProfilePin: { type: Boolean, default: false },
    showOfficialPin: { type: Boolean, default: false }
  },
  computed: {
    showActions() {
      return this.showProfilePin || this.showOfficialPin
    },
    contentPreview() {
      return postContentPreview(this.post.content)
    }
  },
  methods: {
    handleClick() {
      this.$router.push(`/post/${this.post.id}`)
    },
    formatTime(t) {
      if (!t) return ''
      return String(t).replace('T', ' ').slice(0, 16)
    },
    goAuthor(id) {
      this.$router.push(`/user/${id}`)
    }
  }
}
</script>

<style scoped>
.post-item {
  padding: 12px 20px;
  margin: 0 -20px;
  border-bottom: 1px solid rgba(235, 238, 245, 0.85);
  cursor: pointer;
  transition: background-color 0.15s ease;
}
.post-item:hover,
.post-item:active {
  background: rgba(255, 255, 255, 0.92);
}
.title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.title { font-size: 16px; font-weight: 600; }
.excerpt {
  margin: 4px 0 8px;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  line-height: 18px;
  flex-shrink: 0;
}
.tag.featured { background: #fdf6ec; color: #e6a23c; border: 1px solid #f5dab1; }
.tag.pinned, .tag.profile-pin { background: #ecf5ff; color: #409EFF; border: 1px solid #b3d8ff; }
.meta { color: #909399; font-size: 13px; display: flex; gap: 12px; flex-wrap: wrap; }
.author.link { color: #409EFF; cursor: pointer; }
.author.link:hover { text-decoration: underline; }
.actions { margin-top: 6px; }
</style>
