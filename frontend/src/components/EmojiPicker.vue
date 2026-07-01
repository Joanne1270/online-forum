<template>
  <div ref="root" class="emoji-picker">
    <button
      ref="trigger"
      type="button"
      class="emoji-trigger"
      :class="{ active: visible }"
      title="表情"
      @mousedown.prevent
      @click.stop="toggle"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="9.5" fill="none" stroke="currentColor" stroke-width="1.5" />
        <circle cx="9" cy="10" r="1.2" fill="currentColor" />
        <circle cx="15" cy="10" r="1.2" fill="currentColor" />
        <path d="M8.5 14.2c1 1.4 2.2 2.1 3.5 2.1s2.5-.7 3.5-2.1" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      </svg>
    </button>
    <transition name="emoji-fade">
      <div
        v-if="visible"
        ref="panel"
        class="emoji-panel"
        :style="panelStyle"
        @click.stop
      >
        <div v-if="recentEmojis.length" class="emoji-section">
          <div class="section-title">最近使用</div>
          <div class="emoji-grid">
            <button
              v-for="(emoji, index) in recentEmojis"
              :key="'recent-' + index"
              type="button"
              class="emoji-item"
              @click="pick(emoji)"
            >{{ emoji }}</button>
          </div>
        </div>
        <div class="emoji-section">
          <div class="section-title">所有表情</div>
          <div class="emoji-grid emoji-grid-scroll">
            <button
              v-for="(emoji, index) in emojiList"
              :key="'all-' + index"
              type="button"
              class="emoji-item"
              @click="pick(emoji)"
            >{{ emoji }}</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script>
import { EMOJI_LIST, getRecentEmojis, addRecentEmoji } from '../utils/emojis'

const PANEL_WIDTH = 360
const PANEL_GAP = 8

export default {
  name: 'EmojiPicker',
  data() {
    return {
      visible: false,
      emojiList: EMOJI_LIST,
      recentEmojis: getRecentEmojis(),
      panelStyle: {}
    }
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick)
    window.addEventListener('resize', this.handleViewportChange)
    window.addEventListener('scroll', this.handleViewportChange, true)
  },
  beforeDestroy() {
    document.removeEventListener('click', this.handleOutsideClick)
    window.removeEventListener('resize', this.handleViewportChange)
    window.removeEventListener('scroll', this.handleViewportChange, true)
  },
  methods: {
    toggle() {
      this.visible = !this.visible
      if (this.visible) {
        this.recentEmojis = getRecentEmojis()
        this.$nextTick(this.updatePanelPosition)
      }
    },
    close() {
      this.visible = false
    },
    handleViewportChange() {
      if (!this.visible) return
      this.updatePanelPosition()
    },
    handleOutsideClick(e) {
      if (!this.$refs.root || !this.$refs.root.contains(e.target)) {
        this.visible = false
      }
    },
    updatePanelPosition() {
      const trigger = this.$refs.trigger
      const panel = this.$refs.panel
      if (!trigger || !panel) return

      const rect = trigger.getBoundingClientRect()
      const panelHeight = panel.offsetHeight || 300
      const panelWidth = Math.min(PANEL_WIDTH, window.innerWidth - 16)
      const spaceBelow = window.innerHeight - rect.bottom - PANEL_GAP
      const spaceAbove = rect.top - PANEL_GAP
      const openDown = spaceBelow >= panelHeight || spaceBelow >= spaceAbove

      let top
      if (openDown) {
        top = rect.bottom + PANEL_GAP
      } else {
        top = Math.max(PANEL_GAP, rect.top - panelHeight - PANEL_GAP)
      }

      let left = rect.left
      if (left + panelWidth > window.innerWidth - PANEL_GAP) {
        left = window.innerWidth - panelWidth - PANEL_GAP
      }
      left = Math.max(PANEL_GAP, left)

      this.panelStyle = {
        position: 'fixed',
        top: `${top}px`,
        left: `${left}px`,
        width: `${panelWidth}px`,
        zIndex: 3000
      }
    },
    pick(emoji) {
      addRecentEmoji(emoji)
      this.recentEmojis = getRecentEmojis()
      this.$emit('select', emoji)
      this.close()
    }
  }
}
</script>

<style scoped>
.emoji-picker {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.emoji-trigger {
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
  transition: color 0.15s, background 0.15s;
}

.emoji-trigger svg {
  width: 22px;
  height: 22px;
}

.emoji-trigger:hover,
.emoji-trigger.active {
  color: #409EFF;
  background: #ecf5ff;
}

.emoji-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 12px;
  box-sizing: border-box;
}

.emoji-section + .emoji-section {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.section-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  padding-left: 2px;
  line-height: 1.2;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.emoji-grid-scroll {
  max-height: 220px;
  overflow-y: auto;
  padding-right: 2px;
}

.emoji-grid-scroll::-webkit-scrollbar {
  width: 6px;
}

.emoji-grid-scroll::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.emoji-grid-scroll::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

.emoji-item {
  width: 100%;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 22px;
  line-height: 36px;
  cursor: pointer;
  padding: 0;
  transition: background 0.12s;
}

.emoji-item:hover {
  background: #ecf5ff;
}

.emoji-fade-enter-active,
.emoji-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.emoji-fade-enter,
.emoji-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
