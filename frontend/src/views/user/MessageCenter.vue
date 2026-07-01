<template>
  <div class="message-center" :class="{ 'notice-mode': category !== 'chat' }">
    <!-- 左侧分类 -->
    <aside class="sidebar">
      <div class="sidebar-title">
        <i class="el-icon-message" />
        消息中心
      </div>
      <div
        v-for="cat in categories"
        :key="cat.key"
        class="sidebar-item"
        :class="{ active: category === cat.key }"
        @click="switchCategory(cat.key)"
      >
        <i :class="cat.icon" />
        <span>{{ cat.label }}</span>
        <span v-if="unreadBadge(cat.key) > 0" class="sidebar-badge">{{ unreadBadge(cat.key) }}</span>
      </div>
    </aside>

    <!-- 中间列表 -->
    <section class="list-panel">
      <div class="panel-header">
        <span>{{ currentCategoryLabel }}</span>
        <span v-if="category !== 'chat'" class="mark-all" @click="markAllCurrent">全部已读</span>
      </div>

      <!-- 私信会话列表 -->
      <div v-if="category === 'chat'" v-loading="listLoading" class="list-body">
        <div
          v-for="item in conversations"
          :key="item.peerId"
          class="list-item"
          :class="{ active: selectedPeerId === item.peerId }"
          @click="selectPeer(item.peerId)"
        >
          <el-avatar :size="44" :src="item.peerAvatar" icon="el-icon-user-solid" />
          <div class="item-main">
            <div class="item-top">
              <span class="item-name">{{ item.peerNickname }}</span>
              <span class="item-time">{{ formatTime(item.lastTime) }}</span>
            </div>
            <div class="item-bottom">
              <span class="item-preview">{{ previewContent(item.lastContent) }}</span>
              <span v-if="item.unreadCount > 0" class="item-badge">{{ item.unreadCount }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!listLoading && !conversations.length" description="暂无私信" :image-size="70" />
      </div>

      <!-- 通知列表 -->
      <div v-else v-loading="listLoading" class="list-body notice-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notice-item"
          :class="{ unread: isUnread(n), clickable: isNoticeClickable(n) }"
          @click="selectNotification(n)"
        >
          <el-avatar
            :size="48"
            :src="userProfileMap[n.fromUserId] && userProfileMap[n.fromUserId].avatar"
            icon="el-icon-user-solid"
            class="notice-avatar"
          />
          <div class="notice-main">
            <div class="notice-head">
              <span class="notice-name">{{ displaySender(n) }}</span>
              <span class="notice-action">{{ displayActionText(n) }}</span>
            </div>
            <div v-if="displayQuote(n)" class="notice-quote">{{ displayQuote(n) }}</div>
            <div v-else-if="category === 'system'" class="notice-quote">{{ displaySystemText(n) }}</div>
          </div>
          <div class="notice-meta">
            <span class="notice-time">{{ formatRelativeTime(n.createdAt) }}</span>
            <span v-if="displayPostRef(n)" class="notice-post-ref">{{ displayPostRef(n) }}</span>
          </div>
        </div>
        <el-empty v-if="!listLoading && !notifications.length" description="暂无消息" :image-size="70" />
      </div>
    </section>

    <!-- 右侧详情 / 聊天（仅私信） -->
    <section v-if="category === 'chat'" class="detail-panel">
        <div v-if="selectedPeerId" class="chat-wrap">
          <div class="chat-header">
            <div class="peer-info">
              <el-avatar :size="36" :src="peer.avatar" icon="el-icon-user-solid" />
              <span class="peer-name">{{ peer.nickname || '用户' }}</span>
            </div>
            <el-button type="text" @click="goProfile(selectedPeerId)">查看主页</el-button>
          </div>
          <div ref="messageBox" v-loading="chatLoading" class="message-box">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-row"
              :class="{ mine: msg.mine }"
            >
              <template v-if="!msg.mine">
                <el-avatar
                  :size="36"
                  :src="peer.avatar"
                  icon="el-icon-user-solid"
                  class="msg-avatar"
                />
                <div class="bubble">
                  <rich-content :content="msg.content" class="content" />
                  <div class="time">{{ formatTime(msg.createdAt) }}</div>
                </div>
              </template>
              <template v-else>
                <div class="bubble">
                  <rich-content :content="msg.content" class="content" />
                  <div class="time">{{ formatTime(msg.createdAt) }}</div>
                </div>
                <el-avatar
                  :size="36"
                  :src="myAvatar"
                  icon="el-icon-user-solid"
                  class="msg-avatar"
                />
              </template>
            </div>
            <el-empty v-if="!chatLoading && !messages.length" description="暂无消息，发送第一条私信吧" :image-size="60" />
          </div>
          <div class="composer">
            <div class="composer-toolbar">
              <emoji-picker @select="insertEmoji" />
              <el-upload
                action="#"
                :http-request="uploadImage"
                :show-file-list="false"
                accept="image/*"
                class="image-upload"
              >
                <button type="button" class="image-trigger" title="图片" @mousedown.prevent>
                  <i class="el-icon-picture-outline" />
                </button>
              </el-upload>
            </div>
            <el-input
              ref="draftInput"
              v-model="draft"
              type="textarea"
              :rows="3"
              maxlength="2000"
              placeholder="请输入消息内容"
              @keyup.ctrl.enter.native="sendMessage"
            />
            <div v-if="draftMedia.length" class="draft-media-list">
              <div v-for="(item, index) in draftMedia" :key="index" class="draft-media-item">
                <img :src="item.url" class="draft-media-thumb" alt="图片" />
                <button type="button" class="draft-media-remove" title="移除" @click="removeDraftMedia(index)">
                  <i class="el-icon-close" />
                </button>
              </div>
            </div>
            <el-button type="primary" :loading="sending" @click="sendMessage">发送</el-button>
          </div>
        </div>
        <div v-else class="detail-empty">
          <i class="el-icon-chat-dot-round empty-icon" />
          <p>选择一个会话开始聊天</p>
        </div>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { messageApi } from '../../api/message'
import { notificationApi } from '../../api/notification'
import { userApi } from '../../api/user'
import { postApi } from '../../api/post'
import EmojiPicker from '../../components/EmojiPicker.vue'
import RichContent from '../../components/RichContent.vue'
import { fileApi } from '../../api/file'
import { insertTextAtCursor, focusTextarea } from '../../utils/emojis'

const CATEGORIES = [
  { key: 'chat', label: '我的消息', icon: 'el-icon-chat-dot-round' },
  { key: 'reply', label: '回复我的', icon: 'el-icon-chat-line-round' },
  { key: 'mention', label: '@ 我的', icon: 'el-icon-attract' },
  { key: 'like', label: '赞和收藏', icon: 'el-icon-star-off' },
  { key: 'system', label: '系统通知', icon: 'el-icon-bell' }
]

const PROFILE_TYPES = ['profile-approved', 'profile-rejected']
const REPLY_MARKER = '回复了你的帖子：'
const LIKE_POST_MARKER = '点赞了你的帖子：'
const LIKE_POST_MARKER_LEGACY = '赞了你的帖子：'
const LIKE_REPLY_MARKER = '点赞了你的回复：'
const LIKE_REPLY_MARKER_LEGACY = '赞了你的回复：'
const FAVORITE_POST_MARKER = '收藏了你的帖子：'
const NAVIGATE_CATEGORIES = ['reply', 'like', 'mention']

function buildDraftContent(text, mediaItems) {
  const parts = [(text || '').trim()]
  for (const item of mediaItems) {
    parts.push(`![image](${item.url})`)
  }
  return parts.filter(Boolean).join('\n\n')
}

export default {
  components: { EmojiPicker, RichContent },
  data() {
    return {
      categories: CATEGORIES,
      category: 'chat',
      listLoading: false,
      chatLoading: false,
      sending: false,
      conversations: [],
      notifications: [],
      notificationUnread: {},
      messageUnread: 0,
      selectedPeerId: null,
      selectedNotification: null,
      peer: {},
      messages: [],
      draft: '',
      draftMedia: [],
      nicknameMap: {},
      userProfileMap: {},
      replySnippets: {},
      postTitles: {},
      pollTimer: null
    }
  },
  computed: {
    ...mapGetters(['user']),
    myAvatar() {
      return this.user && this.user.avatar
    },
    currentCategoryLabel() {
      const cat = CATEGORIES.find(c => c.key === this.category)
      return cat ? cat.label : '消息'
    }
  },
  watch: {
    '$route'(to) {
      this.syncRoute()
    },
    category() {
      this.selectedNotification = null
      this.loadList()
    },
    selectedPeerId(id) {
      if (id && this.category === 'chat') {
        this.loadChat(id)
      }
    }
  },
  created() {
    this.syncRoute()
    this.loadUnreadSummary()
    this.loadList()
    if (this.selectedPeerId && this.category === 'chat') {
      this.loadChat(this.selectedPeerId)
    }
    this.pollTimer = setInterval(this.refreshCurrent, 5000)
  },
  beforeDestroy() {
    clearInterval(this.pollTimer)
  },
  methods: {
    syncRoute() {
      const queryCategory = this.$route.query.category
      if (CATEGORIES.some(c => c.key === queryCategory)) {
        this.category = queryCategory
      }
      const chatPeer = this.$route.params.userId
      if (chatPeer) {
        this.category = 'chat'
        this.selectedPeerId = Number(chatPeer)
      }
    },
    unreadBadge(key) {
      if (key === 'chat') return this.messageUnread
      return this.notificationUnread[key] || 0
    },
    switchCategory(key) {
      this.category = key
      this.selectedPeerId = null
      this.selectedNotification = null
      const query = key === 'chat' ? {} : { category: key }
      if (this.$route.path.startsWith('/messages')) {
        this.$router.replace({ path: '/messages', query }).catch(() => {})
      }
    },
    async loadUnreadSummary() {
      try {
        const [msgRes, noteRes] = await Promise.all([
          messageApi.unreadCount(),
          notificationApi.unreadSummary()
        ])
        this.messageUnread = msgRes.data || 0
        this.notificationUnread = (noteRes.data && noteRes.data.byCategory) || {}
      } catch (e) {
        this.messageUnread = 0
        this.notificationUnread = {}
      }
      this.$root.$emit('refresh-message-unread')
      this.$root.$emit('refresh-unread')
    },
    async loadList() {
      this.listLoading = true
      try {
        if (this.category === 'chat') {
          const res = await messageApi.conversations()
          this.conversations = res.data || []
          if (this.selectedPeerId && !this.conversations.find(c => c.peerId === this.selectedPeerId)) {
            await this.loadPeerForChat(this.selectedPeerId)
          }
        } else {
          const res = await notificationApi.list({ page: 1, size: 50, category: this.category })
          this.notifications = res.data.list || []
          await this.loadUserProfiles(this.notifications)
          if (this.category === 'reply') {
            await this.loadReplySnippets(this.notifications)
            await this.loadPostTitles(this.notifications)
          }
          if (this.category === 'like') {
            await this.loadPostTitles(this.notifications)
          }
          if (this.category === 'mention') {
            await this.loadPostTitles(this.notifications)
          }
          if (this.category === 'system') {
            await this.loadPostTitles(this.notifications)
          }
        }
      } finally {
        this.listLoading = false
      }
    },
    async loadPeerForChat(peerId) {
      try {
        const res = await userApi.getUser(peerId)
        if (res.data) {
          this.peer = res.data
        }
      } catch (e) {
        // ignore
      }
    },
    async loadUserProfiles(list) {
      const ids = [...new Set(list.filter(n => n.fromUserId).map(n => n.fromUserId))]
      const map = { ...this.userProfileMap }
      const names = { ...this.nicknameMap }
      await Promise.all(ids.map(async id => {
        if (map[id]) return
        try {
          const u = await userApi.getUser(id)
          if (u.data) {
            map[id] = { nickname: u.data.nickname, avatar: u.data.avatar }
            if (u.data.nickname) names[id] = u.data.nickname
          }
        } catch (e) {
          // ignore
        }
      }))
      this.userProfileMap = map
      this.nicknameMap = names
    },
    async loadPostTitles(list) {
      const postIds = []
      for (const n of list) {
        if (n.type === 'reply-created' || n.type === 'mention') {
          if (n.postId) {
            postIds.push(n.postId)
          } else if (n.refType === 'POST' && n.refId) {
            postIds.push(n.refId)
          } else if (n.refType === 'REPLY' && n.refId) {
            try {
              const res = await postApi.replyPostId(n.refId)
              if (res.data) {
                n._resolvedPostId = res.data
                postIds.push(res.data)
              }
            } catch (e) {
              // ignore
            }
          }
          continue
        }
        if (n.type === 'like' || n.type === 'favorite') {
          if (n.refType === 'POST') {
            if (n.content && (n.content.includes(LIKE_POST_MARKER) || n.content.includes(FAVORITE_POST_MARKER)
              || n.content.includes(LIKE_POST_MARKER_LEGACY))) {
              continue
            }
            if (n.postId || n.refId) postIds.push(n.postId || n.refId)
          } else if (n.postId) {
            postIds.push(n.postId)
          }
        }
        if (n.type === 'reply-moderated' && (n.postId || n.refId)) {
          postIds.push(n.postId || n.refId)
        }
      }
      const titles = { ...this.postTitles }
      await Promise.all([...new Set(postIds)].map(async id => {
        if (titles[id]) return
        try {
          const res = await postApi.detail(id)
          if (res.data && res.data.title) titles[id] = res.data.title
        } catch (e) {
          // ignore
        }
      }))
      this.postTitles = titles
    },
    selectPeer(peerId) {
      this.selectedPeerId = peerId
      this.$router.replace({ path: `/messages/chat/${peerId}` }).catch(() => {})
    },
    async loadReplySnippets(list) {
      const replyIds = list
        .filter(n => n.type === 'reply-created' && n.refType === 'REPLY' && n.refId)
        .filter(n => !n.content || !n.content.includes(REPLY_MARKER))
        .map(n => n.refId)
      const snippets = { ...this.replySnippets }
      await Promise.all(replyIds.map(async id => {
        if (snippets[id]) return
        try {
          const res = await postApi.replyPreview(id)
          if (res.data) snippets[id] = res.data
        } catch (e) {
          // ignore
        }
      }))
      this.replySnippets = snippets
    },
    selectNotification(n) {
      if (this.isUnread(n)) {
        notificationApi.markRead(n.id).then(() => {
          n.readFlag = 1
          this.loadUnreadSummary()
        })
      }
      if (NAVIGATE_CATEGORIES.includes(this.category) && this.canNavigate(n)) {
        this.openNotificationTarget(n)
        return
      }
      if (this.category === 'system' && n.type === 'reply-moderated' && this.canNavigate(n)) {
        this.openNotificationTarget(n)
        return
      }
      if (this.category === 'system' && PROFILE_TYPES.includes(n.type)) {
        this.$router.push('/profile')
      }
    },
    async loadChat(peerId) {
      this.chatLoading = true
      try {
        const [peerRes, msgRes] = await Promise.all([
          userApi.getUser(peerId),
          messageApi.listWith(peerId, { page: 1, size: 100 })
        ])
        this.peer = peerRes.data || {}
        this.messages = msgRes.data.list || []
        this.loadUnreadSummary()
        this.$nextTick(this.scrollToBottom)
      } finally {
        this.chatLoading = false
      }
    },
    async refreshCurrent() {
      if (this.category === 'chat' && this.selectedPeerId) {
        try {
          const msgRes = await messageApi.listWith(this.selectedPeerId, { page: 1, size: 100 })
          this.messages = msgRes.data.list || []
          const convRes = await messageApi.conversations()
          this.conversations = convRes.data || []
          this.loadUnreadSummary()
        } catch (e) {
          // ignore
        }
      }
    },
    async sendMessage() {
      const content = buildDraftContent(this.draft, this.draftMedia)
      if (!content || !this.selectedPeerId) return
      if (content.length > 2000) {
        this.$message.warning('消息内容过长')
        return
      }
      this.sending = true
      try {
        const res = await messageApi.send(this.selectedPeerId, { content })
        this.messages.push(res.data)
        this.draft = ''
        this.draftMedia = []
        this.loadUnreadSummary()
        const convRes = await messageApi.conversations()
        this.conversations = convRes.data || []
        this.$nextTick(this.scrollToBottom)
      } finally {
        this.sending = false
      }
    },
    scrollToBottom() {
      const box = this.$refs.messageBox
      if (box) box.scrollTop = box.scrollHeight
    },
    getDraftTextarea() {
      const input = this.$refs.draftInput
      return input && input.$refs && input.$refs.textarea
    },
    insertEmoji(emoji) {
      const textarea = this.getDraftTextarea()
      const result = insertTextAtCursor(textarea, emoji, this.draft)
      this.draft = result.value
      this.$nextTick(() => focusTextarea(textarea, result.pos))
    },
    async uploadImage({ file }) {
      try {
        const res = await fileApi.upload(file, 'media')
        this.draftMedia.push({ url: res.data.url })
        this.$message.success('图片已添加')
      } catch (e) {
        this.$message.error('图片上传失败')
      }
    },
    removeDraftMedia(index) {
      this.draftMedia.splice(index, 1)
    },
    previewContent(content) {
      if (!content) return ''
      return content
        .replace(/!\[[^\]]*\]\([^)]+\)/g, '[图片]')
        .replace(/<video[^>]*>[\s\S]*?<\/video>/g, '[视频]')
        .replace(/\[([^\]]+)\]\([^)]+\)/g, '[$1]')
        .replace(/\s+/g, ' ')
        .trim()
    },
    async markAllCurrent() {
      await notificationApi.markAllRead(this.category)
      this.notifications.forEach(n => { n.readFlag = 1 })
      this.loadUnreadSummary()
      this.$message.success('已全部标记为已读')
    },
    isUnread(n) {
      return Number(n.readFlag) === 0
    },
    canNavigate(n) {
      if (PROFILE_TYPES.includes(n.type)) return false
      if (n.type === 'reply-moderated') return !!(n.postId || n.refId)
      return true
    },
    isNoticeClickable(n) {
      if (!this.canNavigate(n)) return false
      if (NAVIGATE_CATEGORIES.includes(this.category)) return true
      return this.category === 'system' && n.type === 'reply-moderated'
    },
    displaySender(n) {
      if (this.category === 'system' || !n.fromUserId) return '系统通知'
      return this.nicknameMap[n.fromUserId] || '用户'
    },
    displayActionText(n) {
      if (n.type === 'reply-created') return '回复了你的帖子'
      if (n.type === 'favorite') return '收藏了你的帖子'
      if (n.type === 'like') {
        return n.refType === 'REPLY' ? '点赞了你的回复' : '点赞了你的帖子'
      }
      if (n.type === 'mention') return '@ 了你'
      return ''
    },
    displayQuote(n) {
      if (n.type === 'reply-created') {
        if (n.content && n.content.includes(REPLY_MARKER)) {
          return n.content.split(REPLY_MARKER).slice(1).join(REPLY_MARKER)
        }
        if (n.refId && this.replySnippets[n.refId]) return this.replySnippets[n.refId]
        return ''
      }
      if (n.type === 'like' && n.refType === 'REPLY') {
        if (n.content && n.content.includes(LIKE_REPLY_MARKER)) {
          return n.content.split(LIKE_REPLY_MARKER).slice(1).join(LIKE_REPLY_MARKER)
        }
        if (n.content && n.content.includes(LIKE_REPLY_MARKER_LEGACY)) {
          return n.content.split(LIKE_REPLY_MARKER_LEGACY).slice(1).join(LIKE_REPLY_MARKER_LEGACY)
        }
        return ''
      }
      if (n.type === 'like' || n.type === 'favorite') {
        if (n.content && n.content.includes(LIKE_POST_MARKER)) {
          return n.content.split(LIKE_POST_MARKER).slice(1).join(LIKE_POST_MARKER)
        }
        if (n.content && n.content.includes(LIKE_POST_MARKER_LEGACY)) {
          return n.content.split(LIKE_POST_MARKER_LEGACY).slice(1).join(LIKE_POST_MARKER_LEGACY)
        }
        if (n.content && n.content.includes(FAVORITE_POST_MARKER)) {
          return n.content.split(FAVORITE_POST_MARKER).slice(1).join(FAVORITE_POST_MARKER)
        }
        const postId = n.postId || (n.refType === 'POST' ? n.refId : null)
        if (postId && this.postTitles[postId]) return this.postTitles[postId]
        if (n.content && n.content.includes('《') && n.content.includes('》')) {
          const m = n.content.match(/《([^》]+)》/)
          if (m) return m[1]
        }
        return ''
      }
      if (n.type === 'mention') {
        const postId = n.postId || n._resolvedPostId || (n.refType === 'POST' ? n.refId : null)
        if (postId && this.postTitles[postId]) return this.postTitles[postId]
        return ''
      }
      return ''
    },
    displaySystemText(n) {
      return n.content || ''
    },
    displayPostRef(n) {
      if (n.type === 'reply-moderated') {
        const postId = n.postId || n.refId
        if (postId && this.postTitles[postId]) return this.postTitles[postId]
        return ''
      }
      if (n.type === 'reply-created') {
        const postId = n.postId || n._resolvedPostId
        if (postId && this.postTitles[postId]) return this.postTitles[postId]
        return ''
      }
      if ((n.type === 'like' || n.type === 'favorite') && n.refType === 'REPLY') {
        const postId = n.postId
        if (postId && this.postTitles[postId]) return this.postTitles[postId]
      }
      return ''
    },
    formatRelativeTime(t) {
      if (!t) return ''
      const date = new Date(String(t).replace(' ', 'T'))
      const diff = Date.now() - date.getTime()
      const mins = Math.floor(diff / 60000)
      if (mins < 1) return '刚刚'
      if (mins < 60) return `${mins} 分钟前`
      const hours = Math.floor(mins / 60)
      if (hours < 24) return `${hours} 小时前`
      const days = Math.floor(hours / 24)
      if (days === 1) return '昨天 ' + String(t).replace('T', ' ').slice(11, 16)
      if (days < 7) return `${days} 天前`
      return String(t).replace('T', ' ').slice(0, 16)
    },
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    goProfile(id) {
      this.$router.push(`/user/${id}`)
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
    async openNotificationTarget(n) {
      if (PROFILE_TYPES.includes(n.type)) {
        this.$router.push('/profile')
        return
      }
      const postId = await this.resolvePostId(n)
      if (!postId) {
        this.$message.warning('相关内容不存在')
        return
      }
      try {
        const res = await postApi.exists(postId)
        if (res.data !== true) {
          this.$message.warning('该帖子已被删除')
          return
        }
      } catch (e) {
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
.message-center {
  display: flex;
  width: 100%;
  height: calc(100vh - 100px);
  min-height: 560px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.sidebar {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid #e3e5e7;
  background: #fafbfc;
  padding: 16px 0;
}
.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px 16px;
  font-size: 16px;
  font-weight: 700;
  color: #18191c;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  font-size: 14px;
  color: #61666d;
  cursor: pointer;
  position: relative;
}
.sidebar-item i { font-size: 16px; width: 18px; text-align: center; }
.sidebar-item:hover { background: #f1f2f3; color: #409EFF; }
.sidebar-item.active {
  background: #ecf5ff;
  color: #409EFF;
  font-weight: 600;
}
.sidebar-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: #409EFF;
  border-radius: 0 2px 2px 0;
}
.sidebar-badge {
  margin-left: auto;
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
}

.list-panel {
  width: 300px;
  flex-shrink: 0;
  border-right: 1px solid #e3e5e7;
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #e3e5e7;
  font-weight: 600;
  color: #18191c;
}
.mark-all {
  font-size: 12px;
  color: #409EFF;
  cursor: pointer;
  font-weight: normal;
}
.list-body {
  flex: 1;
  overflow-y: auto;
}
.list-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f1f2f3;
}
.list-item:hover { background: #f7f8fa; }
.list-item.active { background: #ecf5ff; }
.list-item.unread { background: #f5faff; }
.item-main { flex: 1; min-width: 0; }
.item-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.item-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-time { font-size: 12px; color: #909399; flex-shrink: 0; }
.item-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
}
.item-preview {
  flex: 1;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  flex-shrink: 0;
}

.detail-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}
.chat-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e3e5e7;
}
.peer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.peer-name { font-size: 16px; font-weight: 600; color: #303133; }
.message-box {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}
.message-row.mine {
  justify-content: flex-end;
}
.msg-avatar { flex-shrink: 0; }
.bubble {
  max-width: 65%;
  background: #fff;
  border-radius: 10px;
  padding: 10px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.message-row.mine .bubble {
  background: #409EFF;
  color: #fff;
}
.message-row.mine .bubble .time { color: rgba(255, 255, 255, 0.75); }
.content {
  line-height: 1.6;
  word-break: break-word;
}
.message-row.mine .bubble >>> .rich-content {
  color: #fff;
}
.message-row.mine .bubble >>> .mention,
.message-row.mine .bubble >>> a {
  color: rgba(255, 255, 255, 0.92);
}
.composer-toolbar .image-upload {
  display: inline-flex;
  align-items: center;
}
.image-trigger {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #909399;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 22px;
  transition: color 0.15s, background 0.15s;
}
.image-trigger:hover {
  color: #409EFF;
  background: #ecf5ff;
}
.time {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
.composer {
  padding: 12px 20px 16px;
  background: #fff;
  border-top: 1px solid #e3e5e7;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}
.composer-toolbar {
  width: 100%;
  display: flex;
  align-items: center;
}
.composer >>> .el-textarea {
  width: 100%;
}
.draft-media-list {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.draft-media-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #fafafa;
}
.draft-media-thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.draft-media-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 12px;
  line-height: 1;
}
.draft-media-remove:hover {
  background: rgba(245, 108, 108, 0.9);
}

.detail-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; color: #c0c4cc; }

/* 通知类消息（回复 / @ / 赞 / 收藏 / 系统） */
.message-center.notice-mode {
  width: 100%;
}

.message-center.notice-mode .list-panel {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  max-width: none;
  border-right: none;
}

.notice-list {
  width: 100%;
  background: #fff;
}

.notice-item {
  display: flex;
  gap: 14px;
  width: 100%;
  box-sizing: border-box;
  padding: 18px 24px;
  border-bottom: 1px solid #f1f2f3;
  transition: background 0.15s;
}

.notice-item.clickable {
  cursor: pointer;
}

.notice-item.clickable:hover {
  background: #f7f8fa;
}

.notice-item.unread {
  background: #fafcff;
}

.notice-avatar {
  flex-shrink: 0;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.notice-meta {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  max-width: 140px;
  margin-left: 16px;
}

.notice-item .notice-time {
  font-size: 13px;
  color: #9499a0;
  white-space: nowrap;
}

.notice-post-ref {
  font-size: 12px;
  color: #9499a0;
  text-align: right;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.notice-name {
  font-size: 15px;
  font-weight: 600;
  color: #18191c;
}

.notice-action {
  font-size: 14px;
  color: #9499a0;
}

.notice-quote {
  font-size: 14px;
  line-height: 1.6;
  color: #61666d;
  padding-left: 10px;
  border-left: 2px solid #e3e5e7;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

@media (max-width: 960px) {
  .message-center { flex-direction: column; height: auto; }
  .sidebar { width: 100%; display: flex; overflow-x: auto; padding: 8px 0; }
  .sidebar-title { display: none; }
  .sidebar-item { white-space: nowrap; padding: 10px 16px; }
  .sidebar-item.active::before { display: none; }
  .list-panel { width: 100%; max-height: none; }
  .message-center.notice-mode .list-panel { max-width: none; }
  .detail-panel { min-height: 400px; }
}
</style>
