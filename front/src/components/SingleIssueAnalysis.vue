<template>
  <section class="single-analysis">
    <div class="single-draw">
      <div class="single-draw-title">
        <strong>第 {{ record ? record.expect : '-' }} 期</strong>
        <span>{{ record ? formatDate(record.drawDate) : '' }}</span>
      </div>
      <div v-for="area in selectedAreas" :key="area.key" class="single-draw-area">
        <span class="single-area-label">{{ area.label }}</span>
        <span
          v-for="number in area.numbers"
          :key="area.key + '-selected-' + number"
          :class="['ball', area.key === 'back' ? 'blue' : 'red']"
        >
          {{ number }}
        </span>
      </div>
    </div>

    <article v-if="record && kl8TierGroups.length" class="single-panel kl8-tier-panel">
      <header class="single-panel-header">
        <strong>快乐8十期分层预测</strong>
        <span>10期转移率 + 双层共识 + 隔壁命中修正 + 平均/最大遗漏 + 历史最大连出；连续出现3期及以上降入观望或强不推荐</span>
      </header>
      <div v-for="area in kl8TierGroups" :key="'kl8-tier-' + area.key" class="kl8-tier-grid">
        <section class="kl8-tier-card kl8-tier-recommend">
          <div class="kl8-tier-title">
            <strong>推荐20码</strong>
            <span>命中 {{ hitCount(area.key, area.recommend) }}/{{ area.recommend.length }}</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="number in area.recommend"
              :key="'kl8-tier-recommend-' + area.key + '-' + number"
              :class="['ball', 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
          </div>
        </section>

        <section class="kl8-tier-card kl8-tier-middle">
          <div class="kl8-tier-title">
            <strong>中间观望号码50码</strong>
            <span>命中 {{ hitCount(area.key, area.middle) }}/{{ area.middle.length }}</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="number in area.middle"
              :key="'kl8-tier-middle-' + area.key + '-' + number"
              :class="['ball', 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
          </div>
        </section>

        <section class="kl8-tier-card kl8-tier-avoid">
          <div class="kl8-tier-title">
            <strong>强不推荐10码</strong>
            <span>命中 {{ hitCount(area.key, area.avoid) }}/{{ area.avoid.length }}</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="number in area.avoid"
              :key="'kl8-tier-avoid-' + area.key + '-' + number"
              :class="['ball', 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
          </div>
        </section>
      </div>
    </article>

    <article v-if="record" class="single-panel">
      <header class="single-panel-header">
        <strong>{{ recommendationTitle }}</strong>
        <span>{{ recommendationSubtitle }}</span>
      </header>
      <div class="recommend-list">
        <div v-for="group in recommendationGroups" :key="'recommend-group-' + group.index" class="recommend-card">
          <div class="recommend-title">第 {{ group.index }} 组</div>
          <div v-for="area in group.areas" :key="'recommend-group-' + group.index + '-' + area.key" class="recommend-area">
            <span class="single-area-label">{{ area.label }}</span>
            <span
              v-for="number in area.numbers"
              :key="'recommend-group-' + group.index + '-' + area.key + '-' + number"
              :class="['ball', area.key === 'back' ? 'blue' : 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
            <em>{{ area.source }}，命中 {{ hitCount(area.key, area.numbers) }}/{{ area.numbers.length }}，分值 {{ area.totalScore }}</em>
          </div>
        </div>
      </div>
    </article>

    <article v-if="record && repeatedTwentyCodeGroups.length" class="single-panel">
      <header class="single-panel-header">
        <strong>快乐8 20码重复号</strong>
        <span>提取5组20码中出现3次及以上的号码</span>
      </header>
      <div class="recommend-list repeated-code-list">
        <div v-for="area in repeatedTwentyCodeGroups" :key="'repeated-20-' + area.key" class="recommend-card">
          <div class="recommend-title">{{ area.label }}重复3次及以上</div>
          <div class="recommend-area repeated-code-area">
            <span
              v-for="number in area.numbers"
              :key="'repeated-20-' + area.key + '-' + number"
              class="repeat-number-item"
            >
              <span :class="['ball', 'red', { hit: isHit(area.key, number) }]">{{ number }}</span>
              <em>{{ area.counts[number] }}次</em>
            </span>
            <em>共 {{ area.numbers.length }} 个，命中 {{ hitCount(area.key, area.numbers) }}/{{ area.numbers.length }}</em>
          </div>
        </div>
      </div>
    </article>

    <article v-if="record && hitTwentyCodeGroups.length" class="single-panel">
      <header class="single-panel-header">
        <strong>快乐8 20码全部命中号</strong>
        <span>汇总5组20码推荐中实际命中的全部号码</span>
      </header>
      <div class="recommend-list repeated-code-list">
        <div v-for="area in hitTwentyCodeGroups" :key="'hit-20-' + area.key" class="recommend-card hit-code-card">
          <div class="recommend-title">{{ area.label }}命中汇总</div>
          <div class="recommend-area repeated-code-area">
            <span
              v-for="number in area.numbers"
              :key="'hit-20-' + area.key + '-' + number"
              class="repeat-number-item"
            >
              <span :class="['ball', 'red', { hit: isHit(area.key, number) }]">{{ number }}</span>
              <em>第{{ area.groups[number].join('、') }}组</em>
            </span>
            <em>共 {{ area.numbers.length }} 个命中</em>
          </div>
        </div>
      </div>
    </article>

    <article v-if="record && coreRecommendationGroups.length" class="single-panel">
      <header class="single-panel-header">
        <strong>快乐8核心10码</strong>
        <span>从20码推荐中进一步筛选更高把握号码</span>
      </header>
      <div class="recommend-list">
        <div v-for="group in coreRecommendationGroups" :key="'core-group-' + group.index" class="recommend-card">
          <div class="recommend-title">第 {{ group.index }} 组</div>
          <div v-for="area in group.areas" :key="'core-group-' + group.index + '-' + area.key" class="recommend-area">
            <span class="single-area-label">{{ area.label }}</span>
            <span
              v-for="number in area.numbers"
              :key="'core-group-' + group.index + '-' + area.key + '-' + number"
              :class="['ball', area.key === 'back' ? 'blue' : 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
            <em>{{ area.source }}，命中 {{ hitCount(area.key, area.numbers) }}/{{ area.numbers.length }}，分值 {{ area.totalScore }}</em>
          </div>
        </div>
      </div>
    </article>

    <article v-if="record && playRecommendationGroups.length" class="single-panel">
      <header class="single-panel-header">
        <strong>快乐8玩法推荐</strong>
        <span>选二、选五、选六各推荐两组</span>
      </header>
      <div class="recommend-list">
        <div v-for="play in playRecommendationGroups" :key="'play-' + play.key" class="recommend-card">
          <div class="recommend-title">{{ play.label }}</div>
          <div v-for="group in play.groups" :key="'play-' + play.key + '-' + group.index" class="recommend-area">
            <span class="single-area-label">第{{ group.index }}组</span>
            <span
              v-for="number in group.numbers"
              :key="'play-' + play.key + '-' + group.index + '-' + number"
              :class="['ball', { red: true, hit: isHit(play.areaKey, number) }]"
            >
              {{ number }}
            </span>
            <em>{{ group.source }}，命中 {{ hitCount(play.areaKey, group.numbers) }}/{{ group.numbers.length }}，分值 {{ group.totalScore }}</em>
          </div>
        </div>
      </div>
    </article>

    <article v-if="record" class="single-panel">
      <header class="single-panel-header">
        <strong>低分观察区</strong>
        <span>仅表示模型评分较低，不代表号码一定不会出现</span>
      </header>
      <div class="recommend-list">
        <div v-for="group in avoidGroups" :key="'avoid-group-' + group.index" class="recommend-card avoid-card">
          <div class="recommend-title avoid-title">第 {{ group.index }} 组</div>
          <div v-for="area in group.areas" :key="'avoid-group-' + group.index + '-' + area.key" class="recommend-area">
            <span class="single-area-label">{{ area.label }}</span>
            <span
              v-for="number in area.numbers"
              :key="'avoid-group-' + group.index + '-' + area.key + '-' + number"
              :class="['ball', area.key === 'back' ? 'blue' : 'red', { hit: isHit(area.key, number) }]"
            >
              {{ number }}
            </span>
            <em>{{ area.source }}，命中 {{ hitCount(area.key, area.numbers) }}/{{ area.numbers.length }}，分值 {{ area.totalScore }}</em>
          </div>
        </div>
      </div>
    </article>

    <article class="single-panel">
      <header class="single-panel-header">
        <strong>本期号码分布统计</strong>
        <span>统计规则：{{ bucketRuleText }}</span>
      </header>
      <div class="single-number-stats">
        <div v-for="area in numberBucketAreas" :key="area.key + '-number-stats'" class="single-number-area">
          <div class="single-number-area-title">{{ area.label }}</div>
          <div class="single-number-grid">
            <div v-for="item in area.items" :key="area.key + '-number-stat-' + item.number" class="single-number-card">
              <div class="single-number-card-head">
                <span :class="['ball', area.key === 'back' ? 'blue' : 'red']">{{ item.number }}</span>
              </div>
              <div v-if="item.cooccurrence" class="single-number-stat-block">
                <strong>共现统计</strong>
                <div class="single-number-buckets">
                  <div v-for="bucket in item.cooccurrence" :key="item.number + '-co-' + bucket.label" class="single-number-bucket">
                    <span>{{ bucket.label }}：{{ bucket.total }}</span>
                    <em v-if="bucket.groups.length">
                      <b v-for="group in bucket.groups" :key="item.number + '-co-' + bucket.label + '-' + group.count">
                        {{ group.count }}次：{{ group.numbers.join('、') }}
                      </b>
                    </em>
                    <em v-else>-</em>
                  </div>
                </div>
              </div>
              <div class="single-number-stat-block">
                <strong>下期统计</strong>
                <div class="single-number-buckets">
                  <div v-for="bucket in item.nextOccurrence" :key="item.number + '-next-' + bucket.label" class="single-number-bucket">
                    <span>{{ bucket.label }}：{{ bucket.total }}</span>
                    <em v-if="bucket.groups.length">
                      <b v-for="group in bucket.groups" :key="item.number + '-next-' + bucket.label + '-' + group.count">
                        {{ group.count }}次：{{ group.numbers.join('、') }}
                      </b>
                    </em>
                    <em v-else>-</em>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>

    <article v-for="area in cooccurrenceAreas" :key="area.key + '-cooccurrence'" class="single-panel">
      <header class="single-panel-header">
        <strong>{{ area.label }}共现统计</strong>
        <span>仅显示当前期号码对应的行</span>
      </header>
      <div class="table-wrap cooccurrence-wrap">
        <table class="cooccurrence-table">
          <thead>
            <tr>
              <th class="sticky-col">号码</th>
              <th class="sticky-col second">出现次数</th>
              <th
                v-for="num in area.numbers"
                :key="area.key + '-single-co-head-' + num"
                :class="{ 'selected-matrix-header': selectedColumn(area.key, 'co') === num }"
              >
                <button
                  class="matrix-number-button"
                  :class="{ selected: selectedColumn(area.key, 'co') === num }"
                  type="button"
                  @click="toggleColumn(area.key, 'co', num)"
                >
                  {{ num }}
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in area.rows"
              :key="area.key + '-single-co-row-' + row.number"
              :class="{ 'selected-matrix-row': selectedRow(area.key, 'co') === row.number }"
            >
              <td class="sticky-col">
                <button
                  class="co-number matrix-number-button"
                  :class="{ selected: selectedRow(area.key, 'co') === row.number }"
                  type="button"
                  @click="toggleRow(area.key, 'co', row.number)"
                >
                  {{ row.number }}
                </button>
              </td>
              <td class="sticky-col second">{{ row.appearCount }}</td>
              <td
                v-for="cell in row.cells"
                :key="area.key + '-single-co-cell-' + row.number + '-' + cell.number"
                :class="{
                  diagonal: cell.same,
                  highlight: cell.count > 0,
                  'selected-matrix-column': selectedColumn(area.key, 'co') === cell.number
                }"
                @click="toggleIntersection(area.key, 'co', row.number, cell.number)"
              >
                {{ cell.same ? '-' : cell.count }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <article v-for="area in nextOccurrenceAreas" :key="area.key + '-next'" class="single-panel">
      <header class="single-panel-header">
        <strong>{{ area.label }}下期统计</strong>
        <span>仅显示当前期号码对应的行</span>
      </header>
      <div class="table-wrap cooccurrence-wrap">
        <table class="cooccurrence-table">
          <thead>
            <tr>
              <th class="sticky-col">号码</th>
              <th class="sticky-col second">作为前期次数</th>
              <th
                v-for="num in area.numbers"
                :key="area.key + '-single-next-head-' + num"
                :class="{ 'selected-matrix-header': selectedColumn(area.key, 'next') === num }"
              >
                <button
                  class="matrix-number-button"
                  :class="{ selected: selectedColumn(area.key, 'next') === num }"
                  type="button"
                  @click="toggleColumn(area.key, 'next', num)"
                >
                  {{ num }}
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in area.rows"
              :key="area.key + '-single-next-row-' + row.number"
              :class="{ 'selected-matrix-row': selectedRow(area.key, 'next') === row.number }"
            >
              <td class="sticky-col">
                <button
                  class="co-number matrix-number-button"
                  :class="{ selected: selectedRow(area.key, 'next') === row.number }"
                  type="button"
                  @click="toggleRow(area.key, 'next', row.number)"
                >
                  {{ row.number }}
                </button>
              </td>
              <td class="sticky-col second">{{ row.baseCount }}</td>
              <td
                v-for="cell in row.cells"
                :key="area.key + '-single-next-cell-' + row.number + '-' + cell.number"
                :class="{
                  highlight: cell.count > 0,
                  'selected-matrix-column': selectedColumn(area.key, 'next') === cell.number
                }"
                @click="toggleIntersection(area.key, 'next', row.number, cell.number)"
              >
                {{ cell.count }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <div v-if="!record" class="empty">暂无历史数据，请先同步。</div>
  </section>
</template>

<script>
import axios from 'axios'

export default {
  name: 'SingleIssueAnalysis',
  data() {
    return {
      selectedColumns: {},
      selectedRows: {},
      kl8TierStrategyEnabled: true,
      dltFilteredRecommendationGroups: null,
      dltFilterRequestId: 0
    }
  },
  props: {
    lottery: { type: String, default: '' },
    record: { type: Object, default: null },
    nextRecord: { type: Object, default: null },
    selectedAreas: { type: Array, required: true },
    cooccurrenceAreas: { type: Array, required: true },
    nextOccurrenceAreas: { type: Array, required: true },
    recentCount: { type: Number, required: true },
    formatDate: { type: Function, required: true }
  },
  computed: {
    recommendationTitle() {
      return '五组推荐策略'
    },
    recommendationSubtitle() {
      return '按不同统计侧重点生成五组号码'
    },
    countBucketsConfig() {
      if (this.recentCount < 30) {
        return [
          { label: '1次', min: 1, max: 1 },
          { label: '2次', min: 2, max: 2 },
          { label: '3次', min: 3, max: 3 },
          { label: '4次', min: 4, max: 4 },
          { label: '5次及以上', min: 5, max: Infinity }
        ]
      }
      if (this.recentCount < 150) {
        return [
          { label: '1-2次', min: 1, max: 2 },
          { label: '3-4次', min: 3, max: 4 },
          { label: '5次', min: 5, max: 5 },
          { label: '6次及以上', min: 6, max: Infinity }
        ]
      }
      if (this.recentCount < 300) {
        return [
          { label: '1-3次', min: 1, max: 3 },
          { label: '4-6次', min: 4, max: 6 },
          { label: '7-9次', min: 7, max: 9 },
          { label: '10次及以上', min: 10, max: Infinity }
        ]
      }
      return [
        { label: '1-5次', min: 1, max: 5 },
        { label: '6-10次', min: 6, max: 10 },
        { label: '11-15次', min: 11, max: 15 },
        { label: '16次及以上', min: 16, max: Infinity }
      ]
    },
    bucketRuleText() {
      return this.countBucketsConfig.map(bucket => bucket.label).join('，')
    },
    numberBucketAreas() {
      return this.selectedAreas.map(area => {
        const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === area.key)
        const nextOccurrenceArea = this.nextOccurrenceAreas.find(item => item.key === area.key)
        return {
          ...area,
          items: area.numbers.map(number => {
            const cooccurrenceRow = cooccurrenceArea
              ? cooccurrenceArea.rows.find(row => row.number === number)
              : null
            const nextOccurrenceRow = nextOccurrenceArea
              ? nextOccurrenceArea.rows.find(row => row.number === number)
              : null
            return {
              number,
              cooccurrence: cooccurrenceRow ? this.countRowBuckets(cooccurrenceRow) : null,
              nextOccurrence: nextOccurrenceRow ? this.countRowBuckets(nextOccurrenceRow) : this.emptyBuckets()
            }
          })
        }
      })
    },
    rawRecommendationGroups() {
      if (!this.record) {
        return []
      }
      const areaRecommendations = this.selectedAreas.map(area => {
        const nextOccurrenceArea = this.nextOccurrenceAreas.find(item => item.key === area.key)
        return {
          key: area.key,
          label: area.label,
          groups: this.recommendationSets(nextOccurrenceArea, area.numbers.length)
        }
      })
      return Array.from({ length: 5 }, (_, index) => ({
        index: index + 1,
        areas: areaRecommendations.map(area => ({
          key: area.key,
          label: area.label,
          numbers: area.groups[index] ? area.groups[index].numbers : [],
          source: area.groups[index] ? area.groups[index].source : '暂无数据',
          totalScore: area.groups[index] ? area.groups[index].totalScore.toFixed(1) : '0.0'
        }))
      }))
    },
    recommendationGroups() {
      if (this.lottery === 'dlt') {
        return this.dltFilteredRecommendationGroups || []
      }
      return this.rawRecommendationGroups
    },
    kl8TierGroups() {
      if (!this.kl8TierStrategyEnabled || !this.record) {
        return []
      }
      return this.kl8OrderedTierGroups.map(area => ({
        key: area.key,
        label: area.label,
        recommend: area.recommend,
        middle: area.middle,
        avoid: area.avoid
      }))
    },
    kl8OrderedTierGroups() {
      if (!this.kl8TierStrategyEnabled || !this.record) {
        return []
      }
      return this.selectedAreas
        .filter(area => area.numbers.length >= 20)
        .map(selectedArea => {
          const area = this.nextOccurrenceAreas.find(item => item.key === selectedArea.key)
          const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === selectedArea.key)
          if (!this.isStructureArea(area, selectedArea.numbers.length)) {
            return null
          }
          const candidates = this.recommendationCandidates(area, cooccurrenceArea)
          const tier = this.kl8TierPrediction(area, candidates, selectedArea.numbers)
          return {
            key: selectedArea.key,
            label: selectedArea.label,
            recommend: tier.recommend.map(item => item.number),
            middle: tier.middle.map(item => item.number),
            avoid: tier.avoid.map(item => item.number),
            ranked: tier.ranked.map((item, index) => ({
              number: item.number,
              rank: index + 1
            }))
          }
        })
        .filter(Boolean)
    },
    repeatedTwentyCodeGroups() {
      if (!this.record) {
        return []
      }
      return this.selectedAreas
        .filter(area => area.numbers.length >= 20)
        .map(area => {
          const counts = this.twentyCodeConsensus(area.key)
          const numbers = Object.keys(counts)
            .filter(number => counts[number].count >= 3)
            .sort((left, right) => counts[right].count - counts[left].count || Number(left) - Number(right))
          return {
            key: area.key,
            label: area.label,
            numbers,
            counts: numbers.reduce((result, number) => {
              result[number] = counts[number].count
              return result
            }, {})
          }
        })
        .filter(area => area.numbers.length)
    },
    hitTwentyCodeGroups() {
      if (!this.record || !this.nextRecord) {
        return []
      }
      return this.selectedAreas
        .filter(area => area.numbers.length >= 20)
        .map(area => {
          const groups = {}
          this.recommendationGroups.forEach((group, index) => {
            const targetArea = group.areas.find(item => item.key === area.key)
            if (!targetArea) {
              return
            }
            targetArea.numbers.forEach(number => {
              if (!this.isHit(area.key, number)) {
                return
              }
              if (!groups[number]) {
                groups[number] = []
              }
              groups[number].push(index + 1)
            })
          })
          const numbers = Object.keys(groups).sort((left, right) => Number(left) - Number(right))
          return {
            key: area.key,
            label: area.label,
            numbers,
            groups
          }
        })
        .filter(area => area.numbers.length)
    },
    coreRecommendationGroups() {
      if (!this.record) {
        return []
      }
      const areaRecommendations = this.selectedAreas.map(area => {
        const nextOccurrenceArea = this.nextOccurrenceAreas.find(item => item.key === area.key)
        if (!this.isStructureArea(nextOccurrenceArea, area.numbers.length)) {
          return null
        }
        return {
          key: area.key,
          label: area.label,
          groups: this.coreRecommendationSets(nextOccurrenceArea, 10)
        }
      }).filter(Boolean)
      if (!areaRecommendations.length) {
        return []
      }
      return Array.from({ length: 2 }, (_, index) => ({
        index: index + 1,
        areas: areaRecommendations.map(area => ({
          key: area.key,
          label: area.label,
          numbers: area.groups[index] ? area.groups[index].numbers : [],
          source: area.groups[index] ? area.groups[index].source : '暂无数据',
          totalScore: area.groups[index] ? area.groups[index].totalScore.toFixed(1) : '0.0'
        }))
      }))
    },
    playRecommendationGroups() {
      if (!this.record) {
        return []
      }
      const area = this.selectedAreas.find(item => item.numbers.length >= 20)
      if (!area) {
        return []
      }
      const nextOccurrenceArea = this.nextOccurrenceAreas.find(item => item.key === area.key)
      if (!this.isStructureArea(nextOccurrenceArea, area.numbers.length)) {
        return []
      }
      return [
        { key: 'pick2', label: '选二', size: 2 },
        { key: 'pick5', label: '选五', size: 5 },
        { key: 'pick6', label: '选六', size: 6 }
      ].map(play => ({
        key: play.key,
        label: play.label,
        areaKey: area.key,
        groups: this.playRecommendationSets(nextOccurrenceArea, play.size).map((group, index) => ({
          index: index + 1,
          ...group,
          totalScore: group.totalScore.toFixed(1)
        }))
      }))
    },
    avoidGroups() {
      if (!this.record) {
        return []
      }
      const areaRecommendations = this.selectedAreas.map(area => {
        const nextOccurrenceArea = this.nextOccurrenceAreas.find(item => item.key === area.key)
        const recommendedNumbers = this.recommendationGroups
          .flatMap(group => {
            const targetArea = group.areas.find(item => item.key === area.key)
            return targetArea ? targetArea.numbers : []
          })
        return {
          key: area.key,
          label: area.label,
          groups: this.avoidSets(nextOccurrenceArea, area.numbers.length, recommendedNumbers)
        }
      })
      return Array.from({ length: 5 }, (_, index) => ({
        index: index + 1,
        areas: areaRecommendations.map(area => ({
          key: area.key,
          label: area.label,
          numbers: area.groups[index] ? area.groups[index].numbers : [],
          source: area.groups[index] ? area.groups[index].source : '暂无数据',
          totalScore: area.groups[index] ? area.groups[index].totalScore.toFixed(1) : '0.0'
        }))
      }))
    }
  },
  watch: {
    rawRecommendationGroups: {
      deep: true,
      immediate: true,
      handler() {
        this.filterDltRecommendationGroups()
      }
    },
    record() {
      this.filterDltRecommendationGroups()
    },
    recentCount() {
      this.filterDltRecommendationGroups()
    }
  },
  methods: {
    async filterDltRecommendationGroups() {
      if (this.lottery !== 'dlt') {
        return
      }
      const groups = this.rawRecommendationGroups
      if (!this.record || !groups.length) {
        this.dltFilteredRecommendationGroups = []
        return
      }
      const requestId = ++this.dltFilterRequestId
      this.dltFilteredRecommendationGroups = null
      try {
        const response = await axios.post('/api/dlt/recommendation/filter', {
          beforeExpect: this.record.expect,
          groups
        })
        if (requestId === this.dltFilterRequestId) {
          this.dltFilteredRecommendationGroups = response.data || []
        }
      } catch (error) {
        if (requestId === this.dltFilterRequestId) {
          this.dltFilteredRecommendationGroups = []
        }
      }
    },
    selectionKey(areaKey, tableKey) {
      return `${tableKey}-${areaKey}`
    },
    hitNumbers(areaKey) {
      if (!this.nextRecord) {
        return []
      }
      const area = this.nextOccurrenceAreas.find(item => item.key === areaKey)
      if (!area) {
        return []
      }
      return (area.fields || []).map(field => String(this.nextRecord[field] || '').padStart(2, '0'))
    },
    isHit(areaKey, number) {
      return this.hitNumbers(areaKey).includes(number)
    },
    hitCount(areaKey, numbers) {
      const hitNumbers = this.hitNumbers(areaKey)
      if (!hitNumbers.length) {
        return 0
      }
      return numbers.filter(number => hitNumbers.includes(number)).length
    },
    selectedColumn(areaKey, tableKey) {
      return this.selectedColumns[this.selectionKey(areaKey, tableKey)] || ''
    },
    selectedRow(areaKey, tableKey) {
      return this.selectedRows[this.selectionKey(areaKey, tableKey)] || ''
    },
    toggleColumn(areaKey, tableKey, number) {
      const key = this.selectionKey(areaKey, tableKey)
      if (this.selectedColumn(areaKey, tableKey) === number) {
        this.$delete(this.selectedColumns, key)
        return
      }
      this.$set(this.selectedColumns, key, number)
    },
    toggleRow(areaKey, tableKey, number) {
      const key = this.selectionKey(areaKey, tableKey)
      if (this.selectedRow(areaKey, tableKey) === number) {
        this.$delete(this.selectedRows, key)
        return
      }
      this.$set(this.selectedRows, key, number)
    },
    toggleIntersection(areaKey, tableKey, rowNumber, columnNumber) {
      const key = this.selectionKey(areaKey, tableKey)
      const isSelected = this.selectedRow(areaKey, tableKey) === rowNumber
        && this.selectedColumn(areaKey, tableKey) === columnNumber
      if (isSelected) {
        this.$delete(this.selectedRows, key)
        this.$delete(this.selectedColumns, key)
        return
      }
      this.$set(this.selectedRows, key, rowNumber)
      this.$set(this.selectedColumns, key, columnNumber)
    },
    emptyBuckets() {
      return this.countBucketsConfig.map(bucket => ({
        ...bucket,
        total: 0,
        groups: []
      }))
    },
    countRowBuckets(row) {
      const buckets = this.countBucketsConfig.map(bucket => ({
        ...bucket,
        total: 0,
        countMap: {},
        groups: []
      }))
      row.cells.forEach(cell => {
        if (!cell.count || cell.count <= 0 || cell.same) {
          return
        }
        const bucket = buckets.find(item => cell.count >= item.min && cell.count <= item.max)
        if (bucket) {
          bucket.total += 1
          if (!bucket.countMap[cell.count]) {
            bucket.countMap[cell.count] = []
          }
          bucket.countMap[cell.count].push(cell.number)
        }
      })
      return buckets.map(bucket => ({
        ...bucket,
        groups: Object.keys(bucket.countMap)
          .map(count => Number(count))
          .sort((a, b) => a - b)
          .map(count => ({
            count,
            numbers: bucket.countMap[count]
          })),
        countMap: undefined
      }))
    },
    recommendationCandidates(area, cooccurrenceArea) {
      if (!area) {
        return []
      }
      const selectedArea = this.selectedAreas.find(item => item.key === area.key)
      const currentNumbers = selectedArea ? selectedArea.numbers : []
      const neighborNumbers = this.neighborNumbers(currentNumbers, area.max)
      const kl8Mode = area.max >= 80 && Math.max(1, currentNumbers.length) >= 20
        const profile = kl8Mode ? this.kl8Profile(area, currentNumbers) : null
        const offsetFeedback = kl8Mode ? this.kl8OffsetFeedback(area) : null
        const rawCandidates = area.numbers.map(number => {
        const nextCounts = area.rows.map(row => {
          const cell = row.cells.find(item => item.number === number)
          return cell ? cell.count : 0
        })
        const nextBaseCount = area.rows.reduce((sum, row) => sum + (row.baseCount || 0), 0)
        const coCounts = cooccurrenceArea
          ? cooccurrenceArea.rows.map(row => {
            const cell = row.cells.find(item => item.number === number)
            return cell && !cell.same ? cell.count : 0
          })
          : []
        const coBaseCount = cooccurrenceArea
          ? cooccurrenceArea.rows.reduce((sum, row) => sum + (row.appearCount || 0), 0)
          : 0
        const omission = this.omissionStats(number, area.records || [], area.fields || [])
        const nextScore = nextCounts.reduce((sum, count) => sum + count, 0)
        const coScore = coCounts.reduce((sum, count) => sum + count, 0)
        const expectedRate = area.max ? currentNumbers.length / area.max : 0
        const smoothing = kl8Mode ? 8 : 3
        const nextRateScore = nextBaseCount > 0
          ? (nextScore + expectedRate * smoothing) / (nextBaseCount + smoothing)
          : 0
        const coRateScore = coBaseCount > 0
          ? (coScore + expectedRate * smoothing) / (coBaseCount + smoothing)
          : 0
        const hitRowsScore = area.rows.length
          ? nextCounts.filter(count => count > 0).length / area.rows.length
          : 0
        const offsetExactScore = offsetFeedback ? (offsetFeedback.exact[number] || 0) : 0
        const offset1Score = offsetFeedback ? (offsetFeedback.near1[number] || 0) : 0
        const offset2Score = offsetFeedback ? (offsetFeedback.near2[number] || 0) : 0
        const offset3Score = offsetFeedback ? (offsetFeedback.near3[number] || 0) : 0
        const neighborMissRate = offsetFeedback
          ? (offsetFeedback.neighborMiss[number] || 0) / Math.max(1, offsetFeedback.predictedMiss[number] || 0)
          : 0
        const neighborHitCount = offsetFeedback ? (offsetFeedback.neighborHit[number] || 0) : 0
        const heatScore = this.appearCount(number, area.records || [], area.fields || [])
        const repeatScore = currentNumbers.includes(number) ? 1 : 0
        const recentRepeatScore = this.recentRepeatCount(number, area.records || [], area.fields || [])
        const neighborScore = neighborNumbers.includes(number) ? 1 : 0
        const zoneScore = profile ? this.targetShareScore(profile.zoneTargets[this.zoneIndex(number)] || 0, profile.zoneAverage) : 0
        const tailScore = profile ? this.targetShareScore(profile.tailTargets[Number(number) % 10] || 0, profile.tailAverage) : 0
        const runLength = kl8Mode
          ? this.kl8CurrentRun(number, area.records || [], area.fields || [])
          : 0
        const omissionAverageRatio = kl8Mode
          ? Math.min(2, omission.current / Math.max(1, omission.average))
          : 0
        const omissionMaxRatio = kl8Mode
          ? omission.current / Math.max(1, omission.max)
          : 0
        const maxConsecutive = kl8Mode
          ? omission.maxConsecutive
          : 0
        const runToMaxRatio = kl8Mode
          ? runLength / Math.max(1, maxConsecutive)
          : 0
        return {
          number,
          nextScore,
          coScore,
          nextRateScore,
          coRateScore,
          omissionScore: omission.score,
          heatScore,
          repeatScore,
          recentRepeatScore,
          neighborScore,
          hitRowsScore,
          offsetExactScore,
          offset1Score,
          offset2Score,
          offset3Score,
          neighborMissRate,
          neighborHitCount,
          zoneScore,
          tailScore,
          runLength,
          omissionAverageRatio,
          omissionMaxRatio,
          maxConsecutive,
          runToMaxRatio,
          hitRows: nextCounts.filter(count => count > 0).length,
          maxCount: nextCounts.length ? Math.max(...nextCounts) : 0
        }
      })
      const maxValues = {
        nextScore: Math.max(0, ...rawCandidates.map(item => item.nextScore)),
        coScore: Math.max(0, ...rawCandidates.map(item => item.coScore)),
        nextRateScore: Math.max(0, ...rawCandidates.map(item => item.nextRateScore)),
        coRateScore: Math.max(0, ...rawCandidates.map(item => item.coRateScore)),
        omissionScore: Math.max(0, ...rawCandidates.map(item => item.omissionScore)),
        heatScore: Math.max(0, ...rawCandidates.map(item => item.heatScore)),
        repeatScore: Math.max(0, ...rawCandidates.map(item => item.repeatScore)),
        recentRepeatScore: Math.max(0, ...rawCandidates.map(item => item.recentRepeatScore)),
        neighborScore: Math.max(0, ...rawCandidates.map(item => item.neighborScore)),
        hitRowsScore: Math.max(0, ...rawCandidates.map(item => item.hitRowsScore)),
        offsetExactScore: Math.max(0, ...rawCandidates.map(item => item.offsetExactScore)),
        offset1Score: Math.max(0, ...rawCandidates.map(item => item.offset1Score)),
        offset2Score: Math.max(0, ...rawCandidates.map(item => item.offset2Score)),
        offset3Score: Math.max(0, ...rawCandidates.map(item => item.offset3Score)),
        neighborMissRate: Math.max(0, ...rawCandidates.map(item => item.neighborMissRate)),
        neighborHitCount: Math.max(0, ...rawCandidates.map(item => item.neighborHitCount)),
        omissionAverageRatio: Math.max(0, ...rawCandidates.map(item => item.omissionAverageRatio)),
        omissionMaxRatio: Math.max(0, ...rawCandidates.map(item => item.omissionMaxRatio)),
        maxConsecutive: Math.max(0, ...rawCandidates.map(item => item.maxConsecutive)),
        runToMaxRatio: Math.max(0, ...rawCandidates.map(item => item.runToMaxRatio)),
        zoneScore: Math.max(0, ...rawCandidates.map(item => item.zoneScore)),
        tailScore: Math.max(0, ...rawCandidates.map(item => item.tailScore))
      }
      const weights = kl8Mode
        ? { next: 0.1, nextRate: 0.18, co: 0.04, coRate: 0.06, omission: 0.03, omissionAverage: 0.025, omissionMax: 0.02, maxConsecutive: 0.01, runToMax: 0.035, heat: 0.05, repeat: 0.07, recentRepeat: 0.07, neighbor: 0.05, hitRows: 0.06, offsetExact: 0.1, offset1: 0.08, offset2: 0.05, offset3: 0.03, neighborMiss: 0.025, neighborHit: 0.025, zone: 0.015, tail: 0.015 }
        : { next: 0.34, nextRate: 0.12, co: 0.12, coRate: 0.08, omission: 0.11, omissionAverage: 0, omissionMax: 0, maxConsecutive: 0, runToMax: 0, heat: 0.06, repeat: 0.04, recentRepeat: 0.04, neighbor: 0.06, hitRows: 0.03, offsetExact: 0, offset1: 0, offset2: 0, offset3: 0, neighborMiss: 0, neighborHit: 0, zone: 0, tail: 0 }
      return rawCandidates.map(candidate => {
        const nextScore = this.normalizeScore(candidate.nextScore, maxValues.nextScore)
        const coScore = this.normalizeScore(candidate.coScore, maxValues.coScore)
        const nextRateScore = this.normalizeScore(candidate.nextRateScore, maxValues.nextRateScore)
        const coRateScore = this.normalizeScore(candidate.coRateScore, maxValues.coRateScore)
        const omissionScore = this.normalizeScore(candidate.omissionScore, maxValues.omissionScore)
        const heatScore = this.normalizeScore(candidate.heatScore, maxValues.heatScore)
        const repeatScore = this.normalizeScore(candidate.repeatScore, maxValues.repeatScore)
        const recentRepeatScore = this.normalizeScore(candidate.recentRepeatScore, maxValues.recentRepeatScore)
        const neighborScore = this.normalizeScore(candidate.neighborScore, maxValues.neighborScore)
        const hitRowsScore = this.normalizeScore(candidate.hitRowsScore, maxValues.hitRowsScore)
        const offsetExactScore = this.normalizeScore(candidate.offsetExactScore, maxValues.offsetExactScore)
        const offset1Score = this.normalizeScore(candidate.offset1Score, maxValues.offset1Score)
        const offset2Score = this.normalizeScore(candidate.offset2Score, maxValues.offset2Score)
        const offset3Score = this.normalizeScore(candidate.offset3Score, maxValues.offset3Score)
        const neighborMissScore = this.normalizeScore(candidate.neighborMissRate, maxValues.neighborMissRate)
        const neighborHitScore = this.normalizeScore(candidate.neighborHitCount, maxValues.neighborHitCount)
        const omissionAverageScore = this.normalizeScore(candidate.omissionAverageRatio, maxValues.omissionAverageRatio)
        const omissionMaxScore = this.normalizeScore(candidate.omissionMaxRatio, maxValues.omissionMaxRatio)
        const maxConsecutiveScore = this.normalizeScore(candidate.maxConsecutive, maxValues.maxConsecutive)
        const runToMaxScore = this.normalizeScore(candidate.runToMaxRatio, maxValues.runToMaxRatio)
        const zoneScore = this.normalizeScore(candidate.zoneScore, maxValues.zoneScore)
        const tailScore = this.normalizeScore(candidate.tailScore, maxValues.tailScore)
        const runExclusionScore = kl8Mode && candidate.runLength >= this.kl8RunExclusionThreshold()
          ? 1000
          : 0
        return {
          ...candidate,
          score: nextScore * weights.next
            + nextRateScore * weights.nextRate
            + coScore * weights.co
            + coRateScore * weights.coRate
            + omissionScore * weights.omission
            + heatScore * weights.heat
            + repeatScore * weights.repeat
            + recentRepeatScore * weights.recentRepeat
            + neighborScore * weights.neighbor
            + hitRowsScore * weights.hitRows
            + offsetExactScore * weights.offsetExact
            + offset1Score * weights.offset1
            + offset2Score * weights.offset2
            + offset3Score * weights.offset3
            - neighborMissScore * weights.neighborMiss
            + neighborHitScore * weights.neighborHit
            + omissionAverageScore * weights.omissionAverage
            + omissionMaxScore * weights.omissionMax
            + maxConsecutiveScore * weights.maxConsecutive
            - runToMaxScore * weights.runToMax
            + zoneScore * weights.zone
            + tailScore * weights.tail
            - runExclusionScore,
          detail: { nextScore, nextRateScore, coScore, coRateScore, omissionScore, omissionAverageScore, omissionMaxScore, maxConsecutiveScore, runToMaxScore, heatScore, repeatScore, recentRepeatScore, neighborScore, hitRowsScore, offsetExactScore, offset1Score, offset2Score, offset3Score, neighborMissScore, neighborHitScore, zoneScore, tailScore }
        }
      }).sort((left, right) => this.sortRecommendationCandidate(left, right))
    },
    recommendationSets(area, count) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const candidates = this.recommendationCandidates(area, cooccurrenceArea)
      const size = Math.max(1, count)
      if (!candidates.length) {
        return []
      }
      if (this.isStructureArea(area, size)) {
        return this.kl8RecommendationSets(area, candidates, cooccurrenceArea, size)
      }
      const strategies = [
        {
          source: '综合高分',
          selected: this.pickCandidates(candidates, size, candidate => candidate.score)
        },
        {
          source: '高频延续',
          selected: this.pickCandidates(candidates, size, candidate => {
            return candidate.detail.nextScore * 0.38
              + candidate.detail.nextRateScore * 0.28
              + candidate.detail.hitRowsScore * 0.16
              + candidate.detail.heatScore * 0.18
          })
        },
        {
          source: '遗漏回补',
          selected: this.pickCandidates(candidates, size, candidate => candidate.detail.omissionScore * 0.7 + candidate.score * 0.3)
        },
        {
          source: '共现增强',
          selected: this.pickCooccurrenceGroup(candidates, size, cooccurrenceArea)
        },
        {
          source: this.isStructureArea(area, size) ? '结构均衡' : '低重复均衡',
          selected: this.pickBalancedGroup(candidates, size, area)
        }
      ]
      return strategies.map(strategy => this.buildRecommendationGroup(strategy.selected, strategy.source, cooccurrenceArea))
    },
    avoidSets(area, count, recommendedNumbers = []) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const recommendedSet = new Set(recommendedNumbers)
      const allCandidates = this.recommendationCandidates(area, cooccurrenceArea)
      const candidates = allCandidates
        .filter(candidate => !recommendedSet.has(candidate.number))
        .concat(allCandidates.filter(candidate => recommendedSet.has(candidate.number)))
        .slice()
        .sort((left, right) => {
          const leftPenalty = recommendedSet.has(left.number) ? 1000 : 0
          const rightPenalty = recommendedSet.has(right.number) ? 1000 : 0
          if (leftPenalty !== rightPenalty) {
            return leftPenalty - rightPenalty
          }
          const leftRisk = this.avoidRiskScore(left)
          const rightRisk = this.avoidRiskScore(right)
          if (leftRisk !== rightRisk) {
            return leftRisk - rightRisk
          }
          if (left.score !== right.score) {
            return left.score - right.score
          }
          return Number(left.number) - Number(right.number)
        })
      const size = Math.max(1, count)
      const groups = []
      const usedCounts = {}
      while (groups.length < 5 && candidates.length) {
        const selected = candidates
          .map(candidate => ({
            ...candidate,
            adjustedScore: candidate.score + (usedCounts[candidate.number] || 0) * 18
          }))
          .sort((left, right) => {
            if (left.adjustedScore !== right.adjustedScore) {
              return left.adjustedScore - right.adjustedScore
            }
            const leftRisk = this.avoidRiskScore(left)
            const rightRisk = this.avoidRiskScore(right)
            if (leftRisk !== rightRisk) {
              return leftRisk - rightRisk
            }
            return Number(left.number) - Number(right.number)
          })
          .slice(0, size)
        selected.forEach(item => {
          usedCounts[item.number] = (usedCounts[item.number] || 0) + 1
        })
        groups.push({
          numbers: selected
            .map(item => item.number)
            .sort((left, right) => Number(left) - Number(right)),
          source: '低分候选',
          totalScore: selected.reduce((sum, item) => sum + this.avoidRiskScore(item), 0),
          hitCount: selected.filter(item => item.nextScore > 0).length,
          maxCount: selected.length ? Math.max(...selected.map(item => item.nextScore)) : 0
        })
      }
      return groups
    },
    avoidRiskScore(candidate) {
      return candidate.detail.nextScore * 0.24
        + candidate.detail.nextRateScore * 0.28
        + candidate.detail.coScore * 0.12
        + candidate.detail.coRateScore * 0.12
        + candidate.detail.hitRowsScore * 0.12
        + candidate.detail.heatScore * 0.12
        + candidate.detail.repeatScore * 0.1
        + candidate.detail.recentRepeatScore * 0.14
        + candidate.detail.neighborScore * 0.1
        + candidate.detail.zoneScore * 0.025
        + candidate.detail.tailScore * 0.025
    },
    coreRecommendationSets(area, size) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const candidates = this.recommendationCandidates(area, cooccurrenceArea)
      if (!candidates.length) {
        return []
      }
      const selectedArea = this.selectedAreas.find(item => item.key === area.key)
      const currentNumbers = selectedArea ? selectedArea.numbers : []
      const profile = this.kl8Profile(area, currentNumbers, size)
      const consensus = this.twentyCodeConsensus(area.key)
      const firstSelected = this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
        return candidate.score
          + this.cappedConsensusScore(candidate.number, consensus) * 8
          + candidate.detail.nextRateScore * 0.35
          + candidate.detail.nextScore * 0.16
          + candidate.detail.hitRowsScore * 0.18
          + candidate.detail.heatScore * 0.12
          + candidate.detail.recentRepeatScore * 0.14
          + candidate.detail.neighborScore * 0.12
      })
      const firstNumbers = new Set(firstSelected.map(item => item.number))
      const strategies = [
        {
          source: '核心综合',
          selected: firstSelected
        },
        {
          source: '核心高频',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.detail.nextRateScore * 0.48
              + candidate.detail.nextScore * 0.22
              + candidate.detail.hitRowsScore * 0.18
              + this.cappedConsensusScore(candidate.number, consensus) * 9
              + candidate.detail.heatScore * 0.14
              + candidate.detail.repeatScore * 0.12
              + candidate.detail.recentRepeatScore * 0.12
              + candidate.detail.neighborScore * 0.08
              - (firstNumbers.has(candidate.number) ? 1000 : 0)
          }, { repeatOffset: 1 })
        }
      ]
      return strategies.map(strategy => this.buildRecommendationGroup(strategy.selected, strategy.source, cooccurrenceArea, profile))
    },
    playRecommendationSets(area, size) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const candidates = this.recommendationCandidates(area, cooccurrenceArea)
      if (!candidates.length) {
        return []
      }
      const selectedArea = this.selectedAreas.find(item => item.key === area.key)
      const currentNumbers = selectedArea ? selectedArea.numbers : []
      const profile = this.kl8Profile(area, currentNumbers, size)
      const consensus = this.recommendationConsensus(area.key)
      const strategies = [
        {
          source: '共识综合',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score
              + this.consensusScore(candidate.number, consensus) * 18
              + candidate.detail.nextRateScore * 0.34
              + candidate.detail.nextScore * 0.16
              + candidate.detail.hitRowsScore * 0.14
              + candidate.detail.heatScore * 0.12
              + candidate.detail.recentRepeatScore * 0.12
              + candidate.detail.neighborScore * 0.1
          })
        },
        {
          source: '共识共现',
          selected: this.pickCooccurrenceGroup(
            this.pickCandidates(candidates, Math.max(size * 4, 12), candidate => {
              return candidate.score
                + this.consensusScore(candidate.number, consensus) * 20
                + candidate.detail.coRateScore * 0.26
                + candidate.detail.coScore * 0.16
                + candidate.detail.nextRateScore * 0.18
                + candidate.detail.nextScore * 0.1
                + candidate.detail.recentRepeatScore * 0.12
            }),
            size,
            cooccurrenceArea
          )
        }
      ]
      return strategies.map(strategy => this.buildRecommendationGroup(strategy.selected, strategy.source, cooccurrenceArea, profile))
    },
    twentyCodeConsensus(areaKey) {
      const result = {}
      this.recommendationGroups.forEach(group => {
        const area = group.areas.find(item => item.key === areaKey)
        if (!area) {
          return
        }
        area.numbers.forEach(number => {
          if (!result[number]) {
            result[number] = {
              count: 0,
              weighted: 0,
              sources: []
            }
          }
          result[number].count += 1
          result[number].weighted += 1
          result[number].sources.push(area.source)
        })
      })
      return result
    },
    recommendationConsensus(areaKey) {
      const result = {}
      const addNumbers = (numbers, weight, source) => {
        numbers.forEach(number => {
          if (!result[number]) {
            result[number] = {
              count: 0,
              weighted: 0,
              sources: []
            }
          }
          result[number].count += 1
          result[number].weighted += weight
          result[number].sources.push(source)
        })
      }
      this.recommendationGroups.forEach(group => {
        const area = group.areas.find(item => item.key === areaKey)
        if (area) {
          addNumbers(area.numbers, 1, area.source)
        }
      })
      this.coreRecommendationGroups.forEach(group => {
        const area = group.areas.find(item => item.key === areaKey)
        if (area) {
          addNumbers(area.numbers, 2, area.source)
        }
      })
      return result
    },
    consensusScore(number, consensus) {
      const item = consensus[number]
      if (!item) {
        return 0
      }
      return item.weighted + item.count * 0.5
    },
    cappedConsensusScore(number, consensus) {
      return Math.min(this.consensusScore(number, consensus), 3)
    },
    kl8TierPrediction(area, candidates, currentNumbers) {
      if (!area || !candidates.length) {
        return {
          recommend: [],
          middle: [],
          avoid: []
        }
      }
      const pickSize = 20
      const profile = this.kl8Profile(area, currentNumbers, pickSize)
      const legacyConsensus = this.kl8LegacyStrategyConsensus(candidates, area, profile, pickSize)
      const transferScore = candidate => {
        return candidate.score
          + this.cappedConsensusScore(candidate.number, legacyConsensus) * 7
          + candidate.detail.nextRateScore * 0.35
          + candidate.detail.hitRowsScore * 0.16
          + candidate.detail.coRateScore * 0.12
          + candidate.detail.coScore * 0.04
          + candidate.detail.offsetExactScore * 0.1
          + candidate.detail.offset1Score * 0.07
          + candidate.detail.offset2Score * 0.04
          + candidate.detail.offset3Score * 0.02
          - candidate.detail.neighborMissScore * 0.08
          + candidate.detail.neighborHitScore * 0.06
          + candidate.detail.omissionAverageScore * 0.08
          + candidate.detail.omissionMaxScore * 0.06
          + candidate.detail.maxConsecutiveScore * 0.025
          - candidate.detail.runToMaxScore * 0.1
          + candidate.detail.recentRepeatScore * 0.05
          + candidate.detail.neighborScore * 0.04
          + candidate.detail.repeatScore * 0.05
          + candidate.detail.heatScore * 0.03
          + candidate.detail.zoneScore * 0.02
          + candidate.detail.tailScore * 0.02
      }
      const firstLayer = this.pickKl8ProfileGroup(
        candidates,
        pickSize,
        area,
        profile,
        transferScore
      )
      const firstNumbers = new Set(firstLayer.map(item => item.number))
      const secondLayer = this.pickKl8ProfileGroup(
        candidates,
        pickSize,
        area,
        profile,
        candidate => {
          return transferScore(candidate)
            + (firstNumbers.has(candidate.number) ? 18 : 0)
            + this.cappedConsensusScore(candidate.number, legacyConsensus) * 4
            + candidate.detail.nextRateScore * 0.18
            + candidate.detail.recentRepeatScore * 0.1
            + candidate.detail.neighborScore * 0.08
            + candidate.detail.offset1Score * 0.06
            + candidate.detail.offset2Score * 0.03
            + candidate.detail.offset3Score * 0.015
            + candidate.detail.omissionAverageScore * 0.04
            + candidate.detail.omissionMaxScore * 0.03
            - candidate.detail.runToMaxScore * 0.05
        },
        { repeatOffset: 1 }
      )
      const consensus = {}
      firstLayer.forEach(item => {
        consensus[item.number] = (consensus[item.number] || 0) + 1.15
      })
      secondLayer.forEach(item => {
        consensus[item.number] = (consensus[item.number] || 0) + 1
      })
      const ranked = candidates
        .slice()
        .sort((left, right) => {
          const leftScore = (consensus[left.number] || 0) * 36 + transferScore(left)
          const rightScore = (consensus[right.number] || 0) * 36 + transferScore(right)
          if (rightScore !== leftScore) {
            return rightScore - leftScore
          }
          return this.sortRecommendationCandidate(left, right)
        })
      const sortNumbers = items => items.slice().sort((left, right) => Number(left.number) - Number(right.number))
      return {
        recommend: sortNumbers(ranked.slice(0, 20)),
        middle: sortNumbers(ranked.slice(20, 70)),
        avoid: sortNumbers(ranked.slice(70, 80)),
        ranked
      }
    },
    kl8LegacyStrategyConsensus(candidates, area, profile, size) {
      const strategies = [
        {
          weight: 1.25,
          selected: this.kl8StableTransferGroup(candidates, size, area, profile)
        },
        {
          weight: 1,
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score
              + candidate.detail.nextRateScore * 0.22
              + candidate.detail.repeatScore * 0.32
              + candidate.detail.recentRepeatScore * 0.22
              + candidate.detail.hitRowsScore * 0.16
              + candidate.detail.heatScore * 0.12
          }, { repeatOffset: 1 })
        },
        {
          weight: 0.85,
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score + candidate.detail.nextRateScore * 0.18 + candidate.detail.zoneScore * 0.42
          })
        },
        {
          weight: 0.85,
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score + candidate.detail.nextRateScore * 0.18 + candidate.detail.tailScore * 0.42
          })
        },
        {
          weight: 0.9,
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.detail.omissionScore * 0.22
              + candidate.detail.nextRateScore * 0.36
              + candidate.detail.nextScore * 0.2
              + candidate.detail.coRateScore * 0.12
              + candidate.detail.coScore * 0.1
          }, { repeatOffset: -1 })
        }
      ]
      return strategies.reduce((result, strategy) => {
        strategy.selected.forEach(candidate => {
          if (!result[candidate.number]) {
            result[candidate.number] = {
              count: 0,
              weighted: 0
            }
          }
          result[candidate.number].count += 1
          result[candidate.number].weighted += strategy.weight
        })
        return result
      }, {})
    },
    kl8RecommendationSets(area, candidates, cooccurrenceArea, size) {
      const selectedArea = this.selectedAreas.find(item => item.key === area.key)
      const currentNumbers = selectedArea ? selectedArea.numbers : []
      const profile = this.kl8Profile(area, currentNumbers)
      const strategies = [
        {
          source: '快乐8十期反馈稳定',
          selected: this.kl8StableTransferGroup(candidates, size, area, profile)
        },
        {
          source: '快乐8重号延续',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score
              + candidate.detail.nextRateScore * 0.22
              + candidate.detail.repeatScore * 0.32
              + candidate.detail.recentRepeatScore * 0.22
              + candidate.detail.hitRowsScore * 0.16
              + candidate.detail.heatScore * 0.12
          }, { repeatOffset: 1 })
        },
        {
          source: '快乐8分区均衡',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score + candidate.detail.nextRateScore * 0.18 + candidate.detail.zoneScore * 0.42
          })
        },
        {
          source: '快乐8尾数均衡',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.score + candidate.detail.nextRateScore * 0.18 + candidate.detail.tailScore * 0.42
          })
        },
        {
          source: '快乐8回补混合',
          selected: this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
            return candidate.detail.omissionScore * 0.22
              + candidate.detail.nextRateScore * 0.36
              + candidate.detail.nextScore * 0.2
              + candidate.detail.coRateScore * 0.12
              + candidate.detail.coScore * 0.1
          }, { repeatOffset: -1 })
        }
      ]
      return strategies.map(strategy => this.buildRecommendationGroup(strategy.selected, strategy.source, cooccurrenceArea, profile))
    },
    kl8StableTransferGroup(candidates, size, area, profile) {
      const stableSelected = this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
        return candidate.score
          + candidate.detail.nextRateScore * 0.28
          + candidate.detail.hitRowsScore * 0.2
          + candidate.detail.coRateScore * 0.12
          + candidate.detail.offsetExactScore * 0.12
          + candidate.detail.offset1Score * 0.08
          + candidate.detail.offset2Score * 0.04
          + candidate.detail.offset3Score * 0.02
      })
      const stableNumbers = new Set(stableSelected.map(item => item.number))
      const secondSelected = this.pickKl8ProfileGroup(candidates, size, area, profile, candidate => {
        return candidate.score
          + (stableNumbers.has(candidate.number) ? 18 : 0)
          + candidate.detail.nextRateScore * 0.18
          + candidate.detail.recentRepeatScore * 0.1
          + candidate.detail.neighborScore * 0.08
          + candidate.detail.offset1Score * 0.08
          + candidate.detail.offset2Score * 0.04
          + candidate.detail.offset3Score * 0.02
      }, { repeatOffset: 1 })
      const consensus = {}
      stableSelected.forEach(item => {
        consensus[item.number] = (consensus[item.number] || 0) + 1.15
      })
      secondSelected.forEach(item => {
        consensus[item.number] = (consensus[item.number] || 0) + 1
      })
      return candidates
        .slice()
        .sort((left, right) => {
          const leftScore = (consensus[left.number] || 0) * 40
            + left.score
            + left.detail.nextRateScore * 0.3
            + left.detail.hitRowsScore * 0.2
          const rightScore = (consensus[right.number] || 0) * 40
            + right.score
            + right.detail.nextRateScore * 0.3
            + right.detail.hitRowsScore * 0.2
          if (rightScore !== leftScore) {
            return rightScore - leftScore
          }
          return this.sortRecommendationCandidate(left, right)
        })
        .slice(0, size)
    },
    pickKl8ProfileGroup(candidates, size, area, profile, scoreGetter, options = {}) {
      const selected = []
      const pool = candidates.slice()
      const repeatTarget = Math.max(0, profile.repeatTarget + (options.repeatOffset || 0))
      while (selected.length < size && pool.length) {
        const next = pool
          .slice()
          .sort((left, right) => {
            const leftScore = scoreGetter(left) + this.kl8FitIncrement(left, selected, profile, repeatTarget)
            const rightScore = scoreGetter(right) + this.kl8FitIncrement(right, selected, profile, repeatTarget)
            if (rightScore !== leftScore) {
              return rightScore - leftScore
            }
            return this.sortRecommendationCandidate(left, right)
          })[0]
        selected.push(next)
        pool.splice(pool.findIndex(item => item.number === next.number), 1)
      }
      return selected.slice(0, size)
    },
    kl8FitIncrement(candidate, selected, profile, repeatTarget) {
      const nextSelected = [...selected, candidate]
      const number = Number(candidate.number)
      const zone = this.zoneIndex(candidate.number)
      const tail = number % 10
      const zoneCount = nextSelected.filter(item => this.zoneIndex(item.number) === zone).length
      const tailCount = nextSelected.filter(item => Number(item.number) % 10 === tail).length
      const oddCount = nextSelected.filter(item => Number(item.number) % 2 === 1).length
      const bigCount = nextSelected.filter(item => Number(item.number) > 40).length
      const repeatCount = nextSelected.filter(item => item.repeatScore > 0).length
      const zoneBonus = zoneCount <= (profile.zoneTargets[zone] || 0) ? 15 : -18 * (zoneCount - (profile.zoneTargets[zone] || 0))
      const tailBonus = tailCount <= (profile.tailTargets[tail] || 0) ? 8 : -10 * (tailCount - (profile.tailTargets[tail] || 0))
      const oddBonus = Math.abs(oddCount - profile.oddTarget) <= 2 ? 6 : -4
      const bigBonus = Math.abs(bigCount - profile.bigTarget) <= 2 ? 6 : -4
      const repeatBonus = repeatCount <= repeatTarget ? 10 : -16 * (repeatCount - repeatTarget)
      return zoneBonus + tailBonus + oddBonus + bigBonus + repeatBonus
    },
    kl8Profile(area, currentNumbers, targetSize = null) {
      const pairs = area.pairs || []
      const nextSets = pairs.map(pair => pair.nextBalls || []).filter(numbers => numbers.length)
      const currentSets = pairs.map(pair => pair.currentBalls || [])
      const sourceSize = currentNumbers.length || 20
      const size = targetSize || sourceSize
      const zoneCount = Math.ceil(area.max / 10)
      const average = (values, fallback) => {
        return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : fallback
      }
      const zoneAverages = Array.from({ length: zoneCount }, (_, zone) => {
        return average(nextSets.map(numbers => numbers.filter(number => this.zoneIndex(number) === zone).length), size / zoneCount)
      })
      const tailAverages = Array.from({ length: 10 }, (_, tail) => {
        return average(nextSets.map(numbers => numbers.filter(number => Number(number) % 10 === tail).length), size / 10)
      })
      const repeatValues = pairs.map((pair, index) => {
        const source = currentSets[index] || []
        return (pair.nextBalls || []).filter(number => source.includes(number)).length
      })
      const oddTarget = Math.round(average(nextSets.map(numbers => numbers.filter(number => Number(number) % 2 === 1).length), size / 2))
      const bigTarget = Math.round(average(nextSets.map(numbers => numbers.filter(number => Number(number) > 40).length), size / 2))
      return {
        repeatTarget: Math.round(average(repeatValues, Math.round(sourceSize * 0.25)) * (size / sourceSize)),
        oddTarget,
        bigTarget,
        zoneAverage: size / zoneCount,
        tailAverage: size / 10,
        zoneTargets: this.distributeTargets(zoneAverages, size, zoneCount),
        tailTargets: this.distributeTargets(tailAverages, size, 10)
      }
    },
    distributeTargets(averages, total, count) {
      const baseTargets = averages.map(value => Math.max(0, Math.floor(value)))
      let assigned = baseTargets.reduce((sum, value) => sum + value, 0)
      const order = averages
        .map((value, index) => ({ index, rest: value - Math.floor(value) }))
        .sort((left, right) => {
          if (right.rest !== left.rest) {
            return right.rest - left.rest
          }
          return left.index - right.index
        })
      let pointer = 0
      while (assigned < total && order.length) {
        baseTargets[order[pointer % order.length].index] += 1
        assigned += 1
        pointer += 1
      }
      while (assigned > total) {
        const index = baseTargets
          .map((value, targetIndex) => ({ value, targetIndex }))
          .sort((left, right) => {
            if (right.value !== left.value) {
              return right.value - left.value
            }
            return left.targetIndex - right.targetIndex
          })[0].targetIndex
        baseTargets[index] -= 1
        assigned -= 1
      }
      while (baseTargets.length < count) {
        baseTargets.push(0)
      }
      return baseTargets
    },
    targetShareScore(target, average) {
      return average > 0 ? target / average : target
    },
    kl8RunExclusionThreshold() {
      return 3
    },
    kl8CurrentRun(number, records, fields) {
      let runLength = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        const balls = this.ballsFromRecord(records[index], fields)
        if (!balls.includes(number)) {
          break
        }
        runLength += 1
      }
      return runLength
    },
    pickCandidates(candidates, size, scoreGetter) {
      return candidates
        .slice()
        .sort((left, right) => {
          const leftScore = scoreGetter(left)
          const rightScore = scoreGetter(right)
          if (rightScore !== leftScore) {
            return rightScore - leftScore
          }
          return this.sortRecommendationCandidate(left, right)
        })
        .slice(0, size)
    },
    pickCooccurrenceGroup(candidates, size, cooccurrenceArea) {
      const selected = []
      const pool = candidates.slice()
      while (selected.length < size && pool.length) {
        const next = pool
          .slice()
          .sort((left, right) => {
            const leftScore = left.score + this.candidateCooccurrenceBonus(left, selected, cooccurrenceArea)
            const rightScore = right.score + this.candidateCooccurrenceBonus(right, selected, cooccurrenceArea)
            if (rightScore !== leftScore) {
              return rightScore - leftScore
            }
            return this.sortRecommendationCandidate(left, right)
          })[0]
        selected.push(next)
        pool.splice(pool.findIndex(item => item.number === next.number), 1)
      }
      return selected
    },
    pickBalancedGroup(candidates, size, area) {
      if (!this.isStructureArea(area, size)) {
        return this.pickCandidates(candidates, size, candidate => candidate.score - candidate.detail.repeatScore * 0.18)
      }
      const selected = []
      const targetPerZone = this.zoneTargets(area, size)
      targetPerZone.forEach(target => {
        const zoneCandidates = candidates.filter(candidate => this.zoneIndex(candidate.number) === target.zone)
        selected.push(...this.pickCandidates(zoneCandidates, target.count, candidate => candidate.score))
      })
      const selectedNumbers = selected.map(item => item.number)
      const remaining = candidates.filter(candidate => !selectedNumbers.includes(candidate.number))
      while (selected.length < size && remaining.length) {
        const next = remaining
          .slice()
          .sort((left, right) => {
            const leftScore = left.score + this.structureFitBonus([...selected, left], area)
            const rightScore = right.score + this.structureFitBonus([...selected, right], area)
            if (rightScore !== leftScore) {
              return rightScore - leftScore
            }
            return this.sortRecommendationCandidate(left, right)
          })[0]
        selected.push(next)
        remaining.splice(remaining.findIndex(item => item.number === next.number), 1)
      }
      return selected.slice(0, size)
    },
    buildRecommendationGroup(selected, source, cooccurrenceArea, profile = null) {
      const coBonus = this.groupCooccurrenceBonus(selected, cooccurrenceArea)
      const structureBonus = this.structureFitBonus(selected, profile)
      return {
        numbers: selected
          .map(item => item.number)
          .sort((left, right) => Number(left) - Number(right)),
        source,
        totalScore: selected.reduce((sum, item) => sum + item.score, 0) + coBonus + structureBonus,
        hitCount: selected.filter(item => item.nextScore > 0).length,
        maxCount: selected.length ? Math.max(...selected.map(item => item.nextScore)) : 0
      }
    },
    isStructureArea(area, size) {
      return Boolean(area && area.max >= 80 && size >= 20)
    },
    zoneIndex(number) {
      return Math.floor((Number(number) - 1) / 10)
    },
    zoneTargets(area, size) {
      const zoneCount = Math.ceil(area.max / 10)
      const base = Math.floor(size / zoneCount)
      let extra = size % zoneCount
      return Array.from({ length: zoneCount }, (_, zone) => {
        const useExtra = extra > 0
        const count = base + (useExtra ? 1 : 0)
        if (useExtra) {
          extra -= 1
        }
        return { zone, count }
      })
    },
    structureFitBonus(selected, profile = null) {
      if (selected.length < 20) {
        return 0
      }
      const numbers = selected.map(item => Number(item.number))
      const oddCount = numbers.filter(number => number % 2 === 1).length
      const bigCount = numbers.filter(number => number > 40).length
      const oddTarget = profile ? profile.oddTarget : selected.length / 2
      const bigTarget = profile ? profile.bigTarget : selected.length / 2
      const oddPenalty = Math.abs(oddCount - oddTarget)
      const bigPenalty = Math.abs(bigCount - bigTarget)
      const zones = selected.reduce((result, item) => {
        const zone = this.zoneIndex(item.number)
        result[zone] = (result[zone] || 0) + 1
        return result
      }, {})
      const zonePenalty = Object.keys(zones).reduce((sum, zone) => {
        const target = profile ? (profile.zoneTargets[zone] || 0) : 4
        return sum + Math.max(0, zones[zone] - target)
      }, 0)
      return Math.max(0, 16 - oddPenalty * 2 - bigPenalty * 2 - zonePenalty * 3)
    },
    candidateCooccurrenceBonus(candidate, selected, cooccurrenceArea) {
      if (!cooccurrenceArea || !selected.length) {
        return 0
      }
      const rows = cooccurrenceArea.allRows || cooccurrenceArea.rows
      return selected.reduce((sum, item) => {
        const row = rows.find(rowItem => rowItem.number === candidate.number)
        const cell = row ? row.cells.find(cellItem => cellItem.number === item.number) : null
        return sum + (cell ? cell.count * 3 : 0)
      }, 0)
    },
    sortRecommendationCandidate(left, right) {
      if (right.score !== left.score) {
        return right.score - left.score
      }
      if (right.nextScore !== left.nextScore) {
        return right.nextScore - left.nextScore
      }
      if (right.coScore !== left.coScore) {
        return right.coScore - left.coScore
      }
      return Number(left.number) - Number(right.number)
    },
    normalizeScore(value, maxValue) {
      return maxValue > 0 ? (value / maxValue) * 100 : 0
    },
    neighborNumbers(numbers, max) {
      const result = new Set()
      numbers.forEach(number => {
        const value = Number(number)
        if (value > 1) {
          result.add(String(value - 1).padStart(2, '0'))
        }
        if (value < max) {
          result.add(String(value + 1).padStart(2, '0'))
        }
      })
      numbers.forEach(number => result.delete(number))
      return Array.from(result)
    },
    ballsFromRecord(record, fields) {
      return fields.map(field => String(record[field] || '').padStart(2, '0'))
    },
    appearCount(number, records, fields) {
      return records.filter(record => fields.some(field => String(record[field] || '').padStart(2, '0') === number)).length
    },
    recentRepeatCount(number, records, fields) {
      return records.slice(Math.max(0, records.length - 5)).filter(record => {
        return fields.some(field => String(record[field] || '').padStart(2, '0') === number)
      }).length
    },
    omissionStats(number, records, fields) {
      let current = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        const appeared = fields.some(field => String(records[index][field] || '').padStart(2, '0') === number)
        if (appeared) {
          break
        }
        current += 1
      }
      let miss = 0
      const misses = []
      let consecutive = 0
      let maxConsecutive = 0
      records.forEach(record => {
        const appeared = fields.some(field => String(record[field] || '').padStart(2, '0') === number)
        if (appeared) {
          misses.push(miss)
          miss = 0
          consecutive += 1
          maxConsecutive = Math.max(maxConsecutive, consecutive)
          return
        }
        miss += 1
        consecutive = 0
      })
      misses.push(miss)
      const average = misses.length ? misses.reduce((sum, value) => sum + value, 0) / misses.length : 0
      const max = misses.length ? Math.max(...misses) : 0
      let score = 0
      if (max > 0) {
        const balancedMiss = Math.min(current, max)
        score = balancedMiss / max
        if (average > 0 && current >= average) {
          score += 0.25
        }
      }
      return {
        current,
        max,
        average,
        maxConsecutive,
        score
      }
    },
    kl8OffsetFeedback(area) {
      const records = area.records || []
      const fields = area.fields || []
      if (records.length < 3 || fields.length < 20) {
        return null
      }
      const feedback = {
        exact: {},
        near1: {},
        near2: {},
        near3: {},
        predictedMiss: {},
        neighborMiss: {},
        neighborHit: {}
      }
      const feedbackWindow = Math.min(records.length, 10)
      const startIndex = Math.max(2, records.length - feedbackWindow)
      for (let index = startIndex; index < records.length - 1; index += 1) {
        const sourceHistory = records.slice(Math.max(0, index - feedbackWindow), index)
        const currentNumbers = fields.map(field => String(records[index][field] || '').padStart(2, '0'))
        const actualNumbers = fields.map(field => String(records[index + 1][field] || '').padStart(2, '0'))
        const actualSet = new Set(actualNumbers)
        const predictedNumbers = this.kl8HistoricalBaselinePrediction(sourceHistory, currentNumbers, fields, area.max)
        actualNumbers.forEach(actualNumber => {
          const distance = predictedNumbers.reduce((min, predictedNumber) => {
            return Math.min(min, Math.abs(Number(actualNumber) - Number(predictedNumber)))
          }, Infinity)
          if (distance === 0) {
            feedback.exact[actualNumber] = (feedback.exact[actualNumber] || 0) + 1
          } else if (distance === 1) {
            feedback.near1[actualNumber] = (feedback.near1[actualNumber] || 0) + 1
          } else if (distance === 2) {
            feedback.near2[actualNumber] = (feedback.near2[actualNumber] || 0) + 1
          } else if (distance === 3) {
            feedback.near3[actualNumber] = (feedback.near3[actualNumber] || 0) + 1
          }
        })
        predictedNumbers.forEach(predictedNumber => {
          if (actualSet.has(predictedNumber)) {
            return
          }
          feedback.predictedMiss[predictedNumber] = (feedback.predictedMiss[predictedNumber] || 0) + 1
          const predictedValue = Number(predictedNumber)
          let nearest = Infinity
          actualNumbers.forEach(actualNumber => {
            nearest = Math.min(nearest, Math.abs(Number(actualNumber) - predictedValue))
          })
          if (nearest < 1 || nearest > 3) {
            return
          }
          const missWeight = nearest === 1 ? 1 : nearest === 2 ? 0.75 : 0.5
          feedback.neighborMiss[predictedNumber] = (feedback.neighborMiss[predictedNumber] || 0) + missWeight
          actualNumbers.forEach(actualNumber => {
            const distance = Math.abs(Number(actualNumber) - predictedValue)
            if (distance >= 1 && distance <= 3) {
              feedback.neighborHit[actualNumber] = (feedback.neighborHit[actualNumber] || 0) + (1 / distance)
            }
          })
        })
      }
      return feedback
    },
    kl8HistoricalBaselinePrediction(records, currentNumbers, fields, max) {
      const numbers = Array.from({ length: max }, (_, index) => String(index + 1).padStart(2, '0'))
      const pairs = []
      for (let index = 0; index < records.length - 1; index += 1) {
        pairs.push({
          currentBalls: fields.map(field => String(records[index][field] || '').padStart(2, '0')),
          nextBalls: fields.map(field => String(records[index + 1][field] || '').padStart(2, '0'))
        })
      }
      const candidates = numbers.map(number => {
        let nextCount = 0
        let nextBaseCount = 0
        let coCount = 0
        let coBaseCount = 0
        let heatCount = 0
        records.forEach(record => {
          const balls = fields.map(field => String(record[field] || '').padStart(2, '0'))
          if (balls.includes(number)) {
            heatCount += 1
          }
          currentNumbers.forEach(currentNumber => {
            if (!balls.includes(currentNumber)) {
              return
            }
            coBaseCount += 1
            if (balls.includes(number) && currentNumber !== number) {
              coCount += 1
            }
          })
        })
        pairs.forEach(pair => {
          currentNumbers.forEach(currentNumber => {
            if (!pair.currentBalls.includes(currentNumber)) {
              return
            }
            nextBaseCount += 1
            if (pair.nextBalls.includes(number)) {
              nextCount += 1
            }
          })
        })
        return {
          number,
          score: (nextCount / Math.max(1, nextBaseCount)) * 0.58
            + (coCount / Math.max(1, coBaseCount)) * 0.27
            + (heatCount / Math.max(1, records.length)) * 0.15
        }
      })
      return candidates
        .sort((left, right) => right.score - left.score || Number(left.number) - Number(right.number))
        .slice(0, 20)
        .map(item => item.number)
    },
    groupCooccurrenceBonus(selected, cooccurrenceArea) {
      if (!cooccurrenceArea || selected.length < 2) {
        return 0
      }
      let bonus = 0
      const rows = cooccurrenceArea.allRows || cooccurrenceArea.rows
      selected.forEach(left => {
        const row = rows.find(item => item.number === left.number)
        if (!row) {
          return
        }
        selected.forEach(right => {
          if (left.number >= right.number) {
            return
          }
          const cell = row.cells.find(item => item.number === right.number)
          bonus += cell ? cell.count * 0.5 : 0
        })
      })
      return bonus
    }
  }
}
</script>
