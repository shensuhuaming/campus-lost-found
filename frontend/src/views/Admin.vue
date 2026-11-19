<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  adminCategories,
  adminStats,
  adminUsers,
  auditItem,
  auditPage,
  createCategory,
  deleteCategory,
  updateCategory,
  updateUserStatus,
  type AdminUser,
  type AuditItem,
  type CategoryAdmin,
  type Stats,
} from '@/api/admin'

echarts.use([LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const route = useRoute()
const tab = computed(() => String(route.name ?? 'adminStats'))

// ---------------- U6.1 审核 ----------------
const auditRows = ref<AuditItem[]>([])
const auditTotal = ref(0)
const auditPageNo = ref(1)
const auditSize = 10
const auditFilter = reactive({ auditStatus: 0 as number | undefined, keyword: '' })
const auditLoading = ref(false)

async function loadAudit() {
  auditLoading.value = true
  try {
    const res = await auditPage({
      page: auditPageNo.value,
      size: auditSize,
      auditStatus: auditFilter.auditStatus,
      keyword: auditFilter.keyword || undefined,
    })
    auditRows.value = res.records
    auditTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    auditLoading.value = false
  }
}

async function onAudit(row: AuditItem, pass: boolean) {
  let reason: string | undefined
  if (!pass) {
    try {
      const input = await ElMessageBox.prompt('请填写驳回原因（1~100 字，将通知发布者）', '驳回信息', {
        inputPattern: /^.{1,100}$/,
        inputErrorMessage: '请输入 1~100 字',
      })
      reason = input.value
    } catch {
      return
    }
  }
  try {
    await auditItem(row.id, pass ? 1 : 2, reason)
    ElMessage.success(pass ? '已通过，信息将展示在前台' : '已驳回，已通知发布者')
    await loadAudit()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

// ---------------- U6.2 分类 ----------------
const categories = ref<CategoryAdmin[]>([])
const categoryLoading = ref(false)
const categoryDialog = ref(false)
const categoryForm = reactive({ id: 0, name: '', sort: 0 })

async function loadCategories() {
  categoryLoading.value = true
  try {
    categories.value = await adminCategories()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    categoryLoading.value = false
  }
}

function openCreateCategory() {
  categoryForm.id = 0
  categoryForm.name = ''
  categoryForm.sort = categories.value.length
  categoryDialog.value = true
}

function openEditCategory(row: CategoryAdmin) {
  categoryForm.id = row.id
  categoryForm.name = row.name
  categoryForm.sort = row.sort
  categoryDialog.value = true
}

async function submitCategory() {
  const name = categoryForm.name.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    if (categoryForm.id) {
      await updateCategory(categoryForm.id, name, categoryForm.sort)
      ElMessage.success('已保存')
    } else {
      await createCategory(name, categoryForm.sort)
      ElMessage.success('已新增')
    }
    categoryDialog.value = false
    await loadCategories()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onDeleteCategory(row: CategoryAdmin) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    await loadCategories()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

// ---------------- U6.3 用户 ----------------
const users = ref<AdminUser[]>([])
const userTotal = ref(0)
const userPage = ref(1)
const userSize = 10
const userFilter = reactive({ keyword: '', role: undefined as number | undefined, status: undefined as number | undefined })
const userLoading = ref(false)

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await adminUsers({
      page: userPage.value,
      size: userSize,
      keyword: userFilter.keyword || undefined,
      role: userFilter.role,
      status: userFilter.status,
    })
    users.value = res.records
    userTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    userLoading.value = false
  }
}

async function onToggleUser(row: AdminUser) {
  const target = row.status === 0 ? 1 : 0
  const action = target === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`${action}用户「${row.nickname}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id, target)
    ElMessage.success(`已${action}`)
    await loadUsers()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

// ---------------- U6.4 统计 ----------------
const stats = ref<Stats | null>(null)
const statsLoading = ref(false)
const pieRef = ref<HTMLDivElement>()
const lineRef = ref<HTMLDivElement>()

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await adminStats(30)
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    statsLoading.value = false
  }
}

function renderCharts() {
  if (!stats.value) {
    return
  }
  if (pieRef.value) {
    const pie = echarts.init(pieRef.value)
    pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '68%'],
        data: stats.value.categoryStats.map((c) => ({ name: c.categoryName, value: c.itemCount })),
      }],
    })
  }
  if (lineRef.value) {
    const line = echarts.init(lineRef.value)
    line.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 24, bottom: 32 },
      xAxis: { type: 'category', data: stats.value.trend.map((t) => t.date.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: stats.value.trend.map((t) => t.count),
      }],
    })
  }
}

// ---------------- 切换加载 ----------------
watch(tab, (value) => {
  if (value === 'adminAudit') loadAudit()
  if (value === 'adminCategories') loadCategories()
  if (value === 'adminUsers') loadUsers()
  if (value === 'adminStats') loadStats()
}, { immediate: true })

onMounted(() => {
  if (tab.value === 'adminStats') loadStats()
})
</script>

<template>
  <div class="page">
    <el-tabs :model-value="tab" class="admin-tabs" @update:model-value="(v: any) => $router.push({ name: v })">
      <el-tab-pane label="信息审核" name="adminAudit">
        <el-card shadow="never">
          <div class="filters">
            <el-radio-group v-model="auditFilter.auditStatus" @change="auditPageNo = 1; loadAudit()">
              <el-radio-button :value="0">待审核</el-radio-button>
              <el-radio-button :value="1">已通过</el-radio-button>
              <el-radio-button :value="2">已驳回</el-radio-button>
            </el-radio-group>
            <el-input v-model="auditFilter.keyword" placeholder="标题或发布者昵称" clearable style="width: 220px"
                      @keyup.enter="loadAudit" />
            <el-button type="primary" @click="loadAudit">查询</el-button>
          </div>

          <el-table v-loading="auditLoading" :data="auditRows" size="small" class="mt12" empty-text="没有待处理的信息">
            <el-table-column label="图片" width="90">
              <template #default="{ row }">
                <el-image v-if="row.images.length" :src="row.images[0]" fit="cover" class="audit-thumb"
                          :preview-src-list="row.images" preview-teleported />
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="typeText" label="类型" width="70" />
            <el-table-column prop="categoryName" label="分类" width="90" />
            <el-table-column prop="publisherNickname" label="发布者" width="100" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="发布时间" width="165" />
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <template v-if="row.auditStatus === 0">
                  <el-button link type="primary" size="small" @click="onAudit(row, true)">通过</el-button>
                  <el-button link type="danger" size="small" @click="onAudit(row, false)">驳回</el-button>
                </template>
                <span v-else class="muted">{{ row.auditStatusText }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination v-if="auditTotal > auditSize" class="pager" layout="prev, pager, next, total"
                         :total="auditTotal" :page-size="auditSize" :current-page="auditPageNo"
                         @current-change="(p: number) => { auditPageNo = p; loadAudit() }" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="分类管理" name="adminCategories">
        <el-card shadow="never">
          <div class="filters">
            <el-button type="primary" @click="openCreateCategory">新增分类</el-button>
            <span class="muted">被信息引用的分类不能删除</span>
          </div>
          <el-table v-loading="categoryLoading" :data="categories" size="small" class="mt12">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="sort" label="排序" width="90" />
            <el-table-column prop="itemCount" label="信息数" width="100" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEditCategory(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="onDeleteCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="adminUsers">
        <el-card shadow="never">
          <div class="filters">
            <el-input v-model="userFilter.keyword" placeholder="学号或昵称" clearable style="width: 200px"
                      @keyup.enter="userPage = 1; loadUsers()" />
            <el-select v-model="userFilter.role" placeholder="全部角色" clearable style="width: 130px">
              <el-option label="学生" :value="0" />
              <el-option label="管理员" :value="1" />
            </el-select>
            <el-select v-model="userFilter.status" placeholder="全部状态" clearable style="width: 130px">
              <el-option label="正常" :value="0" />
              <el-option label="禁用" :value="1" />
            </el-select>
            <el-button type="primary" @click="userPage = 1; loadUsers()">查询</el-button>
          </div>

          <el-table v-loading="userLoading" :data="users" size="small" class="mt12">
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="roleText" label="角色" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="itemCount" label="发布数" width="90" />
            <el-table-column prop="claimCount" label="认领数" width="90" />
            <el-table-column prop="createdAt" label="注册时间" width="165" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link :type="row.status === 0 ? 'danger' : 'success'" size="small"
                           :disabled="row.role === 1" @click="onToggleUser(row)">
                  {{ row.status === 0 ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination v-if="userTotal > userSize" class="pager" layout="prev, pager, next, total"
                         :total="userTotal" :page-size="userSize" :current-page="userPage"
                         @current-change="(p: number) => { userPage = p; loadUsers() }" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="数据统计" name="adminStats">
        <div v-loading="statsLoading">
          <el-row :gutter="16" class="stat-row">
            <el-col :xs="12" :md="6">
              <el-card shadow="never"><div class="stat-label">信息总数</div><div class="stat-value">{{ stats?.totalItems ?? 0 }}</div></el-card>
            </el-col>
            <el-col :xs="12" :md="6">
              <el-card shadow="never"><div class="stat-label">待审核</div><div class="stat-value">{{ stats?.pendingAudit ?? 0 }}</div></el-card>
            </el-col>
            <el-col :xs="12" :md="6">
              <el-card shadow="never"><div class="stat-label">已完成认领</div><div class="stat-value">{{ stats?.completedClaims ?? 0 }}</div></el-card>
            </el-col>
            <el-col :xs="12" :md="6">
              <el-card shadow="never"><div class="stat-label">归还率</div><div class="stat-value">{{ stats?.returnRate ?? 0 }}%</div></el-card>
            </el-col>
          </el-row>

          <el-row :gutter="16" class="mt12">
            <el-col :xs="24" :md="10">
              <el-card shadow="never">
                <template #header><b>分类分布</b></template>
                <div ref="pieRef" class="chart"></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :md="14">
              <el-card shadow="never">
                <template #header><b>近 30 天发布趋势</b></template>
                <div ref="lineRef" class="chart"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="categoryDialog" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="categoryForm.name" maxlength="20" show-word-limit /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sort" :min="0" :max="999" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
