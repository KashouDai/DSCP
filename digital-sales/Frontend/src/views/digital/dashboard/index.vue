<template>
  <div class="app-container">
    <el-row :gutter="20"><el-col :span="6" v-for="item in cards" :key="item.label"><el-card shadow="hover" class="card"><div class="label">{{ item.label }}</div><div class="value">{{ item.value }}</div></el-card></el-col></el-row>
    <el-card shadow="never" class="table-card"><template #header><span>低库存预警</span></template><el-table :data="overview.lowStockList || []" v-loading="loading" empty-text="暂无低库存预警"><el-table-column prop="skuCode" label="SKU编码" /><el-table-column prop="skuName" label="规格名称" /><el-table-column prop="stock" label="当前库存" /><el-table-column prop="warnStock" label="预警库存" /></el-table></el-card>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getDashboardOverview } from '@/api/digital/dashboard'
const loading = ref(false)
const overview = reactive({ todayOrderCount: 0, todaySalesAmount: 0, todayRefundAmount: 0, pendingTicketCount: 0, lowStockList: [] })
const cards = computed(() => [{ label: '今日订单', value: overview.todayOrderCount }, { label: '今日销售额', value: overview.todaySalesAmount }, { label: '今日退款额', value: overview.todayRefundAmount }, { label: '待处理售后', value: overview.pendingTicketCount }])
function loadData() { loading.value = true; getDashboardOverview().then(({ data }) => Object.assign(overview, data || {})).finally(() => { loading.value = false }) }
onMounted(loadData)
</script>
<style scoped>.card{margin-bottom:20px}.label{color:#909399}.value{font-size:28px;margin-top:12px}.table-card{margin-top:20px}</style>
