<template>
  <div v-loading="loading" class="user-public-page">
    <div v-if="profile.deleted" class="deleted-page">
      <el-avatar :size="80" icon="el-icon-user-solid" />
      <h2 class="deleted-title">该用户已注销</h2>
      <p class="deleted-hint">该账号已注销，无法查看主页内容</p>
    </div>

    <template v-else-if="profile.id">
      <!-- 上半屏：渐变背景区 -->
      <section class="upper-zone">
        <div class="upper-inner">
          <div class="profile-row">
            <el-avatar :size="96" :src="profile.avatar" icon="el-icon-user-solid" class="space-avatar" />
            <div class="profile-info">
              <h1 class="nickname">{{ profile.nickname }}</h1>
              <div v-if="profile.profileBrief" class="profile-brief">{{ profile.profileBrief }}</div>
              <div class="bio">{{ profile.bio || '这个人很懒，什么都没写~' }}</div>
            </div>
          </div>

          <div class="upper-bottom">
            <div class="stats">
              <div class="stat-item" @click="switchTab('following')">
                <span class="stat-value">{{ profile.followingCount || 0 }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item" @click="switchTab('followers')">
                <span class="stat-value">{{ profile.followersCount || 0 }}</span>
                <span class="stat-label">粉丝</span>
              </div>
            </div>

            <div v-if="canInteract" class="header-actions">
              <el-button
                v-if="canFollow"
                round
                class="action-btn follow-btn"
                :class="{ followed: profile.followed }"
                @click="toggleFollow"
              >
                {{ profile.followed ? '已关注' : '+ 关注' }}
              </el-button>
              <el-button
                v-if="canMessage"
                round
                class="action-btn message-btn"
                @click="goMessage"
              >
                发私信
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- 下半屏：标签 + 搜索 + 内容 -->
      <section class="lower-zone">
        <div class="lower-inner">
          <div class="nav-bar">
            <div class="nav-tabs">
              <span
                v-for="tab in tabs"
                :key="tab.key"
                class="nav-tab"
                :class="{ active: activeTab === tab.key }"
                @click="switchTab(tab.key)"
              >
                <i :class="tab.icon" />
                {{ tab.label }}
              </span>
            </div>
            <div class="nav-search">
              <el-input
                v-model="searchKeyword"
                :placeholder="searchPlaceholder"
                prefix-icon="el-icon-search"
                clearable
                size="small"
              />
            </div>
          </div>

          <div class="content-main">
            <template v-if="activeTab === 'posts'">
              <div v-if="profile.postsVisible">
                <post-item v-for="p in filteredPosts" :key="p.id" :post="p" />
                <el-empty v-if="!filteredPosts.length" :description="searchKeyword ? '未找到相关帖子' : '暂无帖子'" :image-size="80" />
              </div>
              <el-alert v-else type="info" title="该用户已隐藏帖子列表" :closable="false" show-icon />
            </template>

            <template v-else-if="activeTab === 'favorites'">
              <div v-if="profile.favoritesVisible">
                <post-item v-for="p in filteredFavorites" :key="'f-' + p.id" :post="p" />
                <el-empty v-if="!filteredFavorites.length" :description="searchKeyword ? '未找到相关收藏' : '暂无收藏'" :image-size="80" />
              </div>
              <el-alert v-else type="info" title="该用户已隐藏收藏列表" :closable="false" show-icon />
            </template>

            <template v-else-if="activeTab === 'replies'">
              <div v-if="profile.repliesVisible">
                <div
                  v-for="r in filteredReplies"
                  :key="r.id"
                  class="reply-card clickable"
                  @click="goPost(r.postId)"
                >
                  <div class="reply-content">{{ r.content }}</div>
                  <div class="reply-meta">{{ formatTime(r.createdAt) }}</div>
                </div>
                <el-empty v-if="!filteredReplies.length" :description="searchKeyword ? '未找到相关回复' : '暂无回复'" :image-size="80" />
              </div>
              <el-alert v-else type="info" title="该用户已隐藏回复列表" :closable="false" show-icon />
            </template>

            <template v-else-if="activeTab === 'following'">
              <div v-if="profile.followingVisible">
                <follow-user-list :users="filteredFollowing" empty-text="暂无关注" />
              </div>
              <el-alert v-else type="info" title="该用户已隐藏关注列表" :closable="false" show-icon />
            </template>

            <template v-else-if="activeTab === 'followers'">
              <div v-if="profile.followersVisible">
                <follow-user-list
                  :users="filteredFollowers"
                  time-label="关注于"
                  empty-text="暂无粉丝"
                />
              </div>
              <el-alert v-else type="info" title="该用户已隐藏粉丝列表" :closable="false" show-icon />
            </template>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { userApi } from '../../api/user'
import PostItem from '../../components/PostItem.vue'
import FollowUserList from '../../components/FollowUserList.vue'

const VALID_TABS = ['posts', 'favorites', 'replies', 'following', 'followers']

export default {
  components: { PostItem, FollowUserList },
  data() {
    return {
      loading: false,
      profile: {},
      activeTab: 'posts',
      searchKeyword: '',
      tabs: [
        { key: 'posts', label: '帖子', icon: 'el-icon-document' },
        { key: 'favorites', label: '收藏', icon: 'el-icon-star-off' },
        { key: 'replies', label: '回复', icon: 'el-icon-chat-dot-round' },
        { key: 'following', label: '关注', icon: 'el-icon-user' },
        { key: 'followers', label: '粉丝', icon: 'el-icon-s-custom' }
      ]
    }
  },
  computed: {
    ...mapGetters(['isLogin', 'isAdmin', 'user']),
    canFollow() {
      return this.isLogin && !this.isAdmin && this.profile.id && !this.profile.deleted
        && this.user.id !== this.profile.id
    },
    canMessage() {
      return this.canFollow
    },
    canInteract() {
      return this.canFollow || this.canMessage
    },
    searchPlaceholder() {
      const map = {
        posts: '搜索帖子',
        favorites: '搜索收藏',
        replies: '搜索回复',
        following: '搜索关注用户',
        followers: '搜索粉丝'
      }
      return map[this.activeTab] || '搜索'
    },
    filteredPosts() {
      return this.filterList(this.profile.posts || [], ['title'])
    },
    filteredFavorites() {
      return this.filterList(this.profile.favorites || [], ['title'])
    },
    filteredReplies() {
      return this.filterList(this.profile.replies || [], ['content'])
    },
    filteredFollowing() {
      return this.filterList(this.profile.following || [], ['nickname'])
    },
    filteredFollowers() {
      return this.filterList(this.profile.followers || [], ['nickname'])
    }
  },
  watch: {
    '$route'(to) {
      if (to.path.startsWith('/user/')) {
        this.applyRouteTab()
        if (to.params.id !== String(this.profile.id)) {
          this.load()
        }
      }
    },
    activeTab() {
      this.searchKeyword = ''
    }
  },
  created() {
    this.applyRouteTab()
    this.load()
  },
  methods: {
    applyRouteTab() {
      const tab = this.$route.query.tab
      if (VALID_TABS.includes(tab)) {
        this.activeTab = tab
      }
    },
    switchTab(key) {
      this.activeTab = key
      const query = key === 'posts' ? {} : { tab: key }
      if (this.$route.path === `/user/${this.$route.params.id}`) {
        const current = this.$route.query.tab || 'posts'
        if ((current === 'posts' && key === 'posts') || current === key) return
        this.$router.replace({ path: this.$route.path, query }).catch(() => {})
      }
    },
    filterList(list, fields) {
      const kw = (this.searchKeyword || '').trim().toLowerCase()
      if (!kw) return list
      return list.filter(item => fields.some(f => String(item[f] || '').toLowerCase().includes(kw)))
    },
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    async load() {
      this.loading = true
      try {
        const res = await userApi.publicProfile(this.$route.params.id)
        this.profile = res.data || {}
      } finally {
        this.loading = false
      }
    },
    async toggleFollow() {
      if (this.profile.followed) {
        await userApi.unfollow(this.profile.id)
        this.profile.followed = false
        this.profile.followersCount = Math.max(0, (this.profile.followersCount || 0) - 1)
        this.$message.success('已取消关注')
      } else {
        await userApi.follow(this.profile.id)
        this.profile.followed = true
        this.profile.followersCount = (this.profile.followersCount || 0) + 1
        this.$message.success('关注成功')
      }
    },
    goMessage() {
      if (!this.isLogin) {
        this.$router.push('/login')
        return
      }
      this.$router.push(`/messages/chat/${this.profile.id}`)
    },
    goPost(postId) {
      if (postId) {
        this.$router.push(`/post/${postId}`)
      }
    }
  }
}
</script>

<style scoped>
.user-public-page {
  margin: -20px;
  min-height: calc(100vh - 60px);
  background: #f4f5f7;
}

/* ---- 上半屏渐变区 ---- */
.upper-zone {
  min-height: 50vh;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 42%, #a0cfff 100%);
  color: #fff;
  display: flex;
  align-items: flex-end;
}
.upper-inner {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 28px;
}
.profile-row {
  display: flex;
  align-items: flex-end;
  gap: 24px;
}
.space-avatar {
  flex-shrink: 0;
  border: 4px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.profile-info {
  min-width: 0;
  padding-bottom: 4px;
}
.nickname {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}
.profile-brief {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
  margin-bottom: 6px;
}
.bio {
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.92);
  max-width: 640px;
}
.upper-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 24px;
  flex-wrap: wrap;
}
.stats {
  display: flex;
  gap: 28px;
}
.stat-item {
  cursor: pointer;
  text-align: center;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;
}
.stat-item:hover {
  background: rgba(255, 255, 255, 0.12);
}
.stat-value {
  display: block;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 2px;
}
.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.action-btn {
  min-width: 96px;
  border: none;
  font-weight: 600;
}
.follow-btn {
  background: rgba(255, 255, 255, 0.95);
  color: #409EFF;
}
.follow-btn:hover,
.follow-btn:focus {
  background: #fff;
  color: #409EFF;
}
.follow-btn.followed {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.45);
}
.follow-btn.followed:hover {
  background: rgba(255, 255, 255, 0.3);
  color: #fff;
}
.message-btn {
  background: rgba(0, 0, 0, 0.18);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.35);
}
.message-btn:hover,
.message-btn:focus {
  background: rgba(0, 0, 0, 0.28);
  color: #fff;
}

/* ---- 下半屏内容区 ---- */
.lower-zone {
  background: #f4f5f7;
  min-height: 50vh;
}
.lower-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 40px;
}
.nav-bar {
  background: #fff;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #e3e5e7;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
  min-height: 52px;
  margin-top: -1px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}
.nav-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  overflow-x: auto;
}
.nav-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 14px 14px;
  font-size: 14px;
  color: #61666d;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  transition: color 0.15s;
}
.nav-tab i { font-size: 15px; }
.nav-tab:hover { color: #409EFF; }
.nav-tab.active {
  color: #409EFF;
  border-bottom-color: #409EFF;
  font-weight: 600;
}
.nav-search {
  flex: 1;
  max-width: 360px;
  margin-left: auto;
}
.nav-search >>> .el-input__inner {
  border-radius: 999px;
  background: #f1f2f3;
  border-color: transparent;
}
.nav-search >>> .el-input__inner:focus {
  background: #fff;
  border-color: #409EFF;
}
.content-main {
  background: #fff;
  border-radius: 0 0 8px 8px;
  padding: 8px 20px 24px;
  min-height: 360px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.reply-card {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}
.reply-content {
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}
.reply-meta {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
.clickable { cursor: pointer; }
.clickable:hover .reply-content { color: #409EFF; }

.deleted-page {
  text-align: center;
  padding: 80px 20px;
  color: #909399;
  background: #fff;
  margin: 20px;
  border-radius: 8px;
}
.deleted-title {
  margin: 20px 0 8px;
  color: #606266;
  font-size: 22px;
}
.deleted-hint {
  margin: 0;
  font-size: 14px;
}

@media (max-width: 768px) {
  .profile-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .upper-bottom {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }
  .header-actions {
    width: 100%;
  }
  .action-btn {
    flex: 1;
  }
  .nav-bar {
    flex-wrap: wrap;
    padding: 8px 12px;
  }
  .nav-search {
    width: 100%;
    max-width: none;
    order: 2;
    margin-left: 0;
  }
  .lower-inner {
    padding: 0 12px 24px;
  }
}
</style>
