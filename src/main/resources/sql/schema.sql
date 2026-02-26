# 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS `strategy_engine` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `strategy_engine`;

-- 策略引擎表
CREATE TABLE `strategy_engine` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '引擎名称',
  `type` varchar(20) NOT NULL COMMENT '引擎类型：COMPREHENSIVE_REVIEW-综合复习, SINGLE_EXAM-单场考试',
  `applicable_object` varchar(20) NOT NULL COMMENT '适用对象：STUDENT-学生, CLASS-班级, GRADE-年级, BUREAU-教育局',
  `description` text COMMENT '引擎描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用, 1-启用',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认：0-否, 1-是',
  `tag_count` int(11) NOT NULL DEFAULT '0' COMMENT '标签总数',
  `scene_count` int(11) NOT NULL DEFAULT '0' COMMENT '策略总数',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_applicable_object` (`applicable_object`),
  KEY `idx_status` (`status`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略引擎表';

-- 标签规则表
CREATE TABLE `strategy_tag_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `engine_id` bigint(20) NOT NULL COMMENT '引擎ID',
  `name` varchar(100) NOT NULL COMMENT '标签名称',
  `description` varchar(500) COMMENT '标签说明',
  `rule_config` json COMMENT '规则配置（条件树JSON）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用, 1-启用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_engine_id` (`engine_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签规则表';

-- 场景策略表
CREATE TABLE `strategy_scene` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `engine_id` bigint(20) NOT NULL COMMENT '引擎ID',
  `name` varchar(100) NOT NULL COMMENT '场景名称',
  `description` varchar(500) COMMENT '场景说明',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_engine_id` (`engine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景策略表';

-- 场景标签关联表（权重配置）
CREATE TABLE `strategy_scene_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scene_id` bigint(20) NOT NULL COMMENT '场景ID',
  `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
  `weight_coefficient` int(11) NOT NULL DEFAULT '1' COMMENT '权重系数（1-10）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_tag` (`scene_id`, `tag_id`),
  KEY `idx_scene_id` (`scene_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景标签关联表';

-- 条件字段元数据表
CREATE TABLE `strategy_tag_field` (
  `id`                  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `field_key`           varchar(50)  NOT NULL COMMENT '字段标识，与条件树 field 值对应，如 difficulty_level',
  `field_name`          varchar(100) NOT NULL COMMENT '字段展示名称，如 难度等级',
  `category`            varchar(20)  NOT NULL COMMENT '字段分类：INHERENT-固有属性, EXAM-考试属性, COMPREHENSIVE-综合属性',
  `data_type`           varchar(20)  NOT NULL COMMENT '数据类型：NUMBER-数值, STRING-字符串, ENUM-枚举',
  `operators`           json         NOT NULL COMMENT '允许的运算符列表，如 [">",">=","<","<=","="]',
  `applicable_objects`  json         NOT NULL COMMENT '适用对象列表，["ALL"] 表示所有引擎通用',
  `sort`                int(11)      NOT NULL DEFAULT '0' COMMENT '分类内排序',
  `status`              tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态：0-禁用, 1-启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field_key` (`field_key`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条件字段元数据配置表';

-- 初始字段数据
INSERT INTO `strategy_tag_field` (`field_key`, `field_name`, `category`, `data_type`, `operators`, `applicable_objects`, `sort`) VALUES
-- 固有属性（所有引擎通用）
('kp_name',           '知识点名称',       'INHERENT', 'STRING', '["=","!=","CONTAINS","NOT_CONTAINS"]', '["ALL"]', 1),
('kp_catalog',        '所属目录',          'INHERENT', 'STRING', '["=","!=","IN","NOT_IN"]',             '["ALL"]', 2),
('kp_stage',          '学段',              'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 3),
('kp_course',         '所属课程',          'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 4),
('difficulty_level',  '难度等级',          'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 5),
('water_level',       '水平等级',          'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 6),
('out_degree',        '出度',              'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',          '["ALL"]', 7),
('in_degree',         '入度',              'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',          '["ALL"]', 8),
('relevance',         '关联度',            'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 9),
('base_property',     '基础性',            'INHERENT', 'ENUM',   '["=","!=","IN","NOT_IN"]',             '["ALL"]', 10),
('exam_count_area',   '知识点考核次数(区域)', 'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',       '["ALL"]', 11),
('exam_score_area',   '知识点考核分数(区域)', 'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',       '["ALL"]', 12),
('importance',        '重要度',            'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',          '["ALL"]', 13),
('importance_area',   '重要度(区域)',       'INHERENT', 'NUMBER', '[">",">=","<","<=","=","!="]',          '["ALL"]', 14),
-- 考试属性（学校/班级/学生）
('exam_mastery',      '考试掌握度',        'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 1),
('exam_target',       '目标值(考试)',       'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 2),
('exam_reliability',  '可信度(考试)',       'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 3),
('exam_item_count',   '涉及试题数',        'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 4),
('exam_score',        '涉及分数(考试)',     'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 5),
('exam_pass_status',  '达标状态(考试)',     'EXAM', 'ENUM',   '["=","!=","IN","NOT_IN"]',    '["SCHOOL","CLASS","STUDENT"]', 6),
('exam_pass_rate',    '达标占比(考试)',     'EXAM', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS"]',           7),
-- 综合属性（学校/班级/学生，达标占比仅学校/班级）
('overall_mastery',   '综合掌握度',        'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 1),
('overall_target',    '目标值(综合)',       'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 2),
('overall_reliability','可信度(综合)',      'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 3),
('total_item_count',  '累计考核试题数',     'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 4),
('total_score',       '累计考核分数',       'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS","STUDENT"]', 5),
('mastery_trend',     '掌握趋势',           'COMPREHENSIVE', 'ENUM',   '["=","!=","IN","NOT_IN"]',    '["SCHOOL","CLASS","STUDENT"]', 6),
('overall_pass_status','达标状态(综合)',    'COMPREHENSIVE', 'ENUM',   '["=","!=","IN","NOT_IN"]',    '["SCHOOL","CLASS","STUDENT"]', 7),
('overall_pass_rate', '达标占比(综合)',     'COMPREHENSIVE', 'NUMBER', '[">",">=","<","<=","=","!="]', '["SCHOOL","CLASS"]',           8);
