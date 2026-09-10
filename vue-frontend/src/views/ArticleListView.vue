<script setup lang="ts">
import { Delete, EditPen, MoreFilled, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { articlesApi, type ArticleQuery } from '../api/articles'
import { errorText } from '../api/client'
import { taxonomyApi } from '../api/taxonomy'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import type { ArticleStatus, ArticleSummary, Category, Tag } from '../types/api'
import { displayName, formatDate } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const articles = ref<ArticleSummary[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const total = ref(0)
const filters = reactive({
  keyword: '',
  status: '' as ArticleStatus | '',
  categoryId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  page: 1,
  size: 20,
})

async function load() {
  loading.value = true
  try {
    const query: ArticleQuery = {
      page: filters.page,
      size: filters.size,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      categoryId: filters.categoryId,
      tagId: filters.tagId,
    }
    const page = await articlesApi.page(query)
    articles.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorText(error, '文章列表加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.categoryId = undefined
  filters.tagId = undefined
  filters.page = 1
  load()
}

async function changeStatus(row: ArticleSummary, action: 'draft' | 'publish' | 'archive') {
  const labels = { draft: '转为草稿', publish: '发布', archive: '归档' }
  try {
    await ElMessageBox.confirm(`确定要将《${row.title}》${labels[action]}吗？`, '确认状态变更', {
      type: 'warning',
      confirmButtonText: labels[action],
      cancelButtonText: '取消',
    })
    const updated = await articlesApi.changeStatus(row.id, action)
    Object.assign(row, updated)
    ElMessage.success(`文章已${labels[action]}`)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, '状态变更失败'))
  }
}

async function remove(row: ArticleSummary) {
  try {
    await ElMessageBox.confirm(`永久删除《${row.title}》？此操作无法撤销。`, '删除文章', {
      type: 'error',
      confirmButtonText: '永久删除',
      cancelButtonText: '取消',
    })
    await articlesApi.delete(row.id)
    ElMessage.success('文章已删除')
    if (articles.value.length === 1 && filters.page > 1) filters.page -= 1
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, '删除失败'))
  }
}

function handleCommand(row: ArticleSummary, command: unknown) {
  if (command === 'delete') return remove(row)
  return changeStatus(row, command as 'draft' | 'publish' | 'archive')
}

onMounted(async () => {
  const taxonomy = await Promise.allSettled([taxonomyApi.categories(), taxonomyApi.tags()])
  if (taxonomy[0].status === 'fulfilled') categories.value = taxonomy[0].value
  if (taxonomy[1].status === 'fulfilled') tags.value = taxonomy[1].value
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="文章管理" description="维护草稿、发布内容与归档记录。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="router.push('/articles/new')">新建文章</el-button>
      </template>
    </PageHeader>

    <section class="surface-card filter-card">
      <el-form class="filter-form" :inline="true" @submit.prevent="search">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="标题或摘要" :prefix-icon="Search" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /><el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" clearable filterable placeholder="全部分类" style="width: 160px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="filters.tagId" clearable filterable placeholder="全部标签" style="width: 160px">
            <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="surface-card table-card">
      <el-table v-loading="loading" :data="articles" class="data-table" row-key="id">
        <el-table-column label="文章" min-width="280">
          <template #default="scope"><div class="article-cell"><strong>{{ scope.row.title }}</strong><small>/{{ scope.row.slug }}</small></div></template>
        </el-table-column>
        <el-table-column label="分类" min-width="120"><template #default="scope">{{ scope.row.category?.name || '—' }}</template></el-table-column>
        <el-table-column label="标签" min-width="170">
          <template #default="scope"><div class="tag-stack"><el-tag v-for="tag in scope.row.tags.slice(0, 3)" :key="tag.id" effect="plain" size="small">{{ tag.name }}</el-tag><span v-if="scope.row.tags.length > 3">+{{ scope.row.tags.length - 3 }}</span></div></template>
        </el-table-column>
        <el-table-column label="状态" width="104"><template #default="scope"><StatusTag :status="scope.row.status" /></template></el-table-column>
        <el-table-column label="作者" min-width="110"><template #default="scope">{{ displayName(scope.row.author) }}</template></el-table-column>
        <el-table-column label="更新时间" width="176"><template #default="scope">{{ formatDate(scope.row.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" width="132">
          <template #default="scope">
            <div class="row-actions">
              <el-button text type="primary" :icon="EditPen" @click="router.push(`/articles/${scope.row.id}/edit`)">编辑</el-button>
              <el-dropdown trigger="click" @command="handleCommand(scope.row, $event)">
                <el-button text circle aria-label="更多操作"><el-icon><MoreFilled /></el-icon></el-button>
                <template #dropdown><el-dropdown-menu>
                  <el-dropdown-item v-if="scope.row.status !== 'PUBLISHED'" command="publish">发布</el-dropdown-item>
                  <el-dropdown-item v-if="scope.row.status !== 'DRAFT'" command="draft">转为草稿</el-dropdown-item>
                  <el-dropdown-item v-if="scope.row.status !== 'ARCHIVED'" command="archive">归档</el-dropdown-item>
                  <el-dropdown-item command="delete" divided :icon="Delete">永久删除</el-dropdown-item>
                </el-dropdown-menu></template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有符合条件的文章" :image-size="72" /></template>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ total }} 篇</span>
        <el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next, sizes" :page-sizes="[10, 20, 50, 100]" :total="total" @change="load" />
      </div>
    </section>
  </div>
</template>
