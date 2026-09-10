<script setup lang="ts">
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { errorText } from '../api/client'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const error = ref('')
const form = reactive({ identifier: '', password: '' })
const rules: FormRules = {
  identifier: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const redirect = computed(() => {
  const value = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  return value.startsWith('/') && !value.startsWith('//') ? value : '/dashboard'
})

async function submit() {
  error.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await auth.login(form.identifier.trim(), form.password)
    await router.replace(redirect.value)
  } catch (reason) {
    error.value = errorText(reason, '登录失败')
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro" aria-labelledby="login-brand-title">
      <div class="login-sigil" aria-hidden="true"><span>A</span></div>
      <p class="eyebrow">Xinyu · Aletheia</p>
      <h1 id="login-brand-title">管理你的知识工坊</h1>
      <p>文章、分类、评论与成员权限，都在同一处保持清晰和有序。</p>
      <dl class="login-facts">
        <div><dt>安全会话</dt><dd>短时访问令牌与刷新轮换</dd></div>
        <div><dt>内容秩序</dt><dd>草稿、发布、归档全流程</dd></div>
      </dl>
    </section>

    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-card">
        <p class="eyebrow">仅限管理员</p>
        <h2 id="login-title">登录管理台</h2>
        <p class="login-hint">使用管理员用户名或邮箱继续。</p>

        <el-alert v-if="error" class="login-alert" :title="error" type="error" :closable="false" show-icon />

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名或邮箱" prop="identifier">
            <el-input v-model="form.identifier" size="large" autocomplete="username" placeholder="admin@example.com" :prefix-icon="User" @keyup.enter="submit" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" placeholder="输入密码" show-password :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button class="login-submit" type="primary" size="large" native-type="submit" :loading="auth.authenticating">进入管理台</el-button>
        </el-form>
        <p class="login-note">登录状态不会写入浏览器本地存储。</p>
      </div>
    </section>
  </main>
</template>
