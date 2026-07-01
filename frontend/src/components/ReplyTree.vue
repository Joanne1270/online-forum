<template>
  <div class="reply-tree">
    <div v-for="r in replies" :key="r.id" :id="'reply-' + r.id" class="reply-node" :class="{ pinned: r.pinned }">
      <div class="reply-head">
        <strong
          :class="{ 'author-link': r.authorId }"
          @click.stop="r.authorId && goAuthor(r.authorId)"
        >{{ r.authorName }}</strong>
        <span v-if="r.pinned" class="pin-tag">置顶</span>
        <span class="time">{{ formatTime(r.createdAt) }}</span>
      </div>
      <rich-content :content="r.content" :mention-user-ids="r.mentionedUserIds || []" class="reply-content" />
      <div class="reply-actions">
        <el-button v-if="isLogin" type="text" size="mini" class="vote-btn vote-btn-like" :class="{ liked: r.liked }" @click="$emit('like', r)">
          <span class="vote-icon" aria-hidden="true">
            <vote-icon type="up" :active="r.liked" tone="like" />
          </span>
          <span class="vote-count">{{ r.likeCount || 0 }}</span>
        </el-button>
        <el-button v-if="isLogin" type="text" size="mini" class="vote-btn vote-btn-dislike" :class="{ disliked: r.disliked }" @click="$emit('dislike', r)">
          <span class="vote-icon" aria-hidden="true">
            <vote-icon type="down" :active="r.disliked" tone="dislike" />
          </span>
        </el-button>
        <el-button v-if="isLogin" type="text" size="mini" class="text-action-btn" :class="{ active: activeReplyId === r.id }" @click="$emit('reply', r)">
          回复
        </el-button>
        <el-button v-if="allowPin" type="text" size="mini" @click="$emit('pin-reply', r)">
          {{ r.pinned ? '取消置顶' : '置顶' }}
        </el-button>
        <el-button v-if="isLogin && isOwner(r)" type="text" size="mini" class="text-action-btn" @click="$emit('delete', r.id)">删除</el-button>
        <el-button v-if="isLogin && !isOwner(r)" type="text" size="mini" class="text-action-btn" @click="$emit('report', r)">举报</el-button>
      </div>
      <div v-if="isLogin && activeReplyId === r.id" class="nested-reply-box">
        <div class="nested-label">回复 @{{ r.authorName }}</div>
        <mention-input :ref="'nestedInput-' + r.id" :value="nestedReply" :show-emoji="false" placeholder="写下回复" @input="$emit('update:nestedReply', $event)" />
        <div class="nested-submit-row">
          <emoji-picker @select="emoji => insertNestedEmoji(emoji, r.id)" />
          <el-button type="primary" size="mini" @click="$emit('submit-nested')">回复</el-button>
        </div>
      </div>
      <reply-tree
        v-if="r.children && r.children.length"
        :replies="r.children"
        :is-login="isLogin"
        :current-user-id="currentUserId"
        :active-reply-id="activeReplyId"
        :nested-reply="nestedReply"
        :allow-pin="false"
        @like="$emit('like', $event)"
        @dislike="$emit('dislike', $event)"
        @reply="$emit('reply', $event)"
        @delete="$emit('delete', $event)"
        @report="$emit('report', $event)"
        @pin-reply="$emit('pin-reply', $event)"
        @update:nestedReply="$emit('update:nestedReply', $event)"
        @submit-nested="$emit('submit-nested')"
      />
    </div>
  </div>
</template>

<script>
import RichContent from './RichContent.vue'
import MentionInput from './MentionInput.vue'
import EmojiPicker from './EmojiPicker.vue'
import VoteIcon from './VoteIcon.vue'

export default {
  name: 'ReplyTree',
  components: { RichContent, MentionInput, EmojiPicker, VoteIcon },
  props: {
    replies: { type: Array, default: () => [] },
    isLogin: Boolean,
    currentUserId: { type: Number, default: null },
    activeReplyId: { type: Number, default: null },
    nestedReply: { type: String, default: '' },
    allowPin: { type: Boolean, default: false },
    pinnedReplyId: { type: Number, default: null }
  },
  methods: {
    formatTime(t) {
      return t ? String(t).replace('T', ' ').slice(0, 16) : ''
    },
    goAuthor(id) {
      this.$router.push(`/user/${id}`)
    },
    isOwner(reply) {
      return this.currentUserId && reply.authorId === this.currentUserId
    },
    insertNestedEmoji(emoji, replyId) {
      const ref = this.$refs['nestedInput-' + replyId]
      const input = Array.isArray(ref) ? ref[0] : ref
      if (input && input.insertEmoji) input.insertEmoji(emoji)
    }
  }
}
</script>

<style scoped>
.reply-tree {
  background: #fff;
}
.reply-node {
  border-left: 3px solid #ebeef5;
  padding: 12px;
  margin: 8px 0;
  background: #fff;
  border-radius: 4px;
}
.reply-node.pinned { border-left-color: #409EFF; background: #f5faff; }
.reply-head { display: flex; gap: 12px; align-items: center; }
.author-link { color: #409EFF; cursor: pointer; }
.pin-tag { font-size: 11px; color: #409EFF; background: #ecf5ff; padding: 0 6px; border-radius: 3px; }
.time { color: #909399; font-size: 12px; margin-left: auto; }
.reply-content { margin: 8px 0; }
.reply-actions .vote-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: auto;
  padding: 0 2px;
  font-size: 14px;
  line-height: 1.4;
  flex-shrink: 0;
}
.reply-actions .vote-btn + .vote-btn {
  margin-left: 4px;
}
.vote-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 36px;
  line-height: 0;
  color: inherit;
  margin: -8px 0;
}
.vote-btn-like .vote-icon {
  transform: translateY(6px);
}
.vote-btn-dislike .vote-icon {
  transform: translateY(12px);
}
.vote-count {
  display: inline-block;
  flex-shrink: 0;
  min-width: 3ch;
  text-align: left;
  font-variant-numeric: tabular-nums;
  transform: translateY(-3px);
  line-height: 1;
}
.reply-actions .vote-btn.liked .vote-count {
  color: #ff0000;
}
.reply-actions .text-action-btn.active { color: #409EFF; }
.reply-actions .text-action-btn {
  transform: translateY(-4px);
}
.nested-reply-box { margin-top: 8px; padding: 12px; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; }
.nested-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.nested-submit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
</style>
