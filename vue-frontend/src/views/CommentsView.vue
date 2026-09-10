<script setup lang="ts">
import { Delete, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { commentsApi, type CommentQuery } from '../api/comments'
import { errorText } from '../api/client'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import type { Comment, CommentStatus } from '../types/api'
import { displayName, formatDate, truncate } from '../utils/format'

const loading = ref(false)
const comments = ref<Comment[]>([])
const selected = ref<Comment | null>(null)
const detailOpen = ref(false)
const total = ref(0)
const filters = reactive({
  keyword: '',
  status: 'PENDING' as CommentStatus | '',
  articleId: undefined as number | undefined,
  page: 1,
  size: 20,
})

async function load() {
  loading.value = true
  try {
    const query: CommentQuery = {
      page: filters.page,
      size: filters.size,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      articleId: filters.articleId,
    }
    const page = await commentsApi.page(query)
    comments.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorText(error, '评论列表加载失败'))
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
  filters.status = 'PENDING'
  filters.articleId = undefined
  filters.page = 1
  load()
}

function inspect(comment: Comment) {
  selected.value = comment
  detailOpen.value = true
}

async function setStatus(comment: Comment, status: CommentStatus) {
  try {
    const updated = await commentsApi.updateStatus(comment.id, status)
    Object.assign(comment, updated)
    if (selected.value?.id === comment.id) selected.value = comment
    ElMessage.success('评论状态已更新')
  } catch (error) {
    ElMessage.error(errorText(error, '审核失败'))
  }
}

async function remove(comment: Comment) {
  try {
    await ElMessageBox.confirm('永久删除这条评论及其回复？此操作无法撤销。', '删除评论', {
      type: 'error', confirmButtonText: '永久删除', cancelButtonText: '取消',
    })
    await commentsApi.delete(comment.id)
    detailOpen.value = false
    ElMessage.success('评论已删除')
    if (comments.value.length === 1 && filters.page > 1) filters.page -= 1
    await load()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="评论审核" description="处理待审核内容并维护讨论秩序。">
      <template #actions><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></template>
    </PageHeader>

    <section class="surface-card filter-card">
      <el-form class="filter-form" :inline="true" @submit.prevent="search">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="评论内容" :prefix-icon="Search" @keyup.enter="search" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px"><el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已拒绝" value="REJECTED" /></el-select></el-form-item>
        <el-form-item label="文章 ID"><el-input-number v-model="filters.articleId" :min="1" :controls="false" placeholder="全部文章" /></el-form-item>
        <el-form-item class="filter-actions"><el-button type="primary" :icon="Search" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </section>

    <section class="surface-card table-card">
      <el-table v-loading="loading" :data="comments" class="data-table" row-key="id">
        <el-table-column label="评论" min-width="300"><template #default="scope"><button class="content-preview" type="button" @click="inspect(scope.row)">{{ truncate(scope.row.content, 90) }}</button></template></el-table-column>
        <el-table-column label="作者" min-width="140"><template #default="scope"><div class="author-cell"><strong>{{ displayName(scope.row.author) }}</strong><small>@{{ scope.row.author.username }}</small></div></template></el-table-column>
        <el-table-column prop="articleId" label="文章 ID" width="100" />
        <el-table-column label="状态" width="104"><template #default="scope"><StatusTag :status="scope.row.status" /></template></el-table-column>
        <el-table-column label="提交时间" width="176"><template #default="scope">{{ formatDate(scope.row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" width="250"><template #default="scope"><div class="row-actions"><el-button text :icon="View" @click="inspect(scope.row)">详情</el-button><el-button v-if="scope.row.status !== 'APPROVED'" text type="success" @click="setStatus(scope.row, 'APPROVED')">通过</el-button><el-button v-if="scope.row.status !== 'REJECTED'" text type="warning" @click="setStatus(scope.row, 'REJECTED')">拒绝</el-button><el-button text type="danger" :icon="Delete" @click="remove(scope.row)" /></div></template></el-table-column>
        <template #empty><el-empty description="没有符合条件的评论" :image-size="72" /></template>
      </el-table>
      <div class="pagination-row"><span>共 {{ total }} 条</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next, sizes" :page-sizes="[10, 20, 50, 100]" :total="total" @change="load" /></div>
    </section>

    <el-dialog v-model="detailOpen" title="评论详情" width="min(620px, calc(100vw - 32px))">
      <template v-if="selected"><dl class="detail-list"><div><dt>作者</dt><dd>{{ displayName(selected.author) }}（@{{ selected.author.username }}）</dd></div><div><dt>文章 ID</dt><dd>{{ selected.articleId }}</dd></div><div><dt>回复评论</dt><dd>{{ selected.parentId || '顶层评论' }}</dd></div><div><dt>状态</dt><dd><StatusTag :status="selected.status" /></dd></div><div><dt>提交时间</dt><dd>{{ formatDate(selected.createdAt) }}</dd></div></dl><div class="comment-content">{{ selected.content }}</div></template>
      <template #footer v-if="selected"><el-button v-if="selected.status !== 'PENDING'" @click="setStatus(selected, 'PENDING')">退回待审核</el-button><el-button v-if="selected.status !== 'REJECTED'" type="warning" @click="setStatus(selected, 'REJECTED')">拒绝</el-button><el-button v-if="selected.status !== 'APPROVED'" type="success" @click="setStatus(selected, 'APPROVED')">通过</el-button><el-button type="danger" plain @click="remove(selected)">删除</el-button></template>
    </el-dialog>
  </div>
</template>
