<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <button class="btn btn-outline-secondary btn-sm" @click="goBack">← 返回课程</button>
        <h5 class="mb-0 me-auto">
          {{ courseName }} <span class="text-muted small fw-normal">成员管理</span>
        </h5>
        <input v-model.trim="query.keyword" class="form-control form-control-sm" style="width: 200px;"
               placeholder="按学号搜索" @keyup.enter="onSearch" />
        <button class="btn btn-primary btn-sm" @click="onSearch">查询</button>
        <button class="btn btn-outline-secondary btn-sm" @click="onReset">重置</button>
        <button class="btn btn-success btn-sm" @click="openAddOne">+ 单个加入</button>
        <button class="btn btn-primary btn-sm" @click="openImportClass">按班级批量加入</button>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>学号</th>
              <th>姓名</th>
              <th>班级</th>
              <th>加入时间</th>
              <th style="width: 120px;">操作</th>
            </tr>
          </thead>
          <tbody v-if="list.length > 0">
            <tr v-for="m in list" :key="m.memberId">
              <td>{{ m.studentNo }}</td>
              <td>{{ m.name }}</td>
              <td>{{ m.className || '—' }}</td>
              <td>{{ m.joinedAt }}</td>
              <td>
                <button class="btn btn-sm btn-outline-danger" @click="openRemove(m)">移出</button>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="5" class="text-center text-muted py-4">
                {{ loading ? '加载中…' : '暂无课程成员' }}
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

  <!-- ===== 单个加入 Modal ===== -->
  <div v-if="addVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span>单个加入课程成员</span>
        <button class="btn-close btn-close-white" @click="addVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="addError" class="alert alert-danger py-2">{{ addError }}</div>
        <div class="mb-3">
          <label class="form-label">学生学号 <span class="text-danger">*</span></label>
          <input v-model.trim="addForm.studentNo" class="form-control" placeholder="如：20240001" />
          <div class="form-text">按学号精确匹配，学生须已存在</div>
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" @click="addVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="addSubmitting" @click="submitAdd">
          {{ addSubmitting ? '提交中…' : '加入' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 按班级批量加入 Modal ===== -->
  <div v-if="importVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span>按班级批量加入</span>
        <button class="btn-close btn-close-white" @click="importVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="importError" class="alert alert-danger py-2">{{ importError }}</div>
        <div class="mb-3">
          <label class="form-label">选择班级 <span class="text-danger">*</span></label>
          <select v-model.number="importForm.classId" class="form-select">
            <option :value="0" disabled>请选择正常状态的班级</option>
            <option v-for="c in classes" :key="c.classId" :value="c.classId">
              {{ c.className }}（{{ c.studentCount }} 人）
            </option>
          </select>
          <div class="form-text">将把该班全部在读学生加入课程，已在课程中的学生自动跳过</div>
        </div>
        <div v-if="importResult" class="alert alert-success py-2 mb-0">
          本班在读 {{ importResult.totalCount }} 人：新增 {{ importResult.addedCount }} 人，跳过 {{ importResult.skippedCount }} 人
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" @click="importVisible = false">关闭</button>
        <button class="btn btn-primary" :disabled="importSubmitting" @click="submitImport">
          {{ importSubmitting ? '处理中…' : '确认加入' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 移出确认 Modal ===== -->
  <div v-if="removeVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-danger text-white">确认移出成员</div>
      <div class="card-body">
        <p class="mb-2">
          确定将「<strong>{{ removeTarget?.name }}</strong>」（{{ removeTarget?.studentNo }}）移出该课程吗？
        </p>
        <div v-if="removeError" class="alert alert-danger py-2 mt-2">{{ removeError }}</div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="removeSubmitting" @click="removeVisible = false">取消</button>
        <button class="btn btn-danger" :disabled="removeSubmitting" @click="doRemove">
          {{ removeSubmitting ? '移出中…' : '确认移出' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCourse } from '@/api/course'
import { listClasses } from '@/api/class'
import { listMembers, addMember, addMembersByClass, removeMember } from '@/api/courseMember'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseName = ref('课程')

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageError = ref('')
const query = reactive({ keyword: '' })
const pager = reactive({ page: 1, size: 10 })
const totalPages = computed(() => (pager.size > 0 ? Math.max(1, Math.ceil(total.value / pager.size)) : 1))

async function loadCourseName() {
  try {
    const res = await getCourse(courseId)
    courseName.value = res.data.courseName
  } catch (e) {
    if (e?.code !== 1002) pageError.value = e?.message || '课程信息加载失败'
  }
}

async function fetchMembers(page = pager.page) {
  loading.value = true
  pageError.value = ''
  const params = { page, size: pager.size }
  if (query.keyword) params.keyword = query.keyword
  try {
    const res = await listMembers(courseId, params)
    list.value = res.data.list || []
    total.value = res.data.total || 0
    pager.page = res.data.page || page
    pager.size = res.data.size || pager.size
  } catch (e) {
    if (e?.code === 1003) pageError.value = '无权访问该功能'
    else if (e?.code !== 1002) pageError.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function reload(page) {
  if (page < 1) return
  fetchMembers(page)
}
function onSearch() { reload(1) }
function onReset() { query.keyword = ''; reload(1) }
function goBack() { router.push({ name: 'course-list' }) }

// ---------- 单个加入 ----------
const addVisible = ref(false)
const addSubmitting = ref(false)
const addError = ref('')
const addForm = reactive({ studentNo: '' })

function openAddOne() {
  addForm.studentNo = ''
  addError.value = ''
  addVisible.value = true
}

async function submitAdd() {
  addError.value = ''
  if (!addForm.studentNo) { addError.value = '学号不能为空'; return }
  addSubmitting.value = true
  try {
    await addMember(courseId, { studentNo: addForm.studentNo })
    addVisible.value = false
    reload(1)
  } catch (e) {
    addError.value = e?.message || '加入失败'
  } finally {
    addSubmitting.value = false
  }
}

// ---------- 按班级批量加入 ----------
const importVisible = ref(false)
const importSubmitting = ref(false)
const importError = ref('')
const importResult = ref(null)
const importForm = reactive({ classId: 0 })
const classes = ref([])

async function openImportClass() {
  importError.value = ''
  importResult.value = null
  importForm.classId = 0
  importVisible.value = true
  try {
    const res = await listClasses({ status: 1, page: 1, size: 100 })
    classes.value = res.data.list || []
  } catch (e) {
    importError.value = e?.message || '班级列表加载失败'
  }
}

async function submitImport() {
  importError.value = ''
  if (!importForm.classId) { importError.value = '请先选择班级'; return }
  importSubmitting.value = true
  try {
    const res = await addMembersByClass(courseId, { classId: importForm.classId })
    importResult.value = res.data
    reload(1)
  } catch (e) {
    importError.value = e?.message || '批量加入失败'
  } finally {
    importSubmitting.value = false
  }
}

// ---------- 移出 ----------
const removeVisible = ref(false)
const removeSubmitting = ref(false)
const removeError = ref('')
const removeTarget = ref(null)

function openRemove(m) {
  removeTarget.value = m
  removeError.value = ''
  removeVisible.value = true
}

async function doRemove() {
  if (!removeTarget.value) return
  removeSubmitting.value = true
  removeError.value = ''
  try {
    await removeMember(courseId, removeTarget.value.studentId)
    removeVisible.value = false
    const afterDelete = list.value.length === 1 && pager.page > 1 ? pager.page - 1 : pager.page
    reload(afterDelete)
  } catch (e) {
    removeError.value = e?.message || '移出失败'
  } finally {
    removeSubmitting.value = false
  }
}

onMounted(() => {
  loadCourseName()
  reload(1)
})
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
  width: 460px;
  max-width: 92vw;
}
</style>
