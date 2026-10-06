<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['changed'])

const props = defineProps({
  deptId: { type: Number, default: null },
  packageId: { type: Number, default: null },
  depts: { type: Array, default: () => [] },
  availableDepts: { type: Array, default: () => [] },
  packages: { type: Array, default: () => [] }
})

const GENDERS = ['男', '女']
const EXAM_ITEMS = ['身高体重', '血压', '血常规', '尿常规', '心电图', '胸片', 'B超', '肝功能']
const ITEM_DEPT_KEYWORD = {
  '身高体重': '内科', '血压': '内科',
  '血常规': '检验', '尿常规': '检验', '肝功能': '检验',
  '心电图': '心电', '胸片': '影像', 'B超': '影像'
}

const list = ref([])
const pkgList = ref([])
const deptList = ref([])
const activePkgId = ref(null)

const dialog = ref(false)
const editing = ref(null)
const form = ref({ name: '', phone: '', idCard: '', gender: '女', age: 30, company: '', packageId: null })

const itemDrawer = ref(false)
const itemTarget = ref(null)
const scheduled = ref([])

async function loadAll() {
  const [es, ps, ds] = await Promise.all([api.listExaminees(), api.listPackages(), api.listDepts()])
  list.value = es
  pkgList.value = ps
  deptList.value = ds
}

const visible = computed(() => {
  if (activePkgId.value === null || activePkgId.value === undefined) return list.value
  return list.value.filter(e => e.packageId === activePkgId.value)
})

/** 按套餐项目数推导出该套餐默认包含的检查项，用于勾选卡预览。 */
const previewItems = computed(() => {
  if (activePkgId.value === null || activePkgId.value === undefined) return []
  const p = pkgList.value.find(x => x.id === activePkgId.value)
  if (!p) return []
  return EXAM_ITEMS.slice(0, Math.min(p.itemCount, EXAM_ITEMS.length))
})

const selectedPkg = computed(() => pkgList.value.find(p => p.id === form.value.packageId) || null)

const dialogPreviewItems = computed(() => {
  if (!selectedPkg.value) return []
  return EXAM_ITEMS.slice(0, Math.min(selectedPkg.value.itemCount, EXAM_ITEMS.length))
})

const isBookable = (p) => p.status === '在售'

function pickPackage(p) {
  if (!isBookable(p)) {
    ElMessage.error(`套餐「${p.packageName}」当前为${p.status}，不可被预约`)
    return
  }
  activePkgId.value = (activePkgId.value === p.id) ? null : p.id
}

function pickDialogPackage(p) {
  if (!isBookable(p)) {
    ElMessage.error(`套餐「${p.packageName}」当前为${p.status}，不可被预约`)
    return
  }
  form.value.packageId = p.id
}

function openCreate() {
  editing.value = null
  form.value = {
    name: '', phone: '', idCard: '', gender: '女', age: 30, company: '',
    packageId: activePkgId.value || (pkgList.value[0] && pkgList.value[0].id) || null
  }
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    name: r.name, phone: r.phone, idCard: r.idCard, gender: r.gender,
    age: r.age, company: r.company || '', packageId: r.packageId
  }
  dialog.value = true
}

async function save() {
  try {
    const payload = { ...form.value, age: Number(form.value.age) }
    if (editing.value) {
      await api.updateExaminee(editing.value, payload)
      ElMessage.success('体检人已更新')
    } else {
      await api.createExaminee(payload)
      ElMessage.success('预约成功')
    }
    dialog.value = false
    await loadAll()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function startExam(r) {
  try {
    await api.startExaminee(r.id)
    ElMessage.success(`${r.name} 已开始体检`)
    await loadAll()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function cancelExam(r) {
  try {
    await api.transferExaminee(r.id, '已取消')
    ElMessage.success(`${r.name} 已取消预约`)
    await loadAll()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

function deptForItem(name) {
  const open = deptList.value.filter(d => d.status === '开放')
  const kw = ITEM_DEPT_KEYWORD[name] || ''
  return open.find(d => (d.deptName || '').includes(kw)) || open[0] || null
}

async function openItems(r) {
  itemTarget.value = r
  scheduled.value = await api.listExams({ examineeId: r.id })
  itemDrawer.value = true
}

const itemState = (name) => scheduled.value.find(e => e.itemName === name) || null

async function toggleItem(name) {
  const row = itemTarget.value
  if (row.status !== '体检中') {
    ElMessage.error(`体检人「${row.name}」当前为${row.status}，不可做检查`)
    return
  }
  const existing = itemState(name)
  try {
    if (existing) {
      await api.deleteExam(existing.id)
      ElMessage.success(`已撤销待检项「${name}」`)
    } else {
      const dept = deptForItem(name)
      if (!dept) {
        ElMessage.error('暂无开放状态的科室，无法排检')
        return
      }
      await api.createExam({ examineeId: row.id, deptId: dept.id, itemName: name })
      ElMessage.success(`已排检项「${name}」，科室：${dept.deptName}`)
    }
    scheduled.value = await api.listExams({ examineeId: row.id })
    itemTarget.value = (await api.listExaminees()).find(e => e.id === row.id)
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const statusType = (s) => (s === '已预约' ? 'info' : (s === '体检中' ? 'primary' : (s === '已完成' ? 'success' : 'danger')))
const isTerminal = (s) => s === '已完成' || s === '已取消'

watch(() => props.packageId, (v) => { activePkgId.value = v })

onMounted(async () => {
  await loadAll()
  activePkgId.value = props.packageId
})
</script>

<template>
  <div>
    <div class="bar">
      <h2>体检人预约</h2>
      <el-button type="primary" @click="openCreate">新增体检人</el-button>
    </div>

    <div class="pkg-title">
      <span class="pkg-label">体检套餐勾选卡</span>
      <span class="pkg-sub">勾选卡片选择套餐以此过滤体检人；停售 / 下架套餐不可勾选</span>
    </div>
    <div class="pkg-grid">
      <div
        v-for="p in pkgList"
        :key="p.id"
        class="pkg-card"
        :class="{ active: activePkgId === p.id, disabled: !isBookable(p) }"
        @click="pickPackage(p)"
      >
        <div class="pkg-head">
          <el-checkbox :model-value="activePkgId === p.id" :disabled="!isBookable(p)" @click.stop="pickPackage(p)">
            {{ p.packageName }}
          </el-checkbox>
          <el-tag size="small" :type="isBookable(p) ? 'success' : (p.status === '停售' ? 'warning' : 'info')">
            {{ p.status }}
          </el-tag>
        </div>
        <div class="pkg-body">
          <span class="price">￥{{ p.price }}</span>
          <span class="meta">{{ p.itemCount }} 项 · {{ p.suitable }}</span>
        </div>
        <div v-if="!isBookable(p)" class="pkg-warn">{{ p.status }}套餐不可被预约</div>
      </div>
    </div>

    <div v-if="previewItems.length" class="item-preview">
      <span class="pkg-label">该套餐包含检查项</span>
      <el-tag v-for="i in previewItems" :key="i" size="small" effect="plain">{{ i }}</el-tag>
    </div>

    <el-table :data="visible" border stripe class="table">
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="age" label="年龄" width="70" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="company" label="单位" min-width="150">
        <template #default="{ row }">{{ row.company || '个人体检' }}</template>
      </el-table-column>
      <el-table-column label="体检套餐" min-width="140">
        <template #default="{ row }">
          {{ (pkgList.find(p => p.id === row.packageId) || {}).packageName || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="isTerminal(row.status)" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" :disabled="row.status !== '已预约'" @click="startExam(row)">开始体检</el-button>
          <el-button link type="info" @click="openItems(row)">检查项</el-button>
          <el-button link type="danger" :disabled="isTerminal(row.status)" @click="cancelExam(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑体检人' : '新增体检人'" width="640px">
      <div class="pick-title">体检套餐勾选卡（单选，只能勾选在售套餐）</div>
      <div class="pkg-grid small">
        <div
          v-for="p in pkgList"
          :key="p.id"
          class="pkg-card"
          :class="{ active: form.packageId === p.id, disabled: !isBookable(p) }"
          @click="pickDialogPackage(p)"
        >
          <div class="pkg-head">
            <el-checkbox :model-value="form.packageId === p.id" :disabled="!isBookable(p)" @click.stop="pickDialogPackage(p)">
              {{ p.packageName }}
            </el-checkbox>
            <el-tag size="small" :type="isBookable(p) ? 'success' : 'warning'">{{ p.status }}</el-tag>
          </div>
          <div class="pkg-body">
            <span class="price">￥{{ p.price }}</span>
            <span class="meta">{{ p.itemCount }} 项</span>
          </div>
        </div>
      </div>
      <div v-if="dialogPreviewItems.length" class="item-preview">
        <span class="pkg-label">待勾选检查项</span>
        <el-tag v-for="i in dialogPreviewItems" :key="i" size="small" effect="plain">{{ i }}</el-tag>
      </div>

      <el-form :model="form" label-width="80px" class="form">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="证件号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio v-for="g in GENDERS" :key="g" :value="g">{{ g }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="form.age" :min="0" :max="120" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.company" placeholder="可空，个人体检留空" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="itemDrawer" :title="`检查项勾选 · ${itemTarget?.name || ''}`" size="520px">
      <el-alert
        v-if="itemTarget && itemTarget.status !== '体检中'"
        class="tip"
        type="warning"
        show-icon
        :closable="false"
        :title="`该体检人当前为「${itemTarget.status}」，只有体检中的体检人可以做检查`"
      />
      <div class="item-grid">
        <div
          v-for="i in EXAM_ITEMS"
          :key="i"
          class="item-card"
          :class="{ checked: !!itemState(i), locked: itemState(i) && itemState(i).status !== '待检' }"
          @click="toggleItem(i)"
        >
          <div class="item-head">
            <el-checkbox :model-value="!!itemState(i)" @click.stop="toggleItem(i)">{{ i }}</el-checkbox>
            <el-tag v-if="itemState(i)" size="small" :type="itemState(i).status === '已完成' ? 'success' : 'primary'">
              {{ itemState(i).status }}
            </el-tag>
          </div>
          <div class="item-meta">
            {{ itemState(i) ? (deptList.find(d => d.id === itemState(i).deptId) || {}).deptName || '—' : '未排检' }}
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }

.pkg-title { display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px; }
.pkg-label { font-size: 13px; font-weight: 600; }
.pkg-sub { font-size: 12px; color: var(--muted); }

.pkg-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 14px; }
.pkg-grid.small { grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); margin-bottom: 12px; }
.pkg-card {
  background: #fff; border: 1px solid var(--line); border-radius: 10px;
  padding: 10px 12px; cursor: pointer; transition: all .15s;
}
.pkg-card:hover { border-color: var(--primary-soft); }
.pkg-card.active { border-color: var(--primary); background: var(--primary-wash); }
.pkg-card.disabled { opacity: .55; cursor: not-allowed; }
.pkg-head { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.pkg-body { display: flex; align-items: baseline; gap: 8px; margin-top: 6px; }
.price { color: var(--primary-deep); font-weight: 700; font-size: 15px; }
.meta { font-size: 12px; color: var(--muted); }
.pkg-warn { margin-top: 6px; font-size: 12px; color: #E57373; }

.item-preview { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 14px; }

.table { background: #fff; border-radius: 10px; overflow: hidden; }

.pick-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.form { margin-top: 8px; }

.tip { margin-bottom: 12px; }
.item-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.item-card {
  border: 1px solid var(--line); border-radius: 10px; padding: 10px 12px;
  background: #fff; cursor: pointer;
}
.item-card:hover { border-color: var(--primary); }
.item-card.checked { border-color: var(--primary); background: var(--primary-wash); }
.item-card.locked { cursor: not-allowed; opacity: .8; }
.item-head { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.item-meta { margin-top: 6px; font-size: 12px; color: var(--muted); }
</style>
