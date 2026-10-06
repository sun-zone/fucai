export function sequenceNumbers(max) {
  return Array.from({ length: max }, (_, index) => String(index + 1).padStart(2, '0'))
}

export function sortByExpect(records) {
  return records.slice().sort((left, right) => {
    return String(left.expect).localeCompare(String(right.expect), undefined, { numeric: true })
  })
}

export function ballsFromFields(record, fields) {
  return fields.map(field => String(record[field] || '').padStart(2, '0'))
}

export function calculateCooccurrenceRows(max, fields, sourceRecords) {
  const numbers = sequenceNumbers(max)
  return numbers.map(baseNumber => {
    let appearCount = 0
    const counts = numbers.reduce((result, number) => {
      result[number] = 0
      return result
    }, {})
    sourceRecords.forEach(record => {
      const balls = ballsFromFields(record, fields)
      if (!balls.includes(baseNumber)) {
        return
      }
      appearCount += 1
      numbers.forEach(number => {
        if (number !== baseNumber && balls.includes(number)) {
          counts[number] += 1
        }
      })
    })
    return {
      number: baseNumber,
      appearCount,
      cells: numbers.map(number => ({
        number,
        same: number === baseNumber,
        count: counts[number]
      }))
    }
  })
}

export function buildNextOccurrencePairs(fields, sourceRecords, displayCount) {
  const chronologicalRecords = sortByExpect(sourceRecords)
  const pairs = []
  for (let index = 0; index < chronologicalRecords.length - 1; index += 1) {
    const current = chronologicalRecords[index]
    const next = chronologicalRecords[index + 1]
    pairs.push({
      currentExpect: current.expect,
      nextExpect: next.expect,
      currentBalls: ballsFromFields(current, fields),
      nextBalls: ballsFromFields(next, fields)
    })
  }
  return pairs.slice(Math.max(0, pairs.length - displayCount))
}

export function calculateNextOccurrenceRows(max, pairs) {
  const numbers = sequenceNumbers(max)
  return numbers.map(baseNumber => {
    let baseCount = 0
    const counts = numbers.reduce((result, number) => {
      result[number] = 0
      return result
    }, {})
    pairs.forEach(pair => {
      if (!pair.currentBalls.includes(baseNumber)) {
        return
      }
      baseCount += 1
      numbers.forEach(number => {
        if (pair.nextBalls.includes(number)) {
          counts[number] += 1
        }
      })
    })
    return {
      number: baseNumber,
      baseCount,
      cells: numbers.map(number => ({
        number,
        count: counts[number]
      }))
    }
  })
}

export function calculateOmissionRows(max, fields, sourceRecords, displayCount) {
  const numbers = sequenceNumbers(max)
  const omissionCounts = numbers.map(() => 0)
  const rows = sortByExpect(sourceRecords).map(record => {
    const balls = ballsFromFields(record, fields)
    return {
      expect: record.expect,
      drawDate: record.drawDate,
      cells: numbers.map((number, index) => {
        const appeared = balls.includes(number)
        if (appeared) {
          omissionCounts[index] = 0
          return {
            number,
            hit: true,
            text: number
          }
        }
        omissionCounts[index] += 1
        return {
          number,
          hit: false,
          text: omissionCounts[index]
        }
      })
    }
  })
  return rows.slice(Math.max(0, rows.length - displayCount))
}

export function calculateOmissionSummaryRows(max, fields, sourceRecords, displayCount, summaryOptions, visibleRows) {
  const numbers = sequenceNumbers(max)
  const chronologicalRecords = sortByExpect(sourceRecords).slice(Math.max(0, sourceRecords.length - displayCount))
  const stats = numbers.map(number => {
    let omission = 0
    const values = chronologicalRecords.map(record => {
      const appeared = fields.some(field => String(record[field] || '').padStart(2, '0') === number)
      omission = appeared ? 0 : omission + 1
      return omission
    })
    const positiveValues = values.filter(value => value > 0)
    const total = values.reduce((sum, value) => sum + value, 0)
    return {
      number,
      current: values.length ? values[values.length - 1] : 0,
      max: values.length ? Math.max(...values) : 0,
      min: positiveValues.length ? Math.min(...positiveValues) : 0,
      average: values.length ? (total / values.length).toFixed(1) : '0.0'
    }
  })
  return summaryOptions.filter(option => {
    return visibleRows.includes(option.key)
  }).map(row => ({
    key: row.key,
    label: row.label,
    cells: stats.map(item => ({
      number: item.number,
      text: item[row.key]
    }))
  }))
}
