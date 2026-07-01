<template>
  <div class="file-upload">
    <el-upload
      action="#"
      :http-request="uploadMedia"
      :show-file-list="false"
      accept="image/*,video/*"
    >
      <el-button size="mini" type="primary" plain>上传图片/视频</el-button>
    </el-upload>
    <el-upload
      action="#"
      :http-request="uploadAttachment"
      :show-file-list="false"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.txt,.csv,.json"
    >
      <el-button size="mini" plain>上传其他文件</el-button>
    </el-upload>
  </div>
</template>

<script>
import { fileApi } from '../api/file'

export default {
  methods: {
    async uploadMedia({ file }) {
      const res = await fileApi.upload(file, 'media')
      this.$emit('insert-media', {
        url: res.data.url,
        mime: res.data.mime,
        name: res.data.originalName
      })
      this.$message.success('上传成功')
    },
    async uploadAttachment({ file }) {
      const res = await fileApi.upload(file, 'attachment')
      this.$emit('insert-attachment', {
        url: res.data.url,
        name: res.data.originalName || file.name
      })
      this.$message.success('上传成功')
    }
  }
}
</script>

<style scoped>
.file-upload {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
