<template>
  <div class="mention-input">
    <div v-if="showEmoji" class="input-toolbar">
      <emoji-picker @select="insertEmoji" />
    </div>
    <div class="textarea-wrap">
      <div
        v-show="!isComposing"
        ref="backdrop"
        class="textarea-backdrop"
        v-html="highlightedHtml"
      />
      <el-input
        ref="textarea"
        v-model="innerValue"
        type="textarea"
        :rows="rows"
        :placeholder="placeholder"
        :class="['textarea-field', { 'is-composing': isComposing }]"
        @input="onInput"
        @keyup.native="onKeyup"
        @blur="saveSelection"
        @click.native="onClickTextarea"
        @scroll.native="syncScroll"
      />
      <div v-if="showAtPanel" class="at-dropdown">
        <div v-if="!userSuggestions.length" class="suggestion-hint">输入昵称搜索要 @ 的用户</div>
        <div
          v-for="item in userSuggestions"
          :key="item.id"
          class="suggestion-item"
          @mousedown.prevent="pickUser(item)"
        >
          @{{ item.nickname }}
        </div>
      </div>
    </div>
    <div v-if="showTagPanel" class="tag-suggestions">
      <div class="tag-suggestions-label">{{ tagKeyword ? '匹配标签' : '热门标签' }}</div>
      <div v-if="tagSuggestions.length" class="tag-suggestions-list">
        <button
          v-for="item in tagSuggestions"
          :key="item.id || item.name"
          type="button"
          class="tag-pill"
          @mousedown.prevent="pickTag(item)"
        >
          #{{ item.name }}
          <span v-if="item.usageCount" class="tag-pill-count">{{ item.usageCount }}</span>
        </button>
      </div>
      <div v-else class="tag-suggestions-empty">暂无标签，输入名称后按空格创建</div>
    </div>
  </div>
</template>

<script>
import { userApi } from '../api/user'
import { tagApi } from '../api/tag'
import EmojiPicker from './EmojiPicker.vue'
import { insertTextAtCursor, focusTextarea } from '../utils/emojis'
import {
  highlightEditorContent,
  getEditorPanelContext,
  parseApiMentions,
  toApiMentionFormat,
  syncMentionMap
} from '../utils/content'

function normalizeInputText(text) {
  return (text || '').replace(/\uFF20/g, '@')
}

function getCursorPosition(vm) {
  const textarea = vm.getNativeTextarea()
  if (textarea && typeof textarea.selectionStart === 'number') {
    return textarea.selectionStart
  }
  if (vm.savedSelection && typeof vm.savedSelection.start === 'number') {
    return vm.savedSelection.start
  }
  return vm.innerValue.length
}

function initEditorState(value) {
  const parsed = parseApiMentions(normalizeInputText(value || ''))
  return {
    innerValue: parsed.text,
    mentionIdByNickname: parsed.mentionIdByNickname
  }
}

export default {
  components: { EmojiPicker },
  props: {
    value: String,
    placeholder: { type: String, default: '' },
    rows: { type: Number, default: 4 },
    showEmoji: { type: Boolean, default: false }
  },
  data() {
    const initial = initEditorState(this.value)
    return {
      innerValue: initial.innerValue,
      mentionIdByNickname: initial.mentionIdByNickname,
      userSuggestions: [],
      tagSuggestions: [],
      timer: null,
      showAtPanel: false,
      showTagPanel: false,
      tagKeyword: '',
      atKeyword: '',
      savedSelection: null,
      isComposing: false
    }
  },
  computed: {
    highlightedHtml() {
      return highlightEditorContent(this.innerValue, this.mentionIdByNickname)
    }
  },
  watch: {
    value(v) {
      const apiFromInner = toApiMentionFormat(this.innerValue, this.mentionIdByNickname)
      if (apiFromInner === (v || '')) return
      const parsed = initEditorState(v)
      this.innerValue = parsed.innerValue
      this.mentionIdByNickname = parsed.mentionIdByNickname
    }
  },
  mounted() {
    this.$nextTick(this.bindCompositionEvents)
  },
  beforeDestroy() {
    this.unbindCompositionEvents()
    clearTimeout(this.timer)
  },
  methods: {
    bindCompositionEvents() {
      const textarea = this.getNativeTextarea()
      if (!textarea || textarea.__tagComposeBound) return
      textarea.__tagComposeBound = true
      textarea.addEventListener('compositionstart', this.onCompositionStart)
      textarea.addEventListener('compositionend', this.onCompositionEnd)
    },
    unbindCompositionEvents() {
      const textarea = this.getNativeTextarea()
      if (!textarea || !textarea.__tagComposeBound) return
      textarea.removeEventListener('compositionstart', this.onCompositionStart)
      textarea.removeEventListener('compositionend', this.onCompositionEnd)
      textarea.__tagComposeBound = false
    },
    onCompositionStart() {
      this.isComposing = true
    },
    onCompositionEnd(event) {
      this.isComposing = false
      this.handleTextChange(event.target.value)
    },
    resolveInputText(v) {
      if (typeof v === 'string') return normalizeInputText(v)
      if (v && v.target) return normalizeInputText(v.target.value)
      return normalizeInputText(this.innerValue)
    },
    onInput(v) {
      this.handleTextChange(this.resolveInputText(v))
    },
    onKeyup() {
      this.saveSelection()
      this.updatePanels(this.innerValue)
      this.scheduleRemoteFetch()
    },
    handleTextChange(text, nextCursor) {
      this.innerValue = text
      this.mentionIdByNickname = syncMentionMap(text, this.mentionIdByNickname)
      this.$emit('input', toApiMentionFormat(text, this.mentionIdByNickname))
      this.updatePanels(text)
      this.scheduleRemoteFetch()
      if (typeof nextCursor === 'number') {
        this.savedSelection = { start: nextCursor, end: nextCursor }
      } else {
        this.saveSelection()
      }
      if (!this.isComposing) {
        this.$nextTick(() => {
          if (typeof nextCursor === 'number') {
            focusTextarea(this.getNativeTextarea(), nextCursor)
          }
          this.syncScroll()
        })
      }
    },
    scheduleRemoteFetch() {
      clearTimeout(this.timer)
      this.timer = setTimeout(() => this.fetchRemoteSuggestions(this.innerValue), 200)
    },
    updatePanels(text) {
      const normalized = normalizeInputText(text)
      const cursor = getCursorPosition(this)
      const ctx = getEditorPanelContext(normalized, cursor, this.mentionIdByNickname)
      if (ctx.kind === 'tag') {
        this.showTagPanel = true
        this.showAtPanel = false
        this.tagKeyword = ctx.keyword
        this.atKeyword = ''
        this.userSuggestions = []
        return
      }
      if (ctx.kind === 'at') {
        this.showAtPanel = true
        this.showTagPanel = false
        this.atKeyword = ctx.keyword
        this.tagKeyword = ''
        this.tagSuggestions = []
        return
      }
      this.showAtPanel = false
      this.showTagPanel = false
      this.atKeyword = ''
      this.tagKeyword = ''
      this.userSuggestions = []
      this.tagSuggestions = []
    },
    async fetchRemoteSuggestions(text) {
      const normalized = normalizeInputText(text)
      const cursor = getCursorPosition(this)
      const ctx = getEditorPanelContext(normalized, cursor, this.mentionIdByNickname)
      if (ctx.kind === 'tag') {
        try {
          const res = await tagApi.search({ keyword: ctx.keyword || '', limit: 12 })
          this.tagSuggestions = res.data || []
        } catch (e) {
          this.tagSuggestions = []
        }
        return
      }
      if (ctx.kind === 'at') {
        if (!ctx.keyword) {
          this.userSuggestions = []
          return
        }
        try {
          const res = await userApi.search(ctx.keyword)
          this.userSuggestions = res.data || []
        } catch (e) {
          this.userSuggestions = []
        }
      }
    },
    syncScroll() {
      const textarea = this.getNativeTextarea()
      const backdrop = this.$refs.backdrop
      if (textarea && backdrop) {
        backdrop.scrollTop = textarea.scrollTop
        backdrop.scrollLeft = textarea.scrollLeft
      }
    },
    getNativeTextarea() {
      const input = this.$refs.textarea
      if (!input) return null
      if (input.$refs && input.$refs.textarea) return input.$refs.textarea
      if (input.$el) return input.$el.querySelector('textarea')
      return null
    },
    onClickTextarea() {
      this.saveSelection()
      this.updatePanels(this.innerValue)
    },
    saveSelection() {
      const textarea = this.getNativeTextarea()
      if (!textarea || typeof textarea.selectionStart !== 'number') return
      this.savedSelection = {
        start: textarea.selectionStart,
        end: textarea.selectionEnd
      }
    },
    insertEmoji(emoji) {
      const textarea = this.getNativeTextarea()
      const active = textarea && document.activeElement === textarea
      const selection = active ? null : this.savedSelection
      const result = insertTextAtCursor(textarea, emoji, this.innerValue, selection)
      this.handleTextChange(result.value)
      this.savedSelection = { start: result.pos, end: result.pos }
      this.$nextTick(() => {
        focusTextarea(textarea, result.pos)
        this.syncScroll()
      })
    },
    pickUser(user) {
      const cursor = getCursorPosition(this)
      const before = this.innerValue.slice(0, cursor)
      const after = this.innerValue.slice(cursor)
      const mention = `@${user.nickname}`
      const nextBefore = before.replace(/@([^@\s]*)$/, mention)
      const next = nextBefore + after
      const pos = nextBefore.length
      this.$set(this.mentionIdByNickname, user.nickname, user.id)
      this.handleTextChange(next, pos)
      this.userSuggestions = []
      this.showAtPanel = false
      this.atKeyword = ''
    },
    pickTag(item) {
      const cursor = getCursorPosition(this)
      const before = this.innerValue.slice(0, cursor)
      const after = this.innerValue.slice(cursor)
      const nextBefore = before.replace(/#([^#\s@]*)$/, `#${item.name} `)
      const next = nextBefore + after
      this.handleTextChange(next, nextBefore.length)
      this.tagSuggestions = []
      this.showTagPanel = false
      this.tagKeyword = ''
    }
  }
}
</script>

<style scoped>
.mention-input { position: relative; }
.input-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.textarea-wrap {
  position: relative;
}
.textarea-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 5px 15px;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-word;
  overflow: hidden;
  pointer-events: none;
  color: #606266;
  border: 1px solid transparent;
  box-sizing: border-box;
}
.textarea-backdrop ::v-deep .tag-complete {
  color: #409EFF;
}
.textarea-backdrop ::v-deep .mention-complete {
  color: #0D47A1;
}
.mention-input >>> .textarea-field:not(.is-composing) .el-textarea__inner {
  background: transparent !important;
  color: transparent !important;
  caret-color: #606266;
}
.mention-input >>> .textarea-field.is-composing .el-textarea__inner {
  background: #fff !important;
  color: #606266 !important;
}
.at-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 20;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 180px;
  overflow: auto;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.suggestion-hint {
  padding: 10px 12px;
  color: #909399;
  font-size: 13px;
}
.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  color: #409EFF;
}
.suggestion-item:hover {
  background: #f5f7fa;
}
.tag-suggestions {
  margin-top: 8px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.tag-suggestions-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.tag-suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #d9ecff;
  border-radius: 999px;
  background: #ecf5ff;
  color: #409EFF;
  font-size: 13px;
  cursor: pointer;
  line-height: 1.4;
}
.tag-pill:hover {
  background: #d9ecff;
}
.tag-pill-count {
  font-size: 11px;
  color: #909399;
}
.tag-suggestions-empty {
  font-size: 13px;
  color: #909399;
}
</style>
