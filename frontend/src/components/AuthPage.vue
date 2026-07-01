<template>
  <div class="auth-page">
    <img class="auth-bg" :src="loginBg" alt="" aria-hidden="true" />
    <div class="auth-panel" :style="panelStyle">
      <slot />
    </div>
  </div>
</template>

<script>
import loginBg from '@/assets/images/login-bg.png'

export default {
  props: {
    maxWidth: {
      type: Number,
      default: 420
    }
  },
  data() {
    return { loginBg }
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
  background: #4a86b8;
}
.auth-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  z-index: 0;
  pointer-events: none;
  user-select: none;
  transform: scale(1.03);
  transform-origin: center top;
}
.auth-panel {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 0 20px;
  box-sizing: border-box;
}
.auth-panel >>> .el-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(30, 80, 130, 0.12);
}
</style>
