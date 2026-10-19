import request from '@/utils/request'
export function listProduct(query) { return request({ url: '/digital/product/list', method: 'get', params: query }) }
export function addProduct(data) { return request({ url: '/digital/product', method: 'post', data }) }
export function updateProduct(data) { return request({ url: '/digital/product', method: 'put', data }) }
export function deleteProduct(id) { return request({ url: `/digital/product/${id}`, method: 'delete' }) }
