<template><DigitalList :api="listSku" :columns="columns" :fields="fields" id-prop="skuId" :create="addSku" :update="updateSku" :remove="deleteSku" :generate-barcode="generateBarcode" /></template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import DigitalList from '@/views/digital/common/DigitalList.vue'; import { listSku, addSku, updateSku, deleteSku, generateBarcode } from '@/api/digital/sku'
import { listProduct } from '@/api/digital/product'
const columns = [{ prop: 'skuCode', label: 'SKU编码' }, { prop: 'productName', label: '商品名称' }, { prop: 'skuName', label: '规格' }, { prop: 'salePrice', label: '销售价' }, { prop: 'stock', label: '库存' }, { prop: 'warnStock', label: '预警值' }, { prop: 'status', label: '状态' }]
const products = ref([])
const fields = computed(() => [{ prop: 'productId', label: '所属商品', required: true, options: products.value.map(item => ({ label: `${item.productName} / ${item.brand || '无品牌'}`, value: item.productId })) }, { prop: 'skuCode', label: 'SKU编码', required: true }, { prop: 'skuName', label: '规格名称', required: true }, { prop: 'salePrice', label: '销售价', required: true }, { prop: 'costPrice', label: '成本价' }, { prop: 'barcode', label: '条码', generate: true }, { prop: 'warnStock', label: '预警库存', required: true, default: 0 }, { prop: 'status', label: '上下架', required: true, default: '0', options: [{ label: '上架', value: '0' }, { label: '下架', value: '1' }] }])
onMounted(() => listProduct({ pageNum: 1, pageSize: 500, status: '0' }).then(res => { products.value = res.rows || [] }))
</script>
