-- 阶段三：订单与库存列表查询联合索引
-- 适用于已存在 digital_schema.sql 的数据库

alter table biz_sale_order
  add index idx_biz_sale_order_status_order (status, order_id);

alter table biz_stock_record
  add index idx_biz_stock_record_sku_type_order (sku_id, record_type, record_id);
