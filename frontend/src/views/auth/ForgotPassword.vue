<template>
  <auth-page :max-width="480">
    <el-card shadow="never">
      <h2 class="page-title">忘记密码</h2>
      <el-form :model="form" :rules="rules" ref="form" label-width="90px" @submit.native.prevent="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="注册时使用的手机号" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" maxlength="6" placeholder="6位验证码" />
            <el-button :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
            </el-button>
          </div>
          <div class="hint">验证码将发送至您注册时填写的邮箱</div>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="submit">重置密码</el-button>
        <div class="auth-links">
          <span class="auth-link" @click="$router.push('/login')">返回登录</span>
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
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      form: { phone: '', code: '', password: '', confirmPassword: '' },
      countdown: 0,
      timer: null,
      rules: {
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码至少6位', trigger: 'blur' }
        ],
        confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
      }
    }
  },
  beforeDestroy() {
    clearInterval(this.timer)
  },
  methods: {
    async sendCode() {
      const phone = (this.form.phone || '').trim()
      if (!phonePattern.test(phone)) {
        this.$message.warning('请先输入有效手机号')
        return
      }
      await authApi.sendResetCode({ phone })
      this.$message.success('验证码已发送至您的绑定邮箱')
      this.countdown = 60
      this.timer = setInterval(() => {
        this.countdown -= 1
        if (this.countdown <= 0) {
          clearInterval(this.timer)
        }
      }, 1000)
    },
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        await authApi.resetPassword({
          phone: this.form.phone.trim(),
          code: this.form.code.trim(),
          newPassword: this.form.password
        })
        this.$message.success('密码已重置，请登录')
        this.$router.push('/login')
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
.code-row { display: flex; gap: 8px; }
.code-row .el-input { flex: 1; }
.hint { color: #909399; font-size: 12px; margin-top: 6px; }
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
