<script setup lang="ts">
import { ChatDotRound, DocumentAdd, Files, PriceTag, Refresh, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loadDashboard, type DashboardData, type MetricKey } from '../api/dashboard'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import type { ArticleSummary } from '../types/api'
import { formatDate } from '../utils/format'

const router = useRouter()
const loading = ref(true)
const data = ref<DashboardData | null>(null)

const cards = computed(() => {
  const metrics = data.value?.metrics
  return [
    { key: 'articles' as MetricKey, label: '文章总数', value: metrics?.articles, note: '全部内容资产', tone: 'violet' },
    { key: 'published' as MetricKey, label: '已发布', value: metrics?.published, note: '读者当前可见', tone: 'green' },
    { key: 'drafts' as MetricKey, label: '草稿', value: metrics?.drafts, note: '等待继续完善', tone: 'gold' },
    { key: 'pendingComments' as MetricKey, label: '待审核评论', value: metrics?.pendingComments, note: '需要你的判断', tone: 'rose' },
    { key: 'users' as MetricKey, label: '用户', value: metrics?.users, note: '全部注册成员', tone: 'blue' },
  ]
})

async function load() {
  loading.value = true
  try {
    data.value = await loadDashboard()
  } finally {
    loading.value = false
  }
}

function openArticle(row: ArticleSummary) {
  router.push(`/articles/${row.id}/edit`)
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="Dashboard" description="聚合内容状态与待处理事项。">
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="DocumentAdd" @click="router.push('/articles/new')">新建文章</el-button>
      </template>
    </PageHeader>

    <section v-loading="loading" class="metric-grid" aria-label="关键指标">
      <article v-for="card in cards" :key="card.key" class="metric-card" :class="`metric-card--${card.tone}`">
        <span class="metric-card__label">{{ card.label }}</span>
        <strong>{{ card.value ?? '—' }}</strong>
        <small v-if="data?.failed.includes(card.key)">暂时无法读取</small>
        <small v-else>{{ card.note }}</small>
      </article>
    </section>

    <div class="dashboard-grid">
      <section class="surface-card recent-panel">
        <div class="surface-card__header">
          <div><h2>最近更新</h2><p>最近发生变动的 5 篇文章</p></div>
          <el-button text type="primary" @click="router.push('/articles')">查看全部</el-button>
        </div>
        <el-table v-if="data?.recentArticles.length" :data="data.recentArticles" class="data-table" @row-click="openArticle">
          <el-table-column prop="title" label="文章" min-width="240" show-overflow-tooltip />
          <el-table-column label="状态" width="100"><template #default="scope"><StatusTag :status="scope.row.status" /></template></el-table-column>
          <el-table-column label="更新时间" width="176"><template #default="scope">{{ formatDate(scope.row.updatedAt) }}</template></el-table-column>
        </el-table>
        <el-empty v-else-if="!loading" description="还没有文章" :image-size="72" />
      </section>

      <aside class="dashboard-aside">
        <section class="surface-card compact-card">
          <div class="surface-card__header"><div><h2>内容结构</h2><p>当前分类与标签规模</p></div></div>
          <div class="taxonomy-counts">
            <button type="button" @click="router.push('/categories')"><Files /><span>分类</span><strong>{{ data?.metrics.categories ?? '—' }}</strong></button>
            <button type="button" @click="router.push('/tags')"><PriceTag /><span>标签</span><strong>{{ data?.metrics.tags ?? '—' }}</strong></button>
          </div>
        </section>
        <section class="surface-card compact-card">
          <div class="surface-card__header"><div><h2>快速处理</h2><p>直接进入高频工作</p></div></div>
          <div class="quick-links">
            <button type="button" @click="router.push('/comments')"><ChatDotRound /><span><strong>审核评论</strong><small>处理读者反馈</small></span></button>
            <button type="button" @click="router.push('/users')"><User /><span><strong>管理用户</strong><small>维护角色与状态</small></span></button>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>
