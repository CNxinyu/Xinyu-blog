<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { errorText } from '../api/client'
import { usersApi, type UserQuery } from '../api/users'
import PageHeader from '../components/PageHeader.vue'
import StatusTag from '../components/StatusTag.vue'
import { useAuthStore } from '../stores/auth'
import type { User, UserRole, UserStatus } from '../types/api'
import { displayName, formatDate } from '../utils/format'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const filters = reactive({
  keyword: '',
  role: '' as UserRole | '',
  status: '' as UserStatus | '',
  page: 1,
  size: 20,
})

async function load() {
  loading.value = true
  try {
    const query: UserQuery = {
      page: filters.page,
      size: filters.size,
      keyword: filters.keyword.trim() || undefined,
      role: filters.role || undefined,
      status: filters.status || undefined,
    }
    const page = await usersApi.page(query)
    users.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(errorText(error, '用户列表加载失败'))
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
  filters.role = ''
  filters.status = ''
  filters.page = 1
  load()
}

function isSelf(user: User) {
  return user.id === auth.user?.id
}

async function changeRole(user: User, role: UserRole) {
  if (isSelf(user) && role !== 'ADMIN') return
  try {
    await ElMessageBox.confirm(`确定将 ${displayName(user)} 的角色改为${role === 'ADMIN' ? '管理员' : '普通用户'}吗？`, '修改角色', {
      type: 'warning', confirmButtonText: '确认修改', cancelButtonText: '取消',
    })
    Object.assign(user, await usersApi.updateRole(user.id, role))
    ElMessage.success('用户角色已更新')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, '角色更新失败'))
  }
}

async function changeStatus(user: User, status: UserStatus) {
  if (isSelf(user) && status === 'DISABLED') return
  try {
    await ElMessageBox.confirm(
      `确定${status === 'ACTIVE' ? '启用' : '禁用'}用户 ${displayName(user)} 吗？`,
      `${status === 'ACTIVE' ? '启用' : '禁用'}用户`,
      { type: status === 'ACTIVE' ? 'info' : 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
    Object.assign(user, await usersApi.updateStatus(user.id, status))
    ElMessage.success(`用户已${status === 'ACTIVE' ? '启用' : '禁用'}`)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(errorText(error, '状态更新失败'))
  }
}

function roleChanged(user: User, value: unknown) {
  return changeRole(user, value as UserRole)
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="用户管理" description="管理成员状态与后台角色权限。">
      <template #actions><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></template>
    </PageHeader>

    <section class="surface-card filter-card">
      <el-form class="filter-form" :inline="true" @submit.prevent="search">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="用户名、邮箱或昵称" :prefix-icon="Search" @keyup.enter="search" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="filters.role" clearable placeholder="全部角色" style="width: 140px"><el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px"><el-option label="正常" value="ACTIVE" /><el-option label="已禁用" value="DISABLED" /></el-select></el-form-item>
        <el-form-item class="filter-actions"><el-button type="primary" :icon="Search" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </section>

    <section class="surface-card table-card">
      <el-table v-loading="loading" :data="users" class="data-table" row-key="id">
        <el-table-column label="用户" min-width="220"><template #default="scope"><div class="user-cell"><span class="table-avatar">{{ displayName(scope.row).slice(0, 1).toUpperCase() }}</span><div><strong>{{ displayName(scope.row) }} <small v-if="isSelf(scope.row)" class="self-badge">当前账号</small></strong><small>@{{ scope.row.username }}</small></div></div></template></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        <el-table-column label="角色" width="144"><template #default="scope"><el-select :model-value="scope.row.role" size="small" :disabled="isSelf(scope.row)" @change="roleChanged(scope.row, $event)"><el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" /></el-select></template></el-table-column>
        <el-table-column label="状态" width="104"><template #default="scope"><StatusTag :status="scope.row.status" /></template></el-table-column>
        <el-table-column label="注册时间" width="176"><template #default="scope">{{ formatDate(scope.row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" width="120"><template #default="scope"><el-button v-if="scope.row.status === 'ACTIVE'" text type="danger" :disabled="isSelf(scope.row)" @click="changeStatus(scope.row, 'DISABLED')">禁用</el-button><el-button v-else text type="success" @click="changeStatus(scope.row, 'ACTIVE')">启用</el-button></template></el-table-column>
        <template #empty><el-empty description="没有符合条件的用户" :image-size="72" /></template>
      </el-table>
      <div class="pagination-row"><span>共 {{ total }} 位</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next, sizes" :page-sizes="[10, 20, 50, 100]" :total="total" @change="load" /></div>
    </section>
  </div>
</template>
