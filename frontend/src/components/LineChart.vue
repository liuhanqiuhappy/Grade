<template>
  <div class="line-chart">
    <svg :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="xMidYMid meet" role="img" :aria-label="ariaLabel">
      <defs>
        <linearGradient id="score-line-area" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#6366f1" stop-opacity="0.24" />
          <stop offset="100%" stop-color="#6366f1" stop-opacity="0" />
        </linearGradient>
      </defs>

      <!-- 网格与纵轴刻度（0—100，固定量程，便于横向比较课程） -->
      <g>
        <template v-for="t in ticks" :key="'tick' + t">
          <line :x1="padL" :y1="yOf(t)" :x2="W - padR" :y2="yOf(t)" class="grid" />
          <text :x="padL - 10" :y="yOf(t) + 4" text-anchor="end" class="axis-y">{{ t }}</text>
        </template>
      </g>

      <!-- 及格线：与统计口径一致 -->
      <line :x1="padL" :y1="yOf(threshold)" :x2="W - padR" :y2="yOf(threshold)" class="pass-line" />
      <text :x="W - padR" :y="yOf(threshold) - 7" text-anchor="end" class="pass-label">
        及格线 {{ threshold }}
      </text>

      <!-- 面积 + 折线（只有一个点时画点不画线） -->
      <path v-if="points.length > 1" :d="areaPath" fill="url(#score-line-area)" />
      <polyline v-if="points.length > 1" :points="linePoints" class="line" />

      <g v-for="(p, i) in points" :key="p.label + '-' + i">
        <circle
          :cx="p.x"
          :cy="p.y"
          :r="i === lowestIndex ? 6 : 4.5"
          :class="i === lowestIndex ? 'dot lowest' : 'dot'"
        >
          <title>{{ p.term ? p.term + ' · ' : '' }}{{ p.label }}：{{ p.value.toFixed(1) }} 分</title>
        </circle>
        <text :x="p.x" :y="p.y - 12" text-anchor="middle" :class="i === lowestIndex ? 'value lowest' : 'value'">
          {{ p.value.toFixed(1) }}
        </text>
        <text
          :x="p.x"
          :y="H - 34"
          text-anchor="end"
          class="axis-x"
          :transform="`rotate(-20 ${p.x} ${H - 34})`"
        >
          <title>{{ p.label }}</title>
          {{ shortLabel(p.label) }}
        </text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 纯 SVG 折线图：不引第三方图表库，避免为一张图多背 1MB 依赖，样式也与 proto.css 一致
const props = defineProps({
  /** [{ label, value, term? }]，value 为 0—100 的分数 */
  items: { type: Array, default: () => [] },
  threshold: { type: Number, default: 60 }
})

const W = 760
const H = 320
const padL = 46
const padR = 26
const padT = 26
const padB = 74
const plotW = W - padL - padR
const plotH = H - padT - padB
const ticks = [0, 20, 40, 60, 80, 100]

function clamp(value) {
  const n = Number(value)
  if (Number.isNaN(n)) return 0
  return Math.min(100, Math.max(0, n))
}

function yOf(value) {
  return padT + ((100 - clamp(value)) / 100) * plotH
}

const points = computed(() => {
  const list = props.items
  if (!list.length) return []
  const step = list.length === 1 ? 0 : plotW / (list.length - 1)
  const startX = list.length === 1 ? padL + plotW / 2 : padL
  return list.map((item, i) => ({
    label: item.label,
    term: item.term || '',
    value: clamp(item.value),
    x: startX + step * i,
    y: yOf(item.value)
  }))
})

const linePoints = computed(() => points.value.map((p) => `${p.x},${p.y}`).join(' '))

const areaPath = computed(() => {
  if (points.value.length < 2) return ''
  const baseY = yOf(0)
  const first = points.value[0]
  const last = points.value[points.value.length - 1]
  const body = points.value.map((p) => `L ${p.x},${p.y}`).join(' ')
  return `M ${first.x},${baseY} ${body} L ${last.x},${baseY} Z`
})

// 最低分单独标红：学生最关心「哪科最差」，不用自己找
const lowestIndex = computed(() => {
  if (!points.value.length) return -1
  let idx = 0
  points.value.forEach((p, i) => {
    if (p.value < points.value[idx].value) idx = i
  })
  return idx
})

const ariaLabel = computed(() => {
  if (!points.value.length) return '成绩趋势图：暂无数据'
  return '成绩趋势图：' + points.value.map((p) => `${p.label} ${p.value.toFixed(1)} 分`).join('，')
})

function shortLabel(label) {
  const text = String(label || '')
  return text.length > 8 ? text.slice(0, 8) + '…' : text
}
</script>

<style scoped>
.line-chart svg {
  display: block;
  width: 100%;
  height: auto;
}

.grid {
  stroke: #e2e8f0;
  stroke-width: 1;
}

.axis-y,
.axis-x {
  font-size: 11px;
  fill: #94a3b8;
}

.axis-x {
  fill: #64748b;
}

.pass-line {
  stroke: #f59e0b;
  stroke-width: 1;
  stroke-dasharray: 5 4;
}

.pass-label {
  font-size: 11px;
  fill: #b45309;
}

.line {
  fill: none;
  stroke: #4f46e5;
  stroke-width: 2.5;
  stroke-linejoin: round;
  stroke-linecap: round;
}

.dot {
  fill: #fff;
  stroke: #4f46e5;
  stroke-width: 2.5;
}

.dot.lowest {
  fill: #e11d48;
  stroke: #fff;
  stroke-width: 3;
}

.value {
  font-size: 11px;
  font-weight: 600;
  fill: #475569;
}

.value.lowest {
  fill: #be123c;
}
</style>
