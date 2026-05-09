-- ============================================================
-- 知识库测试数据初始化脚本
-- 执行顺序：分类 → 标签 → 文章 → 文章标签关联
-- ============================================================
-- 使用方式：
--   mysql -u root -p your_database < test-data-init.sql
-- 或在 IDE/数据库客户端中直接执行
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 清空已有数据（按依赖反向顺序）
TRUNCATE TABLE kb_article_tag;
TRUNCATE TABLE kb_article;
TRUNCATE TABLE kb_tag;
TRUNCATE TABLE kb_category;

-- ============================================================
-- 1. 分类数据（5条）
-- ============================================================
INSERT INTO kb_category (id, name, sort, created_at, updated_at) VALUES
(1, '后端开发', 1, NOW(), NOW()),
(2, '前端开发', 2, NOW(), NOW()),
(3, 'DevOps',   3, NOW(), NOW()),
(4, '数据库',   4, NOW(), NOW()),
(5, '人工智能', 5, NOW(), NOW());

-- ============================================================
-- 2. 标签数据（10条）
-- ============================================================
INSERT INTO kb_tag (id, name, created_at, updated_at) VALUES
(1,  'Spring Boot',  NOW(), NOW()),
(2,  'Java',        NOW(), NOW()),
(3,  'Docker',      NOW(), NOW()),
(4,  'Kubernetes',  NOW(), NOW()),
(5,  'MySQL',       NOW(), NOW()),
(6,  'Redis',       NOW(), NOW()),
(7,  'Vue.js',      NOW(), NOW()),
(8,  'React',        NOW(), NOW()),
(9,  'Python',      NOW(), NOW()),
(10, '机器学习',    NOW(), NOW());

-- ============================================================
-- 3. 文章数据（8条）
--    覆盖状态：published(4) / draft(2) / offline(2)
--    覆盖分类：全部 5 个分类
-- ============================================================

-- 文章1：已发布 - Spring Boot + Java，后端开发
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(1,
 'Spring Boot 3.x 快速入门',
 '本文档介绍 Spring Boot 3.x 的核心特性与快速接入流程，适合想快速上手 Spring Boot 3 的开发人员。',
 '一、Spring Boot 3.x 核心特性\n\n1. 要求 JDK 17+，推荐 JDK 21\n2. 引入 Spring Framework 6 的新特性\n3. 更好的 GraalVM 原生镜像支持\n4. Jakarta EE 9+ 默认激活\n\n二、环境准备\n\n1. 安装 JDK 21\n2. 安装 Maven 3.9+\n3. IDE 推荐 IntelliJ IDEA\n\n三、创建第一个项目\n\n使用 Spring Initializr 创建项目，选择 Web 依赖即可。\n\n四、配置文件\n\nSpring Boot 3 推荐使用 application.yml 替代 application.properties。\n\n五、打包部署\n\nmvn clean package -DskipTests\njava -jar target/*.jar',
 1, 'zhangsan', 'published', FALSE, '2026-04-13 08:00:00', NOW(), NOW());

-- 文章2：已发布 - Docker，DevOps
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(2,
 'Docker 容器化部署实战',
 '从零讲解 Docker 部署 Spring Boot 应用的全流程，包括 Dockerfile 编写、镜像构建与容器编排。',
 '一、Docker 简介\n\nDocker 是一个开源的容器化平台，可以让开发者打包应用及其依赖到一个可移植的容器中。\n\n二、Dockerfile 编写\n\nFROM eclipse-temurin:21-jdk-alpine\nWORKDIR /app\nCOPY target/*.jar app.jar\nENTRYPOINT ["java", "-jar", "/app/app.jar"]\n\n三、构建镜像\n\ndocker build -t knowledge-search .\n\n四、启动容器\n\ndocker run -d -p 8080:8080 --name ks knowledge-search\n\n五、Docker Compose 编排\n\nversion: "3.8"\nservices:\n  app:\n    build: .\n    ports:\n      - "8080:8080"',
 3, 'lisi', 'published', FALSE, '2026-04-13 09:00:00', NOW(), NOW());

-- 文章3：已发布 - MySQL，数据库
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(3,
 'MySQL 索引优化指南',
 '深入讲解 MySQL 索引原理与优化实践，涵盖 B+Tree、联合索引、覆盖索引及常见慢查询优化。',
 '一、索引基础\n\nMySQL InnoDB 存储引擎使用 B+Tree 作为索引数据结构。\n\n二、索引分类\n\n1. 主键索引（聚簇索引）\n2. 唯一索引\n3. 普通索引\n4. 联合索引\n\n三、联合索引最左前缀原则\n\n联合索引 (a, b, c) 可以命中以下查询：\n- WHERE a = ?\n- WHERE a = ? AND b = ?\n- WHERE a = ? AND b = ? AND c = ?\n\n四、慢查询优化步骤\n\n1. 开启慢查询日志\n2. 使用 EXPLAIN 分析执行计划\n3. 检查索引使用情况\n4. 优化 SQL 语句\n5. 添加适当索引\n\n五、常见坑点\n\n1. 函数作用于索引列导致索引失效\n2. 类型转换导致索引失效\n3. OR 条件导致索引失效',
 4, 'wangwu', 'published', FALSE, '2026-04-13 10:00:00', NOW(), NOW());

-- 文章4：已发布 - Vue.js，前端开发
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(4,
 'Vue.js 3 组件化开发',
 '详细介绍 Vue.js 3 Composition API、组件通信、Pinia 状态管理与 Vite 构建工具使用。',
 '一、Vue.js 3 新特性\n\n1. Composition API\n2. Teleport 传送门\n3. Fragments\n4. Suspense 异步组件\n\n二、Setup 语法糖\n\n<script setup>\nimport { ref, computed } from ''vue''\nconst count = ref(0)\nconst doubled = computed(() => count.value * 2)\n</script>\n\n三、组件通信\n\n1. Props / Emits\n2. Provide / Inject\n3. Pinia 状态管理\n\n四、Vite 构建\n\nnpm create vite@latest\nnpm install\nnpm run dev',
 2, 'zhaoliu', 'published', FALSE, '2026-04-13 11:00:00', NOW(), NOW());

-- 文章5：草稿 - Kubernetes + Docker，DevOps
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(5,
 'Kubernetes 集群管理与实践',
 'K8s 核心概念、Pod/Deployment/Service 编排、Ingress 配置与 Helm Chart 包管理指南。',
 '一、Kubernetes 架构\n\nMaster 节点：API Server / Scheduler / Controller Manager / Etcd\nWorker 节点：Kubelet / Kube-Proxy / Container Runtime\n\n二、核心资源对象\n\n1. Pod：最小调度单元\n2. Deployment：无状态应用编排\n3. StatefulSet：有状态应用编排\n4. Service：服务发现与负载均衡\n5. Ingress：HTTP/HTTPS 路由\n\n三、常用命令\n\nkubectl get pods\nkubectl apply -f deployment.yaml\nkubectl scale deployment app --replicas=3\n\n四、Helm 使用\n\nhelm install myapp ./chart\nhelm upgrade myapp ./chart\nhelm rollback myapp 1',
 3, 'sunqi', 'draft', FALSE, NULL, NOW(), NOW());

-- 文章6：草稿 - Redis，数据库
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(6,
 'Redis 缓存设计与实战',
 'Redis 数据结构详解、缓存策略（CacheAside/ReadThrough/WriteThrough）、缓存击穿/穿透/雪崩解决方案。',
 '一、Redis 数据结构\n\n1. String：计数器、分布式锁\n2. Hash：对象存储\n3. List：消息队列\n4. Set：标签、好友关系\n5. Sorted Set：排行榜\n\n二、缓存策略\n\nCache-Aside（旁路缓存）：\n1. 读：先读缓存，缓存未命中读 DB 并写入缓存\n2. 写：先写 DB，再删除缓存\n\n三、缓存问题与解决方案\n\n1. 缓存击穿：互斥锁 / 永不过期\n2. 缓存穿透：布隆过滤器 / 存储空值\n3. 缓存雪崩：随机过期时间 / 多级缓存\n\n四、Redis 集群\n\n1. 主从复制\n2. 哨兵模式\n3. Cluster 集群模式',
 4, 'liuba', 'draft', FALSE, NULL, NOW(), NOW());

-- 文章7：已下线 - Python，人工智能
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(7,
 'Python 数据分析入门指南',
 '使用 Pandas / NumPy / Matplotlib 进行数据清洗、分析与可视化的完整教程，附 Jupyter Notebook 示例。',
 '一、环境搭建\n\npip install pandas numpy matplotlib jupyter\n\n二、Pandas 基础\n\nimport pandas as pd\ndf = pd.read_csv(''data.csv'')\ndf.describe()\ndf.groupby(''col'').sum()\n\n三、数据清洗\n\n1. 缺失值处理：df.fillna(0)\n2. 重复值处理：df.drop_duplicates()\n3. 异常值检测：IQR 法则\n\n四、可视化\n\nimport matplotlib.pyplot as plt\nplt.plot(df[''x''], df[''y''])\nplt.show()\n\n五、实战案例\n\n使用 Pandas 分析电商用户行为数据，提取转化率、复购率等核心指标。',
 5, 'yuehen', 'offline', FALSE, '2026-04-10 14:00:00', NOW(), NOW());

-- 文章8：已下线 - Python + 机器学习，人工智能
INSERT INTO kb_article (id, title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES
(8,
 '机器学习模型训练全流程',
 '从数据预处理、特征工程、模型选择到训练调参、模型评估的完整机器学习项目实战。',
 '一、机器学习流程\n\n1. 问题定义\n2. 数据收集\n3. 数据清洗与预处理\n4. 特征工程\n5. 模型选择\n6. 训练与调参\n7. 模型评估\n8. 部署上线\n\n二、数据预处理\n\nfrom sklearn.preprocessing import StandardScaler\nscaler = StandardScaler()\nX_train_scaled = scaler.fit_transform(X_train)\n\n三、模型选择\n\n1. 分类：逻辑回归 / 随机森林 / XGBoost / 神经网络\n2. 回归：线性回归 / 随机森林 / 梯度提升树\n\n四、模型评估指标\n\n1. 分类：Accuracy / Precision / Recall / F1 / AUC\n2. 回归：MSE / RMSE / MAE / R²\n\n五、实战案例\n\n使用 scikit-learn 构建客户流失预测模型，完整展示从特征工程到模型调参的全流程。',
 5, 'chenfd', 'offline', FALSE, '2026-04-11 15:00:00', NOW(), NOW());

-- ============================================================
-- 4. 文章-标签关联数据（10条）
--    规则：article_id, tag_id 组合唯一
-- ============================================================
INSERT INTO kb_article_tag (id, article_id, tag_id, created_at) VALUES
(1, 1, 1,  NOW()),   -- 文章1: Spring Boot
(2, 1, 2,  NOW()),   -- 文章1: Java
(3, 2, 3,  NOW()),   -- 文章2: Docker
(4, 3, 5,  NOW()),   -- 文章3: MySQL
(5, 4, 7,  NOW()),   -- 文章4: Vue.js
(6, 5, 3,  NOW()),   -- 文章5: Docker
(7, 5, 4,  NOW()),   -- 文章5: Kubernetes
(8, 6, 6,  NOW()),   -- 文章6: Redis
(9, 7, 9,  NOW()),   -- 文章7: Python
(10, 8, 9, NOW()),   -- 文章8: Python
(11, 8, 10, NOW());  -- 文章8: 机器学习

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证查询
-- ============================================================
-- SELECT '=== 分类统计 ===' AS '';
-- SELECT * FROM kb_category ORDER BY id;
--
-- SELECT '=== 标签统计 ===' AS '';
-- SELECT * FROM kb_tag ORDER BY id;
--
-- SELECT '=== 文章状态分布 ===' AS '';
-- SELECT status, COUNT(*) AS cnt FROM kb_article GROUP BY status;
--
-- SELECT '=== 文章列表 ===' AS '';
-- SELECT a.id, a.title, a.status, c.name AS category_name, GROUP_CONCAT(t.name) AS tags
-- FROM kb_article a
-- LEFT JOIN kb_category c ON a.category_id = c.id
-- LEFT JOIN kb_article_tag at ON a.id = at.article_id
-- LEFT JOIN kb_tag t ON at.tag_id = t.id
-- GROUP BY a.id, a.title, a.status, c.name
-- ORDER BY a.id;
