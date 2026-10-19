import request from '@/utils/request'

export function listOrder(query) {
  return request({
    url: '/digital/order/list',
    method: 'get',
    params: query
  })
}

export function addOrder(data) {
  return request({
    url: '/digital/order',
    method: 'post',
    data: data
  })
}

export function getOrder(id) { return request({ url: `/digital/order/${id}`, method: 'get' }) }
export function updateOrder(data) { return request({ url: '/digital/order', method: 'put', data }) }
export function confirmOrder(id) { return request({ url: `/digital/order/${id}/confirm`, method: 'post' }) }
export function cancelOrder(id) { return request({ url: `/digital/order/${id}/cancel`, method: 'post' }) }
export function completeOrder(id) { return request({ url: `/digital/order/${id}/complete`, method: 'post' }) }
export function deleteOrder(id) { return request({ url: `/digital/order/${id}`, method: 'delete' }) }
