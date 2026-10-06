<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const emit = defineEmits(['changed'])

const props = defineProps({
  deptId: { type: Number, default: null },
  packageId: { type: Number, default: null },
  depts: { type: Array, default: () => [] },
  availableDepts: { type: Array, default: () => [] },
  packages: { type: Array, default: () => [] }
})

const EXAM_ITEMS = ['身高体重', '血压', '血常规', '尿常规', '心电图', '胸片', 'B超', '肝功能']

const examinees = ref([])
const examineeId = ref(null)
const rows = ref([])
const progress = ref({ total: 0, done: 0, checking: 0, waiting: 0, skipped: 0, abnormal: 0, percent: 0 })

const addDialog = ref(false)
const addForm = ref({ examineeId: null, itemName: '身高体重', deptId: null })

const current = computed(() => examinees.value.find(e => e.id === examineeId.value) || null)

const percent = computed(() => progress.value.percent || 0)

async function loadExaminees() {
  examinees.value = await api.listExaminees(props.packageId)
  if (examineeId.value && !examinees.value.some(e => e.id === examineeId.value)) {
    examineeId.value = examinees.value.length ? examinees.value[0].id : null
  }
  if (!examineeId.value && examinees.value.length) {
    examineeId.value = examinees.value[0].id
  }
}

async function loadRows() {
  if (examineeId.value === null) {
    rows.value = []
    progress.value = { total: 0, done: 0, checking: 0, waiting: 0, skipped: 0, abnormal: 0, percent: 0 }
    return
  }
  const all = await api.listExams({ examineeId: examineeId.value })
  rows.value = (props.deptId === null || props.deptId === undefined)
    ? all
    : all.filter(r => r.deptId === props.deptId)
  progress.value = await api.examProgress(examineeId.value)
}

async function reload() {
  await Promise.all([loadExaminees(), loadRows()])
}

function deptName(id) {
  const d = (props.depts || []).find(x => x.id === id)
  return d ? d.deptName : '—'
}

function openAdd() {
  if (!current.value) {
    ElMessage.error('请先选择体检人')
    return
  }
  const open = props.availableDepts || []
  if (!open.length) {
    ElMessage.error('暂无开放状态的科室，无法排检')
    return
  }
  addForm.value = {
    examineeId: current.value.id,
    itemName: '身高体重',
    deptId: open[0].id
  }
  addDialog.value = true
}

async function doAdd() {
  try {
    await api.createExam(addForm.value)
    ElMessage.success('检查项已排好')
    addDialog.value = false
    await reload()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function change(row, status, options = {}) {
  try {
    let result = options.result === undefined ? row.result : options.result
    let abnormal = options.abnormal === undefined ? row.abnormal : options.abnormal

    if (options.requireResult) {
      const { value } = await ElMessageBox.prompt(options.tip, options.title, {
        inputValue: row.result || '',
        inputType: 'textarea',
        inputPlaceholder: options.placeholder,
        inputValidator: (v) => (v && String(v).trim().length > 0) || options.validatorTip
      })
      result = value
    }

    await api.transferExam(row.id, status, result, Boolean(abnormal))
    ElMessage.success(`检查项已置为${status}`)
    await reload()
    emit('changed')
  } catch (e) {
    if (e && e.message && e.message !== 'cancel') ElMessage.error(e.message)
  }
}

async function markDone(row) {
  let abnormal = false
  try {
    await ElMessageBox.confirm('该项检查结果是否异常？', '结果判定', {
      distinguishCancelAndClose: true,
      confirmButtonText: '结果异常',
      cancelButtonText: '结果正常'
    })
    abnormal = true
  } catch (e) {
    abnormal = (e === 'cancel') ? false : null
  }
  if (abnormal === null) return
  await change(row, '已完成', {
    requireResult: true,
    abnormal,
    title: '标记已完成',
    tip: abnormal ? '请填写异常描述（必填）' : '请填写检查结果（必填）',
    placeholder: abnormal ? '如：血压 148/95mmHg 偏高，建议复查' : '如：各项指标正常',
    validatorTip: '结果与异常描述不可为空'
  })
}

function markSkip(row) {
  change(row, '已弃检', {
    requireResult: true,
    abnormal: false,
    title: '标记已弃检',
    tip: '请填写弃检理由（必填）',
    placeholder: '如：受检者主动放弃该项检查',
    validatorTip: '弃检理由不可为空'
  })
}

const statusType = (s) => (s === '已完成' ? 'success' : (s === '检查中' ? 'primary' : (s === '已弃检' ? 'info' : 'warning')))
const isTerminal = (s) => s === '已完成' || s === '已弃检'

watch(() => props.packageId, async () => { await reload() })
watch(() => props.deptId, async () => { await loadRows() })
watch(examineeId, async () => { await loadRows() })

onMounted(async () => { await reload() })
</script>

<template>
  <div>
    <div class="bar">
      <h2>检查项记录</h2>
      <el-button type="primary" @click="openAdd">排检查项</el-button>
    </div>

    <div class="pick">
      <span class="pick-label">体检人</span>
      <el-select v-model="examineeId" placeholder="请选择体检人" style="width: 280px">
        <el-option
          v-for="e in examinees"
          :key="e.id"
          :label="`${e.name} · ${e.gender} · ${e.age}岁 · ${e.status}`"
          :value="e.id"
        />
      </el-select>
      <el-tag v-if="current" :type="current.status === '体检中' ? 'primary' : 'info'" size="small">
        {{ current.status }}
      </el-tag>
      <el-tag size="small" effect="plain">
        {{ (packages.find(p => p.id === current?.packageId) || {}).packageName || '无套餐' }}
      </el-tag>
    </div>

    <el-card class="panel" shadow="never">
      <div class="prog-head">
        <span class="prog-title">检查进度</span>
        <span class="prog-num">{{ progress.done }} / {{ progress.total }}</span>
      </div>
      <el-progress :percentage="percent" :stroke-width="16" text-inside :status="percent === 100 ? 'success' : undefined" />
      <div class="stats">
        <el-tag type="success" size="small" effect="plain">已完成 {{ progress.done }}</el-tag>
        <el-tag type="primary" size="small" effect="plain">检查中 {{ progress.checking }}</el-tag>
        <el-tag type="warning" size="small" effect="plain">待检 {{ progress.waiting }}</el-tag>
        <el-tag type="info" size="small" effect="plain">已弃检 {{ progress.skipped }}</el-tag>
        <el-tag type="danger" size="small" effect="plain">异常 {{ progress.abnormal }}</el-tag>
      </div>
      <div v-if="current && current.status !== '体检中'" class="warn">
        该体检人当前为「{{ current.status }}」，只有体检中的体检人可以做检查
      </div>
    </el-card>

    <el-table :data="rows" border stripe class="table">
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="itemName" label="检查项" width="110" />
      <el-table-column label="科室" width="130" :formatter="(r) => deptName(r.deptId)" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="result" label="结果描述" min-width="200">
        <template #default="{ row }">{{ row.result || '—' }}</template>
      </el-table-column>
      <el-table-column label="异常" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.abnormal" size="small" type="danger">异常</el-tag>
          <span v-else class="ok">正常</span>
        </template>
      </el-table-column>
      <el-table-column label="检查时间" width="170">
        <template #default="{ row }">{{ row.checkTime ? String(row.checkTime).replace('T', ' ') : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="isTerminal(row.status)" @click="change(row, '检查中')">开始检查</el-button>
          <el-button link type="success" :disabled="isTerminal(row.status)" @click="markDone(row)">完成</el-button>
          <el-button link type="danger" :disabled="isTerminal(row.status)" @click="markSkip(row)">弃检</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="addDialog" title="排检查项" width="420px">
      <el-form :model="addForm" label-width="90px">
        <el-form-item label="检查项">
          <el-select v-model="addForm.itemName">
            <el-option v-for="i in EXAM_ITEMS" :key="i" :label="i" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查科室">
          <el-select v-model="addForm.deptId">
            <el-option v-for="d in availableDepts" :key="d.id" :label="`${d.deptName}（${d.location || ''}）`" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" @click="doAdd">确认排检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }
.pick { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.pick-label { font-size: 13px; font-weight: 600; }

.panel { margin-bottom: 14px; border-radius: 10px; }
.prog-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 8px; }
.prog-title { font-size: 13px; font-weight: 600; }
.prog-num { font-size: 15px; font-weight: 700; color: var(--primary-deep); }
.stats { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.warn { margin-top: 10px; font-size: 12px; color: #E57373; }

.table { background: #fff; border-radius: 10px; overflow: hidden; }
.ok { color: var(--muted); }
</style>
