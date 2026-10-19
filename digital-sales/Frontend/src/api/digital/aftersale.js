import request from '@/utils/request'
export function listAfterSale(query) { return request({ url: '/digital/aftersale/list', method: 'get', params: query }) }
export function getAfterSale(id) { return request({ url: `/digital/aftersale/${id}`, method: 'get' }) }
export function addAfterSale(data) { return request({ url: '/digital/aftersale', method: 'post', data }) }
export function assignAfterSale(id, data) { return request({ url: `/digital/aftersale/${id}/assign`, method: 'post', data }) }
export function handleAfterSale(id, data) { return request({ url: `/digital/aftersale/${id}/handle`, method: 'post', data }) }
export function completeAfterSale(id, data) { return request({ url: `/digital/aftersale/${id}/complete`, method: 'post', data }) }
export function checkReturn(id, data) { return request({ url: `/digital/aftersale/${id}/return-check`, method: 'post', data }) }
export function closeAfterSale(id, data) { return request({ url: `/digital/aftersale/${id}/close`, method: 'post', data }) }
export function listHandlers() { return request({ url: '/digital/aftersale/handlers', method: 'get' }) }
