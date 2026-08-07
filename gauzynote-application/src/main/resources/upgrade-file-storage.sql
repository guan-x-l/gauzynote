-- ============================================================
-- 方案B升级脚本：物理文件从 file 表迁移到 file_storage 表
-- file 表通过 storage_id 引用 file_storage，file_storage 维护引用计数
-- 适用：已有存量数据的数据库；全新安装直接执行 schema.sql 即可
-- 建议执行前备份数据库
-- ============================================================

-- 1. 创建物理文件存储表（与 schema.sql 保持一致）
CREATE TABLE IF NOT EXISTS `file_storage` (
    `storage_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '物理文件唯一标识ID',
    `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
    `file_hash` varchar(255) DEFAULT NULL COMMENT '内容哈希（含扩展名），与物理文件一一对应',
    `file_path` varchar(3000) DEFAULT NULL COMMENT '在服务器的存储路径',
    `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（单位：字节）',
    `ref_count` int(11) NOT NULL DEFAULT '0' COMMENT '引用该物理文件的file记录数',
    `create_time` datetime DEFAULT NULL COMMENT '首次上传时间',
    PRIMARY KEY (`storage_id`) USING BTREE,
    UNIQUE KEY `uk_storage_user_hash` (`user_id`, `file_hash`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='物理文件存储表';

-- 2. 按 用户+内容哈希 回填 file_storage（同一哈希的 file 记录共享同一个物理文件）
--    历史版本曾将图片存放在 images/ 目录、文件存放在 file/ 目录，
--    同一哈希可能对应多个路径，这里取最小路径作为物理文件路径，其余视为重复副本
INSERT INTO `file_storage` (`user_id`, `file_hash`, `file_path`, `file_size`, `ref_count`, `create_time`)
SELECT f.user_id,
       SUBSTRING_INDEX(REPLACE(f.file_path, '\\', '/'), '/', -1) AS file_hash,
       MIN(f.file_path) AS file_path,
       MAX(f.file_size) AS file_size,
       COUNT(*) AS ref_count,
       MIN(f.upload_time) AS create_time
FROM `file` f
WHERE f.file_path IS NOT NULL AND f.file_path <> ''
GROUP BY f.user_id, file_hash;

-- 3. file 表增加 storage_id 并回填
ALTER TABLE `file`
    ADD COLUMN `storage_id` bigint(20) DEFAULT NULL COMMENT '关联file_storage表的storage_id' AFTER `user_id`;

UPDATE `file` f
JOIN `file_storage` s
    ON s.user_id = f.user_id
   AND s.file_hash = SUBSTRING_INDEX(REPLACE(f.file_path, '\\', '/'), '/', -1)
SET f.storage_id = s.storage_id;

-- 4. 确认无误后，可删除 file 表冗余的 file_path 列（保留也不影响新代码）
-- ALTER TABLE `file` DROP COLUMN `file_path`;

-- file_storage 增加 file_size（用于上传总量统计扣减）
ALTER TABLE `file_storage`
    ADD COLUMN `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（单位：字节）' AFTER `file_path`;

-- 从 file 记录回填物理文件大小（同一物理文件内容相同，取任一条即可）
UPDATE `file_storage` s
    JOIN `file` f ON f.storage_id = s.storage_id
    SET s.file_size = f.file_size
WHERE s.file_size IS NULL;
