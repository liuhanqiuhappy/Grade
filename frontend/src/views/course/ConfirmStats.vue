<template>
  <div>
    <div class="page-head">
      <div>
        <h2>确认与统计</h2>
        <p class="sub">确认以「课程 + 班级」分组为单位：分组内缺一人即拒绝并指名；已确认分组只读，其他分组不受影响。</p>
      </div>
      <div class="row-flex">
        <router-link
          v-if="hasConfirmedGroup"
          class="btn btn-o"
          :to="{ name: 'makeup-notice', params: { id: courseId } }"
        >
          <Icon name="bell" /> 挂科名单与补考通知
        </router-link>
        <button
          v-else
          class="btn btn-o"
          disabled
          title="至少确认一个分组后，才能生成挂科名单"
        >
          <Icon name="bell" /> 挂科名单与补考通知
        </button>
        <button class="btn btn-p" :disabled="readonly || loading || (stats && stats.total === 0)" @click="askConfirm(null)">
          <Icon name="check-circle" /> 全部分组确认
        </button>
      </div>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>
    <div v-if="readonly" class="banner b-green mt-12">
      <Icon name="check-circle" />
      <span>该课程已最终确认，成绩已对学生公布，课程内容整体只读。</span>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="k">总人数</div>
        <div class="v">{{ stats?.total ?? '—' }}</div>
        <div class="f"><span class="chip c-indigo">全部已加入成员</span></div>
      </div>
      <div class="stat-card">
        <div class="k">未录入</div>
        <div class="v">{{ stats?.unrecorded ?? '—' }}</div>
        <div class="f"><span class="chip c-amber">空值（不含 0）</span></div>
      </div>
      <div class="stat-card">
        <div class="k">不及格</div>
        <div class="v">{{ stats?.failed ?? '—' }}</div>
        <div class="f"><span class="chip c-rose">低于 60（含 0）</span></div>
      </div>
    </div>

    <section class="card mt-14">
      <div class="card-h">
        <h3><Icon name="table" /> 分组清单</h3>
        <span class="ml-auto muted">确认以「课程 + 班级」为单位；确认后不可回退</span>
      </div>

      <div v-if="loading" class="empty">
        <div class="ico"><Icon name="table" /></div><h4>正在统计…</h4>
      </div>
      <div v-else-if="!stats || stats.groups.length === 0" class="empty">
        <div class="ico"><Icon name="table" /></div>
        <h4>该课程暂无成员</h4>
        <p>先加入课程成员，再录入成绩并确认。</p>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>分组（班级）</th>
              <th style="width: 90px">成员</th>
              <th>统计摘要</th>
              <th style="width: 130px">状态</th>
              <th style="width: 130px; text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="g in stats.groups" :key="g.classId ?? g.className">
              <td style="font-weight: 500">{{ g.className }}</td>
              <td>{{ g.total }} 人</td>
              <td class="small">未录入 {{ g.unrecorded }} · 不及格 {{ g.failed }}</td>
              <td>
                <span class="chip" :class="g.confirmed ? 'c-slate' : 'c-amber'">
                  {{ g.confirmed ? '已确认 · 只读' : '草稿' }}
                </span>
              </td>
              <td style="text-align: right">
                <button
                  v-if="!g.confirmed && !readonly"
                  class="btn btn-sm btn-p"
                  style="margin-right: 8px"
                  :disabled="g.unrecorded > 0 || g.classId == null"
                  :title="g.classId == null
                    ? '该组缺少班级快照（历史数据），无法按组确认，请用「全部分组确认」'
                    : (g.unrecorded > 0 ? `还有 ${g.unrecorded} 人未录入成绩，补录后才能确认该组` : '')"
                  @click="askConfirm(g)"
                >
                  <Icon name="check-circle" /> 确认该组
                </button>
                <router-link class="btn btn-sm btn-o" :to="{ name: 'score-entry', params: { id: courseId } }">
                  查看成绩单
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="banner b-slate mt-14">
      <Icon name="info" />
      <span>
        <b>统计口径：</b>未录入按空值统计、不当作 0 分；不及格只统计已录入且低于 60 的成绩（0 分计入不及格）。
      </span>
    </div>

    <!-- 确认二次确认 -->
    <div v-if="confirming" class="mask" @click.self="confirming = false">
      <div class="modal" style="max-width: 460px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="lock" /></div>
          <div>
            <h3>{{ selectedGroup ? `确认「${selectedGroup.className}」分组` : '确认全部分组' }}</h3>
            <div class="m-sub">确认后该{{ selectedGroup ? '分组' : '课程全部分组' }}只读，学生端立即可见分数；其他分组不受影响。</div>
          </div>
        </div>
        <div class="modal-b">
          <p>共 {{ confirmTarget.total }} 名学生，已录入 {{ confirmTarget.total - confirmTarget.unrecorded }} 人。</p>
          <p v-if="confirmTarget.unrecorded" class="hint err" style="margin-top: 8px">
            仍有 {{ confirmTarget.unrecorded }} 人未录入，服务端会拒绝本次确认。
          </p>
        </div>
        <div class="modal-f">
          <div class="r">
            <button class="btn btn-o" @click="confirming = false">取消</button>
            <button class="btn btn-p" :disabled="submitting" @click="doConfirm">
              {{ submitting ? '确认中…' : '确认发布' }}
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
import { confirmScores, courseStatistics } from '../../api/score'
import { useCourseContextStore } from '../../store/courseContext'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const ctx = useCourseContextStore()
const toast = useToast()

const courseId = Number(route.params.id)
const stats = ref(null)
const loading = ref(false)
const error = ref('')
const confirming = ref(false)
const submitting = ref(false)

const readonly = computed(() => stats.value?.status === 'CONFIRMED')
const selectedGroup = ref(null)
// 挂科名单只从已确认分组派生：一个都没确认时入口置灰，避免点进去误以为「没有挂科学生」
const hasConfirmedGroup = computed(() =>
  (stats.value?.groups || []).some((g) => g.confirmed))

const confirmTarget = computed(() => {
  if (selectedGroup.value) return selectedGroup.value
  return { total: stats.value?.total ?? 0, unrecorded: stats.value?.unrecorded ?? 0 }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await courseStatistics(courseId)
    if (ctx.id !== courseId) {
      ctx.apply({ id: courseId, name: ctx.name || `课程 #${courseId}`, term: ctx.term || '' })
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function askConfirm(group) {
  const target = group || { total: stats.value?.total ?? 0, unrecorded: stats.value?.unrecorded ?? 0 }
  if (target.unrecorded > 0) {
    toast.error('无法确认', `该${group ? '分组' : '课程'}还有 ${target.unrecorded} 名学生未录入成绩`)
    return
  }
  selectedGroup.value = group
  confirming.value = true
}

async function doConfirm() {
  // 防御：按组确认必须带 classId。缺了会被后端理解为「确认全部分组」，
  // 造成其他班级被一并锁定（不可回滚），所以这里先挡住。
  const group = selectedGroup.value
  if (group && group.classId == null) {
    toast.error('无法确认', '该分组缺少班级标识，请改用「全部分组确认」或刷新后重试')
    return
  }
  submitting.value = true
  try {
    await confirmScores(courseId, group?.classId)
    toast.success('成绩已确认发布', '该范围学生端现在可以查到分数')
    confirming.value = false
    await load()
  } catch (e) {
    toast.error('确认失败', e.message)
    confirming.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
