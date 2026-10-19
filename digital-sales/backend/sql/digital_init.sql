-- ----------------------------
-- 数码产品销售与售后运营系统基础初始化
-- 依赖：先执行 dop_20260417.sql，再执行 digital_schema.sql
-- 说明：重复执行会覆盖本脚本管理的菜单、角色、字典和参数
-- ----------------------------

delete from sys_role_menu where menu_id between 2000 and 2199;
delete from sys_menu where menu_id between 2000 and 2199;

-- ----------------------------
-- 1、业务菜单
-- ----------------------------
insert into sys_menu values('2000', '数码业务', '0',   '4', 'digital',      null,              '', '', 1, 0, 'M', '0', '0', '',                         'shopping',  'admin', sysdate(), '', null, '数码业务目录');

insert into sys_menu values('2010', '商品分类', '2000', '1', 'category',     'digital/category/index',   '', 'DigitalCategory', 1, 0, 'C', '0', '0', 'digital:category:list',    'tree-table', 'admin', sysdate(), '', null, '商品分类菜单');
insert into sys_menu values('2020', '商品管理', '2000', '2', 'product',      'digital/product/index',    '', 'DigitalProduct',  1, 0, 'C', '0', '0', 'digital:product:list',     'shopping',   'admin', sysdate(), '', null, '商品管理菜单');
insert into sys_menu values('2030', '商品规格', '2000', '3', 'sku',          'digital/sku/index',        '', 'DigitalSku',      1, 0, 'C', '0', '0', 'digital:sku:list',         'table',      'admin', sysdate(), '', null, '商品规格菜单');
insert into sys_menu values('2040', '库存管理', '2000', '4', 'stock',        'digital/stock/index',      '', 'DigitalStock',    1, 0, 'C', '0', '0', 'digital:stock:list',       'chart',      'admin', sysdate(), '', null, '库存管理菜单');

-- 库存预警任务：每5分钟扫描一次，禁止并发执行
insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '数码业务低库存预警', 'DIGITAL', 'digitalLowStockAlertService.scan()', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '阶段五库存预警任务'
where not exists (select 1 from sys_job where invoke_target = 'digitalLowStockAlertService.scan()');
insert into sys_menu values('2050', '客户管理', '2000', '5', 'customer',     'digital/customer/index',   '', 'DigitalCustomer', 1, 0, 'C', '0', '0', 'digital:customer:list',    'people',     'admin', sysdate(), '', null, '客户管理菜单');
insert into sys_menu values('2060', '订单管理', '2000', '6', 'order',        'digital/order/index',      '', 'DigitalOrder',    1, 0, 'C', '0', '0', 'digital:order:list',       'shopping',   'admin', sysdate(), '', null, '订单管理菜单');
insert into sys_menu values('2070', '售后管理', '2000', '7', 'aftersale',    'digital/aftersale/index',   '', 'DigitalAfterSale',1, 0, 'C', '0', '0', 'digital:aftersale:list',   'tool',       'admin', sysdate(), '', null, '售后管理菜单');
insert into sys_menu values('2080', '退款管理', '2000', '8', 'refund',       'digital/refund/index', '', 'DigitalRefund',   1, 0, 'C', '0', '0', 'digital:refund:list',      'money',      'admin', sysdate(), '', null, '退款管理菜单');

-- 首页看板权限（默认首页复用，不单独显示菜单）
insert into sys_menu values('2090', '首页看板', '2000', '9', '', '', '', '', 1, 0, 'F', '1', '0', 'digital:dashboard:list', '#', 'admin', sysdate(), '', null, '');

-- 商品分类按钮
insert into sys_menu values('2100', '分类查询', '2010', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:category:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2101', '分类新增', '2010', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:category:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2102', '分类修改', '2010', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:category:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2103', '分类删除', '2010', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:category:remove', '#', 'admin', sysdate(), '', null, '');

-- 商品管理按钮
insert into sys_menu values('2110', '商品查询', '2020', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:product:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2111', '商品新增', '2020', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:product:add',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2112', '商品修改', '2020', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:product:edit',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2113', '商品删除', '2020', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:product:remove',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2114', '商品导出', '2020', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:product:export',  '#', 'admin', sysdate(), '', null, '');

-- 商品规格按钮
insert into sys_menu values('2120', '规格查询', '2030', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:sku:query',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2121', '规格新增', '2030', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:sku:add',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2122', '规格修改', '2030', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:sku:edit',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2123', '规格删除', '2030', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:sku:remove',      '#', 'admin', sysdate(), '', null, '');

-- 库存管理按钮
insert into sys_menu values('2130', '库存查询', '2040', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:stock:query',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2131', '库存调整', '2040', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:stock:in',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2132', '库存流水', '2040', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:stock:record',    '#', 'admin', sysdate(), '', null, '');

-- 客户管理按钮
insert into sys_menu values('2140', '客户查询', '2050', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:customer:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2141', '客户新增', '2050', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:customer:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2142', '客户修改', '2050', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:customer:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2143', '客户删除', '2050', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:customer:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2144', '客户导出', '2050', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:customer:export', '#', 'admin', sysdate(), '', null, '');

-- 订单管理按钮
insert into sys_menu values('2150', '订单查询', '2060', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:query',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2151', '订单新增', '2060', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:add',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2152', '订单修改', '2060', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:edit',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2153', '订单确认', '2060', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:confirm',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2154', '订单取消', '2060', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:cancel',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2155', '订单完成', '2060', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:complete',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2156', '订单删除', '2060', '7', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:remove',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2157', '订单导出', '2060', '8', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:order:export',    '#', 'admin', sysdate(), '', null, '');

-- 售后管理按钮
insert into sys_menu values('2160', '售后查询', '2070', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:query',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2161', '售后新增', '2070', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:add',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2162', '售后分配', '2070', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:assign',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2163', '售后处理', '2070', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:handle',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2164', '售后完成', '2070', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:complete', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2165', '售后关闭', '2070', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:aftersale:close',    '#', 'admin', sysdate(), '', null, '');

-- 退款管理按钮
insert into sys_menu values('2170', '退款查询', '2080', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:refund:query',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2171', '退款审核', '2080', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:refund:audit',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2172', '退款修改', '2080', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'digital:refund:edit',       '#', 'admin', sysdate(), '', null, '');

-- ----------------------------
-- 2、演示角色
-- ----------------------------
delete from sys_role_menu where role_id between 100 and 103;
delete from sys_role where role_id between 100 and 103;

insert into sys_role values('100', '销售人员',     'digital_sales',       10, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '数码业务演示角色：客户与订单');
insert into sys_role values('101', '仓库人员',     'digital_warehouse',   11, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '数码业务演示角色：商品、库存与订单确认');
insert into sys_role values('102', '售后人员',     'digital_after_sales', 12, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '数码业务演示角色：售后工单处理');
insert into sys_role values('103', '退款审核人员', 'digital_refund',      13, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '数码业务演示角色：退款审核');

-- 销售人员：客户、订单办理和订单完成；订单录入需要读取商品规格
insert into sys_role_menu values
('100', '2000'),
('100', '2090'),
('100', '2095'),
('100', '2030'),
('100', '2050'), ('100', '2140'), ('100', '2141'), ('100', '2142'), ('100', '2143'), ('100', '2144'),
('100', '2060'), ('100', '2150'), ('100', '2151'), ('100', '2152'), ('100', '2155'), ('100', '2156'), ('100', '2157');

-- 仓库人员：商品、规格、库存、订单确认与取消
insert into sys_role_menu values
('101', '2000'),
('101', '2090'),
('101', '2095'),
('101', '2020'), ('101', '2110'), ('101', '2111'), ('101', '2112'), ('101', '2113'), ('101', '2114'),
('101', '2030'), ('101', '2120'), ('101', '2121'), ('101', '2122'), ('101', '2123'),
('101', '2040'), ('101', '2130'), ('101', '2131'), ('101', '2132'),
('101', '2060'), ('101', '2150'), ('101', '2153'), ('101', '2154');

-- 售后人员：售后工单处理和订单查询
insert into sys_role_menu values
('102', '2000'),
('102', '2090'),
('102', '2095'),
('102', '2070'), ('102', '2160'), ('102', '2161'), ('102', '2162'), ('102', '2163'), ('102', '2164'), ('102', '2165'),
('102', '2060'), ('102', '2150');

-- 退款审核人员：退款办理和售后查询
insert into sys_role_menu values
('103', '2000'),
('103', '2090'),
('103', '2095'),
('103', '2080'), ('103', '2170'), ('103', '2171'), ('103', '2172'),
('103', '2070'), ('103', '2160');

-- ----------------------------
-- 3、业务字典
-- ----------------------------
delete from sys_dict_data where dict_type in
('digital_order_status', 'digital_after_sale_type', 'digital_after_sale_status',
 'digital_stock_record_type', 'digital_refund_status', 'digital_after_sale_handling_result');
delete from sys_dict_type where dict_type in
('digital_order_status', 'digital_after_sale_type', 'digital_after_sale_status',
 'digital_stock_record_type', 'digital_refund_status', 'digital_after_sale_handling_result');

insert into sys_dict_type values
(100, '订单状态',         'digital_order_status',                '0', 'admin', sysdate(), '', null, '数码业务订单状态'),
(101, '售后类型',         'digital_after_sale_type',            '0', 'admin', sysdate(), '', null, '数码业务售后工单类型'),
(102, '售后状态',         'digital_after_sale_status',          '0', 'admin', sysdate(), '', null, '数码业务售后工单状态'),
(103, '库存流水类型',     'digital_stock_record_type',          '0', 'admin', sysdate(), '', null, '数码业务库存流水类型'),
(104, '退款状态',         'digital_refund_status',              '0', 'admin', sysdate(), '', null, '数码业务退款状态'),
(105, '退货商品处理结论', 'digital_after_sale_handling_result', '0', 'admin', sysdate(), '', null, '数码业务退货商品处理结论');

insert into sys_dict_data values
(1000, 1, '草稿',   'DRAFT',     'digital_order_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '订单草稿'),
(1001, 2, '已确认', 'CONFIRMED', 'digital_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '订单已确认'),
(1002, 3, '已完成', 'COMPLETED', 'digital_order_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '订单已完成'),
(1003, 4, '已取消', 'CANCELLED', 'digital_order_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '订单已取消'),

(1010, 1, '退货', 'RETURN', 'digital_after_sale_type', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '退货工单'),
(1011, 2, '维修', 'REPAIR', 'digital_after_sale_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '维修工单'),

(1020, 1, '待处理', 'PENDING',    'digital_after_sale_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '待处理'),
(1021, 2, '处理中', 'PROCESSING', 'digital_after_sale_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '处理中'),
(1022, 3, '已完成', 'COMPLETED',  'digital_after_sale_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已完成'),
(1023, 4, '已关闭', 'CLOSED',     'digital_after_sale_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已关闭'),

(1030, 1, '手工入库', 'IN',         'digital_stock_record_type', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '手工入库'),
(1034, 5, '手工出库', 'OUT',        'digital_stock_record_type', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '手工减少库存'),
(1031, 2, '销售出库', 'SALE_OUT',   'digital_stock_record_type', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '销售出库'),
(1032, 3, '取消恢复', 'CANCEL_IN',  'digital_stock_record_type', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '取消恢复'),
(1033, 4, '退货入库', 'RETURN_IN',  'digital_stock_record_type', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '退货入库'),

(1040, 1, '待退款', 'PENDING',  'digital_refund_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '待退款'),
(1041, 2, '已退款', 'REFUNDED', 'digital_refund_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已退款'),
(1042, 3, '已取消', 'CANCELLED','digital_refund_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已取消'),

(1050, 1, '完好', 'GOOD',    'digital_after_sale_handling_result', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '完好可入库'),
(1051, 2, '损坏', 'DAMAGED', 'digital_after_sale_handling_result', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '损坏不入库');

-- ----------------------------
-- 4、业务参数
-- ----------------------------
delete from sys_config where config_key = 'digital.afterSale.validDays';
insert into sys_config values
(100, '数码业务-售后时效天数', 'digital.afterSale.validDays', '30', 'Y', 'admin', sysdate(), '', null, '订单完成后允许创建售后工单的天数');
set names utf8mb4;

