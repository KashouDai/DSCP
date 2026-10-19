import request from '@/utils/request'
export function getDashboardOverview() { return request({ url: '/digital/dashboard/overview', method: 'get' }) }
