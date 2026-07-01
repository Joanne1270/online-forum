<template>
  <div class="auth-page">
    <img class="auth-bg" :src="loginBg" alt="" aria-hidden="true" />
    <div class="auth-panel" :style="panelStyle">
      <slot />
    </div>
  </div>
</template>

<script>
import appBg from '@/assets/images/app-bg.png'

export default {
  props: {
    maxWidth: {
      type: Number,
      default: 420
    }
  },
  data() {
    return { loginBg: appBg }
  },
  computed: {
    panelStyle() {
      return { maxWidth: `${this.maxWidth}px` }
    }
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #9eb0bc;
}
.auth-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  filter: brightness(0.92) saturate(0.65);
  z-index: 0;
  pointer-events: none;
  user-select: none;
}
.auth-page::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    #409EFF 0,
    rgba(64, 158, 255, 0.82) 72px,
    rgba(64, 158, 255, 0.48) 160px,
    rgba(64, 158, 255, 0.18) 240px,
    transparent 340px
  );
}
.auth-panel {
  position: relative;
  z-index: 2;
  width: 100%;
  padding: 0 20px;
  box-sizing: border-box;
}
.auth-panel >>> .el-card {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  box-shadow: 0 6px 28px rgba(30, 80, 130, 0.08);
}
</style>
