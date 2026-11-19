<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveClaim, listClaims, rejectClaim, type ClaimAdminRow } from '@/api/claim'

const rows = ref<ClaimAdminRow[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const status = ref(0)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listClaims({ page: page.value, size, status: status.value, keyword: keyword.value || undefined })
    rows.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onTabChange(value: string | number) {
  status.value = Number(value)
  page.value = 1
  load()
}

async function onApprove(row: ClaimAdminRow) {
  let reply: string | undefined
  try {
    const input = await ElMessageBox.prompt('可填写领取说明（≤200 字，可留空）', '确认通过', {
      inputPattern: /^.{0,200}$/,
      inputErrorMessage: '不超过 200 字',
      confirmButtonText: '通过',
    })
    reply = input.value || undefined
  } catch {
    return
  }
  try {
    await approveClaim(row.id, reply)
    ElMessage.success('已通过，物品标记为已完成')
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onReject(row: ClaimAdminRow) {
  let reason: string
  try {
    const input = await ElMessageBox.prompt('请填写拒绝原因（1~200 字，将展示给申请人）', '拒绝申请', {
      inputPattern: /^.{1,200}$/,
      inputErrorMessage: '请输入 1~200 字',
      confirmButtonText: '拒绝',
    })
    reason = input.value
  } catch {
    return
  }
  try {
    await rejectClaim(row.id, reason)
    ElMessage.success('已拒绝')
    await load()
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
          <b>认领核验</b>
          <span class="muted">核验通过后物品标记为已完成，其余待核验申请自动拒绝</span>
        </div>
      </template>

      <div class="filters">
        <el-radio-group :model-value="status" @update:model-value="onTabChange">
          <el-radio-button :value="0">待核验</el-radio-button>
          <el-radio-button :value="1">已通过</el-radio-button>
          <el-radio-button :value="2">已拒绝</el-radio-button>
        </el-radio-group>
        <el-input v-model="keyword" placeholder="物品标题或申请人学号" clearable style="width: 240px"
                  @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="small" class="mt12" empty-text="没有符合条件的申请">
        <el-table-column prop="itemTitle" label="物品" min-width="160" />
        <el-table-column label="申请人" width="160">
          <template #default="{ row }">
            {{ row.applicant?.nickname }}（{{ row.applicant?.studentNo }}）
          </template>
        </el-table-column>
        <el-table-column prop="detailDesc" label="认领说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reply" label="回复" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="primary" size="small" @click="onApprove(row)">通过</el-button>
              <el-button link type="danger" size="small" @click="onReject(row)">拒绝</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>

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
