<template>
  <span class="vote-icon-stack">
    <span
      v-if="active"
      class="vote-fill"
      :style="fillStyle"
      aria-hidden="true"
    />
    <img
      :src="outlineSrc"
      class="vote-outline"
      :class="{ 'outline-soft': active && tone === 'like' }"
      alt=""
      aria-hidden="true"
    />
  </span>
</template>

<script>
import thumbUp from '@/assets/icons/thumb-up.png'
import thumbDown from '@/assets/icons/thumb-down.png'
import thumbUpFill from '@/assets/icons/thumb-up-fill.png'
import thumbDownFill from '@/assets/icons/thumb-down-fill.png'

export default {
  name: 'VoteIcon',
  props: {
    type: {
      type: String,
      required: true,
      validator: value => ['up', 'down'].includes(value)
    },
    active: {
      type: Boolean,
      default: false
    },
    tone: {
      type: String,
      default: '',
      validator: value => ['', 'like', 'dislike'].includes(value)
    }
  },
  computed: {
    outlineSrc() {
      return this.type === 'up' ? thumbUp : thumbDown
    },
    fillStyle() {
      const url = this.type === 'up' ? thumbUpFill : thumbDownFill
      const color = this.tone === 'dislike' ? '#c0c4cc' : '#ff0000'
      return {
        backgroundColor: color,
        WebkitMaskImage: `url(${url})`,
        maskImage: `url(${url})`
      }
    }
  }
}
</script>

<style scoped>
.vote-icon-stack {
  position: relative;
  display: block;
  width: 36px;
  height: 36px;
}
.vote-fill,
.vote-outline {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.vote-fill {
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
  -webkit-mask-position: center;
  mask-position: center;
  -webkit-mask-size: contain;
  mask-size: contain;
}
.vote-outline {
  z-index: 1;
}
.vote-outline.outline-soft {
  opacity: 0.45;
  filter: brightness(2.2);
}
</style>
