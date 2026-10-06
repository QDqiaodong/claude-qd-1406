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

const SUITABLE = ['入职体检', '年度体检', '老年体检', '女性专项', '儿童体检']
const PKG_STATUS = ['在售', '停售', '下架']

const list = ref([])
const dialog = ref(false)
const editing = ref(null)
const form = ref({ packageName: '', price: 0, itemCount: 1, suitable: '入职体检', status: '在售' })

const visible = computed(() => {
  if (props.packageId === null || props.packageId === undefined) return list.value
  return list.value.filter(p => p.id === props.packageId)
})

async function load() {
  list.value = await api.listPackages()
}

function openCreate() {
  editing.value = null
  form.value = { packageName: '', price: 0, itemCount: 1, suitable: '入职体检', status: '在售' }
  dialog.value = true
}

function openEdit(r) {
  editing.value = r.id
  form.value = {
    packageName: r.packageName, price: Number(r.price), itemCount: r.itemCount,
    suitable: r.suitable, status: r.status
  }
  dialog.value = true
}

async function save() {
  try {
    const payload = { ...form.value, price: Number(form.value.price), itemCount: Number(form.value.itemCount) }
    if (editing.value) {
      await api.updatePackage(editing.value, payload)
      ElMessage.success('体检套餐已更新')
    } else {
      await api.createPackage(payload)
      ElMessage.success('体检套餐已创建')
    }
    dialog.value = false
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

async function transfer(r, status) {
  if (r.status === status) return
  try {
    await api.transferPackage(r.id, status)
    ElMessage.success(`套餐已置为${status}`)
    await load()
    emit('changed')
  } catch (e) { ElMessage.error(e.message) }
}

const statusType = (s) => (s === '在售' ? 'success' : (s === '停售' ? 'warning' : 'info'))
</script>

<template>
  <div>
    <div class="bar">
      <h2>体检套餐</h2>
      <el-button type="primary" @click="openCreate">新增套餐</el-button>
    </div>

    <el-alert
      v-if="props.packageId"
      class="tip"
      type="info"
      show-icon
      :closable="false"
      title="已按顶部选中的套餐 chip 过滤，取消 chip 选中即可查看全部套餐"
    />

    <el-table :data="visible" border stripe class="table">
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column prop="packageName" label="套餐名" min-width="150" />
      <el-table-column label="价格" width="120">
        <template #default="{ row }"><b class="price">￥{{ row.price }}</b></template>
      </el-table-column>
      <el-table-column prop="itemCount" label="项目数" width="90" />
      <el-table-column prop="suitable" label="适用人群" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预约提示" min-width="150">
        <template #default="{ row }">
          <span v-if="row.status === '在售'" class="ok">可被预约</span>
          <span v-else class="no">{{ row.status === '下架' ? '终态 · 不可预约且不可修改' : '不可被预约' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.status === '下架'" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-for="s in PKG_STATUS"
            :key="s"
            link
            :type="s === '在售' ? 'success' : (s === '停售' ? 'warning' : 'danger')"
            :disabled="row.status === s || row.status === '下架'"
            @click="transfer(row, s)"
          >{{ s }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑体检套餐' : '新增体检套餐'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="套餐名"><el-input v-model="form.packageName" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="项目数"><el-input-number v-model="form.itemCount" :min="0" /></el-form-item>
        <el-form-item label="适用人群">
          <el-select v-model="form.suitable">
            <el-option v-for="s in SUITABLE" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="s in PKG_STATUS" :key="s" :label="s" :value="s" />
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
.table { background: #fff; border-radius: 10px; overflow: hidden; }
.price { color: var(--primary-deep); font-weight: 700; }
.ok { color: #4CAF50; }
.no { color: var(--muted); }
</style>
