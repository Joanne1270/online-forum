<template>
  <auth-page :max-width="480">
    <el-card shadow="never">
      <h2 class="page-title">注册</h2>
      <el-form :model="form" :rules="rules" ref="form" label-width="80px" @submit.native.prevent="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号，用于登录" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="100" placeholder="用于找回密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="50" placeholder="社区内显示的名称，不可重复" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="submit">注册</el-button>
        <div class="auth-links">
          <span class="auth-link" @click="$router.push('/login')">已有账号，去登录</span>
        </div>
      </el-form>
    </el-card>
  </auth-page>
</template>

<script>
import { authApi } from '../../api/user'
import AuthPage from '../../components/AuthPage.vue'

const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default {
  components: { AuthPage },
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!value || !phonePattern.test(value.trim())) {
        callback(new Error('请输入11位有效手机号'))
      } else {
        callback()
      }
    }
    const validateEmail = (rule, value, callback) => {
      if (!value || !emailPattern.test(value.trim())) {
        callback(new Error('请输入有效邮箱'))
      } else {
        callback()
      }
    }
    return {
      form: { phone: '', email: '', nickname: '', password: '' },
      rules: {
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        email: [{ validator: validateEmail, trigger: 'blur' }],
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码至少6位', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.doRegister()
      })
    },
    async doRegister() {
      try {
        const res = await authApi.register({
          phone: this.form.phone.trim(),
          email: this.form.email.trim(),
          nickname: this.form.nickname.trim(),
          password: this.form.password
        })
        this.$store.dispatch('login', { token: res.data.token, user: res.data.user })
        this.$message.success('注册成功')
        this.$router.push('/')
      } catch (e) {
        // 错误提示已由 request 拦截器展示
      }
    }
  }
}
</script>

<style scoped>
.page-title {
  text-align: center;
  margin: 0 0 24px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.submit-btn { width: 100%; margin-top: 8px; }
.auth-links {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
.auth-link {
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}
.auth-link:hover { color: #409EFF; }
</style>
