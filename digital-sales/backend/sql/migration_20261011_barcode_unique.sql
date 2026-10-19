-- 商品规格条码唯一约束
alter table biz_product_sku
  add unique key uk_biz_product_sku_barcode (barcode);
