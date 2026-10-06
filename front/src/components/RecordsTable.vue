<template>
  <section class="table-wrap">
    <table>
      <thead>
        <tr v-if="viewMode === 'records'">
          <th>期号</th>
          <th>{{ frontLabel }}</th>
          <th>{{ backLabel }}</th>
          <th>开奖日期</th>
        </tr>
        <tr v-else>
          <th>期号</th>
          <th>{{ frontLabel }}</th>
          <th>{{ backLabel }}</th>
          <th>重复期数</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="record in rows">
          <tr :key="record.expect">
            <td>{{ record.expect }}</td>
            <td>
              <span v-for="front in frontBalls(record)" :key="record.expect + '-front-' + front" class="ball red">
                {{ front }}
              </span>
            </td>
            <td>
              <span v-for="back in backBalls(record)" :key="record.expect + '-back-' + back" class="ball blue">
                {{ back }}
              </span>
            </td>
            <td v-if="viewMode === 'records'">{{ formatDate(record.drawDate) }}</td>
            <td v-else>
              <button
                class="link-button"
                :disabled="!record.repeatCount"
                @click="$emit('toggle-matches', record.expect)"
              >
                {{ record.repeatCount }} 期
              </button>
            </td>
          </tr>
          <tr v-if="viewMode === 'repeats' && expandedExpect === record.expect" :key="record.expect + '-matches'">
            <td colspan="4" class="detail-cell">
              <div v-if="record.matches && record.matches.length" class="match-list">
                <div v-for="match in record.matches" :key="record.expect + '-match-' + match.expect" class="match-item">
                  <strong>{{ match.expect }}</strong>
                  <div class="match-balls">
                    <span v-for="front in match.frontBalls" :key="match.expect + '-front-' + front" class="ball red">
                      {{ front }}
                    </span>
                    <span v-for="back in match.backBalls" :key="match.expect + '-back-' + back" class="ball blue">
                      {{ back }}
                    </span>
                  </div>
                  <span class="match-summary">
                    重复 {{ match.matchedCount }} 个：
                    <span v-for="ball in match.matchedBalls" :key="match.expect + '-same-' + ball" class="mini-ball">
                      {{ ball }}
                    </span>
                  </span>
                </div>
              </div>
              <div v-else class="empty">没有符合条件的重复期号</div>
            </td>
          </tr>
        </template>
        <tr v-if="!rows.length && !loading">
          <td colspan="4" class="empty">暂无数据</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script>
export default {
  name: 'RecordsTable',
  props: {
    rows: { type: Array, required: true },
    viewMode: { type: String, required: true },
    frontLabel: { type: String, required: true },
    backLabel: { type: String, default: '' },
    expandedExpect: { type: String, default: '' },
    loading: { type: Boolean, default: false },
    frontBalls: { type: Function, required: true },
    backBalls: { type: Function, required: true },
    formatDate: { type: Function, required: true }
  }
}
</script>
