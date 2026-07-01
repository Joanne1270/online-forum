<template>
  <el-card>
    <div slot="header" class="header">
      <span>通知中心</span>
      <el-button size="mini" @click="markAll">全部已读</el-button>
    </div>
    <div
      v-for="n in list"
      :key="n.id"
      class="item"
      :class="{ unread: isUnread(n), clickable: canNavigate(n) }"
      @click="handleClick(n)"
    >
      <div>{{ displayContent(n) }}</div>
      <div class="time">{{ formatTime(n.createdAt) }}</div>
    </div>
    <el-empty v-if="!list.length" description="暂无通知" :image-size="60" />
  </el-card>
</template>

<script>
import { notificationApi } from '../../api/notification'
import { postApi } from '../../api/post'
import { userApi } from '../../api/user'

const PROFILE_TYPES = ['profile-approved', 'profile-rejected']
const ACTION_SUFFIXES = ['点赞了你的帖子', '点赞了你的回复', '收藏了你的帖子', '回复了你的帖子', '在内容中 @ 了你']

export default {
  data() {
    return { list: [], nicknameMap: {} }
  },
  created() {
    this.load()
  },
  methods: {
    isUnread(n) {
      return Number(n.readFlag) === 0
    },
    canNavigate(n) {
      return !PROFILE_TYPES.includes(n.type)
    },
    displayContent(n) {
      const nickname = this.nicknameMap[n.fromUserId]
      if (!nickname || !n.content) return n.content
      for (const suffix of ACTION_SUFFIXES) {
        if (n.content.includes(suffix)) {
          return `${nickname} ${suffix}`
        }
      }
      const space = n.content.indexOf(' ')
      if (space > 0) {
        return nickname + n.content.slice(space)
      }
      return n.content
    },
    async load() {
      const res = await notificationApi.list({ page: 1, size: 50 })
      this.list = res.data.list || []
      const ids = [...new Set(this.list.filter(n => n.fromUserId).map(n => n.fromUserId))]
      const map = {}
      await Promise.all(ids.map(async id => {
        try {
          const u = await userApi.getUser(id)
          if (u.data && u.data.nickname) {
            map[id] = u.data.nickname
          }
        } catch (e) {
          // ignore
        }
      }))
      this.nicknameMap = map
    },
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    refreshBadge() {
      this.$root.$emit('refresh-unread')
    },
    async markOne(n) {
      if (this.isUnread(n)) {
        await notificationApi.markRead(n.id)
        n.readFlag = 1
        this.refreshBadge()
      }
    },
    async markAll() {
      await notificationApi.markAllRead()
      this.list.forEach(n => { n.readFlag = 1 })
      this.refreshBadge()
    },
    async resolvePostId(n) {
      if (n.postId) return n.postId
      if (n.refType === 'POST' && n.refId) return n.refId
      if (n.refType === 'REPLY' && n.refId) {
        try {
          const res = await postApi.replyPostId(n.refId)
          return res.data
        } catch (e) {
          return null
        }
      }
      return null
    },
    async isPostAvailable(postId) {
      try {
        const res = await postApi.exists(postId)
        return res.data === true
      } catch (e) {
        return false
      }
    },
    async handleClick(n) {
      await this.markOne(n)
      if (PROFILE_TYPES.includes(n.type)) {
        this.$router.push('/profile')
        return
      }
      const postId = await this.resolvePostId(n)
      if (!postId || !(await this.isPostAvailable(postId))) {
        this.$message.warning('该帖子已被删除')
        return
      }
      const query = n.refType === 'REPLY' && n.refId ? { replyId: n.refId } : {}
      this.$router.push({ path: `/post/${postId}`, query })
    }
  }
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
.item { padding: 12px; border-bottom: 1px solid #eee; }
.clickable { cursor: pointer; }
.clickable:hover { background: #f5f7fa; }
.unread { background: #ecf5ff; }
.time { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
