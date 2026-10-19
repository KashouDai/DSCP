# DSCP — 数码销售运营平台与 Calligraph Agent

本仓库包含两个子项目及其集成：

- **[digital-sales](digital-sales/)** — 数码销售运营平台（Vue 3 + Spring Boot），覆盖商品、订单、库存、售后、退款全链路，支持 Docker Compose 一键部署。
- **[calligraph](calligraph/)** — 基于大语言模型的 API 编排 Agent（React + Flask），支持多步骤工具选择、参数提取、语义安全校验与人机协作确认（HiTL）。

两者已打通：Agent 以嵌入式抽屉的形式集成进运营平台管理端，用户可以用自然语言驱动订单、库存、售后等业务查询与操作，每次工具调用前暂停等待确认。

各子项目的详细说明见 [digital-sales/README.md](digital-sales/README.md) 和 [calligraph/README.md](calligraph/README.md)。

---

## 架构

```
┌──────────────────────────────────────────────────────────┐
│  浏览器  http://localhost:8082                            │
│  Vue 3 管理端 + 内嵌 Agent 抽屉（AgentDrawer）             │
└──────────────────────────┬───────────────────────────────┘
                           │ REST / JWT
┌──────────────────────────▼───────────────┐  X-Agent-Key  ┌──────────────────────────┐
│  backend   Spring Boot  :8080            │◀─────────────▶│  agent  Flask  :5000     │
│  业务 API + Agent Gateway + 工具目录      │               │  LangGraph 编排 / HiTL   │
└───────┬──────────────────────────────────┘               └────────┬─────────────────┘
        │                                                           │
   ┌────▼─────┐   ┌─────────┐                          ┌───────────┴──────────┐
   │  MySQL   │   │  Redis  │                          │  MongoDB   │  Milvus │
   └──────────┘   └─────────┘                          └────────────┴─────────┘
```

Agent 只在内网暴露，不映射到宿主机端口，仅由 backend 通过 `http://agent:5000` 访问。

---

## 目录结构

```
DSCP/
├── calligraph/
│   ├── agent-calligraph-hitl/      # Python Agent 后端（Flask + LangGraph）
│   ├── calligraph-frontend-hitl/   # Agent 独立前端（React）
│   └── README.md
├── digital-sales/
│   ├── backend/                    # Spring Boot 多模块后端
│   ├── Frontend/                   # Vue 3 管理端
│   ├── docs/                       # 部署说明、接入 PRD、工具目录
│   ├── docker-compose.yml          # 一键启动全部 5 个服务
│   ├── .env.example                # 环境变量模板
│   └── README.md
└── docs/agent/                     # 集成过程的阶段、接口、测试与 ADR 文档
```

---

## 快速开始

### 前置要求

- Docker Desktop（已启动）
- 端口 `3307` / `6382` / `8081` / `8082` 未被占用

### 一键启动

```bash
cd digital-sales
cp .env.example .env
```

编辑 `.env`，至少配置这几项：

| 变量 | 是否必填 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | 必填 | MySQL root 密码 |
| `AGENT_INTERNAL_KEY` | 必填 | 后端与 Agent 的内部调用密钥，两端需一致；留空会导致该链路鉴权失败 |
| `AGENT_SECRET_KEY` | 必填 | Agent 的 JWT 签名密钥，改成随机字符串 |
| `DASHSCOPE_API_KEY` | 建议填 | 大模型 API 密钥；留空时 Agent 可启动但无法调用模型 |

然后启动：

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

首次启动需联网拉取镜像和依赖。数据库脚本在 MySQL 容器首次初始化时按文件名顺序自动执行，数据卷已存在时会跳过。

### 访问地址

| 服务 | 地址 |
|---|---|
| 管理端 | http://localhost:8082 |
| 后端 API | http://localhost:8081 |
| Swagger 文档 | http://localhost:8081/swagger-ui.html |

### 停止与清理

```bash
docker compose restart      # 重启
docker compose down         # 停止并保留数据卷
docker compose down -v      # 停止并删除数据卷（全新环境）
```

---

## 端口与依赖

| 服务 | 容器内端口 | 宿主机端口 | 依赖 |
|---|---|---|---|
| MySQL | 3306 | `MYSQL_PORT`（默认 3307） | — |
| Redis | 6379 | `REDIS_PORT`（默认 6382） | — |
| backend | 8080 | `BACKEND_PORT`（默认 8081） | MySQL、Redis |
| agent | 5000 | 不暴露 | MongoDB、Milvus、大模型 API |
| frontend | 80 | `FRONTEND_PORT`（默认 8082） | backend |

Agent 默认连接宿主机的 MongoDB（`27017`）和 Milvus（`19530`），通过 `MONGO_HOST` / `MILVUS_URI` 配置。这两个服务不包含在本仓库的 compose 中，需自行准备；`AGENT_LOCAL_MODE=1` 会强制改用本机地址。

---

## 本地开发

### 后端（digital-sales/backend）

```bash
cd digital-sales/backend
mvn spring-boot:run -pl dop-admin
```

Java 17 + Maven。数据库连接见 `dop-admin/src/main/resources/application-druid.yml`，通过 `DB_PASSWORD` 等环境变量覆盖。

### 管理端（digital-sales/Frontend）

```bash
cd digital-sales/Frontend
npm install
npm run dev
```

### Agent 后端（calligraph/agent-calligraph-hitl）

```bash
cd calligraph/agent-calligraph-hitl
pip install -r requirements.txt
cp .env.example .env   # 编辑填入 API 密钥与服务地址
python app.py
```

Python 3.11+，运行于 `http://localhost:5000`。本地运行需要可用的 MongoDB 与 Milvus。

### Agent 独立前端（calligraph/calligraph-frontend-hitl）

```bash
cd calligraph/calligraph-frontend-hitl
npm install
npm start
```

Node.js 18+，运行于 `http://localhost:3000`。

---

## 测试

```bash
# 后端单元测试
cd digital-sales/backend && mvn test -pl dop-digital

# Agent 测试
cd calligraph/agent-calligraph-hitl && pytest
```

---

## 注意事项

- `.env` 已被 `.gitignore` 忽略，请勿提交真实密钥。所有配置项以 `.env.example` 为准。
- 生产环境必须替换 `AGENT_SECRET_KEY`、`AGENT_INTERNAL_KEY` 和 `MYSQL_ROOT_PASSWORD`，不要使用示例值。
- 仓库中 `digital-sales/backend/sql/demo_data.sql` 为虚构演示数据，仅用于本地开发。
- `digital-sales/docs/digital-sales-tools.json` 是 Agent 的运行时工具目录，被 backend 与 agent 共同加载，删除会导致 Agent 无法工作。

---

