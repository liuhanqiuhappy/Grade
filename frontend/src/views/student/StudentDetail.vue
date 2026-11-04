<template>
  <div class="card shadow-sm" style="max-width: 640px;">
    <div class="card-body">
      <div class="d-flex align-items-center mb-3">
        <router-link to="/app/students" class="text-decoration-none me-2">←</router-link>
        <h5 class="mb-0">学生详情</h5>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>
      <div v-else-if="!detail">
        <p class="text-muted">加载中…</p>
      </div>
      <template v-else>
        <!-- 详情 -->
        <dl class="row mb-3">
          <dt class="col-sm-3">姓名</dt>
          <dd class="col-sm-9">{{ detail.name }}</dd>

          <dt class="col-sm-3">学号</dt>
          <dd class="col-sm-9">{{ detail.studentNo }}</dd>

          <dt class="col-sm-3">登录名</dt>
          <dd class="col-sm-9">
            <code>{{ detail.username }}</code>
            <div class="form-text text-info">登录名不随学号变更，学生一直用此登录名登录。</div>
          </dd>

          <dt class="col-sm-3">班级</dt>
          <dd class="col-sm-9">
            {{ detail.className || '—' }}
            <span v-if="detail.classStatus === 0" class="badge bg-secondary ms-1">已归档</span>
          </dd>

          <dt class="col-sm-3">状态</dt>
          <dd class="col-sm-9">
            <span class="badge" :class="detail.status === 1 ? 'bg-success' : 'bg-secondary'">
              {{ detail.status === 1 ? '在读' : '停用' }}
            </span>
          </dd>

          <dt class="col-sm-3">建档时间</dt>
          <dd class="col-sm-9">{{ detail.createdAt }}</dd>
        </dl>

        <hr />
        <div class="d-flex gap-2">
          <button class="btn btn-primary" :disabled="detail.classStatus === 0" @click="openEdit">编辑</button>
          <button class="btn btn-warning" @click="openReset">重置口令</button>
        </div>
        <div v-if="detail.classStatus === 0" class="form-text text-muted mt-2">
          该学生所在班级已归档，不可再编辑（转班/改名）。
        </div>
      </template>
    </div>
  </div>

  <!-- ===== 编辑学生 Modal（S5 部分更新）===== -->
  <div v-if="editVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span>编辑学生</span>
        <button class="btn-close btn-close-white" @click="editVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="editError" class="alert alert-danger py-2">{{ editError }}</div>
        <div class="mb-3">
          <label class="form-label">姓名</label>
          <input v-model.trim="editForm.name" class="form-control" />
          <div class="form-text">1–50 个字符</div>
        </div>
        <div class="mb-3">
          <label class="form-label">学号</label>
          <input v-model.trim="editForm.studentNo" class="form-control" />
          <div class="form-text">字母数字 4–32 位；修改学号不影响登录名</div>
        </div>
        <div class="mb-3">
          <label class="form-label">班级</label>
          <select v-model="editForm.classId" class="form-select">
            <option value="">不转班</option>
            <option v-for="c in classOptions" :key="c.classId" :value="c.classId">{{ c.className }}</option>
          </select>
          <div class="form-text">选择新班级即转班（目标必须为正常班级）</div>
        </div>
        <div class="mb-3">
          <label class="form-label">状态</label>
          <select v-model.number="editForm.status" class="form-select">
            <option :value="1">在读</option>
            <option :value="0">停用</option>
          </select>
          <div class="form-text">停用后该学生不能登录</div>
        </div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="editSubmitting" @click="editVisible = false">取消</button>
        <button class="btn btn-primary" :disabled="editSubmitting" @click="doEdit">
          {{ editSubmitting ? '保存中…' : '保存' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 重置口令 确认 Modal ===== -->
  <div v-if="resetConfirmVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-warning text-dark">确认重置口令</div>
      <div class="card-body">
        <p class="mb-2">确定要重置 <strong>{{ detail?.name }}</strong>（登录名 <code>{{ detail?.username }}</code>）的登录口令吗？</p>
        <p class="text-muted mb-0 small">旧口令将立即失效，新口令只显示一次，请确保能当面转告学生。</p>
        <div v-if="resetError" class="alert alert-danger py-2 mt-2">{{ resetError }}</div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="resetSubmitting" @click="resetConfirmVisible = false">取消</button>
        <button class="btn btn-warning" :disabled="resetSubmitting" @click="doReset">
          {{ resetSubmitting ? '重置中…' : '确认重置' }}
        </button>
      </div>
    </div>
  </div>

  <!-- ===== 新口令 Modal（一次性：不可遮罩关闭，必须点「我已抄录」）===== -->
  <div v-if="pwdVisible" class="modal-overlay no-close">
    <div class="modal-card card shadow">
      <div class="card-header bg-warning text-dark">口令已重置</div>
      <div class="card-body">
        <p class="mb-2">学生 <strong>{{ detail?.name }}</strong>（登录名 <code>{{ detail?.username }}</code>）的新口令：</p>
        <div class="d-flex gap-2 align-items-center mb-2">
          <code class="fs-4 fw-bold bg-light border rounded px-3 py-2">{{ newPassword }}</code>
          <button class="btn btn-sm btn-outline-primary" @click="copyPassword">复制</button>
        </div>
        <div class="alert alert-warning small mb-0">口令仅显示这一次，请立即抄录转告学生。</div>
      </div>
      <div class="card-footer">
        <button class="btn btn-primary w-100" @click="closePassword">我已抄录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getStudent, updateStudent, resetStudentPassword } from '@/api/student'
import { listClasses } from '@/api/class'

const route = useRoute()
const studentId = Number(route.params.studentId)

const detail = ref(null)
const pageError = ref('')
const classOptions = ref([])

async function loadDetail() {
  pageError.value = ''
  try {
    const res = await getStudent(studentId)
    detail.value = res.data
  } catch (e) {
    if (e?.code === 2004) pageError.value = '学生不存在'
    else if (e?.code === 1003) pageError.value = '无权访问该功能'
    else if (e?.code !== 1002) pageError.value = e?.message || '加载失败'
  }
}

async function loadClassOptions() {
  try {
    const res = await listClasses({ page: 1, size: 100, status: 1 })
    classOptions.value = res.data.list || []
  } catch (e) { /* 忽略 */ }
}

// ---------- 编辑 ----------
const editVisible = ref(false)
const editSubmitting = ref(false)
const editError = ref('')
const editForm = reactive({ name: '', studentNo: '', classId: '', status: 1 })

function openEdit() {
  editError.value = ''
  editForm.name = detail.value.name
  editForm.studentNo = detail.value.studentNo
  editForm.classId = detail.value.classId
  editForm.status = detail.value.status
  editVisible.value = true
}

async function doEdit() {
  editError.value = ''
  const name = editForm.name.trim()
  const studentNo = editForm.studentNo.trim()
  if (!name) { editError.value = '姓名不能为空'; return }
  if (name.length > 50) { editError.value = '姓名不能超过 50 个字符'; return }
  if (!/^[A-Za-z0-9]{4,32}$/.test(studentNo)) {
    editError.value = '学号需为字母或数字，4–32 位'
    return
  }

  // S5 部分更新：只传发生变化的字段
  const patch = {}
  if (name !== detail.value.name) patch.name = name
  if (studentNo !== detail.value.studentNo) patch.studentNo = studentNo
  if (editForm.classId !== '' && Number(editForm.classId) !== detail.value.classId) {
    patch.classId = Number(editForm.classId)
  }
  if (Number(editForm.status) !== detail.value.status) patch.status = Number(editForm.status)

  if (Object.keys(patch).length === 0) {
    editVisible.value = false
    return
  }

  editSubmitting.value = true
  try {
    const res = await updateStudent(studentId, patch)
    detail.value = res.data
    editVisible.value = false
  } catch (e) {
    // 2003 / 2006 / 2007 等直接展示后端 message
    editError.value = e?.message || '保存失败'
  } finally {
    editSubmitting.value = false
  }
}

// ---------- 重置口令 ----------
const resetConfirmVisible = ref(false)
const resetSubmitting = ref(false)
const resetError = ref('')
const pwdVisible = ref(false)
const newPassword = ref('')

function openReset() {
  resetError.value = ''
  resetConfirmVisible.value = true
}

async function doReset() {
  resetSubmitting.value = true
  resetError.value = ''
  try {
    const res = await resetStudentPassword(studentId)
    newPassword.value = res.data.initialPassword
    resetConfirmVisible.value = false
    pwdVisible.value = true
  } catch (e) {
    resetError.value = e?.message || '重置失败'
  } finally {
    resetSubmitting.value = false
  }
}

async function copyPassword() {
  if (!newPassword.value) return
  try {
    await navigator.clipboard.writeText(newPassword.value)
  } catch (e) {
    window.alert('复制失败，请手动抄录')
  }
}

function closePassword() {
  pwdVisible.value = false
  newPassword.value = ''
}

onMounted(() => {
  loadClassOptions()
  loadDetail()
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
.modal-overlay.no-close { cursor: not-allowed; }
.modal-card { width: 440px; max-width: 92vw; }
</style>
