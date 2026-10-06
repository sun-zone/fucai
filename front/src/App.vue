<template>
  <div id="app">
    <main class="page">
      <header class="toolbar">
        <div>
          <h1>{{ lotteryTitle }}数据</h1>
          <p>{{ lotteryDescription }}</p>
        </div>
        <nav class="tabs" aria-label="彩种切换">
          <button
            v-for="(config, key) in lotteryConfigs"
            :key="key"
            :class="{ active: lottery === key }"
            :disabled="loading"
            @click="switchLottery(key)"
          >
            {{ config.title }}
          </button>
        </nav>
      </header>

      <section class="panel">
        <div class="panel-top">
          <div class="view-section">
            <div class="section-title">分析视图</div>
            <div class="mode-tabs" aria-label="视图切换">
              <button :class="{ active: viewMode === 'records' }" class="secondary" @click="showRecords">历史列表</button>
              <button :class="{ active: viewMode === 'grid' }" class="secondary" @click="showGridAnalysis">方格分析</button>
              <button :class="{ active: viewMode === 'trend' }" class="secondary" @click="showTrendAnalysis">走势连线</button>
              <button :class="{ active: viewMode === 'omission' }" class="secondary" @click="showOmissionAnalysis">遗漏统计</button>
              <button :class="{ active: viewMode === 'cooccurrence' }" class="secondary" @click="showCooccurrenceAnalysis">共现统计</button>
              <button :class="{ active: viewMode === 'nextOccurrence' }" class="secondary" @click="showNextOccurrenceAnalysis">下期统计</button>
              <button :class="{ active: viewMode === 'singleIssue' }" class="secondary" @click="showSingleIssueAnalysis">单期分析</button>
              <button
                v-if="lottery === 'kl8'"
                :class="{ active: viewMode === 'singleIssueLegacy' }"
                class="secondary"
                @click="showLegacySingleIssueAnalysis"
              >
                单期分析（旧）
              </button>
              <button :class="{ active: viewMode === 'repeats' }" class="secondary" @click="showRepeatAnalysis">重复分析</button>
            </div>
          </div>
          <div class="data-actions">
            <button :disabled="loading" @click="syncHistory">
              {{ loadingAction === 'history' ? '同步中...' : '同步历史数据' }}
            </button>
            <button
              v-if="lottery === 'kl8'"
              :disabled="loading || !records.length"
              class="secondary"
              @click="runKl8WeightOptimization"
            >
              {{ loadingAction === 'kl8Weight' ? '调权中...' : '快乐8自动调权' }}
            </button>
            <button :disabled="loading" class="secondary" @click="loadRecords">刷新</button>
          </div>
        </div>

        <div v-if="viewMode === 'records'" class="analysis-controls">
          <label class="field">
            <span class="group-label">最近期数</span>
            <input v-model.number="recentExpect" min="1" type="number" aria-label="最近期数">
          </label>
          <button :disabled="loading" @click="fetchRecent">
            {{ loadingAction === 'recent' ? '获取中...' : '获取最近几期' }}
          </button>
        </div>

        <div v-if="viewMode === 'repeats'" class="analysis-controls">
          <label class="field">
            <span class="group-label">重复号码数</span>
            <input v-model.number="matchCount" :max="maxMatchCount" min="2" type="number" aria-label="重复号码数">
          </label>
          <label class="field">
            <span class="group-label">重复期数筛选</span>
            <input
              v-model.number="repeatCountFilter"
              min="0"
              type="number"
              aria-label="重复期数筛选"
              placeholder="不筛选"
            >
          </label>
          <button :disabled="loading" @click="analyzeRepeats">
            {{ loadingAction === 'repeats' ? '分析中...' : '分析重复' }}
          </button>
        </div>

        <div v-if="viewMode === 'grid' || viewMode === 'trend'" class="analysis-controls">
          <label class="field">
            <span class="group-label">分析方式</span>
            <select v-model="gridFilterMode">
              <option value="recent">最近几期</option>
              <option value="range">期号范围</option>
            </select>
          </label>
          <label v-if="gridFilterMode === 'recent'" class="field">
            <span class="group-label">最近期数</span>
            <input v-model.number="gridRecentCount" min="1" type="number" aria-label="方格分析最近期数">
          </label>
          <label v-if="gridFilterMode === 'range'" class="field">
            <span class="group-label">开始期号</span>
            <input v-model.trim="gridStartExpect" type="text" aria-label="开始期号">
          </label>
          <label v-if="gridFilterMode === 'range'" class="field">
            <span class="group-label">结束期号</span>
            <input v-model.trim="gridEndExpect" type="text" aria-label="结束期号">
          </label>
          <label class="field">
            <span class="group-label">{{ frontLabel }}方格</span>
            <input v-model.number="frontGridSize" min="1" type="number" aria-label="主号码方格尺寸">
          </label>
          <label v-if="viewMode === 'grid'" class="field">
            <span class="group-label">显示区域</span>
            <select v-model="gridArea">
              <option value="all">全部</option>
              <option value="front">{{ frontLabel }}</option>
              <option v-if="currentLotteryConfig.backMax" value="back">{{ backLabel }}</option>
            </select>
          </label>
          <label v-if="viewMode === 'grid' && currentLotteryConfig.backMax" class="field">
            <span class="group-label">{{ backLabel }}方格</span>
            <input v-model.number="backGridSize" min="1" type="number" aria-label="后区方格尺寸">
          </label>
          <label v-if="viewMode === 'trend'" class="field">
            <span class="group-label">号码区域</span>
            <select v-model="trendArea">
              <option value="front">{{ frontLabel }}</option>
              <option v-if="currentLotteryConfig.backMax" value="back">{{ backLabel }}</option>
            </select>
          </label>
          <label v-if="viewMode === 'trend'" class="field">
            <span class="group-label">连线序号</span>
            <select v-model.number="trendLineIndex">
              <option :value="0">全部号码</option>
              <option v-for="index in trendBallCount" :key="'line-' + index" :value="index">
                第 {{ index }} 个号
              </option>
            </select>
          </label>
        </div>
        <div v-if="viewMode === 'omission'" class="analysis-controls">
          <label class="field">
            <span class="group-label">展示期数</span>
            <input v-model.number="omissionRecentCount" min="1" type="number" aria-label="遗漏统计最近期数">
          </label>
          <label class="field">
            <span class="group-label">汇总期数</span>
            <input v-model.number="omissionSummaryRecentCount" min="1" type="number" aria-label="遗漏汇总最近期数">
          </label>
          <div class="field summary-options">
            <span class="group-label">汇总显示</span>
            <div class="checkbox-group">
              <label v-for="option in omissionSummaryOptions" :key="option.key" class="check-field">
                <input v-model="visibleOmissionSummaryRows" type="checkbox" :value="option.key">
                <span>{{ option.label }}</span>
              </label>
            </div>
          </div>
        </div>
        <div v-if="viewMode === 'cooccurrence'" class="analysis-controls">
          <label class="field">
            <span class="group-label">统计期数</span>
            <input v-model.number="cooccurrenceRecentCount" min="1" type="number" aria-label="共现统计最近期数">
          </label>
        </div>
        <div v-if="viewMode === 'nextOccurrence'" class="analysis-controls">
          <label class="field">
            <span class="group-label">统计期数</span>
            <input v-model.number="nextOccurrenceRecentCount" min="1" type="number" aria-label="下期统计最近期数">
          </label>
        </div>
        <div v-if="viewMode === 'singleIssue' || viewMode === 'singleIssueLegacy'" class="analysis-controls">
          <label class="field single-issue-select">
            <span class="group-label">选择期号</span>
            <select v-model="selectedExpect" aria-label="选择分析期号">
              <option v-for="record in records" :key="'single-issue-option-' + record.expect" :value="record.expect">
                {{ record.expect }}{{ record.drawDate ? `（${record.drawDate}）` : '' }}
              </option>
            </select>
          </label>
          <label class="field">
            <span class="group-label">统计期数</span>
            <input v-model.number="singleIssueRecentCount" min="1" type="number" aria-label="单期分析统计期数">
          </label>
        </div>
      </section>

      <section v-if="message" class="message">{{ message }}</section>

      <OmissionAnalysis
        v-if="viewMode === 'omission'"
        :areas="omissionAreas"
        :records="records"
        :recent-count="normalizedOmissionRecentCount"
        :summary-recent-count="normalizedOmissionSummaryRecentCount"
        :loading="loading"
        :format-date="formatDate"
        :omission-scroll-style="omissionScrollStyle"
        :omission-table-style="omissionTableStyle"
        :omission-cell-class="omissionCellClass"
        :omission-row-class="omissionRowClass"
      />

      <MatrixAnalysis
        v-if="viewMode === 'cooccurrence'"
        :areas="cooccurrenceAreas"
        :records="records"
        :note="`当前按最近 ${normalizedCooccurrenceRecentCount} 期统计，每个单元格表示行号码与列号码同期开出的次数。`"
        title="共现统计"
        second-column-label="出现次数"
        second-column-field="appearCount"
        count-field="records"
        show-diagonal
        :loading="loading"
      />

      <MatrixAnalysis
        v-if="viewMode === 'nextOccurrence'"
        :areas="nextOccurrenceAreas"
        :records="records"
        :note="`当前按最近 ${normalizedNextOccurrenceRecentCount} 组相邻期次统计，每个单元格表示行号码出现后，列号码在下一期出现的次数。`"
        title="下期出现统计"
        second-column-label="作为前期次数"
        second-column-field="baseCount"
        count-field="pairs"
        :loading="loading"
      />

      <SsqSingleIssueAnalysis
        v-if="viewMode === 'singleIssue' && lottery === 'ssq'"
        :record="selectedIssueRecord"
        :next-record="selectedIssueNextRecord"
        :selected-areas="selectedIssueAreas"
        :cooccurrence-areas="selectedIssueCooccurrenceAreas"
        :next-occurrence-areas="selectedIssueNextOccurrenceAreas"
        :recent-count="normalizedSingleIssueRecentCount"
        :all-history-records="selectedIssueAllHistoryRecords"
        :format-date="formatDate"
      />

      <SingleIssueAnalysis
        v-if="viewMode === 'singleIssue' && lottery === 'dlt'"
        lottery="dlt"
        :record="selectedIssueRecord"
        :next-record="selectedIssueNextRecord"
        :selected-areas="selectedIssueAreas"
        :cooccurrence-areas="selectedIssueCooccurrenceAreas"
        :next-occurrence-areas="selectedIssueNextOccurrenceAreas"
        :recent-count="normalizedSingleIssueRecentCount"
        :format-date="formatDate"
      />

      <Kl8SingleIssueAnalysis
        v-if="viewMode === 'singleIssue' && lottery === 'kl8'"
        :record="selectedIssueRecord"
        :next-record="selectedIssueNextRecord"
        :selected-areas="selectedIssueAreas"
        :cooccurrence-areas="selectedIssueCooccurrenceAreas"
        :next-occurrence-areas="selectedIssueNextOccurrenceAreas"
        :history-records="selectedIssueAllHistoryRecords"
        :recent-count="normalizedSingleIssueRecentCount"
        :weight-version="kl8WeightVersion"
        :format-date="formatDate"
      />

      <Kl8LegacySingleIssueAnalysis
        v-if="viewMode === 'singleIssueLegacy' && lottery === 'kl8'"
        :record="selectedIssueRecord"
        :next-record="selectedIssueNextRecord"
        :selected-areas="selectedIssueAreas"
        :cooccurrence-areas="selectedIssueCooccurrenceAreas"
        :next-occurrence-areas="selectedIssueNextOccurrenceAreas"
        :recent-count="normalizedSingleIssueRecentCount"
        :format-date="formatDate"
      />

      <RecordsTable
        v-if="viewMode === 'records' || viewMode === 'repeats'"
        :rows="pagedRows"
        :view-mode="viewMode"
        :front-label="frontLabel"
        :back-label="backLabel"
        :expanded-expect="expandedExpect"
        :loading="loading"
        :front-balls="frontBalls"
        :back-balls="backBalls"
        :format-date="formatDate"
        @toggle-matches="toggleMatches"
      />

      <GridAnalysis
        v-if="viewMode === 'grid'"
        :rows="pagedRows"
        :config="currentLotteryConfig"
        :front-label="frontLabel"
        :back-label="backLabel"
        :grid-area="gridArea"
        :front-grid-size="frontGridSize"
        :back-grid-size="backGridSize"
        :draw-card-style="drawCardStyle"
        :loading="loading"
        :front-balls="frontBalls"
        :back-balls="backBalls"
        :format-date="formatDate"
        :grid-style="gridStyle"
        :grid-numbers="gridNumbers"
      />

      <TrendAnalysis
        v-if="viewMode === 'trend'"
        :trend-rows="trendRows"
        :trend-numbers="trendNumbers"
        :trend-lines="trendLines"
        :trend-svg-width="trendSvgWidth"
        :trend-svg-height="trendSvgHeight"
        :trend-table-style="trendTableStyle"
        :trend-scroll-style="trendScrollStyle"
        :loading="loading"
        :format-date="formatDate"
        :trend-cell-class="trendCellClass"
        :trend-cell-text="trendCellText"
      />

      <PaginationBar
        v-if="displayRows.length"
        :total="displayRows.length"
        :page-size.sync="pageSize"
        :current-page.sync="currentPage"
        :total-pages="totalPages"
      />
    </main>
  </div>
</template>

<script>
import axios from 'axios'
import GridAnalysis from './components/GridAnalysis.vue'
import Kl8LegacySingleIssueAnalysis from './components/Kl8LegacySingleIssueAnalysis.vue'
import Kl8SingleIssueAnalysis from './components/Kl8SingleIssueAnalysis.vue'
import MatrixAnalysis from './components/MatrixAnalysis.vue'
import OmissionAnalysis from './components/OmissionAnalysis.vue'
import PaginationBar from './components/PaginationBar.vue'
import RecordsTable from './components/RecordsTable.vue'
import SingleIssueAnalysis from './components/SingleIssueAnalysis.vue'
import SsqSingleIssueAnalysis from './components/SsqSingleIssueAnalysis.vue'
import TrendAnalysis from './components/TrendAnalysis.vue'
import {
  buildNextOccurrencePairs,
  calculateCooccurrenceRows,
  calculateNextOccurrenceRows,
  calculateOmissionRows,
  calculateOmissionSummaryRows,
  sequenceNumbers,
  sortByExpect
} from './utils/lotteryAnalysis'

export default {
  name: 'App',
  components: {
    GridAnalysis,
    Kl8LegacySingleIssueAnalysis,
    Kl8SingleIssueAnalysis,
    MatrixAnalysis,
    OmissionAnalysis,
    PaginationBar,
    RecordsTable,
    SingleIssueAnalysis,
    SsqSingleIssueAnalysis,
    TrendAnalysis
  },
  data() {
    return {
      lottery: 'ssq',
      recentExpect: 10,
      matchCount: 3,
      lotteryConfigs: {
        ssq: {
          title: '双色球',
          description: '先同步历史数据，再按需获取最近几期',
          maxMatchCount: 6,
          frontLabel: '红球',
          backLabel: '蓝球',
          matchLabel: '红球',
          frontMax: 33,
          backMax: 16,
          defaultFrontGridSize: 6,
          defaultBackGridSize: 4,
          frontFields: ['red1', 'red2', 'red3', 'red4', 'red5', 'red6'],
          backFields: ['blue']
        },
        dlt: {
          title: '大乐透',
          description: '同步并查看大乐透历史数据',
          maxMatchCount: 5,
          frontLabel: '前区',
          backLabel: '后区',
          matchLabel: '前区号码',
          frontMax: 35,
          backMax: 12,
          defaultFrontGridSize: 6,
          defaultBackGridSize: 4,
          frontFields: ['front1', 'front2', 'front3', 'front4', 'front5'],
          backFields: ['back1', 'back2']
        },
        kl8: {
          title: '快乐8',
          description: '同步并查看快乐8历史数据',
          maxMatchCount: 20,
          frontLabel: '号码',
          backLabel: '',
          matchLabel: '号码',
          frontMax: 80,
          backMax: 0,
          defaultFrontGridSize: 9,
          defaultBackGridSize: 0,
          frontFields: [
            'num1', 'num2', 'num3', 'num4', 'num5',
            'num6', 'num7', 'num8', 'num9', 'num10',
            'num11', 'num12', 'num13', 'num14', 'num15',
            'num16', 'num17', 'num18', 'num19', 'num20'
          ],
          backFields: []
        }
      },
      viewMode: 'records',
      loading: false,
      loadingAction: '',
      message: '',
      records: [],
      selectedExpect: '',
      kl8WeightVersion: 0,
      kl8WeightPollTimer: null,
      repeatStats: [],
      repeatCountFilter: null,
      expandedExpect: '',
      currentPage: 1,
      pageSize: 20,
      gridFilterMode: 'recent',
      gridRecentCount: 10,
      gridStartExpect: '',
      gridEndExpect: '',
      gridArea: 'all',
      omissionRecentCount: 10,
      omissionSummaryRecentCount: 10,
      visibleOmissionSummaryRows: ['current', 'max', 'min', 'average'],
      omissionSummaryOptions: [
        { key: 'current', label: '当前遗漏' },
        { key: 'max', label: '最大遗漏' },
        { key: 'min', label: '最小遗漏' },
        { key: 'average', label: '平均遗漏' }
      ],
      cooccurrenceRecentCount: 10,
      nextOccurrenceRecentCount: 10,
      singleIssueRecentCount: 10,
      frontGridSize: 6,
      backGridSize: 4,
      trendArea: 'front',
      trendLineIndex: 0,
      trendCellSize: 32,
      trendExpectWidth: 120,
      trendRowHeight: 38,
      trendHeaderHeight: 38,
      trendColors: ['#2563eb', '#dc2626', '#16a34a', '#9333ea', '#ea580c', '#0891b2', '#be123c', '#4f46e5', '#65a30d', '#b45309', '#0f766e', '#7c3aed', '#e11d48', '#0284c7', '#ca8a04', '#059669', '#db2777', '#475569', '#c2410c', '#1d4ed8']
    }
  },
  created() {
    this.loadRecords()
  },
  beforeDestroy() {
    this.stopKl8WeightPolling()
  },
  computed: {
    currentLotteryConfig() {
      return this.lotteryConfigs[this.lottery]
    },
    lotteryTitle() {
      return this.currentLotteryConfig.title
    },
    lotteryDescription() {
      return this.currentLotteryConfig.description
    },
    maxMatchCount() {
      return this.currentLotteryConfig.maxMatchCount
    },
    frontLabel() {
      return this.currentLotteryConfig.frontLabel
    },
    backLabel() {
      return this.currentLotteryConfig.backLabel
    },
    displayRows() {
      if (this.viewMode === 'grid' || this.viewMode === 'trend') {
        return this.gridRows
      }
      if (this.viewMode === 'records') {
        return this.records
      }
      if (this.viewMode !== 'repeats') {
        return []
      }
      if (this.repeatCountFilter === null || this.repeatCountFilter === '') {
        return this.repeatStats
      }
      const count = Number(this.repeatCountFilter)
      return Number.isNaN(count)
        ? this.repeatStats
        : this.repeatStats.filter(record => record.repeatCount === count)
    },
    gridRows() {
      if (this.gridFilterMode === 'range') {
        const start = this.gridStartExpect
        const end = this.gridEndExpect
        return this.records.filter(record => {
          if (start && record.expect < start) {
            return false
          }
          return !(end && record.expect > end)
        })
      }
      const count = Math.max(1, Number(this.gridRecentCount) || 10)
      return this.records.slice(0, count)
    },
    normalizedOmissionRecentCount() {
      return Math.max(1, Number(this.omissionRecentCount) || 10)
    },
    normalizedOmissionSummaryRecentCount() {
      return Math.max(1, Number(this.omissionSummaryRecentCount) || 10)
    },
    normalizedCooccurrenceRecentCount() {
      return Math.max(1, Number(this.cooccurrenceRecentCount) || 10)
    },
    normalizedNextOccurrenceRecentCount() {
      return Math.max(1, Number(this.nextOccurrenceRecentCount) || 10)
    },
    normalizedSingleIssueRecentCount() {
      return Math.max(1, Number(this.singleIssueRecentCount) || 10)
    },
    selectedIssueRecord() {
      return this.records.find(record => record.expect === this.selectedExpect) || this.records[0] || null
    },
    selectedIssueNextRecord() {
      const record = this.selectedIssueRecord
      if (!record) {
        return null
      }
      const chronologicalRecords = sortByExpect(this.records)
      const selectedIndex = chronologicalRecords.findIndex(item => item.expect === record.expect)
      return selectedIndex >= 0 ? chronologicalRecords[selectedIndex + 1] || null : null
    },
    selectedIssueHistoryRecords() {
      const record = this.selectedIssueRecord
      if (!record) {
        return []
      }
      const chronologicalRecords = sortByExpect(this.records)
      const selectedIndex = chronologicalRecords.findIndex(item => item.expect === record.expect)
      if (selectedIndex <= 0) {
        return []
      }
      const startIndex = Math.max(0, selectedIndex - this.normalizedSingleIssueRecentCount)
      return chronologicalRecords.slice(startIndex, selectedIndex)
    },
    selectedIssueAllHistoryRecords() {
      const record = this.selectedIssueRecord
      if (!record) {
        return []
      }
      const chronologicalRecords = sortByExpect(this.records)
      const selectedIndex = chronologicalRecords.findIndex(item => item.expect === record.expect)
      return selectedIndex > 0 ? chronologicalRecords.slice(0, selectedIndex) : []
    },
    analysisPanels() {
      const panels = [
        {
          key: 'front',
          label: this.frontLabel,
          max: this.currentLotteryConfig.frontMax,
          fields: this.currentLotteryConfig.frontFields
        }
      ]
      if (this.currentLotteryConfig.backMax) {
        panels.push({
          key: 'back',
          label: this.backLabel,
          max: this.currentLotteryConfig.backMax,
          fields: this.currentLotteryConfig.backFields
        })
      }
      return panels
    },
    selectedIssueAreas() {
      const record = this.selectedIssueRecord
      const panels = [
        {
          key: 'front',
          label: this.frontLabel,
          balls: record ? this.frontBalls(record) : []
        }
      ]
      if (this.currentLotteryConfig.backMax) {
        panels.push({
          key: 'back',
          label: this.backLabel,
          balls: record ? this.backBalls(record) : []
        })
      }
      return panels.map(panel => ({
        key: panel.key,
        label: panel.label,
        numbers: panel.balls
      }))
    },
    selectedIssueCooccurrenceAreas() {
      const records = this.selectedIssueHistoryRecords
      return this.analysisPanels.filter(panel => panel.fields.length > 1).map(panel => {
        const selectedNumbers = this.selectedIssueAreas.find(item => item.key === panel.key)
        const numbers = selectedNumbers ? selectedNumbers.numbers : []
        const rows = calculateCooccurrenceRows(panel.max, panel.fields, records)
        return {
          ...panel,
          records,
          numbers: sequenceNumbers(panel.max),
          allRows: rows,
          rows: rows.filter(row => numbers.includes(row.number))
        }
      })
    },
    selectedIssueNextOccurrenceAreas() {
      const records = this.selectedIssueHistoryRecords
      return this.analysisPanels.map(panel => {
        const selectedNumbers = this.selectedIssueAreas.find(item => item.key === panel.key)
        const numbers = selectedNumbers ? selectedNumbers.numbers : []
        const pairs = buildNextOccurrencePairs(panel.fields, records, this.normalizedSingleIssueRecentCount)
        const rows = calculateNextOccurrenceRows(panel.max, pairs)
        return {
          ...panel,
          records,
          pairs,
          numbers: sequenceNumbers(panel.max),
          rows: rows.filter(row => numbers.includes(row.number))
        }
      })
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.displayRows.length / this.pageSize))
    },
    pagedRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.displayRows.slice(start, start + this.pageSize)
    },
    trendRows() {
      return this.pagedRows.slice().reverse()
    },
    omissionAreas() {
      const panels = [
        {
          key: 'front',
          label: this.frontLabel,
          max: this.currentLotteryConfig.frontMax,
          fields: this.currentLotteryConfig.frontFields
        }
      ]
      if (this.currentLotteryConfig.backMax) {
        panels.push({
          key: 'back',
          label: this.backLabel,
          max: this.currentLotteryConfig.backMax,
          fields: this.currentLotteryConfig.backFields
        })
      }
      return panels.map(panel => ({
        ...panel,
        numbers: sequenceNumbers(panel.max),
        rows: calculateOmissionRows(panel.max, panel.fields, this.records, this.normalizedOmissionRecentCount),
        summaryRows: calculateOmissionSummaryRows(
          panel.max,
          panel.fields,
          this.records,
          this.normalizedOmissionSummaryRecentCount,
          this.omissionSummaryOptions,
          this.visibleOmissionSummaryRows
        )
      }))
    },
    cooccurrenceAreas() {
      const panels = [
        {
          key: 'front',
          label: this.frontLabel,
          max: this.currentLotteryConfig.frontMax,
          fields: this.currentLotteryConfig.frontFields
        }
      ]
      if (this.currentLotteryConfig.backMax) {
        panels.push({
          key: 'back',
          label: this.backLabel,
          max: this.currentLotteryConfig.backMax,
          fields: this.currentLotteryConfig.backFields
        })
      }
      const records = this.records.slice(0, this.normalizedCooccurrenceRecentCount)
      return panels.map(panel => ({
        ...panel,
        records,
        numbers: sequenceNumbers(panel.max),
        rows: calculateCooccurrenceRows(panel.max, panel.fields, records)
      }))
    },
    nextOccurrenceAreas() {
      const panels = [
        {
          key: 'front',
          label: this.frontLabel,
          max: this.currentLotteryConfig.frontMax,
          fields: this.currentLotteryConfig.frontFields
        }
      ]
      if (this.currentLotteryConfig.backMax) {
        panels.push({
          key: 'back',
          label: this.backLabel,
          max: this.currentLotteryConfig.backMax,
          fields: this.currentLotteryConfig.backFields
        })
      }
      return panels.map(panel => {
        const pairs = buildNextOccurrencePairs(panel.fields, this.records, this.normalizedNextOccurrenceRecentCount)
        return {
          ...panel,
          pairs,
          numbers: sequenceNumbers(panel.max),
          rows: calculateNextOccurrenceRows(panel.max, pairs)
        }
      })
    },
    trendNumbers() {
      return this.gridNumbers(this.trendMax, this.trendGridSize)
    },
    trendGridSize() {
      return this.trendArea === 'back' ? this.backGridSize : this.frontGridSize
    },
    trendMax() {
      return this.trendArea === 'back' ? this.currentLotteryConfig.backMax : this.currentLotteryConfig.frontMax
    },
    trendBallCount() {
      return this.trendArea === 'back'
        ? this.currentLotteryConfig.backFields.length
        : this.currentLotteryConfig.frontFields.length
    },
    trendSvgWidth() {
      return this.trendExpectWidth + this.trendNumbers.length * this.trendCellSize
    },
    trendSvgHeight() {
      return this.trendHeaderHeight + this.trendRows.length * this.trendRowHeight
    },
    trendTableStyle() {
      return {
        gridTemplateColumns: `${this.trendExpectWidth}px repeat(${this.trendNumbers.length}, ${this.trendCellSize}px)`
      }
    },
    trendScrollStyle() {
      return {
        minWidth: `${this.trendSvgWidth}px`
      }
    },
    trendLines() {
      const maxIndex = this.trendBallCount
      const indexes = this.trendLineIndex
        ? [this.trendLineIndex]
        : Array.from({ length: maxIndex }, (_, index) => index + 1)

      return indexes.map(index => {
        const points = this.trendRows
          .map((record, rowIndex) => {
            const ball = this.trendBalls(record)[index - 1]
            const colIndex = this.trendNumbers.indexOf(ball)
            if (colIndex < 0) {
              return null
            }
            const x = this.trendExpectWidth + colIndex * this.trendCellSize + this.trendCellSize / 2
            const y = this.trendHeaderHeight + rowIndex * this.trendRowHeight + this.trendRowHeight / 2
            return `${x},${y}`
          })
          .filter(Boolean)
          .join(' ')
        return {
          index,
          points,
          color: this.trendColors[(index - 1) % this.trendColors.length]
        }
      }).filter(line => line.points)
    },
    drawCardStyle() {
      const sizes = []
      if (this.gridArea !== 'back') {
        sizes.push(this.gridPanelWidth(this.frontGridSize))
      }
      if (this.currentLotteryConfig.backMax && this.gridArea !== 'front') {
        sizes.push(this.gridPanelWidth(this.backGridSize))
      }
      const width = Math.max(220, ...sizes) + 22
      return {
        minWidth: `${width}px`
      }
    }
  },
  watch: {
    repeatCountFilter() {
      this.expandedExpect = ''
      this.currentPage = 1
    },
    pageSize() {
      this.currentPage = 1
      this.expandedExpect = ''
    },
    trendArea() {
      this.trendLineIndex = 0
    },
    displayRows() {
      if (this.currentPage > this.totalPages) {
        this.currentPage = this.totalPages
      }
    }
  },
  methods: {
    switchLottery(lottery) {
      if (this.lottery === lottery) {
        return
      }
      this.lottery = lottery
      if (lottery !== 'kl8' && this.viewMode === 'singleIssueLegacy') {
        this.viewMode = 'records'
      }
      this.resetGridConfig()
      this.loadRecords()
    },
    frontBalls(record) {
      if (this.viewMode === 'repeats') {
        return record.frontBalls || []
      }
      return this.currentLotteryConfig.frontFields.map(field => record[field])
    },
    backBalls(record) {
      if (this.viewMode === 'repeats') {
        return record.backBalls || []
      }
      return this.currentLotteryConfig.backFields.map(field => record[field])
    },
    formatDate(value) {
      return value || ''
    },
    toggleMatches(expect) {
      this.expandedExpect = this.expandedExpect === expect ? '' : expect
    },
    showRecords() {
      this.viewMode = 'records'
      this.repeatStats = []
      this.repeatCountFilter = null
      this.expandedExpect = ''
      this.currentPage = 1
    },
    async showRepeatAnalysis() {
      this.viewMode = 'repeats'
      this.expandedExpect = ''
      this.currentPage = 1
      await this.analyzeRepeats()
    },
    showGridAnalysis() {
      this.viewMode = 'grid'
      this.expandedExpect = ''
      this.currentPage = 1
    },
    showTrendAnalysis() {
      this.viewMode = 'trend'
      this.expandedExpect = ''
      this.currentPage = 1
    },
    showOmissionAnalysis() {
      this.viewMode = 'omission'
      this.expandedExpect = ''
      this.currentPage = 1
    },
    showCooccurrenceAnalysis() {
      this.viewMode = 'cooccurrence'
      this.expandedExpect = ''
      this.currentPage = 1
    },
    showNextOccurrenceAnalysis() {
      this.viewMode = 'nextOccurrence'
      this.expandedExpect = ''
      this.currentPage = 1
    },
    showSingleIssueAnalysis() {
      this.viewMode = 'singleIssue'
      this.expandedExpect = ''
      this.currentPage = 1
      if (!this.selectedExpect && this.records.length) {
        this.selectedExpect = this.records[0].expect
      }
    },
    showLegacySingleIssueAnalysis() {
      if (this.lottery !== 'kl8') {
        return
      }
      this.viewMode = 'singleIssueLegacy'
      this.expandedExpect = ''
      this.currentPage = 1
      if (!this.selectedExpect && this.records.length) {
        this.selectedExpect = this.records[0].expect
      }
    },
    resetGridConfig() {
      this.frontGridSize = this.currentLotteryConfig.defaultFrontGridSize
      this.backGridSize = this.currentLotteryConfig.defaultBackGridSize
      this.gridFilterMode = 'recent'
      this.gridRecentCount = 10
      this.gridStartExpect = ''
      this.gridEndExpect = ''
      this.gridArea = 'all'
      this.trendArea = 'front'
      this.trendLineIndex = 0
    },
    gridStyle(size) {
      const cellSize = this.gridCellSize(size)
      return {
        gridTemplateColumns: `repeat(${Math.max(1, Number(size) || 1)}, ${cellSize}px)`,
        '--grid-cell-size': `${cellSize}px`
      }
    },
    gridCellSize(size) {
      const gridSize = Math.max(1, Number(size) || 1)
      if (gridSize >= 10) {
        return 18
      }
      if (gridSize >= 8) {
        return 20
      }
      if (gridSize >= 6) {
        return 24
      }
      return 28
    },
    gridPanelWidth(size) {
      const gridSize = Math.max(1, Number(size) || 1)
      const cellSize = this.gridCellSize(size)
      return gridSize * cellSize + (gridSize - 1) * 4
    },
    gridNumbers(max, size) {
      const count = Math.max(max, Math.pow(Math.max(1, Number(size) || 1), 2))
      return Array.from({ length: count }, (_, index) => String(index + 1).padStart(2, '0'))
    },
    omissionTableStyle(area) {
      const width = this.trendExpectWidth + area.numbers.length * this.trendCellSize
      return {
        gridTemplateColumns: `${this.trendExpectWidth}px repeat(${area.numbers.length}, ${this.trendCellSize}px)`,
        width: `${width}px`
      }
    },
    omissionScrollStyle(area) {
      const width = this.trendExpectWidth + area.numbers.length * this.trendCellSize
      return {
        minWidth: `${width}px`,
        width: `${width}px`
      }
    },
    omissionCellClass(cell, areaKey) {
      return {
        'omission-cell': true,
        hit: cell.hit,
        missing: !cell.hit,
        back: cell.hit && areaKey === 'back'
      }
    },
    omissionRowClass(index) {
      return index % 2 === 0 ? 'omission-row-even' : 'omission-row-odd'
    },
    trendOrder(record, num) {
      return this.trendBalls(record).indexOf(num) + 1
    },
    trendBalls(record) {
      return this.trendArea === 'back' ? this.backBalls(record) : this.frontBalls(record)
    },
    trendCellText(record, num) {
      const order = this.trendOrder(record, num)
      return order ? num : ''
    },
    trendCellClass(record, num) {
      const order = this.trendOrder(record, num)
      return {
        'trend-cell': true,
        selected: Boolean(order),
        muted: Boolean(this.trendLineIndex && order && order !== this.trendLineIndex),
        focus: Boolean(this.trendLineIndex && order === this.trendLineIndex)
      }
    },
    async syncHistory() {
      await this.saveRecords(`/api/${this.lottery}/sync-history`, 'history')
    },
    async fetchRecent() {
      await this.saveRecords(`/api/${this.lottery}/fetch-recent?expect=${this.recentExpect || 10}`, 'recent')
    },
    async runKl8WeightOptimization() {
      if (this.lottery !== 'kl8') {
        return
      }
      this.stopKl8WeightPolling()
      this.loading = true
      this.loadingAction = 'kl8Weight'
      this.message = ''
      try {
        const { data } = await axios.post('/api/kl8/weight-optimization/start')
        this.message = data.message || '快乐8自动调权已在后台运行。'
        this.startKl8WeightPolling()
      } catch (error) {
        this.message = error.response && error.response.data && error.response.data.message
          ? error.response.data.message
          : '快乐8自动调权启动失败，请确认后端和数据库正常。'
        this.loading = false
        this.loadingAction = ''
      }
    },
    startKl8WeightPolling() {
      this.pollKl8WeightStatus()
      this.kl8WeightPollTimer = window.setInterval(this.pollKl8WeightStatus, 2000)
    },
    stopKl8WeightPolling() {
      if (this.kl8WeightPollTimer) {
        window.clearInterval(this.kl8WeightPollTimer)
        this.kl8WeightPollTimer = null
      }
    },
    async pollKl8WeightStatus() {
      try {
        const { data } = await axios.get('/api/kl8/weight-optimization/status')
        const finished = data.finishedWindowCount || 0
        const total = data.totalWindowCount || 3
        if (data.status === 'RUNNING') {
          this.loading = true
          this.loadingAction = 'kl8Weight'
          this.message = `${data.message || '快乐8自动调权运行中'}（${finished}/${total}）`
          return
        }
        this.stopKl8WeightPolling()
        this.loading = false
        this.loadingAction = ''
        if (data.status === 'SUCCESS') {
          this.kl8WeightVersion += 1
          this.message = `快乐8自动调权完成（${finished}/${total}）。`
          return
        }
        if (data.status === 'FAILED') {
          this.message = data.errorMessage
            ? `快乐8自动调权失败：${data.errorMessage}`
            : '快乐8自动调权失败，请查看后端日志。'
        }
      } catch (error) {
        this.stopKl8WeightPolling()
        this.loading = false
        this.loadingAction = ''
        this.message = '无法获取快乐8自动调权进度，请确认后端服务正常。'
      }
    },
    normalizedMatchCount() {
      const value = Number(this.matchCount) || 2
      return Math.min(this.maxMatchCount, Math.max(2, value))
    },
    async analyzeRepeats() {
      this.loading = true
      this.loadingAction = 'repeats'
      this.message = ''
      this.matchCount = this.normalizedMatchCount()
      try {
        const { data } = await axios.get(`/api/${this.lottery}/repeats?matchCount=${this.matchCount}`)
        this.repeatStats = data
        this.repeatCountFilter = null
        this.expandedExpect = ''
        this.currentPage = 1
        this.viewMode = 'repeats'
        this.message = `按恰好重复 ${this.matchCount} 个${this.currentLotteryConfig.matchLabel}统计，共 ${data.length} 条。`
      } catch (error) {
        this.message = error.response && error.response.data && error.response.data.message
          ? error.response.data.message
          : '分析失败，请先同步历史数据。'
      } finally {
        this.loading = false
        this.loadingAction = ''
      }
    },
    async saveRecords(url, action) {
      this.loading = true
      this.loadingAction = action
      this.message = ''
      try {
        const { data } = await axios.post(url)
        this.message = `解析 ${data.parsedCount} 条，新增 ${data.insertedCount} 条，跳过 ${data.skippedCount} 条。`
        this.viewMode = 'records'
        await this.loadRecords()
      } catch (error) {
        this.message = error.response && error.response.data && error.response.data.message
          ? error.response.data.message
          : '抓取失败，请确认后端、MySQL 和网络访问正常。'
      } finally {
        this.loading = false
        this.loadingAction = ''
      }
    },
    async loadRecords() {
      const { data } = await axios.get(`/api/${this.lottery}`)
      this.records = data
      this.selectedExpect = data.length ? data[0].expect : ''
      this.repeatStats = []
      this.repeatCountFilter = null
      this.expandedExpect = ''
      this.currentPage = 1
      this.viewMode = 'records'
      this.matchCount = Math.min(this.matchCount, this.maxMatchCount)
    }
  }
}
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #f3f5f8;
  color: #1f2933;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.page {
  width: min(1280px, calc(100% - 32px));
  margin: 24px auto 36px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 20px 22px;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .06);
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  line-height: 1.2;
}

p {
  margin: 0;
  color: #647084;
}

.section-title {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.panel-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
}

.view-section {
  display: grid;
  gap: 10px;
  min-width: min(100%, 760px);
}

.data-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 22px;
}

.group-label {
  display: block;
  color: #5f6f82;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid #d7e0ea;
  border-radius: 8px;
  background: #f7f9fc;
}

.tabs button {
  min-width: 96px;
  border-color: transparent;
  background: transparent;
  color: #4b5b6c;
  font-weight: 600;
}

.tabs button.active {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  box-shadow: 0 4px 10px rgba(37, 99, 235, .22);
}

.panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(31, 41, 51, .04);
}

.mode-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 4px;
  border: 1px solid #d7e0ea;
  border-radius: 8px;
  background: #f7f9fc;
}

.mode-tabs button {
  border-color: transparent;
  background: transparent;
  color: #4b5b6c;
}

.mode-tabs button.active {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  box-shadow: 0 4px 10px rgba(37, 99, 235, .18);
}

.analysis-controls {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #dce5ef;
  border-radius: 8px;
  background: #f8fafc;
}

.field {
  display: grid;
  gap: 6px;
}

.field.compact input {
  width: 96px;
}

.field input,
.field select {
  width: 128px;
}

.single-issue-select select {
  width: 220px;
}

.summary-options {
  min-width: 360px;
}

.checkbox-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 40px;
}

.check-field {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 9px;
  border: 1px solid #d7e0ea;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.check-field input {
  width: 14px;
  height: 14px;
  padding: 0;
  margin: 0;
}

input,
select,
button {
  height: 40px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 14px;
}

select {
  min-width: 96px;
  padding: 0 10px;
  background: #fff;
}

input {
  width: 128px;
  padding: 0 10px;
  background: #fff;
  color: #1f2933;
}

input:focus,
select:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, .12);
  outline: none;
}

button {
  padding: 0 16px;
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: background .15s ease, border-color .15s ease, box-shadow .15s ease, transform .15s ease;
}

button:hover:not(:disabled) {
  background: #1d4ed8;
  border-color: #1d4ed8;
  box-shadow: 0 4px 10px rgba(37, 99, 235, .18);
}

button.secondary {
  border-color: #cbd5e1;
  background: #fff;
  color: #1f2933;
}

button.secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
  box-shadow: none;
}

button:disabled {
  cursor: not-allowed;
  opacity: .65;
}

.link-button {
  height: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1f73d1;
  font-weight: 700;
}

.link-button:disabled {
  color: #7b8794;
}

.message {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1e3a8a;
}

.omission-analysis,
.cooccurrence-analysis {
  display: grid;
  gap: 12px;
}

.single-analysis {
  display: grid;
  gap: 12px;
}

.single-draw,
.single-panel {
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
}

.single-draw {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.single-draw-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #edf2f7;
}

.single-draw-title span {
  color: #718096;
  font-size: 12px;
}

.single-draw-area {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.single-area-label {
  min-width: 44px;
  color: #5f6f82;
  font-size: 13px;
  font-weight: 700;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
  padding: 12px;
}

.kl8-tier-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 2.6fr) minmax(0, .8fr);
  gap: 10px;
  padding: 12px;
}

.kl8-tier-card {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.kl8-tier-recommend {
  border-color: #93c5fd;
  background: #eff6ff;
}

.kl8-tier-middle {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.kl8-tier-avoid {
  border-color: #fecaca;
  background: #fff7f7;
}

.kl8-tier-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.kl8-tier-title strong {
  color: #1e3a8a;
  font-size: 13px;
}

.kl8-tier-avoid .kl8-tier-title strong {
  color: #991b1b;
}

.kl8-tier-title span {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.kl8-tier-numbers {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px;
}

.kl8-tier-numbers .ball {
  flex: 0 0 30px;
}

.recommend-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.recommend-card.avoid-card {
  border-color: #fecaca;
  background: #fffafa;
}

.recommend-card.hit-code-card {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.repeated-code-list {
  grid-template-columns: 1fr;
}

.recommend-title {
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 800;
}

.recommend-title.avoid-title {
  color: #991b1b;
}

.recommend-area {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.recommend-area em {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.repeated-code-area {
  align-items: flex-end;
}

.repeat-number-item {
  display: inline-grid;
  justify-items: center;
  gap: 4px;
}

.repeat-number-item em {
  margin-left: 0;
  color: #475569;
  font-size: 11px;
  line-height: 1;
}

.ball.hit {
  outline: 3px solid #22c55e;
  outline-offset: 2px;
  box-shadow: 0 0 0 2px #dcfce7;
}

.single-panel {
  overflow: hidden;
}

.single-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
  background: #f8fafc;
}

.single-panel-header span {
  color: #718096;
  font-size: 12px;
}

@media (max-width: 900px) {
  .kl8-tier-grid {
    grid-template-columns: 1fr;
  }
}

.single-stat-summary {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
  background: #fff;
}

.single-stat-rule {
  color: #64748b;
  font-size: 12px;
}

.single-stat-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.single-stat-card {
  display: grid;
  gap: 2px;
  min-width: 92px;
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
}

.single-stat-card span {
  color: #475569;
  font-size: 12px;
}

.single-stat-card strong {
  color: #1d4ed8;
  font-size: 18px;
  line-height: 1;
}

.single-number-stats {
  display: grid;
  gap: 14px;
  padding: 12px;
}

.single-number-area {
  display: grid;
  gap: 8px;
}

.single-number-area-title {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.single-number-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.single-number-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.single-number-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.single-number-stat-block {
  display: grid;
  gap: 5px;
}

.single-number-stat-block strong {
  color: #475569;
  font-size: 12px;
}

.single-number-buckets {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.single-number-bucket {
  display: grid;
  gap: 4px;
  min-width: 92px;
  max-width: 100%;
  padding: 5px 6px;
  border-radius: 5px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.single-number-bucket span {
  color: #475569;
}

.single-number-bucket em {
  display: grid;
  gap: 2px;
  color: #1d4ed8;
  font-style: normal;
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.single-number-bucket b {
  font-weight: 700;
}

.single-omission-wrap {
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.single-omission-table {
  min-width: 560px;
  border-collapse: collapse;
}

.single-omission-table th,
.single-omission-table td {
  padding: 8px 12px;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
  text-align: center;
}

.single-omission-table th {
  background: #f8fafc;
  color: #4b5b6c;
  font-size: 12px;
}

.analysis-note {
  padding: 10px 12px;
  border-left: 3px solid #2563eb;
  background: #eff6ff;
  color: #4b5b6c;
  font-size: 13px;
  line-height: 1.6;
}

.omission-chart {
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
  overflow: hidden;
}

.omission-chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #edf2f7;
  background: #f8fafc;
}

.omission-chart-header span {
  color: #718096;
  font-size: 12px;
}

.omission-wrap {
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.omission-table {
  position: relative;
  display: grid;
}

.omission-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
  font-size: 12px;
  font-weight: 700;
}

.omission-cell.missing {
  color: #94a3b8;
  font-weight: 600;
}

.omission-row-even {
  background: #fff;
}

.omission-row-odd {
  background: #f8fafc;
}

.omission-cell.missing.omission-row-odd {
  color: #7f8ea3;
}

.omission-cell.hit {
  width: 24px;
  height: 24px;
  min-height: 24px;
  margin: 7px 4px;
  border-radius: 50%;
  background: #dc2626;
  color: #fff;
  font-weight: 800;
  box-shadow: 0 3px 8px rgba(220, 38, 38, .24);
  z-index: 2;
}

.omission-cell.hit.back {
  background: #2563eb;
  box-shadow: 0 3px 8px rgba(37, 99, 235, .24);
}

.omission-summary-title,
.omission-summary-cell {
  background: #f8fafc;
}

.omission-summary-title {
  align-items: center;
  color: #334155;
}

.omission-summary-cell {
  color: #475569;
  font-weight: 700;
}

.cooccurrence-chart {
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
  overflow: hidden;
}

.cooccurrence-wrap {
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.cooccurrence-table {
  min-width: 760px;
  border-collapse: separate;
  border-spacing: 0;
}

.cooccurrence-table th,
.cooccurrence-table td {
  min-width: 42px;
  height: 34px;
  padding: 6px 8px;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
  text-align: center;
  white-space: nowrap;
}

.cooccurrence-table th {
  position: sticky;
  top: 0;
  z-index: 3;
  background: #f8fafc;
  color: #4b5b6c;
  font-size: 12px;
}

.cooccurrence-table .sticky-col {
  position: sticky;
  left: 0;
  z-index: 4;
  min-width: 58px;
  background: #fff;
  font-weight: 700;
}

.cooccurrence-table .sticky-col.second {
  left: 58px;
  min-width: 78px;
  background: #f8fafc;
}

.cooccurrence-table th.sticky-col {
  z-index: 5;
  background: #f8fafc;
}

.cooccurrence-table td.diagonal {
  background: #f1f5f9;
  color: #94a3b8;
}

.cooccurrence-table td.highlight {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
}

.matrix-number-button {
  width: auto;
  height: auto;
  min-height: 24px;
  padding: 2px 5px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.matrix-number-button:hover {
  background: #dbeafe;
  color: #1d4ed8;
  box-shadow: none;
}

.matrix-number-button.selected {
  background: #2563eb;
  color: #fff;
}

.cooccurrence-table th.selected-matrix-header {
  background: #dbeafe;
  color: #1d4ed8;
}

.cooccurrence-table tr.selected-matrix-row td {
  background: #f3e8ff;
  color: #6b21a8;
}

.cooccurrence-table tr.selected-matrix-row td.sticky-col,
.cooccurrence-table tr.selected-matrix-row td.sticky-col.second {
  background: #e9d5ff;
  color: #6b21a8;
}

.cooccurrence-table td.selected-matrix-column {
  background: #fef3c7;
  color: #92400e;
  font-weight: 800;
}

.cooccurrence-table td {
  cursor: pointer;
}

.cooccurrence-table td:hover {
  background: #e0f2fe;
}

.cooccurrence-table tr.selected-matrix-row td.selected-matrix-column {
  background: #fed7aa;
  color: #9a3412;
  box-shadow: inset 0 0 0 2px #f97316;
}

.co-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 24px;
  border-radius: 50%;
  background: #fee2e2;
  color: #b91c1c;
}

.table-wrap {
  overflow: auto;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
}

.grid-analysis {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: start;
  overflow-x: auto;
}

.draw-card {
  flex: 0 0 auto;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
  overflow: hidden;
}

.draw-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 11px;
  border-bottom: 1px solid #edf2f7;
  background: #f8fafc;
}

.draw-card-header strong {
  font-size: 14px;
}

.draw-card-header span {
  color: #5f6f82;
  font-size: 12px;
}

.grid-sections {
  display: grid;
  gap: 10px;
  padding: 10px;
}

.number-panel-title {
  margin-bottom: 6px;
  color: #4b5b6c;
  font-size: 12px;
  font-weight: 700;
}

.number-grid {
  display: grid;
  gap: 4px;
  width: max-content;
  max-width: 100%;
}

.number-grid span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--grid-cell-size, 24px);
  height: var(--grid-cell-size, 24px);
  border: 1px solid #d8e0ea;
  border-radius: 4px;
  background: #fff;
  color: #526071;
  font-size: clamp(9px, calc(var(--grid-cell-size, 24px) * .44), 12px);
  font-weight: 700;
}

.number-grid span.selected {
  border-color: #dc2626;
  background: #dc2626;
  color: #fff;
  box-shadow: 0 4px 10px rgba(220, 38, 38, .22);
}

.grid-empty {
  padding: 24px;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
}

.trend-wrap {
  position: relative;
  overflow: auto;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 22px rgba(31, 41, 51, .05);
}

.trend-scroll {
  position: relative;
}

.trend-lines {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.trend-table {
  position: relative;
  z-index: 1;
  display: grid;
}

.trend-head,
.trend-expect,
.trend-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.trend-head {
  position: sticky;
  top: 0;
  z-index: 3;
  background: #f8fafc;
  color: #4b5b6c;
  font-size: 12px;
  font-weight: 700;
}

.trend-expect {
  position: sticky;
  left: 0;
  z-index: 3;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  padding: 0 10px;
  background: #fff;
}

.trend-expect strong {
  font-size: 13px;
}

.trend-expect span {
  color: #718096;
  font-size: 11px;
}

.trend-cell {
  color: transparent;
  font-size: 12px;
  font-weight: 700;
}

.trend-cell.selected {
  width: 24px;
  height: 24px;
  min-height: 24px;
  margin: 7px 4px;
  border-radius: 50%;
  background: #dc2626;
  color: #fff;
  font-weight: 800;
  box-shadow: 0 3px 8px rgba(220, 38, 38, .24);
  z-index: 4;
}

.trend-cell.selected.muted {
  background: #cbd5e1;
  color: #64748b;
  box-shadow: none;
}

.trend-cell.selected.focus {
  background: #2563eb;
  color: #fff;
  box-shadow: 0 3px 8px rgba(37, 99, 235, .24);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #d9e1ea;
  border-radius: 8px;
  background: #fff;
  color: #4b5b6c;
  box-shadow: 0 6px 18px rgba(31, 41, 51, .04);
}

.pagination select {
  height: 32px;
  min-width: 72px;
  margin: 0 4px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-actions button {
  height: 32px;
  padding: 0 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #edf2f7;
  text-align: left;
}

th {
  background: #f8fafc;
  color: #4b5b6c;
  font-weight: 600;
}

tbody tr:hover {
  background: #fbfdff;
}

.ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 6px;
  border-radius: 50%;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.red {
  background: #dc2626;
}

.blue {
  background: #2563eb;
}

.detail-cell {
  background: #f8fafc;
}

.match-list {
  display: grid;
  gap: 10px;
}

.match-item {
  display: grid;
  grid-template-columns: 80px minmax(250px, 1fr) minmax(180px, auto);
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e1e7f0;
  border-radius: 6px;
  background: #fff;
}

.match-balls {
  white-space: nowrap;
}

.match-summary {
  color: #526071;
  white-space: nowrap;
}

.mini-ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  margin-left: 4px;
  border-radius: 12px;
  background: #eef3fb;
  color: #20242c;
  font-weight: 700;
  font-size: 12px;
}

.empty {
  color: #7b8794;
  text-align: center;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .panel-top,
  .data-actions,
  .pagination,
  .page-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .panel-top,
  .view-section,
  .data-actions,
  .tabs,
  .pagination,
  .analysis-controls {
    width: 100%;
  }

  .data-actions {
    padding-top: 0;
  }

  .tabs {
    display: flex;
    overflow-x: auto;
  }

  input,
  select,
  button {
    width: 100%;
  }

  .field.compact input,
  .field input,
  .field select {
    width: 100%;
  }

  .pagination select {
    width: 100%;
    margin: 6px 0 0;
  }

  .link-button {
    width: auto;
  }

  .match-item {
    grid-template-columns: 1fr;
  }

  .match-balls,
  .match-summary {
    white-space: normal;
  }

  .grid-analysis {
    grid-template-columns: 1fr;
  }
}
</style>
