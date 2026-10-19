# DSCP：数码销售运营平台与 Agent 集成实验项目

DSCP 是一个面向数码产品销售与售后运营场景的全栈项目，包含业务运营平台，以及一个支持人机协作确认的自然语言 Agent。

项目重点是探索：如何让自然语言请求进入真实业务系统，经过工具选择、参数补全、安全校验和人工确认后，再执行订单、库存、售后等业务操作。

## 项目组成

| 子项目 | 说明 | 主要技术 |
| --- | --- | --- |
| [digital-sales](digital-sales/) | 数码销售运营平台，覆盖商品、订单、库存、售后、退款和数据看板 | Vue 3、Spring Boot、MyBatis、MySQL、Redis、Quartz |
| [calligraph](calligraph/) | API 编排 Agent，负责工具选择、参数提取、安全校验和 HiTL 确认 | Flask、LangGraph、MongoDB、Milvus、React |

业务平台和 Agent 的目标集成链路：

~~~text
浏览器
  │
  ▼
Vue 管理端
  │ REST / JWT
  ▼
Spring Boot 业务后端
  │ 内部鉴权 / Agent Gateway
  ▼
Flask Agent
  │
  ├─ 工具选择与参数提取
  ├─ 安全校验
  └─ 人工确认（HiTL）
~~~

Agent 的写操作应在执行前进入确认状态；用户没有确认时，不应直接执行对应业务工具。

## 数码销售运营平台

### 业务功能

- 商品分类、商品和 SKU 管理
- 客户、销售订单和订单明细管理
- 库存记录与库存扣减
- 售后工单、退款记录和退款审计
- 数据看板与业务查询
- 用户、角色、菜单、字典、日志和定时任务等基础管理

### 关键设计

#### 订单幂等

订单相关请求通过业务幂等标识和数据库约束避免重复创建，适用于网络重试或客户端重复提交。

#### 库存并发控制

库存扣减使用带库存条件的更新逻辑，只有库存满足条件时才允许扣减，避免并发请求将库存扣成负数。

#### 低库存预警

通过 Quartz 定时任务检查低库存商品，并记录预警状态。

#### Agent 接入边界

业务后端作为浏览器与 Agent 之间的网关。浏览器不直接访问 Agent，Agent 也不应直接暴露为公网业务接口。

## Calligraph Agent

Calligraph 是一个面向 API 工具编排的 Agent 原型，主要流程包括：

1. 接收用户的自然语言请求；
2. 识别请求意图并选择候选工具；
3. 提取工具所需参数；
4. 对工具和参数进行安全校验；
5. 对缺失参数进行补充询问；
6. 对需要确认的操作进入 WAIT_CONFIRM 等待状态；
7. 用户确认后执行工具并返回结果；
8. 根据执行结果判断任务是否完成。

当前 Agent 的重点是可控编排和人机协作，不应描述为完全自主、无确认的通用执行 Agent。

## 技术栈

### 业务平台

- 前端：Vue 3、Vite、Element Plus、Pinia
- 后端：Java、Spring Boot、MyBatis、Maven
- 数据库：MySQL 8
- 缓存：Redis 7
- 定时任务：Quartz
- 部署：Docker Compose、Nginx

### Agent

- Agent 后端：Python、Flask、LangGraph
- Agent 前端：React
- 工具与状态：MongoDB、Milvus
- 模型接入：兼容 OpenAI API 形式的模型服务
- 交互机制：参数补全、HiTL 确认、任务状态流转

## 目录结构

~~~text
DSCP/
├── digital-sales/
│   ├── backend/                    # Spring Boot 多模块后端
│   ├── Frontend/                   # Vue 3 管理端
│   ├── sql/                        # 业务表结构、初始化和演示数据
│   ├── docker-compose.yml          # 业务平台容器编排
│   └── README.md
├── calligraph/
│   ├── agent-calligraph-hitl/      # Flask + LangGraph Agent 后端
│   ├── calligraph-frontend-hitl/   # React 独立前端
│   └── README.md
├── docs/                           # 当前公开版本保留的必要文档
├── .env.example                    # 业务平台环境变量模板
└── README.md
~~~

## 快速开始：业务平台

### 前置要求

- Docker Desktop
- 可用的 Docker Compose
- 宿主机端口 3307、6382、8081、8082 未被占用

### 启动

PowerShell：

~~~powershell
cd digital-sales
Copy-Item .env.example .env
~~~

编辑 .env，至少设置本地开发密码：

~~~dotenv
MYSQL_ROOT_PASSWORD=change-this-password
~~~

然后启动：

~~~powershell
docker compose up -d --build
docker compose ps
~~~

### 访问地址

| 服务 | 地址 |
| --- | --- |
| 管理端 | http://localhost:8082 |
| 后端 API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| MySQL | localhost:3307 |
| Redis | localhost:6382 |

首次使用空数据卷时，MySQL 会按 Compose 配置顺序执行基础表结构、业务表结构、初始化数据和虚拟演示数据。数据卷已经存在时，MySQL 不会自动重复执行初始化脚本。

演示数据位于 [digital-sales/backend/sql/demo_data.sql](digital-sales/backend/sql/demo_data.sql)，内容为虚拟数据，仅用于本地开发和功能演示。

### 停止服务

~~~powershell
docker compose down       # 停止并移除容器，保留数据卷
docker compose down -v    # 同时删除数据卷，下次启动重新初始化
~~~

## Agent 本地运行

Agent 的独立运行方式和环境变量说明见 [calligraph/README.md](calligraph/README.md)：

~~~powershell
cd calligraph/agent-calligraph-hitl
Copy-Item .env.example .env
pip install -r requirements.txt
python app.py
~~~

Agent 运行通常需要 MongoDB、Milvus 和可用的模型服务。模型密钥只应放在本地 .env 或系统环境变量中，不要提交到 Git。

## 当前集成注意事项

Agent 与业务平台的集成代码仍保留，但完整 Docker 联调依赖一个运行时工具目录文件。当前公开版本已移除过程记录和工具目录文档，而 docker-compose.yml 仍会尝试挂载：

~~~text
digital-sales/docs/digital-sales-tools.json
~~~

如果需要启动包含 Agent 的完整 Compose 集成环境，需要先提供与 backend、Agent 接口约定一致的工具目录文件，或同步调整 Compose 配置。不要把真实内部接口、密钥或生产工具定义直接提交到公开仓库。

## 测试

业务后端：

~~~powershell
cd digital-sales/backend
mvn test -pl dop-digital
~~~

Agent：

~~~powershell
cd calligraph/agent-calligraph-hitl
pytest
~~~

Docker 启动成功只说明容器能够启动，不等同于完整浏览器验收。涉及 Agent 轮询、参数补全和确认卡片的修改，还应在浏览器中实际走完对应流程。

## 安全与发布边界

- 不要提交 .env、API Key、JWT Secret、数据库密码或模型 Token。
- 不要把 Redis RDB/AOF、日志、上传文件、构建目录和依赖目录提交到仓库。
- 演示 SQL 只允许使用虚拟业务数据。
- Agent 写操作应保留人工确认边界。
- 本项目适合本地开发、学习和技术方案展示，不应直接作为生产系统部署。

## 许可证与第三方声明

- 数码销售平台的许可证见 [digital-sales/LICENSE](digital-sales/LICENSE)。
- 前端依赖的许可证见 [digital-sales/Frontend/LICENSE](digital-sales/Frontend/LICENSE)。
- RuoYi-Vue 衍生关系及第三方声明见 [digital-sales/NOTICE](digital-sales/NOTICE)。

## 项目状态

当前仓库是一个可阅读、可本地验证的个人项目版本。业务平台的 Docker 启动链路已经验证过；Agent 的完整集成启动还依赖运行时工具目录，不应将其描述为开箱即用的完整生产方案。
