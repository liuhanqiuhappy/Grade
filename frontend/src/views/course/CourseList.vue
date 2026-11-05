<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <h5 class="mb-0 me-auto">课程管理</h5>

        <input v-model.trim="query.keyword" class="form-control form-control-sm" style="width: 220px;"
               placeholder="按课程代码/名称搜索" @keyup.enter="onSearch" />
        <button class="btn btn-primary btn-sm" @click="onSearch">查询</button>
        <button class="btn btn-outline-secondary btn-sm" @click="onReset">重置</button>
        <button class="btn btn-success btn-sm" @click="openCreate">+ 新建课程</button>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>课程代码</th>
              <th>课程名称</th>
              <th>创建时间</th>
              <th style="width: 200px;">操作</th>
            </tr>
          </thead>
          <tbody v-if="list.length > 0">
            <tr v-for="c in list" :key="c.courseId">
              <td>{{ c.courseCode }}</td>
              <td>{{ c.courseName }}</td>
              <td>{{ c.createdAt }}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary me-1" @click="goMembers(c)">成员</button>
                <button class="btn btn-sm btn-outline-secondary me-1" @click="openEdit(c)">编辑</button>
                <button class="btn btn-sm btn-outline-danger" @click="openArchive(c)">归档</button>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="4" class="text-center text-muted py-4">
                {{ loading ? '加载中…' : '暂无课程' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="d-flex justify-content-between align-items-center mt-2">
        <div class="text-muted small">
          共 {{ total }} 条 · 第 {{ pager.page }} / {{ totalPages }} 页
        </div>
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

  <!-- ===== 新建 / 编辑 课程 Modal ===== -->
  <div v-if="formVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span>{{ formMode === 'create' ? '新建课程' : '编辑课程' }}</span>
        <button class="btn-close btn-close-white" @click="formVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="formError" class="alert alert-danger py-2">{{ formError }}</div>
        <div class="mb-3">
          <label class="form-label">课程代码 <span class="text-danger">*</span></label>
          <input v-model.trim="form.courseCode" class="form-control" placeholder="如：CS101" />
          <div class="form-text">1–32 位字母/数字/下划线/连字符，全局唯一</div>
        </div>
        <div class="mb-3">
          <label class="form-label">课程名称 <span class="text-danger">*</span></label>
          <input v-model.trim="form.courseName" class="form-control" placeholder="如：数据结构" />
          <div class="form-text">1–64 个字符，首尾空格会被去除</div>
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" @click="formVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="formSubmitting" @click="submitForm">
          {{ formSubmitting ? '提交中…' : '保存' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 归档二次确认 Modal ===== -->
  <div v-if="archiveVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-danger text-white">确认归档课程</div>
      <div class="card-body">
        <p class="mb-2">
          确定要归档课程「<strong>{{ archiveTarget?.courseName }}</strong>」（{{ archiveTarget?.courseCode }}）吗？
        </p>
        <p class="text-muted mb-0 small">归档后课程不再出现在列表，相关数据保留。</p>
        <div v-if="archiveError" class="alert alert-danger py-2 mt-2">{{ archiveError }}</div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="archiveSubmitting" @click="archiveVisible = false">取消</button>
        <button class="btn btn-danger" :disabled="archiveSubmitting" @click="doArchive">
          {{ archiveSubmitting ? '归档中…' : '确认归档' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listCourses, createCourse, updateCourse, archiveCourse } from '@/api/course'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageError = ref('')

const query = reactive({ keyword: '' })
const pager = reactive({ page: 1, size: 10 })

const totalPages = computed(() => (pager.size > 0 ? Math.max(1, Math.ceil(total.value / pager.size)) : 1))

async function fetchCourses(page = pager.page) {
  loading.value = true
  pageError.value = ''
  const params = { page, size: pager.size }
  if (query.keyword) params.keyword = query.keyword
  try {
    const res = await listCourses(params)
    list.value = res.data.list || []
    total.value = res.data.total || 0
    pager.page = res.data.page || page
    pager.size = res.data.size || pager.size
  } catch (e) {
    if (e?.code === 1003) {
      pageError.value = '无权访问该功能'
    } else if (e?.code !== 1002) {
      pageError.value = e?.message || '加载失败'
    }
  } finally {
    loading.value = false
  }
}

function reload(page) {
  if (page < 1) return
  fetchCourses(page)
}

function onSearch() { reload(1) }
function onReset() {
  query.keyword = ''
  reload(1)
}
function goMembers(c) {
  router.push({ name: 'course-members', params: { courseId: c.courseId } })
}

// ---------- 新建 / 编辑 ----------
const formVisible = ref(false)
const formMode = ref('create')
const formSubmitting = ref(false)
const formError = ref('')
const form = reactive({ courseCode: '', courseName: '' })
let editCourseId = null

function openCreate() {
  formMode.value = 'create'
  form.courseCode = ''
  form.courseName = ''
  formError.value = ''
  formVisible.value = true
}

function openEdit(c) {
  formMode.value = 'edit'
  form.courseCode = c.courseCode
  form.courseName = c.courseName
  formError.value = ''
  editCourseId = c.courseId
  formVisible.value = true
}

async function submitForm() {
  formError.value = ''
  const code = form.courseCode.trim()
  const name = form.courseName.trim()
  if (!code) { formError.value = '课程代码不能为空'; return }
  if (!/^[A-Za-z0-9_-]{1,32}$/.test(code)) { formError.value = '课程代码须为 1–32 位字母/数字/下划线/连字符'; return }
  if (!name) { formError.value = '课程名称不能为空'; return }
  if (name.length > 64) { formError.value = '课程名称不能超过 64 个字符'; return }

  formSubmitting.value = true
  try {
    if (formMode.value === 'create') {
      await createCourse({ courseCode: code, courseName: name })
    } else {
      await updateCourse(editCourseId, { courseCode: code, courseName: name })
    }
    formVisible.value = false
    reload(1)
  } catch (e) {
    formError.value = e?.message || '保存失败'
  } finally {
    formSubmitting.value = false
  }
}

// ---------- 归档 ----------
const archiveVisible = ref(false)
const archiveTarget = ref(null)
const archiveSubmitting = ref(false)
const archiveError = ref('')

function openArchive(c) {
  archiveTarget.value = c
  archiveError.value = ''
  archiveVisible.value = true
}

async function doArchive() {
  if (!archiveTarget.value) return
  archiveSubmitting.value = true
  archiveError.value = ''
  try {
    await archiveCourse(archiveTarget.value.courseId)
    archiveVisible.value = false
    const afterDelete = list.value.length === 1 && pager.page > 1 ? pager.page - 1 : pager.page
    reload(afterDelete)
  } catch (e) {
    archiveError.value = e?.message || '归档失败'
  } finally {
    archiveSubmitting.value = false
  }
}

onMounted(() => reload(1))
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}
.modal-card {
  width: 420px;
  max-width: 92vw;
}
</style>
