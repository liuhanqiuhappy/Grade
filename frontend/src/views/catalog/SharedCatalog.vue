<template>
  <div>
    <div class="page-head">
      <div>
        <h2>共享目录 · {{ tab === 'class' ? '班级视图' : '学生视图' }}</h2>
        <p class="sub">
          {{ tab === 'class'
            ? '班级为全校共享目录：可新建、重命名；仅「空」班级可删除。'
            : '学生在班级内建档；支持单个添加与 Excel 批量导入。' }}
        </p>
      </div>
    </div>

    <!-- 视图切换 + 搜索 -->
    <div class="row-flex mt-16">
      <div class="tabgroup">
        <span class="tabitem" :class="{ on: tab === 'class' }" @click="switchTab('class')">
          <Icon name="folder" /> 班级视图
        </span>
        <span class="tabitem" :class="{ on: tab === 'student' }" @click="switchTab('student')">
          <Icon name="group" /> 学生视图
        </span>
      </div>

      <label class="search-box" style="width: 300px">
        <Icon name="search" />
        <input
          v-model.trim="keyword"
          class="input"
          type="text"
          :placeholder="tab === 'class' ? '按班级名称搜索' : '按学号前缀或姓名片段搜索'"
          @keyup.enter="reload"
        />
      </label>
      <button class="btn btn-o btn-sm" @click="reload">搜索</button>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>

    <!-- ========== 班级视图 ========== -->
    <template v-if="tab === 'class'">
      <div class="row-flex mt-16">
        <button class="btn btn-p" @click="openClassForm(null)"><Icon name="plus" /> 新建班级</button>
        <span class="muted">共 {{ classTotal }} 个班级；「空」班级才可删除，非空班级删除将被服务端拒绝。</span>
      </div>

      <section class="card mt-16">
        <div v-if="loadingClass" class="empty">
          <div class="ico"><Icon name="folder" /></div><h4>正在加载班级…</h4>
        </div>
        <div v-else-if="classes.length === 0" class="empty">
          <div class="ico"><Icon name="folder" /></div>
          <h4>目录中暂无班级</h4>
          <p>先新建一个班级，再进入学生视图添加学生。</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>班级</th>
                <th style="width: 160px">创建时间</th>
                <th style="width: 100px">学生数</th>
                <th style="width: 110px">状态</th>
                <th style="width: 240px; text-align: right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in classes" :key="c.id">
                <td style="font-weight: 500">{{ c.name }}</td>
                <td class="muted">{{ c.createdAt }}</td>
                <td>{{ c.studentCount }} 人</td>
                <td>
                  <span v-if="currentClassId === c.id" class="chip c-indigo">已选定</span>
                  <span v-else-if="c.studentCount === 0" class="chip c-slate">空</span>
                  <span v-else class="chip c-sky">在用</span>
                </td>
                <td style="text-align: right">
                  <div style="display: inline-flex; gap: 8px">
                    <button class="icon-btn" title="重命名" @click="openClassForm(c)"><Icon name="pencil" /></button>
                    <button class="icon-btn danger" title="删除班级" @click="askDeleteClass(c)"><Icon name="trash" /></button>
                    <button class="btn btn-sm btn-o" @click="viewStudents(c)">查看学生</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <!-- ========== 学生视图 ========== -->
    <template v-else>
      <div class="row-flex mt-16">
        <select v-model="currentClassId" class="select" style="width: 220px" @change="loadStudents">
          <option :value="null" disabled>请选择班级</option>
          <option v-for="c in classes" :key="c.id" :value="c.id">{{ c.name }}（{{ c.studentCount }} 人）</option>
        </select>
        <button class="btn btn-p" :disabled="!currentClassId" @click="openStudentForm">
          <Icon name="plus" /> 添加学生
        </button>
        <button class="btn btn-o" :disabled="!currentClassId" @click="openImport">
          <Icon name="upload" /> 批量导入
        </button>
        <span class="muted">共 {{ studentTotal }} 名学生。</span>
      </div>

      <section class="card mt-16">
        <div v-if="!currentClassId" class="empty">
          <div class="ico"><Icon name="group" /></div>
          <h4>请先选择班级</h4>
          <p>学生按班级建档，选择班级后即可查看名单、单个添加或批量导入。</p>
        </div>
        <div v-else-if="loadingStudent" class="empty">
          <div class="ico"><Icon name="group" /></div><h4>正在加载学生…</h4>
        </div>
        <div v-else-if="students.length === 0" class="empty">
          <div class="ico"><Icon name="group" /></div>
          <h4>该班暂无学生</h4>
          <p>可以「添加学生」逐个建档，或用「批量导入」一次导入整班名单。</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 150px">学号</th>
                <th>姓名</th>
                <th style="width: 90px">性别</th>
                <th style="width: 180px">班级</th>
                <th style="width: 140px; text-align: right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in students" :key="s.id">
                <td class="mono">{{ s.studentNo }}</td>
                <td style="font-weight: 500">{{ s.name }}</td>
                <td>{{ s.gender || '—' }}</td>
                <td>{{ s.className }}</td>
                <td style="text-align: right">
                  <div style="display: inline-flex; gap: 8px">
                    <button class="icon-btn" title="修改资料 / 转班" @click="openStudentForm(s)"><Icon name="pencil" /></button>
                    <button class="icon-btn danger" title="删除学生" @click="askDeleteStudent(s)"><Icon name="trash" /></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页：一次拉全量会随班级人数无限变长，改为每页 20 人按需翻页 -->
        <div v-if="students.length" class="row-flex" style="justify-content: flex-end; padding: 12px 20px">
          <button class="btn btn-sm btn-o" :disabled="studentPage <= 1" @click="changeStudentPage(studentPage - 1)">上一页</button>
          <span class="small">第 {{ studentPage }} / {{ studentPageCount }} 页</span>
          <button class="btn btn-sm btn-o" :disabled="studentPage >= studentPageCount" @click="changeStudentPage(studentPage + 1)">下一页</button>
        </div>
      </section>
    </template>

    <!-- 班级新建/重命名 -->
    <div v-if="classForm" class="mask" @click.self="classForm = null">
      <div class="modal" style="max-width: 420px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="folder" /></div>
          <div>
            <h3>{{ classForm.id ? '重命名班级' : '新建班级' }}</h3>
            <div class="m-sub">班级名称全局唯一。</div>
          </div>
        </div>
        <div class="modal-b">
          <label class="label">班级名称<span class="req">*</span></label>
          <input v-model.trim="classForm.name" class="input" :class="{ err: classFormError }" placeholder="如：计算机2201班" />
          <p v-if="classFormError" class="hint err">{{ classFormError }}</p>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="classForm = null">取消</button>
            <button class="btn btn-p" :disabled="submitting" @click="saveClass">{{ submitting ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 学生新增/编辑 -->
    <div v-if="studentForm" class="mask" @click.self="studentForm = null">
      <div class="modal" style="max-width: 460px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="account" /></div>
          <div>
            <h3>{{ studentForm.id ? '修改学生资料' : '添加学生' }}</h3>
            <div class="m-sub">学号在全校范围内唯一；转班修改所属班级即可。</div>
          </div>
        </div>
        <div class="modal-b">
          <label class="label">学号<span class="req">*</span></label>
          <input v-model.trim="studentForm.studentNo" class="input" :class="{ err: studentFormError.studentNo }" placeholder="如：20220101" />
          <p v-if="studentFormError.studentNo" class="hint err">{{ studentFormError.studentNo }}</p>

          <div class="mt-12">
            <label class="label">姓名<span class="req">*</span></label>
            <input v-model.trim="studentForm.name" class="input" :class="{ err: studentFormError.name }" placeholder="如：陈嘉怡" />
            <p v-if="studentFormError.name" class="hint err">{{ studentFormError.name }}</p>
          </div>

          <div class="mt-12">
            <label class="label">性别</label>
            <select v-model="studentForm.gender" class="select">
              <option value="">未填写</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>

          <div class="mt-12">
            <label class="label">所属班级<span class="req">*</span></label>
            <select v-model="studentForm.classId" class="select">
              <option v-for="c in classes" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div v-if="studentFormError.general" class="banner b-rose mt-12">
            <Icon name="alert" /><span>{{ studentFormError.general }}</span>
          </div>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="studentForm = null">取消</button>
            <button class="btn btn-p" :disabled="submitting" @click="saveStudent">{{ submitting ? '保存中…' : '保存' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量导入 -->
    <div v-if="importOpen" class="mask" @click.self="closeImport">
      <div class="modal" style="max-width: 560px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="upload" /></div>
          <div>
            <h3>批量导入学生</h3>
            <div class="m-sub">仅支持 .xlsx；首行为表头，从第 2 行开始读取。</div>
          </div>
        </div>

        <div class="modal-b">
          <div class="banner b-slate">
            <Icon name="info" />
            <span>
              列顺序固定为 <b>A 列学号 · B 列姓名 · C 列性别</b>（性别可空）。<br />
              学号已存在、文件内重复、必填为空的行会被跳过并逐行报告，其余行照常导入。
            </span>
          </div>

          <div class="mt-12">
            <label class="label">选择文件<span class="req">*</span></label>
            <input
              ref="fileInput"
              class="input"
              style="padding-top: 6px"
              type="file"
              accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              @change="onFileChange"
            />
            <p class="hint">文件大小上限 5MB；不支持 .xls / .csv。</p>
            <p v-if="importError" class="hint err">{{ importError }}</p>
          </div>

          <div v-if="importResult" class="mt-12">
            <div class="banner" :class="importResult.failures.length ? 'b-amber' : 'b-green'">
              <Icon :name="importResult.failures.length ? 'alert' : 'check-circle'" />
              <span>
                导入完成：成功 <b>{{ importResult.successCount }}</b> 条，
                失败 <b>{{ importResult.failures.length }}</b> 条。
              </span>
            </div>

            <div v-if="importResult.failures.length" class="card mt-12" style="max-height: 200px; overflow: auto">
              <table>
                <thead><tr><th style="width: 90px">Excel 行号</th><th>失败原因</th></tr></thead>
                <tbody>
                  <tr v-for="f in importResult.failures" :key="f.row + f.reason">
                    <td class="mono">第 {{ f.row }} 行</td>
                    <td>{{ f.reason }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="modal-f">
          <span class="muted">目标班级：{{ currentClassName || '未选择' }}</span>
          <div class="r">
            <button class="btn btn-o" @click="closeImport">关闭</button>
            <button class="btn btn-p" :disabled="!importFile || importing" @click="doImport">
              {{ importing ? '导入中…' : '开始导入' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认 -->
    <div v-if="deleting" class="mask" @click.self="deleting = null">
      <div class="modal" style="max-width: 420px">
        <div class="modal-h">
          <div class="m-ico" style="background: #fff1f2; color: #be123c"><Icon name="trash" /></div>
          <div>
            <h3>{{ deleting.type === 'class' ? '删除班级' : '删除学生' }}</h3>
            <div class="m-sub">{{ deleting.hint }}</div>
          </div>
        </div>
        <div class="modal-b">{{ deleting.text }}</div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="deleting = null">取消</button>
            <button class="btn btn-danger" @click="doDelete">确认删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '../../components/Icon.vue'
import { createClass, deleteClass, listClasses, updateClass } from '../../api/clazz'
import {
  createStudent,
  deleteStudent,
  importStudents,
  listStudentsOfClass,
  searchStudents,
  updateStudent
} from '../../api/student'
import { useToast } from '../../composables/useToast'

const toast = useToast()

const tab = ref('class')
const keyword = ref('')
const error = ref('')

const classes = ref([])
const classTotal = ref(0)
const loadingClass = ref(false)

const currentClassId = ref(null)
const students = ref([])
const studentTotal = ref(0)
const loadingStudent = ref(false)

// 学生列表分页：每页 20 人。以前一次拉 100 人，大班列表无限变长且超出部分直接看不到
const STUDENT_PAGE_SIZE = 20
const studentPage = ref(1)
const studentPageCount = computed(() => Math.max(1, Math.ceil(studentTotal.value / STUDENT_PAGE_SIZE)))

const submitting = ref(false)
const classForm = ref(null)
const classFormError = ref('')
const studentForm = ref(null)
const studentFormError = reactive({ studentNo: '', name: '', general: '' })
const deleting = ref(null)

const importOpen = ref(false)
const importFile = ref(null)
const importError = ref('')
const importResult = ref(null)
const importing = ref(false)
const fileInput = ref(null)

const currentClassName = computed(
  () => classes.value.find((c) => c.id === currentClassId.value)?.name || ''
)

async function loadClasses() {
  loadingClass.value = true
  error.value = ''
  try {
    const data = await listClasses({ page: 1, size: 100, keyword: keyword.value || undefined })
    classes.value = data.records
    classTotal.value = data.total
    if (currentClassId.value && !classes.value.some((c) => c.id === currentClassId.value)) {
      currentClassId.value = null
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loadingClass.value = false
  }
}

async function loadStudents() {
  if (!currentClassId.value) return
  loadingStudent.value = true
  error.value = ''
  try {
    const data = await searchStudents({
      classId: currentClassId.value,
      page: studentPage.value,
      size: STUDENT_PAGE_SIZE,
      keyword: keyword.value || undefined
    })
    students.value = data.records
    studentTotal.value = data.total
    // 删除等操作后当前页可能已超出总页数，回退一页重拉，避免停在空页
    if (data.records.length === 0 && studentPage.value > 1 && studentPage.value > studentPageCount.value) {
      studentPage.value = studentPageCount.value
      loadingStudent.value = false
      await loadStudents()
      return
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loadingStudent.value = false
  }
}

function changeStudentPage(next) {
  if (next < 1 || next > studentPageCount.value) return
  studentPage.value = next
  loadStudents()
}

function reload() {
  if (tab.value === 'class') loadClasses()
  else {
    studentPage.value = 1
    loadStudents()
  }
}

function switchTab(next) {
  tab.value = next
  keyword.value = ''
  if (next === 'class') {
    loadClasses()
  } else if (currentClassId.value) {
    studentPage.value = 1
    loadStudents()
  }
}

function viewStudents(c) {
  currentClassId.value = c.id
  tab.value = 'student'
  keyword.value = ''
  studentPage.value = 1
  loadStudents()
}

function openClassForm(c) {
  classFormError.value = ''
  classForm.value = c ? { id: c.id, name: c.name } : { id: null, name: '' }
}

async function saveClass() {
  classFormError.value = classForm.value.name ? '' : '班级名称不能为空'
  if (classFormError.value) return
  submitting.value = true
  try {
    if (classForm.value.id) {
      await updateClass(classForm.value.id, { name: classForm.value.name })
      toast.success('班级已重命名')
    } else {
      await createClass({ name: classForm.value.name })
      toast.success('班级已创建')
    }
    classForm.value = null
    await loadClasses()
  } catch (e) {
    classFormError.value = e.message
  } finally {
    submitting.value = false
  }
}

function openStudentForm(s) {
  Object.assign(studentFormError, { studentNo: '', name: '', general: '' })
  studentForm.value = s
    ? { id: s.id, studentNo: s.studentNo, name: s.name, gender: s.gender || '', classId: s.classId }
    : { id: null, studentNo: '', name: '', gender: '', classId: currentClassId.value }
}

async function saveStudent() {
  studentFormError.studentNo = studentForm.value.studentNo ? '' : '学号不能为空'
  studentFormError.name = studentForm.value.name ? '' : '姓名不能为空'
  studentFormError.general = ''
  if (studentFormError.studentNo || studentFormError.name) return

  submitting.value = true
  try {
    const payload = {
      studentNo: studentForm.value.studentNo,
      name: studentForm.value.name,
      gender: studentForm.value.gender || null,
      classId: studentForm.value.classId
    }
    if (studentForm.value.id) {
      await updateStudent(studentForm.value.id, payload)
      toast.success('学生资料已更新')
    } else {
      await createStudent(payload.classId, payload)
      toast.success('学生已添加')
    }
    studentForm.value = null
    await Promise.all([loadStudents(), loadClasses()])
  } catch (e) {
    studentFormError.general = e.message
  } finally {
    submitting.value = false
  }
}

function askDeleteClass(c) {
  deleting.value = {
    type: 'class',
    id: c.id,
    hint: '仅空班级可删除；非空班级会被服务端拒绝。',
    text: `确认删除班级「${c.name}」？当前 ${c.studentCount} 名学生。`
  }
}

function askDeleteStudent(s) {
  deleting.value = {
    type: 'student',
    id: s.id,
    hint: '已有成绩或课程成员关系的学生无法删除。',
    text: `确认删除学生「${s.name}（${s.studentNo}）」？`
  }
}

async function doDelete() {
  try {
    if (deleting.value.type === 'class') {
      await deleteClass(deleting.value.id)
      toast.success('班级已删除')
    } else {
      await deleteStudent(deleting.value.id)
      toast.success('学生已删除')
    }
    deleting.value = null
    await loadClasses()
    if (tab.value === 'student' && currentClassId.value) await loadStudents()
  } catch (e) {
    toast.error('删除失败', e.message)
    deleting.value = null
  }
}

/* ===== 批量导入 ===== */
function openImport() {
  importOpen.value = true
  importFile.value = null
  importError.value = ''
  importResult.value = null
}

function closeImport() {
  importOpen.value = false
  if (importResult.value && importResult.value.successCount > 0) {
    loadStudents()
    loadClasses()
  }
}

function onFileChange(e) {
  importError.value = ''
  importResult.value = null
  const file = e.target.files && e.target.files[0]
  if (!file) {
    importFile.value = null
    return
  }
  // 客户端先做一道格式校验，减少无谓请求
  if (!/\.xlsx$/i.test(file.name)) {
    importError.value = '文件格式不支持：仅接受 .xlsx（Excel 2007 及以上）。请另存为 .xlsx 后重试。'
    importFile.value = null
    e.target.value = ''
    return
  }
  if (file.size === 0) {
    importError.value = '文件内容为空，请检查导出的 Excel。'
    importFile.value = null
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    importError.value = '文件超过 5MB 上限，请拆分后分批导入。'
    importFile.value = null
    return
  }
  importFile.value = file
}

async function doImport() {
  if (!importFile.value) return
  importing.value = true
  importError.value = ''
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    const data = await importStudents(currentClassId.value, fd)
    importResult.value = data
    if (data.successCount > 0) {
      toast.success(`已导入 ${data.successCount} 名学生`, data.failures.length ? `${data.failures.length} 行被跳过` : '')
    } else {
      toast.error('没有学生被导入', '请检查文件内容与列顺序')
    }
  } catch (e) {
    // 2006 = Excel 解析失败，其余按后端 message 展示
    importError.value = e.code === 2006
      ? `文件解析失败：${e.message}`
      : e.message || '导入失败，请稍后重试'
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  await loadClasses()
  if (classes.value.length > 0) {
    currentClassId.value = classes.value[0].id
  }
})
</script>
