<template>
  <div id="app">
    <el-container class="layout" :class="{ 'auth-layout': isAuthPage }">
      <el-header class="header">
        <button
          v-if="showBackButton"
          type="button"
          class="header-back-btn"
          aria-label="返回"
          @click="goBack"
        >
          <i class="el-icon-arrow-left"></i>
        </button>
        <div class="brand" @click="$router.push('/')">在线社区论坛</div>
        <el-menu mode="horizontal" :router="true" background-color="#409EFF" text-color="#fff" active-text-color="#ffd04b" class="nav-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item v-if="isLogin && !isAdmin" index="/following">关注</el-menu-item>
          <el-menu-item v-if="isLogin && !isAdmin" index="/messages">
            消息<span v-if="totalMessageUnread > 0" class="badge">{{ totalMessageUnread }}</span>
          </el-menu-item>
          <el-menu-item v-if="isLogin" index="/profile">个人中心</el-menu-item>
          <el-menu-item v-if="isAdmin" index="/admin">管理后台</el-menu-item>
        </el-menu>
        <div class="auth-area">
          <div v-if="!isAuthPage" class="header-search-wrap">
            <div class="header-search">
              <input
                v-model="searchKeyword"
                class="search-input"
                placeholder="搜索标题或内容......"
                @focus="onSearchFocus"
                @blur="onSearchBlur"
                @keyup.enter="goSearch"
              />
              <button type="button" class="search-btn" aria-label="搜索" @click="goSearch">
                <i class="el-icon-search"></i>
              </button>
            </div>
            <div
              v-if="showSearchDropdown && searchHistoryEnabled && searchHistory.length"
              class="search-dropdown"
              @mousedown.prevent
            >
              <div
                v-for="item in searchHistory"
                :key="item"
                class="search-history-item"
              >
                <span class="history-text" @click="pickHistory(item)">{{ item }}</span>
                <span class="history-remove" @click="removeHistoryItem(item)">×</span>
              </div>
              <div class="search-dropdown-footer">
                <span class="clear-all" @click="clearAllHistory">清除全部</span>
              </div>
            </div>
          </div>
          <template v-if="isLogin">
            <span class="username">{{ user.nickname }}</span>
            <el-button size="mini" @click="logout">退出登录</el-button>
          </template>
          <template v-else>
            <el-button size="mini" type="primary" plain @click="$router.push('/login')">登录</el-button>
            <el-button size="mini" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </el-header>
      <el-main :class="{ 'auth-main': isAuthPage }">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { notificationApi } from './api/notification'
import { messageApi } from './api/message'
import { userApi } from './api/user'
import {
  getSearchHistory,
  addSearchHistory,
  removeSearchHistory,
  clearSearchHistory,
  getSearchHistoryEnabled
} from './utils/searchHistory'

export default {
  name: 'App',
  data() {
    return {
      unread: 0,
      messageUnread: 0,
      notificationUnread: {},
      timer: null,
      messageTimer: null,
      heartbeatTimer: null,
      searchKeyword: '',
      searchHistory: [],
      searchHistoryEnabled: true,
      showSearchDropdown: false
    }
  },
  computed: {
    ...mapGetters(['isLogin', 'isAdmin', 'user']),
    showBackButton() {
      if (this.isAuthPage) return false
      return this.$route.path !== '/'
    },
    isAuthPage() {
      return ['/login', '/register', '/forgot-password'].includes(this.$route.path)
    },
    totalMessageUnread() {
      const note = Object.values(this.notificationUnread || {}).reduce((sum, n) => sum + (Number(n) || 0), 0)
      return (this.messageUnread || 0) + note
    }
  },
  watch: {
    isLogin(val) {
      if (val) {
        this.fetchUnread()
        this.fetchMessageUnread()
        this.sendHeartbeat()
        this.timer = setInterval(this.fetchUnread, 30000)
        this.messageTimer = setInterval(this.fetchMessageUnread, 30000)
        this.heartbeatTimer = setInterval(this.sendHeartbeat, 60000)
      } else {
        this.unread = 0
        this.messageUnread = 0
        clearInterval(this.timer)
        clearInterval(this.messageTimer)
        clearInterval(this.heartbeatTimer)
      }
    },
    '$route'(to) {
      if (this.isLogin) {
        this.fetchUnread()
        this.fetchMessageUnread()
      }
      if (to.path === '/search' && to.query.keyword) {
        this.searchKeyword = to.query.keyword
      }
    }
  },
  mounted() {
    this.$root.$on('refresh-unread', this.fetchUnread)
    this.$root.$on('refresh-message-unread', this.fetchMessageUnread)
    this.$root.$on('search-history-pref-changed', this.refreshSearchPref)
    this.refreshSearchPref()
    if (this.isLogin) {
      this.fetchUnread()
      this.fetchMessageUnread()
      this.sendHeartbeat()
      this.timer = setInterval(this.fetchUnread, 30000)
      this.messageTimer = setInterval(this.fetchMessageUnread, 30000)
      this.heartbeatTimer = setInterval(this.sendHeartbeat, 60000)
    }
  },
  beforeDestroy() {
    this.$root.$off('refresh-unread', this.fetchUnread)
    this.$root.$off('refresh-message-unread', this.fetchMessageUnread)
    this.$root.$off('search-history-pref-changed', this.refreshSearchPref)
    clearInterval(this.timer)
    clearInterval(this.messageTimer)
    clearInterval(this.heartbeatTimer)
  },
  methods: {
    async sendHeartbeat() {
      if (!this.isLogin) return
      try {
        await userApi.heartbeat()
      } catch (e) {
        // ignore heartbeat errors
      }
    },
    refreshSearchPref() {
      this.searchHistoryEnabled = getSearchHistoryEnabled()
      this.searchHistory = getSearchHistory()
    },
    onSearchFocus() {
      this.refreshSearchPref()
      if (this.searchHistoryEnabled && this.searchHistory.length) {
        this.showSearchDropdown = true
      }
    },
    onSearchBlur() {
      setTimeout(() => {
        this.showSearchDropdown = false
      }, 150)
    },
    pickHistory(keyword) {
      this.searchKeyword = keyword
      this.showSearchDropdown = false
      this.goSearch()
    },
    removeHistoryItem(keyword) {
      removeSearchHistory(keyword)
      this.searchHistory = getSearchHistory()
      if (!this.searchHistory.length) {
        this.showSearchDropdown = false
      }
    },
    clearAllHistory() {
      clearSearchHistory()
      this.searchHistory = []
      this.showSearchDropdown = false
    },
    goBack() {
      const from = this.$route.query.from
      if (typeof from === 'string' && from.startsWith('/')) {
        this.$router.push(from).catch(() => {})
        return
      }
      if (window.history.length > 1) {
        this.$router.back()
      } else {
        this.$router.push('/').catch(() => {})
      }
    },
    logout() {
      this.$store.dispatch('logout')
      this.$router.push('/login')
    },
    goSearch() {
      const keyword = (this.searchKeyword || '').trim()
      if (!keyword) {
        if (this.$route.path !== '/search') {
          this.$router.push('/search').catch(() => {})
        }
        return
      }
      if (this.searchHistoryEnabled) {
        addSearchHistory(keyword)
        this.searchHistory = getSearchHistory()
      }
      this.showSearchDropdown = false
      const sameSearch = this.$route.path === '/search'
        && String(this.$route.query.keyword || '') === keyword
      if (sameSearch) {
        this.$root.$emit('header-search')
        return
      }
      this.$router.push({ path: '/search', query: { keyword } }).catch(() => {})
    },
    async fetchUnread() {
      try {
        const res = await notificationApi.unreadCount()
        this.unread = res.data || 0
      } catch (e) {
        this.unread = 0
      }
    },
    async fetchMessageUnread() {
      if (this.isAdmin) {
        this.messageUnread = 0
        this.notificationUnread = {}
        return
      }
      try {
        const [msgRes, noteRes] = await Promise.all([
          messageApi.unreadCount(),
          notificationApi.unreadSummary()
        ])
        this.messageUnread = msgRes.data || 0
        this.notificationUnread = (noteRes.data && noteRes.data.byCategory) || {}
        this.unread = noteRes.data
          ? (Number(noteRes.data.reply || 0) + Number(noteRes.data.mention || 0)
            + Number(noteRes.data.like || 0) + Number(noteRes.data.system || 0))
          : 0
      } catch (e) {
        this.messageUnread = 0
        this.notificationUnread = {}
      }
    }
  }
}
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #9eb0bc;
}
.layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #9eb0bc;
}
.layout:not(.auth-layout)::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  background: url('~@/assets/images/app-bg.png') center center / cover no-repeat;
  filter: brightness(0.92) saturate(0.65);
  pointer-events: none;
}
.layout:not(.auth-layout)::after {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    #409EFF 0,
    #409EFF 60px,
    rgba(64, 158, 255, 0.78) 130px,
    rgba(64, 158, 255, 0.42) 210px,
    rgba(64, 158, 255, 0.16) 290px,
    transparent 380px
  );
}
.layout > * {
  position: relative;
  z-index: 1;
}
.layout.auth-layout {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
}
.layout.auth-layout .el-main.auth-main {
  flex: 1;
  padding: 0 !important;
  overflow: hidden;
  background: transparent;
}
.layout .el-main.auth-main {
  padding: 0 !important;
  overflow: hidden;
}
.layout:not(.auth-layout) .el-main {
  background: transparent;
}
.layout:not(.auth-layout) .el-card {
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.55);
  box-shadow: 0 6px 24px rgba(30, 90, 140, 0.08);
}
.layout:not(.auth-layout) .el-aside {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border-right: 1px solid rgba(255, 255, 255, 0.55);
}
.header { display: flex; align-items: center; background: #409EFF; color: #fff; padding: 0 20px; height: 60px !important; flex-shrink: 0; }
.header-back-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 8px;
  flex-shrink: 0;
}
.header-back-btn:hover { color: #ffd04b; background: rgba(255, 255, 255, 0.12); }
.brand { font-size: 20px; font-weight: bold; cursor: pointer; margin-right: 24px; white-space: nowrap; }
.nav-menu { flex: 1; border-bottom: none !important; }
.auth-area { display: flex; align-items: center; gap: 12px; margin-left: 16px; }
.header-search-wrap { position: relative; }
.header-search {
  display: flex;
  align-items: center;
  background: #1864ab;
  border-radius: 999px;
  padding: 3px 3px 3px 12px;
  min-width: 220px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  color: #303133;
  min-width: 0;
}
.search-input::placeholder { color: #909399; }
.search-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.search-btn:hover { color: #ffd04b; }
.search-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  padding: 6px 0 4px;
  overflow: hidden;
}
.search-history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: 13px;
  color: #303133;
}
.search-history-item:hover { background: #f5f7fa; }
.history-text {
  flex: 1;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-remove {
  color: #f56c6c;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0 4px;
  margin-left: 8px;
  flex-shrink: 0;
}
.history-remove:hover { color: #f78989; }
.search-dropdown-footer {
  display: flex;
  justify-content: flex-end;
  padding: 6px 12px 4px;
  border-top: 1px solid #ebeef5;
}
.clear-all {
  color: #f56c6c;
  font-size: 12px;
  cursor: pointer;
}
.clear-all:hover { color: #f78989; }
.username { margin-right: 8px; }
.badge { background: #f56c6c; border-radius: 10px; padding: 0 6px; margin-left: 4px; font-size: 12px; }
</style>
