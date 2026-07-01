<template>
  <div class="follow-user-list">
    <div v-for="u in users" :key="u.id" class="follow-item">
      <el-avatar
        :size="48"
        :src="u.avatar"
        icon="el-icon-user-solid"
        class="clickable-avatar"
        @click.native="goProfile(u.id)"
      />
      <div class="info">
        <div class="name clickable-name" @click="goProfile(u.id)">{{ u.nickname }}</div>
        <div class="bio">{{ u.bio || '暂无简介' }}</div>
        <div class="time">{{ timeLabel }} {{ formatTime(u.followedAt) }}</div>
      </div>
      <div v-if="showUnfollow" class="actions">
        <el-button size="mini" @click="$emit('unfollow', u)">取消关注</el-button>
      </div>
    </div>
    <el-empty v-if="!users.length" :description="emptyText" :image-size="60" />
  </div>
</template>

<script>
export default {
  props: {
    users: { type: Array, default: () => [] },
    timeLabel: { type: String, default: '关注于' },
    emptyText: { type: String, default: '暂无数据' },
    showUnfollow: { type: Boolean, default: false }
  },
  methods: {
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    goProfile(id) {
      this.$router.push({
        path: `/user/${id}`,
        query: { from: this.$route.fullPath }
      })
    }
  }
}
</script>

<style scoped>
.follow-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}
.info { flex: 1; min-width: 0; }
.name { font-size: 16px; font-weight: 600; color: #303133; }
.clickable-avatar,
.clickable-name {
  cursor: pointer;
}
.clickable-name:hover { color: #409EFF; }
.clickable-avatar:hover { opacity: 0.85; }
.bio { color: #606266; font-size: 13px; margin-top: 4px; }
.time { color: #909399; font-size: 12px; margin-top: 4px; }
.actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
