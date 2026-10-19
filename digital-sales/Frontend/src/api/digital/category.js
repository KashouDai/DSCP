import request from '@/utils/request'
export function listCategory(query) { return request({ url: '/digital/category/list', method: 'get', params: query }) }
export function addCategory(data) { return request({ url: '/digital/category', method: 'post', data }) }
export function updateCategory(data) { return request({ url: '/digital/category', method: 'put', data }) }
export function deleteCategory(id) { return request({ url: `/digital/category/${id}`, method: 'delete' }) }
