<template>
  <div>
    <div class="page-head">
      <div>
        <h2>我的课程</h2>
        <p class="sub">仅列出本人创建的课程；点击「进入课程」写入课程上下文，课程内任务页共享该上下文。</p>
      </div>
      <div>
        <button class="btn btn-p" @click="openCreate"><Icon name="plus" /> 新建课程</button>
      </div>
    </div>

    <div v-if="error" class="banner b-rose mt-12">
      <Icon name="alert" /><span>{{ error }}</span>
    </div>

    <section class="card mt-16">
      <div v-if="loading" class="empty">
        <div class="ico"><Icon name="book" /></div>
        <h4>正在加载课程…</h4>
      </div>

      <div v-else-if="rows.length === 0" class="empty">
        <div class="ico"><Icon name="book" /></div>
        <h4>还没有课程</h4>
        <p>点击右上角「新建课程」创建本学期课程，之后可加入成员并录入成绩。</p>
      </div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="width: 110px">课程 ID</th>
              <th>课程名称</th>
              <th style="width: 180px">学期</th>
              <th style="width: 100px">成员数</th>
              <th style="width: 120px">状态</th>
              <th style="width: 210px; text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id">
              <td class="mono">C{{ String(item.id).padStart(3, '0') }}</td>
              <td style="font-weight: 500">{{ item.name }}</td>
              <td>{{ item.term }}</td>
              <td>{{ item.memberCount }} 人</td>
              <td>
                <span v-if="ctx.id === item.id" class="chip c-indigo">当前课程</span>
                <span v-else-if="item.status === 'CONFIRMED'" class="chip c-green">已确认</span>
                <span v-else class="chip c-slate">草稿</span>
              </td>
              <td style="text-align: right">
                <div style="display: inline-flex; gap: 8px; align-items: center">
                  <button class="btn btn-sm btn-o" @click="enter(item)">进入课程</button>
                  <button class="icon-btn" :title="item.status === 'CONFIRMED' ? '已确认发布 · 点击查看原因' : '编辑课程'" @click="openEdit(item)">
                    <Icon name="pencil" />
                  </button>
                  <button class="icon-btn danger" :title="item.status === 'CONFIRMED' ? '已确认发布 · 点击查看原因' : '删除课程'" @click="askDelete(item)">
                    <Icon name="trash" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <p class="muted mt-12">
      提示：「进入课程」后，课程成员 / 成绩录入 / 确认与统计均针对当前课程；已确认的课程整体只读。
    </p>

    <!-- 新建 / 编辑 -->
    <div v-if="form" class="mask" @click.self="form = null">
      <div class="modal" style="max-width: 440px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="book" /></div>
          <div>
            <h3>{{ form.id ? '编辑课程' : '新建课程' }}</h3>
            <div class="m-sub">课程名称与学期组合唯一，重复将由服务端拒绝。</div>
          </div>
        </div>
        <div class="modal-b">
          <label class="label">课程名称<span class="req">*</span></label>
          <input v-model.trim="form.name" class="input" :class="{ err: formError.name }" placeholder="如：数据结构与算法" />
          <p v-if="formError.name" class="hint err">{{ formError.name }}</p>

          <div class="mt-12">
            <label class="label">学期<span class="req">*</span></label>
            <input v-model.trim="form.term" class="input" :class="{ err: formError.term }" placeholder="如：2026-2027-1" />
            <p v-if="formError.term" class="hint err">{{ formError.term }}</p>
          </div>

          <div v-if="formError.general" class="banner b-rose mt-12">
            <Icon name="alert" /><span>{{ formError.general }}</span>
          </div>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="form = null">取消</button>
            <button class="btn btn-p" :disabled="submitting" @click="save">
              {{ submitting ? '保存中…' : '保存' }}
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
            <h3>删除课程</h3>
            <div class="m-sub">该操作会同时移除课程成员关系，且不可撤销。</div>
          </div>
        </div>
        <div class="modal-b">
          确认删除课程「<b>{{ deleting.name }}</b>」？当前成员 {{ deleting.memberCount }} 人。
        </div>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '../../components/Icon.vue'
import { createCourse, deleteCourse, listCourses, updateCourse } from '../../api/course'
import { useCourseContextStore } from '../../store/courseContext'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const ctx = useCourseContextStore()
const toast = useToast()

const rows = ref([])
const loading = ref(false)
const error = ref('')
const form = ref(null)
const formError = reactive({ name: '', term: '', general: '' })
const submitting = ref(false)
const deleting = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await listCourses()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(formError, { name: '', term: '', general: '' })
  form.value = { id: null, name: '', term: '' }
}

/**
 * 已确认课程整体只读（后端也会以 3003 拒绝）。
 * 不用 :disabled——disabled 元素不派发 click、浏览器也不显示 title，
 * 结果就是「点了毫无反应」。改为可点击但拦截，并明确告知原因。
 */
function blockedByConfirmed(item, action) {
  if (item.status !== 'CONFIRMED') return false
  toast.error(
    `该课程已确认发布，不能${action}`,
    '成绩已对学生公布，课程整体只读；如为误确认产生的测试课程，请联系管理员处理。'
  )
  return true
}

function openEdit(item) {
  if (blockedByConfirmed(item, '编辑')) return
  Object.assign(formError, { name: '', term: '', general: '' })
  form.value = { id: item.id, name: item.name, term: item.term }
}

async function save() {
  formError.name = form.value.name ? '' : '课程名称不能为空'
  formError.term = form.value.term ? '' : '学期不能为空'
  formError.general = ''
  if (formError.name || formError.term) return

  submitting.value = true
  try {
    const payload = { name: form.value.name, term: form.value.term }
    if (form.value.id) {
      await updateCourse(form.value.id, payload)
      toast.success('课程已更新')
    } else {
      await createCourse(payload)
      toast.success('课程已创建')
    }
    form.value = null
    await load()
  } catch (e) {
    formError.general = e.message
  } finally {
    submitting.value = false
  }
}

function askDelete(item) {
  if (blockedByConfirmed(item, '删除')) return
  deleting.value = item
}

async function doDelete() {
  try {
    await deleteCourse(deleting.value.id)
    if (ctx.id === deleting.value.id) ctx.clear()
    toast.success('课程已删除')
    deleting.value = null
    await load()
  } catch (e) {
    toast.error('删除失败', e.message)
    deleting.value = null
  }
}

function enter(item) {
  ctx.apply({ id: item.id, name: item.name, term: item.term })
  router.push({ name: 'course-members', params: { id: item.id } })
}

onMounted(load)
</script>
