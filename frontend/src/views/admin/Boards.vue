<template>
  <el-card>
    <div slot="header" class="header">
      <span>版块管理</span>
      <el-button type="primary" size="mini" @click="openCategoryDialog()">新增大版块</el-button>
    </div>
    <el-table
      :data="boards"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="类型" width="100">
        <template slot-scope="{ row }">{{ typeLabel(row) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="280">
        <template slot-scope="{ row }">
          <el-button
            v-if="row.boardType === 'CATEGORY'"
            size="mini"
            type="primary"
            plain
            @click="openSubDialog(row)"
          >新增小版块</el-button>
          <el-button v-if="canEdit(row)" size="mini" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="canDelete(row)" size="mini" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :visible.sync="visible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="form.parentName" label="所属大版块">
          <el-input :value="form.parentName" disabled />
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="版块名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import { adminApi } from '../../api/admin'

const SYSTEM_TYPES = ['HOME', 'ALL']

export default {
  data() {
    return {
      boards: [],
      visible: false,
      dialogTitle: '版块',
      form: {
        id: null,
        name: '',
        description: '',
        sortOrder: 0,
        parentId: null,
        parentName: '',
        boardType: 'CATEGORY'
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    typeLabel(row) {
      if (row.boardType === 'HOME') return '首页'
      if (row.boardType === 'ALL') return '全部'
      if (row.boardType === 'CATEGORY') return '大版块'
      if (row.parentId) return '小版块'
      return row.boardType || '-'
    },
    isSystem(row) {
      return SYSTEM_TYPES.includes(row.boardType)
    },
    canEdit(row) {
      return !this.isSystem(row)
    },
    canDelete(row) {
      return !this.isSystem(row)
    },
    async load() {
      const res = await adminApi.boards()
      this.boards = res.data || []
    },
    resetForm() {
      this.form = {
        id: null,
        name: '',
        description: '',
        sortOrder: 0,
        parentId: null,
        parentName: '',
        boardType: 'CATEGORY'
      }
    },
    openCategoryDialog() {
      this.resetForm()
      this.form.boardType = 'CATEGORY'
      this.dialogTitle = '新增大版块'
      this.visible = true
    },
    openSubDialog(parent) {
      this.resetForm()
      this.form.parentId = parent.id
      this.form.parentName = parent.name
      this.form.boardType = 'NORMAL'
      this.dialogTitle = `新增小版块 · ${parent.name}`
      this.visible = true
    },
    openDialog(row) {
      this.form = {
        id: row.id,
        name: row.name,
        description: row.description || '',
        sortOrder: row.sortOrder || 0,
        parentId: row.parentId || null,
        parentName: row.parentId ? this.findParentName(row.parentId) : '',
        boardType: row.boardType
      }
      this.dialogTitle = row.parentId ? '编辑小版块' : '编辑大版块'
      this.visible = true
    },
    findParentName(parentId) {
      const parent = this.boards.find(b => b.id === parentId)
      return parent ? parent.name : ''
    },
    async save() {
      if (!this.form.name.trim()) {
        this.$message.warning('请填写版块名称')
        return
      }
      const payload = {
        name: this.form.name.trim(),
        description: this.form.description,
        sortOrder: this.form.sortOrder,
        parentId: this.form.parentId,
        boardType: this.form.boardType
      }
      if (this.form.id) {
        await adminApi.updateBoard(this.form.id, payload)
      } else {
        await adminApi.createBoard(payload)
      }
      this.visible = false
      this.$message.success('保存成功')
      this.load()
    },
    async remove(row) {
      const label = row.parentId ? '小版块' : '大版块'
      await this.$confirm(`确认删除该${label}「${row.name}」？`)
      try {
        await adminApi.deleteBoard(row.id)
        this.$message.success('删除成功')
        this.load()
      } catch (e) {
        // 错误信息由 request 拦截器展示
      }
    }
  }
}
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
