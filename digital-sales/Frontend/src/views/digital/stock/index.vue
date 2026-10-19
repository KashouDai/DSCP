<template>
  <div class="app-container">
    <el-button icon="List" @click="recordVisible = true; loadRecords()">库存流水</el-button>
    <el-table v-loading="loading" :data="rows" class="table"><el-table-column prop="skuCode" label="SKU编码" /><el-table-column prop="skuName" label="规格" /><el-table-column prop="productName" label="商品名称" /><el-table-column prop="stock" label="当前库存" /><el-table-column prop="warnStock" label="预警库存" /><el-table-column label="库存状态"><template #default="{ row }"><el-tag :type="row.stock <= row.warnStock ? 'danger' : 'success'">{{ row.stock <= row.warnStock ? '库存不足' : '正常' }}</el-tag></template></el-table-column><el-table-column label="操作" width="100"><template #default="{ row }"><el-button v-hasPermi="['digital:stock:in']" link type="primary" @click="openAdjust(row)">库存调整</el-button></template></el-table-column></el-table>
    <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
    <el-dialog v-model="dialogVisible" title="库存调整" width="500px"><el-form ref="formRef" :model="form" :rules="rules" label-width="100px"><el-form-item label="商品规格"><el-input v-model="form.skuLabel" disabled /></el-form-item><el-form-item label="当前库存"><el-input :model-value="String(form.currentStock)" disabled /></el-form-item><el-form-item label="调整方向"><el-radio-group v-model="form.direction" @change="renewRequestNo"><el-radio value="IN">增加</el-radio><el-radio value="OUT">减少</el-radio></el-radio-group></el-form-item><el-form-item label="调整数量" prop="quantity"><el-input-number v-model="form.quantity" :min="1" :max="form.direction === 'OUT' ? form.currentStock : undefined" /></el-form-item><el-form-item label="请求号"><el-input v-model="form.requestNo" disabled /></el-form-item><el-form-item label="原因" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="例如：采购补货、盘点损耗" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitAdjust">确认调整</el-button></template></el-dialog>
    <el-dialog v-model="recordVisible" title="库存流水" width="1050px"><el-table v-loading="recordLoading" :data="records" empty-text="暂无库存流水"><el-table-column prop="skuCode" label="SKU编码" /><el-table-column prop="skuName" label="规格" /><el-table-column prop="recordType" label="流水类型" /><el-table-column prop="quantity" label="数量" /><el-table-column prop="beforeStock" label="变更前" /><el-table-column prop="afterStock" label="变更后" /><el-table-column prop="remark" label="调整原因" min-width="180" show-overflow-tooltip /><el-table-column prop="requestNo" label="请求号" /></el-table><pagination v-show="recordTotal > 0" v-model:page="recordQuery.pageNum" v-model:limit="recordQuery.pageSize" :total="recordTotal" @pagination="loadRecords" /></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listStock, listStockRecord, inboundStock, outboundStock } from '@/api/digital/stock'
const loading = ref(false), rows = ref([]), total = ref(0), dialogVisible = ref(false), formRef = ref()
const query = reactive({ pageNum: 1, pageSize: 10 }), form = reactive({ skuId: undefined, skuLabel: '', currentStock: 0, direction: 'IN', quantity: 1, requestNo: '', remark: '' })
const submitting = ref(false)
const rules = { quantity: [{ required: true, message: '请输入调整数量' }], remark: [{ required: true, message: '请输入调整原因', trigger: 'blur' }] }
const recordVisible = ref(false), recordLoading = ref(false), records = ref([]), recordTotal = ref(0), recordQuery = reactive({ pageNum: 1, pageSize: 10 })
function loadData() { loading.value = true; listStock(query).then(({ rows: data, total: count }) => { rows.value = data || []; total.value = count || 0 }).finally(() => { loading.value = false }) }
function loadRecords() { recordLoading.value = true; listStockRecord(recordQuery).then(({ rows: data, total: count }) => { records.value = data || []; recordTotal.value = count || 0 }).finally(() => { recordLoading.value = false }) }
function renewRequestNo() { form.requestNo = `${form.direction}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}` }
function openAdjust(row) { form.skuId = row.skuId; form.skuLabel = `${row.productName} / ${row.skuName}（${row.skuCode}）`; form.currentStock = row.stock; form.direction = 'IN'; form.quantity = 1; form.remark = ''; renewRequestNo(); dialogVisible.value = true }
async function submitAdjust() { await formRef.value.validate(); if (form.direction === 'OUT' && form.quantity > form.currentStock) return ElMessage.warning('减少数量不能超过当前库存'); submitting.value = true; try { const payload = { skuId: form.skuId, quantity: form.quantity, requestNo: form.requestNo, remark: form.remark }; await (form.direction === 'IN' ? inboundStock(payload) : outboundStock(payload)); ElMessage.success('库存调整成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false } }
onMounted(loadData)
</script>
<style scoped>.table{margin-top:20px}</style>
