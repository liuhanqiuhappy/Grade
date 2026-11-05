<template>
  <div class="card shadow-sm">
    <div class="card-body">
      <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
        <button class="btn btn-outline-secondary btn-sm" @click="goBack">← 返回课程</button>
        <h5 class="mb-0 me-auto">
          {{ courseName }} <span class="text-muted small fw-normal">成绩录入与确认</span>
        </h5>
        <span class="badge" :class="confirmed ? 'bg-success' : 'bg-warning text-dark'">
          {{ confirmed ? '已确认发布（只读）' : '草稿可编辑' }}
        </span>
        <button class="btn btn-outline-primary btn-sm" :disabled="confirmed || busy" @click="save">
          {{ saving ? '保存中…' : '保存草稿' }}
        </button>
        <button class="btn btn-success btn-sm" :disabled="confirmed || busy" @click="openConfirm">确认发布</button>
      </div>

      <div v-if="pageError" class="alert alert-danger py-2">{{ pageError }}</div>
      <div v-if="message" class="alert alert-success py-2">{{ message }}</div>
      <div v-if="confirmed" class="alert alert-info py-2">
        该课程成绩已确认发布，数据已锁定只读。
      </div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th style="width: 140px;">学号</th>
              <th>姓名</th>
              <th>班级</th>
              <th style="width: 160px;">分数（0–100，可留空）</th>
              <th style="width: 120px;">状态</th>
            </tr>
          </thead>
          <tbody v-if="rows.length > 0">
            <tr v-for="r in rows" :key="r.studentId">
              <td>{{ r.studentNo }}</td>
              <td>{{ r.name }}</td>
              <td>{{ r.className || '—' }}</td>
              <td>
                <input v-model="r.scoreText" type="number" min="0" max="100" step="0.5"
                       class="form-control form-control-sm" style="width: 110px;"
                       :disabled="confirmed" placeholder="留空" />
              </td>
              <td>
                <span class="badge" :class="r.status === 1 ? 'bg-success' : 'bg-secondary'">
                  {{ r.status === 1 ? '已确认' : '草稿' }}
                </span>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="5" class="text-center text-muted py-4">
                {{ loading ? '加载中…' : '该课程还没有成员，请先到「成员」页加入学生' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="rows.length > 0" class="d-flex justify-content-between text-muted small mt-2">
        <span>共 {{ rows.length }} 名学生</span>
        <span>已录入 {{ filledCount }} 人，留空 {{ rows.length - filledCount }} 人</span>
      </div>
    </div>
  </div>

  <!-- ===== 确认发布二次确认 ===== -->
  <div v-if="confirmVisible" class="modal-overlay">
    <div class="modal-card card shadow">
      <div class="card-header bg-success text-white">确认发布成绩</div>
      <div class="card-body">
        <p class="mb-2">确认发布后，本课程成绩将<strong>锁定为只读</strong>，不能再修改。</p>
        <p class="text-muted small mb-2">当前共 {{ rows.length }} 名学生，已录入 {{ filledCount }} 人、留空 {{ rows.length - filledCount }} 人。</p>
        <div v-if="confirmError" class="alert alert-danger py-2 mt-2">{{ confirmError }}</div>
      </div>
      <div class="card-footer d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" :disabled="busy" @click="confirmVisible = false">取消</button>
        <button class="btn btn-success" :disabled="busy" @click="doConfirm">
          {{ busy ? '处理中…' : '确认发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCourse } from '@/api/course'
import { getGrades, saveGrades, confirmGrades } from '@/api/grade'

const route = useRoute()
const router = useRouter()
const courseId = Number(route.params.courseId)

const courseName = ref('课程')
const rows = ref([])
const confirmed = ref(false)
const loading = ref(false)
const saving = ref(false)
const busy = ref(false)
const pageError = ref('')
const message = ref('')

const filledCount = computed(() => rows.value.filter(r => r.scoreText !== '' && r.scoreText !== null).length)

function toRows(list) {
  return (list || []).map(item => ({
    studentId: item.studentId,
    studentNo: item.studentNo,
    name: item.name,
    className: item.className,
    status: item.status,
    scoreText: item.score === null || item.score === undefined ? '' : String(item.score)
  }))
}

async function loadCourseName() {
  try {
    const res = await getCourse(courseId)
    courseName.value = res.data.courseName
  } catch (e) {
    if (e?.code !== 1002) pageError.value = e?.message || '课程信息加载失败'
  }
}

async function loadBoard() {
  loading.value = true
  pageError.value = ''
  try {
    const res = await getGrades(courseId)
    rows.value = toRows(res.data.list)
    confirmed.value = !!res.data.confirmed
  } catch (e) {
    if (e?.code === 1003) pageError.value = '无权访问该功能'
    else if (e?.code !== 1002) pageError.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  pageError.value = ''
  message.value = ''
  const items = []
  for (const r of rows.value) {
    if (r.scoreText !== '' && r.scoreText !== null) {
      const num = Number(r.scoreText)
      if (Number.isNaN(num) || num < 0 || num > 100) {
        pageError.value = `学号 ${r.studentNo} 的分数必须在 0–100 之间`
        return
      }
      items.push({ studentId: r.studentId, score: num })
    } else {
      items.push({ studentId: r.studentId, score: null })
    }
  }
  saving.value = true
  try {
    const res = await saveGrades(courseId, { items })
    message.value = `已保存草稿（${res.data} 条）`
  } catch (e) {
    pageError.value = e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const confirmVisible = ref(false)
const confirmError = ref('')

function openConfirm() {
  if (rows.value.length === 0) {
    pageError.value = '该课程还没有成员，无法确认成绩'
    return
  }
  confirmError.value = ''
  confirmVisible.value = true
}

async function doConfirm() {
  busy.value = true
  confirmError.value = ''
  pageError.value = ''
  try {
    const res = await confirmGrades(courseId)
    rows.value = toRows(res.data.list)
    confirmed.value = !!res.data.confirmed
    confirmVisible.value = false
    message.value = '成绩已确认发布，已锁定只读'
  } catch (e) {
    confirmError.value = e?.message || '确认失败'
  } finally {
    busy.value = false
  }
}

function goBack() {
  router.push({ name: 'course-list' })
}

onMounted(() => {
  loadCourseName()
  loadBoard()
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
