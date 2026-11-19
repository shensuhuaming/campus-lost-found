<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMessages, markMessageRead, type MessageRow } from '@/api/message'

const rows = ref<MessageRow[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const filter = ref<number | undefined>(undefined)

async function load() {
  loading.value = true
  try {
    const res = await listMessages({ page: page.value, size, isRead: filter.value })
    rows.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onFilterChange(value: string | number) {
  filter.value = value === '' ? undefined : Number(value)
  page.value = 1
  load()
}

async function onRead(row: MessageRow) {
  if (row.isRead === 1) {
    return
  }
  try {
    await markMessageRead(row.id)
    row.isRead = 1
    ElMessage.success('已标记为已读')
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <b>消息中心</b>
          <span class="muted">审核结果、留言提醒、认领申请与结果通知</span>
        </div>
      </template>

      <el-radio-group :model-value="filter ?? ''" @update:model-value="onFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button :value="0">未读</el-radio-button>
        <el-radio-button :value="1">已读</el-radio-button>
      </el-radio-group>

      <div v-loading="loading" class="message-list mt12">
        <el-empty v-if="!loading && rows.length === 0" description="暂无消息" :image-size="80" />
        <div v-for="m in rows" :key="m.id" class="message-item" :class="{ unread: m.isRead === 0 }"
             @click="onRead(m)">
          <div class="message-head">
            <el-tag size="small" type="info">{{ m.typeText }}</el-tag>
            <span class="muted">{{ m.fromNickname }} · {{ m.createdAt }}</span>
            <el-tag v-if="m.isRead === 0" size="small" type="danger" effect="plain">未读</el-tag>
          </div>
          <div class="message-content">{{ m.content }}</div>
        </div>
      </div>

      <el-pagination
        v-if="total > size"
        class="pager"
        layout="prev, pager, next, total"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="(p: number) => { page = p; load() }"
      />
    </el-card>
  </div>
</template>
