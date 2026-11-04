<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <h5 class="mb-0 me-auto">班级管理</h5>

        <!-- 搜索：名称关键词 + 年级 -->
        <input v-model.trim="query.keyword" class="form-control form-control-sm" style="width: 200px;"
               placeholder="按班级名称搜索" @keyup.enter="onSearch" />
        <input v-model.trim="query.gradeYear" class="form-control form-control-sm" style="width: 120px;"
               placeholder="年级(如2023)" @keyup.enter="onSearch" />
        <button class="btn btn-primary btn-sm" @click="onSearch">查询</button>
        <button class="btn btn-outline-secondary btn-sm" @click="onReset">重置</button>
        <button class="btn btn-success btn-sm" @click="openCreate">+ 新建班级</button>
      </div>

      <!-- 业务失败提示（列表加载类错误，若由全局 alert 弹出则此处为空） -->
      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>班级名称</th>
              <th>年级</th>
              <th>在读学生</th>
              <th>创建时间</th>
              <th style="width: 240px;">操作</th>
            </tr>
          </thead>
          <tbody v-if="list.length > 0">
            <tr v-for="c in list" :key="c.classId">
              <td>{{ c.className }}</td>
              <td>{{ c.gradeYear ?? '—' }}</td>
              <td>
                <a href="javascript:void(0)" class="text-decoration-none" @click="goStudents(c.classId)">
                  {{ c.studentCount }} 人
                </a>
              </td>
              <td>{{ c.createdAt }}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary me-1" @click="goStudents(c.classId)">学生名单</button>
                <button class="btn btn-sm btn-outline-secondary me-1" @click="openEdit(c)">重命名</button>
                <button class="btn btn-sm btn-outline-danger" @click="openArchive(c)">归档</button>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="5" class="text-center text-muted py-4">
                {{ loading ? '加载中…' : '暂无班级' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页：页码从 1 开始，size 10/20/50 -->
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

  <!-- ===== 新建 / 重命名 班级 Modal ===== -->
  <div v-if="formVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <span>{{ formMode === 'create' ? '新建班级' : '重命名班级' }}</span>
        <button class="btn-close btn-close-white" @click="formVisible = false"></button>
      </div>
      <div class="card-body">
        <div v-if="formError" class="alert alert-danger py-2">{{ formError }}</div>
        <div class="mb-3">
          <label class="form-label">班级名称 <span class="text-danger">*</span></label>
          <input v-model.trim="form.className" class="form-control" placeholder="如：计算机2301班" />
          <div class="form-text">1–64 个字符，首尾空格会被去除</div>
        </div>
        <div class="mb-3">
          <label class="form-label">年级</label>
          <input v-model.trim="form.gradeYear" class="form-control" placeholder="可空，如 2023" />
          <div class="form-text">2000–2100 的整数</div>
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

  <!-- ===== 归档二次确认 Modal（必须展示学生数）===== -->
  <div v-if="archiveVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-danger text-white">确认归档班级</div>
      <div class="card-body">
        <p class="mb-2">确定要归档班级「<strong>{{ archiveTarget?.className }}</strong>」吗？</p>
        <p class="mb-2">该班当前有 <strong class="text-danger">{{ archiveTarget?.studentCount ?? 0 }}</strong> 名学生。</p>
        <p class="text-muted mb-0 small">
          归档后班级不再出现在列表，<strong>学生与成绩数据保留</strong>；如有学生录错班，请用「转班」而非归档班级。
        </p>
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
import { listClasses, createClass, updateClass, archiveClass } from '@/api/class'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageError = ref('')

const query = reactive({ keyword: '', gradeYear: '' })
const pager = reactive({ page: 1, size: 10 })

const totalPages = computed(() => (pager.size > 0 ? Math.max(1, Math.ceil(total.value / pager.size)) : 1))

async function fetchClasses(page = pager.page) {
  loading.value = true
  pageError.value = ''
  const params = {
    page,
    size: pager.size,
    status: 1 // 默认只看正常班
  }
  if (query.keyword) params.keyword = query.keyword
  if (query.gradeYear) {
    const gy = Number(query.gradeYear)
    if (!Number.isInteger(gy) || gy < 2000 || gy > 2100) {
      pageError.value = '年级需为 2000–2100 的整数'
      loading.value = false
      return
    }
    params.gradeYear = gy
  }
  try {
    const res = await listClasses(params)
    list.value = res.data.list || []
    total.value = res.data.total || 0
    pager.page = res.data.page || page
    pager.size = res.data.size || pager.size
  } catch (e) {
    // 未登录/权限错误由全局处理；此处兜底
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
  fetchClasses(page)
}

function onSearch() { reload(1) }
function onReset() {
  query.keyword = ''
  query.gradeYear = ''
  reload(1)
}

function goStudents(classId) {
  router.push(`/app/classes/${classId}/students`)
}

// ---------- 新建 / 重命名 ----------
const formVisible = ref(false)
const formMode = ref('create')
const formSubmitting = ref(false)
const formError = ref('')
const form = reactive({ className: '', gradeYear: '' })

function openCreate() {
  formMode.value = 'create'
  form.className = ''
  form.gradeYear = ''
  formError.value = ''
  formVisible.value = true
}

function openEdit(c) {
  formMode.value = 'edit'
  form.className = c.className
  form.gradeYear = c.gradeYear ?? ''
  formError.value = ''
  editClassId = c.classId
  formVisible.value = true
}

let editClassId = null

async function submitForm() {
  formError.value = ''
  const name = form.className.trim()
  if (!name) { formError.value = '班级名称不能为空'; return }
  if (name.length > 64) { formError.value = '班级名称不能超过 64 个字符'; return }

  let gradeYear = null
  if (form.gradeYear !== '') {
    gradeYear = Number(form.gradeYear)
    if (!Number.isInteger(gradeYear) || gradeYear < 2000 || gradeYear > 2100) {
      formError.value = '年级需为 2000–2100 的整数'
      return
    }
  }

  formSubmitting.value = true
  try {
    if (formMode.value === 'create') {
      await createClass({ className: name, gradeYear })
    } else {
      const patch = { className: name }
      // 显式传 gradeYear（含 null 表示清空），保证部分更新语义
      patch.gradeYear = gradeYear
      await updateClass(editClassId, patch)
    }
    formVisible.value = false
    reload(1)
  } catch (e) {
    // silent:true 的请求，直接展示后端 message（2001 重名等）
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
    // 契约：先调接口，成功后刷新列表（不做乐观更新）
    await archiveClass(archiveTarget.value.classId)
    archiveVisible.value = false
    // 末页归档后自动回退一页，避免出现空页
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
