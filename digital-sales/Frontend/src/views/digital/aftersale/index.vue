<template>
  <div class="app-container">
    <el-alert title="售后工单请从已完成订单的订单详情中创建" type="info" :closable="false" show-icon />
    <el-table v-loading="loading" :data="rows" class="table"><el-table-column prop="ticketNo" label="工单号" /><el-table-column label="类型"><template #default="{ row }">{{ row.ticketType === 'RETURN' ? '退货' : '维修' }}</template></el-table-column><el-table-column prop="orderNo" label="订单号" /><el-table-column prop="customerName" label="客户" /><el-table-column prop="productName" label="商品" /><el-table-column prop="applyQuantity" label="申请数量" /><el-table-column label="状态"><template #default="{ row }"><el-tag>{{ statusText(row.status) }}</el-tag></template></el-table-column><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link :type="['PENDING','PROCESSING'].includes(row.status) ? 'danger' : 'primary'" @click="showDetail(row)">{{ ['PENDING','PROCESSING'].includes(row.status) ? '处理' : '详情' }}</el-button></template></el-table-column></el-table>
    <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
    <el-dialog v-model="assignVisible" title="分配处理人" width="420px"><el-select v-model="selectedHandlerId" style="width:100%"><el-option v-for="item in handlers" :key="item.userId" :label="`${item.nickName}（${item.userName}）`" :value="item.userId" /></el-select><template #footer><el-button @click="assignVisible=false">取消</el-button><el-button type="primary" @click="submitAssign">确认分配</el-button></template></el-dialog>
    <el-dialog v-model="checkVisible" title="退货检查" width="500px"><el-form ref="checkRef" :model="checkForm" :rules="checkRules" label-width="100px"><el-form-item label="申请数量"><el-input :model-value="checkTicket.applyQuantity" disabled /></el-form-item><el-form-item label="实收数量" prop="receivedQuantity"><el-input-number v-model="checkForm.receivedQuantity" :min="1" :max="checkTicket.applyQuantity" /></el-form-item><el-form-item label="商品结论" prop="handlingResult"><el-radio-group v-model="checkForm.handlingResult"><el-radio value="GOOD">完好，重新入库</el-radio><el-radio value="DAMAGED">损坏，不入库</el-radio></el-radio-group></el-form-item><el-form-item label="检查说明"><el-input v-model="checkForm.handleResult" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="checkVisible=false">取消</el-button><el-button type="primary" @click="submitReturnCheck">确认完成检查</el-button></template></el-dialog>
    <el-dialog v-model="detailVisible" title="售后工单处理" width="760px"><el-descriptions :column="2" border><el-descriptions-item label="工单号">{{ detail.ticketNo }}</el-descriptions-item><el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item><el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item><el-descriptions-item label="商品">{{ detail.productName }} / {{ detail.skuName }}</el-descriptions-item><el-descriptions-item label="申请数量">{{ detail.applyQuantity }}</el-descriptions-item><el-descriptions-item label="实收数量">{{ detail.receivedQuantity ?? '—' }}</el-descriptions-item><el-descriptions-item label="问题描述" :span="2">{{ detail.faultDesc || '—' }}</el-descriptions-item><el-descriptions-item label="处理结果" :span="2">{{ detail.handleResult || '—' }}</el-descriptions-item></el-descriptions><el-divider content-position="left">处理记录</el-divider><el-table :data="detail.logs || []" empty-text="暂无处理记录"><el-table-column prop="operateType" label="操作" width="120" /><el-table-column prop="operateContent" label="处理说明" min-width="300" /><el-table-column prop="createBy" label="操作人" width="120" /><el-table-column prop="createTime" label="操作时间" width="180" /></el-table><template #footer><el-button v-if="detail.status === 'PENDING'" type="primary" @click="assignFromDetail">分配处理人</el-button><el-button v-if="detail.status === 'PROCESSING'" type="primary" @click="handleFromDetail">记录进展</el-button><el-button v-if="detail.status === 'PROCESSING' && detail.ticketType === 'RETURN'" type="success" @click="returnCheckFromDetail">退货检查</el-button><el-button v-if="detail.status === 'PROCESSING' && detail.ticketType === 'REPAIR'" type="success" @click="completeFromDetail">完成维修</el-button><el-button v-if="['PENDING','PROCESSING'].includes(detail.status)" type="danger" plain @click="closeFromDetail">关闭工单</el-button><el-button @click="detailVisible=false">关闭</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAfterSale, getAfterSale, assignAfterSale, handleAfterSale, completeAfterSale, closeAfterSale, listHandlers, checkReturn } from '@/api/digital/aftersale'
const loading = ref(false), rows = ref([]), total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })
const assignVisible=ref(false),handlers=ref([]),selectedHandlerId=ref(),currentTicket=ref()
const checkVisible=ref(false),checkRef=ref(),checkTicket=reactive({}),checkForm=reactive({receivedQuantity:1,handlingResult:'GOOD',handleResult:''})
const detailVisible=ref(false),detail=reactive({})
const checkRules={receivedQuantity:[{required:true,message:'请输入实收数量'}],handlingResult:[{required:true,message:'请选择商品结论'}]}
function loadData() { loading.value = true; listAfterSale(query).then(({ rows: data, total: count }) => { rows.value = data || []; total.value = count || 0 }).finally(() => { loading.value = false }) }
function showDetail(row){getAfterSale(row.ticketId).then(({data})=>{Object.assign(detail,data||{});currentTicket.value=data;detailVisible.value=true})}
function assignFromDetail(){detailVisible.value=false;assign(detail)}
function handleFromDetail(){detailVisible.value=false;handle(detail)}
function returnCheckFromDetail(){detailVisible.value=false;openReturnCheck(detail)}
function completeFromDetail(){detailVisible.value=false;complete(detail)}
function closeFromDetail(){detailVisible.value=false;close(detail)}
function assign(row) { currentTicket.value=row;selectedHandlerId.value=undefined;listHandlers().then(({data})=>{handlers.value=data||[];assignVisible.value=true}) }
function submitAssign(){if(!selectedHandlerId.value)return ElMessage.warning('请选择处理人');assignAfterSale(currentTicket.value.ticketId,{handlerId:selectedHandlerId.value}).then(()=>{ElMessage.success('分配成功');assignVisible.value=false;loadData()})}
function promptAction(title, api, row) { ElMessageBox.prompt('请输入处理结果', title, { inputType: 'textarea', inputPlaceholder: '请输入处理说明' }).then(({ value }) => api(row.ticketId, { handleResult: value })).then(() => { ElMessage.success(`${title}成功`); loadData() }) }
function handle(row) { promptAction('记录处理进展', handleAfterSale, row) }
function complete(row) { promptAction('完成售后工单', completeAfterSale, row) }
function close(row) { promptAction('关闭售后工单', closeAfterSale, row) }
function openReturnCheck(row){Object.assign(checkTicket,row);Object.assign(checkForm,{receivedQuantity:row.applyQuantity,handlingResult:'GOOD',handleResult:''});checkVisible.value=true}
async function submitReturnCheck(){await checkRef.value.validate();checkReturn(checkTicket.ticketId,checkForm).then(()=>{ElMessage.success('退货检查已完成');checkVisible.value=false;loadData()})}
function statusText(status) { return ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', CLOSED: '已关闭' })[status] || status }
onMounted(loadData)
</script>
<style scoped>.table{margin-top:20px}</style>
