<template>
  <div>
    <div class="page-head">
      <div>
        <h2>挂科名单与补考通知</h2>
        <p class="sub">
          名单来自「已确认发布」分组的成绩（低于 60 分，0 分计入）；发送后收件人即固化，学生需登录学生端才能看到站内通知。
        </p>
      </div>
      <div class="row-flex">
        <router-link class="btn btn-o" :to="{ name: 'confirm-stats', params: { id: courseId } }">
          <Icon name="table" /> 返回确认与统计
        </router-link>
      </div>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>

    <!-- 完全没有已确认分组：直接阻断，避免教师以为「没有挂科学生」 -->
    <div v-if="blocking" class="banner b-amber mt-12">
      <Icon name="alert" />
      <span>
        <b>{{ blocking }}</b>
        请先到「确认与统计」确认发布成绩——未公布的成绩不能用来生成名单，否则等于提前告知学生结果。
      </span>
    </div>
    <div v-else-if="warnTip" class="banner b-sky mt-12">
      <Icon name="info" /><span>{{ warnTip }}</span>
    </div>

    <template v-if="!blocking">
      <div class="stat-grid">
        <div class="stat-card">
          <div class="k">挂科人数</div>
          <div class="v">{{ list?.total ?? '—' }}</div>
          <div class="f"><span class="chip c-rose">低于 60 分（含 0）</span></div>
        </div>
        <div class="stat-card">
          <div class="k">可送达（有登录账号）</div>
          <div class="v">{{ deliverable }}</div>
          <div class="f">
            <span class="chip" :class="list?.noAccount ? 'c-amber' : 'c-green'">
              {{ list?.noAccount ? `${list.noAccount} 人无账号，发不达` : '全部已绑定账号' }}
            </span>
          </div>
        </div>
        <div class="stat-card">
          <div class="k">已通知过</div>
          <div class="v">{{ notifiedCount }}</div>
          <div class="f"><span class="chip c-indigo">本课程有未撤回通知覆盖</span></div>
        </div>
      </div>

      <section class="card mt-14">
        <div class="card-h">
          <h3><Icon name="group" /> 挂科学生名单</h3>
          <div class="row-flex ml-auto">
            <span
              v-for="g in groupTabs"
              :key="g.key"
              class="group-tab"
              :class="{ on: activeClassId === g.classId }"
              @click="switchGroup(g.classId)"
            >
              {{ g.className }} <span class="n">{{ g.count ?? '' }}</span>
            </span>
          </div>
        </div>

        <div v-if="loading" class="empty">
          <div class="ico"><Icon name="table" /></div><h4>正在统计…</h4>
        </div>
        <div v-else-if="!list || list.items.length === 0" class="empty">
          <div class="ico"><Icon name="check-circle" /></div>
          <h4>该范围没有挂科学生</h4>
          <p>{{ activeClassId ? '换个分组看看，或确认成绩是否已发布。' : '本次考试全部及格，无需发补考通知。' }}</p>
        </div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 150px">学号</th>
                <th>姓名</th>
                <th style="width: 150px">班级（快照）</th>
                <th style="width: 90px">分数</th>
                <th style="width: 150px">通知状态</th>
                <th style="width: 130px">账号</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in list.items" :key="r.studentId">
                <td class="mono">{{ r.studentNo }}</td>
                <td style="font-weight: 500">{{ r.studentName }}</td>
                <td class="muted">{{ r.className || '未分班' }}</td>
                <td><span class="chip c-rose">{{ r.score }}</span></td>
                <td>
                  <span class="chip" :class="r.notified ? 'c-green' : 'c-slate'">
                    {{ r.notified ? '已发送' : '未发送' }}
                  </span>
                </td>
                <td>
                  <span v-if="r.hasAccount" class="chip c-sky">已绑定</span>
                  <span v-else class="chip c-amber" title="该生没有登录账号，站内信无法送达">无账号</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="card mt-14">
        <div class="card-h">
          <h3><Icon name="bell" /> 发送补考通知</h3>
          <span class="chip c-indigo">{{ scopeLabel }}</span>
          <span class="muted ml-auto">
            收件人由服务端按上方名单生成，前端不能指定学生
          </span>
        </div>
        <div class="card-b">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 14px">
            <div>
              <label class="label">通知标题<span class="req">*</span></label>
              <input v-model.trim="form.title" class="input" maxlength="100" placeholder="补考安排通知" />
            </div>
            <div>
              <label class="label">补考时间</label>
              <input v-model="form.examTime" class="input" type="datetime-local" />
            </div>
            <div>
              <label class="label">考试地点</label>
              <input v-model.trim="form.examPlace" class="input" maxlength="100" placeholder="如：实验楼 302" />
            </div>
            <div>
              <label class="label">考试范围</label>
              <input v-model.trim="form.examScope" class="input" maxlength="500" placeholder="如：第 1—6 章，含实验报告" />
            </div>
          </div>
          <div class="mt-14">
            <label class="label">通知内容<span class="req">*</span></label>
            <textarea
              v-model="form.content"
              class="input"
              style="height: 92px; padding: 10px 12px; resize: vertical; line-height: 1.6"
              maxlength="1000"
              placeholder="请写明补考要求、需携带的证件与联系方式等"
            ></textarea>
            <div class="hint">最多 1000 字，当前 {{ form.content.length }} 字；内容将原样展示在学生端消息里。</div>
          </div>

          <div class="row-flex mt-14">
            <button
              class="btn btn-p"
              :disabled="submitting || !list || list.total === 0"
              @click="askSend"
            >
              <Icon name="bell" /> 发送给 {{ list?.total ?? 0 }} 名挂科学生
            </button>
            <span class="muted">
              {{ list?.noAccount ? `其中 ${list.noAccount} 人无登录账号，站内信发不达` : '全部可送达' }}
            </span>
          </div>
        </div>
      </section>
    </template>

    <section class="card mt-14">
      <div class="card-h">
        <h3><Icon name="table" /> 已发通知</h3>
        <span class="muted ml-auto">「已读」只统计学生真实点击，展开下拉不算</span>
      </div>
      <div v-if="notices.length === 0" class="empty">
        <div class="ico"><Icon name="bell" /></div>
        <h4>还没有发送过通知</h4>
        <p>发送后在这里查看送达与已读情况，也可以撤回。</p>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="width: 150px">发送时间</th>
              <th>标题</th>
              <th style="width: 150px">范围</th>
              <th style="width: 170px">补考时间</th>
              <th style="width: 130px">已读 / 收件</th>
              <th style="width: 90px">状态</th>
              <th style="width: 90px; text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="n in notices" :key="n.id">
              <td class="muted">{{ fmt(n.createdAt) }}</td>
              <td style="font-weight: 500">{{ n.title }}</td>
              <td class="muted">{{ n.className || '全课程' }}</td>
              <td class="muted">{{ n.examTime ? fmt(n.examTime) : '—' }}</td>
              <td>
                <div class="prog" style="width: 80px; display: inline-block; vertical-align: middle">
                  <i :style="{ width: readPercent(n) + '%' }"></i>
                </div>
                <span class="small" style="margin-left: 6px">{{ n.readCount }}/{{ n.totalRecipients }}</span>
              </td>
              <td>
                <span class="chip" :class="n.revoked ? 'c-slate' : 'c-green'">
                  {{ n.revoked ? '已撤回' : '已发送' }}
                </span>
              </td>
              <td style="text-align: right">
                <button
                  v-if="!n.revoked"
                  class="btn btn-sm btn-danger-o"
                  @click="revoking = n"
                >
                  撤回
                </button>
                <span v-else class="muted">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="banner b-slate mt-14">
      <Icon name="info" />
      <span>
        <b>口径说明：</b>挂科 = 已确认发布且分数低于 60（空值不算、0 分算）；分组名取学生加入课程时的班级快照，
        学生转班不影响历史名单与通知归属。撤回只让学生端不再展示，已读记录保留。
      </span>
    </div>

    <!-- 发送二次确认 -->
    <div v-if="confirming" class="mask" @click.self="confirming = false">
      <div class="modal" style="max-width: 480px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="bell" /></div>
          <div>
            <h3>发送补考通知</h3>
            <div class="m-sub">{{ scopeLabel }} · 共 {{ list?.total ?? 0 }} 名挂科学生</div>
          </div>
        </div>
        <div class="modal-b">
          <p>将向 <b>{{ deliverable }}</b> 名学生发送站内通知。</p>
          <p v-if="list?.noAccount" class="hint err" style="margin-top: 8px">
            {{ list.noAccount }} 人未绑定登录账号，收不到这条通知，请线下告知。
          </p>
          <p class="hint" style="margin-top: 8px">发送后收件人固化；如需修改内容，只能撤回后重新发送。</p>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="confirming = false">取消</button>
            <button class="btn btn-p" :disabled="submitting" @click="doSend">
              {{ submitting ? '发送中…' : '确认发送' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 撤回二次确认 -->
    <div v-if="revoking" class="mask" @click.self="revoking = null">
      <div class="modal" style="max-width: 440px">
        <div class="modal-h">
          <div class="m-ico" style="background: #fff1f2; color: #be123c"><Icon name="alert" /></div>
          <div>
            <h3>撤回「{{ revoking.title }}」</h3>
            <div class="m-sub">撤回后学生端不再展示，已读记录保留。</div>
          </div>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="revoking = null">取消</button>
            <button class="btn btn-danger" :disabled="submitting" @click="doRevoke">
              {{ submitting ? '处理中…' : '确认撤回' }}
            </button>
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
import { courseStatistics } from '../../api/score'
import { failedStudents, listNotices, revokeNotice, sendNotice } from '../../api/notice'
import { useCourseContextStore } from '../../store/courseContext'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const ctx = useCourseContextStore()
const toast = useToast()

const courseId = Number(route.params.id)
const stats = ref(null)
const list = ref(null)
const notices = ref([])
const loading = ref(false)
const error = ref('')
const blocking = ref('')
const warnTip = ref('')
const activeClassId = ref(null)
const confirming = ref(false)
const submitting = ref(false)
const revoking = ref(null)

const form = ref({ title: '补考安排通知', examTime: '', examPlace: '', examScope: '', content: '' })

const deliverable = computed(() => (list.value ? list.value.total - list.value.noAccount : 0))
const notifiedCount = computed(() => (list.value ? list.value.items.filter((i) => i.notified).length : 0))
const scopeLabel = computed(() => {
  if (activeClassId.value == null) return '全课程（仅已确认分组）'
  const g = (stats.value?.groups || []).find((x) => x.classId === activeClassId.value)
  return g ? g.className : '当前分组'
})
// 只把已确认发布的分组列成 tab：未确认组的学生本来就不在名单里，列出来只会误导
const groupTabs = computed(() => {
  const groups = (stats.value?.groups || []).filter((g) => g.confirmed && g.classId != null)
  return [
    { key: 'all', classId: null, className: '全部', count: list.value?.total ?? 0 },
    ...groups.map((g) => ({ key: String(g.classId), classId: g.classId, className: g.className, count: g.failed }))
  ]
})

function fmt(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function readPercent(n) {
  if (!n.totalRecipients) return 0
  return Math.round((n.readCount / n.totalRecipients) * 100)
}

async function load() {
  loading.value = true
  error.value = ''
  blocking.value = ''
  warnTip.value = ''
  try {
    stats.value = await courseStatistics(courseId)
    if (ctx.id !== courseId) {
      ctx.apply({ id: courseId, name: ctx.name || `课程 #${courseId}`, term: ctx.term || '' })
    }
    const all = stats.value.groups || []
    const confirmed = all.filter((g) => g.confirmed)
    if (confirmed.length === 0) {
      blocking.value = '该课程还没有任何分组确认发布，暂时没有挂科名单。'
      list.value = null
      notices.value = []
      return
    }
    const pending = all.length - confirmed.length
    if (pending > 0) {
      warnTip.value = `还有 ${pending} 个分组未确认发布，这些组的学生不会出现在名单中，也不会计入收件人。`
    }
    list.value = await failedStudents(courseId, activeClassId.value)
    notices.value = await listNotices(courseId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function switchGroup(classId) {
  activeClassId.value = classId
  loading.value = true
  error.value = ''
  try {
    list.value = await failedStudents(courseId, classId)
  } catch (e) {
    error.value = e.message
    list.value = null
  } finally {
    loading.value = false
  }
}

function askSend() {
  if (!form.value.title.trim()) {
    toast.error('无法发送', '请填写通知标题')
    return
  }
  if (!form.value.content.trim()) {
    toast.error('无法发送', '请填写通知内容')
    return
  }
  if (!list.value || list.value.total === 0) {
    toast.error('无法发送', '该范围没有挂科学生')
    return
  }
  confirming.value = true
}

async function doSend() {
  submitting.value = true
  try {
    const result = await sendNotice(courseId, {
      classId: activeClassId.value,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      examTime: form.value.examTime || null,
      examPlace: form.value.examPlace.trim() || null,
      examScope: form.value.examScope.trim() || null
    })
    toast.success(
      '补考通知已发送',
      `共 ${result.sent} 人${result.noAccount ? `，其中 ${result.noAccount} 人无登录账号、收不到站内信` : ''}`
    )
    confirming.value = false
    form.value.content = ''
    await load()
  } catch (e) {
    toast.error('发送失败', e.message)
  } finally {
    submitting.value = false
  }
}

async function doRevoke() {
  const target = revoking.value
  submitting.value = true
  try {
    await revokeNotice(courseId, target.id)
    toast.success('已撤回', '学生端不再展示该通知')
    revoking.value = null
    await load()
  } catch (e) {
    toast.error('撤回失败', e.message)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
