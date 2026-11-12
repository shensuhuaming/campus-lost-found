<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth, type HealthInfo } from '@/api/system'

type ProbeState = 'loading' | 'ok' | 'fail'

const backend = ref<ProbeState>('loading')
const backendInfo = ref<HealthInfo | null>(null)
const backendError = ref('')
const lastChecked = ref('')

async function probeBackend() {
  backend.value = 'loading'
  backendError.value = ''
  try {
    const res = await getHealth()
    if (res.code === 0) {
      backend.value = 'ok'
      backendInfo.value = res.data
    } else {
      backend.value = 'fail'
      backendError.value = res.message
    }
  } catch (e) {
    backend.value = 'fail'
    backendError.value = e instanceof Error ? e.message : String(e)
  } finally {
    lastChecked.value = new Date().toLocaleTimeString()
  }
}

onMounted(probeBackend)

const modules = [
  { name: '用户模块', desc: '注册 / 登录退出 / 个人中心 / 账号管理', frs: 'FR-01 ~ FR-04' },
  { name: '失物信息模块', desc: '发布（含图片）/ 编辑 / 下架 / 审核', frs: 'FR-05 ~ FR-07' },
  { name: '浏览检索模块', desc: '列表 / 分类类型筛选 / 关键词 / 详情', frs: 'FR-08 ~ FR-11' },
  { name: '认领归还模块', desc: '认领申请 / 管理员核验 / 状态流转 / 关闭', frs: 'FR-12 ~ FR-15' },
  { name: '留言消息模块', desc: '物品留言 / 站内消息与未读数', frs: 'FR-16 ~ FR-17' },
  { name: '管理后台模块', desc: '信息审核 / 分类管理 / 数据统计', frs: 'FR-18 ~ FR-20' },
]

const environments = [
  {
    name: '测试 / 演示（Docker 全栈）',
    cmd: 'docker compose --profile full up -d --build',
    access: 'http://localhost',
    note: '前端 nginx → 后端 8080 → MySQL 3307',
  },
  {
    name: '后端热开发（IDEA + 本机 Maven）',
    cmd: 'docker compose up -d mysql  然后运行 LostFoundApplication',
    access: 'http://localhost:8080/api/system/health',
    note: '只起数据库，后端进程在 IDEA 里调试',
  },
  {
    name: '前端热开发（VS Code）',
    cmd: 'cd frontend && npm install && npm run dev',
    access: 'http://localhost:5173',
    note: '/api 已代理到 localhost:8080',
  },
]
</script>

<template>
  <div class="page">
    <header class="hero">
      <div>
        <h1>校园失物招领系统</h1>
        <p class="subtitle">Campus Lost &amp; Found · 环境骨架 v0.1.0</p>
      </div>
      <el-tag type="warning" effect="dark" size="large">业务功能待设计文档确认</el-tag>
    </header>

    <el-alert
      title="当前仓库只包含开发环境基座与接口骨架"
      type="info"
      :closable="false"
      show-icon
      description="前端首页 + 健康检查 + 数据库建表基线 + Docker 编排已可用；用户、物品、认领等业务模块将在《详细设计 / 接口文档》评审后按契约实现。"
    />

    <el-row :gutter="16" class="section">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><b>后端连通性</b></template>
          <div class="probe">
            <el-tag v-if="backend === 'loading'" type="info">检测中…</el-tag>
            <el-tag v-else-if="backend === 'ok'" type="success">UP</el-tag>
            <el-tag v-else type="danger">不可用</el-tag>
            <el-button size="small" :loading="backend === 'loading'" @click="probeBackend">重新检测</el-button>
          </div>
          <el-descriptions v-if="backendInfo" :column="1" size="small" border class="mt12">
            <el-descriptions-item label="服务">{{ backendInfo.app }}</el-descriptions-item>
            <el-descriptions-item label="JDK">{{ backendInfo.javaVersion }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ backendInfo.timestamp }}</el-descriptions-item>
          </el-descriptions>
          <p v-if="backend === 'fail'" class="error">{{ backendError }}</p>
          <p class="muted">最近检测：{{ lastChecked || '—' }}</p>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><b>环境速查</b></template>
          <el-table :data="environments" size="small" :show-header="false">
            <el-table-column prop="name" label="环境" width="180" />
            <el-table-column>
              <template #default="{ row }">
                <div class="mono">{{ row.cmd }}</div>
                <div class="muted">{{ row.access }} · {{ row.note }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section">
      <template #header><b>功能模块（对应需求 FR-01 ~ FR-20）</b></template>
      <el-row :gutter="12">
        <el-col v-for="m in modules" :key="m.name" :xs="24" :sm="12" :lg="8">
          <div class="module-card">
            <div class="module-head">
              <span class="module-name">{{ m.name }}</span>
              <el-tag size="small" type="info">{{ m.frs }}</el-tag>
            </div>
            <div class="module-desc">{{ m.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <footer class="footer">
      软件工程综合实践 · 7 人小组 ·
      <span class="mono">需求文档/需求规格说明书.md</span> ·
      <span class="mono">项目文档/需求理解与工程方案.md</span>
    </footer>
  </div>
</template>
