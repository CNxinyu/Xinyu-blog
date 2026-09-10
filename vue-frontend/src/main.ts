import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import { configureRefreshHandler } from './api/client'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
const auth = useAuthStore(pinia)
configureRefreshHandler(async () => {
  const refreshed = await auth.refreshSession()
  if (!refreshed && router.currentRoute.value.name !== 'login') {
    const redirect = router.currentRoute.value.fullPath
    await router.replace({ name: 'login', query: { redirect } })
  }
  return refreshed
})
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
