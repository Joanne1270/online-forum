import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  { path: '/', component: () => import('../views/board/Home.vue') },
  { path: '/login', component: () => import('../views/auth/Login.vue') },
  { path: '/register', component: () => import('../views/auth/Register.vue') },
  { path: '/forgot-password', component: () => import('../views/auth/ForgotPassword.vue') },
  { path: '/board/:id', component: () => import('../views/post/PostList.vue') },
  { path: '/post/create', component: () => import('../views/post/PostEditor.vue'), meta: { auth: true } },
  { path: '/post/draft/:id/edit', component: () => import('../views/post/PostEditor.vue'), meta: { auth: true } },
  { path: '/post/:id', component: () => import('../views/post/PostDetail.vue') },
  { path: '/post/:id/edit', component: () => import('../views/post/PostEditor.vue'), meta: { auth: true } },
  { path: '/search', component: () => import('../views/post/Search.vue') },
  { path: '/following', component: () => import('../views/user/Following.vue'), meta: { auth: true, userOnly: true } },
  { path: '/messages', component: () => import('../views/user/MessageCenter.vue'), meta: { auth: true, userOnly: true } },
  { path: '/messages/chat/:userId', component: () => import('../views/user/MessageCenter.vue'), meta: { auth: true, userOnly: true } },
  { path: '/messages/:userId', redirect: to => `/messages/chat/${to.params.userId}` },
  { path: '/profile', component: () => import('../views/user/Profile.vue'), meta: { auth: true } },
  { path: '/user/:id', component: () => import('../views/user/UserPublic.vue') },
  { path: '/notifications', redirect: { path: '/messages', query: { category: 'reply' } } },
  {
    path: '/admin',
    component: () => import('../views/admin/Layout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: 'dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'boards', component: () => import('../views/admin/Boards.vue') },
      { path: 'posts', component: () => import('../views/admin/Posts.vue') },
      { path: 'reports', component: () => import('../views/admin/Reports.vue') },
      { path: 'profile-requests', component: () => import('../views/admin/ProfileRequests.vue') },
      { path: 'banned-words', component: () => import('../views/admin/BannedWords.vue') },
      { path: 'users', component: () => import('../views/admin/Users.vue') }
    ]
  }
]

const router = new VueRouter({ mode: 'history', routes })

router.beforeEach((to, from, next) => {
  if (to.meta.auth && !store.getters.isLogin) {
    next('/login')
    return
  }
  if (to.meta.admin && !store.getters.isAdmin) {
    next('/')
    return
  }
  if (to.meta.userOnly && store.getters.isAdmin) {
    next('/')
    return
  }
  next()
})

export default router
