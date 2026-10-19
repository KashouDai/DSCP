# Calligraph

基于大语言模型的 API 编排 Agent，支持人机协作确认（HiTL），实现多步骤工具选择、参数提取与语义安全校验。

## 项目概述

Calligraph 接收自然语言查询，自主规划并执行多步 API 调用，对接外部业务系统。在执行每一个工具调用之前，Agent 会暂停并请求用户确认——这是一种 **人机协作（Human-in-the-Loop, HiTL）** 设计，有效防止误操作和意外副作用。

### 核心功能

- **多 API 规划** — LLM 判断单次 API 调用能否满足请求，若不能则自动拆解为多步调用链
- **语义化工具选择** — 两阶段检索：Milvus 向量检索 → 重排序 → LLM 最终决策
- **参数提取与补全** — 从自然语言中提取结构化参数；缺失参数通过递归子 API 调用自动补齐
- **提示词注入防御** — 安全校验链在工具执行前检测恶意输入，拦截 prompt injection 攻击
- **人机协作确认** — 每次工具调用前暂停，等待用户确认（同意 / 中止）
- **调用链可视化** — 基于 ECharts 的力导向图，实时展示 API 调用依赖关系
- **用户管理** — JWT 认证 + bcrypt 密码哈希

## 架构

```
┌──────────────────────────────────────────────────────┐
│                     前端 (React)                      │
│  登录 / 注册 ──▶ 仪表板 ──▶ Copilot 对话界面          │
│                               ──▶ API 工具管理        │
│                               ──▶ 调用链可视化        │
└───────────────────────┬──────────────────────────────┘
                        │ REST API (JWT 认证)
┌───────────────────────▼──────────────────────────────┐
│                     后端 (Flask)                      │
│                                                       │
│  /api_planning ──▶ TaskManager.create_task()          │
│           │                                          │
│           ▼                                          │
│  Agent 管道（异步线程）：                               │
│    GenerateTaskHub ──▶ 判断单 API 还是多 API 任务      │
│    ApiSelectionHub ──▶ Milvus 检索 + LLM 选择         │
│    ParamExtractionHub ──▶ 从查询中提取参数             │
│    (递归) ──▶ 补齐缺失参数                            │
│    SecurityChain ──▶ 提示词注入检测                   │
│    ▶ WAIT_CONFIRM (100) — 暂停等待用户确认             │
│           │                                          │
│    [用户确认]                                         │
│           ▼                                          │
│    ToolUseHub ──▶ 执行外部 API 调用                   │
│    GenerateTaskHub ──▶ 判断请求是否已满足              │
│    ToolSummaryHub ──▶ 生成自然语言摘要                │
│    ▶ FINISH (-1)                                     │
└───────────────────────┬──────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   MongoDB          Milvus         LLM API
   (任务、          (工具          (DeepSeek /
    用户、          嵌入向量)       通义千问等)
    工具)
```

### 任务状态机

```
INIT (0) ──▶ RUNNING (1) ──▶ WAIT_CONFIRM (100) ──▶ FINISH (-1)
                                      │
                                      ├──► 用户中止 ──▶ FINISH
                                      │
                                      └──► 用户确认 ──▶ 继续执行
```

## 技术栈

| 层级        | 技术                                 |
|-------------|--------------------------------------|
| 前端        | React 19, Ant Design 5, React Router v7 |
| 可视化      | ECharts 4, echarts-for-react         |
| 后端        | Flask 3, Flask-CORS, Flasgger (Swagger) |
| 认证        | PyJWT, bcrypt                        |
| 数据库      | MongoDB (mongoengine)                |
| 向量存储    | Milvus (pymilvus)                    |
| 大模型      | OpenAI 兼容 SDK（DeepSeek、通义千问等）|
| Agent 框架  | LangGraph、LangChain                   |

## 目录结构

```
calligraph/
├── agent-calligraph-hitl/          # Python 后端
│   ├── app.py                      # Flask 入口，API 路由
│   ├── apis/                       # 规划与选择核心模块
│   ├── chains/                     # LangGraph 链定义
│   ├── customize_milvus_wrapper/   # Milvus 嵌入与检索
│   ├── entity/                     # MongoDB 数据模型
│   ├── models/                     # LLM 与嵌入模型封装
│   ├── param_extraction/           # 参数提取逻辑
│   ├── prompt/                     # 提示词模板
│   ├── services/                   # 模型工厂
│   ├── tasks/                      # 任务生成与管理
│   ├── tools/                      # 工具执行与摘要
│   ├── use_manager/                # 用户认证与 JWT
│   ├── utils/                      # 配置、常量、日志
│   └── workflow/                   # LangGraph 工作流编排
│
└── calligraph-frontend-hitl/       # React 前端
    ├── public/                     # 静态资源
    └── src/
        ├── api/                    # Axios 拦截器与 API 调用
        ├── components/             # 侧边栏、顶栏、共享组件
        └── pages/                  # 路由页面（登录、注册、
                                    #   仪表板、Copilot、API 工具、图）
```

## 快速开始

### 环境要求

- Python 3.11+
- Node.js 18+
- MongoDB（默认地址：`127.0.0.1:27017`）
- Milvus（默认地址：`localhost:19530`）

### 后端启动

```bash
cd agent-calligraph-hitl
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env，填入 API 密钥与服务地址
python app.py
# 后端运行于 http://localhost:5001
```

### 前端启动

```bash
cd calligraph-frontend-hitl
npm install
npm start
# 前端运行于 http://localhost:3000
```

### 环境变量

完整配置项见 `.env.example`：

| 变量 | 说明 |
|---|---|
| `model_api_key` | LLM API 密钥 |
| `model_base_url` | LLM 服务地址 |
| `model_name` | 模型标识（如 `deepseek-v3`） |
| `sim_api_key` | 模拟业务 API 密钥（测试用） |
| `mongo_host` / `mongo_db` / `mongo_port` | MongoDB 连接信息 |
| `milvus_uri` / `milvus_db_name` | Milvus 连接信息 |
| `SECRET_KEY` | JWT 签名密钥 |

## API 接口

| 方法 | 路径              | 说明                   |
|------|-------------------|------------------------|
| POST | `/register_user`  | 用户注册               |
| POST | `/login_user`     | 用户登录（返回 JWT）   |
| POST | `/logout_user`    | 注销（使 JWT 失效）    |
| POST | `/upload_tool`    | 上传 OpenAPI JSON 注册工具 |
| GET  | `/get_all_tools`  | 获取所有已注册工具     |
| POST | `/api_planning`   | 提交查询，开始 API 规划 |
| POST | `/api_task_status`| 轮询任务状态           |
| DELETE | `/delete_tool_by_ids` | 按 ID 删除工具     |
| DELETE | `/delete_all_tool`    | 清空所有工具        |

所有认证接口均需携带 `Authorization: Bearer <token>` 请求头。
