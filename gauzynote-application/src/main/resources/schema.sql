/*
 Source Server Type    : MySQL
 Source Schema         : gauzy_note
 Target Server Type    : MySQL
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file` (
                                        `file_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识ID',
    `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
    `file_name` varchar(255) DEFAULT NULL COMMENT '文件名（存在重复）',
    `file_path` varchar(3000) DEFAULT NULL COMMENT '在服务器的存储路径',
    `file_type` varchar(10) DEFAULT NULL COMMENT '文件类型',
    `file_size` int(11) DEFAULT NULL COMMENT '文件大小（单位：字节）',
    `storage_engine` varchar(255) DEFAULT NULL COMMENT '存储引擎（local：本地存储，oss：对象存储等）',
    `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    PRIMARY KEY (`file_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='文件表';


-- ----------------------------
-- Table structure for note
-- ----------------------------
CREATE TABLE IF NOT EXISTS `note` (
                                      `note_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `user_id` bigint(20) NOT NULL COMMENT '用户id',
    `note_name` varchar(255) DEFAULT NULL COMMENT '名称',
    `content` mediumtext COMMENT '笔记内容',
    `content_html` mediumtext COMMENT 'html内容',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    PRIMARY KEY (`note_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='notes';


-- ----------------------------
-- Table structure for oper_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `oper_log` (
                                          `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `origin` char(1) DEFAULT '1' COMMENT '来源1:客户端',
    `operation_name` varchar(255) DEFAULT NULL COMMENT '操作名称',
    `business_type` int(2) DEFAULT NULL COMMENT '操作类型（0其他 1新增 2删除 3修改 4查询 5登录 6注册 7注销）',
    `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
    `request_url` varchar(1000) DEFAULT NULL COMMENT '请求URI',
    `uuid` varbinary(255) DEFAULT NULL COMMENT 'uuid',
    `request_params` varchar(4000) DEFAULT NULL COMMENT '请求参数',
    `response_status` varbinary(255) DEFAULT NULL COMMENT '响应状态（200代表成功，500代表失败）',
    `execution_time` bigint(20) DEFAULT NULL COMMENT '执行时长ms',
    `response_data` text COMMENT '响应数据',
    `ip_address` varbinary(100) DEFAULT NULL COMMENT '用户IP地址',
    `address_status` int(5) DEFAULT NULL COMMENT '状态码，0为正常',
    `address_message` varchar(255) DEFAULT NULL COMMENT '对addressStatus的描述',
    `lat` varchar(255) DEFAULT NULL COMMENT '纬度',
    `lng` varchar(255) DEFAULT NULL COMMENT '经度',
    `nation` varchar(255) DEFAULT NULL COMMENT '国家',
    `province` varchar(255) DEFAULT NULL COMMENT '省',
    `city` varchar(255) DEFAULT NULL COMMENT '市',
    `district` varchar(255) DEFAULT NULL COMMENT '区',
    `adcode` varchar(255) DEFAULT NULL COMMENT '行政区划代码',
    `current_url` varchar(1000) DEFAULT NULL COMMENT 'url地址',
    `host` varchar(255) DEFAULT NULL COMMENT 'host地址',
    `request_origin` varchar(255) DEFAULT NULL COMMENT 'origin地址',
    `referrer` varchar(3000) DEFAULT NULL COMMENT 'document.referrer',
    `ancestor_origins` varchar(3000) DEFAULT NULL COMMENT 'location.ancestorOrigins',
    `client_version` varchar(255) DEFAULT NULL COMMENT '客户端版本号',
    `user_agent` varchar(1000) DEFAULT NULL COMMENT '用户代理信息',
    `create_time` datetime DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`oper_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=1028 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='操作日志表';


-- ----------------------------
-- Table structure for recycle_bin
-- ----------------------------
CREATE TABLE IF NOT EXISTS `recycle_bin` (
                                             `recycle_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '回收站记录唯一ID',
    `node_id` bigint(20) DEFAULT NULL COMMENT '关联资源节点表的node_id，标识被删除的资源',
    `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
    `resource_type` char(1) DEFAULT NULL COMMENT '资源类型，与resource_node表的node_type一致',
    `resource_name` varchar(255) DEFAULT NULL COMMENT '删除时的资源名称，用于回收站展示',
    `permanently_deleted` char(1) DEFAULT '0' COMMENT '是否已彻底删除：0-在回收站 2-已彻底删除',
    `permanent_delete_time` datetime DEFAULT NULL COMMENT '彻底删除的时间（仅当permanently_deleted=1时有效）',
    `delete_time` datetime DEFAULT NULL COMMENT '资源被放入回收站的时间',
    PRIMARY KEY (`recycle_id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='回收站记录';


-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_config` (
                                            `config_id` int(5) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
    `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
    `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
    `is_system` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `description` varchar(255) DEFAULT '' COMMENT '参数描述',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`config_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='系统参数配置表';


-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
                                               `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
    `dict_sort` int(4) DEFAULT '0' COMMENT '字典排序',
    `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
    `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
    `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
    `user_id` bigint(20) DEFAULT NULL COMMENT '归属用户ID（NULL为全局数据）',
    `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
    `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_code`) USING BTREE,
    KEY `idx_dict_data_type_user` (`dict_type`, `user_id`) USING BTREE,
    KEY `idx_dict_data_status_user` (`status`, `user_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='字典数据表';

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
                                               `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
    `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
    `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
    `user_id` bigint(20) DEFAULT NULL COMMENT '归属用户ID（NULL为系统公共数据）',
    `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`dict_id`) USING BTREE,
    UNIQUE KEY `dict_type` (`dict_type`) USING BTREE,
    KEY `idx_dict_type_user` (`user_id`) USING BTREE,
    KEY `idx_dict_type_status_user` (`status`, `user_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='字典类型表';


-- ----------------------------
-- Table structure for sys_resource_node
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_resource_node` (
                                                   `node_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id` bigint(20) DEFAULT NULL COMMENT '父节点ID',
    `related_id` bigint(20) DEFAULT NULL COMMENT '关联的具体文件ID（仅文件类型节点有效）',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `node_type` char(1) NOT NULL DEFAULT '1' COMMENT '节点类型（1文件夹，2node，3file）',
    `node_name` varchar(255) DEFAULT NULL COMMENT '节点名称',
    `sort` int(11) DEFAULT '0' COMMENT '排序',
    `node_path` varchar(3000) DEFAULT NULL COMMENT '存储完整路径',
    `depth` int(11) DEFAULT '0' COMMENT '存储层级深度',
    `create_time` datetime DEFAULT NULL,
    `update_time` datetime DEFAULT NULL,
    `del_flag` varchar(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    PRIMARY KEY (`node_id`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=262 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='资源节点表';


-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
                                          `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(30) NOT NULL COMMENT '用户账号',
    `nickname` varchar(30) NOT NULL COMMENT '用户昵称',
    `user_type` varchar(1) DEFAULT '2' COMMENT '用户类型: （0管理员 1普通用户 2只读用户）',
    `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
    `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
    `avatar` varchar(100) DEFAULT '' COMMENT '头像地址',
    `password` varchar(100) DEFAULT '' COMMENT '密码',
    `status` char(1) DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
    `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
    `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
    `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`user_id`) USING BTREE,
    UNIQUE KEY `username` (`username`) USING BTREE
    ) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='用户信息表';


-- ----------------------------
-- Table structure for sys_user_webauthn_credential
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_webauthn_credential` (
    `credential_record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
    `credential_id` varchar(1024) NOT NULL COMMENT '凭证ID(base64url)',
    `credential_name` varchar(128) DEFAULT '' COMMENT '凭证名称',
    `public_key` text NOT NULL COMMENT '公钥(SPKI base64)',
    `algorithm` varchar(32) NOT NULL COMMENT '签名算法标识',
    `sign_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '签名计数器',
    `aaguid` varchar(64) DEFAULT '' COMMENT '认证器AAGUID',
    `transports` varchar(255) DEFAULT '' COMMENT '传输能力，逗号分隔',
    `user_handle` varchar(255) DEFAULT '' COMMENT '用户句柄(base64url)',
    `browser_name` varchar(128) DEFAULT '' COMMENT '浏览器信息',
    `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1禁用）',
    `last_used_time` datetime DEFAULT NULL COMMENT '最后使用时间',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`credential_record_id`) USING BTREE,
--     UNIQUE KEY `uk_credential_id` (`credential_id`) USING BTREE,
    KEY `idx_webauthn_user_status` (`user_id`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='用户WebAuthn凭证表';


-- ----------------------------
-- Table structure for sys_user_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_config` (
                                                 `user_config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `user_id` bigint(20) NOT NULL COMMENT '关联的用户ID',
    `config_key` varchar(100) NOT NULL COMMENT '参数键',
    `config_value` varchar(500) DEFAULT '' COMMENT '参数值',
    `description` varchar(255) DEFAULT '' COMMENT '参数描述',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`user_config_id`) USING BTREE,
    UNIQUE KEY `idx_user_key` (`user_id`,`config_key`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='用户个性化配置表';


-- ----------------------------
-- Table structure for task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `task` (
                        `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增id',
                        `user_id` bigint(20) NOT NULL COMMENT '所属用户id',
                        `task_title` varchar(100) DEFAULT NULL COMMENT '任务标题',
                        `task_content` varchar(300) DEFAULT NULL COMMENT '任务内容',
                        `start_time` date DEFAULT NULL COMMENT '开始时间',
                        `end_time` date DEFAULT NULL COMMENT '截止时间',
                        `actual_completion_time` date DEFAULT NULL COMMENT '实际完成时间',
                        `status` char(1) DEFAULT NULL COMMENT '0: 未开始, 1: 进行中, 2: 已完成, 3: 已停滞',
                        `priority` char(1) DEFAULT NULL COMMENT '0: 低, 1: 中, 2: 高',
                        `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
                        `kanban_sort` int(11) NOT NULL DEFAULT '0' COMMENT '看板排序',
                        `gantt_sort` int(11) NOT NULL DEFAULT '0' COMMENT '甘特图排序',
                        `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                        `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                        `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                        PRIMARY KEY (`task_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='任务表';



SET FOREIGN_KEY_CHECKS = 1;
