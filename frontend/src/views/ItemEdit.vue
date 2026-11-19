<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import {
  createItem,
  getItem,
  listCategories,
  updateItem,
  uploadImage,
  type Category,
  type ItemSavePayload,
} from '@/api/item'

const route = useRoute()
const router = useRouter()

const editId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => editId.value != null)

const categories = ref<Category[]>([])
const imageUrls = ref<string[]>([])
const uploading = ref(false)
const submitting = ref(false)

const form = reactive({
  type: 1,
  title: '',
  description: '',
  categoryId: undefined as number | undefined,
  happenTime: '',
  happenPlace: '',
  contact: '',
})

async function onUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const url = await uploadImage(options.file)
    imageUrls.value.push(url)
    ElMessage.success('图片已上传')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    uploading.value = false
  }
}

function removeImage(url: string) {
  imageUrls.value = imageUrls.value.filter((u) => u !== url)
}

function validate(): string | null {
  if (!form.type && form.type !== 0) return '请选择类型'
  if (!form.title.trim()) return '请填写标题'
  if (form.title.length > 30) return '标题不超过 30 字'
  if (form.description.trim().length < 10) return '描述至少 10 字'
  if (!form.categoryId) return '请选择分类'
  if (!form.happenTime) return '请选择发生时间'
  if (!form.happenPlace.trim()) return '请填写地点'
  if (!form.contact.trim()) return '请填写联系方式'
  return null
}

async function onSubmit() {
  const error = validate()
  if (error) {
    ElMessage.warning(error)
    return
  }
  submitting.value = true
  try {
    const payload: ItemSavePayload = { ...form, categoryId: form.categoryId!, imageUrls: imageUrls.value }
    if (isEdit.value && editId.value != null) {
      await updateItem(editId.value, payload)
      ElMessage.success('已保存，信息将重新进入审核')
      router.push(`/items/${editId.value}`)
    } else {
      const res = await createItem(payload)
      ElMessage.success('发布成功，等待管理员审核')
      router.push(`/items/${res.id}`)
    }
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
  if (isEdit.value && editId.value != null) {
    const detail = await getItem(editId.value)
    form.type = detail.type
    form.title = detail.title
    form.description = detail.description
    form.categoryId = detail.categoryId
    form.happenTime = detail.happenTime
    form.happenPlace = detail.happenPlace
    form.contact = detail.contact
    imageUrls.value = [...detail.images]
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><b>{{ isEdit ? '编辑信息' : '发布信息' }}</b></template>
      <el-form :model="form" label-width="90px" class="narrow-form-wide">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">寻物</el-radio>
            <el-radio :value="1">拾物</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="30" show-word-limit placeholder="不超过 30 字" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit
                    placeholder="10~500 字，描述物品特征与拾获/丢失经过" />
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker v-model="form.happenTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                          placeholder="选择时间" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.happenPlace" maxlength="100" placeholder="丢失/拾取地点" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" maxlength="50" placeholder="手机号 / QQ / 微信" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-area">
            <div v-for="url in imageUrls" :key="url" class="upload-item">
              <el-image :src="url" fit="cover" class="upload-thumb" />
              <el-button link type="danger" size="small" @click="removeImage(url)">删除</el-button>
            </div>
            <el-upload
              v-if="imageUrls.length < 3"
              :show-file-list="false"
              :http-request="onUpload"
              accept="image/jpeg,image/png"
              :disabled="uploading"
            >
              <el-button :loading="uploading">上传图片（{{ imageUrls.length }}/3）</el-button>
            </el-upload>
          </div>
          <div class="muted">支持 jpg/png，单张不超过 2MB，最多 3 张</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">
            {{ isEdit ? '保存' : '发布' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
