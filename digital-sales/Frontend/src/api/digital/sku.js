import request from '@/utils/request'
export function listSku(query) { return request({ url: '/digital/sku/list', method: 'get', params: query }) }
export function addSku(data) { return request({ url: '/digital/sku', method: 'post', data }) }
export function updateSku(data) { return request({ url: '/digital/sku', method: 'put', data }) }
export function deleteSku(id) { return request({ url: `/digital/sku/${id}`, method: 'delete' }) }
export function generateBarcode() { return request({ url: '/digital/sku/generate-barcode', method: 'get' }).then(res => res.data) }
