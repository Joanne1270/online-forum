<template>
  <el-container>
    <el-aside width="200px">
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin/dashboard">概览</el-menu-item>
        <el-menu-item index="/admin/boards">版块管理</el-menu-item>
        <el-menu-item index="/admin/posts">帖子管理</el-menu-item>
        <el-menu-item index="/admin/reports">举报管理</el-menu-item>
        <el-menu-item index="/admin/profile-requests">资料审核</el-menu-item>
        <el-menu-item index="/admin/banned-words">违禁词</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main><router-view /></el-main>
  </el-container>
</template>

<script>
export default { name: 'AdminLayout' }
</script>
