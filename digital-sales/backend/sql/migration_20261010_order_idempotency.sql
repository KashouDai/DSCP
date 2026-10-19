-- 订单创建幂等迁移
-- 适用于已执行 digital_schema.sql 的既有数据库；本脚本只执行一次。

alter table biz_sale_order
  add column request_no varchar(64) default null comment '订单创建幂等请求号' after order_no,
  add column request_hash char(64) default null comment '订单创建请求摘要' after request_no,
  add unique key uk_biz_sale_order_request_no (request_no);
