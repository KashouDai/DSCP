-- ----------------------------
-- 数码产品销售与售后运营系统业务表
-- 注意：本脚本会删除并重建业务表，仅用于开发环境结构初始化
-- ----------------------------

drop table if exists biz_refund_record;
drop table if exists biz_after_sale_log;
drop table if exists biz_after_sale_ticket;
drop table if exists biz_stock_record;
drop table if exists biz_sale_order_item;
drop table if exists biz_sale_order;
drop table if exists biz_customer;
drop table if exists biz_product_sku;
drop table if exists biz_product;
drop table if exists biz_product_category;
drop table if exists biz_no_seq;

-- ----------------------------
-- 1、商品分类表
-- ----------------------------
create table biz_product_category (
  category_id       bigint(20)      not null auto_increment    comment '分类ID',
  category_name     varchar(64)     not null                   comment '分类名称',
  order_num         int(4)          not null default 0         comment '显示顺序',
  status            char(1)         not null default '0'       comment '状态（0正常 1停用）',
  del_flag          char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (category_id),
  key idx_biz_category_status (status, del_flag)
) engine=innodb auto_increment=100 comment = '商品分类表';

-- ----------------------------
-- 2、商品表
-- ----------------------------
create table biz_product (
  product_id        bigint(20)      not null auto_increment    comment '商品ID',
  category_id       bigint(20)      not null                   comment '分类ID',
  product_name      varchar(100)    not null                   comment '商品名称',
  brand             varchar(64)     default ''                 comment '品牌',
  description       varchar(500)    default null               comment '商品描述',
  status            char(1)         not null default '0'       comment '状态（0上架 1下架）',
  del_flag          char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (product_id),
  key idx_biz_product_category (category_id),
  key idx_biz_product_status (status, del_flag)
) engine=innodb auto_increment=100 comment = '商品表';

-- ----------------------------
-- 3、商品规格表
-- ----------------------------
create table biz_product_sku (
  sku_id            bigint(20)      not null auto_increment    comment '规格ID',
  product_id        bigint(20)      not null                   comment '商品ID',
  sku_code          varchar(64)     not null                   comment '规格编码',
  sku_name          varchar(100)    not null                   comment '规格名称',
  sale_price        decimal(12,2) unsigned not null default 0  comment '销售价',
  cost_price        decimal(12,2) unsigned not null default 0  comment '成本价',
  barcode           varchar(64)     default null               comment '条码',
  stock             int(11) unsigned not null default 0        comment '当前可售库存',
  warn_stock        int(11) unsigned not null default 0        comment '库存预警值',
  status            char(1)         not null default '0'       comment '状态（0上架 1下架）',
  del_flag          char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (sku_id),
  unique key uk_biz_product_sku_code (sku_code),
  unique key uk_biz_product_sku_barcode (barcode),
  key idx_biz_product_sku_product (product_id),
  key idx_biz_product_sku_stock (stock, status, del_flag)
) engine=innodb auto_increment=100 comment = '商品规格表';

-- ----------------------------
-- 4、库存预警表
-- ----------------------------
create table biz_stock_alert (
  alert_id          bigint(20)      not null auto_increment    comment '预警ID',
  sku_id            bigint(20)      not null                   comment '规格ID',
  sku_code          varchar(64)     not null                   comment '规格编码',
  sku_name          varchar(100)    not null                   comment '规格名称',
  stock             int(11)         not null                   comment '触发时库存',
  warn_stock        int(11)         not null                   comment '预警值',
  status            char(1)         not null default '0'        comment '状态（0活跃 1已恢复）',
  triggered_time    datetime        not null                   comment '触发时间',
  resolved_time     datetime        default null               comment '恢复时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default null               comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default null               comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (alert_id),
  key idx_biz_stock_alert_sku_status (sku_id, status),
  key idx_biz_stock_alert_status_time (status, triggered_time)
) engine=innodb auto_increment=100 comment = '库存预警表';

-- ----------------------------
-- 4、客户表
-- ----------------------------
create table biz_customer (
  customer_id       bigint(20)      not null auto_increment    comment '客户ID',
  customer_name     varchar(100)    not null                   comment '客户名称',
  phone             varchar(30)     default ''                 comment '联系电话',
  email             varchar(100)    default ''                 comment '邮箱',
  address           varchar(255)    default ''                 comment '地址',
  status            char(1)         not null default '0'       comment '状态（0正常 1停用）',
  del_flag          char(1)         not null default '0'       comment '删除标志（0存在 2删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (customer_id),
  key idx_biz_customer_status (status, del_flag)
) engine=innodb auto_increment=100 comment = '客户表';

-- ----------------------------
-- 5、销售订单表
-- ----------------------------
create table biz_sale_order (
  order_id          bigint(20)      not null auto_increment    comment '订单ID',
  order_no          varchar(32)     not null                   comment '订单编号',
  customer_id       bigint(20)      not null                   comment '客户ID',
  request_no        varchar(64)     default null               comment '订单创建幂等请求号',
  request_hash      char(64)        default null               comment '订单创建请求摘要',
  customer_name     varchar(100)    not null                   comment '客户名称快照',
  customer_phone    varchar(30)     default ''                 comment '客户联系方式快照',
  total_amount      decimal(12,2) unsigned not null default 0  comment '订单总金额',
  status            varchar(20)     not null default 'DRAFT'   comment '订单状态',
  confirm_by        varchar(64)     default ''                 comment '确认人',
  confirm_time      datetime                                   comment '确认时间',
  complete_by       varchar(64)     default ''                 comment '完成人',
  complete_time     datetime                                   comment '完成时间',
  cancel_by         varchar(64)     default ''                 comment '取消人',
  cancel_time       datetime                                   comment '取消时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (order_id),
  unique key uk_biz_sale_order_no (order_no),
  unique key uk_biz_sale_order_request_no (request_no),
  key idx_biz_sale_order_status (status, confirm_time),
  key idx_biz_sale_order_status_order (status, order_id),
  key idx_biz_sale_order_create_time (create_time),
  key idx_biz_sale_order_customer (customer_id, create_time)
) engine=innodb auto_increment=100 comment = '销售订单表';

-- ----------------------------
-- 6、销售订单明细表
-- ----------------------------
create table biz_sale_order_item (
  item_id             bigint(20)      not null auto_increment    comment '订单明细ID',
  order_id            bigint(20)      not null                   comment '订单ID',
  sku_id              bigint(20)      not null                   comment '规格ID',
  product_name        varchar(100)    not null                   comment '商品名称快照',
  sku_name            varchar(100)    not null                   comment '规格名称快照',
  sale_price          decimal(12,2) unsigned not null default 0  comment '成交单价',
  quantity            int(11) unsigned not null default 1        comment '购买数量',
  refunded_quantity   int(11) unsigned not null default 0        comment '已退数量',
  refunded_amount     decimal(12,2) unsigned not null default 0  comment '已退金额',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime                                   comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime                                   comment '更新时间',
  remark              varchar(500)    default null               comment '备注',
  primary key (item_id),
  unique key uk_biz_order_item_sku (order_id, sku_id),
  key idx_biz_order_item_sku (sku_id)
) engine=innodb auto_increment=100 comment = '销售订单明细表';

-- ----------------------------
-- 7、库存流水表
-- ----------------------------
create table biz_stock_record (
  record_id         bigint(20)      not null auto_increment    comment '流水ID',
  sku_id            bigint(20)      not null                   comment '规格ID',
  record_type       varchar(20)     not null                   comment '流水类型',
  change_direction  tinyint(1)      not null                   comment '出入库方向（1入库 -1出库）',
  quantity          int(11) unsigned not null                   comment '变更数量',
  before_stock      int(11) unsigned not null                   comment '变更前库存',
  after_stock       int(11) unsigned not null                   comment '变更后库存',
  biz_type          varchar(30)     default ''                 comment '业务类型',
  biz_id            bigint(20)      default null               comment '业务主键',
  request_no        varchar(64)     default null               comment '手工入库幂等键',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (record_id),
  unique key uk_biz_stock_record_request_no (request_no),
  key idx_biz_stock_record_sku (sku_id, create_time),
  key idx_biz_stock_record_sku_type_order (sku_id, record_type, record_id),
  key idx_biz_stock_record_biz (biz_type, biz_id)
) engine=innodb auto_increment=100 comment = '库存流水表';

-- ----------------------------
-- 8、售后工单表
-- ----------------------------
create table biz_after_sale_ticket (
  ticket_id              bigint(20)      not null auto_increment    comment '工单ID',
  ticket_no              varchar(32)     not null                   comment '工单编号',
  ticket_type            varchar(20)     not null                   comment '工单类型（RETURN退货 REPAIR维修）',
  order_item_id          bigint(20)      not null                   comment '订单明细ID',
  source_ticket_id       bigint(20)      default null               comment '来源维修工单ID',
  validity_exempt_reason varchar(500)    default null               comment '售后时效豁免原因',
  apply_quantity         int(11) unsigned not null                  comment '申请数量',
  received_quantity      int(11) unsigned default null              comment '实收数量（仅退货）',
  handling_result        varchar(20)     default null               comment '商品处理结论（GOOD完好 DAMAGED损坏）',
  status                 varchar(20)     not null default 'PENDING' comment '工单状态',
  handler_id             bigint(20)      default null               comment '处理人ID',
  fault_desc             varchar(1000)   default null               comment '故障描述',
  handle_result          varchar(1000)   default null               comment '处理结果',
  image_urls             varchar(2000)   default null               comment '图片附件',
  create_by              varchar(64)     default ''                 comment '创建者',
  create_time            datetime                                   comment '创建时间',
  update_by              varchar(64)     default ''                 comment '更新者',
  update_time            datetime                                   comment '更新时间',
  remark                 varchar(500)    default null               comment '备注',
  primary key (ticket_id),
  unique key uk_biz_after_sale_ticket_no (ticket_no),
  key idx_biz_after_sale_status (status, ticket_type, create_time),
  key idx_biz_after_sale_order_item (order_item_id, status),
  key idx_biz_after_sale_source (source_ticket_id)
) engine=innodb auto_increment=100 comment = '售后工单表';

-- ----------------------------
-- 9、售后处理记录表
-- ----------------------------
create table biz_after_sale_log (
  log_id            bigint(20)      not null auto_increment    comment '记录ID',
  ticket_id         bigint(20)      not null                   comment '工单ID',
  operate_type      varchar(20)     not null                   comment '操作类型',
  operate_content   varchar(1000)   default null               comment '操作内容',
  attachment_urls   varchar(2000)   default null               comment '附件',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (log_id),
  key idx_biz_after_sale_log_ticket (ticket_id, create_time)
) engine=innodb auto_increment=100 comment = '售后处理记录表';

-- ----------------------------
-- 10、退款记录表
-- ----------------------------
create table biz_refund_record (
  refund_id         bigint(20)      not null auto_increment    comment '退款ID',
  ticket_id         bigint(20)      not null                   comment '售后工单ID',
  refund_amount     decimal(12,2) unsigned not null             comment '退款金额',
  status            varchar(20)     not null default 'PENDING' comment '退款状态',
  handler_id        bigint(20)      default null               comment '处理人ID',
  handle_time       datetime                                   comment '处理时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (refund_id),
  unique key uk_biz_refund_record_ticket (ticket_id),
  key idx_biz_refund_record_status (status, handle_time)
) engine=innodb auto_increment=100 comment = '退款记录表';

-- ----------------------------
-- 11、业务编号流水表
-- ----------------------------
create table biz_no_seq (
  seq_type          varchar(10)     not null                   comment '编号类型（SO/AS/RF）',
  seq_date          date            not null                   comment '编号日期',
  seq_value         int(11)         not null default 0         comment '当日流水值',
  primary key (seq_type, seq_date)
) engine=innodb comment = '业务编号流水表';
set names utf8mb4;

