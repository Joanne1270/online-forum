<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="sidebar-card" shadow="never">
          <div class="user-brief">
            <el-avatar :size="72" :src="displayAvatar" icon="el-icon-user-solid" />
            <div class="nickname">{{ form.nickname || '用户' }}</div>
          </div>
          <div class="stats">
            <div class="stat-item" @click="openPanel('posts')">
              <strong>{{ stats.posts }}</strong>
              <span>帖子</span>
            </div>
            <div v-if="!isAdmin" class="stat-item" @click="openPanel('following')">
              <strong>{{ stats.following }}</strong>
              <span>关注</span>
            </div>
            <div v-if="!isAdmin" class="stat-item" @click="openPanel('followers')">
              <strong>{{ stats.followers }}</strong>
              <span>粉丝</span>
            </div>
          </div>
          <el-menu ref="sideMenu" :default-active="activeMenu" class="side-menu" @select="onMenuSelect">
            <el-menu-item index="history">
              <i class="el-icon-time"></i>
              <span>浏览历史</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <i class="el-icon-star-off"></i>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="likes">
              <i class="menu-heart-icon">♡</i>
              <span>我的点赞</span>
            </el-menu-item>
            <el-menu-item index="drafts">
              <i class="el-icon-edit-outline"></i>
              <span>我的草稿</span>
            </el-menu-item>
            <el-menu-item index="reports">
              <i class="el-icon-warning-outline"></i>
              <span>我的举报</span>
            </el-menu-item>
            <el-menu-item index="settings">
              <i class="el-icon-setting"></i>
              <span>设置</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card class="main-card" shadow="never" v-loading="panelLoading">
          <div slot="header" class="panel-header">
            <span>{{ panelTitle }}</span>
          </div>

          <!-- 浏览历史 -->
          <div v-if="activeMenu === 'history'">
            <div v-for="group in historyGroups" :key="group.date" class="history-group">
              <div class="history-date">{{ group.date }}</div>
              <div
                v-for="item in group.items"
                :key="item.id"
                class="history-item clickable"
                @click="goPost(item.id)"
              >
                {{ item.title }}
              </div>
            </div>
            <el-empty v-if="!historyGroups.length" description="暂无浏览记录" :image-size="80" />
            <el-button v-if="historyGroups.length" size="mini" type="text" @click="clearHistory">清空浏览历史</el-button>
          </div>

          <!-- 我的收藏 -->
          <div v-else-if="activeMenu === 'favorites'">
            <post-item v-for="p in favorites" :key="p.id" :post="p" />
            <el-empty v-if="!favorites.length" description="暂无收藏" :image-size="80" />
          </div>

          <!-- 我的点赞 -->
          <div v-else-if="activeMenu === 'likes'">
            <post-item v-for="p in likedPosts" :key="p.id" :post="p" />
            <el-empty v-if="!likedPosts.length" description="暂无点赞" :image-size="80" />
          </div>

          <!-- 我的草稿 -->
          <div v-else-if="activeMenu === 'drafts'">
            <div v-for="d in drafts" :key="d.id" class="draft-item">
              <div class="draft-title">{{ d.title || '无标题' }}</div>
              <div class="draft-meta">
                <span>{{ d.boardName || '未选版块' }}</span>
                <span>·</span>
                <span>{{ formatTime(d.updatedAt || d.createdAt) }}</span>
              </div>
              <div class="draft-actions" @click.stop>
                <el-button size="mini" @click="editDraft(d.id)">编辑</el-button>
                <el-button size="mini" type="primary" @click="publishDraft(d)">发布</el-button>
                <el-button size="mini" type="danger" plain @click="deleteDraft(d)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!drafts.length" description="暂无草稿" :image-size="80" />
          </div>

          <!-- 我的举报 -->
          <div v-else-if="activeMenu === 'reports'">
            <div v-for="r in reports" :key="r.refType + '-' + r.id" class="report-item">
              <div class="report-title">
                <el-tag size="mini" :type="r.refType === 'POST' ? 'primary' : 'info'">
                  {{ r.refType === 'POST' ? '帖子' : '回复' }}
                </el-tag>
                <span class="clickable" @click="goPost(r.postId)">{{ r.title || '相关内容' }}</span>
              </div>
              <div class="report-reason">原因：{{ r.reason }}</div>
              <div class="report-meta">
                {{ formatTime(r.createdAt) }} · {{ reportStatusText(r.status) }}
              </div>
            </div>
            <el-empty v-if="!reports.length" description="暂无举报记录" :image-size="80" />
          </div>

          <!-- 设置 -->
          <div v-else-if="activeMenu === 'settings'">
            <el-tabs v-model="settingsTab">
              <el-tab-pane label="隐私设置" name="privacy">
                <el-form label-width="140px">
                  <el-form-item label="公开已发布的帖子">
                    <el-switch v-model="privacy.showPosts" />
                  </el-form-item>
                  <el-form-item label="公开收藏">
                    <el-switch v-model="privacy.showFavorites" />
                  </el-form-item>
                  <el-form-item label="公开回复">
                    <el-switch v-model="privacy.showReplies" />
                  </el-form-item>
                  <el-form-item label="公开关注列表">
                    <el-switch v-model="privacy.showFollowing" />
                  </el-form-item>
                  <el-form-item label="公开粉丝列表">
                    <el-switch v-model="privacy.showFollowers" />
                  </el-form-item>
                  <el-button type="primary" plain @click="savePrivacy">保存隐私设置</el-button>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="用户设置" name="profile">
                <el-form label-width="96px">
                  <el-form-item label="手机号">
                    <el-input v-model="form.phone" disabled />
                  </el-form-item>
                  <el-form-item label="昵称">
                    <el-input v-model="form.nickname" />
                    <div class="hint">
                      今日已提交 {{ form.nicknameChangesToday || 0 }}/3 次
                      <el-tag v-if="form.pendingNickname" size="mini" type="warning" style="margin-left:8px">
                        待审核：{{ form.pendingNickname }}
                      </el-tag>
                    </div>
                  </el-form-item>
                  <el-form-item label="头像">
                    <div class="avatar-row">
                      <el-avatar :size="64" :src="displayAvatar" icon="el-icon-user-solid" />
                      <el-upload action="#" :http-request="uploadAvatar" :show-file-list="false" accept="image/*">
                        <el-button size="mini" type="primary" plain>上传头像</el-button>
                      </el-upload>
                    </div>
                  </el-form-item>
                  <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
                  <el-form-item label="性别">
                    <el-radio-group v-model="form.gender">
                      <el-radio label="MALE">男</el-radio>
                      <el-radio label="FEMALE">女</el-radio>
                      <el-radio label="SECRET">保密</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="出生年月日">
                    <el-date-picker
                      v-model="form.birthMonth"
                      type="date"
                      value-format="yyyy-MM-dd"
                      placeholder="选择出生日期"
                      :picker-options="birthDatePickerOptions"
                      clearable
                    />
                  </el-form-item>
                  <el-form-item label="简介"><el-input v-model="form.bio" type="textarea" /></el-form-item>
                  <el-button type="primary" @click="save">保存</el-button>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="其他" name="other">
                <el-form label-width="140px">
                  <el-form-item label="消息提醒">
                    <el-switch v-model="notifyEnabled" @change="saveNotifyPref" />
                    <div class="hint">关闭后仍可在通知中心查看，仅减少页面角标刷新提示</div>
                  </el-form-item>
                  <el-form-item label="通知提示音">
                    <el-switch v-model="soundEnabled" @change="saveSoundPref" />
                    <div class="hint">收到新通知时播放短提示音（需浏览器允许）</div>
                  </el-form-item>
                  <el-form-item label="显示搜索记录">
                    <el-switch v-model="searchHistoryEnabled" @change="saveSearchHistoryPref" />
                    <div class="hint">关闭后点击顶部搜索框将不再显示最近搜索记录</div>
                  </el-form-item>
                  <el-form-item label="浏览历史">
                    <el-button size="mini" @click="clearHistory">清空浏览历史</el-button>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 帖子 -->
          <div v-else-if="activeMenu === 'posts'">
            <post-item
              v-for="p in myPosts"
              :key="p.id"
              :post="p"
              show-profile-pin
              @toggle-profile-pin="toggleProfilePin"
            />
            <el-empty v-if="!myPosts.length" description="暂无帖子" :image-size="80" />
          </div>

          <!-- 关注 / 粉丝 -->
          <div v-else-if="activeMenu === 'following'">
            <follow-user-list
              :users="following"
              show-unfollow
              empty-text="暂无关注用户"
              @unfollow="unfollow"
            />
          </div>
          <div v-else-if="activeMenu === 'followers'">
            <follow-user-list
              :users="followers"
              time-label="关注于"
              empty-text="暂无粉丝"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { userApi } from '../../api/user'
import { postApi } from '../../api/post'
import { fileApi } from '../../api/file'
import PostItem from '../../components/PostItem.vue'
import FollowUserList from '../../components/FollowUserList.vue'
import {
  getViewHistory,
  clearViewHistory,
  getNotifyEnabled,
  setNotifyEnabled,
  getSoundEnabled,
  setSoundEnabled
} from '../../utils/viewHistory'
import {
  getSearchHistoryEnabled,
  setSearchHistoryEnabled
} from '../../utils/searchHistory'

const PANEL_TITLES = {
  history: '浏览历史',
  favorites: '我的收藏',
  likes: '我的点赞',
  drafts: '我的草稿',
  reports: '我的举报',
  settings: '设置',
  posts: '我的帖子',
  following: '我的关注',
  followers: '我的粉丝'
}

const VALID_PANELS = Object.keys(PANEL_TITLES)

export default {
  components: { PostItem, FollowUserList },
  data() {
    return {
      activeMenu: 'history',
      settingsTab: 'privacy',
      panelLoading: false,
      form: {},
      originalNickname: '',
      originalAvatar: '',
      pendingAvatarUrl: '',
      privacy: { showPosts: true, showFavorites: true, showReplies: true, showFollowing: true, showFollowers: true },
      notifyEnabled: true,
      soundEnabled: false,
      searchHistoryEnabled: true,
      stats: { posts: 0, following: 0, followers: 0 },
      following: [],
      followers: [],
      myPosts: [],
      favorites: [],
      likedPosts: [],
      drafts: [],
      reports: [],
      viewHistory: [],
      birthDatePickerOptions: {
        disabledDate(time) {
          return time.getTime() > Date.now()
        }
      }
    }
  },
  computed: {
    ...mapGetters(['isAdmin', 'user']),
    displayAvatar() {
      return this.pendingAvatarUrl || this.form.pendingAvatar || this.form.avatar || ''
    },
    panelTitle() {
      return PANEL_TITLES[this.activeMenu] || '个人中心'
    },
    historyGroups() {
      const map = {}
      for (const item of this.viewHistory) {
        const date = item.viewedAt ? String(item.viewedAt).slice(0, 10) : '未知日期'
        if (!map[date]) map[date] = []
        map[date].push(item)
      }
      return Object.keys(map)
        .sort((a, b) => b.localeCompare(a))
        .map(date => ({ date, items: map[date] }))
    }
  },
  watch: {
    '$route'(to) {
      if (to.path === '/profile') {
        this.applyRoutePanel()
      }
    }
  },
  mounted() {
    this.syncMenuActive(this.activeMenu)
  },
  async created() {
    this.notifyEnabled = getNotifyEnabled()
    this.soundEnabled = getSoundEnabled()
    this.searchHistoryEnabled = getSearchHistoryEnabled()
    if (VALID_PANELS.includes(this.$route.query.panel)) {
      this.activeMenu = this.$route.query.panel
    }
    await this.loadBase()
    await this.loadPanel()
  },
  methods: {
    syncMenuActive(menu) {
      this.$nextTick(() => {
        const sideMenu = this.$refs.sideMenu
        if (!sideMenu) return
        const sidebarMenus = ['history', 'favorites', 'likes', 'drafts', 'reports', 'settings']
        sideMenu.activeIndex = sidebarMenus.includes(menu) ? menu : ''
      })
    },
    applyRoutePanel() {
      const menu = VALID_PANELS.includes(this.$route.query.panel)
        ? this.$route.query.panel
        : 'history'
      if (this.activeMenu !== menu) {
        this.activeMenu = menu
        this.loadPanel()
      }
      this.syncMenuActive(menu)
    },
    syncProfileRoute(menu) {
      if (this.$route.path !== '/profile') return
      const currentPanel = this.$route.query.panel || 'history'
      if (currentPanel === menu) return
      const query = menu === 'history' ? {} : { panel: menu }
      this.$router.replace({ path: '/profile', query }).catch(() => {})
    },
    onMenuSelect(menu) {
      this.openPanel(menu, { syncRoute: true })
    },
    async loadBase() {
      const me = await userApi.me()
      this.form = {
        ...me.data,
        gender: me.data.gender || 'SECRET',
        birthMonth: me.data.birthMonth ? String(me.data.birthMonth).slice(0, 10) : ''
      }
      this.privacy = {
        showPosts: me.data.showPosts !== false,
        showFavorites: me.data.showFavorites !== false,
        showReplies: me.data.showReplies !== false,
        showFollowing: me.data.showFollowing !== false,
        showFollowers: me.data.showFollowers !== false
      }
      this.originalNickname = me.data.nickname || ''
      this.originalAvatar = me.data.avatar || ''
      this.pendingAvatarUrl = ''
      const postsRes = await postApi.myPosts({ page: 1, size: 1 })
      this.stats.posts = postsRes.data.total || 0
      if (!this.isAdmin) {
        const [followingRes, followersRes] = await Promise.all([
          userApi.following(),
          userApi.followers()
        ])
        this.following = followingRes.data || []
        this.followers = followersRes.data || []
        this.stats.following = this.following.length
        this.stats.followers = this.followers.length
      }
    },
    async openPanel(menu, options = {}) {
      if (!VALID_PANELS.includes(menu)) return
      const changed = this.activeMenu !== menu
      this.activeMenu = menu
      if (options.syncRoute !== false) {
        this.syncProfileRoute(menu)
      }
      if (changed) {
        await this.loadPanel()
      }
      this.syncMenuActive(menu)
    },
    async loadPanel() {
      this.panelLoading = true
      try {
        if (this.activeMenu === 'history') {
          this.viewHistory = getViewHistory(this.user.id)
        } else if (this.activeMenu === 'favorites') {
          const res = await postApi.favorites({ page: 1, size: 20 })
          this.favorites = res.data.list || []
        } else if (this.activeMenu === 'likes') {
          const res = await postApi.myLikes({ page: 1, size: 20 })
          this.likedPosts = res.data.list || []
        } else if (this.activeMenu === 'drafts') {
          const res = await postApi.myDrafts({ page: 1, size: 20 })
          this.drafts = res.data.list || []
        } else if (this.activeMenu === 'reports') {
          const res = await postApi.myReports({ page: 1, size: 20 })
          this.reports = res.data.list || []
        } else if (this.activeMenu === 'posts') {
          const res = await postApi.myPosts({ page: 1, size: 20 })
          this.myPosts = res.data.list || []
        }
      } finally {
        this.panelLoading = false
      }
    },
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    reportStatusText(status) {
      if (status === 1) return '已处理'
      if (status === 2) return '已驳回'
      return '待处理'
    },
    clearHistory() {
      clearViewHistory(this.user.id)
      this.viewHistory = []
      this.$message.success('浏览历史已清空')
    },
    saveNotifyPref(val) {
      setNotifyEnabled(val)
      this.$message.success('已保存')
    },
    saveSoundPref(val) {
      setSoundEnabled(val)
      this.$message.success('已保存')
    },
    saveSearchHistoryPref(val) {
      setSearchHistoryEnabled(val)
      this.$root.$emit('search-history-pref-changed')
      this.$message.success('已保存')
    },
    async uploadAvatar({ file }) {
      const res = await fileApi.upload(file, 'media')
      this.pendingAvatarUrl = res.data.url
      this.$message.success('头像已上传，请在用户设置中保存')
      this.activeMenu = 'settings'
      this.settingsTab = 'profile'
    },
    async save() {
      const payload = {
        email: this.form.email,
        bio: this.form.bio,
        gender: this.form.gender || 'SECRET',
        birthMonth: this.form.birthMonth || ''
      }
      let submittedReview = false
      if (this.form.nickname !== this.originalNickname) {
        payload.nickname = this.form.nickname
        submittedReview = true
      }
      const avatarToSubmit = this.pendingAvatarUrl || (this.form.avatar !== this.originalAvatar ? this.form.avatar : null)
      if (avatarToSubmit) {
        payload.avatar = avatarToSubmit
        submittedReview = true
      }
      const res = await userApi.updateMe(payload)
      this.form = {
        ...res.data,
        gender: res.data.gender || 'SECRET',
        birthMonth: res.data.birthMonth ? String(res.data.birthMonth).slice(0, 10) : ''
      }
      this.originalNickname = res.data.nickname || ''
      this.originalAvatar = res.data.avatar || ''
      this.pendingAvatarUrl = ''
      this.$store.dispatch('login', { token: this.$store.state.token, user: res.data })
      this.$message.success(submittedReview ? '修改已提交，等待管理员审核' : '保存成功')
    },
    async savePrivacy() {
      await userApi.updatePrivacy(this.privacy)
      this.$message.success('隐私设置已保存')
    },
    goPost(postId) {
      if (postId) this.$router.push(`/post/${postId}`)
    },
    editDraft(id) {
      this.$router.push(`/post/draft/${id}/edit`)
    },
    async publishDraft(draft) {
      if (!(draft.title || '').trim()) {
        this.$message.warning('标题不能为空，请先编辑草稿')
        return
      }
      try {
        await this.$confirm('确认发布该草稿？', '发布帖子')
        const res = await postApi.publishDraft(draft.id, {
          boardId: draft.boardId,
          title: draft.title,
          content: draft.content
        })
        this.$message.success('发布成功')
        this.$router.push(`/post/${res.data.id}`)
      } catch (e) {
        if (e === 'cancel') return
        this.$message.error((e.response && e.response.data && e.response.data.message) || '发布失败')
      }
    },
    async deleteDraft(draft) {
      try {
        await this.$confirm('确认删除该草稿？删除后无法恢复', '删除草稿', { type: 'warning' })
        await postApi.deleteDraft(draft.id)
        this.drafts = this.drafts.filter(d => d.id !== draft.id)
        this.$message.success('草稿已删除')
      } catch (e) {
        if (e === 'cancel') return
        this.$message.error((e.response && e.response.data && e.response.data.message) || '删除失败')
      }
    },
    async toggleProfilePin(post) {
      try {
        if (post.profilePinned) {
          await postApi.unpinProfile(post.id)
          this.$message.success('已取消置顶')
        } else {
          await postApi.pinProfile(post.id)
          this.$message.success('已置顶')
        }
        const res = await postApi.myPosts({ page: 1, size: 20 })
        this.myPosts = res.data.list || []
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || '操作失败')
      }
    },
    async unfollow(user) {
      await this.$confirm(`确认取消关注 ${user.nickname}？`)
      await userApi.unfollow(user.id)
      this.$message.success('已取消关注')
      const followingRes = await userApi.following()
      this.following = followingRes.data || []
      this.stats.following = this.following.length
    }
  }
}
</script>

<style scoped>
.profile-page { max-width: 1100px; margin: 0 auto; }
.sidebar-card, .main-card { border-radius: 8px; }
.user-brief { text-align: center; padding: 12px 0 16px; }
.side-menu { border-right: none; overflow: hidden; }
.side-menu >>> .menu-heart-icon {
  margin-right: 5px;
  width: 24px;
  text-align: center;
  font-size: 18px;
  vertical-align: middle;
  display: inline-block;
  font-style: normal;
  line-height: 18px;
  color: #909399;
}
.side-menu >>> .el-menu-item.is-active .menu-heart-icon {
  color: #409EFF;
}
.nickname { margin-top: 12px; font-size: 18px; font-weight: 600; color: #303133; }
.stats {
  display: flex;
  justify-content: space-around;
  padding: 12px 0 16px;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
}
.stat-item {
  text-align: center;
  cursor: pointer;
  color: #606266;
}
.stat-item strong {
  display: block;
  font-size: 18px;
  color: #303133;
  margin-bottom: 4px;
}
.stat-item:hover strong { color: #409EFF; }
.panel-header { font-size: 16px; font-weight: 600; }
.history-group { margin-bottom: 20px; }
.history-date { color: #909399; font-size: 13px; margin-bottom: 8px; }
.history-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
  color: #303133;
}
.report-item {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}
.report-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.report-reason { color: #606266; margin-top: 6px; font-size: 13px; }
.report-meta { color: #909399; font-size: 12px; margin-top: 4px; }
.draft-item {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}
.draft-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 6px; }
.draft-meta { color: #909399; font-size: 13px; margin-bottom: 10px; display: flex; gap: 6px; }
.draft-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.hint { color: #909399; font-size: 12px; margin-top: 6px; }
.avatar-row { display: flex; align-items: center; gap: 16px; }
.clickable { cursor: pointer; }
.clickable:hover { color: #409EFF; }
</style>
