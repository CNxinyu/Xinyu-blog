<script setup lang="ts">
import { Compass } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <section class="not-found surface-card">
    <div class="not-found__mark"><Compass /></div>
    <p class="eyebrow">404 · Lost archive</p>
    <h1>这页没有被收录</h1>
    <p>地址可能已变更，或页面从未存在。</p>
    <el-button type="primary" @click="router.replace('/dashboard')">返回 Dashboard</el-button>
  </section>
</template>
