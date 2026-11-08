<template>
  <div>
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h4>班级管理</h4>
      <button class="btn btn-primary" @click="openCreate">新建班级</button>
    </div>
    <div class="input-group mb-3" style="max-width: 360px">
      <input v-model.trim="keyword" class="form-control" placeholder="按名称搜索" @keyup.enter="load(1)" />
      <button class="btn btn-outline-secondary" @click="load(1)">搜索</button>
    </div>
    <div v-if="error" class="alert alert-danger py-2">{{ error }}</div>
    <table class="table table-bordered align-middle">
      <thead>
        <tr>
          <th>ID</th><th>班级名称</th><th>学生数</th><th style="width: 220px">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.studentCount }}</td>
          <td>
            <button class="btn btn-sm btn-outline-secondary me-1" @click="goStudents(item)">学生</button>
            <button class="btn btn-sm btn-outline-primary me-1" @click="openEdit(item)">改名</button>
            <button class="btn btn-sm btn-outline-danger" @click="remove(item)">删除</button>
          </td>
        </tr>
        <tr v-if="rows.length === 0"><td colspan="4" class="text-center text-muted">暂无班级</td></tr>
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
          <h5>{{ editing.id ? '修改班级' : '新建班级' }}</h5>
          <input v-model.trim="editing.name" class="form-control my-3" placeholder="班级名称" />
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
import { useRouter } from 'vue-router'
import { createClass, deleteClass, listClasses, updateClass } from '../../api/clazz'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const error = ref('')
const editing = ref(null)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function load(p = 1) {
  error.value = ''
  try {
    const data = await listClasses({ page: p, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records
    total.value = data.total
    page.value = data.page
  } catch (e) {
    error.value = e.message
  }
}

function openCreate() {
  editing.value = { id: null, name: '' }
}

function openEdit(item) {
  editing.value = { id: item.id, name: item.name }
}

async function save() {
  error.value = ''
  try {
    if (editing.value.id) {
      await updateClass(editing.value.id, { name: editing.value.name })
    } else {
      await createClass({ name: editing.value.name })
    }
    editing.value = null
    await load(page.value)
  } catch (e) {
    error.value = e.message
  }
}

async function remove(item) {
  error.value = ''
  if (!confirm(`确认删除班级「${item.name}」？`)) return
  try {
    await deleteClass(item.id)
    await load(page.value)
  } catch (e) {
    error.value = e.message
  }
}

function goStudents(item) {
  router.push({ name: 'class-students', params: { id: item.id }, query: { name: item.name } })
}

onMounted(() => load())
</script>
