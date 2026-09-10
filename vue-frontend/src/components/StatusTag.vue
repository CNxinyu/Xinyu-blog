<script setup lang="ts">
import { computed } from 'vue'
import type { ArticleStatus, CommentStatus, UserRole, UserStatus } from '../types/api'

type Status = ArticleStatus | CommentStatus | UserRole | UserStatus
const props = defineProps<{ status: Status }>()

const labels: Record<Status, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  USER: '用户',
  ADMIN: '管理员',
  ACTIVE: '正常',
  DISABLED: '已禁用',
}

const tagType = computed(() => {
  if (['PUBLISHED', 'APPROVED', 'ACTIVE'].includes(props.status)) return 'success'
  if (['REJECTED', 'DISABLED'].includes(props.status)) return 'danger'
  if (['PENDING', 'DRAFT'].includes(props.status)) return 'warning'
  if (props.status === 'ADMIN') return 'primary'
  return 'info'
})
</script>

<template>
  <el-tag :type="tagType" effect="dark" size="small">{{ labels[status] }}</el-tag>
</template>
