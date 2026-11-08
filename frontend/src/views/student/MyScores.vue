<template>
  <div>
    <div class="page-head">
      <div>
        <h2>我的成绩</h2>
        <p class="sub">只显示你本人已加入的课程；教师尚未最终确认的课程一律「未公布」，接口不会返回任何分数。</p>
      </div>
      <button class="btn btn-o" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div v-if="error" class="banner b-rose mt-12"><Icon name="alert" /><span>{{ error }}</span></div>

    <!-- 概览：课程数 / 已公布 / 未公布 -->
    <div v-if="!loading && rows.length" class="stat-grid">
      <div class="stat-card">
        <div class="k">在修课程</div>
        <div class="v">{{ rows.length }}</div>
        <div class="f muted">本人已加入的全部课程</div>
      </div>
      <div class="stat-card">
        <div class="k">已公布</div>
        <div class="v">{{ publishedCount }}</div>
        <div class="f"><span class="chip c-green">{{ publishedCount ? '可查看最终成绩' : '暂无' }}</span></div>
      </div>
      <div class="stat-card">
        <div class="k">未公布</div>
        <div class="v">{{ pendingCount }}</div>
        <div class="f"><span class="chip c-slate dashed">{{ pendingCount ? '教师尚未确认' : '全部已公布' }}</span></div>
      </div>
    </div>

    <section v-if="loading" class="card mt-16">
      <div class="empty"><div class="ico"><Icon name="book" /></div><h4>正在加载成绩…</h4></div>
    </section>

    <section v-else-if="rows.length === 0" class="card mt-16">
      <div class="empty">
        <div class="ico"><Icon name="book" /></div>
        <h4>还没有可查询的课程</h4>
        <p>你尚未被加入任何课程，请联系任课教师。</p>
      </div>
    </section>

    <!-- 按学期分组，学期新的在前 -->
    <template v-else>
      <section v-for="g in groups" :key="g.term" class="card mt-14">
      <div class="card-h">
        <h3><Icon name="book" /> {{ g.term }}</h3>
        <span class="chip c-indigo">{{ g.rows.length }} 门</span>
        <span class="ml-auto muted">
          已公布 {{ g.rows.filter((r) => r.published).length }} · 未公布 {{ g.rows.filter((r) => !r.published).length }}
        </span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>课程</th>
              <th style="width: 140px">任课教师</th>
              <th style="width: 150px">状态</th>
              <th style="width: 130px; text-align: right">最终成绩</th>
              <th style="width: 96px; text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in g.rows" :key="item.courseId" style="cursor: pointer" @click="detail = item">
              <td><span style="font-weight: 500">{{ item.courseName }}</span></td>
              <td class="muted">{{ item.teacherName || '—' }}</td>
              <td>
                <span v-if="!item.published" class="chip c-slate dashed">未公布</span>
                <span v-else-if="item.score === null" class="chip c-amber">待补录</span>
                <span v-else class="chip c-green">已公布</span>
              </td>
              <td style="text-align: right">
                <span v-if="!item.published" class="muted">—</span>
                <span v-else-if="item.score === null" class="muted">—</span>
                <span v-else class="mono" style="font-size: 16px; font-weight: 600">{{ formatScore(item.score) }}</span>
              </td>
              <td style="text-align: right">
                <button class="btn btn-sm btn-o" @click.stop="detail = item">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      </section>
    </template>

    <!-- 成绩趋势：紧跟成绩表之后；只看已公布且有分数的课程，未公布科目绝不画成 0 -->
    <section v-if="!loading && rows.length" class="card mt-14">
      <div class="card-h">
        <h3><Icon name="chart" /> 各科成绩趋势</h3>
        <span class="chip c-indigo">{{ chartItems.length }} 门已公布</span>
        <span v-if="worstItem" class="ml-auto small">
          最低：<b style="color: #be123c">{{ worstItem.label }} {{ formatScore(worstItem.value) }}</b>
        </span>
      </div>
      <div style="padding: 10px 18px 18px">
        <div v-if="chartItems.length === 0" class="empty" style="padding: 30px 10px">
          <div class="ico"><Icon name="chart" /></div>
          <h4>暂无可绘制的成绩</h4>
          <p>只有任课教师已确认发布的成绩才会进入趋势图。</p>
        </div>
        <template v-else>
          <LineChart :items="chartItems" />
          <div class="row-flex" style="justify-content: space-between; margin-top: 4px">
            <span class="small">横轴按学期先后排列；只含已公布且有分数的课程。</span>
            <span v-if="chartItems.length === 1" class="small">至少 2 门已公布课程才能连成折线。</span>
            <span v-else class="small">
              平均分 <b style="color: #0f172a">{{ averageScore }}</b> · 及格线 60
            </span>
          </div>
        </template>
      </div>
    </section>

    <div class="banner b-slate mt-14">
      <Icon name="info" />
      <span>
        <b>状态口径：</b>「未公布」= 任课教师尚未完成最终确认，此时不返回任何分数数字；「待补录」= 课程已确认但你的成绩记录缺失；「已公布」= 可查看最终分数。只能看到本人课程。
      </span>
    </div>

    <!-- 成绩详情 -->
    <div v-if="detail" class="mask" @click.self="detail = null">
      <div class="modal" style="max-width: 480px">
        <div class="modal-h">
          <div class="m-ico" style="background: #eef2ff; color: #4f46e5"><Icon name="book" /></div>
          <div>
            <h3>{{ detail.courseName }}</h3>
            <div class="m-sub">{{ detail.term }} · 任课教师 {{ detail.teacherName || '—' }}</div>
          </div>
        </div>

        <div class="modal-b">
          <!-- 成绩主体：未公布时绝不出现任何数字 -->
          <div style="text-align: center; padding: 4px 0 18px">
            <template v-if="!detail.published">
              <div class="chip c-slate dashed" style="font-size: 13px">未公布</div>
              <p class="muted" style="margin-top: 10px">任课教师尚未完成最终确认</p>
            </template>
            <template v-else-if="detail.score === null">
              <div class="chip c-amber" style="font-size: 13px">待补录</div>
              <p class="muted" style="margin-top: 10px">课程已确认，但未查到你的成绩记录</p>
            </template>
            <template v-else>
              <div style="font-size: 40px; font-weight: 700; color: #0f172a; line-height: 1.1">
                {{ formatScore(detail.score) }}
              </div>
              <div class="muted" style="margin-top: 4px">最终成绩（满分 100）</div>
              <div style="margin-top: 10px">
                <span class="chip" :class="isPass(detail) ? 'c-green' : 'c-rose'">
                  {{ isPass(detail) ? '及格' : '不及格' }}
                </span>
              </div>
            </template>
          </div>

          <table>
            <tbody>
              <tr>
                <td class="muted" style="width: 96px">课程名称</td>
                <td>{{ detail.courseName }}</td>
              </tr>
              <tr>
                <td class="muted">学期</td>
                <td>{{ detail.term || '—' }}</td>
              </tr>
              <tr>
                <td class="muted">任课教师</td>
                <td>{{ detail.teacherName || '—' }}</td>
              </tr>
              <tr>
                <td class="muted">公布状态</td>
                <td>
                  <span v-if="!detail.published" class="chip c-slate dashed">未公布</span>
                  <span v-else-if="detail.score === null" class="chip c-amber">待补录</span>
                  <span v-else class="chip c-green">已公布</span>
                </td>
              </tr>
              <tr>
                <td class="muted">成绩</td>
                <td>
                  <span v-if="!detail.published" class="muted">未公布（不显示分数）</span>
                  <span v-else-if="detail.score === null" class="muted">待补录</span>
                  <span v-else class="mono">{{ formatScore(detail.score) }}</span>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="banner b-slate mt-14" style="margin-bottom: 0">
            <Icon name="info" />
            <span>{{ detailHint }}</span>
          </div>
        </div>

        <div class="modal-f">
          <div class="r">
            <button class="btn btn-p" @click="detail = null">知道了</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import Icon from '../../components/Icon.vue'
import LineChart from '../../components/LineChart.vue'
import { myScores } from '../../api/score'

const rows = ref([])
const loading = ref(false)
const error = ref('')
const detail = ref(null)

const publishedCount = computed(() => rows.value.filter((r) => r.published).length)
const pendingCount = computed(() => rows.value.filter((r) => !r.published).length)

// 按学期分组：学生成绩单的常规组织方式，多学期时不至于混在一起
const groups = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    const key = (r.term || '').trim() || '未标注学期'
    if (!map.has(key)) map.set(key, { term: key, rows: [] })
    map.get(key).rows.push(r)
  }
  return [...map.values()].sort((a, b) => String(b.term).localeCompare(String(a.term), 'zh'))
})

// 折线图数据：只取「已公布 + 有分数」的课程，未公布绝不参与（否则会被画成 0 分造成误解）
// 按学期升序（早→晚）排列，同 semester 再按课程 id，保证横轴是时间顺序
const chartItems = computed(() =>
  rows.value
    .filter((r) => r.published && r.score !== null && r.score !== undefined)
    .slice()
    .sort(
      (a, b) => String(a.term || '').localeCompare(String(b.term || '')) || a.courseId - b.courseId
    )
    .map((r) => ({ label: r.courseName, term: r.term, value: Number(r.score) }))
)

const worstItem = computed(() => {
  if (!chartItems.value.length) return null
  return chartItems.value.reduce((min, cur) => (cur.value < min.value ? cur : min))
})

const averageScore = computed(() => {
  if (!chartItems.value.length) return '—'
  const sum = chartItems.value.reduce((acc, cur) => acc + cur.value, 0)
  return (sum / chartItems.value.length).toFixed(1)
})

const detailHint = computed(() => {
  if (!detail.value) return ''
  if (!detail.value.published) {
    return '任课教师尚未完成最终确认，确认之前不会显示任何分数。教师确认后点右上角「刷新」即可看到最终成绩。'
  }
  if (detail.value.score === null) {
    return '该课程成绩已确认发布，但系统中没有你的成绩记录，请联系任课教师核实补录。'
  }
  return '该成绩已由任课教师最终确认，仅你本人可见，其他同学看不到你的成绩。'
})

// 及格线与统计口径一致：低于 60 分为不及格（后端统计按同一阈值计算）
function isPass(item) {
  return item.score !== null && Number(item.score) >= 60
}

// 后端返回 BigDecimal，统一保留一位小数展示，避免出现 88 与 88.0 混排
function formatScore(score) {
  return Number(score).toFixed(1)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await myScores()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
