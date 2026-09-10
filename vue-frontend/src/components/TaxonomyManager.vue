<script setup lang="ts">
import { Delete, EditPen, Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { errorText, normalizeApiError } from '../api/client'
import { taxonomyApi } from '../api/taxonomy'
import type { Category, Tag } from '../types/api'
import { formatDate } from '../utils/format'
import PageHeader from './PageHeader.vue'

const props = defineProps<{ kind: 'category' | 'tag' }>()
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const items = ref<(Category | Tag)[]>([])
const keyword = ref('')
const formRef = ref<FormInstance>()
const form = reactive({ name: '', slug: '', description: '' })

const isCategory = computed(() => props.kind === 'category')
const noun = computed(() => (isCategory.value ? '分类' : '标签'))
const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return items.value
  return items.value.filter((item) => item.name.toLowerCase().includes(query) || item.slug.includes(query))
})
const rules: FormRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 80, message: '名称不能超过 80 个字符', trigger: 'blur' },
  ],
  slug: [
    { required: true, message: '请输入 Slug', trigger: 'blur' },
    { max: 100, message: 'Slug 不能超过 100 个字符', trigger: 'blur' },
    { pattern: /^[a-z0-9]+(?:-[a-z0-9]+)*$/, message: '仅支持小写字母、数字和连字符', trigger: 'blur' },
  ],
  description: [{ max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    items.value = isCategory.value ? await taxonomyApi.categories() : await taxonomyApi.tags()
  } catch (error) {
    ElMessage.error(errorText(error, `${noun.value}列表加载失败`))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.description = ''
  dialogOpen.value = true
}

function openEdit(item: Category | Tag) {
  editingId.value = item.id
  form.name = item.name
  form.slug = item.slug
  form.description = 'description' in item ? item.description || '' : ''
  dialogOpen.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isCategory.value) {
      const payload = { name: form.name.trim(), slug: form.slug.trim(), description: form.description.trim() || null }
      if (editingId.value) await taxonomyApi.updateCategory(editingId.value, payload)
      else await taxonomyApi.createCategory(payload)
    } else {
      const payload = { name: form.name.trim(), slug: form.slug.trim() }
      if (editingId.value) await taxonomyApi.updateTag(editingId.value, payload)
      else await taxonomyApi.createTag(payload)
    }
    ElMessage.success(`${noun.value}已${editingId.value ? '更新' : '创建'}`)
    dialogOpen.value = false
    await load()
  } catch (error) {
    const normalized = normalizeApiError(error)
    if (normalized.fields.name) formRef.value?.validateField('name')
    if (normalized.fields.slug) formRef.value?.validateField('slug')
    ElMessage.error(errorText(error, `${noun.value}保存失败`))
  } finally {
    saving.value = false
  }
}

async function remove(item: Category | Tag) {
  try {
    await ElMessageBox.confirm(
      `确定删除${noun.value}“${item.name}”吗？${isCategory.value ? '被文章使用的分类无法删除。' : ''}`,
      `删除${noun.value}`,
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    if (isCategory.value) await taxonomyApi.deleteCategory(item.id)
    else await taxonomyApi.deleteTag(item.id)
    ElMessage.success(`${noun.value}已删除`)
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, `删除${noun.value}失败`))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="`${noun}管理`" :description="isCategory ? '组织文章的主要内容分区。' : '维护跨文章使用的主题标记。'">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增{{ noun }}</el-button>
      </template>
    </PageHeader>

    <section class="surface-card table-card taxonomy-table-card">
      <div class="inline-search">
        <el-input v-model="keyword" clearable :prefix-icon="Search" :placeholder="`搜索${noun}名称或 Slug`" />
        <span>共 {{ filteredItems.length }} 个{{ noun }}</span>
      </div>
      <el-table v-loading="loading" :data="filteredItems" class="data-table" row-key="id">
        <el-table-column prop="name" label="名称" min-width="180"><template #default="scope"><strong>{{ scope.row.name }}</strong></template></el-table-column>
        <el-table-column prop="slug" label="Slug" min-width="190"><template #default="scope"><code class="slug-code">{{ scope.row.slug }}</code></template></el-table-column>
        <el-table-column v-if="isCategory" prop="description" label="描述" min-width="300" show-overflow-tooltip><template #default="scope">{{ scope.row.description || '—' }}</template></el-table-column>
        <el-table-column label="更新时间" width="176"><template #default="scope">{{ formatDate(scope.row.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" width="160"><template #default="scope"><div class="row-actions"><el-button text type="primary" :icon="EditPen" @click="openEdit(scope.row)">编辑</el-button><el-button text type="danger" :icon="Delete" @click="remove(scope.row)">删除</el-button></div></template></el-table-column>
        <template #empty><el-empty :description="`还没有${noun}`" :image-size="72" /></template>
      </el-table>
    </section>

    <el-dialog v-model="dialogOpen" :title="`${editingId ? '编辑' : '新增'}${noun}`" width="min(520px, calc(100vw - 32px))" destroy-on-close @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" maxlength="80" show-word-limit :placeholder="`${noun}名称`" /></el-form-item>
        <el-form-item label="Slug" prop="slug"><el-input v-model="form.slug" maxlength="100" placeholder="lowercase-slug" /></el-form-item>
        <el-form-item v-if="isCategory" label="描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="简要说明这个分类收录的内容" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">保存</el-button></template>
    </el-dialog>
  </div>
</template>
