import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { decideAccess } from './access'

declare module 'vue-router' {
  interface RouteMeta {
    title: string
    public?: boolean
  }
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '管理员登录', public: true },
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: 'Dashboard' } },
      { path: 'articles', name: 'articles', component: () => import('../views/ArticleListView.vue'), meta: { title: '文章管理' } },
      { path: 'articles/new', name: 'article-new', component: () => import('../views/ArticleEditorView.vue'), meta: { title: '新建文章' } },
      { path: 'articles/:id/edit', name: 'article-edit', component: () => import('../views/ArticleEditorView.vue'), meta: { title: '编辑文章' } },
      { path: 'categories', name: 'categories', component: () => import('../views/CategoriesView.vue'), meta: { title: '分类管理' } },
      { path: 'tags', name: 'tags', component: () => import('../views/TagsView.vue'), meta: { title: '标签管理' } },
      { path: 'comments', name: 'comments', component: () => import('../views/CommentsView.vue'), meta: { title: '评论审核' } },
      { path: 'users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理' } },
      { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面未找到' } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.bootstrapped) await auth.bootstrap()

  const decision = decideAccess(Boolean(to.meta.public), auth.isAdmin, to.fullPath, to.query.redirect)
  if (decision === true) return true
  if (decision.kind === 'redirect') return decision.path
  return { name: 'login', query: { redirect: decision.redirect } }
})

router.afterEach((to) => {
  document.title = `${to.meta.title} · Xinyu Aletheia`
})

export default router
