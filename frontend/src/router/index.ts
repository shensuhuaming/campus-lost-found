import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import FrontLayout from '@/layouts/FrontLayout.vue'
import HomeView from '@/views/Home.vue'
import ItemDetail from '@/views/ItemDetail.vue'
import ItemEdit from '@/views/ItemEdit.vue'
import AdminClaims from '@/views/AdminClaims.vue'
import LoginView from '@/views/Login.vue'
import ProfileView from '@/views/Profile.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: FrontLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'items/new', name: 'itemNew', component: ItemEdit, meta: { requiresAuth: true } },
        { path: 'items/:id', name: 'itemDetail', component: ItemDetail },
        { path: 'items/:id/edit', name: 'itemEdit', component: ItemEdit, meta: { requiresAuth: true } },
        {
          path: 'profile',
          name: 'profile',
          component: ProfileView,
          meta: { requiresAuth: true },
        },
        {
          path: 'admin/claims',
          name: 'adminClaims',
          component: AdminClaims,
          meta: { requiresAuth: true, requiresAdmin: true },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

/**
 * 路由守卫（详细设计 3.3）：
 * - requiresAuth 且无 token → /login?redirect=原路径
 * - guestOnly 且已登录 → 首页
 */
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    ElMessage.warning('无访问权限')
    return { path: '/' }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { path: '/' }
  }
  return true
})

export default router
