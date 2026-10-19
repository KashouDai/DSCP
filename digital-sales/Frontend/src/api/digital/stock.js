import request from '@/utils/request'
export function listStock(query) { return request({ url: '/digital/stock/list', method: 'get', params: query }) }
export function listStockRecord(query) { return request({ url: '/digital/stock/record/list', method: 'get', params: query }) }
export function inboundStock(data) { return request({ url: '/digital/stock/inbound', method: 'post', data }) }
export function outboundStock(data) { return request({ url: '/digital/stock/outbound', method: 'post', data }) }
