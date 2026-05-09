-- ============================================================
-- 知识库 1000条测试数据生成脚本
-- 数据库：knowledge_search（见 application-dev.yml）
-- MySQL 版本：8.0+（使用了 WITH RECURSIVE / ELT / RANDOMBITS）
--
-- 使用方式：
--   mysql -u root -p knowledge_search < test-data-100k.sql
--   （Navicat / DBeaver 直接执行亦可）
--
-- 执行时间：约 10-30 秒（取决于机器性能）
-- 脚本执行完后，用末尾的验证 SQL 检查分布
-- ============================================================

-- -------------------------
-- 0. 参数设置
-- -------------------------
SET @ARTICLE_COUNT = 1000;
SET @BATCH_SIZE = 1000;

-- -------------------------
-- 1. 清空文章及关联数据
--    （保留分类和标签，因为它们是可复用基础数据）
-- -------------------------
TRUNCATE TABLE kb_article_tag;
TRUNCATE TABLE kb_article;
ALTER TABLE kb_article AUTO_INCREMENT = 1;

-- -------------------------
-- 2. 生成存储过程
-- -------------------------
DROP PROCEDURE IF EXISTS generate_articles;
DROP PROCEDURE IF EXISTS generate_article_tags;


DELIMITER //

-- ============================================================
-- generate_articles：用单层 WHILE 循环，每 batch_size 条执行一次 INSERT
-- 关键修复：去掉 DECLARE CONTINUE HANDLER FOR NOT FOUND（会导致 done=TRUE 提前退出）
--          去掉嵌套 WHILE，改用单层 i 递增
-- ============================================================
CREATE PROCEDURE generate_articles(IN total_count BIGINT, IN batch_size INT)
BEGIN
    DECLARE i BIGINT DEFAULT 1;
    DECLARE rand_val BIGINT;
    DECLARE rand_status VARCHAR(20);
    DECLARE rand_category BIGINT;
    DECLARE rand_days BIGINT;
    DECLARE rand_author VARCHAR(50);
    DECLARE pub_time_val DATETIME;
    DECLARE content_tpl INT;
    DECLARE content_text TEXT;

    -- 单篇内容模板（5 种不同长度风格，轮流复用）
    DECLARE tpl1 TEXT DEFAULT '本文深入讲解核心技术原理，结合实际业务场景，提供可落地的解决方案。一、环境准备确认 JDK 版本、内存充足、网络可达。二、核心原理从源码层面分析实现逻辑，图文并茂地展示关键流程。三、最佳实践总结踩坑经验，给出生产级别的配置建议。四、扩展思考探讨与其他技术的对比，以及未来的演进方向。';
    DECLARE tpl2 TEXT DEFAULT '这是一篇实战总结，记录了从需求分析到方案设计、从编码实现到测试验证的完整过程。适用于想了解完整项目流程或寻找类似问题解决思路的开发者。包含代码示例、配置说明、常见错误汇总。';
    DECLARE tpl3 TEXT DEFAULT '快速参考指南，将复杂知识浓缩为关键要点。涵盖：1. 核心概念 2. 常用命令 3. 配置示例 4. 排错思路 5. 官方文档链接。适合快速上手或作为日常查阅手册。';
    DECLARE tpl4 TEXT DEFAULT '技术深度解析，从历史演进讲起，梳理该技术发展的关键节点。对比不同版本差异，分析各自适用场景。重点剖析设计思想和实现细节，配合源码级解读。适合有一定基础的开发者进阶学习。';
    DECLARE tpl5 TEXT DEFAULT '避坑指南，汇集了实际项目中真实遇到的典型问题。每个坑均包含：问题现象、原因分析、解决方案、预防措施。通过真实案例帮助你少走弯路，提升代码质量。';

    -- 单层 WHILE：i 从 1 到 total_count，每次处理 batch_size 条
    outer_loop: WHILE i <= total_count DO

        SET @sql = CONCAT(
            'INSERT INTO kb_article (title, summary, content, category_id, author, status, is_deleted, publish_time, created_at, updated_at) VALUES'
        );

        -- 内层循环：构造 batch_size 条 VALUES（最后一条不加逗号）
        inner_loop: WHILE i <= total_count AND MOD(i - 1, batch_size) < batch_size DO

            -- 伪随机（用文章 ID * 质数 做种子，每次生成结果一致）
            SET rand_val = i * 17 + 31;
            SET rand_category = 1 + (rand_val % 5);  -- 1-5 均匀分布
            SET content_tpl = 1 + (rand_val % 5);

            -- 状态分布：70% published / 20% draft / 10% offline
            IF rand_val % 100 < 70 THEN
                SET rand_status = 'published';
            ELSEIF rand_val % 100 < 90 THEN
                SET rand_status = 'draft';
            ELSE
                SET rand_status = 'offline';
            END IF;

            -- 发布时间：近 180 天内随机
            SET rand_days = FLOOR(1 + (rand_val % 180));
            SET pub_time_val = DATE_SUB(NOW(), INTERVAL rand_days DAY);
            SET pub_time_val = DATE_ADD(pub_time_val, INTERVAL MOD(rand_val, 24) HOUR);
            SET pub_time_val = DATE_ADD(pub_time_val, INTERVAL MOD(rand_val * 3, 60) MINUTE);

            -- 作者：20 人池轮询
            SET rand_author = ELT(1 + (rand_val % 20),
                'zhangsan','lisi','wangwu','zhaoliu','sunqi',
                'liuba','yuehen','chenfd','zhubajie','sunwukong',
                'tangsenzan','gaomeigui','libai','dufu','baijuyi',
                'lim Bai','moushi','liumou','wangapi','zhaoji');

            -- 内容模板
            IF content_tpl = 1 THEN SET content_text = tpl1;
            ELSEIF content_tpl = 2 THEN SET content_text = tpl2;
            ELSEIF content_tpl = 3 THEN SET content_text = tpl3;
            ELSEIF content_tpl = 4 THEN SET content_text = tpl4;
            ELSE SET content_text = tpl5;
            END IF;

            -- 拼 VALUES 项（最后一条不加逗号）
            SET @sql = CONCAT(@sql,
                CONCAT(
                    '(', QUOTE(CONCAT('技术文档_', i)),
                    ',', QUOTE(CONCAT('本文档为第 ', i, ' 篇技术文章，涵盖核心技术要点与实战经验。')),
                    ',', QUOTE(content_text),
                    ',', rand_category,
                    ',', QUOTE(rand_author),
                    ',', QUOTE(rand_status),
                    ',0',
                    ',', IF(rand_status = 'published', CONCAT("'", pub_time_val, "'"), 'NULL'),
                    ',NOW(),NOW())'
                )
            );

            -- 每 batch_size 条换一行 VALUES，不在本行最后加逗号
            IF MOD(i, batch_size) != 0 AND i < total_count THEN
                SET @sql = CONCAT(@sql, ',');
            END IF;

            SET i = i + 1;

            -- 本批次已满 batch_size 条，退出内层循环，准备执行 INSERT
            IF MOD(i - 1, batch_size) = 0 THEN
                LEAVE inner_loop;
            END IF;

        END WHILE inner_loop;

        -- 执行本批次 INSERT
        SET @sql = CONCAT(@sql, ';');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

    END WHILE outer_loop;

    SELECT CONCAT('Articles inserted: ', total_count, ' rows') AS result;
END //

-- 生成文章标签关联（每个 article 随机 1-3 个标签）
CREATE PROCEDURE generate_article_tags(IN article_count BIGINT)
BEGIN
    DECLARE i BIGINT DEFAULT 1;
    DECLARE tag1 BIGINT;
    DECLARE tag2 BIGINT;
    DECLARE tag3 BIGINT;
    DECLARE rand_val BIGINT;
    DECLARE tag_count INT;

    WHILE i <= article_count DO
        SET rand_val = i * 17 + 31;

        -- 第一个标签（必有）
        SET tag1 = 1 + (rand_val % 10);

        -- 60% 概率有第二个标签
        IF rand_val % 100 < 60 THEN
            SET tag2 = 1 + ((rand_val * 3) % 10);
            IF tag2 = tag1 THEN SET tag2 = 1 + ((tag2 + 1) % 10); END IF;
        END IF;

        -- 40% 概率有第三个标签
        IF rand_val % 100 < 40 THEN
            SET tag3 = 1 + ((rand_val * 7) % 10);
            IF tag3 = tag1 OR tag3 = tag2 THEN SET tag3 = 1 + ((tag3 + 2) % 10); END IF;
        END IF;

        INSERT IGNORE INTO kb_article_tag (article_id, tag_id) VALUES (i, tag1);

        IF rand_val % 100 < 60 THEN
            INSERT IGNORE INTO kb_article_tag (article_id, tag_id) VALUES (i, tag2);
        END IF;

        IF rand_val % 100 < 40 THEN
            INSERT IGNORE INTO kb_article_tag (article_id, tag_id) VALUES (i, tag3);
        END IF;

        SET i = i + 1;
    END WHILE;

    SELECT CONCAT('Article-Tag relations generated for ', article_count, ' articles') AS result;
END //

DELIMITER ;

-- -------------------------
-- 3. 执行生成
-- -------------------------
CALL generate_articles(@ARTICLE_COUNT, @BATCH_SIZE);
CALL generate_article_tags(@ARTICLE_COUNT);

-- -------------------------
-- 4. 清理存储过程
-- -------------------------
DROP PROCEDURE IF EXISTS generate_articles;
DROP PROCEDURE IF EXISTS generate_article_tags;

-- -------------------------
-- 5. 验证查询
-- -------------------------
SELECT '=== 文章总量 ===' AS '';
SELECT COUNT(*) AS total_articles FROM kb_article;

SELECT '=== 文章状态分布 ===' AS '';
SELECT status, COUNT(*) AS cnt, ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM kb_article),2) AS pct
FROM kb_article GROUP BY status ORDER BY cnt DESC;

SELECT '=== 分类分布 ===' AS '';
SELECT category_id, COUNT(*) AS cnt
FROM kb_article GROUP BY category_id ORDER BY category_id;

SELECT '=== 标签关联数量 ===' AS '';
SELECT COUNT(*) AS total_relations, COUNT(DISTINCT article_id) AS articles_with_tags
FROM kb_article_tag;

SELECT '=== 每篇标签数分布 ===' AS '';
SELECT tag_count, COUNT(*) AS articles
FROM (
    SELECT article_id, COUNT(*) AS tag_count
    FROM kb_article_tag
    GROUP BY article_id
) t GROUP BY tag_count ORDER BY tag_count;

SELECT '=== 示例数据（前5篇）===' AS '';
SELECT a.id, a.title, a.status, c.name AS category_name, a.publish_time,
       GROUP_CONCAT(t.name) AS tags
FROM kb_article a
LEFT JOIN kb_category c ON a.category_id = c.id
LEFT JOIN kb_article_tag at ON a.id = at.article_id
LEFT JOIN kb_tag t ON at.tag_id = t.id
GROUP BY a.id, a.title, a.status, c.name, a.publish_time
ORDER BY a.id
LIMIT 5;
