# **企业级知识库检索与 CDC 实时同步平台**

## 项目定位

面向企业内部的制度文档、FAQ、公告、技术规范等内容，构建一个：

- **MySQL 作为主数据源**
- **Elasticsearch 作为检索副本**
- **Canal 负责监听 Binlog**
- **同步服务负责数据写入 ES**
- **失败可重试、可补偿、可监控**的知识库全文检索平台。

## 二、总体设计原则

### 1. 先做一个强闭环

MVP 只做四件事：

1. 后台管理知识库内容
2. 用户按关键词搜索
3. MySQL 变更自动同步到 ES
4. 同步失败可记录、重试、补偿

这个思路最稳，开发难度可控，最适合你真正落地。 

### 2. 把 MQ 作为增强能力，而不是一开始就强上

**V1：Canal → Sync Worker → ES**

**V2：Canal → MQ → Sync Consumer → ES**

## 三、最终落地架构

### V1主实现框架

             ┌──────────────────────────┐
             │   Admin / Search API     │
             │   Spring Boot 主应用      │
             └─────────────┬────────────┘
                           │
                    写入/查询 MySQL
                           │
                           ▼
                      ┌──────────┐
                      │  MySQL   │
                      └────┬─────┘
                           │ Binlog
                           ▼
                     ┌────────────┐
                     │ Canal Server│
                     └────┬────────┘
                          │
                          ▼
                  ┌────────────────┐
                  │ Sync Worker     │
                  │ 同步服务/消费逻辑 │
                  └───────┬────────┘
                          │ upsert/delete
                          ▼
                   ┌───────────────┐
                   │ Elasticsearch │
                   └───────────────┘

### V2增强版架构

```
Admin/Search API
      │
      ▼
    MySQL
      │
   Binlog
      ▼
 Canal Server
      │
      ▼
 RabbitMQ
      │
      ▼
 ES Sync Consumer
      │
      ▼
Elasticsearch
```

### 这样设计的原因

- **V1 简单可跑通**：最短路径完成项目
- **V2 好讲亮点**：能回答“为什么要 MQ 解耦”

## 四、具体可行的功能范围

### V1 必做

### 1）内容管理

- 新增文档
- 编辑文档
- 删除文档
- 发布/下线

### 2）检索能力

- 关键词搜索
- 标题 / 摘要 / 正文多字段检索
- 分类过滤
- 标签过滤
- 发布时间排序
- 高亮返回

### 3）同步能力

- 全量初始化同步
- Canal 增量同步
- 新增 / 修改 / 删除同步到 ES

### 4）运维能力

- 同步失败日志
- 定时补偿重试
- 简单同步状态查询

### V2 可选增强

- RabbitMQ 解耦
- IK 中文分词
- 搜索建议（completion suggester）
- 热门搜索词统计
- Redis 缓存
- 死信队列 / 延迟重试
- 10 万数据压测与查询性能对比

## 五、技术栈建议

### 主技术栈

- **JDK 21**
- **Spring Boot 3.5.x**
- **MySQL 8.0**
- **Elasticsearch 8.18.x**
- **Spring Data Elasticsearch 5.5.x**
- **Canal 1.1.x**
- **MyBatis-Plus**
- **Knife4j / OpenAPI**
- **Docker Compose**

### 增强技术栈

- **RabbitMQ**：用于同步解耦
- **Redis**：用于热点词和缓存
- **Kibana**：用于索引调试
- **IK 分词器**：用于中文检索优化

## 六、数据库鱼索引设计

### MySQL 表设计

#### 1. `kb_article`

```
id
title
summary
content
category_id
author
status          -- draft/published/offline
is_deleted
publish_time
created_at
updated_at
```

#### 2. `kb_category`

```
id
name
sort
created_at
updated_at
```

#### 3. `kb_tag`

```
id
name
created_at
updated_at
```

#### 4. `kb_article_tag`

```
id
article_id
tag_id
created_at
```

#### 5. `sync_fail_log`

```
id
biz_type
biz_id
payload
error_msg
retry_count
status
next_retry_time
created_at
updated_at
```

### Elasticsearch 索引设计

#### kb_article_index

```
id
title
summary
content
categoryId
categoryName
tagNames
author
status
publishTime
updatedAt
```

### Mapping 建议

- `title`：text，权重最高
- `summary`：text，权重中等
- `content`：text，全文检索
- `categoryName`：keyword + text
- `tagNames`：keyword
- `status`：keyword / integer
- `publishTime`：date
- `updatedAt`：date

### 检索策略

#### 查询逻辑

- `multi_match(title^3, summary^2, content^1)`
- `bool.filter(categoryId/status/tagNames/publishTime)`
- `highlight(title, summary, content)`
- 排序：`_score desc` + `publishTime desc`

## 七.最终一致性设计

### 一致性策略

#### 采用**最终一致性**设计，不追求强一致：

1. **MySQL 是唯一真实数据源**
2. **ES 是检索副本**
3. 系统提供一次**全量初始化**
4. 后续通过 **Canal 增量订阅**
5. 同步失败写入 `sync_fail_log`
6. 定时任务做失败重试
7. 每天做一次离线校验补偿

### 为什么不双写 MySQL + ES

不建议在业务代码里直接同时写 MySQL 和 ES，因为：

- 业务侵入大
- 耦合高
- 失败难处理
- ES 异常可能拖垮主链路

所以选用 Canal 基于 Binlog 的无侵入方案更合理

### 如果用了 MQ，怎么增强可靠性

- 开启手动 ACK
- 消费失败进行有限次重试
- 超过阈值进死信队列
- 死信消息落表，人工排查
- 保留定时补偿任务做最终兜底

## 八.同步链路设计

### V1 直接同步

**MySQL → Canal → Sync Worker → ES**

#### 1）全量同步设计

提供一个初始化接口：

```
POST /admin/sync/full
```

逻辑：

1. 按页查询 MySQL 中已发布文档
2. 补齐分类名、标签名
3. 转成 ES 文档
4. 批量写入 ES
5. 记录同步进度和成功/失败数

关键点：

- 支持重复执行
- 支持断点续跑
- 只同步 `status=published and is_deleted=0`

------

#### 2）增量同步设计

Canal 监听表：

- `kb_article`
- `kb_category`
- `kb_tag`
- `kb_article_tag`

##### 不同表的处理策略

##### A. `kb_article` 变更

- INSERT：新建 ES 文档
- UPDATE：重新查询文章完整信息后 upsert
- DELETE：删除 ES 文档，或标记为下线

##### B. `kb_article_tag` 变更

- 重新查该文章全部标签
- 重建该文章 ES 文档

##### C. `kb_tag` 变更

- 找到受影响文章
- 批量更新 ES 文档中的 `tagNames`

##### D. `kb_category` 变更

- 找到该分类下所有文章
- 批量更新 `categoryName`

##### 核心原则

**不要直接用 binlog 字段拼 ES 文档，统一回查 MySQL 后重建 ES 文档。**

原因：

- 简单
- 正确率高
- 避免关联数据不完整
- 更适合简历项目落地

------

#### 3）失败补偿机制

同步失败时：

1. 记录到 `sync_fail_log`
2. 保存 `biz_type + biz_id + payload + error_msg`
3. 定时任务扫描失败记录
4. 按次数限制重试
5. 多次失败后标记人工处理

重试建议：

- 第 1 次失败：1 分钟后
- 第 2 次失败：5 分钟后
- 第 3 次失败：15 分钟后
- 超过 3 次：标记 `FAILED`

每日校验任务：

每天凌晨做一次：

- 抽样对比 MySQL 与 ES 数据数量
- 可选做按 `updated_at` 的增量校验

# 九、接口设计建议

### 管理端接口

#### 文档管理

- `POST /admin/article`
- `PUT /admin/article/{id}`
- `DELETE /admin/article/{id}`
- `PUT /admin/article/{id}/publish`
- `PUT /admin/article/{id}/offline`

#### 分类标签

- `POST /admin/category`
- `POST /admin/tag`

#### 搜索接口

- `GET /search?q=...`

- ```
  GET /search/condition
  ```

  - 支持 `keyword`
  - `categoryId`
  - `tag`
  - `pageNum`
  - `pageSize`
  - `sort`

#### 同步接口

- `POST /admin/sync/full`
- `POST /admin/sync/retry/{failId}`
- `GET /admin/sync/fail/list`
- `GET /admin/sync/stats`

## 十、实现顺序

### 第一阶段：先做基础业务

- 建表
- 写文章 CRUD
- 写分类/标签管理
- 写发布/下线逻辑

### 第二阶段：先手工接 ES

- 建索引
- 写文档转换器
- 手工同步数据到 ES
- 实现搜索、高亮、过滤

### 第三阶段：做全量同步

- `fullSync()`
- 按页查 MySQL
- 批量写入 ES

### 第四阶段：接 Canal 增量同步

- 配置 MySQL binlog
- 配置 Canal 监听表
- 写 Sync Worker
- 实现增删改同步逻辑

### 第五阶段：加补偿与监控

- 失败日志表
- 定时重试任务
- 同步状态查询接口

### 第六阶段：做增强项

按优先级选做：

1. IK 分词
2. RabbitMQ 解耦
3. 搜索建议
4. 热词统计
5. Redis 缓存
6. 压测与性能对比

