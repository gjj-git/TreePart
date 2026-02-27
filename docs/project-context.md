# 策略引擎管理系统 — 项目上下文速查

> 用途：供后续对话快速理解项目结构，避免重复探索。
> 最后更新：2026-02-26

---

## 一、项目概述

**名称**：strategy-engine-management
**定位**：教育场景知识点优先级计算引擎的配置管理后端。上层业务（个辅推荐、作业分配、复习计划）通过本服务配置"标签规则 + 场景权重"，由调用方按配置对知识点打标签并加权计分，输出知识点优先级得分。

**技术栈**：Spring Boot 2.7.18 / MyBatis-Plus 3.5.3.1 / MySQL 8.0 / Druid 1.2.20 / SpringDoc OpenAPI 3.0
**服务端口**：8080，统一前缀 `/api`
**Swagger UI**：`http://localhost:8080/api/swagger-ui.html`

---

## 二、核心概念

| 概念 | 对应实体 | 说明 |
|------|----------|------|
| 策略引擎 | `StrategyEngine` | 顶层配置单元，定义适用对象（STUDENT/CLASS/GRADE/BUREAU）和引擎类型 |
| 标签规则 | `StrategyTagRule` | 引擎下的诊断规则，通过条件树（JSON）判断知识点是否命中某类学情 |
| 场景策略 | `StrategyScene` | 引擎下的教学目标定义，关联若干标签并设置权重系数 |
| 场景标签关联 | `StrategySceneTag` | 场景与标签的多对多关联，记录权重系数（1-10） |
| 条件字段 | `StrategyTagField` | 条件树叶节点的字段元数据库，供前端规则编辑器使用 |

**知识点得分公式**：`得分 = Σ (命中标签在此场景下的 weightCoefficient)`

---

## 三、数据库（共 5 张表）

数据库名：`strategy_engine`

| 表名 | 说明 | 逻辑删除 |
|------|------|----------|
| `strategy_engine` | 策略引擎 | 有（deleted） |
| `strategy_tag_rule` | 标签规则 | 有（deleted） |
| `strategy_scene` | 场景策略 | 有（deleted） |
| `strategy_scene_tag` | 场景标签关联 | 无（物理删除） |
| `strategy_tag_field` | 条件字段元数据 | 无 |

**关键设计点**：
- `strategy_scene` 无 `status` 字段（场景只有创建/删除，无启用禁用）
- `strategy_scene_tag` 无 `enabled` 字段（有关联即生效）
- `strategy_tag_field.operators` 和 `applicable_objects` 为 MySQL JSON 类型，通过 `@TableName(autoResultMap=true)` + `JacksonTypeHandler` 映射为 `List<String>`
- `strategy_engine` 有冗余统计字段 `tag_count`、`scene_count`，在标签/场景增删时重新 COUNT 写回

---

## 四、源码目录结构

```
src/main/java/com/strategy/engine/
├── common/         Result.java（统一响应：code/message/data/timestamp）
├── config/         MyMetaObjectHandler.java（createdTime/updatedTime 自动填充）
├── controller/     4个业务Controller + EngineFullConfigController（备用）
├── dto/            8个 DTO 类（含 EngineFullConfigDTO 内嵌4个内部类）
├── entity/         5个 Entity 类
├── enums/          EngineType / ApplicableObject / StatusEnum
├── exception/      BusinessException + GlobalExceptionHandler
├── mapper/         5个 Mapper（2个有自定义方法）
├── rule/           RuleMatchEngine + RuleNode（条件树求值核心算法）
├── service/        4个业务Service接口 + EngineFullConfigService（备用）
│   └── impl/       5个 ServiceImpl
└── vo/             8个 VO 类
```

---

## 五、接口速查

### 策略引擎 `/api/engine`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page | 分页查询（name/type/applicableObject/status 筛选） |
| GET | /{id} | 详情 |
| POST | / | 创建 |
| PUT | / | 更新 |
| DELETE | /{id} | 删除（级联删除标签、场景及关联） |
| PUT | /{id}/toggleStatus | 切换启用/禁用 |
| PUT | /{id}/setDefault | 设为默认 |
| PUT | /{id}/cancelDefault | 取消默认 |

### 标签规则 `/api/tag`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page/{engineId} | 分页查询 |
| GET | /{id} | 详情 |
| POST | / | 创建 |
| PUT | / | 更新 |
| DELETE | /{id} | 删除（有场景引用时拒绝） |
| DELETE | /batch/{engineId} | 按引擎批量删除 |
| GET | /list/{engineId} | 全量列表 |
| GET | /list/{engineId}/enabled | 启用标签列表（**供调用方**） |
| PUT | /{id}/toggleStatus | 切换状态（启用→禁用时做引用检查） |
| GET | /{id}/usage | 查询被哪些场景引用 |

### 场景策略 `/api/scene`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /page/{engineId} | 分页查询 |
| GET | /list/{engineId} | 全量列表含标签权重（**供调用方**） |
| GET | /{id} | 详情含标签权重 |
| POST | / | 创建（可同时配置标签关联） |
| PUT | / | 更新（tags=null 不动；tags=[] 清空；tags=[...] 全量替换） |
| DELETE | /{id} | 删除（级联删除标签关联） |
| POST | /{sceneId}/tags | 单独配置场景标签关联（全量替换） |

### 条件字段 `/api/field`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /grouped/{engineId} | 按引擎过滤，按分类分组（**供规则编辑器**） |
| GET | /list | 所有字段（管理页） |
| GET | /{id} | 详情 |
| POST | / | 创建（field_key 唯一） |
| PUT | / | 更新 |
| DELETE | /{id} | 删除 |
| PUT | /{id}/toggleStatus | 切换状态 |

### 备用：完整配置 `/api/engine-config`（当前未启用）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /{engineId} | 获取引擎完整三Tab配置 |
| POST | / | 一次性保存所有配置（事务） |
| POST | /validate | 验证配置有效性 |

---

## 六、关键 Service 业务逻辑要点

### 引擎删除（级联）
1. 查询引擎下所有场景 ID
2. 物理删除 `strategy_scene_tag`（WHERE scene_id IN (...)）
3. 逻辑删除所有场景、所有标签规则、引擎本身

### 默认引擎唯一性
同一事务：先 `UPDATE strategy_engine SET is_default = 0`（全清），再将目标引擎 `is_default = 1`

### 标签删除/禁用保护
查询 `strategy_scene_tag WHERE tag_id = {id}`，count > 0 则抛 BusinessException

### 场景标签关联保存（StrategySceneServiceImpl）
- `tags = null` → 不修改
- `tags = []` → 清空（物理删除）
- `tags = [...]` → 先删后批量 INSERT（`StrategySceneTagMapper.insertBatch`）

### 统计字段维护
标签/场景增删后重新 COUNT 写回 `strategy_engine.tag_count / scene_count`

---

## 七、条件字段元数据（strategy_tag_field）

**字段分类：**

| 分类 | 适用对象 | 预置字段数 |
|------|----------|-----------|
| INHERENT（固有属性） | ALL（所有引擎通用） | 14 条 |
| EXAM（考试属性） | SCHOOL/CLASS/STUDENT | 7 条 |
| COMPREHENSIVE（综合属性） | SCHOOL/CLASS/STUDENT | 8 条 |

**过滤 SQL**（`StrategyTagFieldMapper.listByApplicableObject`）：
```sql
WHERE status = 1
  AND (JSON_CONTAINS(applicable_objects, '"ALL"')
    OR JSON_CONTAINS(applicable_objects, '"{applicableObject}"'))
ORDER BY category, sort
```

---

## 八、RuleMatchEngine（核心算法）

文件：`rule/RuleMatchEngine.java`

```java
boolean match(String ruleConfigJson, Map<String, String> dataMap)
```

- **group 节点**：`operator = AND`（所有子节点满足）或 `OR`（任一满足）
- **condition 节点**：从 dataMap 取字段值，按运算符比较
- 支持运算符：`=`/`EQ`、`!=`/`NEQ`、`>`/`GT`、`>=`/`GTE`、`<`/`LT`、`<=`/`LTE`、`CONTAINS`、`NOT_CONTAINS`、`IN`（逗号分隔值）、`NOT_IN`

---

## 九、主要 DTO/VO 字段（快速参考）

### StrategyEngineDTO（创建/更新引擎）
`id`（更新时必填）、`name`*、`type`*、`applicableObject`*、`description`、`status`*

### StrategyTagRuleDTO（创建/更新标签）
`id`（更新时必填）、`engineId`*、`name`*、`description`、`ruleConfig`（条件树JSON字符串）、`status`*

### StrategySceneDTO（创建/更新场景）
`id`（更新时必填）、`engineId`*、`name`*、`description`、`tags`（List\<StrategySceneTagItemDTO\>，可null）

### StrategySceneTagItemDTO（嵌入场景请求的标签条目）
`tagId`*、`weightCoefficient`*（1-10）

### StrategySceneTagConfigDTO（独立配置场景标签接口）
`sceneId`*、`tagId`*、`weightCoefficient`*（1-10）

### StrategyTagFieldDTO（创建/更新字段）
`id`（更新时必填）、`fieldKey`*、`fieldName`*、`category`*、`dataType`*、`operators`*、`applicableObjects`*、`sort`、`status`*

*= 必填/校验字段

---

## 十、配置文件关键参数

| 参数 | 值 |
|------|----|
| server.port | 8080 |
| context-path | /api |
| datasource.url | jdbc:mysql://localhost:3306/strategy_engine |
| datasource.username/password | root/root |
| mybatis-plus log-impl | StdOutImpl（控制台打印SQL） |
| 逻辑删除字段 | deleted（0=未删，1=已删） |

---

## 十一、注意事项 & 已知设计决策

1. **场景无 status 字段**：场景只有创建/删除两种状态，不支持启用/禁用
2. **场景标签关联无 enabled 字段**：有关联即生效，不需要额外开关
3. **EngineFullConfigController 备用**：已实现但未接入主流程，可在前端改为单步保存时替代分步接口
4. **applicableObject 值域**：`STUDENT`/`CLASS`/`GRADE`/`BUREAU`（注意 schema.sql 的 INSERT 中条件字段使用 `SCHOOL` 而非 `GRADE`/`BUREAU`，两者各有用途）
5. **MyBatis-Plus 逻辑删除**：全局配置，`strategy_scene_tag` 无 deleted 字段，操作为物理删除
6. **`insertBatch` 非 MyBatis-Plus 内置**：`StrategySceneTagMapper` 自定义 `@Insert` SQL 实现批量插入
