<template>
  <section class="trend-wrap">
    <div class="trend-scroll" :style="trendScrollStyle">
      <svg class="trend-lines" :width="trendSvgWidth" :height="trendSvgHeight">
        <polyline
          v-for="line in trendLines"
          :key="line.index"
          :points="line.points"
          fill="none"
          :stroke="line.color"
          stroke-width="2"
          stroke-linejoin="round"
          stroke-linecap="round"
        />
      </svg>
      <div class="trend-table" :style="trendTableStyle">
        <div class="trend-head">期号</div>
        <div
          v-for="num in trendNumbers"
          :key="'trend-head-' + num"
          class="trend-head num-head"
        >
          {{ num }}
        </div>
        <template v-for="record in trendRows">
          <div :key="record.expect + '-expect'" class="trend-expect">
            <strong>{{ record.expect }}</strong>
            <span>{{ formatDate(record.drawDate) }}</span>
          </div>
          <div
            v-for="num in trendNumbers"
            :key="record.expect + '-trend-' + num"
            :class="trendCellClass(record, num)"
          >
            {{ trendCellText(record, num) }}
          </div>
        </template>
      </div>
    </div>
    <div v-if="!trendRows.length && !loading" class="empty grid-empty">暂无数据</div>
  </section>
</template>

<script>
export default {
  name: 'TrendAnalysis',
  props: {
    trendRows: { type: Array, required: true },
    trendNumbers: { type: Array, required: true },
    trendLines: { type: Array, required: true },
    trendSvgWidth: { type: Number, required: true },
    trendSvgHeight: { type: Number, required: true },
    trendTableStyle: { type: Object, required: true },
    trendScrollStyle: { type: Object, required: true },
    loading: { type: Boolean, default: false },
    formatDate: { type: Function, required: true },
    trendCellClass: { type: Function, required: true },
    trendCellText: { type: Function, required: true }
  }
}
</script>
