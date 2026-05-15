# KnowledgeSearch

企业级知识库检索与 CDC 实时同步平台。

本项目以 MySQL 作为唯一事实数据源，以 Elasticsearch 作为搜索副本，通过 Canal 订阅 MySQL Binlog，并由 Sync Worker 回源 MySQL 重建 ES 文档。系统重点解决知识库文章的管理、全文检索、增量同步、失败记录、重试补偿与基础可观测问题。

## 技术栈

后端：

- Java 21
- Spring Boot 3.3.7
- MyBatis-Plus 3.5.8
- MySQL
- Elasticsearch
- Canal Client 1.1.7
- Knife4j / Springdoc OpenAPI
- JUnit 5 + H2

前端：

- React 19
- TypeScript
- Vite 6
- lucide-react

## 核心能力

- 文章 CRUD、发布、下线、逻辑删除
- 分类管理、标签管理
- 关键词全文搜索
- 标题、摘要、正文多字段检索
- 分类、标签、状态、作者、时间范围筛选
- 搜索结果高亮
- 全量同步
- Canal 增量同步
- 同步失败日志记录
- 失败任务手动重试与定时补偿
- 基础同步状态统计
- 企业级前端运营台

## 架构说明

```text
Admin Console / Search UI
          |
          v
Spring Boot Controllers
          |
          v
Service Layer
  |                 |
  v                 v
MySQL --------> Canal Binlog
  |                 |
  |                 v
  |          Sync Event Adapter
  |                 |
  |                 v
  +------> Sync Worker
                    |
                    v
             Elasticsearch
```

关键原则：

- MySQL 是唯一事实数据源。
- Elasticsearch 只作为派生搜索索引。
- 业务写入只落 MySQL，不在业务接口中直接双写 ES。
- 增量同步通过 Canal 事件驱动。
- 构建 ES 文档时回源 MySQL，避免只依赖 Binlog 局部字段。
- 同步失败必须记录，并支持重试与补偿。

## 目录结构

```text
.
├── client/                         # React + Vite 前端运营台
│   ├── src/api                     # 前端接口封装
│   ├── src/components              # 通用 UI 组件
│   ├── src/pages                   # 工作台、文章、搜索、分类标签、同步页面
│   └── vite.config.ts              # Vite 配置，前端端口 5172
├── src/main/java/com/knowledge/search
│   ├── controller                  # 管理端与搜索端 API
│   ├── service                     # 业务服务、搜索服务、同步服务
│   ├── mapper                      # MyBatis-Plus Mapper
│   ├── domain/entity               # MySQL 实体
│   ├── search                      # ES 文档、查询、仓储与构建逻辑
│   ├── sync                        # Canal、Sync Worker、Retry Job
│   ├── common                      # 通用响应、枚举、异常、基础实体
│   └── config                      # MyBatis、OpenAPI、同步配置
├── src/main/resources              # Spring Boot 配置
├── src/test                        # 后端测试
├── start-frontend.bat              # Windows 前端启动脚本
├── start-frontend.ps1              # PowerShell 前端启动脚本
└── pom.xml
```

## 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 22+
- MySQL
- Elasticsearch
- Canal Server

默认服务端口：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5172`
- Elasticsearch：`http://localhost:9200`
- MySQL：`localhost:3307`
- Canal：`127.0.0.1:11111`

## 后端启动

先确认 `src/main/resources/application.yml` 中的 MySQL、Elasticsearch、Canal 配置符合本机环境。

```bash
mvn spring-boot:run
```

打包：

```bash
mvn clean package
```

运行测试：

```bash
mvn test
```

接口文档：

- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Knife4j：根据 Knife4j 默认入口访问

## 前端启动

Windows 下可直接运行：

```powershell
.\start-frontend.bat
```

或手动启动：

```powershell
npm.cmd install --prefix client
npm.cmd run dev --prefix client
```

前端访问地址：

```text
http://localhost:5172/
```

前端开发环境通过 Vite 代理访问后端：

```text
/api/* -> http://localhost:8080/*
```

前端构建：

```powershell
npm.cmd run build --prefix client
```

## 数据同步规则

文章相关 ES 文档采用回源重建策略：

- 文章变更：重建完整文章文档。
- 文章标签关系变更：重新加载完整标签集合后重建。
- 标签变更：查找受影响文章并重建。
- 分类变更：查找受影响文章并重建。
- 删除或下线：明确从 ES 删除或隐藏相关文档。

失败处理：

- 同步失败写入 `sync_fail_log`。
- 失败记录包含业务类型、业务 ID、事件载荷、错误信息、重试次数、状态和下次重试时间。
- 重试次数有上限，避免无限重试。

## 前端页面

- 工作台：展示文章、分类标签、同步状态与最近文章。
- 文章管理：支持筛选、分页、新建、编辑、详情、发布、下线、删除。
- 知识检索：支持关键词、分类、标签、作者、状态、时间范围、排序与高亮。
- 分类标签：支持分类与标签的创建、编辑、删除。
- 同步状态：展示失败统计、失败日志、全量同步与手动重试。

## 测试建议

后端：

```bash
mvn test
```

前端：

```powershell
npm.cmd run build --prefix client
```

联调流程：

1. 启动 MySQL、Elasticsearch、Canal。
2. 启动后端 `http://localhost:8080`。
3. 启动前端 `http://localhost:5172`。
4. 在前端创建分类、标签和文章。
5. 发布文章并触发全量同步。
6. 在搜索页验证关键词检索、筛选和高亮。
7. 在同步页查看统计与失败日志。

## 注意事项

- 当前默认配置适合本地开发，生产环境应将数据库密码、Canal 密码等敏感配置改为环境变量或外部配置。
- V1 默认链路是 `Canal -> Sync Worker -> Elasticsearch`。
- 不默认引入 RabbitMQ、Redis、IK 分词、搜索建议、热词统计等 V2 能力。
