<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changePassword,
  getProfile,
  myClaims,
  myComments,
  myItems,
  updateProfile,
  type ClaimRow,
  type CommentRow,
  type ItemRow,
  type PageResult,
  type Profile,
} from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const activeTab = ref('items')
const loading = ref(false)

const profile = ref<Profile | null>(null)

const emptyPage = <T,>(): PageResult<T> => ({ total: 0, page: 1, size: 12, pages: 0, records: [] })

const items = ref<PageResult<ItemRow>>(emptyPage<ItemRow>())
const claims = ref<PageResult<ClaimRow>>(emptyPage<ClaimRow>())
const comments = ref<PageResult<CommentRow>>(emptyPage<CommentRow>())

const profileForm = reactive({ nickname: '', phone: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadProfile() {
  const data = await getProfile()
  profile.value = data
  profileForm.nickname = data.nickname
  profileForm.phone = data.phone ?? ''
}

async function loadItems(page = 1) {
  loading.value = true
  try {
    items.value = await myItems({ page, size: 10 })
  } finally {
    loading.value = false
  }
}

async function loadClaims(page = 1) {
  loading.value = true
  try {
    claims.value = await myClaims({ page, size: 10 })
  } finally {
    loading.value = false
  }
}

async function loadComments(page = 1) {
  loading.value = true
  try {
    comments.value = await myComments({ page, size: 10 })
  } finally {
    loading.value = false
  }
}

async function onTabChange(name: string | number) {
  if (name === 'items') await loadItems()
  if (name === 'claims') await loadClaims()
  if (name === 'comments') await loadComments()
}

async function onSaveProfile() {
  if (!profileForm.nickname || !profileForm.phone) {
    ElMessage.warning('昵称与联系方式不能为空')
    return
  }
  loading.value = true
  try {
    const user = await updateProfile({ ...profileForm })
    auth.setUser(user)
    await loadProfile()
    ElMessage.success('资料已更新')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

async function onChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  try {
    await ElMessageBox.confirm('修改密码后需要重新登录，是否继续？', '提示', { type: 'warning' })
  } catch {
    return
  }
  loading.value = true
  try {
    await changePassword({ ...passwordForm })
    ElMessage.success('密码已修改，请重新登录')
    auth.logout()
    await router.replace('/login')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadProfile()
    await loadItems()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="section">
      <template #header>
        <div class="card-head">
          <b>个人中心</b>
          <span v-if="profile" class="muted">
            学号 {{ profile.studentNo }} · 注册于 {{ profile.createdAt }}
          </span>
        </div>
      </template>

      <el-row v-if="profile" :gutter="12" class="stats">
        <el-col :xs="12" :sm="6"><div class="stat"><div class="stat-num">{{ profile.stat.published }}</div><div class="muted">我的发布</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="stat"><div class="stat-num">{{ profile.stat.claiming }}</div><div class="muted">我的认领</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="stat"><div class="stat-num">{{ profile.stat.claimingPending }}</div><div class="muted">待核验</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="stat"><div class="stat-num">{{ profile.stat.comments }}</div><div class="muted">我的留言</div></div></el-col>
      </el-row>

      <el-tabs v-model="activeTab" class="mt12" @tab-change="onTabChange">
        <el-tab-pane label="我的发布" name="items">
          <el-table v-loading="loading" :data="items.records" size="small" empty-text="还没有发布过信息">
            <el-table-column label="封面" width="90">
              <template #default="{ row }">
                <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="cover" />
                <span v-else class="muted">无图</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="categoryName" label="分类" width="90" />
            <el-table-column label="审核" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 2 ? 'danger' : 'info'">
                  {{ row.auditStatusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="itemStatusText" label="状态" width="90" />
            <el-table-column prop="claimCount" label="认领数" width="80" />
            <el-table-column prop="createdAt" label="发布时间" width="170" />
          </el-table>
          <el-pagination
            v-if="items.total > items.size"
            class="pager"
            layout="prev, pager, next, total"
            :total="items.total"
            :page-size="items.size"
            :current-page="items.page"
            @current-change="loadItems"
          />
        </el-tab-pane>

        <el-tab-pane label="我的认领" name="claims">
          <el-table v-loading="loading" :data="claims.records" size="small" empty-text="还没有发起过认领">
            <el-table-column prop="itemTitle" label="物品" min-width="160" />
            <el-table-column prop="detailDesc" label="认领说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">
                  {{ row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reply" label="管理员回复" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="申请时间" width="170" />
          </el-table>
          <el-pagination
            v-if="claims.total > claims.size"
            class="pager"
            layout="prev, pager, next, total"
            :total="claims.total"
            :page-size="claims.size"
            :current-page="claims.page"
            @current-change="loadClaims"
          />
        </el-tab-pane>

        <el-tab-pane label="我的留言" name="comments">
          <el-table v-loading="loading" :data="comments.records" size="small" empty-text="还没有发表过留言">
            <el-table-column prop="itemTitle" label="物品" min-width="160" />
            <el-table-column prop="content" label="留言内容" min-width="260" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="留言时间" width="170" />
          </el-table>
          <el-pagination
            v-if="comments.total > comments.size"
            class="pager"
            layout="prev, pager, next, total"
            :total="comments.total"
            :page-size="comments.size"
            :current-page="comments.page"
            @current-change="loadComments"
          />
        </el-tab-pane>

        <el-tab-pane label="修改资料" name="profile">
          <el-form :model="profileForm" label-width="90px" class="narrow-form">
            <el-form-item label="学号">
              <el-input :model-value="profile?.studentNo" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" maxlength="12" show-word-limit />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="profileForm.phone" maxlength="20" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="onSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" label-width="90px" class="narrow-form">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6~20 位，含字母与数字" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="onChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
