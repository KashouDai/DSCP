<template>
  <div class="app-container">
    <el-button v-hasPermi="['digital:customer:add']" type="primary" icon="Plus" @click="openCreate">新增客户</el-button>
    <el-table v-loading="loading" :data="rows" class="table"><el-table-column prop="customerName" label="客户名称" /><el-table-column prop="phone" label="联系电话" /><el-table-column prop="email" label="邮箱" /><el-table-column prop="address" label="地址" /><el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="220"><template #default="{ row }"><el-button link type="primary" @click="showOrders(row)">历史订单</el-button><el-button v-hasPermi="['digital:customer:edit']" link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-hasPermi="['digital:customer:remove']" link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table>
    <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
    <el-dialog v-model="formVisible" :title="form.customerId ? '编辑客户' : '新增客户'" width="520px"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" /></el-form-item><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item><el-form-item label="状态" prop="status"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template></el-dialog>
    <el-dialog v-model="ordersVisible" :title="`${currentCustomer.customerName || ''}的历史订单`" width="820px"><el-table v-loading="ordersLoading" :data="orders"><el-table-column prop="orderNo" label="订单编号" /><el-table-column prop="totalAmount" label="订单金额" /><el-table-column label="状态"><template #default="{ row }">{{ orderStatus(row.status) }}</template></el-table-column><el-table-column prop="createTime" label="创建时间" /></el-table></el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCustomer, addCustomer, updateCustomer, deleteCustomer } from '@/api/digital/customer'
import { listOrder } from '@/api/digital/order'
const loading=ref(false),rows=ref([]),total=ref(0),query=reactive({pageNum:1,pageSize:10})
const formVisible=ref(false),formRef=ref(),form=reactive({}),rules={customerName:[{required:true,message:'请输入客户名称'}],status:[{required:true,message:'请选择状态'}]}
const ordersVisible=ref(false),ordersLoading=ref(false),orders=ref([]),currentCustomer=reactive({})
function loadData(){loading.value=true;listCustomer(query).then(res=>{rows.value=res.rows||[];total.value=res.total||0}).finally(()=>loading.value=false)}
function openCreate(){Object.keys(form).forEach(key=>delete form[key]);Object.assign(form,{status:'0'});formVisible.value=true}
function openEdit(row){Object.assign(form,row);formVisible.value=true}
async function submit(){await formRef.value.validate();const action=form.customerId?updateCustomer(form):addCustomer(form);action.then(()=>{ElMessage.success('保存成功');formVisible.value=false;loadData()})}
function remove(row){ElMessageBox.confirm(`确认删除客户“${row.customerName}”吗？被订单引用的客户只能停用。`,'提示',{type:'warning'}).then(()=>deleteCustomer(row.customerId)).then(()=>{ElMessage.success('删除成功');loadData()})}
function showOrders(row){Object.assign(currentCustomer,row);ordersVisible.value=true;ordersLoading.value=true;listOrder({pageNum:1,pageSize:100,customerId:row.customerId}).then(res=>orders.value=res.rows||[]).finally(()=>ordersLoading.value=false)}
function orderStatus(status){return({DRAFT:'草稿',CONFIRMED:'已确认',COMPLETED:'已完成',CANCELLED:'已取消'})[status]||status}
onMounted(loadData)
</script>
<style scoped>.table{margin-top:20px}</style>
