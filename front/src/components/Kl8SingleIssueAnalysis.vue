<template>
  <section class="single-analysis kl8-single-analysis">
    <div class="single-draw">
      <div class="single-draw-title">
        <strong>第 {{ record ? record.expect : '-' }} 期</strong>
        <span>{{ record ? formatDate(record.drawDate) : '' }}</span>
      </div>
      <div class="single-draw-area">
        <span class="single-area-label">号码</span>
        <span
          v-for="number in currentNumbers"
          :key="'kl8-current-' + number"
          class="ball red"
        >
          {{ number }}
        </span>
      </div>
    </div>

    <article v-if="record && tierPrediction.ranked.length" class="single-panel kl8-tier-panel">
      <header class="single-panel-header">
        <strong>快乐8新版单期分析</strong>
        <span>下期统计为主；新增重号、连号边缘、冷热、尾数区间降权，并对推荐20码做分布和值修正</span>
      </header>
      <div class="kl8-tier-grid">
        <section class="kl8-tier-card kl8-tier-recommend">
          <div class="kl8-tier-title">
            <strong>推荐20码</strong>
            <span>命中 {{ hitCount(tierPrediction.recommend) }}/20</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="item in tierPrediction.recommend"
              :key="'kl8-new-recommend-' + item.number"
              :class="['ball', 'red', { hit: isHit(item.number) }]"
              :title="'排名' + item.rank + '，综合分' + item.scoreText"
            >
              {{ item.number }}
            </span>
          </div>
        </section>

        <section class="kl8-tier-card kl8-tier-middle">
          <div class="kl8-tier-title">
            <strong>观望50码</strong>
            <span>命中 {{ hitCount(tierPrediction.middle) }}/50</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="item in tierPrediction.middle"
              :key="'kl8-new-middle-' + item.number"
              :class="['ball', 'red', { hit: isHit(item.number) }]"
              :title="'排名' + item.rank + '，综合分' + item.scoreText"
            >
              {{ item.number }}
            </span>
          </div>
        </section>

        <section class="kl8-tier-card kl8-tier-avoid">
          <div class="kl8-tier-title">
            <strong>不推荐10码</strong>
            <span>命中 {{ hitCount(tierPrediction.avoid) }}/10</span>
          </div>
          <div class="kl8-tier-numbers">
            <span
              v-for="item in tierPrediction.avoid"
              :key="'kl8-new-avoid-' + item.number"
              :class="['ball', 'red', { hit: isHit(item.number) }]"
              :title="'排名' + item.rank + '，综合分' + item.scoreText"
            >
              {{ item.number }}
            </span>
          </div>
        </section>
      </div>
    </article>

    <article v-if="record && tierPrediction.ranked.length" class="single-panel">
      <header class="single-panel-header">
        <strong>排名明细 Top20</strong>
        <span>展示推荐20码的主要评分来源</span>
      </header>
      <div class="table-wrap">
        <table class="kl8-rank-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>号码</th>
              <th>综合分</th>
              <th>下期</th>
              <th>共现</th>
              <th>当前遗漏</th>
              <th>平均遗漏</th>
              <th>最大遗漏</th>
              <th>当前连出</th>
              <th>最大连出</th>
              <th>热度</th>
              <th>近重</th>
              <th>标签</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tierPrediction.recommend" :key="'kl8-rank-row-' + item.number">
              <td>{{ item.rank }}</td>
              <td><span :class="['ball', 'red', { hit: isHit(item.number) }]">{{ item.number }}</span></td>
              <td>{{ item.scoreText }}</td>
              <td>{{ item.nextScore }}</td>
              <td>{{ item.coScore }}</td>
              <td>{{ item.omission.current }}</td>
              <td>{{ item.omission.averageText }}</td>
              <td>{{ item.omission.max }}</td>
              <td>{{ item.runLength }}</td>
              <td>{{ item.omission.maxConsecutive }}</td>
              <td>{{ item.heatScore }}</td>
              <td>{{ item.recentRepeatScore }}</td>
              <td>{{ item.tags.join('、') || '-' }}</td>
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

const MAX_NUMBER = 80

export default {
  name: 'Kl8SingleIssueAnalysis',
  data() {
    return {
      optimizationRuns: []
    }
  },
  props: {
    record: { type: Object, default: null },
    nextRecord: { type: Object, default: null },
    selectedAreas: { type: Array, required: true },
    cooccurrenceAreas: { type: Array, required: true },
    nextOccurrenceAreas: { type: Array, required: true },
    historyRecords: { type: Array, default: () => [] },
    recentCount: { type: Number, required: true },
    weightVersion: { type: Number, default: 0 },
    formatDate: { type: Function, required: true }
  },
  computed: {
    currentArea() {
      return this.selectedAreas.find(area => area.key === 'front') || { numbers: [] }
    },
    currentNumbers() {
      return this.currentArea.numbers || []
    },
    dynamicWeights() {
      const totals = {}
      const counts = {}
      const windowWeights = {
        3: 0.6,
        100: 0.2,
        200: 0.12,
        500: 0.08
      }
      this.optimizationRuns.forEach(result => {
        const runWindow = result && result.run ? Number(result.run.windowSize) : 0
        const runWeight = windowWeights[runWindow] || 0
        if (!runWeight) {
          return
        }
        ;(result.weights || []).forEach(weight => {
          totals[weight.featureName] = (totals[weight.featureName] || 0) + Number(weight.weight || 0) * runWeight
          counts[weight.featureName] = (counts[weight.featureName] || 0) + runWeight
        })
      })
      return Object.keys(totals).reduce((result, key) => {
        result[key] = totals[key] / counts[key]
        return result
      }, {})
    },
    nextArea() {
      return this.nextOccurrenceAreas.find(area => area.key === 'front') || null
    },
    coArea() {
      return this.cooccurrenceAreas.find(area => area.key === 'front') || null
    },
    allNumbers() {
      return Array.from({ length: MAX_NUMBER }, (_, index) => String(index + 1).padStart(2, '0'))
    },
    consecutiveEdgeNumbers() {
      const sorted = this.currentNumbers.map(number => Number(number)).sort((left, right) => left - right)
      const edges = new Set()
      let group = []
      const flush = () => {
        if (group.length < 2) {
          return
        }
        const left = group[0] - 1
        const right = group[group.length - 1] + 1
        if (left >= 1) {
          edges.add(String(left).padStart(2, '0'))
        }
        if (right <= MAX_NUMBER) {
          edges.add(String(right).padStart(2, '0'))
        }
      }
      sorted.forEach(number => {
        if (!group.length || number === group[group.length - 1] + 1) {
          group.push(number)
          return
        }
        flush()
        group = [number]
      })
      flush()
      this.currentNumbers.forEach(number => edges.delete(number))
      return Array.from(edges)
    },
    hotTails() {
      const counts = this.currentNumbers.reduce((result, number) => {
        const tail = Number(number) % 10
        result[tail] = (result[tail] || 0) + 1
        return result
      }, {})
      return Object.keys(counts)
        .map(tail => ({ tail: Number(tail), count: counts[tail] }))
        .filter(item => item.count >= 2)
        .sort((left, right) => right.count - left.count || left.tail - right.tail)
        .slice(0, 2)
        .map(item => item.tail)
    },
    hotZones() {
      const counts = this.currentNumbers.reduce((result, number) => {
        const zone = this.zoneIndex(number)
        result[zone] = (result[zone] || 0) + 1
        return result
      }, {})
      const maxCount = Math.max(0, ...Object.keys(counts).map(zone => counts[zone]))
      if (maxCount < 3) {
        return []
      }
      return Object.keys(counts)
        .map(zone => ({ zone: Number(zone), count: counts[zone] }))
        .filter(item => item.count === maxCount)
        .sort((left, right) => left.zone - right.zone)
        .slice(0, 1)
        .map(item => item.zone)
    },
    tierPrediction() {
      const ranked = this.candidates.map(candidate => ({
        ...candidate,
        scoreText: candidate.score.toFixed(2)
      })).sort((left, right) => {
        if (right.score !== left.score) {
          return right.score - left.score
        }
        if (right.nextScore !== left.nextScore) {
          return right.nextScore - left.nextScore
        }
        return Number(left.number) - Number(right.number)
      }).map((candidate, index) => ({
        ...candidate,
        rank: index + 1
      }))
      const byNumber = items => items.slice().sort((left, right) => Number(left.number) - Number(right.number))
      const recommend = this.ensurePickSize(this.balanceRecommendation(ranked), ranked, 20)
      const recommendSet = new Set(recommend.map(item => item.number))
      const remaining = ranked.filter(item => !recommendSet.has(item.number))
      const avoid = this.balanceAvoid(remaining)
      const avoidSet = new Set(avoid.map(item => item.number))
      const middle = remaining.filter(item => !avoidSet.has(item.number))
      return {
        ranked,
        recommend: byNumber(recommend),
        middle: byNumber(this.ensurePickSize(middle, remaining, 50)),
        avoid: byNumber(this.ensurePickSize(avoid, remaining.slice().reverse(), 10))
      }
    },
    candidates() {
      if (!this.nextArea) {
        return []
      }
      const raw = this.allNumbers.map(number => this.buildCandidate(number))
      const maxValues = {
        nextScore: this.max(raw, 'nextScore'),
        nextRate: this.max(raw, 'nextRate'),
        coScore: this.max(raw, 'coScore'),
        coRate: this.max(raw, 'coRate'),
        omissionScore: this.max(raw, 'omissionScore'),
        omissionAverageBoost: this.max(raw, 'omissionAverageBoost'),
        omissionMaxBoost: this.max(raw, 'omissionMaxBoost'),
        runBelowMaxBoost: this.max(raw, 'runBelowMaxBoost'),
        maxConsecutiveScore: this.max(raw, 'maxConsecutiveScore'),
        heatScore: this.max(raw, 'heatScore'),
        recentRepeatScore: this.max(raw, 'recentRepeatScore'),
        repeatScore: this.max(raw, 'repeatScore'),
        neighborScore: this.max(raw, 'neighborScore'),
        zoneScore: this.max(raw, 'zoneScore'),
        tailScore: this.max(raw, 'tailScore'),
        offsetScore: this.max(raw, 'offsetScore'),
        multiWindowScore: this.max(raw, 'multiWindowScore')
      }
      return raw.map(candidate => {
        const score = this.normalize(candidate.nextScore, maxValues.nextScore) * this.featureWeight('nextCount', 0.16)
          + this.normalize(candidate.nextRate, maxValues.nextRate) * this.featureWeight('nextRate', 0.22)
          + this.normalize(candidate.coScore, maxValues.coScore) * this.featureWeight('coCount', 0.035)
          + this.normalize(candidate.coRate, maxValues.coRate) * this.featureWeight('coRate', 0.045)
          + this.normalize(candidate.omissionScore, maxValues.omissionScore) * 0.05
          + this.normalize(candidate.omissionAverageBoost, maxValues.omissionAverageBoost) * this.featureWeight('omissionAverage', 0.04)
          + this.normalize(candidate.omissionMaxBoost, maxValues.omissionMaxBoost) * this.featureWeight('omissionMax', 0.025)
          + this.normalize(candidate.runBelowMaxBoost, maxValues.runBelowMaxBoost) * this.featureWeight('runBelowMax', 0.035)
          + this.normalize(candidate.maxConsecutiveScore, maxValues.maxConsecutiveScore) * 0.015
          + this.normalize(candidate.heatScore, maxValues.heatScore) * this.featureWeight('heat', -0.035)
          + this.normalize(candidate.recentRepeatScore, maxValues.recentRepeatScore) * this.featureWeight('recentRepeat', -0.045)
          + this.normalize(candidate.repeatScore, maxValues.repeatScore) * this.featureWeight('repeat', -0.035)
          + (candidate.runLength === 2 ? 100 : 0) * this.featureWeight('runTwo', -0.12)
          + (candidate.runLength >= 3 ? 100 : 0) * this.featureWeight('runThree', -0.35)
          + (candidate.runOverMaxPenalty ? 100 : 0) * -0.22
          + this.normalize(candidate.neighborScore, maxValues.neighborScore) * this.featureWeight('neighbor', 0.055)
          + this.normalize(candidate.zoneScore, maxValues.zoneScore) * 0.025
          + this.normalize(candidate.tailScore, maxValues.tailScore) * 0.025
          + this.normalize(candidate.offsetScore, maxValues.offsetScore) * 0.09
          + this.normalize(candidate.multiWindowScore, maxValues.multiWindowScore) * 0.12
          + candidate.consecutiveEdgePenalty * this.featureWeight('consecutiveEdge', -0.12)
          + candidate.recentHotPenalty * this.featureWeight('recentHot', -0.07)
          + candidate.recentColdPenalty * this.featureWeight('recentCold', -0.05)
          + candidate.hotTailPenalty * this.featureWeight('hotTail', -0.055)
          + candidate.hotZonePenalty * this.featureWeight('hotZone', -0.06)
        return {
          ...candidate,
          score,
          tags: this.candidateTags(candidate)
        }
      })
    },
    offsetFeedback() {
      const records = this.nextArea ? this.nextArea.records || [] : []
      const fields = this.nextArea ? this.nextArea.fields || [] : []
      if (records.length < 3 || fields.length < 20) {
        return {}
      }
      const result = {}
      const feedbackWindow = Math.min(records.length, Math.max(3, this.recentCount))
      const start = Math.max(1, records.length - feedbackWindow)
      for (let index = start; index < records.length - 1; index += 1) {
        const history = records.slice(Math.max(0, index - feedbackWindow), index)
        const currentNumbers = this.balls(records[index], fields)
        const actualNumbers = this.balls(records[index + 1], fields)
        const predicted = this.baselinePrediction(history, currentNumbers, fields)
        actualNumbers.forEach(actual => {
          const distance = predicted.reduce((min, number) => {
            return Math.min(min, Math.abs(Number(actual) - Number(number)))
          }, Infinity)
          if (distance >= 0 && distance <= 3) {
            result[actual] = (result[actual] || 0) + (distance === 0 ? 1.2 : 1 / distance)
          }
        })
      }
      return result
    },
    profile() {
      const pairs = this.nextArea ? this.nextArea.pairs || [] : []
      const nextSets = pairs.map(pair => pair.nextBalls || []).filter(numbers => numbers.length)
      const average = (values, fallback) => values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : fallback
      const zoneAverages = Array.from({ length: 8 }, (_, zone) => {
        return average(nextSets.map(numbers => numbers.filter(number => this.zoneIndex(number) === zone).length), 2.5)
      })
      const tailAverages = Array.from({ length: 10 }, (_, tail) => {
        return average(nextSets.map(numbers => numbers.filter(number => Number(number) % 10 === tail).length), 2)
      })
      return {
        zoneTargets: this.distributeTargets(zoneAverages, 20),
        tailTargets: this.distributeTargets(tailAverages, 20)
      }
    }
  },
  created() {
    this.loadDynamicWeights()
  },
  watch: {
    weightVersion() {
      this.loadDynamicWeights()
    }
  },
  methods: {
    async loadDynamicWeights() {
      try {
        const { data } = await axios.get('/api/kl8/weight-optimization/latest')
        this.optimizationRuns = Array.isArray(data) ? data : []
      } catch (error) {
        this.optimizationRuns = []
      }
    },
    featureWeight(feature, fallback) {
      const value = this.dynamicWeights[feature]
      return typeof value === 'number' && !Number.isNaN(value) ? value : fallback
    },
    buildCandidate(number) {
      const nextRows = this.nextArea.rows || []
      const coRows = this.coArea ? this.coArea.allRows || this.coArea.rows || [] : []
      const nextCounts = nextRows.map(row => {
        const cell = row.cells.find(item => item.number === number)
        return cell ? cell.count : 0
      })
      const coCounts = coRows
        .filter(row => this.currentNumbers.includes(row.number))
        .map(row => {
          const cell = row.cells.find(item => item.number === number)
          return cell && !cell.same ? cell.count : 0
        })
      const nextBase = nextRows.reduce((sum, row) => sum + (row.baseCount || 0), 0)
      const coBase = coRows
        .filter(row => this.currentNumbers.includes(row.number))
        .reduce((sum, row) => sum + (row.appearCount || 0), 0)
      const records = this.nextArea.records || []
      const historyRecords = this.historyRecords.length ? this.historyRecords : records
      const fields = this.nextArea.fields || []
      const omission = this.omission(number, historyRecords, fields)
      const runLength = this.currentRun(number, historyRecords, fields)
      const recent10Count = this.appearCount(number, historyRecords.slice(Math.max(0, historyRecords.length - 10)), fields)
      const nextScore = nextCounts.reduce((sum, count) => sum + count, 0)
      const coScore = coCounts.reduce((sum, count) => sum + count, 0)
      const expectedRate = this.currentNumbers.length / MAX_NUMBER
      const nextRate = nextBase ? (nextScore + expectedRate * 8) / (nextBase + 8) : 0
      const coRate = coBase ? (coScore + expectedRate * 8) / (coBase + 8) : 0
      const currentTail = Number(number) % 10
      const currentZone = this.zoneIndex(number)
      return {
        number,
        nextScore,
        nextRate,
        coScore,
        coRate,
        omission,
        omissionScore: omission.max ? Math.min(omission.current, omission.max) / omission.max : 0,
        omissionAverageBoost: omission.average && omission.current > omission.average ? omission.current / omission.average : 0,
        omissionMaxBoost: omission.max && omission.current > omission.max ? omission.current / omission.max : 0,
        runLength,
        runBelowMaxBoost: omission.maxConsecutive && runLength > 0 && runLength < omission.maxConsecutive
          ? (omission.maxConsecutive - runLength) / omission.maxConsecutive
          : 0,
        runOverMaxPenalty: omission.maxConsecutive && runLength > omission.maxConsecutive ? 1 : 0,
        maxConsecutiveScore: omission.maxConsecutive,
        heatScore: this.appearCount(number, records, fields),
        recentRepeatScore: this.appearCount(number, records.slice(Math.max(0, records.length - 5)), fields),
        recent10Count,
        repeatScore: this.currentNumbers.includes(number) ? 1 : 0,
        neighborScore: this.neighborNumbers(this.currentNumbers).includes(number) ? 1 : 0,
        zoneScore: this.profile.zoneTargets[this.zoneIndex(number)] || 0,
        tailScore: this.profile.tailTargets[Number(number) % 10] || 0,
        offsetScore: this.offsetFeedback[number] || 0,
        multiWindowScore: this.multiWindowScore(number, fields),
        consecutiveEdgePenalty: this.consecutiveEdgeNumbers.includes(number) ? 100 : 0,
        recentHotPenalty: recent10Count >= 4 ? 100 : 0,
        recentColdPenalty: recent10Count < 2 && omission.average && omission.current <= omission.average ? 100 : 0,
        hotTailPenalty: this.hotTails.includes(currentTail) ? 100 : 0,
        hotZonePenalty: this.hotZones.includes(currentZone) ? 100 : 0
      }
    },
    candidateTags(candidate) {
      const tags = []
      if (candidate.repeatScore) {
        tags.push('重号')
      }
      if (candidate.runLength === 2) {
        tags.push('连2降权')
      }
      if (candidate.neighborScore) {
        tags.push('隔壁')
      }
      if (candidate.consecutiveEdgePenalty) {
        tags.push('连号边缘')
      }
      if (candidate.recentHotPenalty) {
        tags.push('近10热降')
      }
      if (candidate.recentColdPenalty) {
        tags.push('近10冷降')
      }
      if (candidate.hotTailPenalty) {
        tags.push('热尾降')
      }
      if (candidate.hotZonePenalty) {
        tags.push('热区降')
      }
      if (candidate.omission.average && candidate.omission.current > candidate.omission.average) {
        tags.push('超均遗漏')
      }
      if (candidate.omission.max && candidate.omission.current > candidate.omission.max) {
        tags.push('超最大遗漏')
      }
      if (candidate.runLength >= 3) {
        tags.push('连出降权')
      }
      if (candidate.offsetScore > 0) {
        tags.push('偏移反馈')
      }
      return tags
    },
    hitNumbers() {
      if (!this.nextRecord || !this.nextArea) {
        return []
      }
      return (this.nextArea.fields || []).map(field => String(this.nextRecord[field] || '').padStart(2, '0'))
    },
    isHit(number) {
      return this.hitNumbers().includes(number)
    },
    hitCount(items) {
      const numbers = items.map(item => item.number)
      return numbers.filter(number => this.isHit(number)).length
    },
    balanceRecommendation(ranked) {
      const selected = ranked.slice(0, 14)
      const pool = ranked.slice(14, 45)
      const selectedSet = new Set(selected.map(item => item.number))
      while (selected.length < 20 && pool.length) {
        const next = pool
          .filter(item => !selectedSet.has(item.number))
          .slice()
          .sort((left, right) => {
            const leftScore = left.score + this.fitBonus(left, selected) + left.multiWindowScore * 0.08
            const rightScore = right.score + this.fitBonus(right, selected) + right.multiWindowScore * 0.08
            if (rightScore !== leftScore) {
              return rightScore - leftScore
            }
            return left.rank - right.rank
          })[0]
        if (!next) {
          break
        }
        selected.push(next)
        selectedSet.add(next.number)
        pool.splice(pool.findIndex(item => item.number === next.number), 1)
      }
      const backupPool = ranked.filter(item => !selectedSet.has(item.number))
      for (let round = 0; round < 80; round += 1) {
        const issue = this.recommendationIssue(selected)
        if (!issue) {
          break
        }
        const removeIndex = this.removalIndex(selected, issue)
        const additionIndex = this.additionIndex(backupPool, issue, selectedSet)
        if (removeIndex < 0 || additionIndex < 0) {
          break
        }
        const removed = selected.splice(removeIndex, 1)[0]
        const added = backupPool.splice(additionIndex, 1)[0]
        selectedSet.delete(removed.number)
        selectedSet.add(added.number)
        backupPool.push(removed)
        backupPool.sort((left, right) => left.rank - right.rank)
      }
      return selected
    },
    balanceAvoid(remaining) {
      const risky = remaining.filter(item => this.positiveSignalScore(item) < 55)
      const fallback = remaining.slice().reverse()
      return this.ensurePickSize(
        risky.slice().sort((left, right) => {
          if (left.score !== right.score) {
            return left.score - right.score
          }
          return right.rank - left.rank
        }).slice(0, 10),
        fallback,
        10
      )
    },
    positiveSignalScore(item) {
      return item.nextRate * 100
        + item.multiWindowScore * 0.55
        + item.offsetScore * 8
        + item.neighborScore * 16
        + item.repeatScore * 8
        + item.omissionAverageBoost * 10
        + item.omissionMaxBoost * 8
    },
    fitBonus(candidate, selected) {
      const next = [...selected, candidate]
      const zone = this.zoneIndex(candidate.number)
      const tail = Number(candidate.number) % 10
      const zoneCount = next.filter(item => this.zoneIndex(item.number) === zone).length
      const tailCount = next.filter(item => Number(item.number) % 10 === tail).length
      const oddCount = next.filter(item => Number(item.number) % 2 === 1).length
      const bigCount = next.filter(item => Number(item.number) > 40).length
      const zoneBonus = zoneCount <= 3 ? 14 : -18 * (zoneCount - 3)
      const tailBonus = tailCount <= 3 ? 8 : -12 * (tailCount - 3)
      const oddBonus = Math.abs(oddCount - 10) <= 2 ? 7 : -6
      const bigBonus = Math.abs(bigCount - 10) <= 2 ? 7 : -6
      return zoneBonus + tailBonus + oddBonus + bigBonus
    },
    ensurePickSize(items, ranked, size) {
      const result = []
      const used = new Set()
      items.forEach(item => {
        if (!item || used.has(item.number) || result.length >= size) {
          return
        }
        result.push(item)
        used.add(item.number)
      })
      ranked.forEach(item => {
        if (!item || used.has(item.number) || result.length >= size) {
          return
        }
        result.push(item)
        used.add(item.number)
      })
      return result
    },
    recommendationIssue(selected) {
      const zoneCounts = this.countBy(selected, item => this.zoneIndex(item.number))
      const hotZone = Object.keys(zoneCounts).find(zone => {
        const limit = this.hotZones.includes(Number(zone)) ? 2 : 3
        return zoneCounts[zone] > limit
      })
      if (hotZone !== undefined) {
        return { type: 'zone', value: Number(hotZone) }
      }
      const tailCounts = this.countBy(selected, item => Number(item.number) % 10)
      const hotTail = Object.keys(tailCounts).find(tail => tailCounts[tail] > 3)
      if (hotTail !== undefined) {
        return { type: 'tail', value: Number(hotTail) }
      }
      const oddCount = selected.filter(item => Number(item.number) % 2 === 1).length
      if (oddCount > 12) {
        return { type: 'parity', value: 'odd' }
      }
      if (oddCount < 8) {
        return { type: 'parity', value: 'even' }
      }
      const bigCount = selected.filter(item => Number(item.number) > 40).length
      if (bigCount > 12) {
        return { type: 'size', value: 'big' }
      }
      if (bigCount < 8) {
        return { type: 'size', value: 'small' }
      }
      const averageSum = this.recentAverageSum()
      if (averageSum) {
        const sum = selected.reduce((total, item) => total + Number(item.number), 0)
        if (sum - averageSum > 30) {
          return { type: 'sum', value: 'high' }
        }
        if (averageSum - sum > 30) {
          return { type: 'sum', value: 'low' }
        }
      }
      return null
    },
    removalIndex(selected, issue) {
      let candidates = selected
      if (issue.type === 'zone') {
        candidates = selected.filter(item => this.zoneIndex(item.number) === issue.value)
      } else if (issue.type === 'tail') {
        candidates = selected.filter(item => Number(item.number) % 10 === issue.value)
      } else if (issue.type === 'parity') {
        candidates = selected.filter(item => (Number(item.number) % 2 === 1 ? 'odd' : 'even') === issue.value)
      } else if (issue.type === 'size') {
        candidates = selected.filter(item => (Number(item.number) > 40 ? 'big' : 'small') === issue.value)
      } else if (issue.type === 'sum') {
        candidates = selected.filter(item => issue.value === 'high' ? Number(item.number) > 40 : Number(item.number) <= 40)
      }
      const target = candidates
        .slice()
        .sort((left, right) => {
          if (left.score !== right.score) {
            return left.score - right.score
          }
          return right.rank - left.rank
        })[0]
      return target ? selected.findIndex(item => item.number === target.number) : -1
    },
    additionIndex(pool, issue, selectedSet) {
      const selected = pool.filter(item => !selectedSet.has(item.number))
        .filter(item => {
          if (issue.type === 'zone') {
            return this.zoneIndex(item.number) !== issue.value
          }
          if (issue.type === 'tail') {
            return Number(item.number) % 10 !== issue.value
          }
          if (issue.type === 'parity') {
            return (Number(item.number) % 2 === 1 ? 'odd' : 'even') !== issue.value
          }
          if (issue.type === 'size') {
            return (Number(item.number) > 40 ? 'big' : 'small') !== issue.value
          }
          if (issue.type === 'sum') {
            return issue.value === 'high' ? Number(item.number) <= 40 : Number(item.number) > 40
          }
          return true
        })
      const target = selected[0] || pool.find(item => !selectedSet.has(item.number))
      return target ? pool.findIndex(item => item.number === target.number) : -1
    },
    countBy(items, getter) {
      return items.reduce((result, item) => {
        const key = getter(item)
        result[key] = (result[key] || 0) + 1
        return result
      }, {})
    },
    recentAverageSum() {
      const records = this.historyRecords.slice(Math.max(0, this.historyRecords.length - 5))
      const fields = this.nextArea ? this.nextArea.fields || [] : []
      if (!records.length || !fields.length) {
        return 0
      }
      const total = records.reduce((sum, record) => {
        return sum + this.balls(record, fields).reduce((itemSum, number) => itemSum + Number(number), 0)
      }, 0)
      return total / records.length
    },
    multiWindowScore(number, fields) {
      const windows = [
        { size: 3, weight: 1.25 },
        { size: 5, weight: 1.15 },
        { size: 10, weight: 1.35 },
        { size: 20, weight: 1 },
        { size: 50, weight: 0.75 }
      ]
      let weightedScore = 0
      let totalWeight = 0
      windows.forEach(window => {
        const records = this.historyRecords.slice(Math.max(0, this.historyRecords.length - window.size))
        if (records.length < 2) {
          return
        }
        const score = this.windowSignalScore(number, records, fields)
        weightedScore += score * window.weight
        totalWeight += window.weight
      })
      return totalWeight ? weightedScore / totalWeight : 0
    },
    windowSignalScore(number, records, fields) {
      let nextHit = 0
      let nextBase = 0
      let coHit = 0
      let coBase = 0
      let heat = 0
      for (let index = 0; index < records.length; index += 1) {
        const balls = this.balls(records[index], fields)
        if (balls.includes(number)) {
          heat += 1
        }
        this.currentNumbers.forEach(current => {
          if (!balls.includes(current)) {
            return
          }
          coBase += 1
          if (balls.includes(number) && current !== number) {
            coHit += 1
          }
        })
        if (index < records.length - 1) {
          const nextBalls = this.balls(records[index + 1], fields)
          this.currentNumbers.forEach(current => {
            if (!balls.includes(current)) {
              return
            }
            nextBase += 1
            if (nextBalls.includes(number)) {
              nextHit += 1
            }
          })
        }
      }
      const expectedRate = this.currentNumbers.length / MAX_NUMBER
      const nextRate = nextBase ? (nextHit + expectedRate * 4) / (nextBase + 4) : 0
      const coRate = coBase ? (coHit + expectedRate * 4) / (coBase + 4) : 0
      const heatRate = heat / records.length
      return nextRate * 58 + coRate * 18 + heatRate * 10
    },
    normalize(value, max) {
      return max > 0 ? value / max * 100 : 0
    },
    max(items, key) {
      return Math.max(0, ...items.map(item => item[key] || 0))
    },
    balls(record, fields) {
      return fields.map(field => String(record[field] || '').padStart(2, '0'))
    },
    appearCount(number, records, fields) {
      return records.filter(record => this.balls(record, fields).includes(number)).length
    },
    currentRun(number, records, fields) {
      let run = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        if (!this.balls(records[index], fields).includes(number)) {
          break
        }
        run += 1
      }
      return run
    },
    omission(number, records, fields) {
      let current = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        if (this.balls(records[index], fields).includes(number)) {
          break
        }
        current += 1
      }
      let miss = 0
      let consecutive = 0
      let maxConsecutive = 0
      const misses = []
      records.forEach(record => {
        if (this.balls(record, fields).includes(number)) {
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
      const total = misses.reduce((sum, value) => sum + value, 0)
      return {
        current,
        max: misses.length ? Math.max(...misses) : 0,
        average: misses.length ? total / misses.length : 0,
        averageText: misses.length ? (total / misses.length).toFixed(1) : '0.0',
        maxConsecutive
      }
    },
    neighborNumbers(numbers) {
      const result = new Set()
      numbers.forEach(number => {
        const value = Number(number)
        if (value > 1) {
          result.add(String(value - 1).padStart(2, '0'))
        }
        if (value < MAX_NUMBER) {
          result.add(String(value + 1).padStart(2, '0'))
        }
      })
      numbers.forEach(number => result.delete(number))
      return Array.from(result)
    },
    baselinePrediction(records, currentNumbers, fields) {
      return this.allNumbers.map(number => {
        let nextScore = 0
        let coScore = 0
        let heatScore = 0
        records.forEach((record, index) => {
          const balls = this.balls(record, fields)
          if (balls.includes(number)) {
            heatScore += 1
          }
          currentNumbers.forEach(current => {
            if (balls.includes(current) && balls.includes(number) && current !== number) {
              coScore += 1
            }
          })
          if (index >= records.length - 1) {
            return
          }
          const nextBalls = this.balls(records[index + 1], fields)
          currentNumbers.forEach(current => {
            if (balls.includes(current) && nextBalls.includes(number)) {
              nextScore += 1
            }
          })
        })
        return {
          number,
          score: nextScore * 0.55 + coScore * 0.18 + heatScore * 0.12
        }
      }).sort((left, right) => right.score - left.score || Number(left.number) - Number(right.number))
        .slice(0, 20)
        .map(item => item.number)
    },
    zoneIndex(number) {
      return Math.floor((Number(number) - 1) / 10)
    },
    distributeTargets(averages, total) {
      const targets = averages.map(value => Math.max(0, Math.floor(value)))
      let assigned = targets.reduce((sum, value) => sum + value, 0)
      const order = averages
        .map((value, index) => ({ index, rest: value - Math.floor(value) }))
        .sort((left, right) => right.rest - left.rest || left.index - right.index)
      let pointer = 0
      while (assigned < total && order.length) {
        targets[order[pointer % order.length].index] += 1
        assigned += 1
        pointer += 1
      }
      while (assigned > total) {
        const index = targets
          .map((value, targetIndex) => ({ value, targetIndex }))
          .sort((left, right) => right.value - left.value || left.targetIndex - right.targetIndex)[0].targetIndex
        targets[index] -= 1
        assigned -= 1
      }
      return targets
    }
  }
}
</script>
