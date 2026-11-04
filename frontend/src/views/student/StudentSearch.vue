<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <h5 class="mb-0 me-auto">学生查询</h5>
        <input v-model.trim="query.keyword" class="form-control form-control-sm" style="width: 200px;"
               placeholder="按学号或姓名搜索" @keyup.enter="onSearch" />
        <select v-model="query.classId" class="form-select form-select-sm" style="width: 180px;" @change="reload(1)">
          <option value="">全部班级</option>
          <option v-for="c in classOptions" :key="c.classId" :value="c.classId">{{ c.className }}</option>
        </select>
        <select v-model.number="query.status" class="form-select form-select-sm" style="width: 120px;" @change="reload(1)">
          <option :value="1">在读</option>
          <option :value="0">停用</option>
        </select>
        <button class="btn btn-primary btn-sm" @click="onSearch">查询</button>
        <button class="btn btn-outline-secondary btn-sm" @click="onReset">重置</button>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>学号</th>
              <th>姓名</th>
              <th>登录名</th>
              <th>班级</th>
              <th>状态</th>
              <th>建档时间</th>
              <th style="width: 90px;">操作</th>
            </tr>
          </thead>
          <tbody v-if="list.length > 0">
            <tr v-for="s in list" :key="s.studentId">
              <td>{{ s.studentNo }}</td>
              <td>{{ s.name }}</td>
              <td><code>{{ s.username }}</code></td>
              <td>
                {{ s.className || '—' }}
                <span v-if="s.classStatus === 0" class="badge bg-secondary ms-1">已归档</span>
              </td>
              <td>
                <span class="badge" :class="s.status === 1 ? 'bg-success' : 'bg-secondary'">
                  {{ s.status === 1 ? '在读' : '停用' }}
                </span>
              </td>
              <td>{{ s.createdAt }}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary" @click="goDetail(s.studentId)">详情</button>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="7" class="text-center text-muted py-4">
                {{ loading ? '加载中…' : '暂无学生' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="d-flex justify-content-between align-items-center mt-2">
        <div class="text-muted small">共 {{ total }} 条 · 第 {{ pager.page }} / {{ totalPages }} 页</div>
        <div class="d-flex align-items-center gap-2">
          <select class="form-select form-select-sm" style="width: auto;" v-model.number="pager.size" @change="reload(1)">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: pager.page <= 1 }">
                <button class="page-link" @click="reload(pager.page - 1)">上一页</button>
              </li>
              <li class="page-item" :class="{ disabled: pager.page >= totalPages }">
                <button class="page-link" @click="reload(pager.page + 1)">下一页</button>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchStudents } from '@/api/student'
import { listClasses } from '@/api/class'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageError = ref('')
const classOptions = ref([])

const query = reactive({ keyword: '', classId: '', status: 1 })
const pager = reactive({ page: 1, size: 10 })
const totalPages = computed(() => (pager.size > 0 ? Math.max(1, Math.ceil(total.value / pager.size)) : 1))

async function fetchStudents(page = pager.page) {
  loading.value = true
  pageError.value = ''
  const params = { page, size: pager.size, status: query.status }
  if (query.keyword) params.keyword = query.keyword
  if (query.classId !== '' && query.classId !== null) params.classId = Number(query.classId)
  try {
    const res = await searchStudents(params)
    const data = res.data
    list.value = data.list || []
    total.value = data.total || 0
    pager.page = data.page || page
    pager.size = data.size || pager.size
  } catch (e) {
    if (e?.code === 1003) pageError.value = '无权访问该功能'
    else if (e?.code !== 1002) pageError.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadClassOptions() {
  try {
    const res = await listClasses({ page: 1, size: 100, status: 1 })
    classOptions.value = res.data.list || []
  } catch (e) { /* 下拉加载失败不阻塞主列表 */ }
}

function reload(page) { if (page >= 1) fetchStudents(page) }
function onSearch() { reload(1) }
function onReset() {
  query.keyword = ''
  query.classId = ''
  query.status = 1
  reload(1)
}
function goDetail(studentId) { router.push(`/app/students/${studentId}`) }

onMounted(() => {
  loadClassOptions()
  fetchStudents(1)
})
</script>
