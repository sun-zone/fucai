<script>
import SingleIssueAnalysis from './SingleIssueAnalysis.vue'

const SSQ_WINDOWS = [
  { size: 10, weight: 0.2 },
  { size: 30, weight: 0.4 },
  { size: 60, weight: 0.25 },
  { size: 120, weight: 0.15 }
]

export default {
  name: 'SsqSingleIssueAnalysis',
  extends: SingleIssueAnalysis,
  props: {
    allHistoryRecords: { type: Array, default: () => [] }
  },
  computed: {
    recommendationTitle() {
      return '双色球新版五组推荐'
    },
    recommendationSubtitle() {
      return '红球、蓝球分开评分；融合10/30/60/120期走势，并进行组合结构和组间分散修正'
    }
  },
  methods: {
    recommendationCandidates(area, cooccurrenceArea) {
      if (!area) {
        return []
      }
      const selectedArea = this.selectedAreas.find(item => item.key === area.key)
      const currentNumbers = selectedArea ? selectedArea.numbers : []
      const allRecords = this.ssqAllRecords(area)
      const fields = area.fields || []
      const isRed = area.key === 'front'
      const profile = this.ssqProfile(area, isRed ? 6 : 1)
      const rawCandidates = area.numbers.map(number => {
        const signal = this.ssqAggregateSignal(
          number,
          area,
          currentNumbers,
          allRecords,
          fields,
          cooccurrenceArea
        )
        const omission = this.ssqOmission(number, allRecords, fields)
        const repeat = this.ssqRepeatSignal(number, currentNumbers, allRecords, fields, area.max)
        const neighbors = this.neighborNumbers(currentNumbers, area.max)
        return {
          number,
          nextScore: signal.nextCount,
          coScore: signal.coCount,
          nextRateScore: signal.nextRate,
          coRateScore: signal.coRate,
          heatScore: signal.heatRate,
          recentHeatScore: signal.recentHeatRate,
          heatTrendScore: signal.heatTrend,
          omissionScore: omission.score,
          omissionAverageRatio: omission.averageRatio,
          omissionMaxRatio: omission.maxRatio,
          repeatScore: currentNumbers.includes(number) ? 1 : 0,
          repeatRate: repeat.rate,
          neighborScore: neighbors.includes(number) ? 1 : 0,
          neighborRate: signal.neighborRate,
          blueRedRate: isRed ? 0 : this.ssqBlueRedRate(number, allRecords),
          tail: Number(number) % 10,
          zone: this.ssqZone(number),
          currentOmission: omission.current,
          averageOmission: omission.average,
          maxOmission: omission.max,
          currentRun: repeat.currentRun
        }
      })
      const maxValues = {}
      ;[
        'nextScore',
        'nextRateScore',
        'coScore',
        'coRateScore',
        'heatScore',
        'recentHeatScore',
        'heatTrendScore',
        'omissionScore',
        'omissionAverageRatio',
        'omissionMaxRatio',
        'repeatRate',
        'neighborRate',
        'blueRedRate'
      ].forEach(key => {
        maxValues[key] = rawCandidates.map(item => item[key])
      })
      const weights = isRed
        ? {
            nextRate: 0.26,
            next: 0.1,
            coRate: 0.12,
            co: 0.06,
            heat: 0.04,
            recentHeat: 0.1,
            trend: 0.06,
            omission: 0.1,
            omissionAverage: 0.06,
            repeat: 0.05,
            neighbor: 0.04
          }
        : {
            nextRate: 0.32,
            next: 0.1,
            heat: 0.08,
            recentHeat: 0.16,
            trend: 0.08,
            omission: 0.1,
            omissionAverage: 0.06,
            repeat: 0.08,
            neighbor: 0.04,
            blueRed: 0.16
          }
      return rawCandidates.map(candidate => {
        const detail = {}
        Object.keys(maxValues).forEach(key => {
          detail[key] = this.ssqRobustNormalize(candidate[key], maxValues[key])
        })
        const currentRunPenalty = candidate.currentRun >= 2 ? Math.min(12, candidate.currentRun * 3) : 0
        const score = detail.nextRateScore * weights.nextRate
          + detail.nextScore * weights.next
          + (isRed ? detail.coRateScore * weights.coRate + detail.coScore * weights.co : 0)
          + detail.heatScore * weights.heat
          + detail.recentHeatScore * weights.recentHeat
          + detail.heatTrendScore * weights.trend
          + detail.omissionScore * weights.omission
          + detail.omissionAverageRatio * weights.omissionAverage
          + detail.repeatRate * weights.repeat
          + detail.neighborRate * weights.neighbor
          + (!isRed ? detail.blueRedRate * weights.blueRed : 0)
          - currentRunPenalty
        return {
          ...candidate,
          score,
          detail,
          profile,
          currentRunPenalty
        }
      }).sort((left, right) => this.ssqSortCandidate(left, right))
    },

    recommendationSets(area, count) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const candidates = this.recommendationCandidates(area, cooccurrenceArea)
      if (!candidates.length) {
        return []
      }
      const size = Math.max(1, count)
      const isRed = area.key === 'front'
      const profile = this.ssqProfile(area, size)
      const pairRateContext = isRed
        ? {
            records: this.ssqAllRecords(area),
            fields: area.fields || [],
            rates: {}
          }
        : null
      const strategies = isRed
        ? [
            {
              source: '双色球综合评分',
              getter: candidate => candidate.score
            },
            {
              source: '双色球转移优先',
              getter: candidate => candidate.score
                + candidate.detail.nextRateScore * 0.16
                + candidate.detail.nextScore * 0.08
                + candidate.detail.heatTrendScore * 0.08
            },
            {
              source: '双色球遗漏平衡',
              getter: candidate => candidate.score
                + candidate.detail.omissionScore * 0.18
                + candidate.detail.omissionAverageRatio * 0.12
                - candidate.detail.heatScore * 0.04
            },
            {
              source: '双色球共现组合',
              getter: candidate => candidate.score
                + candidate.detail.coRateScore * 0.18
                + candidate.detail.coScore * 0.08
            },
            {
              source: '双色球结构分散',
              getter: candidate => candidate.score
                + candidate.detail.neighborRate * 0.05
                + candidate.detail.heatTrendScore * 0.06
            }
          ]
        : [
            {
              source: '蓝球综合评分',
              getter: candidate => candidate.score
            },
            {
              source: '蓝球转移优先',
              getter: candidate => candidate.score
                + candidate.detail.nextRateScore * 0.2
                + candidate.detail.nextScore * 0.1
            },
            {
              source: '蓝球遗漏平衡',
              getter: candidate => candidate.score
                + candidate.detail.omissionScore * 0.18
                + candidate.detail.omissionAverageRatio * 0.1
            },
            {
              source: '红蓝条件增强',
              getter: candidate => candidate.score
                + candidate.detail.blueRedRate * 0.24
                + candidate.detail.recentHeatScore * 0.08
            },
            {
              source: '蓝球分散探索',
              getter: candidate => candidate.score
                + candidate.detail.heatTrendScore * 0.1
                - candidate.detail.repeatRate * 0.04
            }
          ]
      const groups = []
      strategies.forEach(strategy => {
        const selected = this.ssqPickGroup(
          candidates,
          size,
          area,
          profile,
          strategy.getter,
          groups.map(group => group.numbers),
          pairRateContext
        )
        groups.push(this.ssqBuildGroup(selected, strategy.source))
      })
      return groups
    },

    avoidSets(area, count, recommendedNumbers = []) {
      const cooccurrenceArea = this.cooccurrenceAreas.find(item => item.key === (area ? area.key : ''))
      const candidates = this.recommendationCandidates(area, cooccurrenceArea)
      const size = Math.max(1, count)
      const recommendedSet = new Set(recommendedNumbers)
      const pool = candidates
        .filter(candidate => !recommendedSet.has(candidate.number))
        .sort((left, right) => this.ssqSortCandidate(right, left))
      const fallback = candidates.slice().sort((left, right) => this.ssqSortCandidate(right, left))
      const groups = []
      const usedCounts = {}
      for (let index = 0; index < 5; index += 1) {
        const source = pool.length >= size ? pool : fallback
        const selected = source
          .slice()
          .sort((left, right) => {
            const leftScore = left.score + (usedCounts[left.number] || 0) * 12
            const rightScore = right.score + (usedCounts[right.number] || 0) * 12
            if (leftScore !== rightScore) {
              return leftScore - rightScore
            }
            return Number(left.number) - Number(right.number)
          })
          .slice(0, size)
        selected.forEach(candidate => {
          usedCounts[candidate.number] = (usedCounts[candidate.number] || 0) + 1
        })
        groups.push(this.ssqBuildGroup(selected, '低分观察区'))
      }
      return groups
    },

    ssqAllRecords(area) {
      const source = this.allHistoryRecords.length
        ? this.allHistoryRecords
        : (area.records || [])
      return source.slice().sort((left, right) => {
        return String(left.expect).localeCompare(String(right.expect), undefined, { numeric: true })
      })
    },

    ssqAggregateSignal(number, area, currentNumbers, allRecords, fields, cooccurrenceArea) {
      let totalWeight = 0
      const result = {
        nextCount: 0,
        nextRate: 0,
        coCount: 0,
        coRate: 0,
        heatRate: 0,
        recentHeatRate: 0,
        heatTrend: 0,
        neighborRate: 0
      }
      SSQ_WINDOWS.forEach(window => {
        const records = allRecords.slice(Math.max(0, allRecords.length - window.size))
        if (records.length < 2) {
          return
        }
        const transition = this.ssqTransition(number, currentNumbers, records, fields, area.max)
        const cooccurrence = area.key === 'front'
          ? this.ssqCooccurrence(number, currentNumbers, records, fields, area.max)
          : { count: 0, rate: 0 }
        const heat = this.ssqAppearCount(number, records, fields) / records.length
        const recentRecords = allRecords.slice(Math.max(0, allRecords.length - Math.min(10, records.length)))
        const recentHeat = recentRecords.length
          ? this.ssqAppearCount(number, recentRecords, fields) / recentRecords.length
          : 0
        const neighborRate = this.ssqNeighborRate(number, currentNumbers, records, fields, area.max)
        result.nextCount += transition.count * window.weight
        result.nextRate += transition.rate * window.weight
        result.coCount += cooccurrence.count * window.weight
        result.coRate += cooccurrence.rate * window.weight
        result.heatRate += heat * window.weight
        result.recentHeatRate += recentHeat * window.weight
        result.heatTrend += (recentHeat - heat) * window.weight
        result.neighborRate += neighborRate * window.weight
        totalWeight += window.weight
      })
      if (!totalWeight) {
        return result
      }
      Object.keys(result).forEach(key => {
        result[key] /= totalWeight
      })
      return result
    },

    ssqTransition(number, currentNumbers, records, fields, max) {
      let count = 0
      let base = 0
      for (let index = 0; index < records.length - 1; index += 1) {
        const current = this.ballsFromRecord(records[index], fields)
        const next = this.ballsFromRecord(records[index + 1], fields)
        currentNumbers.forEach(currentNumber => {
          if (!current.includes(currentNumber)) {
            return
          }
          base += 1
          if (next.includes(number)) {
            count += 1
          }
        })
      }
      const expectedRate = currentNumbers.length / Math.max(1, max)
      return {
        count,
        rate: base ? (count + expectedRate * 4) / (base + 4) : expectedRate
      }
    },

    ssqCooccurrence(number, currentNumbers, records, fields, max) {
      let count = 0
      let base = 0
      for (let index = 0; index < records.length; index += 1) {
        const balls = this.ballsFromRecord(records[index], fields)
        currentNumbers.forEach(currentNumber => {
          if (!balls.includes(currentNumber)) {
            return
          }
          base += 1
          if (number !== currentNumber && balls.includes(number)) {
            count += 1
          }
        })
      }
      const expectedRate = currentNumbers.length / Math.max(1, max)
      return {
        count,
        rate: base ? (count + expectedRate * 4) / (base + 4) : expectedRate
      }
    },

    ssqNeighborRate(number, currentNumbers, records, fields, max) {
      const neighbors = new Set(this.neighborNumbers(currentNumbers, max))
      if (!neighbors.has(number)) {
        return 0
      }
      let count = 0
      let base = 0
      for (let index = 0; index < records.length - 1; index += 1) {
        const current = this.ballsFromRecord(records[index], fields)
        const next = this.ballsFromRecord(records[index + 1], fields)
        if (!current.some(value => neighbors.has(value))) {
          continue
        }
        base += 1
        if (next.includes(number)) {
          count += 1
        }
      }
      return base ? (count + 1) / (base + 4) : 0.25
    },

    ssqBlueRedRate(number, allRecords) {
      const redArea = this.nextOccurrenceAreas.find(area => area.key === 'front')
      const blueArea = this.nextOccurrenceAreas.find(area => area.key === 'back')
      const selectedRed = this.selectedAreas.find(area => area.key === 'front')
      if (!redArea || !blueArea || !selectedRed || !selectedRed.numbers.length) {
        return 0
      }
      const redFields = redArea.fields || []
      const blueFields = blueArea.fields || []
      let count = 0
      let base = 0
      SSQ_WINDOWS.forEach(window => {
        const records = allRecords.slice(Math.max(0, allRecords.length - window.size))
        for (let index = 0; index < records.length - 1; index += 1) {
          const currentRed = this.ballsFromRecord(records[index], redFields)
          if (!currentRed.some(value => selectedRed.numbers.includes(value))) {
            continue
          }
          base += 1
          const nextBlue = this.ballsFromRecord(records[index + 1], blueFields)
          if (nextBlue.includes(number)) {
            count += 1
          }
        }
      })
      return base ? (count + 1) / (base + 4) : 1 / 16
    },

    ssqRepeatSignal(number, currentNumbers, records, fields, max) {
      let currentCount = 0
      let repeatCount = 0
      let currentRun = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        if (!this.ballsFromRecord(records[index], fields).includes(number)) {
          break
        }
        currentRun += 1
      }
      for (let index = 0; index < records.length - 1; index += 1) {
        const current = this.ballsFromRecord(records[index], fields)
        if (!current.includes(number)) {
          continue
        }
        currentCount += 1
        if (this.ballsFromRecord(records[index + 1], fields).includes(number)) {
          repeatCount += 1
        }
      }
      const expectedRate = 1 / Math.max(1, max)
      return {
        currentRun,
        rate: currentNumbers.includes(number)
          ? (repeatCount + expectedRate * 4) / (currentCount + 4)
          : expectedRate
      }
    },

    ssqAppearCount(number, records, fields) {
      return records.filter(record => this.ballsFromRecord(record, fields).includes(number)).length
    },

    ssqOmission(number, records, fields) {
      let current = 0
      for (let index = records.length - 1; index >= 0; index -= 1) {
        if (this.ballsFromRecord(records[index], fields).includes(number)) {
          break
        }
        current += 1
      }
      let miss = 0
      const misses = []
      records.forEach(record => {
        if (this.ballsFromRecord(record, fields).includes(number)) {
          misses.push(miss)
          miss = 0
        } else {
          miss += 1
        }
      })
      misses.push(miss)
      const average = misses.length
        ? misses.reduce((sum, value) => sum + value, 0) / misses.length
        : 0
      const max = misses.length ? Math.max(...misses) : 0
      const maxRatio = max ? Math.min(1, current / max) : 0
      const averageRatio = average
        ? Math.max(0, Math.min(1.5, (current / average - 0.75) / 1.25))
        : 0
      return {
        current,
        average,
        max,
        maxRatio,
        averageRatio,
        score: maxRatio * 0.55 + averageRatio * 0.45
      }
    },

    ssqProfile(area, size) {
      const records = this.ssqAllRecords(area)
      const fields = area.fields || []
      const isRed = area.key === 'front'
      if (!records.length || !isRed) {
        return {
          zoneTargets: [],
          oddTarget: size / 2,
          sumMean: 0,
          sumStd: 1,
          spanMean: 0
        }
      }
      const zoneAverages = [0, 0, 0]
      const oddValues = []
      const sums = []
      const spans = []
      records.forEach(record => {
        const numbers = this.ballsFromRecord(record, fields).map(Number)
        numbers.forEach(number => {
          zoneAverages[this.ssqZone(String(number))] += 1
        })
        oddValues.push(numbers.filter(number => number % 2 === 1).length)
        sums.push(numbers.reduce((sum, number) => sum + number, 0))
        spans.push(Math.max(...numbers) - Math.min(...numbers))
      })
      return {
        zoneTargets: this.ssqDistributeTargets(
          zoneAverages.map(value => value / records.length),
          size
        ),
        oddTarget: oddValues.reduce((sum, value) => sum + value, 0) / oddValues.length,
        sumMean: sums.reduce((sum, value) => sum + value, 0) / sums.length,
        sumStd: this.ssqStandardDeviation(sums) || 1,
        spanMean: spans.reduce((sum, value) => sum + value, 0) / spans.length
      }
    },

    ssqPickGroup(candidates, size, area, profile, getter, previousGroups, pairRateContext) {
      const selected = []
      const pool = candidates.slice()
      const isRed = area.key === 'front'
      while (selected.length < size && pool.length) {
        const next = pool
          .map(candidate => ({
            candidate,
            adjustedScore: getter(candidate)
              + (isRed ? this.ssqPairSynergy(candidate, selected, area, pairRateContext) : 0)
              + (isRed ? this.ssqStructureIncrement(candidate, selected, profile) : 0)
              + this.ssqDiversityAdjustment(candidate.number, previousGroups)
          }))
          .sort((left, right) => {
            if (right.adjustedScore !== left.adjustedScore) {
              return right.adjustedScore - left.adjustedScore
            }
            return this.ssqSortCandidate(left.candidate, right.candidate)
          })[0].candidate
        selected.push(next)
        pool.splice(pool.findIndex(item => item.number === next.number), 1)
      }
      return selected
    },

    ssqPairSynergy(candidate, selected, area, pairRateContext) {
      if (!selected.length || area.key !== 'front') {
        return 0
      }
      const context = pairRateContext || {
        records: this.ssqAllRecords(area),
        fields: area.fields || [],
        rates: {}
      }
      return selected.reduce((sum, item) => {
        const key = [candidate.number, item.number]
          .map(String)
          .sort((left, right) => Number(left) - Number(right))
          .join(':')
        if (!Object.prototype.hasOwnProperty.call(context.rates, key)) {
          context.rates[key] = this.ssqPairRate(
            candidate.number,
            item.number,
            context.records,
            context.fields
          )
        }
        const pairRate = context.rates[key]
        return sum + pairRate * 8
      }, 0)
    },

    ssqPairRate(left, right, records, fields) {
      let both = 0
      let leftCount = 0
      let rightCount = 0
      records.forEach(record => {
        const numbers = this.ballsFromRecord(record, fields)
        if (numbers.includes(left)) {
          leftCount += 1
        }
        if (numbers.includes(right)) {
          rightCount += 1
        }
        if (numbers.includes(left) && numbers.includes(right)) {
          both += 1
        }
      })
      if (!leftCount || !rightCount) {
        return 0
      }
      return both / Math.sqrt(leftCount * rightCount)
    },

    ssqStructureIncrement(candidate, selected, profile) {
      const next = selected.concat(candidate)
      const zone = this.ssqZone(candidate.number)
      const zoneCount = next.filter(item => this.ssqZone(item.number) === zone).length
      const zoneTarget = profile.zoneTargets[zone] || 0
      let score = zoneCount <= zoneTarget ? 3 : -5 * (zoneCount - zoneTarget)
      const oddCount = next.filter(item => Number(item.number) % 2 === 1).length
      const expectedOdd = profile.oddTarget * next.length / 6
      score += Math.abs(oddCount - expectedOdd) <= 1 ? 2 : -2
      if (next.length === 6) {
        score += this.ssqFinalStructureScore(next, profile)
      }
      return score
    },

    ssqFinalStructureScore(selected, profile) {
      const numbers = selected.map(item => Number(item.number))
      const zoneCounts = [0, 0, 0]
      numbers.forEach(number => {
        zoneCounts[this.ssqZone(String(number))] += 1
      })
      const zonePenalty = zoneCounts.reduce((sum, value, index) => {
        return sum + Math.abs(value - (profile.zoneTargets[index] || 0)) * 1.5
      }, 0)
      const oddCount = numbers.filter(number => number % 2 === 1).length
      const oddPenalty = Math.abs(oddCount - profile.oddTarget) * 1.5
      const sum = numbers.reduce((total, number) => total + number, 0)
      const sumPenalty = Math.min(8, Math.abs(sum - profile.sumMean) / profile.sumStd)
      const span = Math.max(...numbers) - Math.min(...numbers)
      const spanPenalty = Math.min(4, Math.abs(span - profile.spanMean) / 5)
      const consecutiveCount = numbers.filter((number, index) => index > 0 && number === numbers[index - 1] + 1).length
      const consecutivePenalty = consecutiveCount > 2 ? (consecutiveCount - 2) * 1.5 : 0
      return -(zonePenalty + oddPenalty + sumPenalty + spanPenalty + consecutivePenalty)
    },

    ssqDiversityAdjustment(number, previousGroups) {
      const count = previousGroups.reduce((total, group) => {
        return total + (group.includes(number) ? 1 : 0)
      }, 0)
      if (count >= 4) {
        return -12
      }
      if (count === 3) {
        return -5
      }
      if (count === 2) {
        return -1
      }
      return 0
    },

    ssqBuildGroup(selected, source) {
      return {
        numbers: selected
          .map(item => item.number)
          .sort((left, right) => Number(left) - Number(right)),
        source,
        totalScore: selected.reduce((sum, item) => sum + item.score, 0),
        hitCount: selected.filter(item => item.nextScore > 0).length,
        maxCount: selected.length ? Math.max(...selected.map(item => item.nextScore)) : 0
      }
    },

    ssqSortCandidate(left, right) {
      if (right.score !== left.score) {
        return right.score - left.score
      }
      if (right.nextRateScore !== left.nextRateScore) {
        return right.nextRateScore - left.nextRateScore
      }
      if (right.coRateScore !== left.coRateScore) {
        return right.coRateScore - left.coRateScore
      }
      return Number(left.number) - Number(right.number)
    },

    ssqRobustNormalize(value, values) {
      if (!values.length) {
        return 0
      }
      const sorted = values.slice().sort((left, right) => left - right)
      const low = sorted[Math.floor((sorted.length - 1) * 0.1)]
      const high = sorted[Math.floor((sorted.length - 1) * 0.9)]
      if (high === low) {
        return 50
      }
      return Math.max(0, Math.min(100, (value - low) / (high - low) * 100))
    },

    ssqZone(number) {
      return Math.min(2, Math.floor((Number(number) - 1) / 11))
    },

    ssqDistributeTargets(averages, total) {
      const targets = averages.map(value => Math.max(0, Math.floor(value / averages.reduce((sum, item) => sum + item, 0) * total)))
      let assigned = targets.reduce((sum, value) => sum + value, 0)
      const order = averages
        .map((value, index) => ({ index, value }))
        .sort((left, right) => right.value - left.value || left.index - right.index)
      let pointer = 0
      while (assigned < total && order.length) {
        targets[order[pointer % order.length].index] += 1
        assigned += 1
        pointer += 1
      }
      while (assigned > total) {
        const index = targets.indexOf(Math.max(...targets))
        targets[index] -= 1
        assigned -= 1
      }
      return targets
    },

    ssqStandardDeviation(values) {
      if (!values.length) {
        return 0
      }
      const average = values.reduce((sum, value) => sum + value, 0) / values.length
      return Math.sqrt(values.reduce((sum, value) => sum + Math.pow(value - average, 2), 0) / values.length)
    }
  }
}
</script>
