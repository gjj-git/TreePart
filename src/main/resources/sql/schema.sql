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
CREATE TABLE `tag_rule` (
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
CREATE TABLE `scene_strategy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `engine_id` bigint(20) NOT NULL COMMENT '引擎ID',
  `name` varchar(100) NOT NULL COMMENT '场景名称',
  `description` varchar(500) COMMENT '场景说明',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用, 1-启用',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_engine_id` (`engine_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景策略表';

-- 场景标签关联表（权重配置）
CREATE TABLE `scene_tag_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scene_id` bigint(20) NOT NULL COMMENT '场景ID',
  `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-否, 1-是',
  `weight_coefficient` int(11) NOT NULL DEFAULT '1' COMMENT '权重系数（1-10）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_tag` (`scene_id`, `tag_id`),
  KEY `idx_scene_id` (`scene_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景标签关联表';
