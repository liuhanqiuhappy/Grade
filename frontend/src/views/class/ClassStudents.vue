<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <h5 class="mb-0 me-auto">
          <router-link to="/app/classes" class="text-decoration-none me-2">←</router-link>
          班级学生名单
          <span v-if="className">{{ className }}</span>
          <span v-if="classStatus === 0" class="badge bg-secondary ms-2">已归档</span>
        </h5>
        <input v-model.trim="query.keyword" class="form-control form-control-sm" style="width: 200px;"
               placeholder="按学号或姓名搜索" @keyup.enter="onSearch" />
        <select v-model.number="query.status" class="form-select form-select-sm" style="width: 120px;" @change="reload(1)">
          <option :value="1">在读</option>
          <option :value="0">停用</option>
        </select>
        <button class="btn btn-primary btn-sm" @click="onSearch">查询</button>
        <button class="btn btn-success btn-sm" :disabled="classStatus === 0" @click="openAdd">+ 添加学生</button>
        <button class="btn btn-info btn-sm" :disabled="classStatus === 0" @click="openImport">⇪ 批量导入</button>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>
      <div v-if="classStatus === 0" class="alert alert-warning py-2">
        该班级已归档，只能查看历史名单，不能再添加或修改学生。
      </div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>学号</th>
              <th>姓名</th>
              <th>登录名</th>
              <th>状态</th>
              <th>建档时间</th>
              <th style="width: 140px;">操作</th>
            </tr>
          </thead>
          <tbody v-if="list.length > 0">
            <tr v-for="s in list" :key="s.studentId">
              <td>{{ s.studentNo }}</td>
              <td>{{ s.name }}</td>
              <td><code>{{ s.username }}</code></td>
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
              <td colspan="6" class="text-center text-muted py-4">
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

  <!-- ===== 添加学生 Modal（表单）===== -->
  <div v-if="addVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-success text-white d-flex justify-content-between align-items-center">
        <span>添加学生（开通账号）</span>
        <button class="btn-close btn-close-white" @click="addVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="addError" class="alert alert-danger py-2">{{ addError }}</div>
        <div class="mb-3">
          <label class="form-label">学号 <span class="text-danger">*</span></label>
          <input v-model.trim="addForm.studentNo" class="form-control" placeholder="如：2023010101" />
          <div class="form-text">字母或数字，4–32 位，不含空格与中文；将同时作为登录名</div>
        </div>
        <div class="mb-3">
          <label class="form-label">姓名 <span class="text-danger">*</span></label>
          <input v-model.trim="addForm.name" class="form-control" placeholder="学生姓名" />
          <div class="form-text">1–50 个字符</div>
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="addSubmitting" @click="addVisible = false">取消</button>
        <button class="btn btn-success" :disabled="addSubmitting" @click="doAdd">
          {{ addSubmitting ? '提交中…' : '添加并开通账号' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 初始口令 Modal（一次性敏感信息：不可遮罩关闭，必须点「我已抄录」）===== -->
  <div v-if="pwdVisible" class="modal-overlay no-close">
    <div class="modal-card card shadow">
      <div class="card-header bg-warning text-dark">学生账号已开通</div>
      <div class="card-body">
        <p class="mb-2">
          已为学生 <strong>{{ createdStudent?.name }}</strong>（学号
          <strong>{{ createdStudent?.studentNo }}</strong>）开通登录账号。
        </p>
        <p class="mb-2">初始口令：</p>
        <div class="d-flex gap-2 align-items-center mb-2">
          <code class="fs-4 fw-bold bg-light border rounded px-3 py-2">{{ initialPassword }}</code>
          <button class="btn btn-sm btn-outline-primary" @click="copyPassword">复制</button>
        </div>
        <div class="alert alert-warning small mb-0">
          口令仅显示这一次，请立即抄录并转告学生；关闭后无法再次查看，丢失只能由教师重置。请勿在公共场合展示。
        </div>
      </div>
      <div class="card-footer">
        <button class="btn btn-primary w-100" @click="closePassword">我已抄录</button>
      </div>
    </div>
  </div>

  <!-- ===== 批量导入学生 Modal（Excel 上传，部分成功）===== -->
  <div v-if="importVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-info text-white d-flex justify-content-between align-items-center">
        <span>批量导入学生（Excel）</span>
        <button class="btn-close btn-close-white" @click="importVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="importError" class="alert alert-danger py-2">{{ importError }}</div>
        <div class="alert alert-info small mb-3">
          请选择 <code>.xlsx</code> / <code>.xls</code> 文件。<br />
          第一行为表头，从第二行起每行一个学生：<strong>第 1 列学号</strong>（字母或数字，4–32 位）、
          <strong>第 2 列姓名</strong>（1–50 字）。<br />
          重复学号、格式错误的行会被跳过并列出原因，其余正常导入。
        </div>
        <input type="file" class="form-control" accept=".xlsx,.xls"
               :disabled="importSubmitting" @change="onPickFile" />
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="importSubmitting" @click="importVisible = false">取消</button>
        <button class="btn btn-info" :disabled="importSubmitting || !importFile" @click="doImport">
          {{ importSubmitting ? '导入中…' : '开始导入' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 批量导入结果 Modal（部分成功明细）===== -->
  <div v-if="importResultVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header d-flex justify-content-between align-items-center"
           :class="importResult && importResult.failedCount > 0 ? 'bg-warning text-dark' : 'bg-success text-white'">
        <span>批量导入结果</span>
        <button class="btn-close btn-close-white" @click="importResultVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="importResult" class="mb-2">
          <span class="me-3">共 <strong>{{ importResult.totalCount }}</strong> 条</span>
          <span class="text-success me-3">成功 <strong>{{ importResult.successCount }}</strong></span>
          <span :class="importResult.failedCount > 0 ? 'text-danger' : 'text-success'">
            失败 <strong>{{ importResult.failedCount }}</strong>
          </span>
        </div>
        <div v-if="importResult && importResult.failures.length > 0" class="table-responsive" style="max-height: 260px;">
          <table class="table table-sm table-striped">
            <thead><tr><th>行号</th><th>学号</th><th>姓名</th><th>原因</th></tr></thead>
            <tbody>
              <tr v-for="f in importResult.failures" :key="f.row">
                <td>{{ f.row }}</td><td>{{ f.studentNo }}</td><td>{{ f.name }}</td>
                <td class="text-danger small">{{ f.message }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="importResult && importResult.failedCount === 0" class="alert alert-success small mb-0">
          全部导入成功，名单已刷新。
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end">
        <button class="btn btn-primary" @click="closeImportResult">知道了</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listClassStudents, addStudent, importStudents } from '@/api/student'

const route = useRoute()
const router = useRouter()
const classId = Number(route.params.classId)

const className = ref('')
const classStatus = ref(1)
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageError = ref('')

const query = reactive({ keyword: '', status: 1 })
const pager = reactive({ page: 1, size: 10 })
const totalPages = computed(() => (pager.size > 0 ? Math.max(1, Math.ceil(total.value / pager.size)) : 1))

async function fetchStudents(page = pager.page) {
  loading.value = true
  pageError.value = ''
  const params = { page, size: pager.size, status: query.status }
  if (query.keyword) params.keyword = query.keyword
  try {
    const res = await listClassStudents(classId, params)
    const data = res.data
    list.value = data.list || []
    total.value = data.total || 0
    pager.page = data.page || page
    pager.size = data.size || pager.size
    // 从首条取班级信息（已归档也允许查看名单）
    if (data.list && data.list.length > 0) {
      className.value = data.list[0].className
      classStatus.value = data.list[0].classStatus
    }
  } catch (e) {
    if (e?.code === 2002) {
      pageError.value = '班级不存在'
    } else if (e?.code === 1003) {
      pageError.value = '无权访问该功能'
    } else if (e?.code !== 1002) {
      pageError.value = e?.message || '加载失败'
    }
  } finally {
    loading.value = false
  }
}

function reload(page) { if (page >= 1) fetchStudents(page) }
function onSearch() { reload(1) }
function goDetail(studentId) { router.push(`/app/students/${studentId}`) }

// ---------- 添加学生 ----------
const addVisible = ref(false)
const addSubmitting = ref(false)
const addError = ref('')
const addForm = reactive({ studentNo: '', name: '' })

function openAdd() {
  addForm.studentNo = ''
  addForm.name = ''
  addError.value = ''
  addVisible.value = true
}

async function doAdd() {
  addError.value = ''
  const studentNo = addForm.studentNo.trim()
  const name = addForm.name.trim()
  // 学号：字母数字 4–32，不允许空格与中文（契约 §5.2 S2）
  if (!/^[A-Za-z0-9]{4,32}$/.test(studentNo)) {
    addError.value = '学号需为字母或数字，4–32 位，不含空格与中文'
    return
  }
  if (!name) { addError.value = '姓名不能为空'; return }
  if (name.length > 50) { addError.value = '姓名不能超过 50 个字符'; return }

  addSubmitting.value = true
  try {
    const res = await addStudent(classId, { studentNo, name })
    // 展示一次性初始口令（安全约束见契约 §5.2）
    createdStudent.value = res.data.student
    initialPassword.value = res.data.initialPassword
    addVisible.value = false
    pwdVisible.value = true
    reload(1)
  } catch (e) {
    // 2003 / 2006 / 2007 等直接展示后端 message
    addError.value = e?.message || '添加失败'
  } finally {
    addSubmitting.value = false
  }
}

// ---------- 初始口令弹窗（一次性）----------
const pwdVisible = ref(false)
const createdStudent = ref(null)
const initialPassword = ref('')
let copied = false

async function copyPassword() {
  if (!initialPassword.value) return
  try {
    await navigator.clipboard.writeText(initialPassword.value)
    copied = true
    // 复制后不弹 alert，提示文案由按钮短暂变化（此处简化为不额外提示）
  } catch (e) {
    // 剪贴板不可用时提示手动复制
    window.alert('复制失败，请手动抄录')
  }
}

function closePassword() {
  // 关闭后不写入任何持久化存储；仅保留在内存直到组件销毁
  pwdVisible.value = false
  createdStudent.value = null
  initialPassword.value = ''
  copied = false
}

// ---------- 批量导入（Excel，部分成功）----------
const importVisible = ref(false)
const importSubmitting = ref(false)
const importError = ref('')
const importFile = ref(null)
const importResultVisible = ref(false)
const importResult = ref(null)

function openImport() {
  importFile.value = null
  importError.value = ''
  importResult.value = null
  importVisible.value = true
}

function onPickFile(e) {
  importFile.value = (e.target.files && e.target.files[0]) || null
}

async function doImport() {
  if (!importFile.value) { importError.value = '请先选择 Excel 文件'; return }
  const ext = importFile.value.name.split('.').pop().toLowerCase()
  if (!['xlsx', 'xls'].includes(ext)) { importError.value = '仅支持 .xlsx / .xls 文件'; return }
  importError.value = ''
  importSubmitting.value = true
  const formData = new FormData()
  formData.append('file', importFile.value)
  try {
    const res = await importStudents(classId, formData)
    importResult.value = res.data || {}
    importVisible.value = false
    importResultVisible.value = true
    reload(1)
  } catch (e) {
    importError.value = e?.message || '导入失败'
  } finally {
    importSubmitting.value = false
  }
}

function closeImportResult() {
  importResultVisible.value = false
  importResult.value = null
  importFile.value = null
}

onMounted(() => fetchStudents(1))
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
.modal-overlay.no-close {
  /* 初始口令弹窗不可点击遮罩关闭 */
  cursor: not-allowed;
}
.modal-card {
  width: 440px;
  max-width: 92vw;
}
</style>
