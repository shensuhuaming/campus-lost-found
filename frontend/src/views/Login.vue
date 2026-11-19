<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const tab = ref<'login' | 'register'>('login')
const loading = ref(false)

const loginForm = reactive({ studentNo: '', password: '' })
const registerForm = reactive({
  studentNo: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

async function onLogin() {
  if (!loginForm.studentNo || !loginForm.password) {
    ElMessage.warning('请输入学号与密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ ...loginForm })
    auth.setAuth(res.token, res.user)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  if (!registerForm.studentNo || !registerForm.nickname || !registerForm.phone
      || !registerForm.password || !registerForm.confirmPassword) {
    ElMessage.warning('请填写完整的注册信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await register({ ...registerForm })
    ElMessage.success('注册成功，请使用学号登录')
    loginForm.studentNo = registerForm.studentNo
    loginForm.password = ''
    tab.value = 'login'
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="always">
      <div class="auth-brand">
        <h1>校园失物招领</h1>
        <p class="subtitle">Campus Lost &amp; Found</p>
      </div>

      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" @submit.prevent="onLogin">
            <el-form-item label="学号">
              <el-input v-model="loginForm.studentNo" placeholder="请输入学号" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="onLogin" />
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="onLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" @submit.prevent="onRegister">
            <el-form-item label="学号">
              <el-input v-model="registerForm.studentNo" placeholder="6~12 位数字" clearable />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" placeholder="2~12 个字符" clearable />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="registerForm.phone" placeholder="手机号 / QQ / 微信" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="6~20 位，含字母与数字" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="再次输入密码" />
            </el-form-item>
            <el-button type="primary" class="auth-submit" :loading="loading" @click="onRegister">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-alert
        class="auth-tip"
        type="info"
        :closable="false"
        show-icon
        description="注册后需管理员审核发布的信息；管理员账号由系统初始化创建。"
      />
    </el-card>
  </div>
</template>
