# Digital Sales Operation Platform — 数码销售运营平台

> 面向数码产品销售场景的运营管理平台，覆盖商品、订单、库存、售后全链路，支持 Docker Compose 一键部署。

---

## 项目简介

这是一个面向数码产品销售场景的运营管理平台（Digital Operation Platform），在订单、库存、售后等核心模块做了深度扩展。系统提供完整的前后端分离架构，可通过一条 `docker compose up` 命令在本地启动全部服务。

### 主要亮点

- **订单幂等**：通过唯一订单号生成 + 数据库唯一索引双重保障，防止网络重试导致重复下单。
- **库存并发控制**：采用乐观锁（`@Version`）+ CAS 更新实现高并发下的库存扣减安全。
- **低库存预警**：基于 Quartz 定时任务扫描 SKU 库存，触发阈值告警并持久化预警记录。
- **Docker Compose 一键启动**：MySQL、Redis、后端、前端四个服务，本地环境开箱即用。

---

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3.5 + Element Plus 2.13 + Vite 6 + Pinia + Axios + ECharts |
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT |
| 数据 | MySQL 8.0 + Redis 7 |
| 任务调度 | Quartz 定时任务 |
| 构建 | Maven（多模块） + npm + Docker Compose |
| API 文档 | SpringDoc OpenAPI（Swagger UI） |

---

## 功能模块

### 业务模块（dop-digital）

| 模块 | 说明 |
|---|---|
| 商品管理 | 商品 CRUD、分类管理、SKU 规格管理 |
| 订单管理 | 下单、订单列表、订单详情、订单状态流转；幂等写入防重 |
| 库存管理 | 入库单管理、库存明细记录、实时库存查询 |
| 售后管理 | 售后工单创建与处理、售后日志追踪 |
| 退款管理 | 退款申请审核、退款记录 |
| 数据看板 | 销售概览、核心指标 ECharts 图表展示 |
| 客户管理 | 客户信息维护 |

### 系统模块

- 用户 / 角色 / 菜单 / 部门 / 岗位管理
- 字典 & 配置管理
- 操作日志 & 登录日志
- 在线用户监控
- 缓存监控 & 服务监控
- 代码生成器

---

## 项目结构

```
digital-sales/
├── backend/                  # Spring Boot 后端（多模块 Maven 项目）
│   ├── dop-admin/            # 启动入口，集成所有业务模块
│   ├── dop-common/           # 公共工具类
│   ├── dop-digital/          # 数码销售业务核心（订单/库存/售后…）
│   ├── dop-framework/        # 安全、配置、拦截器等框架层
│   ├── dop-generator/        # 代码生成器
│   ├── dop-quartz/           # 定时任务调度（含低库存预警）
│   ├── dop-system/           # 系统管理（用户/角色/菜单等）
│   └── sql/                  # 数据库初始化脚本 & 迁移 SQL
│       ├── dop_20260417.sql  # 基础系统表
│       ├── digital_schema.sql # 业务表结构
│       ├── digital_init.sql  # 初始业务数据
│       └── migration_*.sql   # 增量迁移脚本
├── Frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── api/digital/      # 数码业务 API 封装
│   │   └── views/digital/    # 数码业务页面
│   └── package.json
├── docs/
│   └── 部署说明.md
├── docker-compose.yml        # 一键启动全部服务
└── .env.example              # 环境变量模板（复制为 .env 使用）
```

---

## 快速开始

### 前置要求

- Docker Desktop（已启动）
- 端口 3307 / 6382 / 8081 / 8082 未被占用

### 一键启动

```bash
# 复制环境变量模板
cp .env.example .env
# 编辑 .env，修改 MYSQL_ROOT_PASSWORD 为真实密码

# 启动全部服务（首次需联网拉取镜像和依赖）
docker compose up -d --build

# 查看状态
docker compose ps

# 查看后端日志确认启动成功
docker compose logs -f backend
```

启动完成后访问：

- 前端：`http://localhost:8082`
- 后端 API：`http://localhost:8081`
- Swagger 文档：`http://localhost:8081/swagger-ui.html`

### 数据库初始化

MySQL 容器首次启动时，会按文件名顺序自动执行以下脚本：

1. `00-init-utf8.sh` — 字符集配置
2. `01-dop-system.sql` — 基础系统表（用户、权限、字典等）
3. `02-quartz.sql` — 定时任务表
4. `03-digital-schema.sql` — 商品/订单/库存/售后业务表
5. `04-digital-init.sql` — 初始业务数据
6. `05-demo-data.sql` — 演示数据

> 数据卷已存在时跳过初始化，仅用于全新环境。

### 停止与重启

```bash
docker compose restart            # 重启所有服务
docker compose down               # 停止并保留数据卷
docker compose down -v            # 停止并删除数据卷（全新环境）
```

---

## 关键设计说明

### 订单幂等机制

下单时生成全局唯一订单号（带重试），同时数据库 `sale_order` 表对订单号建立唯一索引，确保网络重试不会创建重复订单。

### 库存并发扣减

SKU 实体使用 `@Version` 乐观锁，扣减库存时执行 `UPDATE stock SET num = num - #{qty} WHERE sku_id = #{id} AND version = #{version}`，版本号不匹配则抛出异常并重试，避免超卖。

### 低库存预警（Quartz）

`dop-quartz` 模块中的定时任务定期扫描库存低于阈值的 SKU，生成 `DigitalStockAlert` 记录供前端展示。预警阈值通过系统配置动态调整。

---

## 测试

后端包含 JUnit 单元测试，覆盖核心业务逻辑：

```bash
cd backend
mvn test -pl dop-digital
```

测试类：

| 测试类 | 覆盖场景 |
|---|---|
| `DigitalSaleOrderServiceImplTest` | 订单创建、幂等校验 |
| `DigitalStockServiceImplTest` | 库存扣减、乐观锁重试 |
| `DigitalStockAlertServiceImplTest` | 低库存预警生成与消除 |
| `DigitalAfterSaleServiceImplTest` | 售后工单流程 |
| `DigitalRefundServiceImplTest` | 退款申请与审核 |
| `DigitalDashboardServiceImplTest` | 数据看板统计 |

---

## 环境变量

参考 `.env.example`，主要变量：

| 变量 | 说明 | 默认值 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | — |
| `MYSQL_PORT` | MySQL 对外端口 | `3307` |
| `REDIS_PORT` | Redis 对外端口 | `6382` |
| `BACKEND_PORT` | 后端对外端口 | `8081` |
| `FRONTEND_PORT` | 前端对外端口 | `8082` |

---

## 许可证与第三方声明

许可证文件固定放在后端模块目录：

- F:\Workspace\DSCP\digital-sales\LICENSE
- F:\Workspace\DSCP\digital-sales\NOTICE

项目部分后端、管理端和患者端代码基于 RuoYi-Vue 二次开发。RuoYi-Vue 的来源、MIT 许可和二次开发说明见 `NOTICE`。其他依赖遵循其自身许可证，依赖清单见各模块的 `pom.xml`、`package-lock.json` 和 `requirements.txt`。
