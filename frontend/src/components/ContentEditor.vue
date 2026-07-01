<template>
  <div class="content-editor">
    <mention-input
      ref="mentionInput"
      v-model="textPart"
      :rows="rows"
      :placeholder="placeholder"
      :show-emoji="false"
      @input="emitValue"
    />
    <div v-if="mediaItems.length" class="media-list">
      <div v-for="(item, index) in mediaItems" :key="index" class="media-item">
        <img v-if="item.type === 'image'" :src="item.url" class="preview-image" alt="图片" />
        <video v-else :src="item.url" controls class="preview-video" />
        <el-button type="text" size="mini" class="remove-btn" @click="removeMedia(index)">删除</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import MentionInput from './MentionInput.vue'
import { trimPostContent, prepareEditorText } from '../utils/content'

const IMAGE_RE = /!\[[^\]]*\]\(([^)]+)\)/g
const VIDEO_RE = /<video[^>]*src="([^"]+)"[^>]*>\s*<\/video>/g

function extractMedia(content) {
  const items = []
  if (!content) return items
  let m
  const imgRe = new RegExp(IMAGE_RE.source, 'g')
  while ((m = imgRe.exec(content)) !== null) {
    items.push({ type: 'image', url: m[1], raw: m[0] })
  }
  const vidRe = new RegExp(VIDEO_RE.source, 'g')
  while ((m = vidRe.exec(content)) !== null) {
    items.push({ type: 'video', url: m[1], raw: m[0] })
  }
  return items
}

function stripMedia(content) {
  if (!content) return ''
  return prepareEditorText(
    content
      .replace(IMAGE_RE, '')
      .replace(VIDEO_RE, '')
      .replace(/\n{3,}/g, '\n\n')
  )
}

function buildContent(text, mediaItems) {
  const parts = [trimPostContent(text || '')]
  for (const item of mediaItems) {
    if (item.type === 'video') {
      parts.push(`<video src="${item.url}" controls style="max-width:100%"></video>`)
    } else {
      parts.push(`![image](${item.url})`)
    }
  }
  return parts.filter(Boolean).join('\n\n')
}

export default {
  components: { MentionInput },
  props: {
    value: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    rows: { type: Number, default: 8 }
  },
  data() {
    return {
      textPart: stripMedia(this.value),
      mediaItems: extractMedia(this.value)
    }
  },
  watch: {
    value(v) {
      const nextText = stripMedia(v)
      const nextMedia = extractMedia(v)
      if (buildContent(this.textPart, this.mediaItems) !== v) {
        this.textPart = nextText
        this.mediaItems = nextMedia
      }
    }
  },
  methods: {
    emitValue() {
      this.$emit('input', buildContent(this.textPart, this.mediaItems))
    },
    syncContent() {
      this.emitValue()
    },
    addMedia({ url, mime }) {
      if (mime && mime.startsWith('video/')) {
        this.mediaItems.push({ type: 'video', url })
      } else {
        this.mediaItems.push({ type: 'image', url })
      }
      this.emitValue()
    },
    removeMedia(index) {
      this.mediaItems.splice(index, 1)
      this.emitValue()
    },
    insertEmoji(emoji) {
      const input = this.$refs.mentionInput
      if (input) input.insertEmoji(emoji)
    }
  }
}
</script>

<style scoped>
.content-editor {
  overflow: visible;
}
.media-list { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 12px; }
.media-item { position: relative; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px; background: #fafafa; }
.preview-image { display: block; max-width: 200px; max-height: 160px; border-radius: 4px; }
.preview-video { display: block; max-width: 240px; max-height: 160px; border-radius: 4px; }
.remove-btn { display: block; margin-top: 4px; color: #f56c6c; }
</style>
