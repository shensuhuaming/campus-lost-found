<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories, listItems, type Category, type ItemCard } from '@/api/item'

const router = useRouter()

const categories = ref<Category[]>([])
const items = ref<ItemCard[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive({ type: undefined as number | undefined, categoryId: undefined as number | undefined, keyword: '' })
const page = ref(1)
const size = 12

async function load() {
  loading.value = true
  try {
    const res = await listItems({
      page: page.value,
      size,
      type: query.type,
      categoryId: query.categoryId,
      keyword: query.keyword || undefined,
    })
    items.value = res.records
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onReset() {
  query.type = undefined
  query.categoryId = undefined
  query.keyword = ''
  page.value = 1
  load()
}

function onPageChange(p: number) {
  page.value = p
  load()
}

watch(() => query.type, onSearch)
watch(() => query.categoryId, onSearch)

onMounted(async () => {
  try {
    categories.value = await listCategories()
  } catch {
    categories.value = []
  }
  await load()
})
</script>

<template>
  <div class="page">
    <div class="home-head">
      <div>
        <h2 class="home-title">校园失物招领</h2>
        <p class="muted">发布寻物启事与拾物招领，认领需管理员核验</p>
      </div>
      <el-button type="primary" @click="router.push('/items/new')">发布信息</el-button>
    </div>

    <el-card shadow="never" class="section">
      <div class="filters">
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 140px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-radio-group v-model="query.type">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">寻物</el-radio-button>
          <el-radio-button :value="1">拾物</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="query.keyword"
          placeholder="搜索标题或描述"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </el-card>

    <div v-loading="loading" class="item-grid">
      <el-empty v-if="!loading && items.length === 0" description="暂时没有符合条件的信息" />
      <el-row v-else :gutter="16">
        <el-col v-for="item in items" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
          <el-card class="item-card" shadow="hover" @click="router.push(`/items/${item.id}`)">
            <div class="item-cover">
              <el-image v-if="item.coverImage" :src="item.coverImage" fit="cover" class="cover-img" />
              <div v-else class="cover-placeholder">暂无图片</div>
            </div>
            <div class="item-body">
              <div class="item-title">{{ item.title }}</div>
              <div class="item-tags">
                <el-tag size="small" :type="item.type === 1 ? 'success' : 'warning'">{{ item.typeText }}</el-tag>
                <el-tag size="small" type="info">{{ item.categoryName ?? '未分类' }}</el-tag>
                <el-tag size="small">{{ item.itemStatusText }}</el-tag>
              </div>
              <div class="muted">{{ item.happenPlace }} · {{ item.createdAt }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-pagination
      v-if="total > size"
      class="pager"
      layout="prev, pager, next, total"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />
  </div>
</template>
