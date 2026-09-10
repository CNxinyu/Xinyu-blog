<script setup lang="ts">
import DOMPurify from 'dompurify'
import { ArrowLeft, Check, CollectionTag, DocumentChecked, FolderOpened, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { articlesApi } from '../api/articles'
import { errorText } from '../api/client'
import { taxonomyApi } from '../api/taxonomy'
import StatusTag from '../components/StatusTag.vue'
import type { ArticleDetail, ArticleStatus, ArticleWritePayload, Category, Tag } from '../types/api'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const articleId = ref<number | null>(route.name === 'article-edit' ? Number(route.params.id) : null)
const currentStatus = ref<ArticleStatus>('DRAFT')
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const loading = ref(false)
const saving = ref(false)
const previewLoading = ref(false)
const previewHtml = ref('')
const previewError = ref('')
const mobileTab = ref<'edit' | 'preview'>('edit')
const cleanSnapshot = ref('')
let previewTimer: number | undefined
let previewSequence = 0

const form = reactive({
  title: '',
  slug: '',
  summary: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as number[],
  contentMarkdown: '',
})

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  slug: [
    { required: true, message: '请输入 Slug', trigger: 'blur' },
    { max: 255, message: 'Slug 不能超过 255 个字符', trigger: 'blur' },
    { pattern: slugPattern, message: '仅支持小写字母、数字和连字符', trigger: 'blur' },
  ],
  summary: [{ max: 500, message: '摘要不能超过 500 个字符', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  tagIds: [{ type: 'array', max: 30, message: '最多选择 30 个标签', trigger: 'change' }],
}

const isNew = computed(() => articleId.value === null)
const pageTitle = computed(() => (isNew.value ? '新建文章' : form.title || '编辑文章'))
const dirty = computed(() => cleanSnapshot.value !== snapshot())

function snapshot() {
  return JSON.stringify({ ...form, status: currentStatus.value })
}

function markClean() {
  cleanSnapshot.value = snapshot()
}

function assignArticle(article: ArticleDetail) {
  articleId.value = article.id
  currentStatus.value = article.status
  form.title = article.title
  form.slug = article.slug
  form.summary = article.summary || ''
  form.categoryId = article.category?.id
  form.tagIds = article.tags.map((tag) => tag.id)
  form.contentMarkdown = article.contentMarkdown || ''
  markClean()
}

function payload(): ArticleWritePayload {
  return {
    title: form.title.trim(),
    slug: form.slug.trim(),
    summary: form.summary.trim() || null,
    categoryId: form.categoryId as number,
    tagIds: form.tagIds,
    contentMarkdown: form.contentMarkdown,
  }
}

async function load() {
  loading.value = true
  try {
    const meta = await Promise.all([taxonomyApi.categories(), taxonomyApi.tags()])
    categories.value = meta[0]
    tags.value = meta[1]
    if (articleId.value) assignArticle(await articlesApi.detail(articleId.value))
    else markClean()
  } catch (error) {
    ElMessage.error(errorText(error, '文章信息加载失败'))
  } finally {
    loading.value = false
  }
}

async function saveBase(): Promise<ArticleDetail | null> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return null
  saving.value = true
  try {
    const article = articleId.value
      ? await articlesApi.update(articleId.value, payload())
      : await articlesApi.create(payload())
    assignArticle(article)
    if (route.name === 'article-new') await router.replace(`/articles/${article.id}/edit`)
    ElMessage.success('文章已保存')
    return article
  } catch (error) {
    ElMessage.error(errorText(error, '保存失败'))
    return null
  } finally {
    saving.value = false
  }
}

async function changeStatus(action: 'draft' | 'publish' | 'archive') {
  if (action === 'publish' && !form.contentMarkdown.trim()) {
    ElMessage.warning('发布前请填写 Markdown 正文')
    return
  }
  const saved = await saveBase()
  if (!saved || !articleId.value) return
  const labels = { draft: '转为草稿', publish: '发布', archive: '归档' }
  try {
    const article = await articlesApi.changeStatus(articleId.value, action)
    assignArticle(article)
    ElMessage.success(`文章已${labels[action]}`)
  } catch (error) {
    ElMessage.error(errorText(error, '状态变更失败'))
  }
}

async function updatePreview(markdown: string) {
  const sequence = ++previewSequence
  if (!markdown.trim()) {
    previewHtml.value = ''
    previewError.value = ''
    return
  }
  previewLoading.value = true
  try {
    const result = await articlesApi.preview(markdown)
    if (sequence !== previewSequence) return
    previewHtml.value = DOMPurify.sanitize(result.contentHtml)
    previewError.value = ''
  } catch (error) {
    if (sequence !== previewSequence) return
    previewError.value = errorText(error, '预览生成失败')
  } finally {
    if (sequence === previewSequence) previewLoading.value = false
  }
}

watch(
  () => form.contentMarkdown,
  (value) => {
    window.clearTimeout(previewTimer)
    previewTimer = window.setTimeout(() => updatePreview(value), 450)
  },
)

function beforeUnload(event: BeforeUnloadEvent) {
  if (!dirty.value) return
  event.preventDefault()
}

onBeforeRouteLeave(async () => {
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('当前文章还有未保存修改，确定离开吗？', '未保存修改', {
      type: 'warning', confirmButtonText: '离开', cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnload)
  await load()
  if (form.contentMarkdown) updatePreview(form.contentMarkdown)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  window.clearTimeout(previewTimer)
})
</script>

<template>
  <div v-loading="loading" class="editor-page">
    <header class="editor-heading">
      <div class="editor-heading__title">
        <el-button text circle aria-label="返回文章列表" @click="router.push('/articles')"><el-icon><ArrowLeft /></el-icon></el-button>
        <div><div class="editor-title-line"><h1>{{ pageTitle }}</h1><StatusTag v-if="!isNew" :status="currentStatus" /></div><p>{{ isNew ? '创建一份新的 Markdown 草稿。' : `文章 ID：${articleId}` }}</p></div>
      </div>
      <div class="editor-actions">
        <span v-if="dirty" class="dirty-indicator">有未保存修改</span>
        <el-button :icon="Check" :loading="saving" @click="saveBase">{{ isNew ? '保存草稿' : '保存修改' }}</el-button>
        <el-button v-if="currentStatus !== 'PUBLISHED'" type="primary" :icon="DocumentChecked" :loading="saving" @click="changeStatus('publish')">发布</el-button>
        <el-dropdown v-if="!isNew" trigger="click">
          <el-button>更多操作</el-button>
          <template #dropdown><el-dropdown-menu>
            <el-dropdown-item v-if="currentStatus !== 'DRAFT'" @click="changeStatus('draft')">转为草稿</el-dropdown-item>
            <el-dropdown-item v-if="currentStatus !== 'ARCHIVED'" @click="changeStatus('archive')">归档文章</el-dropdown-item>
          </el-dropdown-menu></template>
        </el-dropdown>
      </div>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="editor-form">
      <section class="surface-card editor-meta-card">
        <div class="editor-meta-grid">
          <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="200" show-word-limit placeholder="文章标题" /></el-form-item>
          <el-form-item label="Slug" prop="slug"><el-input v-model="form.slug" maxlength="255" placeholder="lowercase-slug" /></el-form-item>
          <el-form-item label="分类" prop="categoryId"><el-select v-model="form.categoryId" filterable placeholder="选择分类" style="width: 100%"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id"><span><el-icon><FolderOpened /></el-icon> {{ item.name }}</span></el-option></el-select></el-form-item>
          <el-form-item label="标签" prop="tagIds"><el-select v-model="form.tagIds" multiple filterable collapse-tags :max-collapse-tags="3" placeholder="最多选择 30 个" style="width: 100%"><el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id"><span><el-icon><CollectionTag /></el-icon> {{ item.name }}</span></el-option></el-select></el-form-item>
          <el-form-item class="summary-field" label="摘要" prop="summary"><el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="用于文章列表和搜索结果的简短说明" /></el-form-item>
        </div>
      </section>

      <div class="mobile-editor-tabs"><el-segmented v-model="mobileTab" :options="[{ label: 'Markdown', value: 'edit' }, { label: '预览', value: 'preview' }]" /></div>
      <section class="editor-workbench surface-card">
        <div class="editor-pane editor-pane--input" :class="{ 'mobile-hidden': mobileTab !== 'edit' }">
          <header><div><h2>Markdown</h2><p>{{ form.contentMarkdown.length.toLocaleString() }} / 1,000,000 字符</p></div></header>
          <el-input v-model="form.contentMarkdown" type="textarea" resize="none" maxlength="1000000" placeholder="# 从这里开始写作…" />
        </div>
        <div class="editor-pane editor-pane--preview" :class="{ 'mobile-hidden': mobileTab !== 'preview' }">
          <header><div><h2>实时预览</h2><p>由后端 Markdown 渲染器生成</p></div><el-icon v-if="previewLoading" class="is-loading"><Refresh /></el-icon></header>
          <el-alert v-if="previewError" :title="previewError" type="error" :closable="false" show-icon />
          <article v-if="previewHtml" class="markdown-preview" v-html="previewHtml" />
          <el-empty v-else-if="!previewLoading && !previewError" description="输入正文后显示预览" :image-size="70" />
        </div>
      </section>
    </el-form>
  </div>
</template>
