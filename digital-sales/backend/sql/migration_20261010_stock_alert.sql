-- 阶段五：Quartz 低库存预警增量迁移
create table if not exists biz_stock_alert (
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

insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '数码业务低库存预警', 'DIGITAL', 'digitalLowStockAlertService.scan()', '0 0/5 * * * ?', '3', '1', '0', 'admin', sysdate(), '阶段五库存预警任务'
where not exists (select 1 from sys_job where invoke_target = 'digitalLowStockAlertService.scan()');
