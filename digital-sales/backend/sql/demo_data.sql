-- ----------------------------
-- 数码业务演示数据（仅用于开发环境）
-- 依赖：先执行 dop_20260417.sql、digital_schema.sql、digital_init.sql
-- 说明：全部使用 9000+ 主键和 DEMO- 前缀，可重复执行，不会影响手工数据
--       数据规模：3 个分类、6 个商品、18 个 SKU、9 个客户、12 笔订单
--       状态覆盖：DRAFT/CONFIRMED/COMPLETED/CANCELLED、售后全状态、退款全状态
--       流程覆盖：销售、仓库入库、订单取消、退货、维修、退款
-- ----------------------------

-- 清理本脚本管理的演示数据（按引用顺序）
delete from biz_refund_record where refund_id >= 9000 or ticket_id >= 9000;
delete from biz_after_sale_log where log_id >= 9000 or ticket_id >= 9000;
delete from biz_after_sale_ticket where ticket_id >= 9000 or ticket_no like 'DEMO-%';
delete from biz_stock_record where record_id >= 9000 or request_no like 'DEMO-%';
delete from biz_sale_order_item where item_id >= 9000 or order_id >= 9000;
delete from biz_sale_order where order_id >= 9000 or order_no like 'DEMO-%';
delete from biz_customer where customer_id >= 9000;
delete from biz_product_sku where sku_id >= 9000 or sku_code like 'DEMO-%';
delete from biz_product where product_id >= 9000;
delete from biz_product_category where category_id >= 9000;

-- ----------------------------
-- 1、分类、商品与规格
-- ----------------------------
insert into biz_product_category (category_id, category_name, order_num, status, del_flag, create_by, create_time, remark)
values
(9001, '充电配件', 10, '0', '0', 'admin', now(), '演示数据'),
(9002, '音频设备', 20, '0', '0', 'admin', now(), '演示数据'),
(9003, '智能穿戴', 30, '0', '0', 'admin', now(), '演示数据');

insert into biz_product (product_id, category_id, product_name, brand, description, status, del_flag, create_by, create_time, remark)
values
(9001, 9001, '65W 氮化镓充电器', '绿联', '双口快充，支持手机和笔记本充电', '0', '0', 'admin', now(), '演示数据-充电配件'),
(9002, 9001, '20000mAh 移动电源', '小米', '大容量移动电源，支持双向快充', '0', '0', 'admin', now(), '演示数据-充电配件'),
(9003, 9002, '主动降噪蓝牙耳机', '漫步者', '入耳式主动降噪，支持蓝牙 5.3', '0', '0', 'admin', now(), '演示数据-音频设备'),
(9004, 9002, '便携蓝牙音箱', 'JBL', '防水便携音箱，适合户外使用', '0', '0', 'admin', now(), '演示数据-音频设备'),
(9005, 9003, '智能运动手环', '华为', '心率、睡眠和运动记录', '0', '0', 'admin', now(), '演示数据-智能穿戴'),
(9006, 9003, '智能健康手表', '华为', '健康监测、蓝牙通话和运动记录', '0', '0', 'admin', now(), '演示数据-智能穿戴');

-- 最终库存 = 初始入库 - 销售出库 + 取消恢复 + 完好退货入库
insert into biz_product_sku
(sku_id, product_id, sku_code, sku_name, sale_price, cost_price, barcode, stock, warn_stock, status, del_flag, create_by, create_time, remark)
values
(9001, 9001, 'DEMO-GAN65-BLACK', '黑色', 129.00, 78.00, 'DEMO6900000009001', 28, 5, '0', '0', 'admin', now(), '演示数据-退货后库存'),
(9002, 9001, 'DEMO-GAN65-WHITE', '白色', 129.00, 78.00, 'DEMO6900000009002', 35, 5, '0', '0', 'admin', now(), '演示数据-销售流程'),
(9003, 9001, 'DEMO-GAN65-KIT', '套装版', 149.00, 92.00, 'DEMO6900000009003', 16, 3, '0', '0', 'admin', now(), '演示数据-已确认订单'),
(9004, 9002, 'DEMO-PB20-BLACK', '黑色', 169.00, 105.00, 'DEMO6900000009004', 14, 5, '0', '0', 'admin', now(), '演示数据-维修流程'),
(9005, 9002, 'DEMO-PB20-WHITE', '白色', 169.00, 105.00, 'DEMO6900000009005', 4, 5, '0', '0', 'admin', now(), '演示数据-低库存'),
(9006, 9002, 'DEMO-PB20-BLUE', '蓝色', 179.00, 112.00, 'DEMO6900000009006', 20, 5, '0', '0', 'admin', now(), '演示数据-维修完成'),
(9007, 9003, 'DEMO-ANC-BLACK', '黑色', 299.00, 190.00, 'DEMO6900000009007', 26, 4, '0', '0', 'admin', now(), '演示数据-待处理维修'),
(9008, 9003, 'DEMO-ANC-WHITE', '白色', 299.00, 190.00, 'DEMO6900000009008', 22, 4, '0', '0', 'admin', now(), '演示数据-退款取消'),
(9009, 9003, 'DEMO-ANC-BLUE', '蓝色', 319.00, 205.00, 'DEMO6900000009009', 10, 4, '0', '0', 'admin', now(), '演示数据-取消恢复'),
(9010, 9004, 'DEMO-SPK-GRAY', '灰色', 219.00, 145.00, 'DEMO6900000009010', 17, 4, '0', '0', 'admin', now(), '演示数据-维修关闭'),
(9011, 9004, 'DEMO-SPK-GREEN', '绿色', 219.00, 145.00, 'DEMO6900000009011', 18, 4, '0', '0', 'admin', now(), '演示数据-今日确认'),
(9012, 9004, 'DEMO-SPK-BLUE', '蓝色', 239.00, 160.00, 'DEMO6900000009012', 11, 4, '0', '0', 'admin', now(), '演示数据-维修转退货'),
(9013, 9005, 'DEMO-BAND-BLACK', '黑色标准版', 199.00, 125.00, 'DEMO6900000009013', 33, 5, '0', '0', 'admin', now(), '演示数据-待处理退货'),
(9014, 9005, 'DEMO-BAND-PINK', '粉色标准版', 199.00, 125.00, 'DEMO6900000009014', 21, 5, '0', '0', 'admin', now(), '演示数据-草稿订单'),
(9015, 9005, 'DEMO-BAND-NFC', '黑色NFC版', 239.00, 151.00, 'DEMO6900000009015', 9, 8, '0', '0', 'admin', now(), '演示数据-退货关闭'),
(9016, 9006, 'DEMO-WATCH-BLACK', '曜石黑', 499.00, 310.00, 'DEMO6900000009016', 12, 3, '0', '0', 'admin', now(), '演示数据-处理中维修'),
(9017, 9006, 'DEMO-WATCH-GREEN', '云杉绿', 499.00, 310.00, 'DEMO6900000009017', 8, 3, '0', '0', 'admin', now(), '演示数据-今日确认'),
(9018, 9006, 'DEMO-WATCH-SILVER', '银色', 569.00, 355.00, 'DEMO6900000009018', 6, 3, '0', '0', 'admin', now(), '演示数据-草稿订单');

-- ----------------------------
-- 2、客户
-- ----------------------------
insert into biz_customer
(customer_id, customer_name, phone, email, address, status, del_flag, create_by, create_time, remark)
values
(9001, '演示客户-张伟', '13800009001', 'zhangwei@demo.local', '上海市浦东新区演示路1号', '0', '0', 'admin', now(), '演示数据'),
(9002, '演示客户-李娜', '13800009002', 'lina@demo.local', '北京市海淀区演示路2号', '0', '0', 'admin', now(), '演示数据'),
(9003, '演示客户-王芳', '13800009003', 'wangfang@demo.local', '广州市天河区演示路3号', '0', '0', 'admin', now(), '演示数据'),
(9004, '演示客户-赵磊', '13800009004', 'zhaolei@demo.local', '深圳市南山区演示路4号', '0', '0', 'admin', now(), '演示数据'),
(9005, '演示客户-陈晨', '13800009005', 'chenchen@demo.local', '杭州市西湖区演示路5号', '0', '0', 'admin', now(), '演示数据'),
(9006, '演示客户-刘洋', '13800009006', 'liuyang@demo.local', '成都市高新区演示路6号', '0', '0', 'admin', now(), '演示数据'),
(9007, '演示客户-周敏', '13800009007', 'zhoumin@demo.local', '武汉市洪山区演示路7号', '0', '0', 'admin', now(), '演示数据'),
(9008, '演示客户-孙浩', '13800009008', 'sunhao@demo.local', '南京市建邺区演示路8号', '0', '0', 'admin', now(), '演示数据'),
(9009, '张志勇', '13800009009', 'zhangzhiyong@demo.local', null, '0', '0', 'admin', now(), '演示数据');

-- ----------------------------
-- 3、销售订单与明细
-- ----------------------------
insert into biz_sale_order
(order_id, order_no, customer_id, customer_name, customer_phone, total_amount, status, confirm_by, confirm_time,
 complete_by, complete_time, cancel_by, cancel_time, create_by, create_time, update_by, update_time, remark)
values
(9001, 'DEMO-SO-9001', 9001, '演示客户-张伟', '13800009001', 1025.00, 'COMPLETED',
 'admin', date_sub(now(), interval 12 day), 'admin', date_sub(now(), interval 10 day), '', null,
 'admin', date_sub(now(), interval 12 day), 'admin', now(), '演示数据-销售、退货、维修'),
(9002, 'DEMO-SO-9002', 9002, '演示客户-李娜', '13800009002', 477.00, 'COMPLETED',
 'admin', date_sub(now(), interval 8 day), 'admin', date_sub(now(), interval 7 day), '', null,
 'admin', date_sub(now(), interval 8 day), 'admin', now(), '演示数据-损坏退货、维修关闭'),
(9003, 'DEMO-SO-9003', 9003, '演示客户-王芳', '13800009003', 897.00, 'COMPLETED',
 'admin', date_sub(now(), interval 6 day), 'admin', date_sub(now(), interval 5 day), '', null,
 'admin', date_sub(now(), interval 6 day), 'admin', now(), '演示数据-待处理退货与维修'),
(9004, 'DEMO-SO-9004', 9004, '演示客户-赵磊', '13800009004', 587.00, 'CONFIRMED',
  'admin', now(), '', null, '', null,
 'admin', date_sub(now(), interval 3 hour), 'admin', now(), '演示数据-今日成交'),
(9005, 'DEMO-SO-9005', 9005, '演示客户-陈晨', '13800009005', 169.00, 'CANCELLED',
 '', null, '', null, 'admin', date_sub(now(), interval 1 day),
 'admin', date_sub(now(), interval 2 day), 'admin', now(), '演示数据-草稿取消'),
(9006, 'DEMO-SO-9006', 9006, '演示客户-刘洋', '13800009006', 657.00, 'COMPLETED',
 'admin', date_sub(now(), interval 5 day), 'admin', date_sub(now(), interval 4 day), '', null,
 'admin', date_sub(now(), interval 5 day), 'admin', now(), '演示数据-退款取消、维修完成'),
(9007, 'DEMO-SO-9007', 9007, '演示客户-周敏', '13800009007', 199.00, 'DRAFT',
 '', null, '', null, '', null,
 'admin', date_sub(now(), interval 1 day), 'admin', now(), '演示数据-草稿订单'),
(9008, 'DEMO-SO-9008', 9008, '演示客户-孙浩', '13800009008', 319.00, 'CANCELLED',
 'admin', date_sub(now(), interval 4 day), '', null, 'admin', date_sub(now(), interval 3 day),
 'admin', date_sub(now(), interval 4 day), 'admin', now(), '演示数据-确认后取消并恢复库存'),
(9009, 'DEMO-SO-9009', 9001, '演示客户-张伟', '13800009001', 239.00, 'COMPLETED',
 'admin', date_sub(now(), interval 20 day), 'admin', date_sub(now(), interval 18 day), '', null,
 'admin', date_sub(now(), interval 20 day), 'admin', now(), '演示数据-维修转退货'),
(9010, 'DEMO-SO-9010', 9002, '演示客户-李娜', '13800009002', 239.00, 'COMPLETED',
 'admin', date_sub(now(), interval 15 day), 'admin', date_sub(now(), interval 13 day), '', null,
 'admin', date_sub(now(), interval 15 day), 'admin', now(), '演示数据-退货关闭'),
(9011, 'DEMO-SO-9011', 9003, '演示客户-王芳', '13800009003', 499.00, 'CONFIRMED',
  'admin', now(), '', null, '', null,
 'admin', date_sub(now(), interval 1 hour), 'admin', now(), '演示数据-今日成交'),
(9012, 'DEMO-SO-9012', 9004, '演示客户-赵磊', '13800009004', 569.00, 'DRAFT',
 '', null, '', null, '', null,
 'admin', now(), 'admin', now(), '演示数据-草稿订单');

insert into biz_sale_order_item
(item_id, order_id, sku_id, product_name, sku_name, sale_price, quantity, refunded_quantity, refunded_amount,
 create_by, create_time, update_by, update_time, remark)
values
(9001, 9001, 9001, '65W 氮化镓充电器', '黑色', 129.00, 2, 1, 129.00, 'admin', date_sub(now(), interval 12 day), 'admin', now(), '已退货退款'),
(9002, 9001, 9004, '20000mAh 移动电源', '黑色', 169.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 12 day), 'admin', now(), '维修完成'),
(9003, 9001, 9007, '主动降噪蓝牙耳机', '黑色', 299.00, 2, 0, 0.00, 'admin', date_sub(now(), interval 12 day), 'admin', now(), '待处理维修'),
(9004, 9002, 9002, '65W 氮化镓充电器', '白色', 129.00, 2, 1, 0.00, 'admin', date_sub(now(), interval 8 day), 'admin', now(), '损坏退货，待退款'),
(9005, 9002, 9010, '便携蓝牙音箱', '灰色', 219.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 8 day), 'admin', now(), '维修已关闭'),
(9006, 9003, 9013, '智能运动手环', '黑色标准版', 199.00, 2, 0, 0.00, 'admin', date_sub(now(), interval 6 day), 'admin', now(), '待处理退货'),
(9007, 9003, 9016, '智能健康手表', '曜石黑', 499.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 6 day), 'admin', now(), '处理中维修'),
(9008, 9004, 9011, '便携蓝牙音箱', '绿色', 219.00, 2, 0, 0.00, 'admin', date_sub(now(), interval 3 hour), 'admin', now(), '今日确认'),
(9009, 9004, 9003, '65W 氮化镓充电器', '套装版', 149.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 3 hour), 'admin', now(), '今日确认'),
(9010, 9005, 9005, '20000mAh 移动电源', '白色', 169.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 2 day), 'admin', now(), '草稿取消'),
(9011, 9006, 9008, '主动降噪蓝牙耳机', '白色', 299.00, 1, 1, 0.00, 'admin', date_sub(now(), interval 5 day), 'admin', now(), '退款已取消'),
(9012, 9006, 9006, '20000mAh 移动电源', '蓝色', 179.00, 2, 0, 0.00, 'admin', date_sub(now(), interval 5 day), 'admin', now(), '维修完成'),
(9013, 9007, 9014, '智能运动手环', '粉色标准版', 199.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 1 day), 'admin', now(), '草稿'),
(9014, 9008, 9009, '主动降噪蓝牙耳机', '蓝色', 319.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 4 day), 'admin', now(), '确认后取消'),
(9015, 9009, 9012, '便携蓝牙音箱', '蓝色', 239.00, 1, 1, 239.00, 'admin', date_sub(now(), interval 20 day), 'admin', now(), '维修转退货并退款'),
(9016, 9010, 9015, '智能运动手环', '黑色NFC版', 239.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 15 day), 'admin', now(), '退货关闭'),
(9017, 9011, 9017, '智能健康手表', '云杉绿', 499.00, 1, 0, 0.00, 'admin', date_sub(now(), interval 1 hour), 'admin', now(), '今日确认'),
(9018, 9012, 9018, '智能健康手表', '银色', 569.00, 1, 0, 0.00, 'admin', now(), 'admin', now(), '草稿');

-- ----------------------------
-- 4、库存流水
-- ----------------------------
-- 初始入库：每个 SKU 一条手工入库流水
insert into biz_stock_record
(record_id, sku_id, record_type, change_direction, quantity, before_stock, after_stock, biz_type, biz_id,
 request_no, create_by, create_time, remark)
values
(9001, 9001, 'IN', 1, 29, 0, 29, 'MANUAL_IN', null, 'DEMO-IN-9001', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9002, 9002, 'IN', 1, 37, 0, 37, 'MANUAL_IN', null, 'DEMO-IN-9002', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9003, 9003, 'IN', 1, 17, 0, 17, 'MANUAL_IN', null, 'DEMO-IN-9003', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9004, 9004, 'IN', 1, 15, 0, 15, 'MANUAL_IN', null, 'DEMO-IN-9004', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9005, 9005, 'IN', 1, 4, 0, 4, 'MANUAL_IN', null, 'DEMO-IN-9005', 'admin', date_sub(now(), interval 30 day), '演示数据-低库存'),
(9006, 9006, 'IN', 1, 22, 0, 22, 'MANUAL_IN', null, 'DEMO-IN-9006', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9007, 9007, 'IN', 1, 28, 0, 28, 'MANUAL_IN', null, 'DEMO-IN-9007', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9008, 9008, 'IN', 1, 22, 0, 22, 'MANUAL_IN', null, 'DEMO-IN-9008', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9009, 9009, 'IN', 1, 10, 0, 10, 'MANUAL_IN', null, 'DEMO-IN-9009', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9010, 9010, 'IN', 1, 18, 0, 18, 'MANUAL_IN', null, 'DEMO-IN-9010', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9011, 9011, 'IN', 1, 20, 0, 20, 'MANUAL_IN', null, 'DEMO-IN-9011', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9012, 9012, 'IN', 1, 11, 0, 11, 'MANUAL_IN', null, 'DEMO-IN-9012', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9013, 9013, 'IN', 1, 35, 0, 35, 'MANUAL_IN', null, 'DEMO-IN-9013', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9014, 9014, 'IN', 1, 21, 0, 21, 'MANUAL_IN', null, 'DEMO-IN-9014', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9015, 9015, 'IN', 1, 10, 0, 10, 'MANUAL_IN', null, 'DEMO-IN-9015', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9016, 9016, 'IN', 1, 13, 0, 13, 'MANUAL_IN', null, 'DEMO-IN-9016', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9017, 9017, 'IN', 1, 9, 0, 9, 'MANUAL_IN', null, 'DEMO-IN-9017', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库'),
(9018, 9018, 'IN', 1, 6, 0, 6, 'MANUAL_IN', null, 'DEMO-IN-9018', 'admin', date_sub(now(), interval 30 day), '演示数据-初始入库');

-- 订单确认、取消恢复和完好退货入库流水
insert into biz_stock_record
(record_id, sku_id, record_type, change_direction, quantity, before_stock, after_stock, biz_type, biz_id,
 request_no, create_by, create_time, remark)
values
(9019, 9001, 'SALE_OUT', -1, 2, 29, 27, 'SALE_OUT', 9001, null, 'admin', date_sub(now(), interval 12 day), '订单确认：DEMO-SO-9001'),
(9020, 9004, 'SALE_OUT', -1, 1, 15, 14, 'SALE_OUT', 9001, null, 'admin', date_sub(now(), interval 12 day), '订单确认：DEMO-SO-9001'),
(9021, 9007, 'SALE_OUT', -1, 2, 28, 26, 'SALE_OUT', 9001, null, 'admin', date_sub(now(), interval 12 day), '订单确认：DEMO-SO-9001'),
(9022, 9001, 'RETURN_IN', 1, 1, 27, 28, 'RETURN', 9001, null, 'admin', date_sub(now(), interval 9 day), '退货入库：DEMO-AS-9001'),
(9023, 9002, 'SALE_OUT', -1, 2, 37, 35, 'SALE_OUT', 9002, null, 'admin', date_sub(now(), interval 8 day), '订单确认：DEMO-SO-9002'),
(9024, 9010, 'SALE_OUT', -1, 1, 18, 17, 'SALE_OUT', 9002, null, 'admin', date_sub(now(), interval 8 day), '订单确认：DEMO-SO-9002'),
(9025, 9013, 'SALE_OUT', -1, 2, 35, 33, 'SALE_OUT', 9003, null, 'admin', date_sub(now(), interval 6 day), '订单确认：DEMO-SO-9003'),
(9026, 9016, 'SALE_OUT', -1, 1, 13, 12, 'SALE_OUT', 9003, null, 'admin', date_sub(now(), interval 6 day), '订单确认：DEMO-SO-9003'),
(9027, 9011, 'SALE_OUT', -1, 2, 20, 18, 'SALE_OUT', 9004, null, 'admin', now(), '订单确认：DEMO-SO-9004'),
(9028, 9003, 'SALE_OUT', -1, 1, 17, 16, 'SALE_OUT', 9004, null, 'admin', now(), '订单确认：DEMO-SO-9004'),
(9029, 9008, 'SALE_OUT', -1, 1, 22, 21, 'SALE_OUT', 9006, null, 'admin', date_sub(now(), interval 5 day), '订单确认：DEMO-SO-9006'),
(9030, 9006, 'SALE_OUT', -1, 2, 22, 20, 'SALE_OUT', 9006, null, 'admin', date_sub(now(), interval 5 day), '订单确认：DEMO-SO-9006'),
(9031, 9008, 'RETURN_IN', 1, 1, 21, 22, 'RETURN', 9008, null, 'admin', date_sub(now(), interval 3 day), '退货入库：DEMO-AS-9008'),
(9032, 9009, 'SALE_OUT', -1, 1, 10, 9, 'SALE_OUT', 9008, null, 'admin', date_sub(now(), interval 4 day), '订单确认：DEMO-SO-9008'),
(9033, 9009, 'CANCEL_IN', 1, 1, 9, 10, 'CANCEL_IN', 9008, null, 'admin', date_sub(now(), interval 3 day), '订单取消：DEMO-SO-9008'),
(9034, 9012, 'SALE_OUT', -1, 1, 11, 10, 'SALE_OUT', 9009, null, 'admin', date_sub(now(), interval 20 day), '订单确认：DEMO-SO-9009'),
(9035, 9012, 'RETURN_IN', 1, 1, 10, 11, 'RETURN', 9011, null, 'admin', date_sub(now(), interval 16 day), '退货入库：DEMO-AS-9011'),
(9036, 9015, 'SALE_OUT', -1, 1, 10, 9, 'SALE_OUT', 9010, null, 'admin', date_sub(now(), interval 15 day), '订单确认：DEMO-SO-9010'),
(9037, 9017, 'SALE_OUT', -1, 1, 9, 8, 'SALE_OUT', 9011, null, 'admin', now(), '订单确认：DEMO-SO-9011');

-- ----------------------------
-- 5、售后工单
-- ----------------------------
insert into biz_after_sale_ticket
(ticket_id, ticket_no, ticket_type, order_item_id, source_ticket_id, validity_exempt_reason, apply_quantity,
 received_quantity, handling_result, status, handler_id, fault_desc, handle_result, image_urls,
 create_by, create_time, update_by, update_time, remark)
values
(9001, 'DEMO-AS-9001', 'RETURN', 9001, null, null, 1, 1, 'GOOD', 'COMPLETED', 1,
 '充电口接触不良，申请退货', '实收1件，检查完好，同意退货，已退款', null,
 'admin', date_sub(now(), interval 11 day), 'admin', date_sub(now(), interval 9 day), '演示数据-已退款退货'),
(9002, 'DEMO-AS-9002', 'REPAIR', 9002, null, null, 1, null, null, 'COMPLETED', 1,
 '移动电源无法充电', '已更换充电接口并完成测试', null,
 'admin', date_sub(now(), interval 10 day), 'admin', date_sub(now(), interval 8 day), '演示数据-维修完成'),
(9003, 'DEMO-AS-9003', 'REPAIR', 9003, null, null, 1, null, null, 'PENDING', null,
 '右耳偶发断连，申请检测维修', null, null,
 'admin', date_sub(now(), interval 9 day), '', null, '演示数据-待分配维修'),
(9004, 'DEMO-AS-9004', 'RETURN', 9004, null, null, 1, 1, 'DAMAGED', 'COMPLETED', 1,
 '外壳破损，申请退货', '实收1件，检查为损坏，不进入可售库存，退款待审核', null,
 'admin', date_sub(now(), interval 6 day), 'admin', date_sub(now(), interval 5 day), '演示数据-损坏退货'),
(9005, 'DEMO-AS-9005', 'REPAIR', 9005, null, null, 1, null, null, 'CLOSED', 1,
 '蓝牙连接不稳定', '客户撤回维修申请，工单关闭', null,
 'admin', date_sub(now(), interval 6 day), 'admin', date_sub(now(), interval 4 day), '演示数据-维修关闭'),
(9006, 'DEMO-AS-9006', 'RETURN', 9006, null, null, 2, null, null, 'PENDING', null,
 '手环屏幕显示异常，申请退货', null, null,
 'admin', date_sub(now(), interval 5 day), '', null, '演示数据-待处理退货'),
(9007, 'DEMO-AS-9007', 'REPAIR', 9007, null, null, 1, null, null, 'PROCESSING', 1,
 '手表无法开机', '已拆机检测，等待主板配件', null,
 'admin', date_sub(now(), interval 5 day), 'admin', date_sub(now(), interval 2 day), '演示数据-处理中维修'),
(9008, 'DEMO-AS-9008', 'RETURN', 9011, null, null, 1, 1, 'GOOD', 'COMPLETED', 1,
 '耳机降噪异常，申请退货', '实收1件，检查完好，客户撤回退款', null,
 'admin', date_sub(now(), interval 4 day), 'admin', date_sub(now(), interval 3 day), '演示数据-退款取消'),
(9009, 'DEMO-AS-9009', 'REPAIR', 9012, null, null, 1, null, null, 'COMPLETED', 1,
 '移动电源续航异常', '更换电池并完成放电测试', null,
 'admin', date_sub(now(), interval 5 day), 'admin', date_sub(now(), interval 3 day), '演示数据-维修完成'),
(9010, 'DEMO-AS-9010', 'REPAIR', 9015, null, null, 1, null, null, 'COMPLETED', 1,
 '音箱蓝牙模块故障', '检测后建议转退货，已登记转退货', null,
 'admin', date_sub(now(), interval 18 day), 'admin', date_sub(now(), interval 17 day), '演示数据-维修转退货'),
(9011, 'DEMO-AS-9011', 'RETURN', 9015, 9010, null, 1, 1, 'GOOD', 'COMPLETED', 1,
 '来源维修工单转退货', '实收1件，检查完好，已退款', null,
 'admin', date_sub(now(), interval 17 day), 'admin', date_sub(now(), interval 16 day), '演示数据-维修转退货完成'),
(9012, 'DEMO-AS-9012', 'RETURN', 9016, null, null, 1, null, null, 'CLOSED', 1,
 'NFC 功能偶发失效', '客户撤销退货申请，工单关闭', null,
 'admin', date_sub(now(), interval 13 day), 'admin', date_sub(now(), interval 12 day), '演示数据-退货关闭');

-- ----------------------------
-- 6、售后处理日志
-- ----------------------------
insert into biz_after_sale_log
(log_id, ticket_id, operate_type, operate_content, attachment_urls, create_by, create_time, update_by, update_time)
values
(9001, 9001, 'CREATE', '创建退货工单：65W 氮化镓充电器 / 黑色，数量：1', null, 'admin', date_sub(now(), interval 11 day), '', null),
(9002, 9001, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 10 day), '', null),
(9003, 9001, 'RETURN_CHECK', '退货检查完成，实收数量：1，商品处理结论：完好', null, 'admin', date_sub(now(), interval 9 day), '', null),
(9004, 9001, 'COMPLETE', '退货完成', null, 'admin', date_sub(now(), interval 9 day), '', null),
(9005, 9002, 'CREATE', '创建维修工单：20000mAh 移动电源 / 黑色，数量：1', null, 'admin', date_sub(now(), interval 10 day), '', null),
(9006, 9002, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 9 day), '', null),
(9007, 9002, 'HANDLE', '已更换充电接口并完成测试', null, 'admin', date_sub(now(), interval 8 day), '', null),
(9008, 9002, 'COMPLETE', '维修完成', null, 'admin', date_sub(now(), interval 8 day), '', null),
(9009, 9003, 'CREATE', '创建维修工单：主动降噪蓝牙耳机 / 黑色，数量：1', null, 'admin', date_sub(now(), interval 9 day), '', null),
(9010, 9004, 'CREATE', '创建退货工单：65W 氮化镓充电器 / 白色，数量：1', null, 'admin', date_sub(now(), interval 6 day), '', null),
(9011, 9004, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 6 day), '', null),
(9012, 9004, 'RETURN_CHECK', '退货检查完成，实收数量：1，商品处理结论：损坏', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9013, 9004, 'COMPLETE', '退货完成，损坏商品不进入可售库存', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9014, 9005, 'CREATE', '创建维修工单：便携蓝牙音箱 / 灰色，数量：1', null, 'admin', date_sub(now(), interval 6 day), '', null),
(9015, 9005, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 6 day), '', null),
(9016, 9005, 'CLOSE', '客户撤回维修申请，工单关闭', null, 'admin', date_sub(now(), interval 4 day), '', null),
(9017, 9006, 'CREATE', '创建退货工单：智能运动手环 / 黑色标准版，数量：2', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9018, 9007, 'CREATE', '创建维修工单：智能健康手表 / 曜石黑，数量：1', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9019, 9007, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 4 day), '', null),
(9020, 9007, 'HANDLE', '已拆机检测，等待主板配件', null, 'admin', date_sub(now(), interval 2 day), '', null),
(9021, 9008, 'CREATE', '创建退货工单：主动降噪蓝牙耳机 / 白色，数量：1', null, 'admin', date_sub(now(), interval 4 day), '', null),
(9022, 9008, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 4 day), '', null),
(9023, 9008, 'RETURN_CHECK', '退货检查完成，实收数量：1，商品处理结论：完好', null, 'admin', date_sub(now(), interval 3 day), '', null),
(9024, 9008, 'COMPLETE', '退货完成，退款取消', null, 'admin', date_sub(now(), interval 3 day), '', null),
(9025, 9009, 'CREATE', '创建维修工单：20000mAh 移动电源 / 蓝色，数量：1', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9026, 9009, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 5 day), '', null),
(9027, 9009, 'HANDLE', '更换电池并完成放电测试', null, 'admin', date_sub(now(), interval 4 day), '', null),
(9028, 9009, 'COMPLETE', '维修完成', null, 'admin', date_sub(now(), interval 3 day), '', null),
(9029, 9010, 'CREATE', '创建维修工单：便携蓝牙音箱 / 蓝色，数量：1', null, 'admin', date_sub(now(), interval 18 day), '', null),
(9030, 9010, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 18 day), '', null),
(9031, 9010, 'HANDLE', '检测后建议转退货', null, 'admin', date_sub(now(), interval 17 day), '', null),
(9032, 9010, 'COMPLETE', '维修完成，处理结果为转退货', null, 'admin', date_sub(now(), interval 17 day), '', null),
(9033, 9011, 'CREATE', '创建退货工单：便携蓝牙音箱 / 蓝色，来源维修工单 DEMO-AS-9010，数量：1', null, 'admin', date_sub(now(), interval 17 day), '', null),
(9034, 9011, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 17 day), '', null),
(9035, 9011, 'RETURN_CHECK', '退货检查完成，实收数量：1，商品处理结论：完好', null, 'admin', date_sub(now(), interval 16 day), '', null),
(9036, 9011, 'COMPLETE', '退货完成并退款', null, 'admin', date_sub(now(), interval 16 day), '', null),
(9037, 9012, 'CREATE', '创建退货工单：智能运动手环 / 黑色NFC版，数量：1', null, 'admin', date_sub(now(), interval 13 day), '', null),
(9038, 9012, 'ASSIGN', '分配处理人：admin', null, 'admin', date_sub(now(), interval 13 day), '', null),
(9039, 9012, 'CLOSE', '客户撤销退货申请，工单关闭', null, 'admin', date_sub(now(), interval 12 day), '', null);

-- ----------------------------
-- 7、退款记录
-- ----------------------------
insert into biz_refund_record
(refund_id, ticket_id, refund_amount, status, handler_id, handle_time, create_by, create_time, update_by, update_time, remark)
values
(9001, 9001, 129.00, 'REFUNDED', 1, now(), 'admin', date_sub(now(), interval 9 day), 'admin', now(), '完好退货，已退款，计入今日退款'),
(9002, 9004, 0.00, 'PENDING', null, null, 'admin', date_sub(now(), interval 5 day), '', null, '损坏退货，退款待审核'),
(9003, 9008, 0.00, 'CANCELLED', 1, date_sub(now(), interval 3 day), 'admin', date_sub(now(), interval 3 day), 'admin', date_sub(now(), interval 3 day), '客户撤回退款申请'),
(9004, 9011, 239.00, 'REFUNDED', 1, date_sub(now(), interval 16 day), 'admin', date_sub(now(), interval 16 day), 'admin', date_sub(now(), interval 16 day), '维修转退货，已退款');
set names utf8mb4;

