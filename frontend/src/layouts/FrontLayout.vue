<script setup lang="ts">
import { useRouter, RouterLink, RouterView } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout as logoutApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function onLogout() {
  try {
    await logoutApi()
  } catch {
    // 服务端只记日志，失败也不阻塞本地退出
  }
  auth.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<template>
  <div class="layout">
    <header class="layout-header">
      <RouterLink to="/" class="brand">校园失物招领</RouterLink>
      <nav class="nav">
        <RouterLink to="/" class="nav-link">首页</RouterLink>
        <RouterLink v-if="auth.isLoggedIn" to="/items/new" class="nav-link">发布信息</RouterLink>
        <RouterLink v-if="auth.isLoggedIn" to="/profile" class="nav-link">个人中心</RouterLink>
      </nav>
      <div class="layout-right">
        <template v-if="auth.isLoggedIn">
          <span class="nickname">{{ auth.user?.nickname }}</span>
          <el-tag v-if="auth.isAdmin" size="small" type="warning">管理员</el-tag>
          <el-button size="small" @click="onLogout">退出</el-button>
        </template>
        <el-button v-else type="primary" size="small" @click="router.push('/login')">登录 / 注册</el-button>
      </div>
    </header>

    <main class="layout-main">
      <RouterView />
    </main>

    <footer class="footer">软件工程综合实践 · 7 人小组</footer>
  </div>
</template>
