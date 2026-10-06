<script setup>
import { computed, onMounted, ref } from 'vue'
import router from './router'
import api from './api'

import DeptView from './views/DeptView.vue'
import PackageView from './views/PackageView.vue'
import ExamineeView from './views/ExamineeView.vue'
import ExamView from './views/ExamView.vue'

// ------------------------------------------------------------------
// 导航形态：左侧科室列表（点选后作为全局科室过滤）+ 顶部体检套餐 chips（过滤套餐 / 体检人）。
// 四个业务模块通过侧栏顶部的菜单项切换，路由实例由本 App.vue 驱动 + <component :is> 渲染。
// ------------------------------------------------------------------

const modules = [
  { path: '/dept', label: '科室管理', comp: DeptView },
  { path: '/package', label: '体检套餐', comp: PackageView },
  { path: '/examinee', label: '体检人', comp: ExamineeView },
  { path: '/exam', label: '检查项记录', comp: ExamView }
]

const currentPath = ref(router.currentRoute.value.path)
const depts = ref([])
const packages = ref([])
const selectedDeptId = ref(null)
const selectedPackageId = ref(null)
const examCountByDept = ref({})

const currentView = computed(() => {
  const m = modules.find(i => i.path === currentPath.value)
  return m ? m.comp : modules[2]
})

const availableDepts = computed(() => depts.value.filter(d => d.status === '开放'))

function selectDept(id) {
  selectedDeptId.value = (selectedDeptId.value === id) ? null : id
}

function selectPackage(id) {
  selectedPackageId.value = (selectedPackageId.value === id) ? null : id
}

function onNavigate(path) {
  if (path !== currentPath.value) {
    router.push(path)
  }
}

async function loadDepts() {
  depts.value = await api.listDepts()
}

async function loadPackages() {
  packages.value = await api.listPackages()
}

async function loadExamCounts() {
  try {
    const exams = await api.listExams()
    const m = {}
    exams.forEach(e => {
      const k = String(e.deptId)
      m[k] = (m[k] || 0) + 1
    })
    examCountByDept.value = m
  } catch (e) {
    examCountByDept.value = {}
  }
}

async function reloadAll() {
  await Promise.all([loadDepts(), loadPackages(), loadExamCounts()])
}

router.afterEach((to) => { currentPath.value = to.path })

onMounted(async () => {
  currentPath.value = router.currentRoute.value.path
  await reloadAll()
})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">检</span>
        <div class="brand-text">
          <div class="brand-title">体检中心</div>
          <div class="brand-sub">亮蓝主题 · 左侧科室导航</div>
        </div>
      </div>

      <el-menu class="nav-menu" :default-active="currentPath" @select="onNavigate">
        <el-menu-item v-for="m in modules" :key="m.path" :index="m.path">{{ m.label }}</el-menu-item>
      </el-menu>

      <div class="side-title">科室列表</div>
      <div class="dept-list">
        <div
          class="dept-item all"
          :class="{ active: selectedDeptId === null }"
          @click="selectedDeptId = null"
        >
          <span class="dept-name">全部科室</span>
          <span class="dept-count">{{ depts.length }}</span>
        </div>
        <div
          v-for="d in depts"
          :key="d.id"
          class="dept-item"
          :class="{ active: selectedDeptId === d.id, blocked: d.status !== '开放' }"
          @click="selectDept(d.id)"
        >
          <el-tooltip :content="`${d.deptNo} · ${d.location || '待定位置'} · ${d.doctor || '待安排医师'}`" placement="right">
            <div class="dept-main">
              <span class="dept-name">{{ d.deptName }}</span>
              <span class="dept-count">{{ examCountByDept[String(d.id)] || 0 }}</span>
            </div>
          </el-tooltip>
          <el-tag size="small" :type="d.status === '开放' ? 'success' : (d.status === '装修' ? 'warning' : 'danger')">
            {{ d.status }}
          </el-tag>
        </div>
      </div>

      <div class="hint">停诊 / 装修科室不可排检查项</div>
    </aside>

    <main class="content">
      <div class="chip-bar">
        <span class="chip-label">体检套餐</span>
        <div
          class="chip all"
          :class="{ active: selectedPackageId === null }"
          @click="selectedPackageId = null"
        >全部套餐</div>
        <div
          v-for="p in packages"
          :key="p.id"
          class="chip"
          :class="{ active: selectedPackageId === p.id, blocked: p.status !== '在售' }"
          :title="p.status !== '在售' ? `${p.packageName} 已${p.status}，不可预约` : `${p.suitable} · ${p.itemCount} 项`"
          @click="selectPackage(p.id)"
        >
          {{ p.packageName }}
          <span class="chip-sub">￥{{ p.price }}</span>
        </div>
      </div>

      <component
        :is="currentView"
        :dept-id="selectedDeptId"
        :package-id="selectedPackageId"
        :depts="depts"
        :available-depts="availableDepts"
        :packages="packages"
        @changed="reloadAll"
      />
    </main>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; display: flex; }

.sidebar {
  width: 232px; flex-shrink: 0;
  background: #fff; border-right: 1px solid var(--line);
  display: flex; flex-direction: column;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 18px; border-bottom: 1px solid var(--line);
}
.brand-mark {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary); color: #fff; font-weight: 700; font-size: 18px;
}
.brand-title { font-size: 15px; font-weight: 600; }
.brand-sub { font-size: 12px; color: var(--muted); }

.nav-menu { border-right: none; }

.side-title {
  padding: 10px 18px 6px; font-size: 12px; color: var(--muted);
  border-top: 1px solid var(--line);
}
.dept-list { flex: 1; overflow: auto; padding: 0 10px; }
.dept-item {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 7px 10px; margin-bottom: 4px; border-radius: 8px; cursor: pointer;
  border: 1px solid transparent;
}
.dept-item:hover { background: var(--primary-wash); }
.dept-item.active { background: var(--primary-wash); border-color: var(--primary); }
.dept-item.blocked { opacity: .62; }
.dept-main { display: flex; align-items: center; gap: 6px; width: 100%; }
.dept-name { font-size: 13px; font-weight: 600; }
.dept-count {
  font-size: 12px; color: var(--muted); background: var(--primary-wash);
  border-radius: 9px; padding: 0 6px; margin-left: auto;
}
.hint { padding: 12px 18px; font-size: 12px; color: var(--muted); border-top: 1px solid var(--line); }

.content { flex: 1; padding: 16px 24px 28px; overflow: auto; }

.chip-bar {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  padding: 10px 12px; margin-bottom: 16px;
  background: #fff; border: 1px solid var(--line); border-radius: 10px;
}
.chip-label { font-size: 12px; color: var(--muted); margin-right: 2px; }
.chip {
  padding: 5px 12px; border-radius: 16px; cursor: pointer; font-size: 13px;
  border: 1px solid var(--line); background: var(--primary-wash); color: var(--ink);
  display: flex; align-items: center; gap: 6px;
}
.chip:hover { border-color: var(--primary); }
.chip.active { background: var(--primary); border-color: var(--primary); color: #fff; }
.chip.blocked { opacity: .55; text-decoration: line-through; }
.chip-sub { font-size: 12px; opacity: .8; }
</style>
