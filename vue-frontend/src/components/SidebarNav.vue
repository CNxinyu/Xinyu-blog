<script setup lang="ts">
import {
  ChatDotRound,
  Collection,
  DataAnalysis,
  Document,
  Files,
  PriceTag,
  User,
} from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = defineProps<{ collapsed?: boolean }>()
const emit = defineEmits<{ navigate: [] }>()
const route = useRoute()
const router = useRouter()

const activePath = computed(() => {
  if (route.path.startsWith('/articles')) return '/articles'
  return route.path
})

async function navigate(path: string) {
  await router.push(path)
  emit('navigate')
}
</script>

<template>
  <div class="sidebar-nav" :class="{ 'is-collapsed': props.collapsed }">
    <button class="brand" type="button" aria-label="返回 Dashboard" @click="navigate('/dashboard')">
      <span class="brand-mark" aria-hidden="true">A</span>
      <span v-if="!props.collapsed" class="brand-copy">
        <strong>Aletheia</strong>
        <small>管理中枢</small>
      </span>
    </button>

    <el-menu
      class="nav-menu"
      :default-active="activePath"
      :collapse="props.collapsed"
      :collapse-transition="false"
      @select="navigate"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataAnalysis /></el-icon>
        <template #title>Dashboard</template>
      </el-menu-item>
      <el-menu-item index="/articles">
        <el-icon><Document /></el-icon>
        <template #title>文章管理</template>
      </el-menu-item>
      <el-menu-item index="/categories">
        <el-icon><Files /></el-icon>
        <template #title>分类管理</template>
      </el-menu-item>
      <el-menu-item index="/tags">
        <el-icon><PriceTag /></el-icon>
        <template #title>标签管理</template>
      </el-menu-item>
      <el-menu-item index="/comments">
        <el-icon><ChatDotRound /></el-icon>
        <template #title>评论审核</template>
      </el-menu-item>
      <el-menu-item index="/users">
        <el-icon><User /></el-icon>
        <template #title>用户管理</template>
      </el-menu-item>
    </el-menu>

    <div v-if="!props.collapsed" class="sidebar-footnote">
      <Collection aria-hidden="true" />
      <span>内容与秩序，由此维护</span>
    </div>
  </div>
</template>
