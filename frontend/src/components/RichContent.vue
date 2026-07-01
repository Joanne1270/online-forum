<template>
  <div ref="root" class="rich-content" @click="onClick">
    <div v-html="html"></div>
    <image-lightbox ref="lightbox" />
  </div>
</template>

<script>
import { renderPostContent, extractMentionNicknames } from '../utils/content'
import { userApi } from '../api/user'
import ImageLightbox from './ImageLightbox.vue'

export default {
  name: 'RichContent',
  components: { ImageLightbox },
  props: {
    content: { type: String, default: '' },
    mentionUserIds: { type: Array, default: () => [] }
  },
  data() {
    return {
      mentionMap: {}
    }
  },
  computed: {
    html() {
      return renderPostContent(this.content, this.mentionMap)
    }
  },
  watch: {
    content: {
      immediate: true,
      handler() {
        this.loadMentions()
      }
    },
    mentionUserIds: {
      immediate: true,
      handler() {
        this.loadMentions()
      }
    }
  },
  methods: {
    async openMentionProfile(mention) {
      const cachedId = mention.getAttribute('data-user-id')
      if (cachedId) {
        this.$router.push(`/user/${cachedId}`)
        return
      }
      const nickname = mention.getAttribute('data-nickname')
      if (!nickname) return
      if (this.mentionUserIds.length === 1) {
        this.$router.push(`/user/${this.mentionUserIds[0]}`)
        return
      }
      try {
        const res = await userApi.batchByNicknames([nickname])
        const user = (res.data || []).find(item => item.nickname === nickname) || (res.data || [])[0]
        if (user && user.id) {
          this.$set(this.mentionMap, nickname, user.id)
          this.$router.push(`/user/${user.id}`)
          return
        }
        this.$message.warning('未找到该用户')
      } catch (e) {
        this.$message.warning('无法打开用户主页')
      }
    },
    async loadMentions() {
      const nicknames = extractMentionNicknames(this.content)
      const map = {}
      if (nicknames.length) {
        try {
          const res = await userApi.batchByNicknames(nicknames)
          for (const user of res.data || []) {
            if (user.nickname) map[user.nickname] = user.id
          }
        } catch (e) {
          // ignore
        }
      }
      const unresolved = nicknames.filter(name => !map[name])
      if (unresolved.length && this.mentionUserIds.length) {
        if (unresolved.length === 1 && this.mentionUserIds.length === 1) {
          map[unresolved[0]] = this.mentionUserIds[0]
        } else {
          unresolved.forEach((name, index) => {
            if (this.mentionUserIds[index] && !map[name]) {
              map[name] = this.mentionUserIds[index]
            }
          })
        }
      }
      this.mentionMap = map
    },
    onClick(event) {
      const hashtag = event.target.closest('.hashtag-link')
      if (hashtag && this.$refs.root.contains(hashtag)) {
        event.preventDefault()
        event.stopPropagation()
        const tag = hashtag.getAttribute('data-tag')
        if (tag) {
          this.$router.push({ path: '/search', query: { keyword: `#${tag}` } })
        }
        return
      }
      const mention = event.target.closest('.mention-link')
      if (mention && this.$refs.root.contains(mention)) {
        event.preventDefault()
        event.stopPropagation()
        this.openMentionProfile(mention)
        return
      }
      const button = event.target.closest('.content-image-zoom')
      if (!button || !this.$refs.root.contains(button)) return
      event.preventDefault()
      event.stopPropagation()
      const src = button.getAttribute('data-src')
      if (src) {
        this.$refs.lightbox.open(src)
      }
    }
  }
}
</script>

<style scoped>
.rich-content {
  line-height: 1.8;
}
::v-deep .content-image-wrap {
  position: relative;
  display: inline-block;
  max-width: 360px;
  margin: 8px 0;
  vertical-align: top;
}
::v-deep .content-image {
  display: block;
  max-width: 100%;
  max-height: 240px;
  width: auto;
  height: auto;
  border-radius: 4px;
  background: #f5f7fa;
}
::v-deep .content-image-zoom {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  padding: 0;
  transition: background 0.2s;
}
::v-deep .content-image-zoom:hover {
  background: rgba(64, 158, 255, 0.9);
}
::v-deep .content-image-zoom i {
  pointer-events: none;
}
::v-deep video {
  max-width: 360px;
  max-height: 240px;
  display: block;
  margin: 8px 0;
  border-radius: 4px;
}
::v-deep a {
  color: #409EFF;
}
::v-deep .mention-link {
  color: #0D47A1;
  font-weight: 500;
  cursor: pointer;
}
::v-deep .mention-link:hover {
  text-decoration: underline;
}
::v-deep .hashtag-link {
  color: #409EFF;
  cursor: pointer;
  text-decoration: none;
}
::v-deep .hashtag-link:hover {
  text-decoration: underline;
}
</style>
