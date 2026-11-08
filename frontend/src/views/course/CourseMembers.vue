<template>
  <div>
    <div class="page-head">
      <div>
        <h2>课程成员</h2>
        <p class="sub">按「所属班级」分组呈现；已最终确认的课程整体只读，不可增删成员。</p>
      </div>
      <div>
        <button class="btn btn-p" :disabled="confirmed || !course" @click="openPicker">
          <Icon name="plus" /> 从共享目录选人
        </button>
      </div>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>
    <div v-if="confirmed" class="banner b-amber mt-12">
      <Icon name="lock" /><span>该课程已最终确认，成员与成绩均已锁定，不可再修改。</span>
    </div>

    <div v-if="loading" class="card mt-16">
      <div class="empty"><div class="ico"><Icon name="group" /></div><h4>正在加载成员…</h4></div>
    </div>

    <div v-else-if="groups.length === 0" class="card mt-16">
      <div class="empty">
        <div class="ico"><Icon name="group" /></div>
        <h4>该课程暂无成员</h4>
        <p>点击右上角「从共享目录选人」，按班级批量或单人加入。</p>
      </div>
    </div>

    <section v-for="g in groups" :key="g.classId" class="card mt-14">
      <div class="card-h">
        <h3><Icon name="folder" /> 班级：{{ g.className }}</h3>
        <span class="chip" :class="confirmed ? 'c-slate' : 'c-sky'">
          {{ confirmed ? '已确认 · 只读' : '草稿中 · 可调整' }}
        </span>
        <span class="ml-auto muted">成员 {{ g.members.length }} 人</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="width: 160px">学号</th>
              <th>姓名</th>
              <th style="width: 120px; text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in g.members" :key="m.studentId">
              <td class="mono">{{ m.studentNo }}</td>
              <td style="font-weight: 500">{{ m.studentName }}</td>
              <td style="text-align: right">
                <button class="icon-btn danger" :title="confirmed ? '课程已确认 · 点击查看原因' : '移除成员'" @click="askRemove(m)">
                  <Icon name="close" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="banner b-sky mt-14" v-if="groups.length">
      <Icon name="info" />
      <span>成员未锁定时可移除；移除只影响本课程，不影响共享目录中的学生档案。</span>
    </div>

    <!-- 选人抽屉 -->
    <div v-if="pickerOpen" class="dmask" @click.self="pickerOpen = false">
      <aside class="drawer">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="group" /></div>
          <div>
            <h3>从共享目录选人</h3>
            <div class="m-sub">可搜索单个加入，也可勾选多名批量加入。</div>
          </div>
        </div>

        <div style="padding: 16px 20px; border-bottom: 1px solid #f1f5f9">
          <label class="search-box" style="width: 100%">
            <Icon name="search" />
            <input v-model.trim="pickerKeyword" class="input" placeholder="按学号前缀或姓名片段搜索" @keyup.enter="searchCandidates" />
          </label>
          <div class="row-flex mt-12">
            <button class="btn btn-o btn-sm" @click="searchCandidates">搜索</button>
            <button
              class="btn btn-o btn-sm"
              :disabled="selectableCandidates.length === 0"
              :title="allSelected ? '取消当前列表的全部勾选' : '勾选当前列表中所有未加入本课程的学生'"
              @click="toggleAll"
            >
              <Icon :name="allSelected ? 'close' : 'check'" />
              {{ allSelected ? '取消全选' : '全选' }}
            </button>
            <button v-if="picked.length" class="btn btn-o btn-sm" @click="picked = []">清空已选</button>
            <span class="muted">已选 {{ picked.length }} 人 · 可选 {{ selectableCandidates.length }} 人</span>
          </div>
          <p v-if="candidates.length >= PICKER_PAGE_SIZE" class="small mt-8" style="margin-bottom: 0">
            列表最多显示 {{ PICKER_PAGE_SIZE }} 人，输入关键词缩小范围后再全选，避免漏选。
          </p>
        </div>

        <div style="flex: 1; overflow: auto">
          <div v-if="candidates.length === 0" class="empty" style="padding: 32px 20px">
            <div class="ico"><Icon name="search" /></div>
            <h4>没有可加入的学生</h4>
            <p>换一个关键词，或先去共享目录添加学生。</p>
          </div>

          <!-- 按班级折叠分组：默认收起，展开哪个班看哪个班；组头吸顶，加入按钮常驻底部 -->
          <template v-else>
            <div v-for="g in candidateGroups" :key="g.className" class="pick-group">
              <div class="pick-group-h" @click="toggleGroup(g.className)">
                <input
                  type="checkbox"
                  :checked="isGroupAllPicked(g)"
                  :indeterminate="isGroupSomePicked(g)"
                  :disabled="g.selectableIds.length === 0"
                  title="全选 / 取消全选本班"
                  @click.stop
                  @change="toggleGroupAll(g, $event.target.checked)"
                />
                <span class="pick-name">{{ g.className }}</span>
                <span class="muted small">
                  {{ g.students.length }} 人 · 可选 {{ g.selectableIds.length }} · 已选 {{ g.pickedCount }}
                </span>
                <span class="ml-auto chev" :class="{ open: isExpanded(g.className) }">
                  <Icon name="chevron-down" />
                </span>
              </div>
              <table v-if="isExpanded(g.className)">
                <tbody>
                  <tr v-for="s in g.students" :key="s.id">
                    <td style="width: 42px; padding-left: 20px">
                      <input v-model="picked" type="checkbox" :value="s.id" :disabled="isMember(s.id)" />
                    </td>
                    <td class="mono" style="width: 110px">{{ s.studentNo }}</td>
                    <td>
                      <div style="font-weight: 500">{{ s.name }}</div>
                    </td>
                    <td class="right" style="padding-right: 20px">
                      <span v-if="isMember(s.id)" class="chip c-slate">已在课程</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>

        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="pickerOpen = false">取消</button>
            <button class="btn btn-p" :disabled="picked.length === 0 || adding" @click="addPicked">
              {{ adding ? '加入中…' : `加入选中（${picked.length}）` }}
            </button>
          </div>
        </div>
      </aside>
    </div>

    <!-- 移除确认 -->
    <div v-if="removing" class="mask" @click.self="removing = null">
      <div class="modal" style="max-width: 420px">
        <div class="modal-h">
          <div class="m-ico" style="background: #fff1f2; color: #be123c"><Icon name="close" /></div>
          <div>
            <h3>移除课程成员</h3>
            <div class="m-sub">只解除本课程的成员关系，学生档案仍保留在共享目录。</div>
          </div>
        </div>
        <div class="modal-b">确认将「{{ removing.studentName }}（{{ removing.studentNo }}）」移出本课程？</div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="removing = null">取消</button>
            <button class="btn btn-danger" @click="doRemove">确认移除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Icon from '../../components/Icon.vue'
import { addMembers, listMembers, removeMember } from '../../api/course'
import { listCourses } from '../../api/course'
import { searchStudents } from '../../api/student'
import { useCourseContextStore } from '../../store/courseContext'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const ctx = useCourseContextStore()
const toast = useToast()

const courseId = Number(route.params.id)
const course = ref(null)
const confirmed = computed(() => course.value?.status === 'CONFIRMED')

const members = ref([])
const loading = ref(false)
const error = ref('')

const pickerOpen = ref(false)
const pickerKeyword = ref('')
const candidates = ref([])
const picked = ref([])
const adding = ref(false)
const removing = ref(null)

const memberIds = computed(() => new Set(members.value.map((m) => m.studentId)))

// 抽屉一次最多列出的人数：一个班通常 40—60 人，原来的 50 会漏选，200 够覆盖多个班
const PICKER_PAGE_SIZE = 200

// 可勾选的候选人：已在课程中的置灰不可选，全选也必须排除它们
const selectableCandidates = computed(() => candidates.value.filter((s) => !isMember(s.id)))

// 候选人按班级折叠分组：组头 = 本班全选 checkbox + 班级名 + 计数，默认收起
const candidateGroups = computed(() => {
  const map = new Map()
  for (const s of candidates.value) {
    const key = s.className || '未分班'
    if (!map.has(key)) map.set(key, { className: key, students: [] })
    map.get(key).students.push(s)
  }
  return [...map.values()]
    .map((g) => ({
      ...g,
      selectableIds: g.students.filter((s) => !isMember(s.id)).map((s) => s.id),
      pickedCount: g.students.filter((s) => picked.value.includes(s.id)).length
    }))
    .sort((a, b) => String(a.className).localeCompare(String(b.className), 'zh'))
})

const expandedGroups = ref(new Set())

function isExpanded(className) {
  return expandedGroups.value.has(className)
}

function toggleGroup(className) {
  const next = new Set(expandedGroups.value)
  if (next.has(className)) next.delete(className)
  else next.add(className)
  expandedGroups.value = next
}

// 搜索后重置展开状态：结果只有一个班时自动展开，多个班默认全收起（按需展开）
function initExpandedGroups() {
  expandedGroups.value = new Set(
    candidateGroups.value.length === 1 ? [candidateGroups.value[0].className] : []
  )
}

function isGroupAllPicked(g) {
  return g.selectableIds.length > 0 && g.selectableIds.every((id) => picked.value.includes(id))
}

function isGroupSomePicked(g) {
  const count = g.selectableIds.filter((id) => picked.value.includes(id)).length
  return count > 0 && count < g.selectableIds.length
}

function toggleGroupAll(g, checked) {
  if (checked) {
    picked.value = [...new Set([...picked.value, ...g.selectableIds])]
  } else {
    picked.value = picked.value.filter((id) => !g.selectableIds.includes(id))
  }
}

const allSelected = computed(() => {
  const ids = selectableCandidates.value.map((s) => s.id)
  return ids.length > 0 && ids.every((id) => picked.value.includes(id))
})

// 全选 / 取消全选：只作用于当前列表，不会把已加入课程的成员重复塞进 picked
function toggleAll() {
  picked.value = allSelected.value ? [] : selectableCandidates.value.map((s) => s.id)
}

const groups = computed(() => {
  const map = new Map()
  for (const m of members.value) {
    const key = m.classId ?? 0
    if (!map.has(key)) map.set(key, { classId: key, className: m.className || '未分班', members: [] })
    map.get(key).members.push(m)
  }
  return [...map.values()].sort((a, b) => String(a.className).localeCompare(String(b.className), 'zh'))
})

function syncContext() {
  if (ctx.id === courseId) return
  ctx.apply({ id: courseId, name: course.value?.name || `课程 #${courseId}`, term: course.value?.term || '' })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [list, courses] = await Promise.all([listMembers(courseId), listCourses()])
    members.value = list
    course.value = courses.find((c) => c.id === courseId) || null
    syncContext()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function isMember(id) {
  return memberIds.value.has(id)
}

async function openPicker() {
  pickerOpen.value = true
  picked.value = []
  pickerKeyword.value = ''
  await searchCandidates()
}

async function searchCandidates() {
  try {
    const data = await searchStudents({
      page: 1,
      size: PICKER_PAGE_SIZE,
      keyword: pickerKeyword.value || undefined
    })
    candidates.value = data.records
    initExpandedGroups()
  } catch (e) {
    toast.error('搜索失败', e.message)
  }
}

async function addPicked() {
  adding.value = true
  try {
    const data = await addMembers(courseId, picked.value)
    const skipped = data.skipped || 0
    // 跳过有两种原因：已在课程中、或目标班级分组已确认发布（成绩只读，不能再加人）
    const locked = data.skippedConfirmed || 0
    const reasons = []
    if (skipped - locked > 0) reasons.push(`${skipped - locked} 名已在课程中`)
    if (locked > 0) reasons.push(`${locked} 名所在班级分组成绩已确认发布，不能加入`)
    const detail = reasons.join('；')
    if (data.added === 0 && skipped > 0) {
      toast.success('没有新增成员', detail)
    } else if (skipped > 0) {
      toast.success(`已加入 ${data.added} 名学生`, detail)
    } else {
      toast.success(`已加入 ${data.added} 名学生`)
    }
    picked.value = []
    pickerOpen.value = false
    await load()
  } catch (e) {
    toast.error('加入失败', e.message)
  } finally {
    adding.value = false
  }
}

function askRemove(m) {
  // 与 CourseList 同一套口径：禁用按钮点不动、也不显示 title，用户只会觉得「坏了」
  if (confirmed.value) {
    toast.error('该课程已确认发布，不能移除成员', '成绩已对学生公布，成员名单整体只读。')
    return
  }
  removing.value = m
}

async function doRemove() {
  try {
    await removeMember(courseId, removing.value.studentId)
    toast.success('成员已移除')
    removing.value = null
    await load()
  } catch (e) {
    toast.error('移除失败', e.message)
    removing.value = null
  }
}

onMounted(load)
</script>

<style scoped>
/* 折叠分组：组头吸顶，滚动列表时始终知道当前在哪个班 */
.pick-group {
  border-bottom: 1px solid #f1f5f9;
}

.pick-group-h {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  cursor: pointer;
  background: #f8fafc;
  position: sticky;
  top: 0;
  z-index: 1;
}

.pick-group-h:hover {
  background: #f1f5f9;
}

.pick-name {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}

.chev {
  display: flex;
  color: #94a3b8;
  transition: transform 0.15s;
}

.chev.open {
  transform: rotate(180deg);
}
</style>
