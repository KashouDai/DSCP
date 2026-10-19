<template><DigitalList :api="listCategory" :columns="columns" :fields="fields" id-prop="categoryId" :create="addCategory" :update="updateCategory" :remove="deleteCategory" /></template>
<script setup>
import DigitalList from '@/views/digital/common/DigitalList.vue'; import { listCategory, addCategory, updateCategory, deleteCategory } from '@/api/digital/category'
const columns = [{ prop: 'categoryId', label: '分类ID' }, { prop: 'categoryName', label: '分类名称' }, { prop: 'orderNum', label: '排序' }, { prop: 'status', label: '状态' }]
const fields = [{ prop: 'categoryName', label: '分类名称', required: true }, { prop: 'orderNum', label: '显示顺序', required: true, default: 0 }, { prop: 'status', label: '状态', required: true, default: '0', options: [{ label: '启用', value: '0' }, { label: '停用', value: '1' }] }]
</script>
