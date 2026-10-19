<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="rows" class="table"><el-table-column prop="orderNo" label="订单号" /><el-table-column prop="ticketNo" label="售后工单" /><el-table-column prop="customerName" label="客户" /><el-table-column prop="productName" label="商品" /><el-table-column label="退款金额"><template #default="{ row }"><span v-if="row.status === 'PENDING'">待审核</span><span v-else-if="row.status === 'CANCELLED'">—</span><span v-else :class="{ 'amount-error': Number(row.refundAmount) <= 0 }">{{ Number(row.refundAmount) <= 0 ? '异常：0.00' : row.refundAmount }}</span></template></el-table-column><el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === 'REFUNDED' ? 'success' : row.status === 'CANCELLED' ? 'info' : 'warning'">{{ statusText(row.status) }}</el-tag></template></el-table-column><el-table-column prop="handleTime" label="处理时间" /><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button v-if="row.status === 'PENDING'" link type="primary" @click="audit(row)">审核</el-button><el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancel(row)">取消</el-button></template></el-table-column></el-table>
    <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
    <el-dialog v-model="auditVisible" title="审核退款" width="420px"><el-form ref="auditRef" :model="auditForm" :rules="auditRules" label-width="100px"><el-form-item label="可退上限"><span>{{ auditMax.toFixed(2) }} 元</span></el-form-item><el-form-item label="退款金额" prop="refundAmount"><el-input-number v-model="auditForm.refundAmount" :min="0.01" :max="auditMax" :precision="2" /></el-form-item></el-form><template #footer><el-button @click="auditVisible = false">取消</el-button><el-button type="primary" @click="submitAudit">确认退款</el-button></template></el-dialog>
    <el-dialog v-model="detailVisible" title="退款详情" width="620px"><el-descriptions :column="2" border><el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item><el-descriptions-item label="售后工单">{{ detail.ticketNo }}</el-descriptions-item><el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item><el-descriptions-item label="商品">{{ detail.productName }} / {{ detail.skuName }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item><el-descriptions-item label="退款金额">{{ detail.status === 'REFUNDED' ? detail.refundAmount : '—' }}</el-descriptions-item><el-descriptions-item label="处理人">{{ detail.handlerName || '—' }}</el-descriptions-item><el-descriptions-item label="处理时间">{{ detail.handleTime || '—' }}</el-descriptions-item><el-descriptions-item :label="detail.status === 'CANCELLED' ? '取消原因' : '备注'" :span="2">{{ detail.remark || '—' }}</el-descriptions-item></el-descriptions></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRefund, getRefund, auditRefund, cancelRefund } from '@/api/digital/refund'
const loading = ref(false), rows = ref([]), total = ref(0), auditVisible = ref(false), auditRef = ref(), currentId = ref(), auditMax = ref(0)
const detailVisible = ref(false), detail = reactive({})
const query = reactive({ pageNum: 1, pageSize: 10 }), auditForm = reactive({ refundAmount: 0 })
const auditRules = { refundAmount: [{ required: true, message: '请输入退款金额' }] }
function loadData() { loading.value = true; listRefund(query).then(({ rows: data, total: count }) => { rows.value = data || []; total.value = count || 0 }).finally(() => { loading.value = false }) }
function audit(row) { const receivedLimit = Number(row.salePrice || 0) * Number(row.receivedQuantity || 0); const detailLimit = Number(row.salePrice || 0) * Number(row.purchaseQuantity || 0) - Number(row.refundedAmount || 0); auditMax.value = Math.max(0, Math.min(receivedLimit, detailLimit)); currentId.value = row.refundId; auditForm.refundAmount = auditMax.value; auditVisible.value = true }
function showDetail(row) { getRefund(row.refundId).then(({ data }) => { Object.assign(detail, data || {}); detailVisible.value = true }) }
async function submitAudit() { await auditRef.value.validate(); auditRefund(currentId.value, auditForm).then(() => { ElMessage.success('退款审核成功'); auditVisible.value = false; loadData() }) }
function cancel(row) { ElMessageBox.prompt('请输入取消原因', '取消退款', { inputType: 'textarea', inputValidator: value => value ? true : '取消原因不能为空' }).then(({ value }) => cancelRefund(row.refundId, { remark: value })).then(() => { ElMessage.success('退款已取消'); loadData() }) }
function statusText(status) { return ({ PENDING: '待审核', REFUNDED: '已退款', CANCELLED: '已取消' })[status] || status }
onMounted(loadData)
</script>
<style scoped>.table{margin-top:20px}.amount-error{color:var(--el-color-danger);font-weight:600}</style>
