<template>
  <section class="omission-analysis">
    <div class="analysis-note">
      当前按最近 {{ recentCount }} 期展示遗漏走势；底部汇总按最近 {{ summaryRecentCount }} 期统计，可选择显示项。
    </div>
    <article v-for="area in areas" :key="area.key" class="omission-chart">
      <header class="omission-chart-header">
        <strong>{{ area.label }}遗漏走势</strong>
        <span>{{ area.rows.length }} 期 / {{ area.numbers.length }} 个号码</span>
      </header>
      <div class="trend-wrap omission-wrap">
        <div class="trend-scroll" :style="omissionScrollStyle(area)">
          <div class="omission-table" :style="omissionTableStyle(area)">
            <div class="trend-head">期号</div>
            <div
              v-for="num in area.numbers"
              :key="area.key + '-head-' + num"
              class="trend-head num-head"
            >
              {{ num }}
            </div>
            <template v-for="(row, rowIndex) in area.rows">
              <div
                :key="area.key + '-' + row.expect + '-expect'"
                :class="['trend-expect', omissionRowClass(rowIndex)]"
              >
                <strong>{{ row.expect }}</strong>
                <span>{{ formatDate(row.drawDate) }}</span>
              </div>
              <div
                v-for="cell in row.cells"
                :key="area.key + '-' + row.expect + '-' + cell.number"
                :class="[omissionCellClass(cell, area.key), omissionRowClass(rowIndex)]"
              >
                {{ cell.text }}
              </div>
            </template>
            <template v-for="summary in area.summaryRows">
              <div :key="area.key + '-summary-' + summary.key" class="trend-expect omission-summary-title">
                <strong>{{ summary.label }}</strong>
              </div>
              <div
                v-for="cell in summary.cells"
                :key="area.key + '-summary-' + summary.key + '-' + cell.number"
                class="omission-cell omission-summary-cell"
              >
                {{ cell.text }}
              </div>
            </template>
          </div>
        </div>
      </div>
    </article>
    <div v-if="!records.length && !loading" class="empty">暂无历史数据，请先同步。</div>
  </section>
</template>

<script>
export default {
  name: 'OmissionAnalysis',
  props: {
    areas: { type: Array, required: true },
    records: { type: Array, required: true },
    recentCount: { type: Number, required: true },
    summaryRecentCount: { type: Number, required: true },
    loading: { type: Boolean, default: false },
    formatDate: { type: Function, required: true },
    omissionScrollStyle: { type: Function, required: true },
    omissionTableStyle: { type: Function, required: true },
    omissionCellClass: { type: Function, required: true },
    omissionRowClass: { type: Function, required: true }
  }
}
</script>
