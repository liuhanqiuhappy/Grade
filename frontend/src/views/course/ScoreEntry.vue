<template>
  <div>
    <div class="page-head">
      <div>
        <h2>成绩录入</h2>
        <p class="sub">按班级分组录入；暂存可留空，清空 ≠ 填 0；最终确认在「确认与统计」完成。</p>
      </div>
      <div class="row-flex">
        <button class="btn btn-o" :disabled="readonly || rows.length === 0 || importing" @click="openImport">
          <Icon name="upload" /> Excel 导入
        </button>
        <button class="btn btn-p" :disabled="readonly || rows.length === 0 || saving" @click="saveAll">
          <Icon name="save" /> {{ saving ? '保存中…' : '暂存' }}
        </button>
        <button class="btn btn-o" :disabled="readonly || rows.length === 0" @click="clearLocal">
          <Icon name="close" /> 清空
        </button>
      </div>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>

    <!-- 状态条 -->
    <div class="card mt-16" style="display: flex; align-items: center; gap: 14px; flex-wrap: wrap; padding: 12px 18px">
      <span class="chip" :class="readonly ? 'c-slate' : 'c-amber'">{{ readonly ? '已确认 · 只读' : '草稿' }}</span>
      <span class="small">共 {{ rows.length }} 名学生 · 已录入 {{ recordedCount }} 人</span>
      <div style="display: flex; align-items: center; gap: 10px; min-width: 240px; flex: 1">
        <div class="prog" style="flex: 1" :class="{ done: progress === 100 }">
          <i :style="{ width: progress + '%' }"></i>
        </div>
        <span class="small" style="white-space: nowrap">{{ progress }}%</span>
      </div>
    </div>

    <!-- 分组标签 -->
    <div v-if="groups.length" class="row-flex mt-16">
      <span
        v-for="g in groups"
        :key="g.className"
        class="group-tab"
        :class="{ on: activeGroup === g.className }"
        @click="activeGroup = g.className"
      >
        {{ g.className }} <span class="n">{{ g.rows.length }} 人</span>
      </span>
    </div>

    <section class="card mt-14">
      <div class="card-h">
        <h3><Icon name="table" /> 学生成绩表</h3>
        <span class="chip c-indigo">{{ activeGroup || '全部' }}</span>
        <div class="ml-auto row-flex">
          <label class="search-box" style="width: 220px">
            <Icon name="search" />
            <input v-model.trim="keyword" class="input" style="height: 34px" placeholder="搜索学号 / 姓名" />
          </label>
        </div>
      </div>

      <div v-if="loading" class="empty">
        <div class="ico"><Icon name="table" /></div><h4>正在加载成绩单…</h4>
      </div>
      <div v-else-if="rows.length === 0" class="empty">
        <div class="ico"><Icon name="table" /></div>
        <h4>该课程暂无成员</h4>
        <p>先到「课程成员」把学生加入课程，之后才能在成绩单中录入。</p>
      </div>
      <div v-else-if="visibleRows.length === 0" class="empty">
        <div class="ico"><Icon name="search" /></div>
        <h4>没有匹配的学生</h4>
        <p>换个学号或姓名关键词试试。</p>
      </div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="width: 150px">学号</th>
              <th>姓名</th>
              <th style="width: 90px">班级</th>
              <th style="width: 150px">分数（0—100）</th>
              <th style="width: 120px">录入状态</th>
              <th style="width: 90px; text-align: right">清空</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in visibleRows" :key="r.studentId">
              <td class="mono">{{ r.studentNo }}</td>
              <td style="font-weight: 500">{{ r.studentName }}</td>
              <td class="muted">{{ r.className }}</td>
              <td>
                <input
                  v-model="draft[r.studentId]"
                  class="score-input"
                  :class="{ blank: isBlank(r.studentId) && draft[r.studentId] !== '0', zero: draft[r.studentId] === '0' || draft[r.studentId] === '0.0' }"
                  :readonly="isRowLocked(r)"
                  inputmode="decimal"
                  placeholder="未录入"
                />
              </td>
              <td>
                <span v-if="isRowLocked(r)" class="chip c-slate">已确认 · 只读</span>
                <span v-else-if="isBlank(r.studentId)" class="chip c-slate">未录入</span>
                <span v-else-if="isInvalid(r.studentId)" class="chip c-rose">数值非法</span>
                <span v-else class="chip c-green">已录入</span>
              </td>
              <td style="text-align: right">
                <button class="icon-btn" :title="isRowLocked(r) ? '已确认发布 · 点击查看原因' : '清空该行（回到未录入）'" @click="clearRow(r)">
                  <Icon name="close" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="banner b-slate mt-14">
      <Icon name="info" />
      <span>
        <b>留空表示「尚未录入」</b>，与 0 分不同：留空不参与不及格统计，也不会在最终确认时通过完整性校验。
      </span>
    </div>

    <!-- Excel 批量导入成绩 -->
    <div v-if="importOpen" class="mask" @click.self="closeImport">
      <div class="modal" style="max-width: 580px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="upload" /></div>
          <div>
            <h3>批量导入成绩</h3>
            <div class="m-sub">{{ course?.name || '当前课程' }} · 共 {{ rows.length }} 名学生</div>
          </div>
        </div>

        <div class="modal-b">
          <div class="banner b-slate" style="margin-bottom: 16px">
            <Icon name="info" />
            <span>
              <b>模板格式：</b>第 1 列学号、第 2 列分数（0—100），首行表头自动跳过，从第 2 行开始填数据。
              导入直接写入后端并覆盖同学号的现有成绩，<b>页面上未暂存的修改会丢失</b>；已确认发布的分组不会被改动。
            </span>
          </div>

          <input ref="fileInput" type="file" accept=".xlsx" hidden @change="onPick" />
          <div class="row-flex">
            <button class="btn btn-o" :disabled="importing" @click="fileInput.click()">
              <Icon name="upload" /> 选择 .xlsx 文件
            </button>
            <span class="small">{{ importFile ? importFile.name : '尚未选择文件' }}</span>
          </div>

          <div v-if="importError" class="banner b-rose mt-14" style="margin-bottom: 0">
            <Icon name="alert" /><span>{{ importError }}</span>
          </div>

          <div v-if="importResult" class="mt-16">
            <div class="row-flex">
              <span class="chip c-green">成功 {{ importResult.successCount }} 条</span>
              <span v-if="importResult.failures.length" class="chip c-rose">
                失败 {{ importResult.failures.length }} 条
              </span>
              <span v-else class="chip c-slate">无失败行</span>
            </div>
            <div
              v-if="importResult.failures.length"
              class="table-wrap mt-12"
              style="max-height: 220px; overflow-y: auto"
            >
              <table>
                <thead>
                  <tr>
                    <th style="width: 100px">Excel 行号</th>
                    <th>失败原因</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="f in importResult.failures" :key="f.row + f.reason">
                    <td class="mono">第 {{ f.row }} 行</td>
                    <td class="small">{{ f.reason }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-if="importResult.failures.length" class="small mt-8" style="margin-bottom: 0">
              失败行未写入数据库，修正 Excel 后可重新导入；成功行已生效并已刷新到下表。
            </p>
          </div>
        </div>

        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="closeImport">{{ importResult ? '关闭' : '取消' }}</button>
            <button class="btn btn-p" :disabled="!importFile || importing" @click="submitImport">
              {{ importing ? '导入中…' : '开始导入' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import Icon from '../../components/Icon.vue'
import { listScores, saveScores, importScores } from '../../api/score'
import { listCourses } from '../../api/course'
import { useCourseContextStore } from '../../store/courseContext'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const ctx = useCourseContextStore()
const toast = useToast()

const courseId = Number(route.params.id)
const course = ref(null)
const rows = ref([])
const draft = reactive({})
// 每行读取时的版本号，保存时原样回传：他人已改则后端拒绝，避免陈旧覆盖（§6-4e）
const versions = reactive({})
const activeGroup = ref('')
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')

// Excel 导入状态：文件选择 → 上传 → 结果明细都在这一个弹窗里完成
const fileInput = ref(null)
const importOpen = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importError = ref('')
const importResult = ref(null)

const readonly = computed(() => course.value?.status === 'CONFIRMED')

// 行级锁：课程整体已确认，或该行所属分组已确认发布（H1：确认粒度 = 课程 + 班级）
function isRowLocked(r) {
  return readonly.value || r.groupConfirmed === true
}

const groups = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    const key = r.className || '未分班'
    if (!map.has(key)) map.set(key, { className: key, rows: [] })
    map.get(key).rows.push(r)
  }
  return [...map.values()]
})

const visibleRows = computed(() => {
  const kw = keyword.value.trim()
  return rows.value.filter((r) => {
    if (activeGroup.value && (r.className || '未分班') !== activeGroup.value) return false
    if (!kw) return true
    return r.studentNo.includes(kw) || r.studentName.includes(kw)
  })
})

const recordedCount = computed(
  () => rows.value.filter((r) => !isBlank(r.studentId) && !isInvalid(r.studentId)).length
)
const progress = computed(() =>
  rows.value.length ? Math.round((recordedCount.value / rows.value.length) * 100) : 0
)

function isBlank(id) {
  return draft[id] === '' || draft[id] === null || draft[id] === undefined
}

function isInvalid(id) {
  const v = draft[id]
  if (isBlank(id)) return false
  const n = Number(v)
  return Number.isNaN(n) || n < 0 || n > 100
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, courses] = await Promise.all([listScores(courseId), listCourses()])
    rows.value = list
    course.value = courses.find((c) => c.id === courseId) || null
    for (const r of list) {
      draft[r.studentId] = r.score === null || r.score === undefined ? '' : String(r.score)
      versions[r.studentId] = r.version ?? null
    }
    if (!activeGroup.value && groups.value.length) activeGroup.value = groups.value[0].className
    if (ctx.id !== courseId) {
      ctx.apply({ id: courseId, name: course.value?.name || `课程 #${courseId}`, term: course.value?.term || '' })
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function clearRow(r) {
  // 只读行不静默禁用：点得动、但说清为什么不能改
  if (isRowLocked(r)) {
    toast.error('该行成绩已确认发布，不能清空', '已确认分组的成绩只读；整门课确认后所有行都不可修改。')
    return
  }
  draft[r.studentId] = ''
}

function clearLocal() {
  if (!window.confirm('清空当前分组已填写的分数？此操作只影响本页输入，需点「暂存」后才会生效。')) return
  const targets = visibleRows.value.filter((r) => !isRowLocked(r))
  if (targets.length === 0) {
    toast.error('没有可清空的行', '当前分组已确认发布，成绩只读')
    return
  }
  for (const r of targets) draft[r.studentId] = ''
  toast.success('已清空当前分组', '点「暂存」后写入后端')
}

async function saveAll() {
  const invalid = rows.value.filter((r) => isInvalid(r.studentId))
  if (invalid.length) {
    toast.error('存在非法分数', `${invalid[0].studentName} 的分数必须在 0—100 之间`)
    return
  }

  // 只提交未锁定的行；已确认分组的行不参与暂存（H1）
  const editable = rows.value.filter((r) => !isRowLocked(r))
  if (editable.length === 0) {
    toast.error('没有可保存的成绩', '本课程各分组均已确认发布，成绩只读')
    return
  }

  saving.value = true
  try {
    const entries = editable.map((r) => ({
      studentId: r.studentId,
      score: isBlank(r.studentId) ? null : Number(draft[r.studentId]),
      expectedVersion: versions[r.studentId] ?? null
    }))
    const data = await saveScores(courseId, entries)
    if (data.skipped > 0) {
      const names = (data.skippedStudents || []).slice(0, 3).join('、')
      toast.success(`已暂存 ${data.saved} 条成绩`, `另有 ${data.skipped} 条因分组已确认被跳过：${names}${data.skipped > 3 ? ' 等' : ''}`)
    } else {
      toast.success(`已暂存 ${data.saved} 条成绩`)
    }
    await load()
  } catch (e) {
    if (e.code === 3010) {
      // 冲突时把最新数据重新拉回，避免用户对着旧值反复重试
      toast.error('保存冲突', `${e.message}。已为你刷新最新数据，请核对后重新录入`)
      await load()
    } else {
      toast.error('暂存失败', e.message)
    }
  } finally {
    saving.value = false
  }
}

function openImport() {
  importOpen.value = true
  importError.value = ''
  importFile.value = null
  importResult.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function closeImport() {
  if (importing.value) return
  importOpen.value = false
  importFile.value = null
  importError.value = ''
  importResult.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function onPick(ev) {
  const picked = ev.target.files?.[0]
  importError.value = ''
  importResult.value = null
  if (!picked) {
    importFile.value = null
    return
  }
  if (!picked.name.toLowerCase().endsWith('.xlsx')) {
    importError.value = '仅支持 .xlsx 文件（Excel 2007 及以上），请另存为 .xlsx 后重试'
    importFile.value = null
    ev.target.value = ''
    return
  }
  importFile.value = picked
}

async function submitImport() {
  if (!importFile.value) return
  importing.value = true
  importError.value = ''
  importResult.value = null
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    const data = await importScores(courseId, fd)
    importResult.value = data
    // 导入推进了行版本号，必须重新拉取，否则后续「暂存」会拿旧版本撞 3010
    await load()
    if (data.successCount > 0) {
      toast.success(
        `已导入 ${data.successCount} 条成绩`,
        data.failures.length ? `另有 ${data.failures.length} 行失败，见明细` : '全部写入成功'
      )
    } else {
      toast.error('没有成绩被写入', `${data.failures.length} 行全部校验失败，请按明细修正后重试`)
    }
  } catch (e) {
    importError.value = e.message
    if (e.code === 3003) await load()
  } finally {
    importing.value = false
  }
}

onMounted(load)
</script>
