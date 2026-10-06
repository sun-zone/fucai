<template>
  <section class="pagination">
    <div>
      共 {{ total }} 条，每页
      <select :value="pageSize" aria-label="每页条数" @change="$emit('update:pageSize', Number($event.target.value))">
        <option :value="20">20</option>
        <option :value="50">50</option>
        <option :value="100">100</option>
      </select>
      条
    </div>
    <div class="page-actions">
      <button class="secondary" :disabled="currentPage === 1" @click="$emit('update:currentPage', 1)">首页</button>
      <button class="secondary" :disabled="currentPage === 1" @click="$emit('update:currentPage', currentPage - 1)">上一页</button>
      <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button class="secondary" :disabled="currentPage === totalPages" @click="$emit('update:currentPage', currentPage + 1)">下一页</button>
      <button class="secondary" :disabled="currentPage === totalPages" @click="$emit('update:currentPage', totalPages)">末页</button>
    </div>
  </section>
</template>

<script>
export default {
  name: 'PaginationBar',
  props: {
    total: { type: Number, required: true },
    pageSize: { type: Number, required: true },
    currentPage: { type: Number, required: true },
    totalPages: { type: Number, required: true }
  }
}
</script>
