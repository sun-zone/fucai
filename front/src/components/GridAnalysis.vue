<template>
  <section class="grid-analysis">
    <article v-for="record in rows" :key="record.expect" class="draw-card" :style="drawCardStyle">
      <header class="draw-card-header">
        <strong>{{ record.expect }}</strong>
        <span>{{ formatDate(record.drawDate) }}</span>
      </header>
      <div class="grid-sections">
        <div v-if="gridArea !== 'back'" class="number-panel">
          <div class="number-panel-title">{{ frontLabel }}</div>
          <div class="number-grid" :style="gridStyle(frontGridSize)">
            <span
              v-for="num in gridNumbers(config.frontMax, frontGridSize)"
              :key="record.expect + '-front-grid-' + num"
              :class="{ selected: frontBalls(record).includes(num) }"
            >
              {{ num }}
            </span>
          </div>
        </div>
        <div v-if="config.backMax && gridArea !== 'front'" class="number-panel">
          <div class="number-panel-title">{{ backLabel }}</div>
          <div class="number-grid" :style="gridStyle(backGridSize)">
            <span
              v-for="num in gridNumbers(config.backMax, backGridSize)"
              :key="record.expect + '-back-grid-' + num"
              :class="{ selected: backBalls(record).includes(num) }"
            >
              {{ num }}
            </span>
          </div>
        </div>
      </div>
    </article>
    <div v-if="!rows.length && !loading" class="empty grid-empty">暂无数据</div>
  </section>
</template>

<script>
export default {
  name: 'GridAnalysis',
  props: {
    rows: { type: Array, required: true },
    config: { type: Object, required: true },
    frontLabel: { type: String, required: true },
    backLabel: { type: String, default: '' },
    gridArea: { type: String, required: true },
    frontGridSize: { type: Number, required: true },
    backGridSize: { type: Number, required: true },
    drawCardStyle: { type: Object, required: true },
    loading: { type: Boolean, default: false },
    frontBalls: { type: Function, required: true },
    backBalls: { type: Function, required: true },
    formatDate: { type: Function, required: true },
    gridStyle: { type: Function, required: true },
    gridNumbers: { type: Function, required: true }
  }
}
</script>
