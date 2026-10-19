import request from '@/utils/request'
export function listRefund(query) { return request({ url: '/digital/refund/list', method: 'get', params: query }) }
export function getRefund(id) { return request({ url: `/digital/refund/${id}`, method: 'get' }) }
export function auditRefund(id, data) { return request({ url: `/digital/refund/${id}/audit`, method: 'post', data }) }
export function cancelRefund(id, data) { return request({ url: `/digital/refund/${id}/cancel`, method: 'post', data }) }
