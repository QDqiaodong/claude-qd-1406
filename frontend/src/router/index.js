import { createRouter, createWebHashHistory } from 'vue-router'

// 注意：main.js 里没有 app.use(router)，路由实例由 App.vue 直接驱动，
// 所以这里全部用静态 import —— <component :is> 拿到的是组件对象本身。
import DeptView from '../views/DeptView.vue'
import PackageView from '../views/PackageView.vue'
import ExamineeView from '../views/ExamineeView.vue'
import ExamView from '../views/ExamView.vue'

const routes = [
  { path: '/', redirect: '/examinee' },
  { path: '/dept', name: 'dept', component: DeptView },
  { path: '/package', name: 'package', component: PackageView },
  { path: '/examinee', name: 'examinee', component: ExamineeView },
  { path: '/exam', name: 'exam', component: ExamView },
  { path: '/:pathMatch(.*)*', redirect: '/examinee' }
]

export default createRouter({
  history: createWebHashHistory(),
  routes
})
