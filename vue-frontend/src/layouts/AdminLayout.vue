<script setup lang="ts">
import { ArrowDown, Fold, Menu, MoonNight, SwitchButton, Expand } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SidebarNav from '../components/SidebarNav.vue'
import { errorText } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { displayName } from '../utils/format'

const collapsed = ref(false)
const mobileOpen = ref(false)
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const title = computed(() => route.meta.title)
const initials = computed(() => displayName(auth.user).slice(0, 1).toUpperCase())

async function accountCommand(command: string) {
  if (command !== 'logout') return
  try {
    await auth.logout()
    ElMessage.success('已安全退出')
  } catch (error) {
    ElMessage.warning(errorText(error, '会话已在本地清除'))
  } finally {
    await router.replace('/login')
  }
}
</script>

<template>
  <div class="admin-shell" :class="{ 'sidebar-collapsed': collapsed }">
    <aside class="desktop-sidebar">
      <SidebarNav :collapsed="collapsed" />
    </aside>

    <el-drawer v-model="mobileOpen" direction="ltr" size="286px" :with-header="false" class="mobile-drawer">
      <SidebarNav @navigate="mobileOpen = false" />
    </el-drawer>

    <section class="admin-stage">
      <header class="topbar">
        <div class="topbar__left">
          <el-button class="mobile-menu-button" text circle aria-label="打开导航" @click="mobileOpen = true">
            <el-icon><Menu /></el-icon>
          </el-button>
          <el-button class="collapse-button" text circle :aria-label="collapsed ? '展开侧栏' : '收起侧栏'" @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <span class="breadcrumb-root">Aletheia</span>
          <span class="breadcrumb-divider">/</span>
          <strong>{{ title }}</strong>
        </div>

        <div class="topbar__right">
          <div class="environment-chip"><MoonNight /> 本地管理端</div>
          <el-dropdown trigger="click" @command="accountCommand">
            <button class="account-trigger" type="button">
              <span class="account-avatar">{{ initials }}</span>
              <span class="account-copy">
                <strong>{{ displayName(auth.user) }}</strong>
                <small>{{ auth.user?.email }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
