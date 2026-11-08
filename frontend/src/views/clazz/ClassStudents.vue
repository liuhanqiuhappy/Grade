<template>
  <div>
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h4>班级学生管理（{{ className }}）</h4>
      <div>
        <button class="btn btn-outline-success me-2" @click="fileInput.click()">Excel 批量导入</button>
        <input ref="fileInput" type="file" accept=".xlsx" hidden @change="onImport" />
        <button class="btn btn-primary" @click="openCreate">新增学生</button>
      </div>
    </div>
    <p class="text-muted small">Excel 模板：第一列学号、第二列姓名、第三列性别；第一行为表头将被跳过。</p>
    <div class="input-group mb-3" style="max-width: 360px">
      <input v-model.trim="keyword" class="form-control" placeholder="按学号/姓名搜索" @keyup.enter="load(1)" />
      <button class="btn btn-outline-secondary" @click="load(1)">搜索</button>
    </div>
    <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>
    <div v-if="importResult" class="alert alert-info py-2">
      导入完成：成功 {{ importResult.successCount }} 条
      <span v-if="importResult.failures.length">
        ，失败 {{ importResult.failures.length }} 条
        （{{ importResult.failures.map(f => `第${f.row}行 ${f.reason}`).join('；') }}）
      </span>
    </div>
    <table class="table table-bordered align-middle">
      <thead><tr><th>ID</th><th>学号</th><th>姓名</th><th>性别</th><th style="width: 160px">操作</th></tr></thead>
      <tbody>
        <tr v-for="item in rows" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.studentNo }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.gender }}</td>
          <td>
            <button class="btn btn-sm btn-outline-primary me-1" @click="openEdit(item)">编辑</button>
            <button class="btn btn-sm btn-outline-danger" @click="remove(item)">删除</button>
          </td>
        </tr>
        <tr v-if="rows.length === 0"><td colspan="5" class="text-center text-muted">暂无学生</td></tr>
      </tbody>
    </table>
    <nav v-if="total > size">
      <ul class="pagination pagination-sm">
        <li v-for="p in pageCount" :key="p" class="page-item" :class="{ active: p === page }">
          <a class="page-link" href="#" @click.prevent="load(p)">{{ p }}</a>
        </li>
      </ul>
    </nav>

    <div v-if="editing" class="modal d-block" style="background: rgba(0,0,0,.4)" @click.self="editing = null">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content p-3">
          <h5>{{ editing.id ? '编辑学生' : '新增学生' }}</h5>
          <input v-model.trim="editing.studentNo" class="form-control my-2" placeholder="学号" />
          <input v-model.trim="editing.name" class="form-control my-2" placeholder="姓名" />
          <input v-model.trim="editing.gender" class="form-control my-2" placeholder="性别（男/女，可空）" />
          <div class="text-end">
            <button class="btn btn-secondary me-2" @click="editing = null">取消</button>
            <button class="btn btn-primary" @click="save">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  createStudent, deleteStudent, importStudents, listStudentsOfClass, updateStudent
} from '../../api/student'

const route = useRoute()
const classId = Number(route.params.id)
const className = route.query.name || `#${classId}`

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const error = ref('')
const editing = ref(null)
const importResult = ref(null)
const fileInput = ref(null)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function load(p = 1) {
  error.value = ''
  try {
    const data = await listStudentsOfClass(classId, { page: p, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records
    total.value = data.total
    page.value = data.page
  } catch (e) {
    error.value = e.message
  }
}

function openCreate() {
  editing.value = { id: null, studentNo: '', name: '', gender: '' }
}

function openEdit(item) {
  editing.value = { ...item }
}

async function save() {
  error.value = ''
  try {
    if (editing.value.id) {
      await updateStudent(editing.value.id, { ...editing.value, classId })
    } else {
      await createStudent(classId, {
        studentNo: editing.value.studentNo, name: editing.value.name, gender: editing.value.gender
      })
    }
    editing.value = null
    await load(page.value)
  } catch (e) {
    error.value = e.message
  }
}

async function remove(item) {
  if (!confirm(`确认删除学生「${item.name}」？`)) return
  error.value = ''
  try {
    await deleteStudent(item.id)
    await load(page.value)
  } catch (e) {
    error.value = e.message
  }
}

async function onImport(ev) {
  const file = ev.target.files[0]
  ev.target.value = ''
  if (!file) return
  error.value = ''
  importResult.value = null
  const fd = new FormData()
  fd.append('file', file)
  try {
    importResult.value = await importStudents(classId, fd)
    await load(1)
  } catch (e) {
    error.value = e.message
  }
}

onMounted(() => load())
</script>
