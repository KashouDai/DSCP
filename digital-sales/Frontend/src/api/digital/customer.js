import request from '@/utils/request'
export function listCustomer(query) { return request({ url: '/digital/customer/list', method: 'get', params: query }) }
export function getCustomer(id) { return request({ url: `/digital/customer/${id}`, method: 'get' }) }
export function addCustomer(data) { return request({ url: '/digital/customer', method: 'post', data }) }
export function updateCustomer(data) { return request({ url: '/digital/customer', method: 'put', data }) }
export function deleteCustomer(id) { return request({ url: `/digital/customer/${id}`, method: 'delete' }) }
