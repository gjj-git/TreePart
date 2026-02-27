# 【策略引擎服务】-【策略引擎管理】- 后端设计文档

| 版本 | 作者 | 日期 | 备注 |
|------|------|------|------|
| v1.0 | 郭俊杰 | 2026-02-25 | 初稿 |
| v1.1 | 郭俊杰 | 2026-02-25 | 修正引擎应用流程；删除场景匹配接口；补充标签条件与存储设计 |
| v1.2 | 郭俊杰 | 2026-02-26 | 新增条件字段元数据（StrategyTagField）模块；表名统一使用 strategy_ 前缀；移除 strategy_scene.status 和 strategy_scene_tag.enabled；Java 类名全部加 Strategy 前缀 |
| v1.3 | 郭俊杰 | 2026-02-27 | 新增枚举选项查询接口；新增按适用对象获取默认引擎接口；默认引擎设置改为原子 SQL |

---

## 目录

1. [功能概述](#1-功能概述)
2. [术语表](#2-术语表)
3. [技术栈](#3-技术栈)
4. [详细设计](#4-详细设计)
5. [数据库设计](#5-数据库设计)
6. [接口设计](#6-接口设计)
7. [附录](#7-附录)

---

## 1. 功能概述

策略引擎管理模块为上层业务（个辅推荐、作业分配、复习计划等）提供可配置的**知识点优先级计算**能力。通过标签规则将知识点的多维学习数据转化为学情标签，再通过场景策略对标签加权求和，从不同教学目标出发输出知识点优先级得分，实现业务规则与代码逻辑解耦。

**主要功能：**

| 模块 | 功能点 |
|------|--------|
| 策略引擎管理 | 引擎的增删改查、启用/禁用、设置/取消默认 |
| 标签规则管理 | 标签的增删改查、启用/禁用、使用情况查询 |
| 场景策略管理 | 场景的增删改查、场景关联标签配置 |
| 条件字段管理 | 规则编辑器可用字段的增删改查、启用/禁用、按引擎过滤分组 |
| 引擎配置对外输出 | 向调用方提供标签规则列表及场景权重配置，由调用方执行打标签与计分 |

---

## 2. 术语表

| 术语 | 说明 |
|------|------|
| 策略引擎（StrategyEngine） | 顶层配置单元，定义一套完整的标签规则 + 场景策略体系 |
| 标签规则（StrategyTagRule） | 引擎下的一条诊断规则，通过条件树判断知识点是否命中某类学情，命中则打上对应标签 |
| 条件树（RuleConfig） | 以 JSON 表示的嵌套条件结构，叶节点为字段条件，非叶节点为 AND/OR 逻辑组合 |
| 条件字段（StrategyTagField） | 条件树叶节点中的判断字段元数据，如"考试掌握度"、"难度等级"等，按适用对象过滤后供前端规则编辑器使用 |
| 场景策略（StrategyScene） | 引擎下的一个教学目标定义，关联若干标签规则并设置权重系数 |
| 权重系数（WeightCoefficient） | 标签在所属场景中的重要程度，取值 1-10，参与知识点得分计算 |
| 知识点得分 | 计算公式：`得分 = Σ (命中标签在此场景下的权重系数)` |
| 默认引擎（isDefault） | 同一时刻只允许一个引擎处于默认状态 |

---

## 3. 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / 框架 | Java 17 / Spring Boot 2.7.18 |
| ORM | MyBatis-Plus 3.5.3.1 |
| 数据库 | MySQL 8.0 |
| 连接池 | Druid 1.2.20 |
| 接口文档 | SpringDoc OpenAPI 3.0（Swagger UI） |
| 工具库 | Hutool 5.8.22 / Lombok 1.18.30 |
| 服务端口 | 8080，统一前缀 `/api` |

---

## 4. 详细设计

### 4.1 引擎应用流程（调用方视角）

本模块仅负责配置存储，调用方按以下流程完成知识点优先级计算：

```
1. 获取配置
   GET /api/tag/list/{engineId}/enabled  → 所有启用标签规则（含 ruleConfig JSON）
   GET /api/scene/list/{engineId}        → 所有场景及标签权重配置

2. 打标签（对每个知识点）
   调用 RuleMatchEngine.match(ruleConfigJson, dataMap)
   · 命中规则 → 打上对应标签
   · 未命中  → 无标签

3. 场景计分（对每个知识点 × 每个场景）
   得分 = Σ (命中标签在此场景下的权重系数)
   示例（高考冲刺："基础薄弱"权重3，"高频错题"权重10）：
   · 命中两个标签 → 得分 = 13
   · 仅命中"基础薄弱" → 得分 = 3

4. 按得分降序排列知识点，得分越高优先级越高
```

### 4.2 标签条件树结构

条件树以 JSON 存储于 `strategy_tag_rule.rule_config`，由两种节点组成：

**Group 节点**（逻辑组合）

```json
{ "type": "group", "operator": "AND", "children": [ ... ] }
```

**Condition 节点**（条件判断）

```json
{ "type": "condition", "field": "exam_mastery", "operator": ">", "value": "60" }
```

`operator` 字段含义：`type=group` 时为 `AND`/`OR`；`type=condition` 时为比较运算符。

**支持的运算符：**

| 类型 | 运算符 |
|------|--------|
| 数值比较 | `>` `>=` `<` `<=` `=` `!=`（及别名 GT/GTE/LT/LTE/EQ/NEQ） |
| 字符串 | `=` `!=` `CONTAINS` `NOT_CONTAINS` |
| 枚举列表 | `IN` `NOT_IN`（value 使用逗号分隔） |

> `RuleMatchEngine` 递归求值：Group 节点按 AND/OR 聚合子结果，Condition 节点从 dataMap 取值后按运算符比较，数值比较使用 BigDecimal。

### 4.3 条件字段元数据（StrategyTagField）

前端规则编辑器左侧字段库所需的字段列表由 `strategy_tag_field` 表统一维护，支持按引擎适用对象过滤。

**字段分类：**

| 分类 | 说明 | 适用对象 |
|------|------|----------|
| INHERENT（固有属性） | 知识点本身的固有属性，如难度等级、知识点关联度等 | ALL（所有引擎通用） |
| EXAM（考试属性） | 来自单次考试的维度数据，如考试掌握度、达标状态等 | SCHOOL / CLASS / STUDENT |
| COMPREHENSIVE（综合属性） | 跨多次考试的综合统计维度，如综合掌握度、掌握趋势等 | SCHOOL / CLASS / STUDENT |

**过滤规则：** 查询时通过 `JSON_CONTAINS(applicable_objects, '"ALL"')` 或 `JSON_CONTAINS(applicable_objects, '"STUDENT"')` 筛选当前引擎适用对象的可用字段，结果按分类（INHERENT→EXAM→COMPREHENSIVE）和 sort 排序后分组返回。

### 4.4 关键业务逻辑

#### 引擎删除（级联）

```
1. 查询引擎下所有场景 ID
2. 物理删除 strategy_scene_tag（WHERE scene_id IN (...)）
3. 逻辑删除所有场景
4. 逻辑删除所有标签规则
5. 逻辑删除引擎
```

#### 标签禁用/删除保护

操作前查询 `strategy_scene_tag WHERE tag_id = {id}`，若 count > 0 则拒绝并提示"该标签正在被 N 个场景使用"。

#### 场景标签关联保存（全量替换）

```
tags = null  → 不修改现有关联
tags = []    → 清空所有标签关联
tags = [...]  → 先物理删除现有关联，再批量 INSERT
```

#### 默认引擎唯一性

设置默认时执行单条原子 SQL：

```sql
UPDATE strategy_engine
SET is_default = CASE WHEN id = #{id} THEN 1 ELSE 0 END
WHERE deleted = 0
```

一条语句同时完成"清除其他默认"和"设置目标默认"，避免并发下两步 UPDATE 产生多个默认引擎的竞态问题。

#### 统计字段自动维护

`tag_count`、`scene_count` 在标签/场景增删时重新 COUNT 后写回，保证绝对准确。

---

## 5. 数据库设计

### 5.1 ER 关系

```
strategy_engine (1) ──── (N) strategy_tag_rule
       │
       └──── (N) strategy_scene (N) ──── (N) strategy_tag_rule
                       通过 strategy_scene_tag 关联，额外记录 weight_coefficient

strategy_tag_field  （独立元数据表，不与其他表关联）
```

### 5.2 表结构

#### strategy_engine

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| name | varchar(100) | 引擎名称 |
| type | varchar(20) | COMPREHENSIVE_REVIEW / SINGLE_EXAM |
| applicable_object | varchar(20) | STUDENT / CLASS / GRADE / BUREAU |
| description | text | 引擎描述 |
| status | tinyint(1) | 0-禁用 1-启用，默认 1 |
| is_default | tinyint(1) | 0-否 1-是，默认 0 |
| tag_count | int | 冗余统计：关联标签数 |
| scene_count | int | 冗余统计：关联场景数 |
| created_time / updated_time | datetime | 自动维护 |
| deleted | tinyint(1) | 逻辑删除，默认 0 |

#### strategy_tag_rule

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| engine_id | bigint | 所属引擎 |
| name | varchar(100) | 标签名称 |
| description | varchar(500) | 标签说明 |
| rule_config | json | 条件树 JSON，详见 4.2 节 |
| status | tinyint(1) | 0-禁用 1-启用 |
| created_time / updated_time / deleted | - | 同上 |

#### strategy_scene

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| engine_id | bigint | 所属引擎 |
| name | varchar(100) | 场景名称 |
| description | varchar(500) | 场景说明 |
| created_time / updated_time / deleted | - | 同上 |

#### strategy_scene_tag

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| scene_id | bigint | 场景 ID |
| tag_id | bigint | 标签规则 ID |
| weight_coefficient | int | 权重系数 1-10 |

> 无逻辑删除字段，所有删除均为物理删除。唯一索引：`uk_scene_tag(scene_id, tag_id)`。

#### strategy_tag_field

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| field_key | varchar(50) | 字段标识，与条件树 field 值对应，全局唯一 |
| field_name | varchar(100) | 展示名称，如"考试掌握度" |
| category | varchar(20) | INHERENT / EXAM / COMPREHENSIVE |
| data_type | varchar(20) | NUMBER / STRING / ENUM |
| operators | json | 允许的运算符列表，如 `[">",">=","<","<=","=","!="]` |
| applicable_objects | json | 适用对象列表，`["ALL"]` 表示所有引擎通用 |
| sort | int | 分类内排序 |
| status | tinyint(1) | 0-禁用 1-启用 |

> `operators` 和 `applicable_objects` 均为 MySQL JSON 类型，MyBatis-Plus 通过 `@TableField(typeHandler = JacksonTypeHandler.class)` + `@TableName(autoResultMap = true)` 自动映射为 `List<String>`。

### 5.3 枚举说明

| 枚举 | 值 |
|------|----|
| EngineType | COMPREHENSIVE_REVIEW（综合复习）/ SINGLE_EXAM（单场考试） |
| ApplicableObject | STUDENT / CLASS / GRADE / BUREAU |
| Status | 0-禁用 / 1-启用 |

---

## 6. 接口设计

统一响应格式：`{ "code": 200, "message": "success", "data": {}, "timestamp": 1700000000000 }`

### 6.1 策略引擎管理 `/api/engine`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page | 分页查询（支持 name/type/applicableObject/status 筛选） |
| GET | /{id} | 引擎详情 |
| POST | / | 创建引擎 |
| PUT | / | 更新引擎 |
| DELETE | /{id} | 删除引擎（级联删除标签、场景及关联） |
| PUT | /{id}/toggleStatus | 切换启用/禁用 |
| PUT | /{id}/setDefault | 设为默认引擎（原子 SQL，全局唯一） |
| PUT | /{id}/cancelDefault | 取消默认引擎 |
| GET | /enums | 获取枚举选项列表（引擎类型、适用对象） |
| GET | /default | 按适用对象获取默认引擎（不匹配则返回 null） |

### 6.2 标签规则管理 `/api/tag`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page/{engineId} | 分页查询 |
| GET | /{id} | 标签详情 |
| POST | / | 创建标签 |
| PUT | / | 更新标签 |
| DELETE | /{id} | 删除标签（存在场景引用时拒绝） |
| DELETE | /batch/{engineId} | 批量删除引擎下所有标签 |
| GET | /list/{engineId} | 全量列表（不分页） |
| GET | /list/{engineId}/enabled | 启用标签列表，**供调用方获取参与求值的规则** |
| PUT | /{id}/toggleStatus | 切换状态（启用→禁用时做引用检查） |
| GET | /{id}/usage | 查询标签被哪些场景引用 |

### 6.3 场景策略管理 `/api/scene`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page/{engineId} | 分页查询 |
| GET | /list/{engineId} | 全量列表（含标签权重），**供调用方获取计分配置** |
| GET | /{id} | 场景详情（含标签权重） |
| POST | / | 创建场景（可同时配置标签关联） |
| PUT | / | 更新场景（tags=null 不动关联；tags=[] 清空；tags=[...] 全量替换） |
| DELETE | /{id} | 删除场景（级联删除标签关联） |
| POST | /{sceneId}/tags | 单独配置场景标签关联（全量替换） |

### 6.4 条件字段管理 `/api/field`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /grouped/{engineId} | 按引擎适用对象过滤，按分类分组返回，**供前端规则编辑器字段库使用** |
| GET | /list | 查询所有字段（管理页面使用） |
| GET | /{id} | 字段详情 |
| POST | / | 创建字段（field_key 全局唯一校验） |
| PUT | / | 更新字段（field_key 变更时唯一性校验） |
| DELETE | /{id} | 删除字段 |
| PUT | /{id}/toggleStatus | 切换启用/禁用 |

**`GET /api/field/grouped/{engineId}` 响应示例：**

```json
[
  {
    "category": "INHERENT",
    "categoryName": "固有属性",
    "fields": [
      { "id": 1, "fieldKey": "difficulty_level", "fieldName": "难度等级", "dataType": "ENUM",
        "operators": ["=","!=","IN","NOT_IN"], "sort": 5 }
    ]
  },
  {
    "category": "EXAM",
    "categoryName": "考试属性",
    "fields": [ ... ]
  }
]
```

**`GET /api/engine/enums` 响应示例：**

```json
{
  "engineTypes": [
    { "code": "COMPREHENSIVE_REVIEW", "label": "综合复习" },
    { "code": "SINGLE_EXAM",          "label": "单场考试" }
  ],
  "applicableObjects": [
    { "code": "STUDENT", "label": "学生" },
    { "code": "CLASS",   "label": "班级" },
    { "code": "GRADE",   "label": "年级" },
    { "code": "BUREAU",  "label": "教育局" }
  ]
}
```

**`GET /api/engine/default?applicableObject=BUREAU` 说明：**

查询 `is_default=1` 且 `applicable_object` 与传入值一致的引擎。若全局默认引擎的适用对象不匹配（如默认引擎为 STUDENT 类型，调用方传 BUREAU），则 `data` 返回 `null`，HTTP 状态码仍为 200，调用方自行降级处理。

### 6.5 备用接口（当前未启用）

`EngineFullConfigController`（`/api/engine-config`）：一次性获取/提交/校验引擎完整配置，可在前端改为单步保存时启用，替代当前分步接口。

---

## 7. 附录

### 7.1 项目结构

```
src/main/java/com/strategy/engine/
├── common/          Result.java
├── config/          MyMetaObjectHandler.java
├── controller/      StrategyEngineController    StrategyTagRuleController
│                    StrategySceneController     StrategyTagFieldController
│                    EngineFullConfigController（备用）
├── dto/             StrategyEngineDTO           StrategyEngineQueryDTO
│                    StrategyTagRuleDTO          StrategySceneDTO
│                    StrategySceneTagItemDTO
│                    StrategyTagFieldDTO         EngineFullConfigDTO
├── entity/          StrategyEngine  StrategyTagRule  StrategyScene
│                    StrategySceneTag  StrategyTagField
├── enums/           EngineType  ApplicableObject  StatusEnum
├── exception/       BusinessException  GlobalExceptionHandler
├── mapper/          StrategyEngineMapper        StrategyTagRuleMapper
│                    StrategySceneMapper         StrategySceneTagMapper
│                    StrategyTagFieldMapper
├── rule/            RuleMatchEngine（条件树求值，供调用方集成）  RuleNode
├── service/         StrategyEngineService       StrategyTagRuleService
│                    StrategySceneService        StrategyTagFieldService
│                    EngineFullConfigService（备用）
│   └── impl/        （各 ServiceImpl）
└── vo/              StrategyEngineVO            StrategyTagRuleVO
                     StrategySceneVO             StrategySceneTagVO
                     StrategyTagFieldVO          StrategyTagFieldGroupVO
                     TagUsageVO                  EngineFullConfigVO

src/main/resources/
├── application.yml
└── sql/schema.sql
```

### 7.2 待优化项

| 优先级 | 说明 |
|--------|------|
| 低 | 调用方高频获取引擎配置时可引入 Redis 缓存（以 engineId 为 key），配置变更时主动失效 |
| 低 | 引擎类型与适用对象当前以字符串存储，业务稳定后可改为数字枚举降低存储开销 |
| 低 | 备用的 EngineFullConfigService 可在前端改为单步保存时启用，替代当前分步接口 |
