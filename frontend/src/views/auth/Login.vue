<template>
  <auth-page>
    <el-card shadow="never">
      <h2 class="page-title">欢迎来到论坛！</h2>
      <el-form :model="form" :rules="rules" ref="form" @submit.native.prevent="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="submit">登录</el-button>
        <div class="auth-links">
          <span class="auth-link" @click="$router.push('/register')">注册账号</span>
          <span class="auth-divider">|</span>
          <span class="auth-link" @click="$router.push('/forgot-password')">忘记密码？</span>
        </div>
      </el-form>
    </el-card>
  </auth-page>
</template>

<script>
import { authApi } from '../../api/user'
import AuthPage from '../../components/AuthPage.vue'

const phonePattern = /^1[3-9]\d{9}$/

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
    return {
      form: { phone: '', password: '' },
      rules: {
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        try {
          const res = await authApi.login({
            phone: this.form.phone.trim(),
            password: this.form.password
          })
          this.$store.dispatch('login', { token: res.data.token, user: res.data.user })
          this.$message.success('登录成功')
          this.$router.push('/')
        } catch (e) {
          const err = e && e.code != null ? e : null
          if (err && err.code === 403) {
            this.$alert(err.message.replace(/\n/g, '<br/>'), '账号已封禁', {
              type: 'warning',
              confirmButtonText: '我知道了',
              dangerouslyUseHTMLString: true
            })
          } else if (!err) {
            this.$message.error('登录失败，请稍后重试')
          }
        }
      })
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
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}
.auth-link {
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}
.auth-link:hover { color: #409EFF; }
.auth-divider {
  color: #dcdfe6;
  font-size: 14px;
  line-height: 1;
}
</style>
