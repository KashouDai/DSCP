<template><DigitalList :api="listProduct" :columns="columns" :fields="fields" id-prop="productId" :create="addProduct" :update="updateProduct" :remove="deleteProduct" /></template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import DigitalList from '@/views/digital/common/DigitalList.vue'; import { listProduct, addProduct, updateProduct, deleteProduct } from '@/api/digital/product'
import { listCategory } from '@/api/digital/category'
const columns = [{ prop: 'productId', label: '商品ID' }, { prop: 'productName', label: '商品名称' }, { prop: 'brand', label: '品牌' }, { prop: 'categoryName', label: '分类' }, { prop: 'status', label: '状态' }]
const categories = ref([])
const fields = computed(() => [{ prop: 'categoryId', label: '商品分类', required: true, options: categories.value.map(item => ({ label: item.categoryName, value: item.categoryId })) }, { prop: 'productName', label: '商品名称', required: true }, { prop: 'brand', label: '品牌' }, { prop: 'description', label: '商品描述', type: 'textarea' }, { prop: 'status', label: '上下架', required: true, default: '0', options: [{ label: '上架', value: '0' }, { label: '下架', value: '1' }] }])
onMounted(() => listCategory({ pageNum: 1, pageSize: 200, status: '0' }).then(res => { categories.value = res.rows || [] }))
</script>
