<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" @submit.prevent="loadData">
      <el-form-item label="关键词"><el-input v-model="queryParams.keyword" placeholder="请输入关键词" clearable @keyup.enter="loadData" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="loadData">搜索</el-button><el-button icon="Refresh" @click="reset">重置</el-button><el-button v-if="props.create" type="primary" icon="Plus" @click="openCreate">新增</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" empty-text="暂无数据"><el-table-column v-for="column in columns" :key="column.prop" v-bind="column"><template v-if="column.prop === 'status'" #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column v-if="props.update || props.remove" label="操作" width="160"><template #default="{ row }"><el-button v-if="props.update" link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-if="props.remove" link type="danger" @click="removeRow(row)">删除</el-button></template></el-table-column></el-table>
    <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="loadData" />
    <el-dialog v-model="dialogVisible" :title="formTitle" width="500px"><el-form ref="formRef" :model="form" :rules="rules" label-width="90px"><el-form-item v-for="field in fields" :key="field.prop" :label="field.label" :prop="field.prop"><el-select v-if="field.options" v-model="form[field.prop]" style="width:100%"><el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" /></el-select><el-input v-else v-model="form[field.prop]" :type="field.type || 'text'" :disabled="field.disabled" /><el-button v-if="field.generate" native-type="button" link type="primary" :loading="generating" @click.stop.prevent="generateField(field)">自动生成</el-button></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确定</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
const props = defineProps({ api: { type: Function, required: true }, columns: { type: Array, required: true }, fields: { type: Array, default: () => [] }, idProp: { type: String, default: 'id' }, create: Function, update: Function, remove: Function, generateBarcode: Function })
const loading = ref(false), rows = ref([]), total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: undefined })
const dialogVisible = ref(false), editing = ref(false), generating = ref(false), formRef = ref(), form = reactive({})
const formTitle = computed(() => editing.value ? '编辑' : '新增')
const rules = computed(() => Object.fromEntries(props.fields.filter(field => field.required).map(field => [field.prop, [{ required: true, message: `请输入${field.label}`, trigger: 'blur' }]])))
function loadData() { loading.value = true; props.api(queryParams).then(({ rows: data, total: count }) => { rows.value = data || []; total.value = count || 0 }).finally(() => { loading.value = false }) }
function reset() { queryParams.pageNum = 1; queryParams.keyword = undefined; loadData() }
function openCreate() { editing.value = false; Object.keys(form).forEach(key => delete form[key]); props.fields.forEach(field => { form[field.prop] = field.default ?? undefined }); dialogVisible.value = true }
function openEdit(row) { editing.value = true; Object.assign(form, row); dialogVisible.value = true }
async function generateField(field) { if (!props.generateBarcode || generating.value) return; generating.value = true; try { const barcode = await props.generateBarcode(); if (!barcode) throw new Error('接口未返回条码'); form[field.prop] = barcode; ElMessage.success('条码生成成功') } catch (error) { ElMessage.error(error?.message || '条码生成失败') } finally { generating.value = false } }
async function submitForm() { await formRef.value.validate(); const result = editing.value ? props.update(form) : props.create(form); result.then(() => { ElMessage.success('保存成功'); dialogVisible.value = false; loadData() }) }
function removeRow(row) { ElMessageBox.confirm('确认删除该数据吗？', '提示', { type: 'warning' }).then(() => props.remove(row[props.idProp])).then(() => { ElMessage.success('删除成功'); loadData() }) }
onMounted(loadData)
</script>
