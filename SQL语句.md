# SQL语句

-- 1. 知识库标签表
CREATE TABLE IF NOT EXISTS kb_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '标签名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库标签表';

-- 2. 知识库分类表
CREATE TABLE IF NOT EXISTS kb_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 3. 知识库文章表
CREATE TABLE IF NOT EXISTS kb_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(255) NOT NULL COMMENT '文章标题',
    summary VARCHAR(500) COMMENT '文章摘要',
    content LONGTEXT COMMENT '文章正文',
    category_id BIGINT COMMENT '分类ID',
    author VARCHAR(100) COMMENT '作者',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft(草稿)/published(已发布)/offline(已下线)',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志: 0-未删除, 1-已删除',
    publish_time DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_id (category_id),
    INDEX idx_status_deleted (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文章表';

-- 4. 文章-标签关联表
CREATE TABLE IF NOT EXISTS kb_article_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_article_tag (article_id, tag_id),
    INDEX idx_article_id (article_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章与标签关联表';

-- 5. 同步失败日志表 (用于 CDC 补偿)
CREATE TABLE IF NOT EXISTS sync_fail_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    biz_type VARCHAR(50) NOT NULL COMMENT '业务类型: ARTICLE/CATEGORY/TAG/ARTICLE_TAG_RELATION',
    biz_id BIGINT NOT NULL COMMENT '业务主键ID',
    payload TEXT COMMENT '操作载荷或Binlog快照数据',
    error_msg TEXT COMMENT '异常报错信息',
    retry_count INT DEFAULT 0 COMMENT '已重试次数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING(待重试)/SUCCESS(重试成功)/FAILED(最终失败,需人工)',
    next_retry_time DATETIME COMMENT '下次重试时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status_next_time (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步失败补偿日志表';
