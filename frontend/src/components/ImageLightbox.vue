<template>
  <el-dialog
    :visible.sync="visible"
    width="92%"
    top="4vh"
    append-to-body
    custom-class="image-lightbox-dialog"
    :show-close="true"
    @closed="reset"
  >
    <div
      ref="viewer"
      class="viewer"
      @wheel.prevent="onWheel"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
    >
      <img
        v-if="src"
        ref="img"
        class="viewer-image"
        :src="src"
        :style="imgStyle"
        draggable="false"
        @dragstart.prevent
      >
    </div>
    <div slot="footer" class="toolbar">
      <el-button size="mini" icon="el-icon-zoom-out" @click="zoomOut">缩小</el-button>
      <span class="scale-label">{{ Math.round(scale * 100) }}%</span>
      <el-button size="mini" icon="el-icon-zoom-in" @click="zoomIn">放大</el-button>
      <el-button size="mini" @click="reset">重置</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ImageLightbox',
  data() {
    return {
      visible: false,
      src: '',
      scale: 1,
      translateX: 0,
      translateY: 0,
      dragging: false,
      startX: 0,
      startY: 0,
      originX: 0,
      originY: 0
    }
  },
  computed: {
    imgStyle() {
      return {
        transform: `translate(${this.translateX}px, ${this.translateY}px) scale(${this.scale})`
      }
    }
  },
  methods: {
    open(src) {
      this.src = src
      this.reset()
      this.visible = true
    },
    zoomIn() {
      this.scale = Math.min(5, this.scale + 0.2)
    },
    zoomOut() {
      this.scale = Math.max(0.3, this.scale - 0.2)
      if (this.scale <= 1) {
        this.translateX = 0
        this.translateY = 0
      }
    },
    onWheel(event) {
      if (event.deltaY < 0) {
        this.zoomIn()
      } else {
        this.zoomOut()
      }
    },
    onMouseDown(event) {
      if (this.scale <= 1) return
      this.dragging = true
      this.startX = event.clientX
      this.startY = event.clientY
      this.originX = this.translateX
      this.originY = this.translateY
    },
    onMouseMove(event) {
      if (!this.dragging) return
      this.translateX = this.originX + (event.clientX - this.startX)
      this.translateY = this.originY + (event.clientY - this.startY)
    },
    onMouseUp() {
      this.dragging = false
    },
    reset() {
      this.scale = 1
      this.translateX = 0
      this.translateY = 0
      this.dragging = false
    }
  }
}
</script>

<style scoped>
.viewer {
  height: 70vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1f1f1f;
  border-radius: 4px;
  cursor: grab;
  user-select: none;
}
.viewer-image {
  max-width: 100%;
  max-height: 100%;
  transition: transform 0.05s linear;
  transform-origin: center center;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}
.scale-label {
  min-width: 48px;
  text-align: center;
  color: #606266;
}
</style>

<style>
.image-lightbox-dialog .el-dialog__body {
  padding: 12px 20px;
}
</style>
