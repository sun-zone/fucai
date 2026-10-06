<template>
  <section class="cooccurrence-analysis">
    <div class="analysis-note">{{ note }}</div>
    <article v-for="area in areas" :key="area.key" class="cooccurrence-chart">
      <header class="omission-chart-header">
        <strong>{{ area.label }}{{ title }}</strong>
        <span>{{ countText(area) }} / {{ area.numbers.length }} 个号码</span>
      </header>
      <div class="table-wrap cooccurrence-wrap">
        <table class="cooccurrence-table">
          <thead>
            <tr>
              <th class="sticky-col">号码</th>
              <th class="sticky-col second">{{ secondColumnLabel }}</th>
              <th
                v-for="num in area.numbers"
                :key="area.key + '-matrix-head-' + num"
                :class="{ 'selected-matrix-header': selectedColumn(area.key) === num }"
              >
                <button
                  class="matrix-number-button"
                  :class="{ selected: selectedColumn(area.key) === num }"
                  type="button"
                  :title="`点击查看 ${num} 所在行`"
                  @click="toggleColumn(area.key, num)"
                >
                  {{ num }}
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in area.rows"
              :key="area.key + '-matrix-row-' + row.number"
              :class="{ 'selected-matrix-row': selectedRow(area.key) === row.number }"
            >
              <td class="sticky-col">
                <button
                  class="co-number matrix-number-button"
                  :class="{ selected: selectedRow(area.key) === row.number }"
                  type="button"
                  :title="`点击查看 ${row.number} 所在列`"
                  @click="toggleRow(area.key, row.number)"
                >
                  {{ row.number }}
                </button>
              </td>
              <td class="sticky-col second">{{ row[secondColumnField] }}</td>
              <td
                v-for="cell in row.cells"
                :key="area.key + '-matrix-cell-' + row.number + '-' + cell.number"
                :class="{
                  diagonal: showDiagonal && cell.same,
                  highlight: cell.count > 0,
                  'selected-matrix-column': selectedColumn(area.key) === cell.number
                }"
                :title="`点击选中 ${row.number} 行和 ${cell.number} 列`"
                @click="toggleIntersection(area.key, row.number, cell.number)"
              >
                {{ showDiagonal && cell.same ? '-' : cell.count }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
    <div v-if="!records.length && !loading" class="empty">暂无历史数据，请先同步。</div>
  </section>
</template>

<script>
export default {
  name: 'MatrixAnalysis',
  data() {
    return {
      selectedColumns: {},
      selectedRows: {}
    }
  },
  props: {
    areas: { type: Array, required: true },
    records: { type: Array, required: true },
    note: { type: String, required: true },
    title: { type: String, required: true },
    secondColumnLabel: { type: String, required: true },
    secondColumnField: { type: String, required: true },
    countField: { type: String, required: true },
    showDiagonal: { type: Boolean, default: false },
    loading: { type: Boolean, default: false }
  },
  methods: {
    selectedColumn(areaKey) {
      return this.selectedColumns[areaKey] || ''
    },
    selectedRow(areaKey) {
      return this.selectedRows[areaKey] || ''
    },
    toggleColumn(areaKey, number) {
      if (this.selectedColumn(areaKey) === number) {
        this.$delete(this.selectedColumns, areaKey)
        return
      }
      this.$set(this.selectedColumns, areaKey, number)
    },
    toggleRow(areaKey, number) {
      if (this.selectedRow(areaKey) === number) {
        this.$delete(this.selectedRows, areaKey)
        return
      }
      this.$set(this.selectedRows, areaKey, number)
    },
    toggleIntersection(areaKey, rowNumber, columnNumber) {
      const isSelected = this.selectedRow(areaKey) === rowNumber
        && this.selectedColumn(areaKey) === columnNumber
      if (isSelected) {
        this.$delete(this.selectedRows, areaKey)
        this.$delete(this.selectedColumns, areaKey)
        return
      }
      this.$set(this.selectedRows, areaKey, rowNumber)
      this.$set(this.selectedColumns, areaKey, columnNumber)
    },
    countText(area) {
      const count = Array.isArray(area[this.countField]) ? area[this.countField].length : 0
      return `${count} ${this.countField === 'pairs' ? '组' : '期'}`
    }
  }
}
</script>
