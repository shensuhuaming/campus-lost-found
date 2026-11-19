<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { advanceItem, closeItem, getItem, type ItemDetail } from '@/api/item'
import { createClaim } from '@/api/claim'
import { createComment, deleteComment } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const detail = ref<ItemDetail | null>(null)
const loading = ref(false)

const id = computed(() => Number(route.params.id))
const isOwner = computed(() => !!detail.value && auth.user?.id === detail.value.publisher?.id)
const canEdit = computed(() => isOwner.value && detail.value?.itemStatus === 0)
const canAdvance = computed(() => isOwner.value && detail.value?.type === 0
  && detail.value?.auditStatus === 1 && (detail.value?.itemStatus === 0 || detail.value?.itemStatus === 1))
const canClose = computed(() => (isOwner.value || auth.isAdmin) && detail.value != null
  && (detail.value.itemStatus === 0 || detail.value.itemStatus === 1))

async function load() {
  loading.value = true
  try {
    detail.value = await getItem(id.value)
  } catch (e) {
    ElMessage.error((e as Error).message)
    router.replace('/')
  } finally {
    loading.value = false
  }
}

async function onEdit() {
  router.push(`/items/${id.value}/edit`)
}

async function onAdvance(target: number) {
  const tip = target === 1 ? '标记为「已联系到拾到者」？' : '标记为「已找回」？'
  try {
    await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await advanceItem(id.value, target)
    ElMessage.success('状态已更新')
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onClose() {
  let reason: string | undefined
  if (auth.isAdmin) {
    try {
      const input = await ElMessageBox.prompt('管理员下架需填写理由（1~100 字）', '关闭信息', {
        inputPattern: /^.{1,100}$/,
        inputErrorMessage: '请输入 1~100 字理由',
      })
      reason = input.value
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm('关闭后信息不再展示，确定关闭？', '提示', { type: 'warning' })
    } catch {
      return
    }
  }
  try {
    await closeItem(id.value, reason)
    ElMessage.success('信息已关闭')
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

const claimDialog = ref(false)
const claimDesc = ref('')
const claimLoading = ref(false)

function onClaim() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  claimDesc.value = ''
  claimDialog.value = true
}

async function submitClaim() {
  if (claimDesc.value.trim().length === 0) {
    ElMessage.warning('请填写认领说明')
    return
  }
  claimLoading.value = true
  try {
    await createClaim(id.value, claimDesc.value.trim())
    ElMessage.success('认领申请已提交，等待管理员核验')
    claimDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    claimLoading.value = false
  }
}

const newComment = ref('')
const commentLoading = ref(false)

async function submitComment() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  const content = newComment.value.trim()
  if (!content) {
    ElMessage.warning('请输入留言内容')
    return
  }
  commentLoading.value = true
  try {
    await createComment(id.value, content)
    ElMessage.success('留言已发表')
    newComment.value = ''
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    commentLoading.value = false
  }
}

async function onDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定删除这条留言？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteComment(commentId)
    ElMessage.success('留言已删除')
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page">
    <el-card v-if="detail" shadow="never">
      <div class="detail-head">
        <div>
          <h2 class="detail-title">{{ detail.title }}</h2>
          <div class="detail-tags">
            <el-tag :type="detail.type === 1 ? 'success' : 'warning'">{{ detail.typeText }}</el-tag>
            <el-tag type="info">{{ detail.categoryName ?? '未分类' }}</el-tag>
            <el-tag>{{ detail.itemStatusText }}</el-tag>
            <el-tag v-if="detail.auditStatus !== 1" type="danger">{{ detail.auditStatusText }}</el-tag>
          </div>
        </div>
        <div class="detail-actions">
          <el-button v-if="canEdit" size="small" @click="onEdit">编辑</el-button>
          <el-button v-if="canAdvance" size="small" type="primary" @click="onAdvance(detail.itemStatus === 0 ? 1 : 2)">
            {{ detail.itemStatus === 0 ? '已联系到拾到者' : '已找回' }}
          </el-button>
          <el-button v-if="canClose" size="small" type="danger" plain @click="onClose">关闭</el-button>
          <el-button v-if="detail.type === 1 && detail.itemStatus === 0" size="small" type="warning"
                     :disabled="detail.myClaimStatus !== null" @click="onClaim">
            {{ detail.myClaimStatus === null ? '认领' : '已申请' }}
          </el-button>
        </div>
      </div>

      <el-row :gutter="16" class="section">
        <el-col :xs="24" :md="10">
          <div v-if="detail.images.length" class="detail-images">
            <el-image
              v-for="(img, idx) in detail.images"
              :key="img"
              :src="img"
              :preview-src-list="detail.images"
              :initial-index="idx"
              fit="cover"
              class="detail-image"
            />
          </div>
          <el-empty v-else description="暂无图片" :image-size="80" />
        </el-col>
        <el-col :xs="24" :md="14">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="发生时间">{{ detail.happenTime }}</el-descriptions-item>
            <el-descriptions-item label="地点">{{ detail.happenPlace }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">
              <span>{{ detail.contact }}</span>
              <span v-if="!auth.isLoggedIn || (!isOwner && !auth.isAdmin)" class="muted">（登录且非本人时脱敏）</span>
            </el-descriptions-item>
            <el-descriptions-item label="发布者">
              {{ detail.publisher?.nickname }}（{{ detail.publisher?.studentNo }}）
            </el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ detail.createdAt }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.closeReason" label="关闭理由">{{ detail.closeReason }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-desc">{{ detail.description }}</div>
        </el-col>
      </el-row>

      <el-divider content-position="left">留言（{{ detail.comments.length }}）</el-divider>

      <div class="comment-box">
        <el-input v-model="newComment" type="textarea" :rows="2" maxlength="200" show-word-limit
                  :placeholder="auth.isLoggedIn ? '询问物品细节，留言会通知发布者' : '登录后可留言'" />
        <div class="comment-actions">
          <el-button type="primary" size="small" :loading="commentLoading"
                     :disabled="!auth.isLoggedIn || !newComment.trim()" @click="submitComment">发表留言</el-button>
          <span v-if="!auth.isLoggedIn" class="muted">登录后才能留言</span>
        </div>
      </div>

      <el-empty v-if="detail.comments.length === 0" description="还没有留言" :image-size="60" />
      <div v-for="c in detail.comments" :key="c.id" class="comment-item">
        <span class="comment-author">{{ c.nickname ?? '匿名用户' }}</span>
        <span class="muted">{{ c.createdAt }}</span>
        <el-button v-if="auth.isLoggedIn && (auth.user?.id === c.userId || auth.isAdmin)"
                   link type="danger" size="small" @click="onDeleteComment(c.id)">删除</el-button>
        <div>{{ c.content }}</div>
      </div>
    </el-card>

    <el-dialog v-model="claimDialog" title="发起认领" width="520px">
      <el-alert type="info" :closable="false" show-icon
                title="请描述只有失主才知道的细节（颜色、品牌、内含物等），管理员据此核验身份" />
      <el-input v-model="claimDesc" type="textarea" :rows="4" maxlength="300" show-word-limit
                class="mt12" placeholder="例如：内有一卡通，姓名与我一致，可提供学号核验" />
      <template #footer>
        <el-button @click="claimDialog = false">取消</el-button>
        <el-button type="primary" :loading="claimLoading" @click="submitClaim">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>
