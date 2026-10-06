<script setup>
import { computed, onMounted, ref } from 'vue'
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

const DEPT_STATUS = ['开放', '停诊', '装修']

const list = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({ deptNo: '', deptName: '', location: '', doctor: '', status: '开放' })

const visible = computed(() => {
  if (props.deptId === null || props.deptId === undefined) return list.value
  return list.value.filter(d => d.id === props.deptId)
})

async function load() {
  list.value = await api.listDepts()
}

function openCreate() {
  editing.value = null
  form.value = { deptNo: '', deptName: '', location: '', doctor: '', status: '开放' }
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    deptNo: r.deptNo, deptName: r.deptName, location: r.location || '',
    doctor: r.doctor || '', status: r.status
  }
  dialog.value = true
}

async function save() {
  try {
    if (editing.value) {
      await api.updateDept(editing.value, form.value)
      ElMessage.success('科室已更新')
    } else {
      await api.createDept(form.value)
      ElMessage.success('科室已创建')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function transfer(r, status) {
  if (r.status === status) return
  try {
    await api.transferDept(r.id, status)
    ElMessage.success(`科室已置为${status}`)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const statusType = (s) => (s === '开放' ? 'success' : (s === '装修' ? 'warning' : 'danger'))

onMounted(load)
</script>

<template>
  <div>
    <div class="bar">
      <h2>科室管理</h2>
      <el-button type="primary" @click="openCreate">新增科室</el-button>
    </div>

    <el-alert
      v-if="props.deptId"
      class="tip"
      type="info"
      show-icon
      :closable="false"
      title="已按左侧选中的科室过滤，取消左侧选中即可查看全部科室"
    />

    <div class="grid">
      <el-card v-for="r in visible" :key="r.id" class="card" shadow="hover">
        <template #header>
          <span class="code">{{ r.deptNo }}</span>
          <el-tag size="small" :type="statusType(r.status)">{{ r.status }}</el-tag>
        </template>
        <div class="row"><span>科室名</span><b>{{ r.deptName }}</b></div>
        <div class="row"><span>位置</span><b>{{ r.location || '待定位置' }}</b></div>
        <div class="row"><span>负责医师</span><b>{{ r.doctor || '待安排医师' }}</b></div>
        <div class="row" v-if="r.status !== '开放'">
          <el-tag size="small" type="danger">{{ r.status }}科室不可排检查项</el-tag>
        </div>
        <div class="row" v-if="r.status === '装修'">
          <el-tag size="small" type="warning">装修完成后只能恢复为开放</el-tag>
        </div>
        <div class="actions">
          <el-button link type="primary" @click="openEdit(r)">编辑</el-button>
          <el-button
            v-for="s in DEPT_STATUS"
            :key="s"
            link
            :type="s === '开放' ? 'success' : (s === '装修' ? 'warning' : 'danger')"
            :disabled="r.status === s"
            @click="transfer(r, s)"
          >{{ s }}</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑科室' : '新增科室'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="科室编号"><el-input v-model="form.deptNo" placeholder="如 KS008" /></el-form-item>
        <el-form-item label="科室名"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" placeholder="可空，默认待定位置" /></el-form-item>
        <el-form-item label="负责医师"><el-input v-model="form.doctor" placeholder="可空，默认待安排医师" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="s in DEPT_STATUS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.bar h2 { margin: 0; font-size: 18px; }
.tip { margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; }
.card { position: relative; }
.card :deep(.el-card__body) { padding-bottom: 46px; }
.code { font-weight: 600; margin-right: 8px; font-family: ui-monospace, Menlo, monospace; }
.row { display: flex; justify-content: space-between; padding: 4px 0; color: var(--muted); }
.row b { color: var(--ink); font-weight: 600; }
.actions {
  position: absolute; left: 0; right: 0; bottom: 0;
  display: flex; gap: 2px; padding: 8px 14px; flex-wrap: wrap;
  background: var(--primary-wash); opacity: 0; transition: opacity .2s;
}
.card:hover .actions { opacity: 1; }
</style>
