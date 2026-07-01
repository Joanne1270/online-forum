<template>
  <el-card>
    <h2>{{ pageTitle }}</h2>
    <el-form label-width="80px">
      <el-form-item v-if="!isPublishedEdit && allPostable.length > 1" label="版块">
        <el-select v-model="form.boardId" placeholder="选择小版块">
          <el-option-group v-for="cat in groupedBoards" :key="cat.id" :label="cat.name">
            <el-option v-for="c in cat.children" :key="c.id" :label="c.name" :value="c.id" />
          </el-option-group>
          <el-option v-for="b in standaloneBoards" :key="b.id" :label="b.name" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item :label="contentLabel">
        <content-editor ref="editor" v-model="form.content" :rows="8" :placeholder="contentPlaceholder" />
        <div class="content-actions">
          <emoji-picker @select="insertEmoji" />
          <file-upload @insert-media="insertMedia" @insert-attachment="insertAttachment" />
        </div>
      </el-form-item>
      <el-form-item v-if="!isPublishedEdit && !isAdmin" class="submit-row">
        <el-button @click="saveDraft">保存为草稿</el-button>
        <el-button type="primary" @click="submit">{{ isDraft ? '发布' : '提交' }}</el-button>
      </el-form-item>
      <el-form-item v-else-if="!isPublishedEdit">
        <el-button type="primary" @click="submit">提交</el-button>
      </el-form-item>
      <el-form-item v-else>
        <el-button type="primary" @click="submit">提交</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script>
import { mapGetters } from 'vuex'
import { postApi } from '../../api/post'
import ContentEditor from '../../components/ContentEditor.vue'
import FileUpload from '../../components/FileUpload.vue'
import EmojiPicker from '../../components/EmojiPicker.vue'
import { categoryBoards, postableBoards, allowsEmptyPostContent, isHomeSectionBoard, flattenBoards } from '../../utils/board'

export default {
  components: { ContentEditor, FileUpload, EmojiPicker },
  data() {
    return {
      boards: [],
      form: { boardId: null, title: '', content: '' },
      isPublishedEdit: false,
      isDraft: false,
      postId: null
    }
  },
  computed: {
    ...mapGetters(['isAdmin']),
    pageTitle() {
      if (this.isPublishedEdit) {
        return this.editingOfficialAnnouncement ? '编辑公告' : '编辑帖子'
      }
      if (this.isDraft) return '编辑草稿'
      return this.isAdmin ? '发布官方公告' : '发布帖子'
    },
    editingOfficialAnnouncement() {
      if (!this.isPublishedEdit || !this.form.boardId) return false
      const flat = flattenBoards(this.boards)
      const board = flat.find(b => String(b.id) === String(this.form.boardId))
      return this.isAdmin && isHomeSectionBoard(board, flat)
    },
    groupedBoards() {
      return categoryBoards(this.boards, this.isAdmin)
    },
    standaloneBoards() {
      return postableBoards(this.boards, this.isAdmin).filter(b => !b.parentId)
    },
    allPostable() {
      return postableBoards(this.boards, this.isAdmin)
    },
    allowsEmptyContent() {
      const board = this.allPostable.find(b => b.id === this.form.boardId)
      return allowsEmptyPostContent(board)
    },
    contentLabel() {
      return this.allowsEmptyContent ? '正文（可选）' : '内容'
    },
    contentPlaceholder() {
      return this.allowsEmptyContent
        ? '可选：正文、#标签（空格确认）或 @昵称'
        : '支持 #标签（空格确认）、@昵称；图片/视频上传后可直接预览'
    }
  },
  async created() {
    const boardsRes = await postApi.boards()
    this.boards = boardsRes.data || []
    this.isDraft = this.$route.path.includes('/post/draft/')
    this.postId = this.$route.params.id
    this.isPublishedEdit = !!this.postId && !this.isDraft
    if (this.isDraft) {
      const res = await postApi.draftDetail(this.postId)
      this.form.boardId = res.data.boardId
      this.form.title = res.data.title
      this.form.content = res.data.content
    } else if (this.isPublishedEdit) {
      const res = await postApi.detail(this.postId)
      this.form.boardId = res.data.boardId
      this.form.title = res.data.title
      this.form.content = res.data.content
    } else {
      const queryBoard = Number(this.$route.query.boardId)
      if (queryBoard && this.allPostable.some(b => b.id === queryBoard)) {
        this.form.boardId = queryBoard
      } else if (this.allPostable.length) {
        this.form.boardId = this.allPostable[0].id
      }
    }
  },
  methods: {
    insertMedia(payload) {
      this.$refs.editor.addMedia(payload)
    },
    insertAttachment({ url, name }) {
      const label = name || '附件'
      this.form.content += `\n[${label}](${url})\n`
    },
    insertEmoji(emoji) {
      if (this.$refs.editor) this.$refs.editor.insertEmoji(emoji)
    },
    flushEditorContent() {
      if (this.$refs.editor && this.$refs.editor.syncContent) {
        this.$refs.editor.syncContent()
      }
    },
    buildPayload() {
      this.flushEditorContent()
      return {
        boardId: this.form.boardId,
        title: this.form.title,
        content: this.form.content
      }
    },
    async saveDraft() {
      if (!this.form.boardId) {
        this.$message.warning('请选择版块')
        return
      }
      try {
        if (this.isDraft) {
          await postApi.updateDraft(this.postId, this.buildPayload())
          this.$message.success('草稿已保存')
        } else {
          const res = await postApi.createDraft(this.buildPayload())
          this.isDraft = true
          this.postId = String(res.data.id)
          this.$message.success('已保存为草稿')
          await this.$router.replace(`/post/draft/${res.data.id}/edit`)
        }
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || '保存失败')
      }
    },
    async submit() {
      this.flushEditorContent()
      if (!(this.form.title || '').trim()) {
        this.$message.warning('标题不能为空')
        return
      }
      if (!this.allowsEmptyContent && !(this.form.content || '').trim()) {
        this.$message.warning('内容不能为空')
        return
      }
      let confirmSimilar = false
      if (!this.isPublishedEdit && !this.isAdmin) {
        const similarResult = await this.confirmSimilarIfNeeded()
        if (similarResult === 'cancel') return
        if (similarResult === 'confirm') confirmSimilar = true
      }
      try {
        const payload = { ...this.buildPayload(), confirmSimilar }
        if (this.isPublishedEdit) {
          await postApi.update(this.postId, {
            title: this.form.title,
            content: this.form.content
          })
          this.$router.push(`/post/${this.postId}`)
        } else if (this.isDraft) {
          const res = await postApi.publishDraft(this.postId, payload)
          this.$message.success('发布成功')
          this.$router.push(`/post/${res.data.id}`)
        } else {
          const res = await postApi.create(payload)
          this.$router.push(`/post/${res.data.id}`)
        }
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || e.message || '操作失败')
      }
    },
    async confirmSimilarIfNeeded() {
      try {
        const res = await postApi.checkSimilar({
          ...this.buildPayload(),
          excludePostId: this.isDraft ? Number(this.postId) : null
        })
        if (!res.data || !res.data.similar) {
          return 'ok'
        }
        await this.$confirm('您刚刚发布过相似内容，是否继续发布？', '提示', {
          confirmButtonText: '继续发布',
          cancelButtonText: '取消',
          type: 'warning'
        })
        return 'confirm'
      } catch (e) {
        return 'cancel'
      }
    }
  }
}
</script>

<style scoped>
.content-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.content-actions >>> .file-upload {
  margin-top: 0;
}
.submit-row >>> .el-form-item__content {
  display: flex;
  gap: 12px;
}
</style>
